package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.event.PSEventReprocessExportMessages;

public class GenerateReprocessEventNoAccountClosingMessageRule extends GenerateReprocessEventMessageRule {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		PSEventReprocessExportMessages event = new PSEventReprocessExportMessages(message);
		event.setSendAccountClosing(false);
		events.addElement(event);
		return true;
	}
}
