package calypsox.tk.bo.workflow.rule;


import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.workflow.rule.UpdateTransferMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import java.util.*;


public class SantUpdateXferNotSentToPaymentsHubMessageRule extends UpdateTransferMessageRule {


    /** Description of the rule*/
    private static final String DESCRIPTION = "Applies action SETTLE or action UPDATE to the related xfer and add the xfer attribute:\nUnsettled\nXfers will reach"
                    + "SETTLED status only if all related payment msgs are in relevant status.";
    /** Rule Name. */
    private static final String RULE_NAME = "SantUpdateXferNotSentToPaymentsHub";
    private static final String MESSAGE = "The status of the message for the same transfer is not a relevant Status, so it doesn't apply SETTLE action.";
    public static final String DN_RELEVANT_MSG_STATUS_SETTLE_XFER = "relevantMsgStatusToSettleXfer";


    // Constructor
    public SantUpdateXferNotSentToPaymentsHubMessageRule() {
        super();
        ruleName = RULE_NAME;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                         BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        boolean rst = false;
        // Check if BOMessage was not send to PaymentsHub
        if (!PaymentsHubUtil.isSentToPaymentsHub(message)) {
            rst = super.check(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
        } else {
            // Message sent to PaymentsHub. Nothing to do.
            rst = true;
        }
        return rst;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
                          BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        boolean rst = false;
        // Check if BOMessage was not send to PaymentsHub
        if (!PaymentsHubUtil.isSentToPaymentsHub(message)) {
            rst = super.update(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
        } else {
            // Message sent to PaymentsHub. Nothing to do.
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
        // If the transfer is already in status SETTLED, do not check its related messages.
        // This is to avoid applying the action SETTLE to the transfer several times.
        if (Status.S_SETTLED.equals(transfer.getStatus())) {
            Log.debug(this, String.format("Transfer %d is already SETTLED. Not checking related messages", transfer.getLongId()));
        } else {
            // Get Map Attribute Name/Value
            final Map<String, String> map = getAttributeToKeywordMap(wc, message, oldMessage, trade, selectedTransfer,
                    transfer, messages, dsCon, excps, task, dbCon, events);
            // if not empty, update transfer
            if (!Util.isEmpty(map)) {
                map.forEach((attrName, attrValue) -> selectedTransfer.setAttribute(attrName, attrValue));
                isXferUpdated = true;
            }
        }
        return isXferUpdated;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public Map<String, String> getAttributeToKeywordMap(final TaskWorkflowConfig wc, final BOMessage message,
                                                        final BOMessage oldMessage, final Trade trade, final BOTransfer selectedTransfer, final BOTransfer transfer,
                                                        final Vector messages, final DSConnection dsCon, final Vector excps, final Task task, final Object dbCon,
                                                        final Vector events) {
        // Get BOTransfer Attributes to Update.
        Map<String, String> mapXferAttributes = this.getMapping(dsCon, wc, ruleName);
        if (Util.isEmpty(mapXferAttributes)) {
            mapXferAttributes = new HashMap<String, String>(1);
        }
        // Add Unsettled attribute
        mapXferAttributes.put("Unsettled", "true");
        return mapXferAttributes;
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
            // Get all the messages linked to this transfer
            try {
                final MessageArray transferMessages = dsCon.getRemoteBO().getTransferMessages(selectedTransfer.getLongId());
                // Transfer Goes to settle only if all related payment messages are in relevant status
                if (checkAllRelatedMessages(message, excps, transferMessages, message.getTemplateName())) {
                    xferAction = Action.SETTLE;
                } else {
                    xferAction = Action.UPDATE;
                }
            } catch (final CalypsoServiceException e) {
                Log.error(this, "Error retrieving messages from the DataBase with the transfer ID " + selectedTransfer.getLongId(), e);
            }
        }
        return xferAction;
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean checkAllRelatedMessages(final BOMessage message, final Vector excps, final MessageArray messages, final String template) {
        // we need to check if the rest of msgs(with different templates) are a relevant status
        final List<Status> relevantStatus = getRelevantStatus();
        for (int j = 0; j < messages.size(); j++) {
            final BOMessage mess = messages.elementAt(j);
            if ((mess.getLongId() != message.getLongId()) && !mess.getTemplateName().equals(template)
                    && !relevantStatus.contains(mess.getStatus())) {
                final BOException ev = new BOException(message.getLongId(),
                        SantUpdateXferNotSentToPaymentsHubMessageRule.class.getSimpleName(), MESSAGE);
                ev.setType(BOException.EXCEPTION);
                excps.addElement(ev);
                return false;
            }
        }
        return true;
    }


    /**
     * Get Relevant Status from DomainValues.
     *
     * @return
     */
    private List<Status> getRelevantStatus() {
        final List<Status> relevantStatus = new ArrayList<Status>();
        // Get relevant Status from domainValues.
        final List<String> relevantStatusStr = DomainValues.values(DN_RELEVANT_MSG_STATUS_SETTLE_XFER);
        if (!Util.isEmpty(relevantStatusStr)) {
            for (final String status : relevantStatusStr) {
                relevantStatus.add(Status.valueOf(status));
            }
        }
        return relevantStatus;
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


}
