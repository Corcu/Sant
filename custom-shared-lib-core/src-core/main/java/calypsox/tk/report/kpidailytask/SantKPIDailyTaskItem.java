/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.kpidailytask;

import calypsox.tk.report.SantKPIDailyTaskReportStyle;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.tk.report.quotes.FXQuoteHelper;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.CashAllocation;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.util.*;

import static calypsox.tk.core.CollateralStaticAttributes.*;

public class SantKPIDailyTaskItem {

    private static final String FIXED = "FIXED";
    private static final String MOODY = "Moody";
    private static final String SNP = "S&P";
    private static final String FITCH = "Fitch";
    @SuppressWarnings("unused")
    private static final String CREDIT_RATING = "CREDIT RATING";
    private static final String GLOBAL_RATING = "GLOBAL RATING";
    private static final String AMOUNT = "AMOUNT";

    private int contractId;
    private String contractPOShortName; // OWNER
    private String contractNameWithoutPrefix; // ID
    private String contractCptyFullName; // DESCRIPTION
    private String contractType; // TYPE
    private String frequency; // FREQUENCY
    private String event; // EVENT
    private String status; // STATUS
    private String marginCallSituation; // MARGIN CALL SITUATION
    private String contractDirection; // EXCHANGE DIRECTION
    private String cptyCollateralType; // ELIGIBLE ASSET TYPE
    private String effectiveAssetType; // EFFECTIVE ASSET TYPE
    private JDate contractStartDate; // AGREEMENT INPUT DATE
    private JDate eventDate; // EVENT DATE
    private JDate inputDate; // INPUT DATE
    private String contractCurrency; // BASE CURRENCY
    private String effectiveCurrency; // EFFECTIVE CURRENCY
    private String poSnPRating; // OWNER S&P RATING
    private String poMoodyRating; // OWNER MOODYS RATING
    private String cptySnPRating; // COUNTERPARTY S&P RATING
    private String cptyMoodyRating; // COUNTERPARTY MOODYS RATING
    private double exposure; // EXPOSURE
    private double cptyMtm; // COUNTERPARTY EXPOSURE
    private double independentAmount; // INDEPEDENT
    private double entryThresholdAmount; // THRESHOLD
    private double entryMTA; // MTA
    private double lastBalance; // LAST BALANCE
    private double entryGlobalRequiredMargin; // MARGIN CALL
    private double collateralInTransit; // COLLATERAL IN TRANSIT
    private double entryTotalPreviousMargin; // BALANCE
    private double dispute; // DISPUTE
    private String disputeType; // DISPUTE TYPE
    private Double disputeOutOfExposure; // % DISPUTE / EXPOSURE
    private JDate disputeDate; // DISPUTE DATE
    private String isMarginCallCalculation; // MARGIN CALL CALCULATION
    private int tradeNumber; // DEALS
    private double diffMtm; // DIFF MTM
    private String discrepencyType; // TIPO DE DISCREPENCIA
    // private double limitAmount; // LIMIT AMOUNT
    // private double availableLimit; // AVALAIBLE LIMIT
    private String poFitchRating; // OWNER FITCH RATING
    private String cptyFitchRating; // COUNTERPARTY FITCH RATING
    //required for SA-CCR - GSM 17/01/2017
    private CollateralConfig collateralContract;//Sonar

    private final Map<String, Object> columnMap = new HashMap<String, Object>();
    private final List<String> pendingStatusList;
    private final SantFrequencyHelper freqHelper;

    public SantKPIDailyTaskItem(SantMarginCallEntry santEntry, MarginCallEntry entry, List<String> pendingStatusList,
                                SantFrequencyHelper freqHelper) {
        this.freqHelper = freqHelper;
        CachedFXQuoteHelper.setPricingEnv(entry.getPricingEnv());
        this.pendingStatusList = pendingStatusList;
        build(santEntry, entry);
        buildMap();
    }

