package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

public class SantCancelUpdateExternalRefTradeRule implements WfTradeRule {
	
	@SuppressWarnings("rawtypes")
	@Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade,
    	    final Trade oldTrade, final Vector messages, final DSConnection ds,
    	    final Vector excps, final Task task, final Object db,
    	    final Vector events) {
		return true;
	}

	@Override
	public String getDescription() {
		return "Change the external reference to C + ExternalReference";
	}

    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade,
	    final Trade oldTrade, final Vector messages,
	    final DSConnection dsCon, final Vector excps, final Task task,
	    final Object dbCon, final Vector events) {
		trade.setExternalReference("C"+trade.getExternalReference());
		return true;
	}

}
