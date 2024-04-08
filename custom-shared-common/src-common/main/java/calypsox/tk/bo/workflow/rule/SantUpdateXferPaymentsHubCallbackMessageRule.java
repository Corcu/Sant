package calypsox.tk.bo.workflow.rule;


import calypsox.tk.bo.util.PaymentHubImportUtil;
import calypsox.tk.bo.util.PaymentsHubCallback;
import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.workflow.rule.UpdateTransferMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class SantUpdateXferPaymentsHubCallbackMessageRule extends UpdateTransferMessageRule {


    protected static final PaymentHubImportUtil util = PaymentHubImportUtil.getInstance();

    /**
     * Description of the rule
     */
    private static final String DESCRIPTION = "Updates attributes and applies action to the Transfer.";

    /** Rule Name. */
    private static final String RULE_NAME = "SantUpdateXferPaymentsHubCallback";

    /** The Constant TRANSFER_ATTRIBUTE_PH_SETTLEMENT_STATUS. */
    protected static final String XFER_ATTR_PH_SETTLEMENT_STATUS = "PHSettlementStatus";

    /** The Constant TRANSFER_ATTRIBUTE_PH_CALLBACK_TIMESTAMP. */
    protected static final String XFER_ATTR_PH_CALLBACK_TIMESTAMP = "PHCallbackTimestamp";;

    /** The Constant MESSAGE_ATTRIBUTE_PH_CALLBACK_MESSAGEID. */
    protected static final String MSG_ATTR_PH_CALLBACK_MSGID = "PHCallbackMessageId";


    // Constructor
    public SantUpdateXferPaymentsHubCallbackMessageRule() {
        super();
        ruleName = RULE_NAME;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        boolean rst = false;
        // Check if BOMessage sent to PaymentsHub
        if (PaymentsHubUtil.isSentToPaymentsHub(message)) {
            rst = super.check(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
        } else {
            // Message did not send to PaymentsHub. Nothing to do.
            rst = true;
        }
        return rst;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                          BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        boolean rst = false;
        // Check if BOMessage sent to PaymentsHub
        if (PaymentsHubUtil.isSentToPaymentsHub(message)) {
            rst = super.update(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
        } else {
            // Message did not send to PaymentsHub. Nothing to do.
            rst = true;
        }
        return rst;
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected boolean updateTransfer(final TaskWorkflowConfig wc, final BOMessage message, final BOMessage oldMessage,
                                     final Trade trade, final BOTransfer selectedTransfer, final BOTransfer transfer, final Vector messages,
                                     final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon, final Vector events) {
        boolean isXferUpdated = false;
        // Get Map Attribute Name/Value
        final Map<String, String> map = getAttributeToKeywordMap(wc, message, oldMessage, trade, selectedTransfer, transfer, messages, dsCon, excps, task, dbCon, events);
        // if not empty, update transfer
        if (!Util.isEmpty(map)) {
            map.forEach((attrName, attrValue) -> selectedTransfer.setAttribute(attrName, attrValue));
            isXferUpdated = true;
        }
        return isXferUpdated;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public Map<String, String> getAttributeToKeywordMap(final TaskWorkflowConfig wc, final BOMessage message,
                                                        final BOMessage oldMessage, final Trade trade, final BOTransfer selectedTransfer, final BOTransfer transfer,
                                                        final Vector messages, final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon,
                                                        final Vector events) {
        String debug = "";
        // Get BOTransfer Attributes to Update.
        Map<String, String> mapXferAttributes = this.getMapping(dsCon, wc, ruleName);
        if (Util.isEmpty(mapXferAttributes)) {
            mapXferAttributes = new HashMap<String, String>(2);
        }
        // Get JSON text
        final String text = getPaymentsHubCallbackText(message);
        if (!Util.isEmpty(text)) {
            // Parse JSON text to Java object
            final PaymentsHubCallback paymentsHubCallback = PaymentsHubCallback.parseText(text);
            if (paymentsHubCallback != null) {
                // Add PHSettlementStatus attribute value
                addPHSettlementStatusAttribute(mapXferAttributes, paymentsHubCallback);
                // Add PHCallbackTimestamp attribute value
                addPHCallbackTimestampAttribute(mapXferAttributes, paymentsHubCallback);
            } else {
                debug = String.format("Could not parse Callback Text [%s] in Java Object.", text);
                Log.debug(SantUpdateXferPaymentsHubCallbackMessageRule.class, debug);
            }
        } else {
            debug = String.format("The Callback Text is empty or null [%s]", text);
            Log.debug(SantUpdateXferPaymentsHubCallbackMessageRule.class, debug);
        }
        return mapXferAttributes;
    }


    /**
     * Add 'PHSettlementStatus' attribute.
     *
     * @param mapXferAttributes
     * @param paymentsHubCallback
     */
    private static void addPHSettlementStatusAttribute(final Map<String, String> mapXferAttributes,
                                                       final PaymentsHubCallback paymentsHubCallback) {
        if (!mapXferAttributes.containsKey(XFER_ATTR_PH_SETTLEMENT_STATUS)) {
            // Get data from input JSON
            final String status = paymentsHubCallback.getStatus();
            final String commStatus = paymentsHubCallback.getCommunicationStatus();
            if (status != null && commStatus != null) {
                // Get 'PHSettlementStatus' value.
                final String phSettlementStatusValue = util.getPHSettlementStatusAttributeValue(status, commStatus);
                // Add attribute
                mapXferAttributes.put(XFER_ATTR_PH_SETTLEMENT_STATUS, phSettlementStatusValue);
            } else {
                final String msg = String.format(
                        "The Status [%s] and/or the CommunicationStatus [%s] retrieved from Input JSON are incorrect.", status, commStatus);
                Log.debug(SantUpdateXferPaymentsHubCallbackMessageRule.class, msg);
            }
        }
    }


    /**
     * Add 'PHCallbackTimestamp' attribute.
     *
     * @param mapXferAttributes
     * @param paymentsHubCallback
     */
    private static void addPHCallbackTimestampAttribute(final Map<String, String> mapXferAttributes, final PaymentsHubCallback paymentsHubCallback) {
        if (!mapXferAttributes.containsKey(XFER_ATTR_PH_CALLBACK_TIMESTAMP)) {
            // Get data from input JSON
            final String timestamp = paymentsHubCallback.getTimestamp();
            if (timestamp != null) {
                // Add attribute
                mapXferAttributes.put(XFER_ATTR_PH_CALLBACK_TIMESTAMP, timestamp);
            } else {
                final String msg = String.format("The Timestamp [%s] retrieved from Input JSON is incorrect.", timestamp);
                Log.debug(SantUpdateXferPaymentsHubCallbackMessageRule.class, msg);
            }
        }
    }


    /**
     * Get PaymentsHub Callback JSON text.
     *
     * @param  boMessage
     * @return
     */
    private String getPaymentsHubCallbackText(final BOMessage boMessage) {
        String text = "";
        String debug = "";
        AdviceDocument document = null;
        // Get Input BOMessage
        final BOMessage messageInput = getInputPaymentsHubBOMessage(boMessage);
        // Get AdviceDocument
        if (messageInput != null) {
            // Get the AdviceDocument with the JSON text received from PaymentsHub.
            try {
                document = DSConnection.getDefault().getRemoteBO().getLatestAdviceDocument(messageInput.getLongId(), null);
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass().getName(), "Error getting latest Advice Doument.", e);
            }
        } else {
            debug = "Nothing to process: input BOMessage from PaymensHub does not exists.";
            Log.debug(this, debug);
        }
        if (document != null) {
            // Parse JSON document to Java object
            text = document.getTextDocument().toString();
        }
        return text;
    }


    /**
     * Get BOMessage Input from attribute 'PHCallbackMessageId'.
     *
     * @param  boMessage
     * @return
     */
    private BOMessage getInputPaymentsHubBOMessage(final BOMessage boMessage) {
        String debug = "";
        BOMessage messageInput = null;
        // Get BOMessage Input from attribute 'PHCallbackMessageId'.
        final String phCallbackMsgInputId = boMessage.getAttribute(MSG_ATTR_PH_CALLBACK_MSGID);
        if (!Util.isEmpty(phCallbackMsgInputId)) {
            messageInput = getBOMessage(Long.parseLong(phCallbackMsgInputId));
            if (messageInput == null) {
                debug = String.format("Nothing to process: input BOMessage [%s] from PaymensHub does not exists. ", phCallbackMsgInputId);
                Log.debug(this, debug);
            }
        } else {
            debug = String.format(
                    "Nothing to process: there is not input BOMessage in BOMessage [%s]. Attribute [%s] is empty. ",
                    String.valueOf(boMessage.getLongId()), MSG_ATTR_PH_CALLBACK_MSGID);
            Log.debug(this, debug);
        }
        return messageInput;
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected Action getUpdateTransferAction(final TaskWorkflowConfig wc, final BOMessage message,
                                             final BOMessage oldMessage, final Trade trade, final BOTransfer selectedTransfer, final BOTransfer transfer,
                                             final Vector messages, final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon,
                                             final Vector events) {
        // Get action from Workflow config
        Action xferAction = this.getAction(dsCon, wc, ruleName);
        if (xferAction == null) {
            // Get the Action applied to the message
            final Action msgActionApplied = message.getAction();
            if (Action.ACK.equals(msgActionApplied)) {
                xferAction = Action.SETTLE; //
            }
            else if (Action.NACK.equals(msgActionApplied) && transfer.getStatus().getStatus().equalsIgnoreCase(Status.SETTLED)) {
                xferAction = Action.valueOf("UNSETTLE"); //
            }
            else {
                // By Default
                xferAction = Action.UPDATE;
            }
        }
        return xferAction;
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected boolean checkAttributes(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                                      BOTransfer selectedTransfer, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task,
                                      Object dbCon, Vector events) {
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


}