    // no calculation required
    private void build(SantMarginCallEntry santEntry, MarginCallEntry entry) {

        CollateralConfig contract = entry.getCollateralConfig();
        //required for SA-CCR - GSM 17/01/2017
        this.collateralContract = contract;
        computeAllocations(entry.getAllocations());
        this.contractId = contract.getId();
        this.contractPOShortName = contract.getProcessingOrg().getCode();
        this.contractNameWithoutPrefix = getContractNameWithoutPrefix(contract.getName());
        this.contractCptyFullName = contract.getLegalEntity().getName();
        this.contractType = contract.getContractType();
        String freq = contract.getAdditionalField("FREQUENCY");
        this.frequency = this.freqHelper.getFrequency(freq);
        this.event = "Margin Call";
        this.status = entry.getStatus();
        this.marginCallSituation = this.pendingStatusList.contains(entry.getStatus()) ? "Pending" : "Valid";
        // contractDirection
        computeContractDirection(contract);
        // eligibleAssetType
        computeEligibleAssetType(contract);
        // effectiveAssetType
        this.contractStartDate = contract.getStartingDate().getJDate(TimeZone.getDefault());
        this.eventDate = entry.getProcessDatetime().getJDate(TimeZone.getDefault());
        // inputDate
        this.contractCurrency = contract.getCurrency();
        // this.effectiveCurrency

        // poSnPRating, poMoodyRating, cptySnPRating, cptyMoodyRating
        computeCreditRatings(contract, entry.getValueDatetime().getJDate(TimeZone.getDefault()));
        if (entry.getAttribute(MC_ENTRY_PO_MTM) != null) {
            this.exposure = getAmountInEUR((Double) entry.getAttribute(MC_ENTRY_PO_MTM), entry.getContractCurrency());
        } else {
            this.exposure = getAmountInEUR(entry.getNetBalance(), entry.getContractCurrency());
        }
        this.cptyMtm = entry.getAttribute(MC_ENTRY_CPTY_MTM) == null ? 0 : -(Double) entry
                .getAttribute(MC_ENTRY_CPTY_MTM);
        this.cptyMtm = getAmountInEUR(this.cptyMtm, entry.getContractCurrency());
        this.independentAmount = getAmountInEUR(entry.getIndependentAmount(), entry.getContractCurrency());
        this.entryThresholdAmount = getAmountInEUR(Math.abs(entry.getThresholdAmount()), entry.getContractCurrency());
        this.entryMTA = getAmountInEUR(entry.getMTAAmount(), entry.getContractCurrency());
        // Total Previous Actual Margin
        // GSM: Hotfix 1.9.20. Incidence 844: Balance & last balance mapped incorrectly
        // this.lastBalance = getAmountInEUR(
        // entry.getPreviousActualCashMargin() + entry.getPreviousActualSecurityMargin(),
        // entry.getContractCurrency());
        this.lastBalance = getAmountInEUR(entry.getPreviousTotalMargin(), entry.getContractCurrency());
        this.entryGlobalRequiredMargin = getAmountInEUR(entry.getGlobalRequiredMargin(), entry.getContractCurrency());
        this.collateralInTransit = getAmountInEUR(entry.getDailyCashMargin() + entry.getDailySecurityMargin(),
                entry.getContractCurrency());
        this.entryTotalPreviousMargin = this.lastBalance + this.collateralInTransit;
        // this.entryTotalPreviousMargin = getAmountInEUR(entry.getPreviousTotalMargin(), entry.getContractCurrency());
        // MARGIN CALL - COLLATERAL IN TRANSIT
        this.dispute = this.entryGlobalRequiredMargin - this.collateralInTransit;

        if (!Double.isNaN(this.dispute)) {
            String disputeStr = Util.numberToString(this.dispute, 2, Locale.ENGLISH, false);
            this.dispute = Double.valueOf(disputeStr);
            if ((this.dispute == 0) && disputeStr.startsWith("-")) {
                this.dispute = Math.abs(this.dispute);
            }
        }

        this.disputeType = ((this.dispute == 0) || (Double.isNaN(this.dispute))) ? null : this.dispute > 0 ? "Deficit"
                : "Excess";
        this.disputeOutOfExposure = this.exposure == 0 ? null : this.dispute / this.exposure;
        // To avoid -0.0000
        if ((this.disputeOutOfExposure != null) && (this.disputeOutOfExposure == 0)) {
            this.disputeOutOfExposure = Math.abs(this.disputeOutOfExposure);
        }
        // this.disputeDate = entry.getAttribute(PART_EXEC_STATUS_SINCE) == null ? null : (JDate) entry
        // .getAttribute(PART_EXEC_STATUS_SINCE);
        this.disputeDate = CollateralUtilities.getEntryAttributeAsJDate(entry.toDTO(), PART_EXEC_STATUS_SINCE);
        // isMarginCallCalculation
        computeMarginCallCalculation(freq, entry.getProcessDatetime().getJDate(TimeZone.getDefault()));
        this.tradeNumber = santEntry.getNbTrades();
        this.diffMtm = entry.getAttribute(MC_ENTRY_MTM_DIFF) == null ? 0 : getAmountInEUR(
                (Double) entry.getAttribute(MC_ENTRY_MTM_DIFF), entry.getContractCurrency());
        // discrepency
        double poMtm = entry.getAttribute(MC_ENTRY_PO_MTM) == null ? 0 : (Double) entry.getAttribute(MC_ENTRY_PO_MTM);
        double cptyMtm = entry.getAttribute(MC_ENTRY_CPTY_MTM) == null ? 0 : (Double) entry
                .getAttribute(MC_ENTRY_CPTY_MTM);
        this.discrepencyType = computeDiscrepency(poMtm, cptyMtm);

        // limit amount and available limit left to BLANK at the moment
        // poFitchRating
        // cptyFitchRating
    }

