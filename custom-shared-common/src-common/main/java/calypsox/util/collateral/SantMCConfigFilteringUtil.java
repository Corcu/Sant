package calypsox.util.collateral;

import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;

import java.util.List;

/**
 * @author aalonsop
 * MarginCallConfigFilter related methods utilities class
 * Builder methods (Builder pattern not used)
 */
public class SantMCConfigFilteringUtil {

    /**
     * @return
     */
    public static synchronized SantMCConfigFilteringUtil getInstance() {
        return new SantMCConfigFilteringUtil();
    }

    private SantMCConfigFilteringUtil() {
        //Hidden constructor
    }

    public MarginCallConfigFilter buildMCConfigFilter(JDate processDate, List<Integer> contractIds, List<Integer> poIds, List<String> contractTypes, List<Integer> legalEntityIds, String sdFilterNames, List<String> contractGroupFilters) {
        MarginCallConfigFilter mcFilter = buildMCConfigFilter(processDate, contractIds, poIds, contractTypes, legalEntityIds, sdFilterNames);
        if (!Util.isEmpty(contractGroupFilters)) {
            mcFilter.setContractGroups(contractGroupFilters);
        }
        return mcFilter;
    }

    public MarginCallConfigFilter buildMCConfigFilter(JDate processDate, List<Integer> contractIds, List<Integer> poIds, List<String> contractTypes, List<Integer> legalEntityIds, String sdFilterNames) {
        MarginCallConfigFilter mcFilter = buildMCConfigFilter(processDate, contractIds, poIds, contractTypes, legalEntityIds);
        if (!Util.isEmpty(sdFilterNames)) {
            mcFilter.setContractFilters(sdFilterNames);
        }
        return mcFilter;
    }

    public MarginCallConfigFilter buildMCConfigFilter(JDate processDate, List<Integer> contractIds, List<Integer> poIds, List<String> contractTypes, List<Integer> legalEntityIds) {
        MarginCallConfigFilter mcFilter = buildMCConfigFilter(processDate, contractIds, poIds, contractTypes);
        if (!legalEntityIds.isEmpty()) {
            mcFilter.setLegalEntityIds(legalEntityIds);
        }
        return mcFilter;
    }

    public MarginCallConfigFilter buildMCConfigFilter(JDate processDate, List<Integer> contractIds, List<Integer> poIds, List<String> contractTypes) {
        MarginCallConfigFilter mcFilter = buildMCConfigFilter(processDate, contractIds, poIds);
        if (!contractTypes.isEmpty()) {
            mcFilter.setContractTypes(contractTypes);
        }
        return mcFilter;
    }

    public MarginCallConfigFilter buildMCConfigFilter(JDate processDate, List<Integer> contractIds, List<Integer> poIds) {
        MarginCallConfigFilter mcFilter = buildMCConfigFilter(processDate, contractIds);
        if (!Util.isEmpty(poIds)) {
            mcFilter.setProcessingOrgIds(poIds);
        }
        return mcFilter;
    }

    public MarginCallConfigFilter buildMCConfigFilter(JDate processDate, List<Integer> contractIds) {
        MarginCallConfigFilter mcFilter = buildMCConfigFilter(processDate);
        if (!Util.isEmpty(contractIds)) {
            mcFilter.setContractIds(contractIds);
        }
        return mcFilter;
    }

    public MarginCallConfigFilter buildMCConfigFilter(List<Integer> contractIds) {
        MarginCallConfigFilter mcFilter = buildMCConfigFilter();
        if (!Util.isEmpty(contractIds)) {
            mcFilter.setContractIds(contractIds);
        }
        return mcFilter;
    }

    public MarginCallConfigFilter buildMCConfigFilter(JDate processDate) {
        MarginCallConfigFilter mcFilter = buildMCConfigFilter();
        if (processDate != null) {
            mcFilter.setProcessDate(processDate);
        }
        return mcFilter;
    }

    public MarginCallConfigFilter buildMCConfigFilter() {
        return new MarginCallConfigFilter();
    }
}
