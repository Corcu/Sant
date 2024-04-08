/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: José David Sevillano Carretero (josedavid.sevillano@siag.es)
 * All rights reserved.
 *
 */
package calypsox.tk.bo.document;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.KondorPlusUtilities;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.DocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;

public abstract class TibcoDocumentSender implements DocumentSender {

    private static final String KONDOR_GROUP_LIST_SIGNBALANCE = "KONDOR_GROUP_LIST_SIGNBALANCE";
    private static final String KONDOR_GROUP_LIST_CANCELMC = "KONDOR_GROUP_LIST_CANCELMC";
    private static final String KONDOR_GROUP_LIST_CUTOFF = "KONDOR_GROUP_LIST_CUTOFF";
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
    public static final String ADAPTER_TYPE = "ADAPTER_TYPE";
    public static final String ADAPTER_CONFIG = "TibcoQueue";

    private final Object mutex = new Object();

    protected IEAdapter adapter = null;

    protected String adapterType = null;

    /**
     * @param adapterType
     */
    public TibcoDocumentSender(final String adapterType) {
        super();
        this.adapterType = adapterType;
    }

    public abstract String getConfigFileName();

    @Override
    public boolean isOnline() {
        return true;
    }

    /*
     * It is synchronized as we want only one instance of the Adapter created.
     */
    public synchronized void createAdapter() throws ConnectException {

        if (this.adapter != null) {
            return;
        }

        Properties p = Defaults.getProperties();
        if (p == null) {
            p = new Properties();
        }

        p.put(ADAPTER_TYPE, this.adapterType);
        Defaults.setProperties(p);
        IEAdapterConfig _config = null;

        @SuppressWarnings("unused")
        StringBuffer output = null;

        if (_config == null) {
            _config = IEAdapter.getConfig(getConfigName());
        }
        if ((_config == null) || !_config.isConfigured(getConfigFileName())) {
            Log.error(this + this.adapterType, "TibcoDocumentSender.send() - " + getConfigName()
                    + " not configured properly ");
            final String error = ("Message can not be Sent; Exception in gateway " + this.adapterType) != null ? this.adapterType
                    : "" + " " + " not configured properly";
            Log.error(TibcoDocumentSender.class, error);
        } else {
            this.adapter = _config.getSenderIEAdapter();

            if (!getAdapter().isOnline()) {
                synchronized (this.mutex) {
                    getAdapter().init();
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean send(final DSConnection ds, final SenderConfig config, final SenderCopyConfig copyConfig,
                        final long eventId, final AdviceDocument document, final Vector copies, final BOMessage message,
                        final Vector errors, final java.lang.String engineName, final boolean[] saved) {

        Log.debug(this + this.adapterType, "TibcoDocumentSender.send() - entry");

        boolean result = false;
        if (this.adapterType == null) {
            Log.error(this, "TibcoDocumentSender.send() - Implementation must specify a valid adapter type");
            final String error = ("Message can not be Sent; Exception in gateway " + this.adapterType) != null ? this.adapterType
                    : "" + " " + "adapterType cannot be null";

            errors.addElement(error);
        } else {

            @SuppressWarnings("unused")
            IEAdapterConfig _config = null;
            // final String _configName = ADAPTER_CONFIG;
            StringBuffer output = null;

            try {
                if (getAdapter() == null) {
                    createAdapter();
                }

                if (!getAdapter().isOnline()) {
                    getAdapter().init();
                }

                output = document.getDocument();

                final BOMessage msg = (BOMessage) message.clone();
                msg.setAction(Action.SEND);
                msg.setEnteredUser(DSConnection.getDefault().getUser());

                final boolean success = this.adapter.write(output.toString());

                if (success) {
                    DSConnection.getDefault().getRemoteBO().save(msg, 0, null);
                }

                if (this.adapter.getTransactionEnabled()) {
                    this.adapter.commit();
                }

                result = success;

                // We get the trade from the message.
                final Trade trade = ds.getRemoteTrade().getTrade(message.getTradeLongId());

                if (!(trade.getProduct() instanceof MarginCall)) {
                    return result;
                }

                // We get the Margin Call related to the Trade.
                final MarginCall marginCall = (MarginCall) trade.getProduct();
                // We call the three methods to check if we have to send any
                // message by email or not, depending on the conditions.
                handleMCTEnteredAfterCutoff(trade, ds, marginCall);
                handlePositionMovementSignChange(trade, ds, marginCall);
                handleMCTCancelled(trade, ds, marginCall);
            } catch (final Exception e) {
                Log.error(this + this.adapterType, "TibcoDocumentSender.send() - Exception:\n" + e);
                String error = "Message can not be Sent; Exception in gateway "
                        + (this.adapterType != null ? this.adapterType : "") + ": " + e.getMessage();

                errors.addElement(error);
                try {
                    if (this.adapter.getTransactionEnabled()) {
                        this.adapter.rollback();
                    }
                } catch (ConnectException e1) {
                    Log.error(this, "Error during queue session rollback() for config " + this.adapter.getConfigName(),
                            e);
                }
            }

        }
        Log.debug(this + this.adapterType, "TibcoDocumentSender.send() - finished with result=" + result
                + " MessageId: " + message.getAllocatedLongSeed());
        // Save the task

        return result;
    }

    public String getAdapterType() {
        return this.adapterType;
    }

    public void setAdapterType(final String adapterType) {
        this.adapterType = adapterType;
    }

    public IEAdapter getAdapter() {
        return this.adapter;
    }

    /**
     * This method checks if the sign for the balance has changed or not, from positive to negative or viceversa.
     *
     * @param trade Margin Call Trade saved in the system.
     * @param conn  Connection with the Database.
     */
    public void handlePositionMovementSignChange(final Trade trade, final DSConnection conn, final MarginCall marginCall) {
        double tradeAmount = 0.0;
        final String subject = "[Kondor+ Interface] WARNING: Sign of the balance changed.";

        // We call the static method specified in the KondorPlusUtilities class.
        final boolean hasChangedSign = KondorPlusUtilities.hasChangedSignBalance(trade, conn, true);
        // If the result is TRUE, means that the sign has changed, so we send
        // the email to the BOUsers.
        if (hasChangedSign) {
            final String textBody = "Margin Call Trade with id " + trade.getLongId()
                    + " has changed the sign of the balance.";
            if (null != marginCall.getSecurity()) {
                tradeAmount = trade.getQuantity() * marginCall.getPrincipal();
            } else {
                tradeAmount = marginCall.getPrincipal();
            }
            CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                    marginCall.getMarginCallId());
            generateMessageToSendByEmail(subject, textBody, KONDOR_GROUP_LIST_SIGNBALANCE, conn, mcc.getAuthName(),
                    trade.getCounterParty().getAuthName(), trade.getBook().getAuthName(), trade.getTradeDate()
                            .getJDate(TimeZone.getDefault()), tradeAmount, trade.getTradeCurrency());
        }
    }

    /**
     * This method checks if the transfer was sent after the cutoff time for the Book related to the Margin Call Trade.
     *
     * @param trade Margin Call Trade saved in the system.
     * @param conn  Connection with the Database.
     */
    public void handleMCTEnteredAfterCutoff(final Trade trade, final DSConnection conn, final MarginCall marginCall) {
        double tradeAmount = 0.0;
        final String subject = "[Kondor+ Interface] WARNING: Margin Call Trade after cutoff.";
        // We get the cutoff time included in the Book for the trade.
        final Book book = trade.getBook();
        if (null != book) {
            final JDatetime cutoffTime = book.getEODTime(JDate.getNow());
            // We get the date for the trade.
            final JDatetime tradeDate = trade.getTradeDate();
            // We compare the two dates, to see when is sent the transfer, and
            // then, decide if we send or not an email.
            if (tradeDate.after(cutoffTime)) {
                final String textBody = "Margin Call Trade with id " + trade.getLongId()
                        + " sent to Kondor+ after the cutoff for the Book " + book.getAuthName();
                if (null != marginCall.getSecurity()) {
                    tradeAmount = trade.getQuantity() * marginCall.getPrincipal();
                } else {
                    tradeAmount = marginCall.getPrincipal();
                }
                CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                        marginCall.getMarginCallId());
                generateMessageToSendByEmail(subject, textBody, KONDOR_GROUP_LIST_CUTOFF, conn, mcc.getAuthName(),
                        trade.getCounterParty().getAuthName(), trade.getBook().getAuthName(), trade.getTradeDate()
                                .getJDate(TimeZone.getDefault()), tradeAmount, trade.getTradeCurrency());
            }
        }
    }

    /**
     * This method checks if the Margin Call Trade was cancelled or not.
     *
     * @param trade Margin Call Trade saved in the system.
     * @param conn  Connection with the Database.
     */
    public void handleMCTCancelled(final Trade trade, final DSConnection conn, final MarginCall marginCall) {
        double tradeAmount = 0.0;
        final String subject = "[Kondor+ Interface] WARNING: Margin Call Trade Cancelled.";
        final Status status = trade.getStatus();
        // We check if the status for the trade is equals or not to CANCELLED.
        if (status.equals(Status.CANCELED)) {
            final String textBody = "Margin Call Trade with id " + trade.getLongId() + " was cancelled.";
            if (null != marginCall.getSecurity()) {
                tradeAmount = trade.getQuantity() * marginCall.getPrincipal();
            } else {
                tradeAmount = marginCall.getPrincipal();
            }
            CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                    marginCall.getMarginCallId());
            generateMessageToSendByEmail(subject, textBody, KONDOR_GROUP_LIST_CANCELMC, conn, mcc.getAuthName(), trade
                            .getCounterParty().getAuthName(), trade.getBook().getAuthName(), trade.getTradeDate().getJDate(TimeZone.getDefault()),
                    tradeAmount, trade.getTradeCurrency());
        }
    }

    /**
     * This method generates the message to send by email to the BOUsers, depending on the group list defined in the
     * DomainValues, and send it, using the EmailSender class.
     *
     * @param subject   String that indicates the subject of the email to send.
     * @param textBody  Body of the message.
     * @param groupList Distribution list with all the email address for the BOUsers.
     * @param conn      Connection with the Database.
     */
    public void generateMessageToSendByEmail(final String subject, String textBody, final String groupList,
                                             final DSConnection conn, final String marginCallName, final String cpty, final String book,
                                             final JDate tradeDate, final double amount, final String currency) {
        List<String> to = null;

        // We get all the email address to send the message, in the group
        // defined.
        try {
            // We add at the end the information about the Trade.
            textBody = textBody + "\n\nAdditional Information:\n\nMargin Call Contract Name:\t" + marginCallName
                    + "\nCounterparty:\t" + cpty + "\nBook:\t" + book + "\nTrade Date:\t" + tradeDate + "\nAmount:\t"
                    + amount + "\nCurrency:\t" + currency;
            to = conn.getRemoteReferenceData().getDomainValues(groupList);
            CollateralUtilities.sendEmail(to, subject, textBody, DEFAULT_FROM_EMAIL);
        } catch (final RemoteException e) {
        }
    }

    public abstract String getConfigName();
}