    private String getContractNameWithoutPrefix(String contractName) {
        if (contractName.startsWith("OSLA -")) {
            return contractName.substring("OSLA -".length()).trim();
        }
        if (contractName.startsWith("ISMA -")) {
            return contractName.substring("ISMA -".length()).trim();
        }
        if (contractName.startsWith("CSA -")) {
            return contractName.substring("CSA -".length()).trim();
        }
        return contractName;
    }

    private void computeEligibleAssetType(CollateralConfig contract) {
        String eligbleAssetType = contract.getLeCollType();
        if ("BOTH".equalsIgnoreCase(eligbleAssetType)) {
            this.cptyCollateralType = "Cash/Securities";
        } else if ("CASH".equalsIgnoreCase(eligbleAssetType)) {
            this.cptyCollateralType = "Cash";
        } else {
            this.cptyCollateralType = "Securities";
        }

    }

    private void computeMarginCallCalculation(String freq, JDate processDate) {
        this.isMarginCallCalculation = this.freqHelper.getMarginCallCalculation(freq, processDate);
    }

    private void computeContractDirection(CollateralConfig contract) {
        if (CollateralConfig.NET_BILATERAL.equals(contract.getContractDirection())) {
            this.contractDirection = "None";
        } else {
            if ("ProcessingOrg".equals(contract.getSecuredParty())) {
                this.contractDirection = "Owner";
            } else {
                this.contractDirection = "Counterparty";
            }
        }
    }

    private void computeCreditRatings(CollateralConfig contract, JDate valueDate) {
        if (AMOUNT.equals(contract.getPoNewThresholdType()) && AMOUNT.equals(contract.getPoMTAType())) {
            this.poSnPRating = FIXED;
            this.poMoodyRating = FIXED;
            this.poFitchRating = FIXED;
        } else if (GLOBAL_RATING.equals(contract.getPoNewThresholdType())
                && GLOBAL_RATING.equals(contract.getPoMTAType())) {
            this.poSnPRating = getCreditRatingValue(contract.getPoId(), SNP, valueDate);
            this.poMoodyRating = getCreditRatingValue(contract.getPoId(), MOODY, valueDate);
            this.poFitchRating = getCreditRatingValue(contract.getPoId(), FITCH, valueDate);
        }

        if (AMOUNT.equals(contract.getLeNewThresholdType()) && AMOUNT.equals(contract.getLeMTAType())) {
            this.cptySnPRating = FIXED;
            this.cptyMoodyRating = FIXED;
            this.cptyFitchRating = FIXED;
        } else if (GLOBAL_RATING.equals(contract.getPoNewThresholdType())
                && GLOBAL_RATING.equals(contract.getPoMTAType())) {
            this.cptySnPRating = getCreditRatingValue(contract.getLeId(), SNP, valueDate);
            this.cptyMoodyRating = getCreditRatingValue(contract.getLeId(), MOODY, valueDate);
            this.cptyFitchRating = getCreditRatingValue(contract.getLeId(), FITCH, valueDate);
        }
    }

