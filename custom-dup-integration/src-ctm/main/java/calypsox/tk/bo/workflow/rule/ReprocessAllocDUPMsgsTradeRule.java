package calypsox.tk.bo.workflow.rule;

import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.ctm.util.PlatformAllocationTradeFilterAdapter;
import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
//TODO add msgType filtering and change DSConnection with direct sql access
public class ReprocessAllocDUPMsgsTradeRule implements WfTradeRule, PlatformAllocationTradeFilterAdapter {

    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Reprocess pending ION/CTM allocation messages";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        Optional.ofNullable(trade)
                .filter(this::isPlatformOrCTMBlockTrade)
                .ifPresent(this::reprocessPendingChildAllocationMessages);
        return true;
    }

    private void reprocessPendingChildAllocationMessages(Trade blockTrade) {
        Optional.of(blockTrade)
                .map(this::getBlockTradeReference)
                .map(this::mapMsgExternalRef)
                .map(this::findPendingGatewayMsgs)
                .ifPresent(this::applyReprocessActionToMsgs);

    }

    private String getBlockTradeReference(Trade blockTrade) {
        return Optional.of(blockTrade)
                .map(t -> t.getKeywordValue(CTMUploaderConstants.ALLOCATED_FROM_EXT_REF))
                .orElseGet(() -> blockTrade.getKeywordValue(CTMUploaderConstants.ALLOCATED_FROM_MX_GLOBALID));
    }

    private String mapMsgExternalRef(String reference) {
        return Optional.ofNullable(reference)
                .map(ref -> ref.substring(0, ref.indexOf("_")))
                .orElse("");
    }

    private MessageArray findPendingGatewayMsgs(String reference) {
        MessageArray msgs = new MessageArray();
        try {
            msgs = DSConnection.getDefault().getRemoteBO()
                    .getMessages("mess_attributes", "mess_attributes.message_id = bo_message.message_id " +
                            "AND attr_name='UploadObjectExternalRef' AND attr_value = ?", null, CustomBindVariablesUtil.createNewBindVariable(reference));
        } catch (CalypsoServiceException exc) {
            Log.error(this, exc.getCause());
        }
        return msgs;
    }

    private void applyReprocessActionToMsgs(MessageArray msgs) {
        for (BOMessage msg : msgs) {
            if (isBOMessageActionApplicable(msg, Action.valueOf("REPROCESS"))) {
                try {
                    DSConnection.getDefault().getRemoteBO().save(msg, 0L, "");
                } catch (CalypsoServiceException exc) {
                    Log.error(this, exc.getCause());
                }
            }
        }
    }

    /**
     * Checks if the BO message action is applicable.
     *
     * @param transfer the trade
     * @return true if sucess, false otherwise
     */
    protected boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, DSConnection.getDefault(), null);
    }

}
