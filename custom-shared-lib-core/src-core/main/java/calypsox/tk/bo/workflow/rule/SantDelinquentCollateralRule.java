package calypsox.tk.bo.workflow.rule;

import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * Add additional information on the margin call if the result of execution is delinquent => when counterparty does not
 * pay or pay less than expected
 */
public class SantDelinquentCollateralRule extends BaseCollateralWorkflowRule {

    private static final String COLLATERAL_EXECUTE_ACTIONS = "CollateralExecuteActions";

    @Override
    public String getDescription() {
        return "Add additional information on the margin call if the result of execution is delinquent";
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig arg0, MarginCallEntry arg1, DSConnection arg2) {
        WorkflowResult wfr = new WorkflowResult();
        wfr.success();
        return wfr;
    }

    /**
     * @param wc
     * @param entry
     * @param state
     * @param oldState
     * @param messages
     * @param dsConnection
     * @param excps
     * @param task
     * @param dbCon
     * @param events
     * @return
     */
    @Override
    protected boolean isApplicable(TaskWorkflowConfig wc, MarginCallEntry entry, EntityState state,
                                   EntityState oldState, List<String> messages, DSConnection dsConnection, List<BOException> excps, Task task,
                                   Object dbCon, List<PSEvent> events) {

        MarginCallEntryDTO lastEntry = loadLastMarginCallEntry(entry, messages);

        new PartExecInfosSetter().setPartExecInfosAttributes(entry, lastEntry);

        return setDelinquentAttributes(entry, lastEntry, messages);
    }

    private boolean setDelinquentAttributes(MarginCallEntry entry, MarginCallEntryDTO lastEntry, List<String> messages) {

        try {
            Double delinquentAmount = computeDelinquentAmount(DSConnection.getDefault(), entry);

            if (delinquentAmount == null) {
                entry.addAttribute(CollateralStaticAttributes.DELINQUENT_AMOUNT, null);
                entry.addAttribute(CollateralStaticAttributes.DELINQUENT_SINCE, null);
                return true;
            }

            JDate delinquentSince = getDelinquentSince(entry, lastEntry);

            // add additional information on the entry about why the margin call is partially allocated.
            entry.addAttribute(CollateralStaticAttributes.DELINQUENT_AMOUNT, delinquentAmount);
            entry.addAttribute(CollateralStaticAttributes.DELINQUENT_SINCE, delinquentSince);

        } catch (Exception e) {
            messages.add("Unable to add additional infos on margin call: " + e.getMessage());
            Log.error(this, e);//Sonar
            return false;
        }
        return true;
    }

    private JDate getDelinquentSince(MarginCallEntry entry, MarginCallEntryDTO lastMarginCallEntry) {
        JDate delinquentSince = entry.getProcessDate();

        if (lastMarginCallEntry != null) {
            Object lastExecutionDate = lastMarginCallEntry
                    .getAttribute(CollateralStaticAttributes.DELINQUENT_SINCE);
            if (lastExecutionDate != null) {
                if (lastExecutionDate instanceof Date) {
                    delinquentSince = JDate.valueOf((Date) lastExecutionDate);
                } else if (lastExecutionDate instanceof JDate) {
                    delinquentSince = (JDate) lastExecutionDate;
                }
            }
        }
        return delinquentSince;
    }

    private MarginCallEntryDTO loadLastMarginCallEntry(MarginCallEntry currentEntry, List<String> messages) {

        MarginCallEntryDTO lastMarginCallEntry = null;

        RemoteSantCollateralService remoteColService = DSConnection.getDefault().getRemoteService(RemoteSantCollateralService.class);

        try {
            List<MarginCallEntryDTO> lastEntry = remoteColService.getMarginCallEntries("mcc_id= "
                    + currentEntry.getCollateralConfigId()
                    + " and process_date = (select max(process_date) "
                    + "						from margin_call_entries temp where temp.mcc_id=margin_call_entries.mcc_id and temp.mcc_id="
                    + currentEntry.getCollateralConfigId() + " and temp.process_date < "
                    + Util.date2SQLString(currentEntry.getProcessDate()) + ")");

            if (!Util.isEmpty(lastEntry)) {
                lastMarginCallEntry = lastEntry.get(0);
            }
        } catch (PersistenceException e) {
            messages.add(e.getMessage());
        }
        return lastMarginCallEntry;
    }

    private Double computeDelinquentAmount(DSConnection dsConnection, MarginCallEntry entry) {
        double allocationAmount = entry.getDailyCashMargin() + entry.getDailySecurityMargin();

        Vector<String> executeActions = LocalCache.getDomainValues(dsConnection, COLLATERAL_EXECUTE_ACTIONS);

        if (!Util.isEmpty(executeActions) && (entry.getEntityState() != null)
                && (entry.getEntityState().getAction() != null)
                && executeActions.contains(entry.getEntityState().getAction().toString())) {
            allocationAmount += entry.getCashMargin() + entry.getSecurityMargin();
        }
        double amount = entry.getGlobalRequiredMargin() - allocationAmount;

        // In case amount = 0.0000012
        String delinquentAmountStr = Util.numberToString(amount, 2, Locale.ENGLISH, false);
        amount = Double.valueOf(delinquentAmountStr);

        if (amount > 0) {
            return amount;
        }
        return null;
    }

    /**
     * Sets SantPartExecInfosCollateralRule attributes. Created in V16 to unify SantDelinquenCollateralRule and SantPartExecInfosCollateralRule.
     * MC Entry retrieval is quite slow, so both rules are fused to reduce DDBB accesses.
     */
    private class PartExecInfosSetter {


        private void setPartExecInfosAttributes(MarginCallEntry currentMarginCallEntry,
                                                MarginCallEntryDTO lastEntry) {
            // add additional information on the entry about why the margin call is partially allocated.
            currentMarginCallEntry.addAttribute(CollateralStaticAttributes.IS_PART_EXEC, Boolean.TRUE);
            currentMarginCallEntry.addAttribute(CollateralStaticAttributes.PART_EXEC_REMAINING_MC,
                    currentMarginCallEntry.getRemainingMargin());
            currentMarginCallEntry.addAttribute(CollateralStaticAttributes.PART_EXEC_STATUS_SINCE,
                    currentMarginCallEntry.getProcessDate());

            if (lastEntry != null) {
                Object lastExecutionDate = lastEntry
                        .getAttribute(CollateralStaticAttributes.PART_EXEC_STATUS_SINCE);
                // if previous entry has PART_EXEC_STATUS_SINCE set, then we need to set the same value for the
                // current entry
                if (lastExecutionDate != null) {
                    JDate partExecSince = null;
                    if (lastExecutionDate instanceof Date) {
                        partExecSince = JDate.valueOf((Date) lastExecutionDate);
                    } else if (lastExecutionDate instanceof JDate) {
                        partExecSince = (JDate) lastExecutionDate;
                    }
                    currentMarginCallEntry.addAttribute(CollateralStaticAttributes.PART_EXEC_STATUS_SINCE,
                            partExecSince);
                }
            }
        }
    }
}
