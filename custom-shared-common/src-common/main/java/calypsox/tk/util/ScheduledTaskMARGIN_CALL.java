package calypsox.tk.util;

import calypsox.tk.collateral.command.CustomCollateralCommandFactory;
import com.calypso.tk.collateral.CollateralManager;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.*;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.OptimizationConfiguration;
import com.calypso.tk.report.MarginCallReportTemplate;
import com.calypso.tk.util.TimingStatCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author jbv
 */
public class ScheduledTaskMARGIN_CALL extends com.calypso.tk.util.ScheduledTaskMARGIN_CALL {

    public static final String FORCE_VANILLA_PROCESS="Force Vanilla Processing";

    @Override
    public void processEntries(List<MarginCallEntry> entries, List<String> errors) {
        boolean forceVanillaProcess=getBooleanAttribute(FORCE_VANILLA_PROCESS);
        if(forceVanillaProcess){
            super.processEntries(entries,errors);
        }else{
            processEntriesWithCustomBuildCommand(entries,errors);
        }
    }


    public void processEntriesWithCustomBuildCommand(List<MarginCallEntry> entries, List<String> errors) {
        Vector<String> m = new Vector<>();
        if (!this.isValidInput(m)) {
            errors.addAll(m);
        } else {
            for(JDatetime processDatetime:this.getMCProcessDatetimes()){
                List<MarginCallEntry> processedMCEntryList=new ArrayList<>();
                JDate processDate = this.getMCProcessDate(processDatetime);
                JDatetime valDatetime = this.getMCValuationDatetime(processDatetime);
                Log.debug(this, "Starting processing for " + processDate + " with valuation at " + valDatetime);
                MarginCallReportTemplate template = this.instanciateReportTemplate(this.getTemplateName(), processDate, valDatetime, errors);
                entries.addAll(processTemplateWithCustomBuildCommand(template,processDate,valDatetime,processedMCEntryList,errors));
            }
        }
    }


    public List<MarginCallEntry> processTemplateWithCustomBuildCommand(MarginCallReportTemplate template,JDate processDate, JDatetime valDatetime, List<MarginCallEntry> processedMCEntryList, List<String> errors) {
        ExecutionContext context = this.getExecutionContext(template, processDate, valDatetime);
        this.initContext(context);
        CollateralManager manager = new CollateralManager(context);
        List<String> messages = new ArrayList<>();
        long start = System.currentTimeMillis();
        List<MarginCallEntry> createdEntries = this.filterEntriesByCollateralConfigLevel(manager.createEntries(context.getFilter(), messages));
        CollateralExecutor executor = getExecutorWithCustomCommands(context,this.getLoadAction(), createdEntries);
        executor.execute();
        long end = System.currentTimeMillis();
        TimingStatCollector.recordTiming("ScheduledTask-Load", end - start);
        String action = this.getWorkflowAction();
        for (MarginCallEntry entry : createdEntries) {
            if (context.getFilter() != null && !context.getFilter().accept(entry) && action != null) {
                entry.setAction(this.getWorkflowAction());
            }
            if (action != null) {
                entry.setAction(this.getWorkflowAction());
            }
            processedMCEntryList.add(entry);
        }
        saveEntries(manager,processedMCEntryList,errors);
        return processedMCEntryList;
    }



    public void saveEntries(CollateralManager manager,List<MarginCallEntry> processedMCEntryList,List<String> errors) {
        if (this.isCleanupRun()) {
            manager.getExecutor(ACTION.REMOVE, processedMCEntryList).execute();
        } else {
            if (this.isOptimize()) {
                long start = System.currentTimeMillis();
                OptimizationConfiguration config = this.getOptimizationConfiguration();
                if (config != null) {
                    manager.optimize(config, processedMCEntryList, errors);
                }
                long end = System.currentTimeMillis();
                TimingStatCollector.recordTiming("ScheduledTask-Optimize", end - start);
            }
            this.fillAdditionalInfos(processedMCEntryList);
            if (!this.isDryRun()) {
                manager.getExecutor(ACTION.SAVE, processedMCEntryList).execute();
            }
        }
    }

    /**
     *
     * @param executionContext
     * @param type
     * @param entries
     * @return An executor containing a CollateralCommand list. This command list is being built by a custom factory allowing to override core's collateral ones.
     */
    public CollateralExecutor getExecutorWithCustomCommands(ExecutionContext executionContext, ACTION type, List<MarginCallEntry> entries) {
        List<CollateralCommand> commands = CustomCollateralCommandFactory.getInstance(executionContext).createCommands(entries, type);
        return  CollateralExecutorFactory.getInstance(executionContext).createExecutor(commands);
    }

    @Override
    public Vector<String> getDomainAttributes() {
        Vector<String> domainAttrs=super.getDomainAttributes();
        domainAttrs.add(FORCE_VANILLA_PROCESS);
        return domainAttrs;
    }

}
