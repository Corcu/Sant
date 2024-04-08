package calypsox.tk.util.concentrationlimits;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.refdata.DomainValues.DomainValuesRow;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.util.*;

//Project: Eligibility control
//Project: Concentration Limits - Phase II

public class SantConcentrationLimitsUtil {

    private static final Map<SantConcentrationLimitsRuleType, String> RULE_TYPE_PERCENTAGE_ADDITIONAL_FIELD_MAP = new HashMap<SantConcentrationLimitsRuleType, String>();

    static {
        RULE_TYPE_PERCENTAGE_ADDITIONAL_FIELD_MAP.put(
                SantConcentrationLimitsRuleType.SECURITY,
                "IM_CL_SECURITY_PERCENT");
        RULE_TYPE_PERCENTAGE_ADDITIONAL_FIELD_MAP.put(
                SantConcentrationLimitsRuleType.COUNTRY,
                "IM_CL_COUNTRY_PERCENT");
        RULE_TYPE_PERCENTAGE_ADDITIONAL_FIELD_MAP.put(
                SantConcentrationLimitsRuleType.ISSUER, "IM_CL_ISSUER_PERCENT");
        RULE_TYPE_PERCENTAGE_ADDITIONAL_FIELD_MAP.put(
                SantConcentrationLimitsRuleType.BONDTYPE,
                "IM_CL_BONDTYPE_PERCENT");
    }

    private static final Map<SantConcentrationLimitsRuleType, String> RULE_TYPE_AMOUNT_ADDITIONAL_FIELD_MAP = new HashMap<SantConcentrationLimitsRuleType, String>();

    static {
        RULE_TYPE_AMOUNT_ADDITIONAL_FIELD_MAP.put(
                SantConcentrationLimitsRuleType.SECURITY,
                "IM_CL_SECURITY_AMOUNT");
        RULE_TYPE_AMOUNT_ADDITIONAL_FIELD_MAP.put(
                SantConcentrationLimitsRuleType.COUNTRY,
                "IM_CL_COUNTRY_AMOUNT");
        RULE_TYPE_AMOUNT_ADDITIONAL_FIELD_MAP.put(
                SantConcentrationLimitsRuleType.ISSUER, "IM_CL_ISSUER_AMOUNT");
        RULE_TYPE_AMOUNT_ADDITIONAL_FIELD_MAP.put(
                SantConcentrationLimitsRuleType.BONDTYPE,
                "IM_CL_BONDTYPE_AMOUNT");
    }

    // Is MarginCall trade eligible in its contract
    private static final String DOMAIN_NAME_ELIGIBILITY_CHECK = "SANT_ELIGIBILITY_CHECK";

    private static final String DOMAIN_VALUE_AVERAGE_VOLUME_AT_90_DAYS = "SANT_AVERAGE_VOLUME_AT_90_DAYS";
    private static final String DOMAIN_VALUE_MAX_VALUE_CURRENCY = "SANT_MAX_VALUE_CURRENCY";
    private static final String DOMAIN_VALUE_MAX_VALUE_AMOUNT = "SANT_MAX_VALUE_AMOUNT";

    private static final String HOLIDAY_CODE_SYSTEM = "SYSTEM";
    private static final String PRICING_ENVIRONMENT_CURRENCY_CONVERSION = "OFFICIAL";

    private static final String AVERAGE_VOLUME_REJECTED_COMMENT = "Trade has been rejected because of the following check: Min (\"Average Volume at 90 days\"; EUR 50 MM or equivalent currency)";
    // Is MarginCall trade eligible in its contract - End

    // Concentration Limits - Phase II
    private static final String DOMAIN_NAME_CONCENTRATION_LIMITS_VALUES = "SANT_CONCENTRATION_LIMITS_VALUES";