    private String getCreditRatingValue(final int leId, final String agency, JDate processDate) {
        JDate asOfDate = processDate;
        if (processDate == null) {
            asOfDate = JDate.getNow();
        }
        CreditRating cr = new CreditRating();
        cr.setLegalEntityId(leId);
        cr.setDebtSeniority("SENIOR_UNSECURED");
        cr.setAgencyName(agency);
        cr.setRatingType(CreditRating.CURRENT);
        cr.setAsOfDate(asOfDate);
        cr = BOCache.getLatestRating(DSConnection.getDefault(), cr);
        if (cr != null) {
            return cr.getRatingValue();
        }
        return null;
    }

    // Calypso :
    // PO MTM = is expressed in calypso from the PO point of view
    // (if negative : means Santander (the PO) has to pay. if positive : means Santander (the PO) receives).
    // CP MTM = is expressed in calypso from the PO point of view
    // (if negative : means Santander (the PO) has to pay. if positive : means Santander (the PO) receives).

    // 1 = "extreme case : both parties ask collateral". (Santander wants to receive, cpty wants to receive (in
    // * calypso : PO MtM = +100 ; Cp MtM = -100)

    // 2 = "We ask more than the cpty is ready to give) (in calypso : PO MtM =
    // * +100 ; Cp MtM = +80)

    // 3 = "We ask less than the cpty is ready to give. we are in deficit if we look at the Cp MtM.
    // * we are covered if we look at ours.
    // (in calypso : PO MtM = +100 ; Cp MtM = +120)

    // 4 = We can pay more collat than the cpty asks. we pay in excess according to CP MtM, we cover according to ours.
    // (in calypso : PO MtM = -100 ; Cp MtM = -80

    // 5 = We can pay less collateral than what our cpty asks. the cpty has a deficit of collateral.
    // (in calypso : PO MtM = -100 ; Cp MtM = -120

    // 6 = both cpties can pay collateral.
    // (in calypso : PO MtM = -100 ; Cp MtM = +100)
    private String computeDiscrepency(double poMtm, double cptyMtm) {

        if ((poMtm == 0) && (cptyMtm != 0)) {
            return "0";
        }
        if (poMtm > 0) {
            if (cptyMtm < 0) {
                return "1"; // PO MTM > 0 and CPTY MTM < 0
            }
            if (cptyMtm < poMtm) {
                return "2"; // PO MTM > 0 and CPTY MTM > 0 and CPTY MTM < PO MTM
            }
            // Here CPTY MTM > 0
            if (cptyMtm > poMtm) {
                return "3"; // PO MTM > 0 and CPTY MTM > 0 and CPTY MTM > PO MTM
            }
        } else if (poMtm < 0) {
            if (cptyMtm > 0) {
                return "6"; // PO MTM < 0 and CPTY MTM > 0
            }
            // Here CPTY MTM < 0
            if (cptyMtm < poMtm) {
                return "5"; // PO MTM < 0 and CPTY MTM < 0 and CPTY MTM < PO MTM
            }
            if (cptyMtm > poMtm) {
                return "4"; // PO MTM < 0 and CPTY MTM < 0 and CPTY MTM > PO MTM
            }
        }

        return null;

    }

    private void computeAllocations(List<MarginCallAllocation> allocations) {
        if (allocations.isEmpty()) {
            return;
        }
        Set<String> currencies = new HashSet<>();
        Set<String> eligibleAssetTypes = new HashSet<>();

        JDate newerSettleDate = null;
        for (MarginCallAllocation alloc : allocations) {
            if (alloc.isCanceled() || (alloc.getTradeId() <= 0)) {
                continue;
            }

            currencies.add(alloc.getCurrency());
            JDate settleDate = alloc.getSettlementDate();
            if (newerSettleDate == null) {
                newerSettleDate = settleDate;
            }
            if (settleDate.after(newerSettleDate)) {
                newerSettleDate = settleDate;
            }

            if (eligibleAssetTypes.size() < 2) {
                eligibleAssetTypes.add(alloc instanceof CashAllocation ? "Cash" : "Securities");
            }

        } // END FOR

        this.inputDate = newerSettleDate;
        this.effectiveCurrency = Util.collectionToString(currencies);
        if (eligibleAssetTypes.size() == 2) {
            this.effectiveAssetType = "Cash/Securities";
        } else if (eligibleAssetTypes.size() == 1) {
            this.effectiveAssetType = eligibleAssetTypes.iterator().next();
        }
    }

