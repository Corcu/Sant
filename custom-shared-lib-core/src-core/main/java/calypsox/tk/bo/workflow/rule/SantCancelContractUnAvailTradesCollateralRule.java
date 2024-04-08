package calypsox.tk.bo.workflow.rule;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.List;

public class SantCancelContractUnAvailTradesCollateralRule extends BaseCollateralWorkflowRule {

    @Override
    public String getDescription() {
        return "Cancels UnAvailability Trades related to a contract.";
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig arg0, MarginCallEntry arg1, DSConnection arg2) {
        WorkflowResult wfr = new WorkflowResult();
        wfr.success();
        return wfr;
    }

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry marginCallEntry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList, DSConnection dsCon,
                                   List<BOException> paramList1, Task paramTask, Object paramObject, List<PSEvent> paramList2) {

        Log.info(SantCancelContractUnAvailTradesCollateralRule.class,
                "In SantCancelContractUnAvailTradesCollateralRule.isApplicable()");

        int mcId = marginCallEntry.getCollateralConfigId();
        TradeArray trades = null;
        try {
            trades = getUnAvailableTransfers(dsCon, mcId);

        } catch (Exception e1) {
            Log.info(SantCancelContractUnAvailTradesCollateralRule.class,
                    "Error getting Unavailable Transfers for contract " + mcId, e1);
            paramList.add("Error getting Unavailable Transfers for contract " + mcId);
            return false;
        }

        if (Util.isEmpty(trades)) {
            return true;
        }

        for (Trade trade : trades.getTrades()) {
            Trade tmpTrade = null;
            tmpTrade = (Trade) trade.clone();
            tmpTrade.setAction(Action.CANCEL);
            try {
                dsCon.getRemoteTrade().save(tmpTrade);
                Log.info(SantCancelContractUnAvailTradesCollateralRule.class,
                        "UnAvailableTransfer Trade Cancelled, id=" + tmpTrade.getLongId());
            } catch (RemoteException e) {
                Log.info(SantCancelContractUnAvailTradesCollateralRule.class,
                        "Failed to cancel UnAvailableTransfer Trade " + tmpTrade.getLongId(), e);
                paramList.add("Failed to cancel UnAvailableTransfer Trade " + tmpTrade.getLongId());
                return false;
            }
        }
        return true;

    }

    public TradeArray getUnAvailableTransfers(DSConnection dsCon, int mcId) throws RemoteException {
        Log.info(SantCancelContractUnAvailTradesCollateralRule.class, "In getUnAvailableTransfers()");

        String from = "trade_keyword tk1, trade_keyword tk2";
        String where = "trade.trade_id=tk1.trade_id and trade.trade_id=tk2.trade_id"
                + " and product_desc.product_type='UnavailabilityTransfer' and trade.trade_status<>'CANCELED' "
                + " and tk1.KEYWORD_NAME='UnavailabilityReason' and tk1.KEYWORD_VALUE='AutomaticOptimization' "
                + " and tk2.KEYWORD_NAME='MC_CONTRACT_NUMBER' and tk2.KEYWORD_VALUE='" + mcId + "'";
        TradeArray unAvailableTransfers = dsCon.getRemoteTrade().getTrades(from, where, null, null);

        Log.info(SantCancelContractUnAvailTradesCollateralRule.class, unAvailableTransfers.size() + " Trade(s) found");

        return unAvailableTransfers;
    }
}
