/**
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import calypsox.util.MarginCallConstants;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

/**
 * @author aela
 * 
 */
@SuppressWarnings("rawtypes")
public class UpdateNotAttachPortfolioMessageRule implements WfMessageRule {

	@Override
	public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		return true;
	}

	@Override
	public String getDescription() {
		return "This rule will set the information to not attach the portfolio while sending notifications";
	}

	@Override
	public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		// set the attribute ATTACH_PORTFOLIO to true
		message.setAttribute(MarginCallConstants.MESSAGE_ATTR_ATTACH_PORTFOLIO, "false");
		return true;
	}

}
