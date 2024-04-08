package calypsox.tk.bo.workflow.rule;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.stream.Collectors;

public class SantUpdateClientTransferTransferRule implements WfTransferRule {
    private static final String CP_TRANSFER = "CounterParty";
    private static final String NETTING_TYPE = "CounterParty";
    private static final String EX_TYPE = "EX_TRANSFER_SYNC";

    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Sync 'Client' transfer status with 'CounterParty' transfer status";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if( null!=transfer && CP_TRANSFER.equalsIgnoreCase(transfer.getExternalRole())){
            List<BOTransfer> clientRuleTransfers = new ArrayList<>();

            if(isNetting(transfer)){
                clientRuleTransfers = getClientTransfers(transfer);
            }else{
                clientRuleTransfers = BOCreUtils.getInstance().getClientRuleTransfer(trade);
            }

            if(!Util.isEmpty(clientRuleTransfers)){
                //Get Client transfer with same status of CounterParty transfer
                List<BOTransfer> clientTransfer = getClientTransfer(clientRuleTransfers, transfer);
                if(!Util.isEmpty(clientTransfer)){
                    for(BOTransfer xfer : clientTransfer){
                        Action action = transfer.getAction();
                        if(null!=xfer){
                            try {
                                BOTransfer clone = (BOTransfer) xfer.clone();
                                clone.setAction(action);
                                saveBOTransfer(transfer,clone);
                            } catch (CloneNotSupportedException e) {
                                Log.error(this,"Error cloning BOTransfer: " + e);
                            }
                        }else{
                            generateTask(transfer);
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isNetting(BOTransfer transfer){
        return null!=transfer && NETTING_TYPE.equalsIgnoreCase(transfer.getNettingType());
    }

    /**
     * @param clientRuleTransfers
     * @param transfer
     * @return
     */
    private List<BOTransfer> getClientTransfer(List<BOTransfer> clientRuleTransfers, BOTransfer transfer){
        try{
            return clientRuleTransfers.stream().filter(trans -> trans.getStatus().equals(transfer.getStatus())).collect(Collectors.toList());
        }catch (Exception e){
            Log.error(this,"Error: " + e);
        }
        return null;
    }


    /**
     * @param nettingTransfer
     * @return
     */
    private List<BOTransfer> getClientTransfers(BOTransfer nettingTransfer){
        List<BOTransfer> clienTransfers = new ArrayList<>();
        if(null!=nettingTransfer){
            TransferArray underlyingTransfers = getUnderlying(nettingTransfer);
            if(!Util.isEmpty(underlyingTransfers)){
                List<String> tradesIds = underlyingTransfers.stream().map(transfer -> String.valueOf(transfer.getTradeLongId())).collect(Collectors.toList());
                String ids = "";
                try {
                    if (!Util.isEmpty(tradesIds)) {
                        ids = String.join("','", tradesIds);
                        final TransferArray boTransfers = DSConnection.getDefault().getRemoteBO().getBOTransfers("TRADE_ID IN ('"+ids+"')",null);
                        if(!Util.isEmpty(boTransfers)){
                            clienTransfers = boTransfers.stream().filter(transfer -> transfer.getExternalRole().equalsIgnoreCase("Client")).collect(Collectors.toList());
                        }
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this,"Error loading BOTransfers for trades: " + ids);
                }
            }
        }
        return clienTransfers;
    }


    private TransferArray getUnderlying(BOTransfer nettingTransfer){
        TransferArray underlyingTransfers = nettingTransfer.getUnderlyingTransfers();
        if(Util.isEmpty(underlyingTransfers)){
            try {
                underlyingTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(nettingTransfer.getLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading Netting Transfer for BOTransfer: " + nettingTransfer.getLongId());
            }
        }
        return underlyingTransfers;
    }

    /**
     * @param counterpartyTrasnfer
     * @param clientTransfer
     */
    private void saveBOTransfer(BOTransfer counterpartyTrasnfer,BOTransfer clientTransfer){
        try {
            DSConnection.getDefault().getRemoteBackOffice().save(clientTransfer,0,null);
        } catch (CalypsoServiceException e) {
            generateTask(counterpartyTrasnfer);
            Log.error(this,"Error saving Client BOTransfer for trade: " + clientTransfer.getTradeLongId());
        }
    }

    /**
     * @param counterpartyTrasnfer
     */
    private void generateTask(BOTransfer counterpartyTrasnfer){
        Task errorTask = new Task();
        errorTask.setComment("\"Workflow Rule UpdateClientTransfer failed. BOTransfer "+counterpartyTrasnfer.getLongId()+" has not been able to apply the action SETTLE to the corresponding BOTransfer Client");

        errorTask.setStatus(Task.NEW);
        errorTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        errorTask.setEventType(EX_TYPE);
        errorTask.setPriority(Task.PRIORITY_NORMAL);
        errorTask.setPoId(counterpartyTrasnfer.getProcessingOrg());
        errorTask.setBookId(counterpartyTrasnfer.getBookId());
        errorTask.setTradeLongId(counterpartyTrasnfer.getTradeLongId());
        errorTask.setObjectStatus(counterpartyTrasnfer.getStatus());
        errorTask.setDatetime( JDate.getNow().getJDatetime(TimeZone.getDefault()));

        try {
            DSConnection.getDefault().getRemoteBackOffice().save(errorTask);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error saving task for BOTransfer: " +counterpartyTrasnfer.getLongId());
        }

    }
}
