package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.ReserveCollateralRule;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

import java.util.List;
import java.util.Optional;

/**
 * Reserve only if there is some pending allocations
 *
 * @author aela
 */
public class SantReserveCollateralRule extends ReserveCollateralRule {

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig taskConfig,
                                   MarginCallEntry entry, DSConnection dsCon) {

        if (null == entry.getPricingEnv()){
            setPricingEnvEntry(entry, dsCon);
        }

        List<MarginCallAllocation> allocsToReserve = entry
                .getPendingMarginAllocations();
        WorkflowResult wr = new WorkflowResult();
        if (Util.isEmpty(allocsToReserve)) {
            // nothing to do, skip this rule
            wr.success();
            return wr;
        }
        // apply the core reserve process
        wr = super.apply(taskConfig, entry, dsCon);
        return wr;
    }

    @Override
    protected boolean isApplicable(TaskWorkflowConfig wc,
                                   MarginCallEntry entry, EntityState state, EntityState oldState,
                                   List<String> messages, DSConnection dsConnection,
                                   List<BOException> excps, Task task, Object dbCon,
                                   List<PSEvent> events) {
        List<MarginCallAllocation> allocsToReserve = entry
                .getPendingMarginAllocations();

        if (Util.isEmpty(allocsToReserve)) {
            return true;
        }

        return super.isApplicable(wc, entry, state, oldState, messages,
                dsConnection, excps, task, dbCon, events);
    }

    private void setPricingEnvEntry(MarginCallEntry entry, DSConnection dsCon) {
        String pricingEnvStr = Optional.of(entry.getLastUsedPricingEnv()).orElse(null);
        try {
            PricingEnv pricingEnv = dsCon.getRemoteMarketData().getPricingEnv(pricingEnvStr);
            if (null != pricingEnv) {
                entry.setPricingEnv(pricingEnv);
            }
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getName() + " - " + "Error retrieving pricingEnv: " + pricingEnvStr, e);
        }
    }
}
