package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;

public class SantNotGenerateMsgMessageRule implements WfMessageRule {

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		if (trade.getProduct() instanceof MarginCall) {
			// We get the Margin Call through the trade.
			MarginCall marginCall = ((MarginCall) trade.getProduct());
			if (null != marginCall) {
				// We get the total for the Margin Call Contract.
				if (null != marginCall.getSecurity()) {
					if (null != message.getAttribute("FATHER")) {
						if (message.getAttribute("FATHER").equals("yes")) { // We check if the Message has or not the
																			// field
																			// FATHER_FRONT_ID populated.
							return true;
						} else {
							return false;
						}
					} else {
						return true;
					}
				} else {
					return true;
				}
			} else {
				return false;
			}
		} else {
			// SimpleXfer: SBWO INTEREST trades
			return true;
		}
	}

	@Override
	public String getDescription() {
		return "Check if we have or not FATHER_FRONT_ID and we permit or not, depending on the result, the sent of the message.";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {

		return true;
	}
}
