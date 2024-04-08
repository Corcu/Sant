/**
 *
 */
package calypsox.tk.util;

import calypsox.apps.reporting.SantPolandSecurityPledgeUtil;
import calypsox.tk.bo.SLBKondorPlusMarginCallMessageFormatter;
import calypsox.util.SantDomainValuesUtil;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.DOMUtility;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/** @author fperezur */
public class SLBKondorPlusJMSQueueIEAdapter extends KondorPlusJMSQueueIEAdapter {

    public SLBKondorPlusJMSQueueIEAdapter(int isReceiver) {
        super(isReceiver);
    }

    private static final String TOPIC = "TOPIC";
    private boolean firtsMovement = false;

    // Poland Security Pledge
    private static final String TOPIC_POLAND = "Poland";
    private static final String MESSAGE_PROPERTY_REVERSE_FROM = "ReverseFrom";
    // Poland Security Pledge - End

    private static final String TOPIC_MADRID = "Madrid";
    private static final String TOPIC_SCF = "SCF";

    // SLB
    private static final String TRADE_KEYWORD_SLB = "SLB";
    private static final String S_TRUE = "true";
    private static final String SLB = "SLB";
    private static final String SLB_MUREX_ID = "contractId";
    private static final String SLB_DEAL_ID = "dealId";
    private static final String SLB_LOT_SIZE = "lotSize";
    private static final String SLB_QUANTITY = "quantity";
    private static final String SLB_ACC_CLOSING = "accountClosing";
    private static final String SLB_ACC_CLOSING_VAL = "accountClosingValue";
    private static final String SLB_ACC_NEW_OPEN_VAL = "newOpenValue";

    // Type of contract
    private static final String SLB_MX_CASH_DEPOSIT_CONTRACT_ID = "DEPOSIT_CONTRACT_ID_";
    private static final String SLB_MX_CASH_LOAN_CONTRACT_ID = "LOAN_CONTRACT_ID_";
    private static final String SLB_MX_CASH_DEPOSIT_DEAL_ID = "DEPOSIT_DEAL_ID_";
    private static final String SLB_MX_CASH_LOAN_DEAL_ID = "LOAN_DEAL_ID_";

    // Trade
    private static final String ISIN = "ISIN";
    private static final String MUREX_ID = "MxID";
    private static final String DEAL_ID = "DealID";
    private static final String LOT_SIZE = "LotSize";

    // Type of Event
    private static final String VERIFIED_TRADE = "VERIFIED_TRADE";

    // Body field
    private String tomadaPrestada;

    // Additional Field
    private static final String SLB_CIRCUIT_PO = "SLB_CIRCUIT_PO";

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.util.JMSTopicIEAdapter#write(java.lang.String)
     */
    @Override
    public boolean write(final String message, final BOMessage boMessage) {
        try {

            final TextMessage msg = this.session.createTextMessage();
            parseXml(message);

            String topicValue = getTopic(boMessage);

            msg.setText(message);
            msg.setStringProperty(TOPIC, topicValue);
            Trade trade = null;

            try {
                trade = DSConnection.getDefault().getRemoteTrade().getTrade(boMessage.getTradeLongId());

                super.mainWrite(msg, trade);

                // SLB - add new property
                if (trade != null && !Util.isEmpty(trade.getKeywordValue(TRADE_KEYWORD_SLB))) {
                    final String kwSlb = trade.getKeywordValue(TRADE_KEYWORD_SLB);
                    if (S_TRUE.equalsIgnoreCase(kwSlb)) {
                        msg.setBooleanProperty(TRADE_KEYWORD_SLB, true);
                    }
                }

                // Poland Security Pledge - add new property
                if (TOPIC_POLAND.equals(topicValue)) {
                    String reverseFrom =
                            trade.getKeywordValue(SantPolandSecurityPledgeUtil.TRADE_KEYWORD_REVERSE_FROM);
                    if (!Util.isEmpty(reverseFrom)) {
                        msg.setStringProperty(MESSAGE_PROPERTY_REVERSE_FROM, reverseFrom);
                    }
                }

                boolean external = super.getTripartyExternal(trade);
                // Madrid
                if (isTriparty(trade) && TOPIC_MADRID.equalsIgnoreCase(topicValue)) {
                    if (external) {
                        addExternalMessageProperties(msg, trade);
                    } else {
                        addMessageProperties(msg, trade);
                    }
                    // Madrid - End
                    // SCF
                } else if (isTriparty(trade) && TOPIC_SCF.equalsIgnoreCase(topicValue)) {
                    if (external) {
                        addExternalMessageProperties(msg, trade);
                    } else {
                        addSCFMessageProperties(msg, trade);
                    }
                    // SCF - End
                }

                addSLBProperties(msg, trade, boMessage);

                if (isTriparty(trade) && SLB.equalsIgnoreCase(topicValue)) {
                    if (external) {
                        addExternalMessageProperties(msg, trade);
                    } else {
                        addMessageProperties(msg, trade);
                    }
                }

            } catch (RemoteException e) {
                Log.error(SLBKondorPlusJMSQueueIEAdapter.class.getName(), e);
                Log.error(SLBKondorPlusJMSQueueIEAdapter.class.getName(), e.getStackTrace().toString());
            }

            Log.system("SLBKondorPlusJMSQueueIEAdapter", "Publishing SLB msg Text:" + msg);

            // Add Topic attributes to BoMessage
            setBoMessageTopicAtt(msg, boMessage);

            this.publisher.publish(msg);
        } catch (final JMSException e) {
            Log.error("SLBKondorPlusJMSQueueIEAdapter", "Unable to send Msg", e);
            onException(new JMSException("Disconnected from Message Adapter"));
            return false;
        } catch (Exception e) {
            Log.error(
                    SLBKondorPlusJMSQueueIEAdapter.class, "Failed to process message: " + e.getMessage(), e);
            return false;
        } // end of try-catch
        return true;
    }

