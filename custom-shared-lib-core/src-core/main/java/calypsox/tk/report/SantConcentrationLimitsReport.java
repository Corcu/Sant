package calypsox.tk.report;

import calypsox.tk.util.concentrationlimits.SantConcentrationLimitsUtil;
import calypsox.util.collateral.SantMCConfigFilteringUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.concentration.Concentration;
import com.calypso.tk.collateral.concentration.dto.ConcentrationDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.ConcentrationLimit;
import com.calypso.tk.refdata.ConcentrationRule;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

//Project: Concentration Limits
// Project: SantConcentrationLimitsReport - Filter By PO

public class SantConcentrationLimitsReport extends ConcentrationReport {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Each concentration is stored in this ReportRow property.
     */
    public static final String ROW_PROPERTY_CONCENTRATION = "ConcentrationDTO";

    /**
     * Each contract is stored in this ReportRow property.
     */
    public static final String ROW_PROPERTY_COLLATERAL_CONFIG = "CollateralConfig";

    /**
     * ReportRow property that stores the concentration limit that generated the
     * concentration.
     */
    public static final String ROW_PROPERTY_CONCENTRATION_LIMIT = "ConcentrationLimit";

    @Override
    public ReportOutput load(Vector rawErrorMessages) {
        DefaultReportOutput output = new DefaultReportOutput(this);
        Vector<String> errorMessages = new Vector<>();

        SantConcentrationLimitsReportTemplate template = getReportTemplate();
        JDate valDate = template.getValDate();

        List<ReportRow> reportRows = new LinkedList<>();

        // Filter by Processing Org
        List<Integer> poIds = SantConcentrationLimitsUtil
                .getProcessingOrgIds(template);

        MarginCallConfigFilter mccFilter = SantMCConfigFilteringUtil.getInstance().buildMCConfigFilter(valDate, null, poIds);

        List<Integer> contractIds = getContractIds(mccFilter);
        // Retrieve all entries for the valuation date.
        List<MarginCallEntry> entryDTOs = getEntries(contractIds, valDate);
        // Retrieve all contracts for the valuation date.
        Map<Integer, CollateralConfig> contracts = getContracts(mccFilter);

        for (MarginCallEntry entry : entryDTOs) {
            // For each entry, prepare the report template to retrieve the
            // concentrations.
            template = getReportTemplate();
            template.put(ConcentrationReportTemplate.ACTION,
                    ConcentrationReportTemplate.ACTION_SHOW);
            List<Concentration> concentrationsList = entry
                    .getConcentrations();
            template.setConcentrations(concentrationsList);

            // Set the report template and call the original load method.
            setReportTemplate(template);
            DefaultReportOutput partialOutput = (DefaultReportOutput) super.load(
                    errorMessages);

            if (partialOutput != null && partialOutput.getRows() != null
                    && partialOutput.getRows().length > 0) {
                // Add contract and concentration limit to each row.
                addContractAndLimitInformation(partialOutput, entry, contracts);

                // Add rows produced by the current entry to the main report
                // output.
                reportRows.addAll(Arrays.asList(partialOutput.getRows()));
            }
        }

        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

        // Add all errors produced during this processing.
        rawErrorMessages.addAll(errorMessages);

        return output;
    }

    /**
     * Retrieve the ids of the contracts that are accepted by the given filter.
     *
     * @param mccFilter The MarginCallConfigFilter that accepts the contracts.
     * @return A List of contract ids.
     */
    private List<Integer> getContractIds(MarginCallConfigFilter mccFilter) {
        List<Integer> contractIds = new ArrayList<Integer>();

        try {
            long loadContractsBegin = System.currentTimeMillis();
            List<CollateralConfig> contracts = CollateralManagerUtil
                    .loadCollateralConfigs(mccFilter);
            long loadContractsEnd = System.currentTimeMillis();
            Log.info(this, String.format("Time to load %d contracts: %dms",
                    contracts.size(), loadContractsEnd - loadContractsBegin));

            for (CollateralConfig contract : contracts) {
                contractIds.add(contract.getId());
            }
        } catch (CollateralServiceException e) {
            Log.error(this, "Could not load contract ids", e);
        }

        return contractIds;
    }

    /**
     * Retrive the Margin Call entries of the given contract ids for the given
     * date.
     *
     * @param contractIds A List of the contract ids which entries we want to retrieve.
     * @param valDate     Valuation Date.
     * @return A List of MarginCallEntryDTOs.
     */
    private List<MarginCallEntry> getEntries(List<Integer> contractIds,
                                             JDate valDate) {
        List<MarginCallEntry> entries = new ArrayList<>();

        try {
            long loadEntriesBegin = System.currentTimeMillis();
            entries = CollateralManagerUtil.loadEntries(contractIds, valDate, new ArrayList<>());
            long loadEntriesEnd = System.currentTimeMillis();
            Log.info(this, String.format("Time to load %d entries: %dms",
                    entries.size(), loadEntriesEnd - loadEntriesBegin));
        } catch (RemoteException e) {
            Log.error(this, "Could not load Margin Call Entries", e);
        }

        return entries;
    }