    private double getAmountInEUR(Double amountToConvert, String fromCCY) {
        if (amountToConvert == null) {
            return Double.NaN;
        }
        if ("EUR".equals(fromCCY)) {
            return amountToConvert;
        }
        Double amountEUR = null;
        try {
            amountEUR = CachedFXQuoteHelper.convertAmountInEUR(amountToConvert, fromCCY);
        } catch (MarketDataException e) {
            Log.error(this, "Cannot retrieve FX quote", e);
        }
        if (amountEUR == null) {
            return Double.NaN;
        }
        return amountEUR;
    }

    public Object getColumnValue(String columnName) {
        return this.columnMap.get(columnName);
    }

    private void buildMap() {
        //required for SA-CCR - GSM 17/01/2017
        this.columnMap.put(SantKPIDailyTaskReportStyle.MARGIN_CALL_CONFIG_PREFIX, this.collateralContract);
        this.columnMap.put(SantKPIDailyTaskReportStyle.CONTRACT_ID, format(this.contractId));
        this.columnMap.put(SantKPIDailyTaskReportStyle.OWNER, format(this.contractPOShortName));
        this.columnMap.put(SantKPIDailyTaskReportStyle.ID, format(this.contractNameWithoutPrefix));
        this.columnMap.put(SantKPIDailyTaskReportStyle.DESCRIPTION, format(this.contractCptyFullName));
        this.columnMap.put(SantKPIDailyTaskReportStyle.TYPE, format(this.contractType));
        this.columnMap.put(SantKPIDailyTaskReportStyle.FREQUENCY, format(this.frequency));
        this.columnMap.put(SantKPIDailyTaskReportStyle.EVENT, format(this.event));
        this.columnMap.put(SantKPIDailyTaskReportStyle.STATUS, format(this.status));
        this.columnMap.put(SantKPIDailyTaskReportStyle.MARGIN_CALL_SITUATION, format(this.marginCallSituation));
        this.columnMap.put(SantKPIDailyTaskReportStyle.EXCHANGE_DIRECTION, format(this.contractDirection));
        this.columnMap.put(SantKPIDailyTaskReportStyle.ELIGIBLE_ASSET_TYPE, format(this.cptyCollateralType));
        this.columnMap.put(SantKPIDailyTaskReportStyle.EFFECTIVE_ASSET_TYPE, format(this.effectiveAssetType));
        this.columnMap.put(SantKPIDailyTaskReportStyle.AGREEMENT_INPUT_DATE, format(this.contractStartDate));
        this.columnMap.put(SantKPIDailyTaskReportStyle.EVENT_DATE, format(this.eventDate));
        this.columnMap.put(SantKPIDailyTaskReportStyle.INPUT_DATE, format(this.inputDate));
        this.columnMap.put(SantKPIDailyTaskReportStyle.BASE_CURRENCY, format(this.contractCurrency));
        this.columnMap.put(SantKPIDailyTaskReportStyle.EFFECTIVE_CURRENCY, format(this.effectiveCurrency));
        this.columnMap.put(SantKPIDailyTaskReportStyle.OWNER_SNP_RATING, format(this.poSnPRating));
        this.columnMap.put(SantKPIDailyTaskReportStyle.OWNER_MOODYS_RATING, format(this.poMoodyRating));
        this.columnMap.put(SantKPIDailyTaskReportStyle.COUNTERPARTY_SNP_RATING, format(this.cptySnPRating));
        this.columnMap.put(SantKPIDailyTaskReportStyle.COUNTERPARTY_MOODYS_RATING, format(this.cptyMoodyRating));
        this.columnMap.put(SantKPIDailyTaskReportStyle.EXPOSURE, format(this.exposure));
        this.columnMap.put(SantKPIDailyTaskReportStyle.EXPOSURE_COUNTERPARTY, format(this.cptyMtm));
        this.columnMap.put(SantKPIDailyTaskReportStyle.INDEPENDENT, format(this.independentAmount));
        this.columnMap.put(SantKPIDailyTaskReportStyle.THRESHOLD, format(this.entryThresholdAmount));
        this.columnMap.put(SantKPIDailyTaskReportStyle.MTA, format(this.entryMTA));
        this.columnMap.put(SantKPIDailyTaskReportStyle.LAST_BALANCE, format(this.lastBalance));
        this.columnMap.put(SantKPIDailyTaskReportStyle.MARGIN_CALL, format(this.entryGlobalRequiredMargin));
        this.columnMap.put(SantKPIDailyTaskReportStyle.COLLATERAL_IN_TRANSIT, format(this.collateralInTransit));
        this.columnMap.put(SantKPIDailyTaskReportStyle.BALANCE, format(this.entryTotalPreviousMargin));
        this.columnMap.put(SantKPIDailyTaskReportStyle.DISPUTE, format(this.dispute));
        this.columnMap.put(SantKPIDailyTaskReportStyle.DISPUTE_TYPE, format(this.disputeType));
        this.columnMap.put(SantKPIDailyTaskReportStyle.DISPUTE_EXPOSURE, format(this.disputeOutOfExposure, 4));
        this.columnMap.put(SantKPIDailyTaskReportStyle.DISPUTE_DATE, format(this.disputeDate));
        this.columnMap.put(SantKPIDailyTaskReportStyle.MARGIN_CALL_CALCULATION, format(this.isMarginCallCalculation));
        this.columnMap.put(SantKPIDailyTaskReportStyle.DEALS, this.tradeNumber);
        this.columnMap.put(SantKPIDailyTaskReportStyle.DIFF_MTM, format(this.diffMtm));
        this.columnMap.put(SantKPIDailyTaskReportStyle.TIPO_DE_DISCREPANCIA, format(this.discrepencyType));
        // for later
        // this.columnMap.put(SantKPIDailyTaskReportStyle.LIMIT_AMOUNT, format(this.limitAmount));
        // this.columnMap.put(SantKPIDailyTaskReportStyle.AVAILABLE_LIMIT, format(this.availableLimit));
        this.columnMap.put(SantKPIDailyTaskReportStyle.LIMIT_AMOUNT, null);
        this.columnMap.put(SantKPIDailyTaskReportStyle.AVAILABLE_LIMIT, null);
        this.columnMap.put(SantKPIDailyTaskReportStyle.OWNER_FITCH_RATING, format(this.poFitchRating));
        this.columnMap.put(SantKPIDailyTaskReportStyle.COUNTERPARTY_FITCH_RATING, format(this.cptyFitchRating));

    }

