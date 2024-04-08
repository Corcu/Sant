package calypsox.tk.collateral.command;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.*;
import com.calypso.tk.collateral.command.impl.DummyCommand;
import com.calypso.tk.collateral.optimization.command.OptimizationExecutionContext;
import com.calypso.tk.core.Util;

import java.util.List;

/**
 * @author aalonsop
 */
public class CustomCollateralCommandFactory extends CollateralCommandFactory {

    private final ExecutionContext childContext;
    private final OptimizationExecutionContext childOpContext;

    public CustomCollateralCommandFactory(ExecutionContext context) {
        super(context);
        this.childContext=context;
        this.childOpContext=new OptimizationExecutionContext(context);
    }

    @Override
    protected List<CollateralCommand> createCommandList(MarginCallEntry entry, boolean linked, ACTION type, boolean fullProcess, List<CollateralCommand> commandList) {
        EntryCommandBuilderFactory entryCommandBuilderFactory = new EntryCommandBuilderFactory(this.childContext, this.childOpContext);
        EntryCommandBuilder entryCommandBuilder = entryCommandBuilderFactory.createEntryCommandBuilder(entry, linked);
        if(entryCommandBuilder instanceof DefaultEntryCommandBuilder){
            entryCommandBuilder=new CustomDefaultEntryCommandBuilder(entry,this.childContext);
        }
        EntryCommandSequencerFactory entryCommandSequencerFactory = new EntryCommandSequencerFactory();
        EntryCommandSequencer entryCommandSequencer = entryCommandSequencerFactory.buildEntryCommandSequencer(entry);
        List<CollateralCommand.TYPE> commandTypeList = entryCommandSequencer.buildCommandSequence(type, this.childContext, fullProcess);
        this.buildCommandList(entryCommandBuilder, commandTypeList, commandList);
        if (Util.isEmpty(commandList)) {
            commandList.add(new DummyCommand(this.childContext));
        }

        return commandList;
    }
}
