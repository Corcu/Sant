package calypsox.tk.collateral.manager.worker.impl;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ACTION;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.manager.worker.impl.LoadTaskWorker;

import java.util.List;

/**
 * @author Cedric
 * <p>
 * Enables MarginCallEntry heavyweight loading
 */
public class FullLoadTaskWorker extends LoadTaskWorker {

    public FullLoadTaskWorker(ExecutionContext executionContext, List<MarginCallEntry> marginCallEntries) {
        super(executionContext, marginCallEntries);
    }

    @Override
    protected ACTION getAction() {
        return ACTION.LOAD;
    }
}


