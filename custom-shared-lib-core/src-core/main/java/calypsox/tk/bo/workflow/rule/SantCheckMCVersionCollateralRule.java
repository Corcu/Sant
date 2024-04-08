package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.impl.BaseMarginCallEntryService;
import com.calypso.tk.collateral.service.impl.DefaultMarginCallEntryService;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import java.util.List;

/**
 * Workaround to implement an optimistic lock on margin call entries
 *
 * @author aela
 */
public class SantCheckMCVersionCollateralRule extends BaseCollateralWorkflowRule {

    @Override
    public String getDescription() {
        return "Check that the Margin call entry was not modified by another user";
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig arg0, MarginCallEntry arg1, DSConnection arg2) {
        WorkflowResult wfr = new WorkflowResult();
        wfr.success();
        return wfr;
    }

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry paramMarginCallEntry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList,
                                   DSConnection paramDSConnection, List<BOException> paramList1, Task paramTask, Object paramObject,
                                   List<PSEvent> paramList2) {
        try {
            BaseMarginCallEntryService mceService = new DefaultMarginCallEntryService();
            MarginCallEntryDTO oldEntry = mceService.loadEntry(paramMarginCallEntry.getId());
            if ((oldEntry != null) && (paramMarginCallEntry.getVersion() != oldEntry.getVersion())) {
                paramList
                        .add("Margin call aleady modified by another user, please reload the margin call. (new entry version: "
                                + oldEntry.getVersion()
                                + ", current entry version: "
                                + paramMarginCallEntry.getVersion() + " )");
                return false;
            }
        } catch (CollateralServiceException e) {
            paramList.add("Unable to check margin call entry version: " + e.getMessage());
            Log.error(this, e);//Sonar
            return false;
        }

        return true;
    }
}
