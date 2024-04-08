package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

public class RemoveMxIDKeywordTradeRule implements WfTradeRule {
	public final static String KW_MUREX_ID = "MxID";
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		return true;
	}

	@Override
	public String getDescription() {
		return "Remove keyword " + KW_MUREX_ID ;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		trade.removeKeyword(KW_MUREX_ID);
		return true;
	}

}
