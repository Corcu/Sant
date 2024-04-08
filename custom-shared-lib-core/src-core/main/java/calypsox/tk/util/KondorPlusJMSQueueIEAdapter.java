/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.apps.reporting.SantPolandSecurityPledgeUtil;
import calypsox.util.SantDomainValuesUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * This class contains all the functionality required to manage a sending or receiving to KondorPlus
 * JMS queue.
 */
// CAL_DODD_112
public class KondorPlusJMSQueueIEAdapter extends JMSTopicIEAdapter
        implements MessageListener, ExceptionListener {

    private static final String TOPIC = "TOPIC";
    private static final String PRODUCT_TYPE_EQUITY = "Equity";
    private static final String PRODUCT_TYPE = "ProductType";

    // Poland Security Pledge
    private static final String TOPIC_POLAND = "Poland";
    private static final String MESSAGE_PROPERTY_REVERSE_FROM = "ReverseFrom";
    // Poland Security Pledge - End

    private static final String TOPIC_MADRID = "Madrid";
    private static final String TOPIC_SCF = "SCF";
    protected static final String DESTINATION_FOLDER = "DestinationFolder";
    private static final String RUN = "Run";
    private static final String DAIL = "DAIL";
    private static final String EOD = "EOD";
    protected static final String PREFIX_BOND_ACC = "Bond_";
    protected static final String PREFIX_EQUITY_ACC = "Equity_";

    // New TripartyExternal REQ.
    private static final String TP_EXTERNAL = "TripartyExternal";
    private static final String AD_EXTERNAL = "TRIPARTY_EXTERNAL";

    private static final String KEY_MUREX = "MurexReversedAllocationTrade";


    public KondorPlusJMSQueueIEAdapter(final int isReceiver) {
        super(isReceiver);
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.JMSTopicIEAdapter#write(java.lang.String)
     */
    @Override
    public boolean write(final String message, final BOMessage boMessage) {
        try {
            final TextMessage msg = this.session.createTextMessage();

            msg.setText(message);
            msg.setStringProperty(TOPIC, getTopicAttribute(boMessage));
            Trade trade = null;

            try {
                trade = DSConnection.getDefault().getRemoteTrade().getTrade(boMessage.getTradeLongId());

                // Main message properties
                mainWrite(msg, trade);

                // Poland Security Pledge
                if (TOPIC_POLAND.equals(getTopicAttribute(boMessage))) {
                    String reverseFrom =
                            trade.getKeywordValue(SantPolandSecurityPledgeUtil.TRADE_KEYWORD_REVERSE_FROM);
                    if (!Util.isEmpty(reverseFrom)) {
                        msg.setStringProperty(MESSAGE_PROPERTY_REVERSE_FROM, reverseFrom);
                    }
                }
                // Poland Security Pledge - End

                boolean external = getTripartyExternal(trade);
                // Madrid
                if (isTriparty(trade) && TOPIC_MADRID.equalsIgnoreCase(getTopicAttribute(boMessage))) {
                    if (external) {
                        addExternalMessageProperties(msg, trade);
                    } else {
                        addMessageProperties(msg, trade);
                    }
                    // Madrid - End
                    // SCF
                } else if (isTriparty(trade) && TOPIC_SCF.equalsIgnoreCase(getTopicAttribute(boMessage))) {
                    if (external) {
                        addExternalMessageProperties(msg, trade);
                    } else {
                        addSCFMessageProperties(msg, trade);
                    }
                    // SCF - End
                }

            } catch (RemoteException exc) {
                Log.error(KondorPlusJMSQueueIEAdapter.class.getName(), exc);
            }
            Log.system("QueueAdapter", "Publishing msg Text:" + msg);

            // Add Topic attributes to BoMessage
            setBoMessageTopicAtt(msg, boMessage);
            //V6.6
            //publishWithDelay(msg);
            this.publisher.publish(msg);
        } catch (final JMSException exc) {
            Log.error("QueueAdapter", "Unable to send Msg", exc);
            onException(new JMSException("Disconnected from Message Adapter"));
            return false;
        } // end of try-catch
        return true;
    }


    /**
     * @param boMessage
     * @return
     */
    protected String getTopicAttribute(final BOMessage boMessage) {

        String rst = "";
        try {
            final Trade trade =
                    DSConnection.getDefault().getRemoteTrade().getTrade(boMessage.getTradeLongId());
            if (trade != null) {
                Vector<LegalEntityAttribute> attrs =
                        BOCache.getLegalEntityAttributes(
                                DSConnection.getDefault(), trade.getBook().getLegalEntity().getId());
                if (!Util.isEmpty(attrs)) {
                    for (LegalEntityAttribute legalEntityAttribute : attrs) {
                        if (TOPIC.equals(legalEntityAttribute.getAttributeType())) {
                            return legalEntityAttribute.getAttributeValue();
                        }
                    }
                }
            }
        } catch (final RemoteException e) {
            Log.error(
                    this,
                    "Error getting the PO attribute " + TOPIC + " from message:" + boMessage.getLongId(),
                    e);
        }
        return rst;
    }

    /**
     * @param trade
     * @return true if the trade has keyword FromTripartyAllocation with value true, false otherwise
     */
    public boolean isTriparty(final Trade trade) {
        String fromTripartyAllocation =
                trade.getKeywordValue(SantTradeKeywordUtil.FROM_TRIPARTY_ALLOCATION);
        if (!Util.isEmpty(fromTripartyAllocation)) {
            return Boolean.valueOf(fromTripartyAllocation);
        }
        return false;
    }

    public void addMessageProperties(TextMessage msg, final Trade trade) {
        try {
            addDestinationFolder(msg, trade);
            addRun(msg, trade);
            addFromtripartyAllocation(msg, trade);
            addReversedAllocatonTrade(msg, trade);
        } catch (JMSException ex) {
            Log.error(ex, ex.getMessage());
        }
    }

    public void addExternalMessageProperties(TextMessage msg, final Trade trade) {
        try {
            addExternal(msg);
            addRun(msg, trade);
            addReversedAllocatonTrade(msg, trade);
        } catch (JMSException ex) {
            Log.error(ex, ex.getMessage());
        }
    }

    public void addSCFMessageProperties(TextMessage msg, final Trade trade) {
        try {
            addSCFDestinationFolder(msg, trade);
            addRun(msg, trade);
            addFromtripartyAllocation(msg, trade);
            addReversedAllocatonTrade(msg, trade);
        } catch (JMSException ex) {
            Log.error(ex, ex.getMessage());
        }
    }

    /**
     * @param trade
     * @return Additional Field - TRIPARTY_EXTERNAL
     */
    public boolean getTripartyExternal(Trade trade) {
        Integer contractid = null;
        if (trade.getProduct() instanceof MarginCall) {
            contractid = ((MarginCall) trade.getProduct()).getMarginCallId();
        }
        try {
            if (contractid != null) {
                CollateralConfig contract =
                        CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractid);

                if (contract != null
                        && !Util.isEmpty(contract.getAdditionalField(AD_EXTERNAL))
                        && contract.getAdditionalField(AD_EXTERNAL).toLowerCase().equals("true")) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.error(this, "Cannot load contract: " + e);
        }

        return false;
    }

    private void addExternal(TextMessage msg) throws JMSException {
        msg.setBooleanProperty(TP_EXTERNAL, true);
    }

    public void addDestinationFolder(TextMessage msg, final Trade trade) throws JMSException {
        List<String> domValues =
                CollateralUtilities.getDomainValues(SantDomainValuesUtil.MT569_DESTINATION_FOLDER_MAPPING);
        String account = trade.getKeywordValue(SantTradeKeywordUtil.COLLATERAL_GIVER);

        if (!Util.isEmpty(account) && domValues.contains(account)) {
            msg.setStringProperty(DESTINATION_FOLDER, getAcBook(account));
        } else if (trade.getProduct() instanceof MarginCall
                && (((MarginCall) trade.getProduct()).getSecurity() != null)) {

            if (((MarginCall) trade.getProduct()).getSecurity() instanceof Bond
                    && domValues.contains(PREFIX_BOND_ACC + account)) {
                msg.setStringProperty(DESTINATION_FOLDER, getAcBook(PREFIX_BOND_ACC + account));
            } else if (((MarginCall) trade.getProduct()).getSecurity() instanceof Equity
                    && domValues.contains(PREFIX_EQUITY_ACC + account)) {
                msg.setStringProperty(DESTINATION_FOLDER, getAcBook(PREFIX_EQUITY_ACC + account));
            }
        }
    }

    public void addSCFDestinationFolder(TextMessage msg, final Trade trade) throws JMSException {
        List<String> domValues =
                CollateralUtilities.getDomainValues(SantDomainValuesUtil.SCF_DESTINATION_FOLDER_MAPPING);
        String account = trade.getKeywordValue(SantTradeKeywordUtil.COLLATERAL_GIVER);

        if (!Util.isEmpty(account) && domValues.contains(account)) {
            msg.setStringProperty(DESTINATION_FOLDER, getSCFAcBook(account));
        } else if (trade.getProduct() instanceof MarginCall
                && (((MarginCall) trade.getProduct()).getSecurity() != null)) {

            if (((MarginCall) trade.getProduct()).getSecurity() instanceof Bond
                    && domValues.contains(PREFIX_BOND_ACC + account)) {
                msg.setStringProperty(DESTINATION_FOLDER, getSCFAcBook(PREFIX_BOND_ACC + account));
            } else if (((MarginCall) trade.getProduct()).getSecurity() instanceof Equity
                    && domValues.contains(PREFIX_EQUITY_ACC + account)) {
                msg.setStringProperty(DESTINATION_FOLDER, getSCFAcBook(PREFIX_EQUITY_ACC + account));
            }
        }
    }

    public void addRun(TextMessage msg, final Trade trade) throws JMSException {
        String statementFrequency =
                trade.getKeywordValue(SantTradeKeywordUtil.STATEMENT_FREQUENCY_INDICATOR);
        if (!Util.isEmpty(statementFrequency) && DAIL.equalsIgnoreCase(statementFrequency)) {
            msg.setStringProperty(RUN, EOD);
        } else {
            String statementNumber = trade.getKeywordValue(SantTradeKeywordUtil.STATEMENT_NUMBER);
            if (!Util.isEmpty(statementNumber)) {
                msg.setStringProperty(RUN, statementNumber);
            }
        }
    }

    public void addFromtripartyAllocation(TextMessage msg, final Trade trade) throws JMSException {
        msg.setStringProperty(SantTradeKeywordUtil.FROM_TRIPARTY_ALLOCATION, "true");
    }

    public void addReversedAllocatonTrade(TextMessage msg, final Trade trade) throws JMSException {
        String reversedTrade = trade.getKeywordValue(SantTradeKeywordUtil.REVERSED_ALLOCATION_TRADE);
        if (!Util.isEmpty(reversedTrade)) {
            if (!Util.isEmpty(trade.getKeywordValue(KEY_MUREX))) {
                msg.setStringProperty(
                        SantTradeKeywordUtil.REVERSED_ALLOCATION_TRADE, trade.getKeywordValue(KEY_MUREX));
            } else {
                msg.setStringProperty(SantTradeKeywordUtil.REVERSED_ALLOCATION_TRADE, reversedTrade);
            }
        } else {
            msg.setStringProperty(SantTradeKeywordUtil.REVERSED_ALLOCATION_TRADE, "");
        }
    }

    private static String getAcBook(String account) {
        String comment =
                CollateralUtilities.getDomainValueComment(
                        SantDomainValuesUtil.MT569_DESTINATION_FOLDER_MAPPING, account);
        return comment;
    }

    private static String getSCFAcBook(String account) {
        String comment =
                CollateralUtilities.getDomainValueComment(
                        SantDomainValuesUtil.SCF_DESTINATION_FOLDER_MAPPING, account);
        return comment;
    }

    protected void mainWrite(TextMessage msg, Trade trade) throws JMSException {

        // adc anadir nueva propiedad para tryparty equity
        if ((trade != null)
                && (trade.getProduct() != null)
                && (trade.getProduct() instanceof MarginCall)
                && (((MarginCall) trade.getProduct()).getSecurity() != null)) {

            if (((MarginCall) trade.getProduct()).getSecurity().getType().equals(PRODUCT_TYPE_EQUITY)) {
                msg.setStringProperty(PRODUCT_TYPE, PRODUCT_TYPE_EQUITY);
            }
        }
    }

    protected void setBoMessageTopicAtt(TextMessage msg, BOMessage boMessage) {
        try {
            List<String> propertyNames = Collections.list(msg.getPropertyNames());
            for (String property : propertyNames) {
                boMessage.setAttribute("IL: " + property, msg.getStringProperty(property));
            }
        } catch (JMSException e) {
            Log.error(this, "Cannot set Topic on BoMessage Attributes: " + e);
        }
    }
}
