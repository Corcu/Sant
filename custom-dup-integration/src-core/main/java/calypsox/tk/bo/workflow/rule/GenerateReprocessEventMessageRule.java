package calypsox.tk.bo.workflow.rule;

import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.event.PSEventReprocessExportMessages;

public class GenerateReprocessEventMessageRule implements WfMessageRule {

	public static String ruleName = "GenerateReprocessEvent";
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		return true;
	}

	@Override
	public String getDescription() {
		return "Generate a reprocess event for an export message";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		PSEventReprocessExportMessages event = new PSEventReprocessExportMessages(message);
		Map ruleParams = WorkflowUtil.getAttributeToValueMap(wc, ruleName);
		if(ruleParams!=null) {
			Object accountClosing = ruleParams.get("SendAccountClosing");
			if(accountClosing!=null)
				event.setSendAccountClosing(Util.isTrue(accountClosing.toString()));
		}
		events.addElement(event);
		return true;
	}


}
