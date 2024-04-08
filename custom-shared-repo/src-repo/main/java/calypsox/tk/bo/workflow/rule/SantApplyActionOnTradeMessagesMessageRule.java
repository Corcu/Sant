package calypsox.tk.bo.workflow.rule;

import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteBackOffice;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TaskArray;


public class SantApplyActionOnTradeMessagesMessageRule implements WfMessageRule {
	private static final String LOG_CATEGORY = "SantApplyActionOnTradeMessages";
	
	protected String getLogCategory() {
		return LOG_CATEGORY;
	}
	
	protected String getRuleName() {
		return LOG_CATEGORY;
	}

	@Override
	public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		return true;
	}

	@Override
	public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		if (message == null || trade == null) {
			return true;
		}
		
		logRuleParams(wc, getRuleName());
		
		String messageAction = getMessageAction(wc, getRuleName());
		
		try {
			DSConnection ds = DSConnection.getDefault();
			Action action = Action.valueOf(messageAction);
			RemoteBackOffice rbo = ds.getRemoteBackOffice();
			
			MessageArray tradeMessages = DSConnection.getDefault().getRemoteBackOffice().getMessages(trade.getLongId());
			if (tradeMessages != null && tradeMessages.size() > 0) {
				MessageArray messagesToSave = new MessageArray();
				for (BOMessage currentTradeMessage : tradeMessages) {
					// do nothing for the message currently moving in the WF
					if (currentTradeMessage.getLongId() == message.getLongId()) {
						continue;
					}

					if (currentTradeMessage.getMessageType().equals(message.getMessageType())) {
						BOMessage messageToSave = null;
						try {
							messageToSave = (BOMessage)currentTradeMessage.clone();
						} catch (CloneNotSupportedException e) {
							Log.error(this, "Message could not be cloned " + e.toString());
							continue;
						}
						if (BOMessageWorkflow.isMessageActionApplicable(messageToSave, transfer, trade, action, ds, null)) {
							messageToSave.setAction(action);
							messagesToSave.add(messageToSave);
						}
					}
				}

				if (messagesToSave.size() > 0) {
					TaskArray exceptions = new TaskArray();
					rbo.saveMessages(0L, (String)null, messagesToSave, exceptions);
					
					if (exceptions.size() > 0) {
						Log.error(this, "Exceptions found while saving messages : " + exceptions.toString());
					}
				}
			}
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error while applying action to messages: " + e.toString());
		}
		
		return true;
	}

	private String getMessageAction(TaskWorkflowConfig wc, String ruleName) {
		Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, ruleName);

		String paramValue = null;
		if (!Util.isEmpty(map)) {
			for (String key : map.keySet()) {
				if (key.equalsIgnoreCase("MessageAction"))        {
					paramValue = (String)map.get(key);
					break;
				}
			}
		}
		return paramValue;
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
		return "Applies action on all previous Trade messages of the same type.";
	}
}
