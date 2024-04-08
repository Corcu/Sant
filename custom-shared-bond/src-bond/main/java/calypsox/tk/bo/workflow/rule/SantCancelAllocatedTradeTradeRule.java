package calypsox.tk.bo.workflow.rule;

import calypsox.ctm.util.CTMUploaderConstants;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.TradeRoleAllocation;
import com.calypso.tk.bo.workflow.rule.CancelAllocatedTradeTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.AuditFilter;
import com.calypso.tk.refdata.AuditFilterUtil;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TaskArray;

import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * @author aalonsop
 */
public class SantCancelAllocatedTradeTradeRule extends CancelAllocatedTradeTradeRule {

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if(!Util.isEmpty(trade.getKeywordValue("BlockTradeDetail")) && !isOnlyBookChange(trade, oldTrade)){
            return Optional.of(super.update(wc, trade, oldTrade, messages, dsCon, excps, task, dbCon, events))
                    .filter(value-> value)
                    .map(value-> deletePendingAndProcessedAllocations(trade))
                    .orElse(false);
        }else {
            return true;
        }
    }

    private boolean deletePendingAndProcessedAllocations(Trade trade){
        return deletePendingAllocations(trade) && deleteTradeRoleAllocations(trade);
    }

    private boolean deletePendingAllocations(Trade trade){
        String ref = getBlockTradeReference(trade);
        if (ref == null || Util.isEmpty(ref)){
            return true;
        }
        StringBuilder where = new StringBuilder();
        MessageArray msgs;
        MessageArray result = new MessageArray();
        BackOfficeServerImpl BOServer = new BackOfficeServerImpl();

        try{
            where.append("mess_attributes.message_id = bo_message.message_id AND " +
                    "bo_message.message_status NOT IN ('COMPLETED','CANCELED') AND " +
                    "mess_attributes.attr_name = 'UploadObjectExternalRef' AND mess_attributes.attr_value LIKE '");
            where.append(ref.concat("/_%' "));
            where.append("ESCAPE '/'");

            msgs = BOServer.getMessages("mess_attributes", where.toString(), null);

            if (msgs != null && msgs.size() > 0){
                for (BOMessage msg: msgs){
                    BOMessage clonedMsg = (BOMessage)msg.clone();
                    String user = clonedMsg.getEnteredUser();
                    if (Util.isEmpty(user)){
                        clonedMsg.setEnteredUser(user);
                    }
                    clonedMsg.setAction(Action.CANCEL);
                    result.add(clonedMsg);
                }
                BOServer.saveMessages(0L, null, result, new TaskArray());
            }
        } catch (CalypsoServiceException | CloneNotSupportedException e) {
            Log.error(this, e);
            return false;
        }

        return true;
    }

    private String getBlockTradeReference(Trade blockTrade) {
        if(blockTrade.getKeywordValue("BlockTradeDetail").equals("Platform")){
            return blockTrade.getKeywordValue(CTMUploaderConstants.TRADE_KEYWORD_MX_GLOBAL_ID);
        }else{
            return blockTrade.getExternalReference();
        }
    }

    private boolean deleteTradeRoleAllocations(Trade trade){
        Vector<TradeRoleAllocation> finalAllocs= getRoleAllocations(trade).stream()
                .filter(this::isNotCanceledAllocationTrade)
                .collect(Collectors.toCollection(Vector::new));
        double quantity= getRoleAllocations(trade).stream()
                .filter(this::isCanceledAllocationTrade)
                .map(TradeRoleAllocation::getRelatedTrade)
                .map(Trade::getQuantity)
                .mapToDouble(Double::valueOf).sum();
        trade.setRoleAllocations(finalAllocs);
        trade.setQuantity(trade.getQuantity() + quantity);
        return true;
    }

    private Vector<TradeRoleAllocation> getRoleAllocations(Trade trade) {
        return Optional.ofNullable((Vector<TradeRoleAllocation>) trade.getRoleAllocations())
                .orElseGet(Vector::new);
    }

    private boolean isNotCanceledAllocationTrade(TradeRoleAllocation roleAllocation){
        return !isCanceledAllocationTrade(roleAllocation);
    }

    private boolean isCanceledAllocationTrade(TradeRoleAllocation roleAllocation){
        return Optional.ofNullable(roleAllocation)
                .map(TradeRoleAllocation::getRelatedTrade)
                .map(trade-> Status.S_CANCELED.equals(trade.getStatus()))
                .orElse(false);
    }

    private boolean isOnlyBookChange(Trade trade, Trade oldTrade){
        AuditFilter indiferentChanges = AuditFilterUtil.findByName("Bond Allocation Book Change");
        AuditFilter hasBookChange = AuditFilterUtil.findByName("Trade Book Change");

        return !indiferentChanges.accept(AuditFilter.OP_NOT_ALL_IN, oldTrade, trade) &&
                hasBookChange.accept(AuditFilter.OP_IN, oldTrade, trade);
    }
}
