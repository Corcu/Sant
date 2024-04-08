package calypsox.apps.reporting.workflowaction;

import java.awt.Frame;
import java.util.List;

import com.calypso.apps.reporting.workflowaction.WorkflowActionHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Action;

public class TransferWorkflowActionHandlerMANUAL_SETTLE extends WorkflowActionHandler<BOTransfer>  {
	@Override
	protected boolean handleWorkflowAction(Action action, BOTransfer transfer, List<String> messages, Frame owner) {
		return new SantWorkflowActionUtil().handleWorkflowAction(action, transfer, messages, owner);
	}

	@Override
	protected boolean handleWorkflowAction(Action action, List<String> messages, Frame owner, Task... task) {
		return true;
	}

	@Override
	protected boolean isWorkflowActionImplemented(Action action, Task... tasks) {
		return false;
	}
	
	@Override
	protected boolean isWorkflowActionImplemented(Action action, BOTransfer... transfers) {
		return new SantWorkflowActionUtil().isWorkflowActionImplemented(action, transfers);
	}
}