    /**
     * This method add the SLB boolean property to Text Message.
     *
     * @param msg
     * @param trade
     * @throws JMSException
     */
    public void addSLB(TextMessage msg, Trade trade) throws JMSException {
        msg.setBooleanProperty(SLB, true);
    }

    /**
     * This method add the MurexId property to Text Message.
     *
     * @param msg
     * @param trade
     * @param boMessage
     * @throws JMSException
     */
    public void addMurexID(TextMessage msg, Trade trade, BOMessage boMessage) throws JMSException {
        if (trade.getProduct() instanceof MarginCall) {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            String mxContractId = null;
            String mxDealId = null;
            String mxLotSize = null;
            if (marginCall != null) {

                if (boMessage != null && VERIFIED_TRADE.equals(boMessage.getEventType())
                        || marginCall.getSecurity() == null) {

                    if (marginCall.getSecurity() != null) {
                        // Get MxId, DealId and LotSize from Security
                        ArrayList<String> murexValues = getMurexValuesFromSecTrade(trade);
                        if (!Util.isEmpty(murexValues)) {
                            mxContractId = murexValues.get(0);
                            mxDealId = murexValues.get(1);
                            mxLotSize = murexValues.get(2);
                        }
                    } else {
                        // Get MurexId and DealId from Cash
                        ArrayList<String> murexValues = getMurexValuesFromCashTrade(trade);
                        if (!Util.isEmpty(murexValues)) {
                            mxContractId = murexValues.get(0);
                            mxDealId = murexValues.get(1);
                        }
                    }

                } else {
                    mxContractId = trade.getKeywordValue(MUREX_ID);
                    mxDealId = trade.getKeywordValue(DEAL_ID);
                    mxLotSize = trade.getKeywordValue(LOT_SIZE);
                }

                // Easier view of the SLB message headers in Calypso
                if (!Util.isEmpty(mxContractId) && !Util.isEmpty(mxDealId)) {
                    boMessage.setAttribute(SLB_MUREX_ID, mxContractId);
                    boMessage.setAttribute(SLB_DEAL_ID, mxDealId);
                    if (!Util.isEmpty(mxLotSize)) {
                        boMessage.setAttribute(SLB_LOT_SIZE, mxLotSize);
                    }
                }

                msg.setStringProperty(SLB_MUREX_ID, mxContractId);
                msg.setStringProperty(SLB_DEAL_ID, mxDealId);
                if (marginCall.getSecurity() != null) {
                    msg.setStringProperty(SLB_LOT_SIZE, mxLotSize);
                }
            }
        }
    }

