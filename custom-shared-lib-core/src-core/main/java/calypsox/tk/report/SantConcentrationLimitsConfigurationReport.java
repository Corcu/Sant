package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.ConcentrationLimit;
import com.calypso.tk.refdata.ConcentrationRule;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import calypsox.tk.util.concentrationlimits.SantConcentrationLimitsUtil;
import calypsox.util.collateral.CollateralManagerUtil;

// Project: Concentration Limits

public class SantConcentrationLimitsConfigurationReport
        extends MarginCallReport {

    private static final long serialVersionUID = 12345L;

    private static final String MIN = "Min";
    private static final String MAX = "Max";

    private static final String PERCENTAGE = "percentage";
    private static final String AMOUNT = "amount";

    private static final String CR_TECH_SECURITY = "CR_TECH_SECURITY_";
    private static final String CR_TECH_COUNTRY = "CR_TECH_COUNTRY_";
    private static final String CR_TECH_BONDTYPE = "CR_TECH_BONDTYPE_";
    private static final String CR_TECH_ISSUER = "CR_TECH_ISSUER_";

    private static final String SECURITY_TYPE = "Security";
    private static final String COUNTRY_TYPE = "Country";
    private static final String BONDTYPE_TYPE = "BondType";
    private static final String ISSUER_TYPE = "Issuer";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ReportOutput load(final Vector errorMsgsP) {

        try {
            return getReportOutput();

        } catch (RemoteException e) {
            String error = "Error generating SantConcentrationLimitsConfigurationReport.\n";
            Log.error(this, error, e);
            errorMsgsP.add(error + e.getMessage());
        }

        return null;

    }

    /**
     * Get report output
     * 
     * @return
     * @throws RemoteException
     */
    private DefaultReportOutput getReportOutput() throws RemoteException {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

        // load contracts
        Collection<CollateralConfig> contracts = loadContracts();
        if (Util.isEmpty(contracts)) {
            Log.info(this, "Cannot find any contract.\n");
            return null;
        }

        // load items
        List<SantConcentrationLimitsConfigurationItem> concLimitsItems = buildItems(
                contracts);
        for (SantConcentrationLimitsConfigurationItem concLimitsItem : concLimitsItems) {

            ReportRow row = new ReportRow(concLimitsItem.getContract(),
                    ReportRow.MARGIN_CALL_CONFIG);
            row.setProperty(
                    SantConcentrationLimitsConfigurationReportTemplate.CONC_LIMITS_ITEM,
                    concLimitsItem);
            reportRows.add(row);

        }

        // set report rows on output
        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

        return output;

    }

    /**
     * Load all contracts in the system
     * 
     * @return
     * @throws CollateralServiceException
     */
    private Collection<CollateralConfig> loadContracts()
            throws CollateralServiceException {

        MarginCallConfigFilter contractFilter = new MarginCallConfigFilter();

        // Select POs in Concentration Limit reports
        List<Integer> poIds = SantConcentrationLimitsUtil
                .getProcessingOrgIds(getReportTemplate());
        if (poIds != null && poIds.size() > 0) {
            contractFilter.setProcessingOrgIds(poIds);
        }
        // Select POs in Concentration Limit reports - End

        List<CollateralConfig> contracts = CollateralManagerUtil
                .loadCollateralConfigs(contractFilter);

        return contracts;
    }

    /**
     * Build data items
     * 
     * @param contracts
     * @return
     * @throws CollateralServiceException
     */
    private List<SantConcentrationLimitsConfigurationItem> buildItems(
            Collection<CollateralConfig> contracts)
            throws CollateralServiceException {

        List<SantConcentrationLimitsConfigurationItem> items = new ArrayList<SantConcentrationLimitsConfigurationItem>();

        for (CollateralConfig contract : contracts) {

            // get concentration rule id list in the contract
            List<Integer> concentrationRuleIdList = contract
                    .getConcentrationRuleIds();

            if (!Util.isEmpty(concentrationRuleIdList)) {

                // get the rules in the contract
                List<ConcentrationRule> rules = ServiceRegistry.getDefault()
                        .getCollateralDataServer()
                        .loadConcentrationRule(concentrationRuleIdList);

                // get map rule-limits
                HashMap<ConcentrationRule, List<ConcentrationLimit>> ruleLimitsMap = getRuleLimitsMap(
                        rules);

                // get items
                List<SantConcentrationLimitsConfigurationItem> itemsContract = getItemsContract(
                        ruleLimitsMap, contract);

                // add items
                items.addAll(itemsContract);
            }
        }

        return items;

    }

    /**
     * Create a new item and add it to list of items.
     * 
     * @param ruleLimitsMap
     * @param contract
     * @param items
     */
    private List<SantConcentrationLimitsConfigurationItem> getItemsContract(
            HashMap<ConcentrationRule, List<ConcentrationLimit>> ruleLimitsMap,
            CollateralConfig contract) {

        List<SantConcentrationLimitsConfigurationItem> itemsContract = new ArrayList<SantConcentrationLimitsConfigurationItem>();

        for (Map.Entry<ConcentrationRule, List<ConcentrationLimit>> entry : ruleLimitsMap
                .entrySet()) {
            List<ConcentrationLimit> limits = entry.getValue();
            ConcentrationRule rule = entry.getKey();

            // Get the rule type from rule name and contract id
            String ruleType = getRuleType(rule.getName(), contract.getId());

            for (ConcentrationLimit limit : limits) {

                // for the same contract and for the same rule, two different
                // lines can come, one for min and one for max value of the
                // rule.
                HashMap<String, Double> limitsMaxMap = new HashMap<String, Double>();
                limitsMaxMap.put(AMOUNT, limit.getMaximumValue());
                limitsMaxMap.put(PERCENTAGE, limit.getMaximumPercentage());

                HashMap<String, Double> limitsMinMap = new HashMap<String, Double>();
                limitsMinMap.put(AMOUNT, limit.getMinimumValue());
                limitsMinMap.put(PERCENTAGE, limit.getMinimumPercentage());

                // Get if the limit is Max or Min, or both
                List<String> minMaxValueList = getMinMaxList(limitsMinMap,
                        limitsMaxMap);

                for (String minMaxValue : minMaxValueList) {
                    List<String> ruleParameterList = getRuleParameterList(
                            minMaxValue, limitsMinMap, limitsMaxMap);
                    List<Double> parameterValueList = getParameterValueList(
                            minMaxValue, limitsMinMap, limitsMaxMap);

                    // ruleParameterList and parameterValueList should have the
                    // same size
                    for (int i = 0; i < ruleParameterList.size(); i++) {
                        // Add item
                        SantConcentrationLimitsConfigurationItem item = new SantConcentrationLimitsConfigurationItem(
                                contract, ruleType, minMaxValue,
                                ruleParameterList.get(i),
                                parameterValueList.get(i));
                        itemsContract.add(item);
                    }
                }
            }
        }

        return itemsContract;
    }

    /**
     * Build a map rule-limits
     * 
     * @param rules
     * @return
     */
    private static HashMap<ConcentrationRule, List<ConcentrationLimit>> getRuleLimitsMap(
            List<ConcentrationRule> rules) {

        HashMap<ConcentrationRule, List<ConcentrationLimit>> map = new HashMap<ConcentrationRule, List<ConcentrationLimit>>();

        for (ConcentrationRule rule : rules) {
            List<ConcentrationLimit> limits = rule.getLimits();
            map.put(rule, limits);
        }

        return map;
    }

    /**
     * Get list with the parameters values of the rule.
     * 
     * @param minMax
     * @param limitsMinMap
     * @param limitsMaxMap
     * @return
     */
    private static List<Double> getParameterValueList(String minMax,
            HashMap<String, Double> limitsMinMap,
            HashMap<String, Double> limitsMaxMap) {

        List<Double> paramValue = new ArrayList<Double>();

        if (MAX.equals(minMax)) {
            for (Map.Entry<String, Double> limitMax : limitsMaxMap.entrySet()) {
                if (limitMax.getValue() != 0.0) {
                    paramValue.add(limitMax.getValue());
                }
            }
        } else if (MIN.equals(minMax)) {
            for (Map.Entry<String, Double> limitMin : limitsMinMap.entrySet()) {
                if (limitMin.getValue() != 0.0) {
                    paramValue.add(limitMin.getValue());
                }
            }
        }

        return paramValue;
    }

    /**
     * Get list of the rule parameters of the rule
     * 
     * @param minMax
     * @param limitsMinMap
     * @param limitsMaxMap
     * @return
     */
    private static List<String> getRuleParameterList(String minMax,
            HashMap<String, Double> limitsMinMap,
            HashMap<String, Double> limitsMaxMap) {

        List<String> ruleParameter = new ArrayList<String>();

        if (MAX.equals(minMax)) {
            for (Map.Entry<String, Double> limitMax : limitsMaxMap.entrySet()) {
                if (limitMax.getValue() != 0.0) {
                    ruleParameter.add(limitMax.getKey());
                }
            }
        } else if (MIN.equals(minMax)) {
            for (Map.Entry<String, Double> limitMin : limitsMinMap.entrySet()) {
                if (limitMin.getValue() != 0.0) {
                    ruleParameter.add(limitMin.getKey());
                }
            }
        }

        return ruleParameter;
    }

    /**
     * Get if the parameter is max or min.
     * 
     * @param limitsMinMap
     * @param limitsMaxMap
     * @return
     */
    private static List<String> getMinMaxList(
            HashMap<String, Double> limitsMinMap,
            HashMap<String, Double> limitsMaxMap) {
        List<String> minMaxList = new ArrayList<String>();

        for (Map.Entry<String, Double> limitMin : limitsMinMap.entrySet()) {
            if (limitMin.getValue() != 0.0 && !minMaxList.contains(MIN)) {
                minMaxList.add(MIN);
            }
        }

        for (Map.Entry<String, Double> limitMax : limitsMaxMap.entrySet()) {
            if (limitMax.getValue() != 0.0 && !minMaxList.contains(MAX)) {
                minMaxList.add(MAX);
            }
        }

        return minMaxList;
    }

    /**
     * Get the rule type from rule name.
     * 
     * @param contractId
     * @param name
     * @return
     */
    private String getRuleType(final String ruleName, final int contractId) {

        String ruleType = "";

        for (final Map.Entry<String, String> entry : getRuleTypeMap()
                .entrySet()) {
            String aRuleName = entry.getKey() + contractId;
            if (ruleName.equals(aRuleName) && Util.isEmpty(ruleType)) {
                ruleType = entry.getValue();
            }

        }
        return ruleType;
    }

    /**
     * Get the map with the rules types.
     * 
     * @return map HashMap<String, String>
     */
    private static HashMap<String, String> getRuleTypeMap() {
        HashMap<String, String> ruleTypeMap = new HashMap<String, String>();
        ruleTypeMap.put(CR_TECH_BONDTYPE, BONDTYPE_TYPE);
        ruleTypeMap.put(CR_TECH_COUNTRY, COUNTRY_TYPE);
        ruleTypeMap.put(CR_TECH_ISSUER, ISSUER_TYPE);
        ruleTypeMap.put(CR_TECH_SECURITY, SECURITY_TYPE);

        return ruleTypeMap;
    }

}
