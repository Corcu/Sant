package calypsox.tk.util;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.command.impl.SaveCommand;
import com.calypso.tk.core.Action;
import com.calypso.tk.refdata.ContextUserAction;

import java.util.List;

public class SantAllocateActionCommand extends SaveCommand {

    public static String DESCRIPTION = "Takes ALLOCATE Action";
    public static String ALLOCATE = "ALLOCATE";

    public SantAllocateActionCommand(ExecutionContext context, MarginCallEntry marginCallEntry) {
        super(context, marginCallEntry);
    }

    @Override
    protected void doExecute(List<String> messages) {
        @SuppressWarnings("unused")
        ContextUserAction action = null;

        if (this.getEntry().isActionPossible(ALLOCATE)) {
            this.getEntry().setAction(Action.valueOf(ALLOCATE));
        } else {
            messages.add("Action " + ALLOCATE + " is possible for Entry=" + this.getEntry().getId() + "; Contract Id="
                    + this.getEntry().getCollateralConfigId());
        }
        super.doExecute(messages);
    }
}