    /**
     * This method get the murexId and the dealId from Cash Trades.
     *
     * @param trade
     * @return
     */
    private ArrayList<String> getMurexValuesFromCashTrade(Trade trade) {

        MarginCall marginCall = (MarginCall) trade.getProduct();
        String mxContractId = new String();
        String mxDealId = new String();
        ArrayList<String> values = new ArrayList<>();

        MarginCallConfig marginCallConf = marginCall.getMarginCallConfig();
        String ccy = trade.getTradeCurrency();
        if ("P".equalsIgnoreCase(this.tomadaPrestada)) {
            mxContractId = marginCallConf.getAdditionalField(SLB_MX_CASH_LOAN_CONTRACT_ID + ccy);
            mxDealId = marginCallConf.getAdditionalField(SLB_MX_CASH_LOAN_DEAL_ID + ccy);
        } else if ("T".equalsIgnoreCase(this.tomadaPrestada)) {
            mxContractId = marginCallConf.getAdditionalField(SLB_MX_CASH_DEPOSIT_CONTRACT_ID + ccy);
            mxDealId = marginCallConf.getAdditionalField(SLB_MX_CASH_DEPOSIT_DEAL_ID + ccy);
        }
        if (!Util.isEmpty(mxContractId) && !Util.isEmpty(mxDealId)) {
            values.add(0, mxContractId);
            values.add(1, mxDealId);
        }
        return values;
    }

    /**
     * This method get the murexId,the dealId and the LotSize from Security Trades.
     *
     * @param trade
     * @return String murexId
     */
    private ArrayList<String> getMurexValuesFromSecTrade(Trade trade) {

        String mxContractId = null;
        String mxDealId = null;
        String mxLotSize = null;
        String where = getSecurityWhereClause(trade);
        String orderBy = "trade.trade_id DESC";
        String from =
                "trade , trade currentTrade , product_desc productDescSelection, "
                        + "product_desc currentProductDesc, product_simplexfer psxCurrent, "
                        + "product_simplexfer psxSelection";
        ArrayList<String> values = new ArrayList<>();

        try {
            firtsMovement = false;
            TradeArray tradeArray =
                    DSConnection.getDefault().getRemoteTrade().getTrades(from, where, orderBy, false, null);
            if (!Util.isEmpty(tradeArray)) {
                Trade tr = tradeArray.firstElement();
                if (tr != null) {
                    mxContractId = tr.getKeywordValue(MUREX_ID);
                    mxDealId = tr.getKeywordValue(DEAL_ID);
                    mxLotSize = tr.getKeywordValue(LOT_SIZE);
                    if (!Util.isEmpty(mxContractId) && !Util.isEmpty(mxDealId)) {
                        values.add(0, mxContractId);
                        values.add(1, mxDealId);
                        values.add(2, mxLotSize);
                    }
                }
            } else {
                firtsMovement = true;
            }
        } catch (CalypsoServiceException e) {
            Log.error("CalypsoServiceException", "Unable to get the trades DataServer", e);
            return values;
        }
        return values;
    }

    /**
     * Method to create the where clause for SQL searching.
     *
     * @param trade
     * @return String where clause
     */
    private String getSecurityWhereClause(Trade trade) {

        StringBuilder where = new StringBuilder();
        where.append("trade.product_id = productDescSelection.product_id");
        where.append(" AND productDescSelection.und_security_id = currentProductDesc.und_security_id");
        where.append(" AND psxCurrent.product_id = currentTrade.product_id");
        where.append(" AND currentTrade.trade_id = " + trade.getLongId());
        where.append(" AND currentProductDesc.product_id = currentTrade.product_id");
        where.append(" AND psxCurrent.linked_id = psxSelection.linked_id");
        where.append(" AND psxSelection.product_id = trade.product_id");
        where.append(" AND trade.TRADE_ID <> " + trade.getLongId());

        return where.toString();
    }

    /**
     * This method add the quantity property to Text Message.
     *
     * @param msg
     * @param trade
     * @throws JMSException
     */
    public void addQuantity(TextMessage msg, Trade trade) throws JMSException {

        if (trade != null) {
            double amount = Math.abs(trade.getQuantity());
            String qty = String.valueOf(amount);
            msg.setStringProperty(
                    SLB_QUANTITY, qty.endsWith(".0") ? qty.substring(0, qty.length() - 2) : qty);
        }
    }

