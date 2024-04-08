package calypsox.tk.util;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.DateRule;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;

public class SantCollateralOptimUtil {

    public static boolean acceptDateRule(String dateRuleName, JDate processDate)
            throws Exception {
        DateRule dateRule = DSConnection.getDefault().getRemoteReferenceData()
                .getDateRule(dateRuleName);

        if (dateRule == null) {
            Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class,
                    "Date Rule " + dateRuleName
                            + " Doesn't exist. Contract will be excluded");
            return false;
        }

        JDate nextDate = DateRule.nextDate(processDate.addDays(-1), dateRule);
        return (nextDate != null && nextDate.equals(processDate));
    }

    public static boolean acceptDateRuleBusinessDay(String dateRuleName,
                                                    JDate processDate) throws Exception {
        DateRule dateRule = DSConnection.getDefault().getRemoteReferenceData()
                .getDateRule(dateRuleName);

        if (dateRule == null) {
            Log.error(ScheduledTaskSANT_COLLATERAL_OPTIMIZATION.class,
                    "Date Rule " + dateRuleName
                            + " Doesn't exist. Contract will be excluded");
            return false;
        }
        JDate previousDate = DateRule.previousDate(processDate, dateRule);
        JDate nextDate = DateRule.nextDate(processDate.addDays(-1), dateRule);
        Holiday hol = Holiday.getCurrent();
        if ((nextDate != null) && nextDate.equals(processDate)) {
            // Matches the Date Rule
            return true;
        } else if (!hol.isBusinessDay(previousDate,
                Util.string2Vector("SYSTEM"))) {
            if (previousDate.addBusinessDays(1, Util.string2Vector("SYSTEM"))
                    .equals(processDate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param entry
     * @return
     */
    public static List<MarginCallAllocation> removeSubstitAllocations(MarginCallEntry entry) {

        Log.info(SantCollateralOptimUtil.class, "entry.getPendingMarginAllocations(true).size())="
                + entry.getPendingMarginAllocations().size());

        List<MarginCallAllocation> deletedAllocations = new ArrayList<MarginCallAllocation>();

        if (!Util.isEmpty(entry.getPendingMarginAllocations())) {
            for (MarginCallAllocation alloc : entry.getPendingMarginAllocations()) {
                //boolean hasExecuteAllocatioAttr = isMarginAllocation(alloc);
                if (isOptimizerSubstitAllocation(alloc)) {
                    Log.info(SantCollateralOptimUtil.class, "One Allocation to Delete... amount=" + alloc.getValue());
                    deletedAllocations.add(alloc);
                    entry.removeAllocation(alloc);
                    Log.info(SantCollateralOptimUtil.class,
                            "One Allocation to Deleted. Now entry.getPendingMarginAllocations(true).size())="
                                    + entry.getPendingMarginAllocations().size());
                }

            }
        }
        Log.info(SantCollateralOptimUtil.class, "deletedAllocations.size()=" + deletedAllocations.size());
        Log.info(SantCollateralOptimUtil.class, "After Delete, entry.getPendingMarginAllocations(true).size())="
                + entry.getPendingMarginAllocations().size());
        return deletedAllocations;
    }

    /**
     * @param alloc
     * @return
     */
    public static boolean isOptimizerMarginAllocation(MarginCallAllocation alloc) {
        return "Margin".equals(alloc.getType()) && "true".equals(alloc.getAttribute("isOptimizerAllocation"));
    }

    /**
     * @param alloc
     * @return
     */
    public static boolean isOptimizerSubstitAllocation(MarginCallAllocation alloc) {
        return "Substitution".equals(alloc.getType()) && "true".equals(alloc.getAttribute("isOptimizerAllocation"));
    }

    /**
     * @param entry
     * @return
     */
    public static List<MarginCallAllocation> removeMarginAllocations(MarginCallEntry entry) {

        Log.info(SantCollateralOptimUtil.class, "entry.getPendingMarginAllocations(true).size())="
                + entry.getPendingMarginAllocations().size());

        List<MarginCallAllocation> deletedAllocations = new ArrayList<>();

        if (!Util.isEmpty(entry.getPendingMarginAllocations())) {
            for (MarginCallAllocation alloc : entry.getPendingMarginAllocations()) {
                //boolean hasExecuteAllocatioAttr = isMarginAllocation(alloc);
                if (isOptimizerMarginAllocation(alloc)) {
                    Log.info(SantCollateralOptimUtil.class, "One Allocation to Delete... amount=" + alloc.getValue());
                    deletedAllocations.add(alloc);
                    entry.removeAllocation(alloc);
                    Log.info(SantCollateralOptimUtil.class,
                            "One Allocation to Deleted. Now entry.getPendingMarginAllocations(true).size())="
                                    + entry.getPendingMarginAllocations().size());
                }

            }
        }
        Log.info(SantCollateralOptimUtil.class, "deletedAllocations.size()=" + deletedAllocations.size());
        Log.info(SantCollateralOptimUtil.class, "After Delete, entry.getPendingMarginAllocations(true).size())="
                + entry.getPendingMarginAllocations().size());
        return deletedAllocations;
    }


}
