package calypsox.tk.collateral.command;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.CollateralCommand;
import com.calypso.tk.collateral.command.DefaultEntryCommandBuilder;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.command.impl.CustomBuildCommand;
import com.calypso.tk.collateral.command.impl.CustomUpdateCommand;
import com.calypso.tk.collateral.command.impl.UpdateCommand;

import java.util.List;

/**
 * @author aalonsop
 */
public class CustomDefaultEntryCommandBuilder extends DefaultEntryCommandBuilder {

    ExecutionContext execContext;

    public CustomDefaultEntryCommandBuilder(MarginCallEntry entry, ExecutionContext context) {
        super(entry, context);
        this.execContext=context;
    }

    @Override
    public void addBuildCommands(List<CollateralCommand> commandList) {
        commandList.add(new CustomBuildCommand(this.execContext, this.entry));
    }
    @Override
    public void addUpdateCommands(List<CollateralCommand> commandList) {
        commandList.add(new CustomUpdateCommand(this.execContext, this.entry));
    }

}
