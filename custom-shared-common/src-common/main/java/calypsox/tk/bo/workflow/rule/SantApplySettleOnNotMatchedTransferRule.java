package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.Optional;
import java.util.Vector;

public class SantApplySettleOnNotMatchedTransferRule implements WfTransferRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Apply settle action on not matched transfers, only for transfer agents with blocking account type.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        Account account = BOCache.getAccount(dsCon, transfer.getGLAccountNumber());
        boolean isToBlockingAccount = isBlockingAccount(account);
        boolean isFromBlockingAccount = false;
        if(!isToBlockingAccount){
            int externalSettleDeliveryId = transfer.getExternalSettleDeliveryId();
            account = getAccountFromSdi(externalSettleDeliveryId, dsCon);
            isFromBlockingAccount = isBlockingAccount(account);

        }
        boolean isBlocking = isToBlockingAccount || isFromBlockingAccount;
        if(isBlocking){
            try {
                TransferArray boTransfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(trade.getLongId());
                for(BOTransfer trans : boTransfers){
                    if(trans.getLongId() != transfer.getLongId() && Status.VERIFIED.equalsIgnoreCase(trans.getStatus().getStatus())){
                        trans.setAction(Action.SETTLE);
                        DSConnection.getDefault().getRemoteBO().save(trans,0,"","");
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error saving TransferAgent: " + e.getMessage());
            }
        }
        return true;
    }

    /**
     * @param account
     * @return
     */
    private boolean isBlockingAccount(Account account){
        return Optional.ofNullable(account).map(acc -> acc.getAccountProperty("Bloqueo")).filter("true"::equalsIgnoreCase).isPresent();
    }
    protected Account getAccountFromSdi(int sdiId, DSConnection dsCon){
        SettleDeliveryInstruction settleDeliveryInstruction = BOCache.getSettleDeliveryInstruction(dsCon, sdiId);
        int accountId = Optional.ofNullable(settleDeliveryInstruction).map(SettleDeliveryInstruction::getGeneralLedgerAccount).orElse(0);
        return BOCache.getAccount(dsCon, accountId);
    }
}




