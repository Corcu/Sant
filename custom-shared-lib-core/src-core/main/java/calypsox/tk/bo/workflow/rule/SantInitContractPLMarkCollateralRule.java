package calypsox.tk.bo.workflow.rule;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.sql.PLMarkSQL;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;

import java.sql.Connection;
import java.util.Calendar;
import java.util.List;

/**
 * Init pl mark for dispute adjustment (Independent amount !!) trade
 *
 * @author aela
 */
public class SantInitContractPLMarkCollateralRule extends BaseCollateralWorkflowRule {

    @Override
    public String getDescription() {
        return "Init pl mark for dispute adjustment (Independent amount !!) trade.";
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig arg0, MarginCallEntry arg1, DSConnection arg2) {
        WorkflowResult wfr = new WorkflowResult();
        wfr.success();
        return wfr;
    }

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry paramMarginCallEntry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
                                   DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
                                   List<PSEvent> paramList2) {

        // get the contract
        CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(paramDSConnection,
                paramMarginCallEntry.getCollateralConfigId());
        if (mcc == null) {
            paramList.add("Unable to get the margin call contract.");
            return false;
        }

        try {
            TradeArray disputeTrades = new TradeArray();
            TradeSQL.getTrades(disputeTrades, "product_collateral_exposure",
                    "trade.product_id=product_collateral_exposure.product_id and product_collateral_exposure.mcc_id="
                            + mcc.getId()
                            + " and underlying_type='DISPUTE_ADJUSTMENT' and trade.trade_status<>'CANCELED' ",
                    "trade.trade_id", null);

            if (!Util.isEmpty(disputeTrades) && (disputeTrades.size() > 0)) {
                if (disputeTrades.size() > 1) {
                    paramList.add("More than one dispute adjustment trade found for the contract");
                    return false;
                }

                Trade tradeDispute = disputeTrades.get(0);
                PLMark plMark = CollateralUtilities.createPLMarkIfNotExists(tradeDispute, mcc.getPricingEnvName(),
                        paramMarginCallEntry.getValueDate());
                PLMarkValue marginCallValue = CollateralUtilities.buildPLMarkValue(PricerMeasure.S_MARGIN_CALL,
                        mcc.getCurrency(), 0, "Dispute init");
                plMark.addPLMarkValue(marginCallValue);

                if (plMark != null) {
                    PLMarkSQL.save(plMark);
                }

                // amend that trade
                Trade tradeToAmend = (Trade) tradeDispute.clone();
                tradeToAmend.setAction(Action.AMEND);
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                tradeToAmend.setUpdatedTime(new JDatetime(cal.getTimeInMillis()));
                TradeSQL.save(tradeToAmend, (Connection) paramObject);

            } else {
                paramList.add("Unable to get dispute adjustment trade from the contract " + mcc.getId());
                return false;
            }
        } catch (Exception e) {
            Log.error(this, e);
            paramList.add("Unable to save pl mark on the dispute adjustment trade");
            return false;
        }

        return true;
    }

}
