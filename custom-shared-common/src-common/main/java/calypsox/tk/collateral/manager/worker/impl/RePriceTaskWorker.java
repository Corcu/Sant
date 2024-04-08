package calypsox.tk.collateral.manager.worker.impl;

import calypsox.tk.collateral.command.CustomCollateralCommandFactory;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.*;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccessUtil;

import java.util.List;

/**
 * @author aalonsop
 */
public class RePriceTaskWorker extends com.calypso.tk.collateral.manager.worker.impl.RePriceTaskWorker {


    public RePriceTaskWorker(ExecutionContext executionContext, List<MarginCallEntry> marginCallEntries) {
        super(executionContext, marginCallEntries);
    }

    public void process() {
        try {
            if (!AccessUtil.isAuthorized("RepriceMarginCallEntry", false)) {// 41
                Log.error(this, "You are not authorized to reprice entries");// 42
                this.done();// 43
                return;// 44
            }

            List<MarginCallEntry> entries = this.getMarginCallEntries();
            if (!Util.isEmpty(entries)) {// 48
                ExecutionContext context = this.getExecutionContext();
                context.getPricingEnvProxy().reload();
                context.setUserAction("Reprice");
                List<CollateralCommand> commands = new CustomCollateralCommandFactory(context).createCommands(entries, ACTION.REPRICE);
                this.executor = CollateralExecutorFactory.getInstance(context).createExecutor(commands);
                this.executor.addPropertyChangeListeners(this.getPropertyChangeListeners());
                this.executor.execute();
            }
        } finally {
            this.done();// 64
        }

    }
}
