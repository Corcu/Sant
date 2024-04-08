/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util.riskparameters;

import calypsox.tk.report.SantAgreementParametersReportStyle;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.rating.impl.DefaultRatingSelector;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Vector;

public class SantRiskParameterBuilder {

    private static final String GLOBAL_RATING = "GLOBAL RATING";
    private static final String NEVER = "NEVER";
    private static final String PERCENT = "PERCENT";
    private static final String MC_PERCENT = "MC_PERCENT";
    private static final String AMOUNT = "AMOUNT";
    private static final String BOTH = "BOTH";
    private static String USE_LONG_TERM = "UseLongTerm";

    private static final String FIXED = "FIXED";

    public SantRiskParameter build(CollateralConfig contract, JDate valDate, PricingEnv pe) {
        SantRiskParameter rp = new SantRiskParameter();

        rp.setContractId(contract.getId());
        rp.setCollateralAgreement(contract.getName());
        rp.setCurrencyAgreement(contract.getCurrency());
        rp.setValDate(valDate);

        // 1- ProcessingOrg
        buildProcessingOrg(rp, contract, valDate, pe);
        // 2- Counterparty
        buildCounterparty(rp, contract, valDate, pe);

        return rp;
    }

    private void buildProcessingOrg(SantRiskParameter rp, CollateralConfig contract, JDate valDate, PricingEnv pe) {

        // 1- Rounding
        rp.setPoRounding(new Amount(contract.getPoReturnRoundingFigure(), 0).toString());
        // 2- PO Threshold
        buildPoThreshold(rp, contract, valDate, pe);
        // 3- MTA
        buildPoMta(rp, contract, valDate, pe);

    }

    private void buildCounterparty(SantRiskParameter rp, CollateralConfig contract, JDate valDate, PricingEnv pe) {
        // 1- Rounding
        rp.setCptyRounding(new Amount(contract.getLeReturnRoundingFigure(), 0).toString());
        // 2- Threshold
        buildCptyThreshold(rp, contract, valDate, pe);
        // 3- MTA
        buildCptyMta(rp, contract, valDate, pe);

    }

