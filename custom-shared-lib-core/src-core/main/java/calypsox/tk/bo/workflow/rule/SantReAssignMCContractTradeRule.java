package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

/**
 * Check if the trade needs to be reassigned by checking that the relationship trade<-->MC_CONTRACT_NUMBER is still
 * valid.
 * 
 * @author aela
 * 
 */
public class SantReAssignMCContractTradeRule extends SantAssignMCContractTradeRule {

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
			final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

		return true;
	}

	@Override
	public String getDescription() {
		return "Check if the trade needs to be reassigned by checking that the relationship trade<-->MC_CONTRACT_NUMBER is still valid.";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
			final DSConnection ds, final Vector exception, final Task task, final Object dbCon, final Vector events) {
		if (CollateralUtilities.needMrgCallReindexation(trade, oldTrade)) {
			super.update(wc, trade, oldTrade, messages, ds, exception, task, dbCon, events);
		}

		return true;
	}

}