    private static final String DEFAULT_CONCENTRATION_LIMIT_CURRENCY = "EUR";
    private static final String DOMAIN_VALUE_MAX_GLOBAL_REQUIERED_MARGIN = "SANT_MAX_GLOBAL_REQUIRED_MARGIN";
    private static final double DEFAULT_MAX_GLOBAL_REQUIERED_MARGIN = 1000000000.0;
    private static final String DOMAIN_VALUE_COUNTRY_PERCENTAGE_LIMIT = "SANT_COUNTRY_PERCENTAGE_LIMIT";
    private static final double DEFAULT_COUNTRY_PERCENTAGE_LIMIT = 50;
    // Concentration Limits - Phase II - End

    private static final String DOMAIN_VALUE_ISSUER_PERCENTAGE_LIMIT = "SANT_ISSUER_PERCENTAGE_LIMIT";
    private static final double DEFAULT_ISSUER_PERCENTAGE_LIMIT = 50;

    public static List<CollateralConfig> getContracts(
            MarginCallConfigFilter mccFilter, JDate date) {
        List<CollateralConfig> contracts = new ArrayList<>();

        try {
            List<CollateralConfig> allContracts = CollateralManagerUtil
                    .loadCollateralConfigs(mccFilter);
            for (CollateralConfig contract : allContracts) {
                if (!getConcentrationLimits(contract, date).isEmpty()) {
                    contracts.add(contract);
                }
            }
        } catch (CollateralServiceException e) {
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    "Could not load contracts using MarginCallConfigFilter", e);
        }

