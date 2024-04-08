package calypsox.tk.bo.workflow.rule;

import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;


public class AddExtRefPrefixTradeRule implements WfTradeRule {
	private static final String LOG_CATEGORY = "AddExtRefPrefixTradeRule";
	private static final String PREFIX_PARAM = "Prefix";
	
	private static final String WKF_MXLASTEVENT_KEYWORD_REPO_MUREX = "MxLastEvent";
	private static final String MX_LAST_EVENT_PORTFOLIO_ASSIGNMENT_VALUE = "PORTFOLIO_ASSIGNMENT";

	protected String getLogCategory() {
		return LOG_CATEGORY;
	}
	
	protected String getRuleName() {
		return "AddExtRefPrefix";
	}

	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
			return true;
	}
	
	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events)
	{
		if (trade == null) {
			return true;
		}
		
		logRuleParams(wc, getRuleName());
		
		// In case action is TERMINATE but MXLastEvent is not PORTFOLIO_ASSIGNMENT, do nothing
		String mxLastEventKW = trade.getKeywordValue(WKF_MXLASTEVENT_KEYWORD_REPO_MUREX);
		if (Action.TERMINATE.equals(trade.getAction()) && !Util.isEmpty(mxLastEventKW)  && !mxLastEventKW.endsWith(MX_LAST_EVENT_PORTFOLIO_ASSIGNMENT_VALUE)) {
			Log.info(this, getRuleName() + " Action is TERMINATE but MxLastEvent is not a PORTFOLIO ASSIGNMENT : " + mxLastEventKW + ". We do not modify the extRef.");
			return true;
		}
		
		String prefix = getPrefix(wc, getRuleName());
		String extRef = trade.getExternalReference();
		if (!Util.isEmpty(prefix) && !Util.isEmpty(extRef) && !extRef.startsWith(prefix)) {
			String newExtRef = prefix + extRef;
			Log.info(this, getRuleName() + " Setting Trade ExtRef to " + newExtRef);
			trade.setExternalReference(newExtRef);
		}
		
		return true;
	}


	private String getPrefix(TaskWorkflowConfig wc, String ruleName) {
		Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, ruleName);

		String domainName = null;
		if (!Util.isEmpty(map)) {
			for (String key : map.keySet()) {
				if (key.equalsIgnoreCase(PREFIX_PARAM))        {
					domainName = (String)map.get(key);
					break;
				}
			}
		}
		return domainName;
	}
	
	private void logRuleParams(TaskWorkflowConfig wc, String ruleName) {
		StringBuilder sb = new StringBuilder();
		sb.append(getRuleName());
		sb.append(" in ");
		sb.append(wc.getCurrentWorkflow());
		sb.append(" Workflow");
		sb.append(" from ");
		sb.append(wc.getStatus());
		sb.append(" to ");
		sb.append(wc.getResultingStatus());
		sb.append(" applying ");
		sb.append(wc.getPossibleAction());
		Log.info(this, sb.toString());

		Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, ruleName);
		if (!Util.isEmpty(map)) {
			for (String key : map.keySet()) {
				Log.info(this, getRuleName() + " Rule param : " + key + "=" + (String)map.get(key));
			}
		}
		else {
			Log.info(this, getRuleName() + " Rule has no parameters set.");
		}
	}
	
	@Override
	public String getDescription() {
		return "Add a prefix to the External Reference of the Trade.\nThe prefix must be set in the rule params (use the new WF Window, not the old one).";
	}
}