    /**
     * Retrieves the contracts that are accepted by the given filter.
     *
     * @param mccFilter The MarginCallConfigFilter that accepts the contracts.
     * @return A Map that uses the contract id as key and the contract itself as
     * value.
     */
    private Map<Integer, CollateralConfig> getContracts(
            MarginCallConfigFilter mccFilter) {
        Map<Integer, CollateralConfig> contracts = new TreeMap<Integer, CollateralConfig>();

        try {
            List<CollateralConfig> contractLists = CollateralManagerUtil
                    .loadCollateralConfigs(mccFilter);
            for (CollateralConfig contract : contractLists) {
                contracts.put(contract.getId(), contract);
            }
        } catch (CollateralServiceException e) {
            Log.error(this, "Could not load contracts", e);
        }

        return contracts;
    }

    /**
     * Retrieves the concentration rules defined for the given contract.
     *
     * @param contract The CollateralConfig which rules we want to retrieve.
     * @return A Map that uses the name of the concentration rule as key and the
     * rule itself as value.
     */
    private Map<String, ConcentrationRule> getConcentrationRules(
            CollateralConfig contract) {
        Map<String, ConcentrationRule> concentrationRules = new HashMap<String, ConcentrationRule>();

        if (contract != null) {
            try {
                List<ConcentrationRule> concentrationRuleList = ServiceRegistry
                        .getDefault(DSConnection.getDefault())
                        .getCollateralDataServer().loadConcentrationRule(
                                contract.getConcentrationRuleIds());
                for (ConcentrationRule rule : concentrationRuleList) {
                    concentrationRules.put(rule.getName(), rule);
                }
            } catch (CollateralServiceException e) {
                Log.error(this,
                        String.format(
                                "Could not load concentration rules for contract id %d",
                                contract.getId()),
                        e);
            }
        }

        return concentrationRules;
    }

    /**
     * Retrieves the Concentration Limit that has the given name from the given
     * rule.
     *
     * @param rule      The ConcentrationRule where the limit is defined.
     * @param limitName The name of the Concentration Limit.
     * @return The Concentration Limit, or <code>null</code> if it could not be
     * found.
     */
    private ConcentrationLimit getConcentrationLimit(ConcentrationRule rule,
                                                     String limitName) {
        ConcentrationLimit limitToReturn = null;

        List<ConcentrationLimit> limits = rule.getLimits();
        int iLimit = 0;
        int limitsSize = limits.size();
        while (limitToReturn == null && iLimit < limitsSize) {
            ConcentrationLimit limit = limits.get(iLimit);
            if (limitName.equals(limit.getName())) {
                limitToReturn = limit;
            }
            iLimit++;
        }

        return limitToReturn;
    }

    /**
     * Adds the contract related to the given entry and the Concentration Limit
     * that generated each concentration as properties in each report row.
     *
     * @param partialOutput The ReportOutput generated by the ConcentrationReport. This
     *                      output should only contain the concentrations associated with
     *                      the given entry.
     * @param entry         The Margin Call entry to which the concentrations are
     *                      associated.
     * @param contracts     Map of all contracts which entries have been obtained in this
     *                      report. This Map is generated by the method
     *                      {@link #getContracts(MarginCallConfigFilter)}.
     */
    private void addContractAndLimitInformation(
            DefaultReportOutput partialOutput, MarginCallEntry entry,
            Map<Integer, CollateralConfig> contracts) {
        CollateralConfig contract = contracts
                .get(entry.getCollateralConfigId());
        Map<String, ConcentrationRule> concentrationRules = getConcentrationRules(
                contract);
        for (ReportRow row : Arrays.asList(partialOutput.getRows())) {
            // Always add Collateral Config to Row
            row.setProperty(ROW_PROPERTY_COLLATERAL_CONFIG, contract);

            Object rawConcentration = row
                    .getProperty(ROW_PROPERTY_CONCENTRATION);
            if (rawConcentration instanceof ConcentrationDTO) {
                ConcentrationDTO concentration = (ConcentrationDTO) rawConcentration;
                ConcentrationLimit concentrationLimit = getConcentrationLimit(
                        concentrationRules.get(concentration.getRuleName()),
                        concentration.getDescription());
                // Add concentration limit to row
                row.setProperty(ROW_PROPERTY_CONCENTRATION_LIMIT,
                        concentrationLimit);
            }
        }
    }

}
