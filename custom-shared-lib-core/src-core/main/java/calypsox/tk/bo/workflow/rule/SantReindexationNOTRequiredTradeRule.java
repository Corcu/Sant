package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

/**
 * This rule checks if the trade has to be reindexed based on the collateral
 * context property EVERYDAY_REINDEXATION
 * 
 * @author aela
 * 
 */
@SuppressWarnings("rawtypes")
public class SantReindexationNOTRequiredTradeRule extends SantReindexationRequiredTradeRule {

	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector exceptions, Task task, Object dbCon, Vector events) {

		return !super.check(wc, trade, oldTrade, messages, dsCon, exceptions, task, dbCon, events);
	}

	@Override
	public String getDescription() {
		String desc = "This rule checks if the trade has to be reindexed based on the collateral context property 'EVERYDAY_REINDEXATION'. \n";
		return desc;
	}

	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection ds,
			Vector exceptions, Task task, Object dbCon, Vector events) {
		return true;
	}
}
