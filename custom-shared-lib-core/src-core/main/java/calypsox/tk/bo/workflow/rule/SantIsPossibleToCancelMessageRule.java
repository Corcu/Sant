package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

public class SantIsPossibleToCancelMessageRule implements WfMessageRule {

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		// We get the Status for the Trade.
		Status status = trade.getStatus();

		// Trade.Status == CANCELLED --> Return TRUE, else FALSE.
		if (status.equals(Status.CANCELED)) {
			return true;
		}

		return false;
	}

	@Override
	public String getDescription() {
		return "Check if the message related to a Trade is possible to cancel or not, depending on the Trade Status. "
				+ "If the Status for the Trade is CANCEL, the message could be cancelled, else not.";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(TaskWorkflowConfig arg0, BOMessage arg1, BOMessage arg2, Trade arg3, BOTransfer arg4,
			Vector arg5, DSConnection arg6, Vector arg7, Task arg8, Object arg9, Vector arg10) {
		return true;
	}
}
