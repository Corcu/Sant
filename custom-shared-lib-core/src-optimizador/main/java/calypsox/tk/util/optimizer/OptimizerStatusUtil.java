package calypsox.tk.util.optimizer;

import calypsox.tk.report.generic.loader.SantMarginCallEntryLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.tk.util.ScheduledTaskSANT_EXPORT_OPTIMIZER;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.PreviousPositionDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.MarginCallEntryReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

public class OptimizerStatusUtil {

    private static final String MARGIN_CALL_ENTRY = "MarginCallEntry";

    private static final String OPTIM_EXTRACT_MARGIN_CALLS_UNDER_OPTIMIZATION = "OptimExtract_MarginCalls_UNDER_OPTIMIZATION";

    public static final String OPTIMIZER_STATUS = "OPTIMIZER_STATUS";

    public static final String FAILED_OPTIMIZATION = "FAILED_OPTIMIZATION";

    public static final String UNDER_OPTIMIZATION = "UNDER_OPTIMIZATION";

    public static final String OPTIMIZED = "OPTIMIZED";

    public static void updateOptimizerStatus(
            List<MarginCallEntryDTO> listEntriesToUpdate, String status) {
        for (MarginCallEntryDTO marginCallEntryDTO : listEntriesToUpdate) {
            updateOptimizerStatus(marginCallEntryDTO, status);
        }
    }

    public static void updateOptimizerStatus(
            MarginCallEntryDTO marginCallEntryDTO, String status) {
        // update status send to optimizer
        String actionToApply = "UPDATE";

        int entryId;
        try {
            addOptimizerStatus(marginCallEntryDTO, status);

            entryId = ServiceRegistry
                    .getDefault(DSConnection.getDefault())
                    .getCollateralServer()
                    .save(marginCallEntryDTO, actionToApply,
                            TimeZone.getDefault());
            Log.info(OptimizerStatusUtil.class.getName(), "Entry with id "
                    + entryId + " successfully saved for the contract "
                    + marginCallEntryDTO.getCollateralConfigId());

        } catch (RemoteException e) {

            Log.error(ScheduledTaskSANT_EXPORT_OPTIMIZER.class.getName(), e);
            // TODO limit the second save just to the mismatch version error
            try {
                MarginCallEntryDTO reloadedEntryDTO = ServiceRegistry
                        .getDefault(DSConnection.getDefault())
                        .getCollateralServer()
                        .loadEntry(marginCallEntryDTO.getId());

                addOptimizerStatus(reloadedEntryDTO, status);

                entryId = ServiceRegistry
                        .getDefault(DSConnection.getDefault())
                        .getCollateralServer()
                        .save(reloadedEntryDTO, actionToApply,
                                TimeZone.getDefault());
                Log.info(OptimizerStatusUtil.class.getName(), "Entry with id "
                        + entryId + " successfully saved for the contract "
                        + marginCallEntryDTO.getCollateralConfigId());
            } catch (RemoteException e1) {
                Log.error(ScheduledTaskSANT_EXPORT_OPTIMIZER.class.getName(), e);
                Log.error(ScheduledTaskSANT_EXPORT_OPTIMIZER.class, e1); //sonar
            }
        }
    }

    public static boolean isUnderOptimization() {
        MarginCallEntryReportTemplate template;
        try {
            template = (MarginCallEntryReportTemplate) DSConnection
                    .getDefault()
                    .getRemoteReferenceData()
                    .getReportTemplate(MARGIN_CALL_ENTRY,
                            OPTIM_EXTRACT_MARGIN_CALLS_UNDER_OPTIMIZATION);
            final SantMarginCallEntryLoader loader = new SantMarginCallEntryLoader();
            final Collection<SantMarginCallEntry> santEntries = loader.loadSantMarginCallEntries(template, JDate.getNow());
            for (SantMarginCallEntry santMarginCallEntry : santEntries) {
                if (santMarginCallEntry != null
                        && santMarginCallEntry.getEntry() != null
                        && UNDER_OPTIMIZATION.equals(santMarginCallEntry
                        .getEntry().getAttribute(OPTIMIZER_STATUS))) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.error(OptimizerStatusUtil.class.getName(), e);
            return false;

        }
        return false;
    }

    /**
     * Add optimize status attribute
     *
     * @param mcEntryDTO
     */
    private static void addOptimizerStatus(MarginCallEntryDTO mcEntryDTO, String status) {
        if (mcEntryDTO != null) {
            mcEntryDTO
                    .addAttribute(
                            OPTIMIZER_STATUS,
                            status);

            // TODO: delete with upgrade 1.6.3
            if (Util.isEmpty(mcEntryDTO.getCashPositions())) {
                mcEntryDTO
                        .setCashPosition(new PreviousPositionDTO<CashPositionDTO>());
            }
        }
    }
}
