package calypsox.tk.bo.workflow.rule;

import calypsox.tk.csdr.CSDRFiFlowTradeBuilder;
import calypsox.tk.swift.formatter.common.CustomSecuritySWIFTFormatter;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.sql.Connection;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class SetClientRefMessageRule implements WfMessageRule, CustomSecuritySWIFTFormatter {


    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Sets message SEME (as it is shown inside the SWIFT) into ClientRef's Xfer attribute. " +
                "For dummy OPFI SimpleXfers, OriginalFailedMsgIdFromFIFLOW keyword is used";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        Trade simpleXferOPFITrade=getLinkedDummyTrade(message);
        String swiftSeme;
        if(simpleXferOPFITrade!=null){
            //SEME from dummy trade KWD
            swiftSeme=trade.getKeywordValue(CSDRFiFlowTradeBuilder.SWIFTSEMETRADEKWD);
        }else{
            //SEME from formatted msgId
            swiftSeme=this.customizeMessageIdentifier(String.valueOf(message.getLongId()));
        }
        return updateTransfer(message,transfer,swiftSeme,dsCon,dbCon);
    }


    private boolean updateTransfer(BOMessage message,BOTransfer transfer, String swiftSeme, DSConnection dsCon, Object dbCon){
        boolean res=true;
        try {
            BOTransfer xfer2Update=getBOTransfer(transfer,message,dsCon,dbCon);
            if(xfer2Update!=null){
                xfer2Update= (BOTransfer) xfer2Update.cloneIfImmutable();
                xfer2Update.setAttribute("ClientRef",swiftSeme);
                DSConnection.getDefault().getRemoteBO().updateAttributes(xfer2Update);
            }
        } catch (Exception exc) {
            Log.error(this,exc.getCause());
            res=false;
        }
        return res;
    }


    private BOTransfer getBOTransfer(BOTransfer transfer, BOMessage message, DSConnection dsCon, Object dbCon) throws PersistenceException {
        BOTransfer xfer2Update;
        if (transfer == null) {
            xfer2Update = BOTransferUtil.getTransfer(message, dsCon, dbCon);
        } else {
            xfer2Update = BOTransferSQL.getTransfer(transfer.getLongId());
        }
        return xfer2Update;
    }

    protected void saveTransfer(BOTransfer transfer, Object dbCon) throws CloneNotSupportedException, PersistenceException, WorkflowException, CalypsoServiceException {
        Connection con=null;
            if(dbCon instanceof Connection) {
                 con = (Connection) dbCon;
            }
        transfer.setAction(Action.UPDATE);
            TransferArray transfers = new TransferArray();
            TransferArray payments = new TransferArray();
            TransferArray links = new TransferArray();
            Trade ttrade = null;
            if (transfer.getTradeLongId() > 0L) {
                ttrade = TradeSQL.getTrade(transfer.getTradeLongId(), con);
            }

            if (transfer.getNettedTransfer()) {
                payments.add(transfer);
                TransferArray nettedTransfers = BOTransferSQL.getNettedTransfers(transfer.getLongId(), con);
                if (nettedTransfers != null) {
                    int size = nettedTransfers.size();

                    for(int i = 0; i < size; ++i) {// 343
                        BOTransfer bTransfer = (BOTransfer)nettedTransfers.elementAt(i).clone();
                        if (!Status.isCanceled(bTransfer.getStatus())) {
                            bTransfer.setAction(transfer.getAction());
                            transfers.add(bTransfer);
                            if (ttrade == null) {
                                ttrade = TradeSQL.getTrade(bTransfer.getTradeLongId(), con);
                            }

                            links.add(transfer);
                            links.add(bTransfer);
                        }
                    }
                }
            } else {
                transfers.add(transfer);
            }
            BackOfficeServerImpl.saveTransfers(0L, null, transfers, payments, links, ttrade, new ExternalArray(),null, new Vector(), con);
    }

    private Trade getLinkedDummyTrade(BOMessage boMessage){
        Trade targetTrade=null;
        if(isINCOMINGMsg(boMessage)) {
            try {
                Trade auxTrade = DSConnection.getDefault().getRemoteTrade().getTrade(boMessage.getTradeLongId());
                if(CSDRFiFlowTradeBuilder.isTargetDummyTrade(auxTrade)){
                    targetTrade=auxTrade;
                }
            } catch (CalypsoServiceException e) {
                Log.error(this,e.getCause());
            }
        }
        return targetTrade;
    }

    private boolean isINCOMINGMsg(BOMessage message){
        return Optional.ofNullable(message)
                .map(BOMessage::getMessageType).map(t->t.equals("INC_RECON")).orElse(false);
    }

}
