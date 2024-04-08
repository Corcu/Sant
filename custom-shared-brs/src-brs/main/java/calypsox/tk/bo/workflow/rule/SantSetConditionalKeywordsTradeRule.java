package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;

import java.util.Iterator;
import java.util.Map;

public class SantSetConditionalKeywordsTradeRule implements WfTradeRule {
	
	private static final String TRADE_RULE = "TradeRule";
	private static final String LOG_CATEGORY = "SantSetConditionalKeywordsTradeRule";
	private static final String PROCESSING_ORG = "ProcessingOrg";
	

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Trade rules that sets keyword based on rule param criterias./n"
				+ "Currently supports/n"
				+ "ProcessingOrg=ShortName";	
	}
	
	
	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
			Vector messages, DSConnection dsCon, Vector excps, Task task,
			Object dbCon, Vector events) {
		
		// No check performed in this rule
		
		return true;
	}
	
	protected String getLogCategory() {
		return LOG_CATEGORY;
	}
	
	protected String getRuleName() {
		String name = this.getClass().getSimpleName();
		if (name.endsWith(TRADE_RULE)) {
			name = name.substring(0, name.length() - TRADE_RULE.length());
		}

		return name;
	}

		
	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade,
			Vector messages, DSConnection dsCon, Vector excps, Task task,
			Object dbCon, Vector events) {
		
		String poParam = getPOParam(wc, getRuleName());
		
		if(poParam != null){
			String poShortName = trade.getBook().getLegalEntity().getCode();
			if(poShortName.length() < 1){
				Log.info(this, getRuleName() + " No ProcessingOrg found for trade " + trade.getLongId());
				return false;
			}
			
			Log.info(this, getRuleName() + " Trade " + trade.getLongId() + " has ProcessingOrg : " + poShortName);
			
			if (!poShortName.equalsIgnoreCase(poParam)) {
				Log.info(this, getRuleName() + " Keywords not set");	
				return true;
			}
		}

		Map<String, String> map = this.getKeywordToValueMap(wc, trade, oldTrade, messages,
					dsCon, excps, task, dbCon, events);

		if (!Util.isEmpty(map)) {
			Iterator i = map.keySet().iterator();

			while (i.hasNext()) {
				String attribute = (String) i.next();
				trade.addKeyword(attribute, (String) map.get(attribute));
				Log.info(this, getRuleName() + " Keyword " + attribute + " set to " + (String) map.get(attribute));
			}
		}
		
		return true;
	}
	
	public Map<String, String> getKeywordToValueMap(TaskWorkflowConfig wc,
			Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		
		Map<String, String> map = null;
		String s = wc.getComment();
		Log.info(this, getRuleName() + " Comment is " + s);
		if (!Util.isEmpty(s)) {
			s = s.trim();
			int startIndex = s.indexOf("{");
			int endIndex = s.indexOf("}");
			if (startIndex != -1 && endIndex != -1) {
				++startIndex;
				s = s.substring(startIndex, endIndex);
				Log.info(this, getRuleName() + " Comment substring is " + s);
				map = Util.stringToMap(s);
			}
		}

		return map;
	}
	
	private String getPOParam(TaskWorkflowConfig wc, String ruleName) {
		Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, ruleName);

		String paramValue = null;
		if (!Util.isEmpty(map)) {
			for (String key : map.keySet()) {
				if (key.equalsIgnoreCase(PROCESSING_ORG)){
					paramValue = (String)map.get(key);
					Log.info(this, getRuleName() + " ParamValue is " + paramValue);
					break;
				}
			}
		}
		return paramValue;
	}

}
