package calypsox.tk.bo.workflow.rule;


import calypsox.tk.bo.util.PaymentsHubUtil;
import calypsox.tk.core.KeywordConstantsUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.rule.UpdateMessageMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import java.rmi.RemoteException;
import java.util.Vector;


public class SantUpdateCoverMessagePaymentsHubMessageRule extends UpdateMessageMessageRule {


    /**
     * Description of the rule
     */
    public static final String DESCRIPTION = "Updates attributes and applies action to the Transfer.";

    /** Rule Name. */
    private static final String RULE_NAME = "SantUpdateCoverMessagePaymentsHub";

    /** The Constant MESSAGE_PH_FICCT. */
    public static final String MESSAGE_PH_FICCT = "PH-FICCT";

    /** The Constant MESSAGE_ATTRIBUTE_COVER_MESSAGE. */
    public static final String MSG_ATTRIBUTE_COVER_MESSAGE = "HasCoverMessage";

    /** The Constant MESSAGE_PH_FICTCOV. */
    public static final String MESSAGE_PH_FICTCOV = "PH-FICTCOV";


    // Constructor
    public SantUpdateCoverMessagePaymentsHubMessageRule() {
        super();
        ruleName = RULE_NAME;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(final TaskWorkflowConfig wc, final BOMessage message, final BOMessage oldMessage,
                         final Trade trade, final BOTransfer transfer, final Vector messages, final DSConnection dsCon,
                         final Vector excps, final Task task, final Object dbCon, final Vector events) {
        boolean rst = false;
        // Check if BOMessage sent to PaymentsHub
        if (PaymentsHubUtil.isSentToPaymentsHub(message)) {
            // Check if BOMessage is a PH-FICCT Message and has fill the 'HasCoverMessage' attribute.
            if (hasCoverMessage(message)) {
                rst = super.check(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
            } else {
                // No Cover Message to update
                rst = true;
            }
        } else {
            // Message did not send to PaymentsHub. Nothing to do.
            rst = true;
        }
        return rst;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(final TaskWorkflowConfig wc, final BOMessage message, final BOMessage oldMessage,
                          final Trade trade, final BOTransfer transfer, final Vector messages, final DSConnection dsCon,
                          final Vector excps, final Task task, final Object dbCon, final Vector events) {
        boolean rst = false;
        // Check if BOMessage sent to PaymentsHub
        if (PaymentsHubUtil.isSentToPaymentsHub(message)) {
            // Check if BOMessage is a PH-FICCT Message and has fill the 'HasCoverMessage' attribute.
            if (hasCoverMessage(message)) {
                rst = super.update(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
            } else {
                // No Cover Message to update
                rst = true;
            }
        } else {
            // Message did not send to PaymentsHub. Nothing to do.
            rst = true;
        }
        return rst;
    }


    /**
     * Check if the BOMessage has a Cover Message.
     *
     * @param message
     * @return
     */
    protected static boolean hasCoverMessage(final BOMessage message) {
        boolean hasCoverMessage = false;
        if (message != null) {
            // Check if BOMessage is a PH-FICCT Message and has fill the 'HasCoverMessage' attribute.
            final boolean isPHFICCT = MESSAGE_PH_FICCT.equals(message.getTemplateName());
            final boolean hasCoverMessageAttr = Util.isTrue(message
                    .getAttribute(MSG_ATTRIBUTE_COVER_MESSAGE));
            hasCoverMessage = isPHFICCT && hasCoverMessageAttr;
        }
        return hasCoverMessage;
    }


    /**
     *
     * Get BOMessage PH-FICTCOV using linked TransferId.
     *
     * @param wc
     * @param message
     * @param oldMessage
     * @param trade
     * @param transfer
     * @param messages
     * @param dsCon
     * @param excps
     * @param task
     * @param dbCon
     * @param events
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected BOMessage getMessage(final TaskWorkflowConfig wc, final BOMessage message, final BOMessage oldMessage,
                                   final Trade trade, final BOTransfer transfer, final Vector messages, final DSConnection dsCon,
                                   final Vector excps, final Task task, final Object dbCon, final Vector events) {
        BOMessage coverMessage = null;
        if (message == null) {
            return coverMessage;
        }
        // Get the related BOMessage PH-FICTCOV through the transfer.
        // From the DataBase, retrieves the messages PH-FICTCOV in the same transfer
        // that the PH-FICCT message.
        try {
            // Get 'MessageRef' attribute
            final String msgRefAttr = message.getAttribute(KeywordConstantsUtil.MSG_ATTRIBUTE_MESSAGE_REF);
            if (!Util.isEmpty(msgRefAttr)) {
                // Get BOMessage directly
                final BOMessage msgRef = getBOMessage(Long.parseLong(msgRefAttr));
                if (msgRef != null) {
                    coverMessage = (BOMessage) msgRef.clone();
                }
            }
            if (coverMessage == null && transfer != null) {
                // From the DataBase, retrieves the messages PH-FICTCOV in the same transfer that the
                // PH-FICCT message.
                final MessageArray messageArray = getMessageArray(transfer.getLongId(), dsCon);
                if ((null != messageArray) && (messageArray.size() > 0)) {
                    final BOMessage[] msgArray = messageArray.getMessages();
                    for (int i = 0; (i < msgArray.length) && (null != msgArray[i]); i++) {
                        // Checks if the type message = PH-FICTCOV.
                        final String templateNameMsg = msgArray[i].getTemplateName();
                        if (MESSAGE_PH_FICTCOV.equals(templateNameMsg)) {
                            // Get BOMessage
                            coverMessage = (BOMessage) msgArray[i].clone();
                        }
                    }
                }
            }
        } catch (final CloneNotSupportedException e) {
            Log.error(SantUpdateCoverMessagePaymentsHubMessageRule.class, "Error cloning BOMessage.", e);
        } catch (final RemoteException e) {
            Log.error(SantUpdateCoverMessagePaymentsHubMessageRule.class, "Error retrieving messages from the DataBase with the transfer ID "
                    + (transfer != null ? transfer.getLongId() : 0), e);
        }
        return coverMessage;
    }


    /**
     * Get Action to apply to the Cover Message.
     *
     * It should be the same action as the action applied to the original BOMessage.
     *
     * @param wc
     * @param message
     * @param oldMessage
     * @param selectedMessage
     * @param trade
     * @param transfer
     * @param messages
     * @param dsCon
     * @param excps
     * @param task
     * @param dbCon
     * @param events
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected Action getUpdateMessageAction(final TaskWorkflowConfig wc, final BOMessage message,
                                            final BOMessage oldMessage, final BOMessage selectedMessage, final Trade trade, final BOTransfer transfer,
                                            final Vector messages, final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon,
                                            final Vector events) {
        Action action = this.getAction(dsCon, wc, ruleName);
        if (action == null) {
            action = message.getAction();
        }
        return action;
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected boolean handleMissingMessage(final TaskWorkflowConfig wc, final BOMessage message,
                                           final BOMessage oldMessage, final Trade trade, final BOTransfer transfer, final Vector messages,
                                           final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon, final Vector events) {
        final String debug = String.format("Cover Message not found for BOMessage [%s].", message.getLongId());
        if (messages != null) {
            messages.add(debug);
        }
        return true;
    }


    @Override
    public String getDescription() {
        return DESCRIPTION;
    }


    protected BOMessage getBOMessage(final long messageId) {
        BOMessage message = null;
        if(messageId > 0){
            try {
                message = DSConnection.getDefault().getRemoteBackOffice().getMessage(messageId);
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass().getName(), "Error getting BOMessage.", e);
            }
        }
        return message;
    }


    protected MessageArray getMessageArray(final long xferId, final DSConnection dsCon) throws CalypsoServiceException {
        return (xferId > 0) ? dsCon.getRemoteBO().getTransferMessages(xferId) : null;
    }


}
