package calypsox.util.collateral;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.manager.DefaultMarginCallEntryFiller;
import com.calypso.tk.collateral.manager.MarginCallEntryFiller;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author aalonsop
 * MarginCallEntry related methods utilities class
 * Lightweight MCEntries filling
 */
public class SantMarginCallEntryUtil {

    private SantMarginCallEntryUtil() {
    }

    /**
     * @param entry
     * @return
     */
    public static MarginCallEntry getFullWeightMarginCallEntry(MarginCallEntry entry) {
        MarginCallEntry fullWeightMCEntry = entry;
        if (entry != null && entry.getId() > 0 && entry.isLightweight()) {
            try {
                fullWeightMCEntry.update(ServiceRegistry.getDefault().getCollateralServer().loadEntry(entry.getId(), false));
            } catch (CollateralServiceException e) {
                Log.error(SantMarginCallEntryUtil.class.getSimpleName(), "Exception while loading non-lightweight MCEntryDTO");
            }
        }
        return fullWeightMCEntry;
    }

    /**
     * @param marginCallEntry
     * @return
     */
    public static boolean fillSingleLightWeightMarginCallEntry(MarginCallEntry marginCallEntry) {
        Collection<MarginCallEntry> singleEntryList = new ArrayList<>();
        singleEntryList.add(marginCallEntry);
        return fillLightWeightMarginCallEntry(singleEntryList);
    }

    /**
     * @param marginCallEntries
     * @return
     */
    public static boolean fillLightWeightMarginCallEntry(Collection<MarginCallEntry> marginCallEntries) {
        MarginCallEntryFiller filler = new DefaultMarginCallEntryFiller();
        return fillLightWeightMarginCallEntry(filler, marginCallEntries);
    }

    /**
     * @param executionContext
     * @param marginCallEntries
     * @return
     */
    public static boolean fillLightWeightMarginCallEntry(ExecutionContext executionContext, Collection<MarginCallEntry> marginCallEntries) {
        return fillLightWeightMarginCallEntry(executionContext.getMarginCallEntryFiller(), marginCallEntries);
    }

    /**
     * @param executionContext
     * @param marginCallEntries
     * @return
     */
    protected static boolean fillLightWeightMarginCallEntry(MarginCallEntryFiller filler, Collection<MarginCallEntry> marginCallEntries) {
        boolean res = true;
        try {
            filler.fillAll(marginCallEntries);
        } catch (CollateralServiceException exc) {
            Log.error(SantMarginCallEntryUtil.class.getSimpleName(), "Exception while trying to fill lightweight MCEntries");
            res = false;
        }
        return res;
    }
}
