package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.sql.BOMessageSQL;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.swiftparser.SecurityMatcher;

import java.sql.Connection;
import java.util.Optional;
import java.util.Vector;

import static calypsox.tk.util.swiftparser.MT541MessageProcessor.PROCESSING_STATUS;

public class SetXferMessageRefMessageRule
        extends com.calypso.tk.bo.workflow.rule.SetXferMessageRefMessageRule {

    private static final String AGENT_REF = "AgentRef";
    private static final String WRITE_OFF_MATCH = "WRITE_OFF_MATCH";
    private static final String SPLITREASON = "SPLITREASON";
    private static final String SECURITY_NETTING = "SecurityNetting";

    private static final String XFER_PROCESSING_STATUS = "ProcessingStatus";

    public boolean isRefactorEnabled(){
        return !isRefactorUnabled();
    }

    public boolean isRefactorUnabled(){
        String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(),"CodeActivationDV","DEACTIVATE_UNSETXFERREF_RULE");
        return Boolean.parseBoolean(activationFlag);
    }

    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message,
                         BOMessage oldMessage, Trade trade, BOTransfer transfer,
                         Vector messages, DSConnection dsCon, Vector excps, Task task,
                         Object dbCon, Vector events) {
        if(isRefactorUnabled()&&!isMT537(message)) {
                setCustomLinkedIds(message, dsCon, dbCon);
        }
        if (message.getTransferLongId() == 0L) {
            messages.add(String.format("Transfer not found for message %d. %s", message.getLongId()>0?message.getLongId():message.getAllocatedLongSeed(), (Util.isEmpty(message.getAttribute("PORef"))?"":"PO Reference "+message.getAttribute("PORef"))));
            return false;
        }

        return super.check(wc, message, oldMessage, trade, transfer, messages,
                dsCon, excps, task, dbCon, events);
    }


    /**
     * setCustomLinkedIds must go on update's method instead of the check one.
     * Added a DV trigger to switch between this new behaviour and the old one in case of PRO's impact
     * If this switch remains months after commit's date (23/02/2022), please, delete it, only update method should remain.
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
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (isRefactorEnabled() && !isINCOMINGMsg(message)) {
            setCustomLinkedIds(message, dsCon, dbCon);
        }
        return super.update(wc, message, oldMessage, trade, transfer, messages, dsCon, excps, task, dbCon, events);
    }

    /**
     * setXferAttribute overrides  setXferAttribute to set custom attribute SETTLEMENT_INDICATOR used to trigger acceptance acknowledgement message for new bond issue.
     * This workaround in neccessary to avoid savint transfer twice in Calypso and custom code
     *
     * @param transfer
     * @param message
     * @param attribute
     * @param resetIfEmpty
     */
@Override
    protected boolean setXferAttribute(BOTransfer transfer, BOMessage message, String attribute, boolean resetIfEmpty) {
        if (transfer != null && message.getExternalB() && message.getTemplateName() != null && (message.getTemplateName().startsWith("MT541") || message.getTemplateName().startsWith("MT540"))) {
            transfer.setAttribute(XFER_PROCESSING_STATUS, message.getAttribute(PROCESSING_STATUS));
        }
        if (transfer != null && transfer.getNettedTransfer() && WRITE_OFF_MATCH.equals(message.getAttribute(SecurityMatcher.ATTR_MATCHING_PROCESS))) {
            //workaround to fix Calypso issue with DAP cash trade transfers
            transfer.setAttribute(SPLITREASON, SECURITY_NETTING);
        }
        return super.setXferAttribute(transfer, message, attribute, resetIfEmpty);
    }

    /**
     * Custom logic extracted from this.check() method.
     * @param message
     * @param dsCon
     * @param dbCon
     */
    private void setCustomLinkedIds(BOMessage message,DSConnection dsCon, Object dbCon){

        long linkedMsgId=parseAgentRef(message);

        if (linkedMsgId>0L) {
            // get MT2xx
            BOMessage messageRef = getBOMessage(linkedMsgId,
                    dbCon, dsCon);

            if (messageRef != null) {
                message.setTransferLongId(messageRef.getTransferLongId());
                message.setTradeLongId(messageRef.getTradeLongId());
                message.setLinkedLongId(messageRef.getLongId());
            }
        }
    }

    private long parseAgentRef(BOMessage message){
        long refId=0L;
        String agentRef=message.getAttribute(AGENT_REF);
        if(!Util.isEmpty(agentRef)) {
            try {
                refId = Long.parseLong(agentRef);
            }catch (NumberFormatException exc) {
                Log.debug(SetXferMessageRefMessageRule.class.getName(), "Invalid swift Agent's Reference "+agentRef);
            }
        }
        return refId;
    }

    private BOMessage getBOMessage(long messageId, Object dbCon,
                                   DSConnection dsCon) {
        try {
            if (dbCon != null) {
                return BOMessageSQL.getMessage(messageId,
                        (Connection) dbCon);
            } else {
                return dsCon.getRemoteBO()
                        .getMessage(messageId);
            }
        } catch (CalypsoServiceException | PersistenceException exc) {
            Log.error(SetXferMessageRefMessageRule.class.getName(), exc);
        }
        return null;
    }

    @Override
    protected boolean isSettlementMessage(BOMessage message) {
        boolean res=false;
        String t = message.getTemplateName();
        if (!Util.isEmpty(t)) {
            res=t.equals("MT210") || t.equals("MT202") || t.contains("MT540")
                    || t.contains("MT541") || t.contains("MT542") || t.contains("MT543")
                    || t.startsWith("sese.023");
        }
        return res;
    }

    private boolean isINCOMINGMsg(BOMessage message){
        boolean isIncRecon=Optional.ofNullable(message)
                .map(BOMessage::getMessageType).map(t->t.equals("INC_RECON")).orElse(false);
        boolean isIncoming= Optional.ofNullable(message)
                .map(BOMessage::getMessageType).map(t->t.equals("INCOMING")).orElse(false);
        return isIncoming||isIncRecon;
    }

    private boolean isMT537(BOMessage message){
        return Optional.ofNullable(message)
                .map(BOMessage::getTemplateName).map(t->t.contains("MT537")).orElse(false);
    }
}