    private void buildPoThreshold(SantRiskParameter rp, CollateralConfig contract, JDate valDate, PricingEnv pe) {

        if (NEVER.equals(contract.getPoNewThresholdType())) {
            rp.setPoThresholdRiskLevel1(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setPoThresholdRiskLevel2(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setPoThresholdRiskLevel3(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setPoThresholdType(contract.getPoNewThresholdType());
            rp.setPoThreshold(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setPoThresholdCurrency(contract.getPoNewThresholdCurrency());
        } else if (GLOBAL_RATING.equals(contract.getPoNewThresholdType())) {

            Vector<CreditRating> crV = new Vector<>();
            CreditRating cr = getMoody(contract.getPoId(), valDate);
            if (cr != null) {
                rp.setPoThresholdRiskLevel1(cr.getRatingValue());
                crV.add(cr);
            }
            cr = getSnP(contract.getPoId(), valDate);
            if (cr != null) {
                rp.setPoThresholdRiskLevel2(cr.getRatingValue());
                crV.add(cr);
            }
            cr = getFitch(contract.getPoId(), valDate);
            if (cr != null) {
                rp.setPoThresholdRiskLevel3(cr.getRatingValue());
                crV.add(cr);
            }
            rp.setPoThresholdType(contract.getPoNewThresholdType());

            try {
                MarginCallCreditRatingConfiguration mcRatingConfiguration = CollateralUtilities
                        .getMCRatingConfiguration(contract.getPoRatingsConfigId());

                Vector<MarginCallCreditRating> mccRatings = getMarginCallCreditRating(contract, crV,
                        contract.getPoRatingsConfigId(), valDate);

                /*MarginCallCreditRating chosenOne = MarginCallCreditRating.filterPriority(mccRatings,
                        contract.getPoThresholdRatingDirection());*/
                //MIG V16 CREDIT RATING SORT
                MarginCallCreditRating chosenOne = new DefaultRatingSelector(contract.getPoThresholdRatingDirection()).selectRating(mccRatings).getRating();


                if (chosenOne != null) {
                    double amount = 0.0;
                    if (!Util.isEmpty(chosenOne.getThreshold())) {
                        amount = SantAgreementParametersReportStyle.convertThreshold(chosenOne.getThreshold());
                    }
                    double percent = chosenOne.getThresholdPercent();

                    String retThreshold = formatGlobalRatingValue(chosenOne.getThresholdType(), amount, percent);

                    rp.setPoThreshold(retThreshold);
                    rp.setPoThresholdCurrency(mcRatingConfiguration.getThresholdCurrency());
                }
            } catch (Exception e) {
                Log.error(this, e);
            }

        } else { // AMOUNT, PERCENTAGE, MC_PERCENT, BOTH
            rp.setPoThresholdRiskLevel1(FIXED);
            rp.setPoThresholdRiskLevel2(FIXED);
            rp.setPoThresholdRiskLevel3(FIXED);
            rp.setPoThresholdType(contract.getPoNewThresholdType());
            rp.setPoThresholdCurrency(contract.getPoNewThresholdCurrency());

            if (AMOUNT.equals(contract.getPoNewThresholdType())) {
                rp.setPoThreshold(new Amount(contract.getPoNewThresholdAmount(), 2).toString());
            } else if (PERCENT.equals(contract.getPoNewThresholdType())
                    || MC_PERCENT.equals(contract.getPoNewThresholdType())) {
                rp.setPoThreshold(new Amount(contract.getPoNewThresholdPercentage() * 100, 2).toString() + "%");
            } else if (BOTH.equals(contract.getPoNewThresholdType())) {
                rp.setPoThreshold(new Amount(contract.getPoNewThresholdAmount()).toString() + " / "
                        + new Amount(contract.getPoNewThresholdPercentage() * 100, 2).toString() + "%");
            }
        }
    }

    private void buildPoMta(SantRiskParameter rp, CollateralConfig contract, JDate valDate, PricingEnv pe) {

        if (NEVER.equals(contract.getPoMTAType())) {
            rp.setPoMTARiskLevel1(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setPoMTARiskLevel2(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setPoMTARiskLevel3(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setPoMTAType(contract.getPoMTAType());
            rp.setPoMTA(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setPoMTACurrency(contract.getPoMTACurrency());
        } else if (GLOBAL_RATING.equals(contract.getPoMTAType())) {
            Vector<CreditRating> crV = new Vector<CreditRating>();
            CreditRating cr = getMoody(contract.getPoId(), valDate);
            if (cr != null) {
                rp.setPoMTARiskLevel1(cr.getRatingValue());
                crV.add(cr);
            }
            cr = getSnP(contract.getPoId(), valDate);
            if (cr != null) {
                rp.setPoMTARiskLevel2(cr.getRatingValue());
                crV.add(cr);
            }
            cr = getFitch(contract.getPoId(), valDate);
            if (cr != null) {
                rp.setPoMTARiskLevel3(cr.getRatingValue());
                crV.add(cr);
            }
            rp.setPoMTAType(contract.getPoMTAType());
            Vector<MarginCallCreditRating> mccRatings = getMarginCallCreditRating(contract, crV,
                    contract.getPoRatingsConfigId(), valDate);

            /*MarginCallCreditRating chosenOne = MarginCallCreditRating.filterPriority(mccRatings,
                    contract.getPoMTARatingDirection());*/
            //MIG V16 CREDIT RATING SORT
            MarginCallCreditRating chosenOne = new DefaultRatingSelector(contract.getPoMTARatingDirection()).selectRating(mccRatings).getRating();

            if (chosenOne != null) {
                try {
                    MarginCallCreditRatingConfiguration mcRatingConfiguration = CollateralUtilities
                            .getMCRatingConfiguration(contract.getPoRatingsConfigId());


                    //GSM 11/04 Fix format number for String
                    NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
                    Number number = format.parse(chosenOne.getMta());
                    double amount = number.doubleValue();
                    double percent = chosenOne.getMtaPercent();

                    String mta = formatGlobalRatingValue(chosenOne.getMtaType(), amount, percent);

                    rp.setPoMTA(mta);
                    rp.setPoMTACurrency(mcRatingConfiguration.getMtaCurrency());
                } catch (Exception e) {
                    Log.error(this, e);
                }

            }
        } else { // AMOUNT, PERCENTAGE, MC_PERCENTAGE, BOTH
            rp.setPoMTARiskLevel1(FIXED);
            rp.setPoMTARiskLevel2(FIXED);
            rp.setPoMTARiskLevel3(FIXED);
            rp.setPoMTAType(contract.getPoMTAType());
            rp.setPoMTACurrency(contract.getPoMTACurrency());

            if (AMOUNT.equals(contract.getPoMTAType())) {
                rp.setPoMTA(new Amount(contract.getPoMTAAmount(), 2).toString());
            } else if (PERCENT.equals(contract.getPoMTAType()) || MC_PERCENT.equals(contract.getPoMTAType())) {
                rp.setPoMTA(new Amount(contract.getPoMTAPercentage() * 100, 2).toString() + "%");
            } else if (BOTH.equals(contract.getPoMTAType())) {
                rp.setPoMTA(new Amount(contract.getPoMTAAmount(), 2).toString() + " / "
                        + new Amount(contract.getPoMTAPercentage() * 100, 2).toString() + "%");
            }

        }
    }

    private void buildCptyThreshold(SantRiskParameter rp, CollateralConfig contract, JDate valDate, PricingEnv pe) {

        if (NEVER.equals(contract.getLeNewThresholdType())) {
            rp.setCptyThresholdRiskLevel1(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setCptyThresholdRiskLevel2(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setCptyThresholdRiskLevel3(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setCptyThresholdType(contract.getLeNewThresholdType());
            rp.setCptyThreshold(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setCptyThresholdCurrency(contract.getLeNewThresholdCurrency());
        } else if (GLOBAL_RATING.equals(contract.getLeNewThresholdType())) {
            Vector<CreditRating> crV = new Vector<CreditRating>();
            CreditRating cr = getMoody(contract.getLeId(), valDate);
            if (cr != null) {
                rp.setCptyThresholdRiskLevel1(cr.getRatingValue());
                crV.add(cr);
            }
            cr = getSnP(contract.getLeId(), valDate);
            if (cr != null) {
                rp.setCptyThresholdRiskLevel2(cr.getRatingValue());
                crV.add(cr);
            }
            cr = getFitch(contract.getLeId(), valDate);
            if (cr != null) {
                rp.setCptyThresholdRiskLevel3(cr.getRatingValue());
                crV.add(cr);
            }
            rp.setCptyThresholdType(contract.getLeNewThresholdType());

            try {
                MarginCallCreditRatingConfiguration mcRatingConfiguration = CollateralUtilities
                        .getMCRatingConfiguration(contract.getLeRatingsConfigId());

                Vector<MarginCallCreditRating> mccRatings = getMarginCallCreditRating(contract, crV,
                        contract.getLeRatingsConfigId(), valDate);

                /*MarginCallCreditRating chosenOne = MarginCallCreditRating.filterPriority(mccRatings,
                        contract.getLeThresholdRatingDirection());*/
                //MIG V16 CREDIT RATING SORT
                MarginCallCreditRating chosenOne = new DefaultRatingSelector(contract.getLeThresholdRatingDirection()).selectRating(mccRatings).getRating();


                if (chosenOne != null) {
                    double amount = 0.0;
                    if (!Util.isEmpty(chosenOne.getThreshold())) {
                        amount = SantAgreementParametersReportStyle.convertThreshold(chosenOne.getThreshold());
                    }
                    double percent = chosenOne.getThresholdPercent();

                    String retThreshold = formatGlobalRatingValue(chosenOne.getThresholdType(), amount, percent);

                    rp.setCptyThreshold(retThreshold);
                    rp.setCptyThresholdCurrency(mcRatingConfiguration.getThresholdCurrency());
                }
            } catch (Exception e) {
                Log.error(this, e);
            }

        } else { // AMOUNT, PERCENTAGE, MC_PERCENT, BOTH
            rp.setCptyThresholdRiskLevel1(FIXED);
            rp.setCptyThresholdRiskLevel2(FIXED);
            rp.setCptyThresholdRiskLevel3(FIXED);
            rp.setCptyThresholdType(contract.getLeNewThresholdType());
            rp.setCptyThresholdCurrency(contract.getLeNewThresholdCurrency());

            if (AMOUNT.equals(contract.getLeNewThresholdType())) {
                rp.setCptyThreshold(new Amount(contract.getLeNewThresholdAmount(), 2).toString());
            } else if (PERCENT.equals(contract.getLeNewThresholdType())
                    || MC_PERCENT.equals(contract.getLeNewThresholdType())) {
                rp.setCptyThreshold(new Amount(contract.getLeNewThresholdPercentage() * 100, 2).toString() + "%");
            } else if (BOTH.equals(contract.getLeNewThresholdType())) {
                rp.setCptyThreshold(new Amount(contract.getLeNewThresholdAmount(), 2).toString() + " / "
                        + new Amount(contract.getLeNewThresholdPercentage() * 100, 2).toString() + "%");
            }

        }

    }

    private void buildCptyMta(SantRiskParameter rp, CollateralConfig contract, JDate valDate, PricingEnv pe) {

        if (NEVER.equals(contract.getLeMTAType())) {
            rp.setCptyMTARiskLevel1(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setCptyMTARiskLevel2(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setCptyMTARiskLevel3(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setCptyMTAType(contract.getLeMTAType());
            rp.setCptyMTA(String.valueOf(Double.POSITIVE_INFINITY));
            rp.setCptyMTACurrency(contract.getLeMTACurrency());
        } else if (GLOBAL_RATING.equals(contract.getLeMTAType())) {
            Vector<CreditRating> crV = new Vector<CreditRating>();
            CreditRating cr = getMoody(contract.getLeId(), valDate);
            if (cr != null) {
                rp.setCptyMTARiskLevel1(cr.getRatingValue());
                crV.add(cr);
            }
            cr = getSnP(contract.getLeId(), valDate);
            if (cr != null) {
                rp.setCptyMTARiskLevel2(cr.getRatingValue());
                crV.add(cr);
            }
            cr = getFitch(contract.getLeId(), valDate);
            if (cr != null) {
                rp.setCptyMTARiskLevel3(cr.getRatingValue());
                crV.add(cr);
            }
            rp.setCptyMTAType(contract.getLeMTAType());

            Vector<MarginCallCreditRating> mccRatings = getMarginCallCreditRating(contract, crV,
                    contract.getLeRatingsConfigId(), valDate);
           /* MarginCallCreditRating chosenOne = MarginCallCreditRating.filterPriority(mccRatings,
                    contract.getLeMTARatingDirection());*/
            //MIG V16 CREDIT RATING SORT
            MarginCallCreditRating chosenOne = new DefaultRatingSelector(contract.getLeMTARatingDirection()).selectRating(mccRatings).getRating();

            if (chosenOne != null) {
                try {
                    MarginCallCreditRatingConfiguration mcRatingConfiguration = CollateralUtilities
                            .getMCRatingConfiguration(contract.getLeRatingsConfigId());


                    //GSM 11/04 Fix format number for String
                    NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
                    Number number = format.parse(chosenOne.getMta());
                    double amount = number.doubleValue();
                    double percent = chosenOne.getMtaPercent();

                    String mta = formatGlobalRatingValue(chosenOne.getMtaType(), amount, percent);

                    rp.setCptyMTA(mta);
                    rp.setCptyMTACurrency(mcRatingConfiguration.getMtaCurrency());
                } catch (Exception e) {
                    Log.error(this, e);
                }

            }
        } else { // AMOUNT, PERCENTAGE, MC_PERCENT, BOTH
            rp.setCptyMTARiskLevel1(FIXED);
            rp.setCptyMTARiskLevel2(FIXED);
            rp.setCptyMTARiskLevel3(FIXED);
            rp.setCptyMTAType(contract.getLeMTAType());
            rp.setCptyMTACurrency(contract.getLeMTACurrency());

            if (AMOUNT.equals(contract.getLeMTAType())) {
                rp.setCptyMTA(new Amount(contract.getLeMTAAmount(), 2).toString());
            } else if (PERCENT.equals(contract.getLeMTAType()) || MC_PERCENT.equals(contract.getLeMTAType())) {
                rp.setCptyMTA(new Amount(contract.getLeMTAPercentage() * 100, 2).toString() + "%");
            } else if (BOTH.equals(contract.getLeMTAType())) {
                rp.setCptyMTA(new Amount(contract.getLeMTAAmount(), 2).toString() + " / "
                        + new Amount(contract.getLeMTAPercentage() * 100, 2).toString() + "%");
            }

        }

    }

    private CreditRating getMoody(int leId, JDate valDate) {
        return getLECreditRating(leId, "Moody", valDate);
    }

    private CreditRating getSnP(int leId, JDate valDate) {
        return getLECreditRating(leId, "S&P", valDate);
    }

    private CreditRating getFitch(int leId, JDate valDate) {
        return getLECreditRating(leId, "Fitch", valDate);
    }

    // 4.2 Fixed method to get normal or long_term, depending on cpty attribute
    private CreditRating getLECreditRating(final int leId, final String agency, JDate valDate) {
        CreditRating cr = new CreditRating();
        cr.setLegalEntityId(leId);
        cr.setAgencyName(agency);
        cr.setRatingType(CreditRating.CURRENT);
        cr.setAsOfDate(valDate);
        // check which type of rating we have to pick, long term or normal one
        if (isLongTermRating(agency, leId)) {
            // long term
            cr.setDebtSeniority("LONG_TERM");

        } else {
            // normal
            cr.setDebtSeniority("SENIOR_UNSECURED");
        }
        cr = BOCache.getLatestRating(DSConnection.getDefault(), cr);
        return cr;
    }

    private static boolean isLongTermRating(final String agency, int leId) {

        LegalEntityAttribute useLongTerm = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, leId, "ALL",
                USE_LONG_TERM + agency);
        if (useLongTerm != null) {
            String useLongTermValue = useLongTerm.getAttributeValue();
            return !Util.isEmpty(useLongTermValue) && useLongTermValue.equals("true");
        } else {
            return false;
        }

    }

    private Vector<MarginCallCreditRating> getMarginCallCreditRating(CollateralConfig contract,
                                                                     Vector<CreditRating> ratings1, int ratingConfigId, JDate valDate) {

        Vector<CreditRating> ratings = CollateralUtilities.getEligibleAgenciesOnly(ratings1,
                contract.getEligibleAgencies());
        // Vector<String> eligibleAgencies = contract.getEligibleAgencies();

        Vector<MarginCallCreditRating> mccRatings = new Vector<MarginCallCreditRating>();
        try {
            for (CreditRating rating : ratings) {
                MarginCallCreditRating mccCreditRating = CollateralUtilities.getLatestMCCreditRating(ratingConfigId,
                        rating.getRatingValue(), rating.getAgencyName(), valDate);

                if (mccCreditRating != null) {
                    mccRatings.add(mccCreditRating);
                }
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return mccRatings;
    }

    public static String formatGlobalRatingValue(String ratingType, double amount, double percent) {
        String retVal = "";
        if (ratingType.equals("AMOUNT")) {
            retVal = formatNumber(amount);
        } else if (ratingType.equals("PERCENT") || ratingType.equals("MC_PERCENT")
                || ratingType.equals("DIRTY_PERCENT")) {
            retVal = formatNumber(percent * 100) + "%";
        } else if (ratingType.equals("BOTH")) {
            retVal = formatNumber(amount);
            retVal = retVal + " - " + formatNumber(percent * 100) + "%";
        }
        return retVal;
    }

    public static String formatNumber(final double number) {
        return Util.numberToString(number, 2, Locale.getDefault(), true);
    }

}