    /**
     * This method add the account closing double properties to Text Message.
     *
     * @param msg
     * @param trade
     * @throws JMSException
     */
    public void addAccountClosing(TextMessage msg, final Trade trade, BOMessage boMessage)
            throws JMSException {

        SLBKondorPlusMarginCallMessageFormatter formatter =
                new SLBKondorPlusMarginCallMessageFormatter();
        AccountClosingValues accClosingVals =
                formatter.getAccountClosingValues(
                        boMessage, trade, null, null, null, null, DSConnection.getDefault(), firtsMovement);

        if (accClosingVals != null) {

            msg.setStringProperty(
                    SLB_ACC_CLOSING, String.valueOf(accClosingVals.isAccClosing()).toLowerCase());
            boMessage.setAttribute(
                    SLB_ACC_CLOSING, String.valueOf(accClosingVals.isAccClosing()).toLowerCase());

            if (accClosingVals.isAccClosing()) {
                msg.setStringProperty(SLB_ACC_CLOSING_VAL, String.valueOf(accClosingVals.getClosingVal()));
                msg.setStringProperty(SLB_ACC_NEW_OPEN_VAL, String.valueOf(accClosingVals.getOpenVal()));
                boMessage.setAttribute(SLB_ACC_CLOSING_VAL, String.valueOf(accClosingVals.getClosingVal()));
                boMessage.setAttribute(SLB_ACC_NEW_OPEN_VAL, String.valueOf(accClosingVals.getOpenVal()));
            }
        }
    }

    /**
     * This method add all the new properties to Text Message.
     *
     * @param msg
     * @param trade
     * @param boMessage
     * @throws JMSException
     */
    private void addSLBProperties(TextMessage msg, final Trade trade, BOMessage boMessage)
            throws JMSException {

        addMurexID(msg, trade, boMessage);
        if (trade.getProduct() instanceof MarginCall) {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            if (marginCall != null && marginCall.getSecurity() != null) {
                addAccountClosing(msg, trade, boMessage);
                addQuantity(msg, trade);
            }
        }
    }

    /**
     * Method to read the body of the SLB message
     *
     * @param xmlMessage
     * @throws Exception
     */
    private void parseXml(String xmlMessage) throws Exception {
        ByteArrayInputStream xmlStream = new ByteArrayInputStream(xmlMessage.getBytes());
        Document document = DOMUtility.createDOMDocument(xmlStream);

        Node node1 = document.getFirstChild();
        String prefix = node1.getPrefix();
        if (prefix != null) {
            prefix += ":";
        } else {
            prefix = "";
        }

        NodeList childNodes = node1.getChildNodes();

        for (int j = 0; j < childNodes.getLength(); j++) {
            Node child = childNodes.item(j);
            NodeList childNodesField = child.getChildNodes();
            System.err.println(child.getNodeName() + " = " + child.getTextContent());
            for (int i = 0; i < childNodesField.getLength(); i++) {
                Node childField = childNodesField.item(i);
                System.err.println(childField.getNodeName() + " = " + childField.getTextContent());
                if (childField.getNodeName().equals(prefix + "tomadaPrestada")
                        && !childField.getTextContent().isEmpty()) {
                    this.tomadaPrestada = childField.getTextContent();
                }
            }
        }
    }

    @Override
    public void addDestinationFolder(TextMessage msg, final Trade trade) throws JMSException {
        List<String> domValues =
                CollateralUtilities.getDomainValues(SantDomainValuesUtil.SLB_DESTINATION_FOLDER_MAPPING);
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

    private static String getAcBook(String account) {
        String comment =
                CollateralUtilities.getDomainValueComment(
                        SantDomainValuesUtil.SLB_DESTINATION_FOLDER_MAPPING, account);
        return comment;
    }

    /**
     * @param @{@link BOMessage} message
     * @return
     */
    private String getTopic(BOMessage message) {

        Integer contractid;
        try {
            final Trade trade =
                    DSConnection.getDefault().getRemoteTrade().getTrade(message.getTradeLongId());
            if (trade != null && trade.getProduct() instanceof MarginCall) {
                contractid = Math.toIntExact(((MarginCall) trade.getProduct()).getLinkedLongId());
                CollateralConfig contract =
                        CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractid);

                if (contract != null) {
                    if (!Util.isEmpty(contract.getAdditionalField(SLB_CIRCUIT_PO))) {
                        LegalEntity legalEntity =
                                BOCache.getLegalEntity(
                                        DSConnection.getDefault(), contract.getAdditionalField(SLB_CIRCUIT_PO));

                        if (legalEntity != null && !Util.isEmpty(legalEntity.getLegalEntityAttributes())) {
                            List<LegalEntityAttribute> attrs =
                                    new ArrayList(legalEntity.getLegalEntityAttributes());
                            for (LegalEntityAttribute legalEntityAttribute : attrs) {
                                if (TOPIC.equals(legalEntityAttribute.getAttributeType())) {
                                    return legalEntityAttribute.getAttributeValue();
                                }
                            }
                        }
                    } else {
                        return getTopicAttribute(message);
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(
                    this,
                    "Error getting the PO attribute " + TOPIC + " from message:" + message.getLongId(),
                    e);
        }
        return "";
    }
}
