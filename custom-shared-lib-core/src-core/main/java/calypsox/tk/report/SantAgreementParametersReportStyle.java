package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.SantCollateralConfigUtil;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.*;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class SantAgreementParametersReportStyle extends CollateralConfigReportStyle {

    private static final long serialVersionUID = 1L;

    public static final String ADDITIONAL_LES = "Additional LEs";
    public static final String ADDITIONAL_POS = "Additional POs";
    public static final String BALANCE_CASH = "Balance Cash";
    public static final String BALANCE_BOND = "Balance Bond";
    public static final String NO_OF_DEALS = "No Of Deals";
    public static final String ELIGIBLE_CURRENCIES = "ELIGIBLE_CURRENCIES";
    public static final String ELIGIBLE_SECURITIES = "ELIGIBLE_SECURITIES";

    public static final String ELIGIBLE_ASSET_TYPE = "Eligible Asset Type";
    public static final String EFFECTIVE_ASSET_TYPE = "Effective Asset Type";

    public static final String THRESHOLD_OWNER = "Threshold Owner";
    public static final String DELIVERYMTA_OWNER = "Delivery MTA Owner";
    public static final String RETURNMTA_OWNER = "Return MTA Owner";

    public static final String THRESHOLD_CPTY = "Threshold Cpty";
    public static final String DELIVERYMTA_CPTY = "Delivery MTA Cpty";
    public static final String RETURNMTA_CPTY = "Return MTA Cpty";
    public static final String REPORT_DATE = "Report Date";

    public static final String DELIVERY_DPLUS = "Delivery D+";
    public static final String REHYPOT_COLLAT = "Rehypothecable Collateral";

    // BAU
    public static final String EXPOSURE_TYPES = "Exposure Types";

    // add columns ACD
    public static final String IA = "Independent Amount";
    public static final String NOTIFICATION_TIME = "Notification Time";
    public static final String NOTIFICATION_TIME_ZONE = "Notification Time Zone";

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        CollateralConfig marginCall = row.getProperty(ReportRow.DEFAULT);
        MarginCallEntryDTO entryDto = row.getProperty(ReportRow.MARGIN_CALL_ENTRY);

        JDate valDate = row.getProperty("ValDate");
        PricingEnv pricingEnv = row.getProperty("PricingEnv");

        String value = SantCollateralConfigUtil.overrideBookAndContractDirectionReportColumnValue(marginCall, columnName, this);
        if (!Util.isEmpty(value)) {
            return value;
        }
        if (columnName.equals(REPORT_DATE)) {
            return row.getProperty("ProcessDate");
        } else if (columnName.equals(ADDITIONAL_POS)) {
            List<LegalEntity> additionalPOs = marginCall.getAdditionalPO();
            return getLeNamesStr(additionalPOs);
        } else if (columnName.equals(ADDITIONAL_LES)) {
            List<LegalEntity> additionalLEs = marginCall.getAdditionalLE();
            return getLeNamesStr(additionalLEs);
        } else if (columnName.equals(BALANCE_CASH)) {
            if ((entryDto != null)
                    && ("CASH".equals(row.getProperty(EFFECTIVE_ASSET_TYPE)) || "BOTH".equals(row
                    .getProperty(EFFECTIVE_ASSET_TYPE)))) {
                return formatNumber(entryDto.getPreviousCashMargin());
            }
            // if no position, shows 0
            return formatNumber(0);
        } else if (columnName.equals(BALANCE_BOND)) {
            if ((entryDto != null)
                    && ("SECURITY".equals(row.getProperty(EFFECTIVE_ASSET_TYPE)) || "BOTH".equals(row
                    .getProperty(EFFECTIVE_ASSET_TYPE)))) {
                return formatNumber(entryDto.getPreviousSecurityMargin());
            }
            // if no position, shows 0
            return formatNumber(0);
        } else if (columnName.equals(NO_OF_DEALS)) {
            return row.getProperty("TRADE_COUNT");
        } else if (columnName.equals(ELIGIBLE_CURRENCIES)) {
            List<CollateralConfigCurrency> eligibleCurrencies = marginCall.getEligibleCurrencies();
            return getCcyListStr(eligibleCurrencies);
        } else if (columnName.equals(ELIGIBLE_SECURITIES)) {
            List<String> eligibilityFilterNames = marginCall.getEligibilityFilterNames();
            return Util.collectionToString(eligibilityFilterNames);
        } else if (columnName.equals(ELIGIBLE_ASSET_TYPE)) {
            return marginCall.getPoCollType();
        } else if (columnName.equals(EFFECTIVE_ASSET_TYPE)) {
            return row.getProperty(EFFECTIVE_ASSET_TYPE);
        } else if (columnName.equals(THRESHOLD_OWNER)) {
            return getThresholdOwner(marginCall, valDate, pricingEnv);
        } else if (columnName.equals(DELIVERYMTA_OWNER)) {
            return getMTAOwner(marginCall, valDate, pricingEnv);
        } else if (columnName.equals(RETURNMTA_OWNER)) {
            return getMTAOwner(marginCall, valDate, pricingEnv);
        } else if (columnName.equals(THRESHOLD_CPTY)) {
            return getThresholdLe(marginCall, valDate, pricingEnv);
        } else if (columnName.equals(DELIVERYMTA_CPTY)) {
            return getMTALe(marginCall, valDate, pricingEnv);
        } else if (columnName.equals(RETURNMTA_CPTY)) {
            return getMTALe(marginCall, valDate, pricingEnv);
        } else if (columnName.equals(DELIVERY_DPLUS)) {
            return marginCall.getPoCashOffset();
        } else if (columnName.equals(REHYPOT_COLLAT)) {// NEW
            return isRehypotCollat(marginCall);
            // BAU
        } else if (columnName.equals(EXPOSURE_TYPES)) {
            return Util.collectionToString(getExposureTypes(marginCall));
        } else if (columnName.equals(IA)) {
            if (entryDto != null) {
                return entryDto.getIndependentAmount();
            }
            return 0;

        } else if (columnName.equals(NOTIFICATION_TIME)) {
            String time = "";

            int seconds = marginCall.getNotificationTimeSeconds();

            int day = (int) TimeUnit.SECONDS.toDays(seconds);
            long hours = TimeUnit.SECONDS.toHours(seconds) - (day * 24);
            long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
            long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);

            JDatetime notificationtime = new JDatetime(new JDate(), ((int) hours), ((int) minute), ((int) second), TimeZone.getDefault());
            SimpleDateFormat formatoFecha = new SimpleDateFormat("HH:mm");
            time = formatoFecha.format(notificationtime);

            return time;

        } else if (columnName.equals(NOTIFICATION_TIME_ZONE)) {
            if (null != marginCall.getNotificationTimeZone().getID()) {
                return marginCall.getNotificationTimeZone().getID();
            }
            return "";
        }

        return super.getColumnValue(row, columnName, errors);

    }

    private String getThresholdOwner(CollateralConfig marginCall, JDate valDate, PricingEnv pe) {
        String retThreshold = "";
        String thresholdType = marginCall.getPoNewThresholdType();

        if (thresholdType.equals("AMOUNT")) {
            double amount = marginCall.getPoNewThresholdAmount();
            retThreshold = convertToBaseAndFormat(marginCall.getCurrency(), marginCall.getPoNewThresholdCurrency(),
                    amount, valDate, pe);
        } else if (thresholdType.equals("PERCENT") || thresholdType.equals("MC_PERCENT")) {
            double amount = marginCall.getPoNewThresholdPercentage();
            retThreshold = formatNumber(amount * 100) + "%";
        } else if (thresholdType.equals("BOTH")) {
            double amount = marginCall.getPoNewThresholdAmount();
            retThreshold = convertToBaseAndFormat(marginCall.getCurrency(), marginCall.getPoNewThresholdCurrency(),
                    amount, valDate, pe);
            amount = marginCall.getPoNewThresholdPercentage();
            retThreshold = retThreshold + " - " + formatNumber(amount * 100) + "%";
        } else if (thresholdType.equals("GLOBAL RATING")) {
            try {

                MarginCallCreditRatingConfiguration mcRatingConfiguration = CollateralUtilities
                        .getMCRatingConfiguration(marginCall.getPoRatingsConfigId());
                MarginCallCreditRating creditRating = getCreditRating(marginCall, marginCall.getPoRatingsConfigId(),
                        marginCall.getProcessingOrg(), valDate, marginCall.getPoThresholdRatingDirection());
                if (creditRating != null) {
                    double amount = 0.0;
                    if (!Util.isEmpty(creditRating.getThreshold())) {
                        amount = convertThreshold(creditRating.getThreshold());
                    }
                    double percent = creditRating.getThresholdPercent();

                    retThreshold = formatGlobalRatingValue(creditRating.getThresholdType(), marginCall.getCurrency(),
                            mcRatingConfiguration.getThresholdCurrency(), amount, percent, valDate, pe);

                }

            } catch (Exception e) {
                Log.error(this, e); //sonar
                retThreshold = "ERROR RATINGS";
            }

            // retThreshold = formatNumberString(getThresholdOwner2(marginCall)); // NEW

        } else if (thresholdType.equals("NEVER")) {
            retThreshold = "NEVER";
        }
        return retThreshold;
    }

    public static String getThresholdLe(CollateralConfig marginCall, JDate valDate, PricingEnv pe) {
        String retThreshold = "";
        String thresholdType = marginCall.getLeNewThresholdType();

        if (thresholdType.equals("AMOUNT")) {
            double amount = marginCall.getLeNewThresholdAmount();
            retThreshold = convertToBaseAndFormat(marginCall.getCurrency(), marginCall.getLeNewThresholdCurrency(),
                    amount, valDate, pe);
        } else if (thresholdType.equals("PERCENT") || thresholdType.equals("MC_PERCENT")) {
            double amount = marginCall.getLeNewThresholdPercentage();
            retThreshold = formatNumber(amount * 100) + "%";
        } else if (thresholdType.equals("BOTH")) {
            double amount = marginCall.getLeNewThresholdAmount();
            retThreshold = convertToBaseAndFormat(marginCall.getCurrency(), marginCall.getLeNewThresholdCurrency(),
                    amount, valDate, pe);
            amount = marginCall.getLeNewThresholdPercentage();
            retThreshold = retThreshold + " - " + formatNumber(amount * 100) + "%";
        } else if (thresholdType.equals("GLOBAL RATING")) {
            try {
                MarginCallCreditRatingConfiguration mcRatingConfiguration = CollateralUtilities
                        .getMCRatingConfiguration(marginCall.getLeRatingsConfigId());
                MarginCallCreditRating creditRating = getCreditRating(marginCall, marginCall.getLeRatingsConfigId(),
                        marginCall.getLegalEntity(), valDate, marginCall.getLeThresholdRatingDirection());
                if (creditRating != null) {
                    double amount = 0.0;
                    if (!Util.isEmpty(creditRating.getThreshold())) {
                        amount = convertThreshold(creditRating.getThreshold());
                    }
                    double percent = creditRating.getThresholdPercent();

                    retThreshold = formatGlobalRatingValue(creditRating.getThresholdType(), marginCall.getCurrency(),
                            mcRatingConfiguration.getThresholdCurrency(), amount, percent, valDate, pe);

                }
            } catch (Exception e) {
                Log.warn(SantAgreementParametersReportStyle.class, e); //sonar
                retThreshold = "ERROR RATINGS";
            }

            // retThreshold = formatNumberString(getThresholdLe2(marginCall)); // NEW
        } else if (thresholdType.equals("NEVER")) {
            retThreshold = "NEVER";
        }
        return retThreshold;
    }

    private String getMTAOwner(CollateralConfig marginCall, JDate valDate, PricingEnv pe) {
        String retMTA = "";
        String mtaType = marginCall.getPoMTAType();

        if (mtaType.equals("AMOUNT")) {
            double amount = marginCall.getPoMTAAmount();
            retMTA = convertToBaseAndFormat(marginCall.getCurrency(), marginCall.getPoMTACurrency(), amount, valDate,
                    pe);
        } else if (mtaType.equals("PERCENT") || mtaType.equals("MC_PERCENT")) {
            double amount = marginCall.getPoMTAPercentage() * 100;
            retMTA = formatNumber(amount) + "%";
        } else if (mtaType.equals("BOTH")) {
            double amount = marginCall.getPoMTAAmount();
            retMTA = convertToBaseAndFormat(marginCall.getCurrency(), marginCall.getPoMTACurrency(), amount, valDate,
                    pe);

            amount = marginCall.getPoMTAPercentage();
            retMTA = retMTA + " - " + formatNumber(amount * 100) + "%";
        } else if (mtaType.equals("GLOBAL RATING")) {
            try {
                MarginCallCreditRatingConfiguration mcRatingConfiguration = CollateralUtilities
                        .getMCRatingConfiguration(marginCall.getPoRatingsConfigId());
                MarginCallCreditRating creditRating = getCreditRating(marginCall, marginCall.getPoRatingsConfigId(),
                        marginCall.getProcessingOrg(), valDate, marginCall.getPoMTARatingDirection());
                if (creditRating != null) {
                    double amount = 0.0;
                    if (!Util.isEmpty(creditRating.getMta())) {
                        amount = convertThreshold(creditRating.getMta());
                    }
                    double percent = 0.00;
                    percent = creditRating.getMtaPercent();

                    retMTA = formatGlobalRatingValue(creditRating.getMtaType(), marginCall.getCurrency(),
                            mcRatingConfiguration.getMtaCurrency(), amount, percent, valDate, pe);
                }
            } catch (Exception e) {
                Log.error(this, e); //sonar
                retMTA = "ERROR RATINGS";
            }


        } else if (mtaType.equals("NEVER")) {
            retMTA = "NEVER";
        }
        return retMTA;
    }

    public static String getMTALe(CollateralConfig marginCall, JDate valDate, PricingEnv pe) {
        String retMTA = "";
        String mtaType = marginCall.getLeMTAType();

        if (mtaType.equals("AMOUNT")) {
            double amount = marginCall.getLeMTAAmount();
            retMTA = convertToBaseAndFormat(marginCall.getCurrency(), marginCall.getLeMTACurrency(), amount, valDate,
                    pe);
        } else if (mtaType.equals("PERCENT") || mtaType.equals("MC_PERCENT")) {
            double amount = marginCall.getLeMTAPercentage() * 100;
            retMTA = formatNumber(amount) + "%";
        } else if (mtaType.equals("BOTH")) {
            double amount = marginCall.getLeMTAAmount();
            retMTA = convertToBaseAndFormat(marginCall.getCurrency(), marginCall.getLeMTACurrency(), amount, valDate,
                    pe);
            amount = marginCall.getLeMTAPercentage();
            retMTA = retMTA + " - " + formatNumber(amount * 100) + "%";
        } else if (mtaType.equals("GLOBAL RATING")) {
            try {
                MarginCallCreditRatingConfiguration mcRatingConfiguration = CollateralUtilities
                        .getMCRatingConfiguration(marginCall.getLeRatingsConfigId());

                MarginCallCreditRating creditRating = getCreditRating(marginCall, marginCall.getLeRatingsConfigId(),
                        marginCall.getLegalEntity(), valDate, marginCall.getLeMTARatingDirection());

                if (creditRating != null) {
                    double amount = 0.0;
                    if (!Util.isEmpty(creditRating.getMta())) {
                        amount = convertThreshold(creditRating.getMta());
                    }
                    double percent = 0.00;
                    percent = creditRating.getMtaPercent();

                    retMTA = formatGlobalRatingValue(creditRating.getMtaType(), marginCall.getCurrency(),
                            mcRatingConfiguration.getMtaCurrency(), amount, percent, valDate, pe);
                }

                // retMTA = convertToBaseAndFormat(marginCall.getCurrency(), mcRatingConfiguration.getMtaCurrency(),
                // amount, valDate, pe);
            } catch (Exception e) {
                Log.warn(SantAgreementParametersReportStyle.class, e); //sonar
                retMTA = "ERROR RATINGS";
            }

        } else if (mtaType.equals("NEVER")) {
            retMTA = "NEVER";
        }
        return retMTA;
    }

    public static String formatGlobalRatingValue(String ratingType, String baseCcy, String ratingCurrency,
                                                 double amount, double percent, JDate valDate, PricingEnv pe) {
        String retVal = "";
        if (ratingType.equals("AMOUNT")) {
            retVal = convertToBaseAndFormat(baseCcy, ratingCurrency, amount, valDate, pe);
        } else if (ratingType.equals("PERCENT") || ratingType.equals("MC_PERCENT")
                || ratingType.equals("DIRTY_PERCENT")) {
            retVal = formatNumber(percent * 100) + "%";
        } else if (ratingType.equals("BOTH")) {
            retVal = convertToBaseAndFormat(baseCcy, ratingCurrency, amount, valDate, pe);
            retVal = retVal + " - " + formatNumber(percent * 100) + "%";
        }
        return retVal;
    }

    private static String convertToBaseAndFormat(String baseCcy, String ratingCcy, double value, JDate valDate,
                                                 PricingEnv pe) {
        if (baseCcy.equals(ratingCcy)) {
            return formatNumber(value);
        }

        try {
            double convertValue = CollateralUtilities.convertCurrency(ratingCcy, value, baseCcy, valDate, pe);
            return formatNumber(convertValue);
        } catch (MarketDataException e) {
            Log.error(SantAgreementParametersReportStyle.class, e); //sonar
        }

        return "";
    }

    // NEW
    private String isRehypotCollat(CollateralConfig marginCall) {
        if (marginCall.isRehypothecable()) {
            return "TRUE";
        } else {
            return "FALSE";
        }
    }

    /**
     * Retreives the MarginCallCreditRating applicable for a Date, Direction(HIGHER/LOWRE), Counter Party.
     */
    @SuppressWarnings("unchecked")
    public static MarginCallCreditRating getCreditRating(CollateralConfig contract, int ratingConfigId, LegalEntity le,
                                                         JDate valDate, String direction) throws Exception {

        final int leID = le.getId();
        final DSConnection dsConn = DSConnection.getDefault();
        MarginCallCreditRating ret = null;
        MarginCallCreditRating mccrtFinal = null;
        int currentPriority = -1;

        Vector<CreditRating> leRatings = dsConn
                .getRemoteMarketData()
                .getRatings(
                        null,
                        "credit_rating.legal_entity_id = "
                                + leID
                                + " and credit_rating.rating_type = 'Current' "
                                + "AND as_of_date in (select max(cr2.as_of_date) from credit_rating cr2  where cr2.rating_agency_name=credit_rating.rating_agency_name "
                                + "and cr2.legal_entity_id=credit_rating.legal_entity_id and cr2.rating_type = credit_rating.rating_type "
                                + " AND as_of_date<=" + Util.date2SQLString(valDate)
                                + " ) order by trunc(updated_datetime) desc ");

        if (Util.isEmpty(leRatings)) {
            return null;
        }

        leRatings = CollateralUtilities.getEligibleAgenciesOnly(leRatings, contract.getEligibleAgencies());

        for (CreditRating leRating : leRatings) {

            MarginCallCreditRating mccCreditRating = CollateralUtilities.getLatestMCCreditRating(ratingConfigId,
                    leRating.getRatingValue(), leRating.getAgencyName(), valDate);

            if (mccCreditRating == null) {
                continue;
            }

            // initial case
            if (currentPriority == -1) {
                mccrtFinal = mccCreditRating;
                currentPriority = mccCreditRating.getPriority();
                // rest of iterations
            } else {
                // same priority
                if (mccrtFinal.getPriority() == mccCreditRating.getPriority()) {
                    continue;
                } else {
                    // different priority
                    if ("HIGHER".equals(direction)) {
                        if (mccCreditRating.getPriority() < currentPriority) {
                            mccrtFinal = mccCreditRating;
                            currentPriority = mccCreditRating.getPriority();
                        }
                    }
                    if ("LOWER".equals(direction)) {
                        if (mccCreditRating.getPriority() > currentPriority) {
                            mccrtFinal = mccCreditRating;
                            currentPriority = mccCreditRating.getPriority();
                        }
                    }
                }
            }
        }
        ret = mccrtFinal;

        return ret;
    }

    private String getLeNamesStr(List<LegalEntity> list) {
        String str = "";
        for (LegalEntity le : list) {
            if (str.length() > 0) {
                str += ",";
            }
            str += le.getCode();
        }
        return str;
    }

    private String getCcyListStr(List<CollateralConfigCurrency> list) {
        String str = "";
        for (CollateralConfigCurrency mcCcy : list) {
            if (str.length() > 0) {
                str += ",";
            }
            str += mcCcy.getCurrency();
        }
        return str;
    }

    public static String formatNumber(final double number) {

        return Util.numberToString(number, 2, Locale.getDefault(), true);

    }

    /**
     * Unfortunately - threshold is saved in DB as a string But can be formatted with a Locale
     *
     * @param number
     * @return
     */
    @SuppressWarnings("unused")
    private static String formatThreshold(final String number) {
        Double threshold = convertThreshold(number);

        return formatNumber(threshold);
    }

    /**
     * Updated for v16
     *
     * @param number
     * @return
     */
    public static Double convertThreshold(final String number) {
        Double threshold = null;
        try {
            threshold = Util.stringToNumberThrowException(number, Locale.ENGLISH, true);
        } catch (Exception e) {
            Log.error(SantAgreementParametersReportStyle.class, e); //sonar
            try {
                threshold = Util.stringToNumberThrowException(number, Locale.getDefault(), false);
            } catch (Exception e1) {
                Log.warn(SantAgreementParametersReportStyle.class, e1); //sonar
                try {
                    threshold = Util.stringToNumberThrowException(number, Locale.ENGLISH, true);
                } catch (Exception e2) {
                    Log.warn(SantAgreementParametersReportStyle.class, e2); //sonar
                    try {
                        threshold = Util.stringToNumberThrowException(number, Locale.ENGLISH, false);
                    } catch (Exception e3) {
                        threshold = Double.valueOf(number);
                        Log.info(SantAgreementParametersReportStyle.class, e3); //sonar
                    }
                }
            }
        }
        return threshold;
    }


    // BAU
    private List<String> getExposureTypes(CollateralConfig mcc) {
        Vector<String> elegibleCollat = mcc.getProductList();
        if (!Util.isEmpty(elegibleCollat)) {
            // If collatExposure, get subtypes list
            if (elegibleCollat.get(0).equals(CollateralExposure.PRODUCT_TYPE)) {
                return mcc.getExposureTypeList();
            }
        }
        return null;

    }

    @Override
    public boolean containsPricingEnvDependentColumns(ReportTemplate template) {
        return true;
    }
}
