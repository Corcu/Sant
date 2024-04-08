package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.service.DSConnection;

import java.util.TimeZone;
import java.util.Vector;

/*
 * This Trade workflow rule finds if there is any IM Contract that can be applied
 * to this trade. If it finds one then the contract id is added to the trade keyword MC_CONTRACT_NUMBER, EXTERNAL REFERENCE, TRADE DATE, START DATE.
 */
public class SantAssignMCContractIMTradeRule implements WfTradeRule {

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon,
                         final Vector events) {

        return true;
    }

    @Override
    public String getDescription() {
        final String desc = "Applied to the trade for Exposure IM add a day to Trade Date and StartDate \n"
                + " add attribute MC_CONTRACT_NUMBER, add Contract Id in Internal, External Reference / .";
        return desc;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                          final DSConnection ds, final Vector exception, final Task task, final Object dbCon, final Vector events) {
        Log.debug("SantAssignMCContractIMTradeRule", "Update - Start");

        if (trade == null) {
            return false;
        }
        try {

            if (trade.getProduct() instanceof CollateralExposure) {

                JDate tradeDate = trade.getTradeDate().getJDate(TimeZone.getDefault());
                // add one day
                JDate tradeD1 = tradeDate.addDays(1);
                CollateralExposure collatExp = (CollateralExposure) trade.getProduct();
                // contract id
                int mccId = collatExp.getMccId();
                // add contract id in Internal and External Reference
                trade.setInternalReference(String.valueOf(mccId));
                trade.setExternalReference(String.valueOf(mccId));
                // add one day
                trade.setTradeDate(tradeD1.getJDatetime(TimeZone.getDefault()));
                collatExp.setStartDate(tradeD1);
                // add contract id in attribute MC_CONTRACT_NUMBER
                trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, mccId);
                // add BO_SYSTEM and BO_REFERENCE for L22 report
                trade.addKeyword(CollateralStaticAttributes.BO_SYSTEM, "IM");
                trade.addKeyword(CollateralStaticAttributes.BO_REFERENCE, mccId);
                //add maturity date
                trade.getProduct().setMaturityDate(JDate.valueOf("01/01/3500"));
                //add End Date same as maturity
                collatExp.setEndDate(JDate.valueOf("01/01/3500"));
            }

        } catch (final Exception e) {
            String msg = "Error finding MCContract for the trade " + trade.getLongId();
            msg = msg + ".[SantAssignMCContractIM]";
            messages.add(msg);
            Log.error("SantAssignMCContractIMTradeRule", e);
            return false;
        }
        return true;
    }
}
