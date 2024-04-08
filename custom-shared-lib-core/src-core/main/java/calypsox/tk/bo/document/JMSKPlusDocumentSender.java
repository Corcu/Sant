package calypsox.tk.bo.document;

import calypsox.tk.util.SantanderIEAdapter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;

import java.util.Vector;

public class JMSKPlusDocumentSender extends TibcoDocumentSender {

    public static final String ADAPTER = "mcliquidation.out";
    public static final String CONFIG_FILE_NAME = "mcliquidation.connection.properties";

    public static final String CONFIG_NAME_KONDOR_PLUS = "SantanderKondorPlus";

    public JMSKPlusDocumentSender() {
        super(ADAPTER);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean send(DSConnection ds, SenderConfig config, SenderCopyConfig copyConfig, long eventId,
                        AdviceDocument document, Vector copies, BOMessage message, Vector errors, String engineName, boolean[] saved) {

        Log.info(this, "Sending entry - adpaterType:" + this.adapterType);

        boolean result = false;
        if (this.adapterType == null) {
            Log.error(this, "Implementation must specify a valid adapter type");
            final String error = ("Message can not be sent; Exception in gateway " + this.adapterType) != null ? this.adapterType
                    : "" + " " + "adapterType cannot be null";

            errors.addElement(error);
        } else {
            StringBuffer output = null;
            try {
                if (getAdapter() == null) {
                    createAdapter();
                }

                if (!getAdapter().isOnline()) {
                    getAdapter().init();
                }

                output = document.getDocument();

                boolean success = false;
                if (this.adapter instanceof SantanderIEAdapter) {
                    success = ((SantanderIEAdapter) this.adapter).write(output.toString(), message);
                } else {
                    success = this.adapter.write(output.toString());
                }

                if (success) {
                    saveMessage(ds, message);
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

                // We get the Margin Call related to the Trade.ActiveMQConnectionFactory
                final MarginCall marginCall = (MarginCall) trade.getProduct();

                // We call the three methods to check if we have to send any
                // message by email or not, depending on the conditions.
                handleMCTEnteredAfterCutoff(trade, ds, marginCall);
                handlePositionMovementSignChange(trade, ds, marginCall);
                handleMCTCancelled(trade, ds, marginCall);
            } catch (final Exception e) {
                Log.error(JMSKPlusDocumentSender.class.getName(), "Exception occurred while sending:\n" + e);
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
        Log.info(JMSKPlusDocumentSender.class.getName(), "Sending finished with result=" + result + " MessageId: "
                + message.getAllocatedLongSeed());

        return result;
    }

    @Override
    public String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    @Override
    public String getConfigName() {
        return CONFIG_NAME_KONDOR_PLUS;
    }


    /**
     * Checks if the action is applicable and saves the message
     *
     * @param ds
     * @param message
     */
    @SuppressWarnings("unchecked")
    private void saveMessage(DSConnection ds, BOMessage message) {
        Vector<String> att = message.getAttributes();
        final BOMessage msg = getBOMessageFromDB(ds, message);
        if (msg != null) {
            msg.setAttributes(att);
            if (isBOMessageActionApplicable(ds, msg, Action.SEND)) {
                msg.setAction(Action.SEND);
                msg.setEnteredUser(DSConnection.getDefault().getUser());
                try {
                    DSConnection.getDefault().getRemoteBO().save(msg, 0, null);
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Could not save message with messageId=" + msg.getLongId());
                }
            } else {
                Log.error(this, "Could not apply action " + Action.S_SEND + " to message with id: " + msg.getLongId());
            }
        }
    }


    private BOMessage getBOMessageFromDB(DSConnection ds, BOMessage message) {
        BOMessage dbMessage = null;
        try {
            dbMessage = ds.getRemoteBackOffice().getMessage(message.getLongId());
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not get message with id=" + message.getLongId());
        }
        return dbMessage;
    }

    /**
     * Checks if the BO message action is applicable.
     *
     * @param transfer the trade
     * @return true if sucess, false otherwise
     */
    protected boolean isBOMessageActionApplicable(DSConnection ds, final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, ds, null);
    }
}
