/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
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
 * @author Guillermo Solano
 * 
 */
@SuppressWarnings("rawtypes")
public class UpdateAttachPositionsMessageRule implements WfMessageRule {

	/**
	 * Check if the rule must be check (not used)
	 */
	@Override
	public boolean check(TaskWorkflowConfig taskworkflowconfig, BOMessage bomessage, BOMessage bomessage1, Trade trade,
			BOTransfer botransfer, Vector vector, DSConnection dsconnection, Vector vector1, Task task, Object obj,
			Vector vector2) {
		// do nothing
		return true;
	}

	/**
	 * WF rule description
	 */
	@Override
	public String getDescription() {
		return "This rule will set the information to attach the Collateral Positions while sending notifications";
	}

	/**
	 * Updates the message attribute so the Sender has the "knowledge" to attach or not Collateral Positions
	 */
	@Override
	public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade,
			BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon,
			Vector events) {
		// set the attribute ATTACH_POSITIONS to true
		message.setAttribute(MarginCallConstants.MESSAGE_ATTR_ATTACH_POSITIONS, "true");
		return true;
	}

}
