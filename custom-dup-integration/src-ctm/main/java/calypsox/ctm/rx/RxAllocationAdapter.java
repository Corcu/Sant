package calypsox.ctm.rx;

import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class RxAllocationAdapter {


    public static void reprocessPendingAllocationMsgs(Trade trade){
        Log.info("UPLOADER", Thread.currentThread().getName() +
                " -> [RxAllocationAdapter] Starting to process trade with id: " +
                Optional.of(trade).map(Trade::getLongId).orElse(0L));

        Optional.of(trade)
                .map(RxAllocationAdapter::getBlockTradeReference)
                .filter(extRef -> !extRef.isEmpty())
                .map(RxAllocationAdapter::findPendingGatewayMsgs)
                .filter(msgArray -> !msgArray.isEmpty())
                .ifPresent(RxAllocationAdapter::applyReprocessActionToMsgs);
    }

    /**
     *
     * @param blockTrade
     * @return Platform trades reference
     */
    private static String getBlockTradeReference(Trade blockTrade) {
        return Optional.of(blockTrade)
                .map(t -> t.getKeywordValue(CTMUploaderConstants.TRADE_KEYWORD_MX_GLOBAL_ID))
                .orElse("");
    }

    private static MessageArray findPendingGatewayMsgs(String reference) {
        MessageArray msgs = new MessageArray();
        try {
            List<CalypsoBindVariable> bindVariables= CustomBindVariablesUtil.createNewBindVariable("GATEWAYMSG");
            CustomBindVariablesUtil.addNewBindVariableToList("PENDING_VALID",bindVariables);
            CustomBindVariablesUtil.addNewBindVariableToList("PENDING_TRADE",bindVariables);
            CustomBindVariablesUtil.addNewBindVariableToList(reference+"_%",bindVariables);
            msgs = DSConnection.getDefault().getRemoteBO()
                    .getMessages("mess_attributes", "bo_message.message_type= ? " +
                            " AND bo_message.message_status IN ( ? , ?)" +
                            " AND mess_attributes.message_id = bo_message.message_id " +
                            " AND mess_attributes.attr_name='UploadObjectExternalRef' AND mess_attributes.attr_value LIKE ?", null, bindVariables);
        } catch (CalypsoServiceException exc) {
            Log.error(RxAllocationAdapter.class.getSimpleName(), exc.getCause());
        }
        return msgs;
    }

    private static void applyReprocessActionToMsgs(MessageArray msgs) {
        for (BOMessage msg : msgs) {
            Action actionToApply=Action.valueOf("REPROCESS");
            if (isBOMessageActionApplicable(msg, actionToApply)) {
                try {
                    msg.setAction(actionToApply);
                    DSConnection.getDefault().getRemoteBO().save(msg, 0L, "");
                } catch (CalypsoServiceException exc) {
                    Log.error(RxAllocationAdapter.class.getSimpleName(), exc.getCause());
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
    protected static boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, DSConnection.getDefault(), null);
    }

}
