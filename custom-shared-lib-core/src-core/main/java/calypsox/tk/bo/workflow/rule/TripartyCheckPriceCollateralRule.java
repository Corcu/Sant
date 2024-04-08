package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.CheckPriceCollateralRule;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.MarginCallPosition;
import com.calypso.tk.core.EntityState;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import java.util.List;
import java.util.function.Predicate;

// Rule created for Triparty contracts. Check if the Margin Call Entry has its position(s) correctly priced

public class TripartyCheckPriceCollateralRule extends CheckPriceCollateralRule {
    private static final String DESCRIPTION = "Check (only for Triparty Contracts) if the Margin Call Entry has its position(s) correctly priced. Return false if the contract is not fully priced.";
    private static final String NotPriceable = "NotPriceable";
    @Override
    protected boolean isApplicable(TaskWorkflowConfig wc, MarginCallEntry entry, EntityState state, EntityState oldState, List<String> messages, DSConnection dsConnection, List<BOException> excps, Task task, Object dbCon, List<PSEvent> events) {
        if (entry.getCollateralConfig().isTriParty()){
            // if it's a triparty contract, check if all the positions are priced correctly
            Predicate<MarginCallPosition> mcPosition = s -> s.getStatus().equalsIgnoreCase(NotPriceable);
            Boolean positionsPricedCorrectly = entry.getPositions().stream().noneMatch(mcPosition);
            return positionsPricedCorrectly;
        } else {
            //not triparty contract
            return true;
        }
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig wc, MarginCallEntry entry, DSConnection dsConnection) {
        return super.apply(wc, entry, dsConnection);
    }
}