        return contracts;
    }

    public static Map<SantConcentrationLimitsRuleType, SantConcentrationLimitsValue> getConcentrationLimits(
            CollateralConfig contract, JDate date) {
        Map<SantConcentrationLimitsRuleType, SantConcentrationLimitsValue> concentrationLimits = new HashMap<SantConcentrationLimitsRuleType, SantConcentrationLimitsValue>();

        for (SantConcentrationLimitsRuleType ruleType : Arrays
                .asList(SantConcentrationLimitsRuleType.values())) {
            String percentageString = contract.getAdditionalField(
                    RULE_TYPE_PERCENTAGE_ADDITIONAL_FIELD_MAP.get(ruleType));
            String amountString = contract.getAdditionalField(
                    RULE_TYPE_AMOUNT_ADDITIONAL_FIELD_MAP.get(ruleType));

            if (!Util.isEmpty(percentageString)) {
                percentageString = percentageString.replace("%", "");
                percentageString = percentageString.replace(",", ".");
            }
            double percentageValue = parseAsDouble(percentageString,
                    contract.getId()) / 100.0;
            double amountValue = parseAsDouble(amountString, contract.getId());

            if (percentageValue > 0.0 || amountValue > 0.0) {
                // Concentration Limits - Phase II
                String contractCurrency = contract.getCurrency();
                amountValue = getConvertedAmount(
                        DEFAULT_CONCENTRATION_LIMIT_CURRENCY, contractCurrency,
                        amountValue, date);
                // Concentration Limits - Phase II - End
                concentrationLimits.put(ruleType,
                        new SantConcentrationLimitsValue(percentageValue,
                                amountValue));
            }
        }

        // Concentration Limits - Phase II
        // DELETED As per Concentration Limits - Phase III
        // addCountryLimit(contract, concentrationLimits, date);
        // Concentration Limits - Phase II - End

        return concentrationLimits;
    }

    private static double parseAsDouble(String string, int contractId) {
        double value = 0.0;

        if (!Util.isEmpty(string)) {
            try {
                value = Double.valueOf(string);
            } catch (NumberFormatException e) {
                Log.error(
                        SantConcentrationLimitsRulesManager.class
                                .getCanonicalName(),
                        String.format(
                                "Cannot parse string \"%s\" as double in contract id %d",
                                string, contractId));
            }
        }

        return value;
    }

    // Select POs in Concentration Limit reports

    /**
     * Returns a list containing the ids of the selected Processing Orgs.
     *
     * @param reportTemplate Report Template that uses a Processing Org selector
     * @return A list of Processing Org Ids
     */
    public static List<Integer> getProcessingOrgIds(
            ReportTemplate reportTemplate) {
        List<Integer> poIdsList = new ArrayList<Integer>();

        Object rawPOIds = reportTemplate
                .get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
        if (rawPOIds != null && rawPOIds instanceof String) {
            String poIdsString = (String) rawPOIds;
            String[] poIdsArray = poIdsString.split(",");
            for (int iPoId = 0; iPoId < poIdsArray.length; iPoId++) {
                try {
                    poIdsList.add(Integer.parseInt(poIdsArray[iPoId]));
                } catch (NumberFormatException e) {
                    Log.error(
                            SantConcentrationLimitsUtil.class
                                    .getCanonicalName(),
                            String.format("Could not parse \"%s\" as integer",
                                    poIdsArray[iPoId]),
                            e);
                }
            }
        }

        return poIdsList;
    }
    // Select POs in Concentration Limit reports - End

    // Is MarginCall trade eligible in its contract
    public static boolean isEligibleInContract(Trade marginCallTrade,
                                               Vector<String> comments) {
        boolean isEligible = false;

        if (marginCallTrade != null) {
            Product rawProduct = marginCallTrade.getProduct();
            if (rawProduct instanceof MarginCall) {
                MarginCall marginCall = (MarginCall) rawProduct;
                CollateralConfig contract = getContract(marginCall);

                Product security = marginCall.getSecurity();
                if (security != null) {
                    isEligible = isSecurityEligible(security, contract);
                    if (isEligible && isMarginCallEquity(security)) {
                        isEligible = isMarginCallEquityEligible(marginCallTrade,
                                (Equity) security, comments);
                    }
                } else {
                    isEligible = isCurrencyEligible(marginCall.getCurrency(),
                            contract);
                }
            }
        }

        return isEligible;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static CollateralConfig getContract(MarginCall marginCall) {
        CollateralConfig contract = null;
        if (marginCall != null) {
            try {
                contract = ServiceRegistry.getDefault()
                        .getCollateralDataServer()
                        .getMarginCallConfig(marginCall.getMarginCallId());
            } catch (CollateralServiceException e) {
                Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                        String.format("Could not retrieve contract id: %d",
                                marginCall.getMarginCallId()),
                        e);
            }
        }

        return contract;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    public static boolean isSecurityEligible(Product security,
                                             CollateralConfig contract) {
        boolean isEligible = false;

        if (security != null && contract != null) {
            List<String> filterNames = contract.getEligibilityFilterNames();

            for (int iFilter = 0; !isEligible
                    && iFilter < filterNames.size(); iFilter++) {
                String sdfName = filterNames.get(iFilter);
                StaticDataFilter sdf = BOCache.getStaticDataFilter(
                        DSConnection.getDefault(), sdfName);
                isEligible = sdf.accept(security);
            }

            if (!isEligible) {
                Log.info(SantConcentrationLimitsUtil.class.getCanonicalName(),
                        String.format(
                                "Security \"%s\" is not eligible for contract id %d",
                                security.getSecCode(
                                        CollateralStaticAttributes.BOND_SEC_CODE_ISIN),
                                contract.getId()));
            }
        }

        return isEligible;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static boolean isCurrencyEligible(String currency,
                                              CollateralConfig contract) {
        boolean isEligible = false;

        if (!Util.isEmpty(currency) && contract != null) {
            List<CollateralConfigCurrency> eligibleCurrencies = contract
                    .getEligibleCurrencies();
            for (int iCurrency = 0; !isEligible
                    && iCurrency < eligibleCurrencies.size(); iCurrency++) {
                CollateralConfigCurrency eligibleCurrency = eligibleCurrencies
                        .get(iCurrency);
                isEligible = eligibleCurrency.getCurrency().equals(currency);
            }

            if (!isEligible) {
                Log.info(SantConcentrationLimitsUtil.class.getCanonicalName(),
                        String.format(
                                "Currency %s is not eligible for contract id %d",
                                currency, contract.getId()));
            }
        }

        return isEligible;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static boolean isMarginCallEquity(Product security) {
        return security != null && security instanceof Equity;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static boolean isMarginCallEquityEligible(Trade trade,
                                                      Equity equity, Vector<String> comments) {
        double maxValue = getMaxValue(trade, equity);

        boolean isEligible = trade.getQuantity() < maxValue;
        if (!isEligible) {
            comments.add(AVERAGE_VOLUME_REJECTED_COMMENT);
        }

        return isEligible;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static double getMaxValue(Trade trade, Equity equity) {
        String currencyFrom = getMaxValueCurrency();
        String currencyTo = trade.getSettleCurrency();
        double amount = getMaxValueAmount();

        double maxValue = getConvertedAmount(currencyFrom, currencyTo, amount);
        if (contractHasValidAverageVolume(equity)) {
            double averageVolume = getAverageVolumeAt90Days(equity);

            double dirtyPrice = CollateralUtilities.getDirtyPrice(equity,
                    trade.getTradeDate().getJDate(TimeZone.getDefault()),
                    getPricingEnv(), null);

            if (dirtyPrice != 0.0) {
                double averageVolumePrice = averageVolume * dirtyPrice;

                maxValue = Math.min(maxValue, averageVolumePrice);
            }
        }
        return maxValue;
    }

    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static String getMaxValueCurrency() {
        String currency = null;

        try {
            DomainValuesRow domainValuesRow = DSConnection.getDefault()
                    .getRemoteReferenceData()
                    .getDomainValuesRow(DOMAIN_NAME_ELIGIBILITY_CHECK,
                            DOMAIN_VALUE_MAX_VALUE_CURRENCY);
            if (domainValuesRow != null
                    && !Util.isEmpty(domainValuesRow.getComment())) {
                String currencyName = domainValuesRow.getComment();
                if (DSConnection.getDefault().getRemoteReferenceData()
                        .getCurrencyDefault(currencyName) != null) {
                    currency = currencyName;
                }
            }
        } catch (CalypsoServiceException e) {
            String message = String.format(
                    "Could not get max value curreny from domain value %s.%s",
                    DOMAIN_NAME_ELIGIBILITY_CHECK,
                    DOMAIN_VALUE_MAX_VALUE_CURRENCY);
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        }

        return currency;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static double getMaxValueAmount() {
        double amount = 0.0;

        try {
            DomainValuesRow domainValuesRow = DSConnection.getDefault()
                    .getRemoteReferenceData()
                    .getDomainValuesRow(DOMAIN_NAME_ELIGIBILITY_CHECK,
                            DOMAIN_VALUE_MAX_VALUE_AMOUNT);
            if (domainValuesRow != null
                    && !Util.isEmpty(domainValuesRow.getComment())) {
                String amountString = domainValuesRow.getComment();
                try {
                    amount = Double.parseDouble(amountString);
                } catch (NumberFormatException e) {
                    String message = String.format(
                            "Could not parse \"%s\" as double", amountString);
                    Log.error(SantConcentrationLimitsUtil.class
                            .getCanonicalName(), message, e);
                }
            }
        } catch (CalypsoServiceException e) {
            String message = String.format(
                    "Could not get max value amount from domain value %s.%s",
                    DOMAIN_NAME_ELIGIBILITY_CHECK,
                    DOMAIN_VALUE_MAX_VALUE_AMOUNT);
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        }

        return amount;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static double getConvertedAmount(String currencyFrom,
                                             String currencyTo, double amount) {
        double convertedAmount = 0.0;
        if (currencyFrom.equals(currencyTo)) {
            convertedAmount = amount;
        } else {
            try {
                JDate date = getPreviousDate();
                PricingEnv princingEnv = getPricingEnv();

                convertedAmount = CollateralUtilities.convertCurrency(
                        currencyFrom, amount, currencyTo, date, princingEnv);
            } catch (MarketDataException e) {
                String message = String.format(
                        "Could not convert amount %f %s to %s", amount,
                        currencyFrom, currencyTo);
                Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                        message, e);
            }
        }

        return convertedAmount;
    }
    // Is MarginCall trade eligible in its contract - End

    // Concentration Limits - Phase II
    private static double getConvertedAmount(String currencyFrom,
                                             String currencyTo, double amount, JDate date) {
        double convertedAmount = 0.0;
        if (currencyFrom.equals(currencyTo)) {
            convertedAmount = amount;
        } else {
            try {
                PricingEnv princingEnv = getPricingEnv();

                convertedAmount = CollateralUtilities.convertCurrency(
                        currencyFrom, amount, currencyTo, date, princingEnv);
            } catch (MarketDataException e) {
                String message = String.format(
                        "Could not convert amount %f %s to %s", amount,
                        currencyFrom, currencyTo);
                Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                        message, e);
            }
        }

        return convertedAmount;
    }
    // Concentration Limits - Phase II - End

    // Is MarginCall trade eligible in its contract
    private static JDate getPreviousDate() {
        JDate today = JDate.getNow();

        Vector<String> holidays = new Vector<String>();
        holidays.add(HOLIDAY_CODE_SYSTEM);

        JDate previousDate = today.addBusinessDays(-1, holidays);

        return previousDate;
    }
    // Is MarginCall trade eligible in its contract - End

    // Concentration Limits - Phase II
    // private static JDate getCurrentDate() {
    // return JDate.getNow();
    // }
    // Concentration Limits - Phase II - End

    // Is MarginCall trade eligible in its contract
    private static PricingEnv getPricingEnv() {
        PricingEnv pricingEnv = null;

        try {
            pricingEnv = DSConnection.getDefault().getRemoteMarketData()
                    .getPricingEnv(PRICING_ENVIRONMENT_CURRENCY_CONVERSION);
        } catch (CalypsoServiceException e) {
            String message = String.format(
                    "Could not retrieve pricing environment \"%s\"",
                    PRICING_ENVIRONMENT_CURRENCY_CONVERSION);
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        }

        return pricingEnv;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static boolean contractHasValidAverageVolume(Equity equity) {
        boolean validAverageVolume = false;

        String fieldName = getAverageVolumeAt90DaysFieldName();
        String fieldValue = equity.getSecCode(fieldName);

        try {
            if (!Util.isEmpty(fieldValue)) {
                Double.parseDouble(fieldValue);
                validAverageVolume = true;
            }
        } catch (NullPointerException e) {
            String message = String.format(
                    "SecCode \"%s\" in equity %d is null", fieldName,
                    equity.getId());
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        } catch (NumberFormatException e) {
            String message = String.format(
                    "SecCode \"%s\" in equity %d does not contain a valid value",
                    fieldName, equity.getId());
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        }

        return validAverageVolume;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static String getAverageVolumeAt90DaysFieldName() {
        String fieldName = null;

        try {
            DomainValuesRow domainValuesRow = DSConnection.getDefault()
                    .getRemoteReferenceData()
                    .getDomainValuesRow(DOMAIN_NAME_ELIGIBILITY_CHECK,
                            DOMAIN_VALUE_AVERAGE_VOLUME_AT_90_DAYS);
            if (domainValuesRow == null) {
                String message = String.format(
                        "Domain value %s.%s is not defined",
                        DOMAIN_NAME_ELIGIBILITY_CHECK,
                        DOMAIN_VALUE_AVERAGE_VOLUME_AT_90_DAYS);
                Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                        message);
            } else if (Util.isEmpty(domainValuesRow.getComment())) {
                String message = String.format(
                        "Domain value %s.%s does not have a comment. This comment should be the name of the Additional Field in the contract",
                        DOMAIN_NAME_ELIGIBILITY_CHECK,
                        DOMAIN_VALUE_AVERAGE_VOLUME_AT_90_DAYS);
                Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                        message);
            } else {
                fieldName = domainValuesRow.getComment();
            }
        } catch (CalypsoServiceException e) {
            String message = String.format(
                    "Could not retrieve domain value %s.%s",
                    DOMAIN_NAME_ELIGIBILITY_CHECK,
                    DOMAIN_VALUE_AVERAGE_VOLUME_AT_90_DAYS);
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        }

        return fieldName;
    }
    // Is MarginCall trade eligible in its contract - End

    // Is MarginCall trade eligible in its contract
    private static double getAverageVolumeAt90Days(Equity equity) {
        double averageVolume = 0.0;

        String fieldName = getAverageVolumeAt90DaysFieldName();
        String fieldValue = equity.getSecCode(fieldName);

        try {
            averageVolume = Double.parseDouble(fieldValue);
        } catch (NullPointerException e) {
            String message = String.format(
                    "SecCode \"%s\" in equity %d is null", fieldName,
                    equity.getId());
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        } catch (NumberFormatException e) {
            String message = String.format(
                    "SecCode \"%s\" in equity %d does not contain a valid value",
                    fieldName, equity.getId());
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        }

        return averageVolume;
    }
    // Is MarginCall trade eligible in its contract - End

    // Concentration Limits - Phase II
    private static void addCountryLimit(CollateralConfig contract,
                                        Map<SantConcentrationLimitsRuleType, SantConcentrationLimitsValue> concentrationLimits,
                                        JDate date) {

        double defaultCountryPertentageLimit = getDefaultCountryPercentageLimit();

        SantConcentrationLimitsValue countryConcentrationLimits = concentrationLimits
                .get(SantConcentrationLimitsRuleType.COUNTRY);
        if (countryConcentrationLimits != null && countryConcentrationLimits
                .getPercentage() < defaultCountryPertentageLimit) {
            // If there is already a country limit with a percentage lower than
            // the default for Concentration Limits Phase II, then we don't have
            // to alter this limit.
        } else {
            if (exceedsMaxGlobalRequiredMargin(contract, date)) {
                double amountValue = 0.0;
                if (countryConcentrationLimits != null) {
                    amountValue = countryConcentrationLimits.getAmount();
                }
                SantConcentrationLimitsValue newCountryLimit = new SantConcentrationLimitsValue(
                        defaultCountryPertentageLimit, amountValue);
                concentrationLimits.put(SantConcentrationLimitsRuleType.COUNTRY,
                        newCountryLimit);
            }
        }
    }
    // Concentration Limits - Phase II - End

    // Concentration Limits - Phase II
    public static boolean exceedsMaxGlobalRequiredMargin(
            CollateralConfig contract, JDate currentDate) {
        boolean exceeds = false;

        int contractId = contract.getId();
        MarginCallEntryDTO entry = getEntry(contractId, currentDate);
        if (entry != null) {
            double maxGlobalRequiredMarginEur = getMaxGlobalRequiredMargin();
            String contractCurrency = contract.getCurrency();

            // Get FX quote from previous day
            Vector<String> holidayCodes = new Vector<String>();
            holidayCodes.add("SYSTEM");
            JDate previousDate = Holiday.getCurrent()
                    .previousBusinessDay(currentDate, holidayCodes);
            double maxGlobalRequiredMargin = getConvertedAmount(
                    DEFAULT_CONCENTRATION_LIMIT_CURRENCY, contractCurrency,
                    maxGlobalRequiredMarginEur, previousDate);
            // Use global required margin absolute value
            if (Math.abs(entry
                    .getGlobalRequiredMargin()) > maxGlobalRequiredMargin) {
                exceeds = true;
            }
        }

        return exceeds;
    }
    // Concentration Limits - Phase II - End

    // Concentration Limits - Phase II
    private static MarginCallEntryDTO getEntry(int contractId, JDate date) {
        MarginCallEntryDTO entry = null;

        List<Integer> contractIdsList = new ArrayList<Integer>();
        contractIdsList.add(contractId);

        int defaultContextId = ServiceRegistry.getDefaultContext().getId();
        try {
            List<MarginCallEntryDTO> entryDTOs = new ArrayList<MarginCallEntryDTO>();
            entryDTOs = ServiceRegistry.getDefault().getCollateralServer()
                    .loadEntries(contractIdsList, date, defaultContextId);
            if (entryDTOs != null && entryDTOs.size() > 0) {
                entry = entryDTOs.get(0);
            }
        } catch (CollateralServiceException e) {
            String message = String.format(
                    "Could not load MarginCall Entries for Contract id %d, Date %s, Context id %d",
                    contractId, date, defaultContextId);
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        }

        return entry;
    }
    // Concentration Limits - Phase II - End

    private static double getMaxGlobalRequiredMargin() {
        double maxGlobalRequiredMargin = DEFAULT_MAX_GLOBAL_REQUIERED_MARGIN;

        Double valueFromDomain = domainValueCommentToDouble(
                DOMAIN_NAME_CONCENTRATION_LIMITS_VALUES,
                DOMAIN_VALUE_MAX_GLOBAL_REQUIERED_MARGIN);
        if (valueFromDomain != null) {
            maxGlobalRequiredMargin = valueFromDomain;
        }

        return maxGlobalRequiredMargin;
    }

    public static double getDefaultCountryPercentageLimit() {
        double countryPercentageLimit = DEFAULT_COUNTRY_PERCENTAGE_LIMIT;

        Double valueFromDomain = domainValueCommentToDouble(
                DOMAIN_NAME_CONCENTRATION_LIMITS_VALUES,
                DOMAIN_VALUE_COUNTRY_PERCENTAGE_LIMIT);
        if (valueFromDomain != null) {
            countryPercentageLimit = valueFromDomain;
        }

        countryPercentageLimit = countryPercentageLimit / 100.0;

        return countryPercentageLimit;
    }

    public static double getDefaultIssuerPercentageLimit() {
        double countryPercentageLimit = DEFAULT_ISSUER_PERCENTAGE_LIMIT;

        Double valueFromDomain = domainValueCommentToDouble(
                DOMAIN_NAME_CONCENTRATION_LIMITS_VALUES,
                DOMAIN_VALUE_ISSUER_PERCENTAGE_LIMIT);
        if (valueFromDomain != null) {
            countryPercentageLimit = valueFromDomain;
        }

        countryPercentageLimit = countryPercentageLimit / 100.0;

        return countryPercentageLimit;
    }

    private static Double domainValueCommentToDouble(String domainName,
                                                     String domainValue) {
        Double amount = null;

        try {
            DomainValuesRow domainValuesRow = DSConnection.getDefault()
                    .getRemoteReferenceData()
                    .getDomainValuesRow(domainName, domainValue);
            if (domainValuesRow != null
                    && !Util.isEmpty(domainValuesRow.getComment())) {
                String amountString = domainValuesRow.getComment();
                try {
                    amount = Double.parseDouble(amountString);
                } catch (NumberFormatException e) {
                    String message = String.format(
                            "Could not parse \"%s\" as double", amountString);
                    Log.error(SantConcentrationLimitsUtil.class
                            .getCanonicalName(), message, e);
                }
            }
        } catch (CalypsoServiceException e) {
            String message = String.format(
                    "Could not get max value amount from domain value %s.%s",
                    domainName, domainValue);
            Log.error(SantConcentrationLimitsUtil.class.getCanonicalName(),
                    message, e);
        }

        return amount;
    }

}
