package calypsox.tk.bo.workflow.rule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.event.PSEventReprocessExportMessages;

public class GenerateReprocessEventTradeRule implements WfTradeRule {
	
	protected static String ruleName = "GenerateReprocessEvent";

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig wc, Trade newTrade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		return true;
	}

	@Override
	public String getDescription() {
		return "Generate a reprocessing event for trade export messages Parameters {Action=SEND,Status=NACK_ACC|NACK_INSERT} will apply action SEND on last export messages if their status is ACK_ACC or ACK_INSERT";
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events){
		PSEventReprocessExportMessages event = new PSEventReprocessExportMessages(trade.getLongId());
		
		Map ruleParams = WorkflowUtil.getAttributeToValueMap(wc, ruleName);
		if(ruleParams==null){
			ruleParams = new HashMap<String,String>();
			ruleParams.put("Action", "REGENERATE");
			ruleParams.put("Status", "FAILED_SEND|NACKED|NACK_ACC|NACK_INSERT");
		}
		if(ruleParams != null) {
			Object action = ruleParams.get("Action");
			if(action!=null)
				event.setMessageAction(Action.valueOf(action.toString()));
			
			Object status = ruleParams.get("Status");
			if(status!=null) {
				HashSet<Status> statusSet = new HashSet<Status>();
				String[] allStatus = status.toString().split("\\|");
				for(int i=0;i<allStatus.length;i++)
					statusSet.add(Status.valueOf(allStatus[i]));
				event.setStatus(statusSet);
			}	
		}
		events.addElement(event);
		return true;
	}

}