    private Object format(Object value) {
        if (value instanceof Double) {
            return new Amount((Double) value, 2);
        }
        return value;
    }

    private Object format(Object value, int digit) {
        if (value instanceof Double) {
            return new Amount((Double) value, digit);
        }
        return value;
    }

    public Map<String, Object> getColumnMap() {
        return columnMap;
    }

    /**
     * Removes cached data. Use it when starting a new report.
     */
    public static void cleanCache() {
        CachedFXQuoteHelper.cleanCache();
    }

}


class CachedFXQuoteHelper extends FXQuoteHelper {

    private static PricingEnv pricingEnvCopy;            // we'll keep a copy because FXQuoteHelper.pricingEnvCopy is private, so we can't access to it.
    private static HashMap<String, QuoteValue> quoteValueCache = new HashMap<>();

    public CachedFXQuoteHelper(String pricingEnvName) {
        super(pricingEnvName);
    }

    public static void setPricingEnv(PricingEnv pricingEnv) {
        pricingEnvCopy = pricingEnv;
        FXQuoteHelper.setPricingEnv(pricingEnv);
    }

    public static Double convertAmountInEUR(Double value, String ccy1) throws MarketDataException {        // it is a static method, so we're not "overridding", we are "hidding" FXQuoteHelper.convertAmountInEUR().  ("hidding" means that there is no polymorphism, the class of the reference variable is used to determine the method to use).
        if ("EUR".equals(ccy1)) {
            return value;
        }
        String key = ccy1 + "|" + pricingEnvCopy.getDate();
        QuoteValue qv;
        if (quoteValueCache.containsKey(key)) {
            qv = quoteValueCache.get(key);
        } else {
            qv = pricingEnvCopy.getFXQuote(ccy1, "EUR", pricingEnvCopy.getDate());
            quoteValueCache.put(key, qv);
        }
        if ((qv == null) || (qv.getClose() == Double.NaN)) {
            return null;
        }
        return value * qv.getClose();
    }

    public static void cleanCache() {
        quoteValueCache.clear();
    }

}
