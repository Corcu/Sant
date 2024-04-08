package calypsox.tk.regulation;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.marketdata.rating.impl.DefaultRatingSelector;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Class with all necessary stuff to manage ELBE & KGR contracts files
 *
 * @author David Porras Mart?nez
 */
public class CreditRatingUtilsRegulation {

    private static final String FITCH = "Fitch";
    private static final String SYP = "S&P";
    private static final String MOODY = "Moody";
    private static final String BLANK = "";
    private static final String IA_AMOUNT = "CONTRACT_INDEPENDENT_AMOUNT";
    private static final String NONE = "NONE";
    private static final String CONTRACT_IA_CCY = "CONTRACT_IA_CCY";
    private static String USE_LONG_TERM = "UseLongTerm";

    // *** Rating stuff *** //

    // Get creditRatings for given agencies an date
    public static Vector<CreditRating> getCreditRatingsForLE(
            CollateralConfig marginCall, Vector<String> agencies, int leID,
            JDate date) {

        Vector<CreditRating> ownerCreditRatings = new Vector<CreditRating>();
        MarginCallCreditRatingConfiguration mccRatingConfig = null;
        String ratingType = null;

        // get po rating matrix to get rating type we need
        try {
            mccRatingConfig = CollateralUtilities
                    .getMCRatingConfiguration(marginCall.getPoRatingsConfigId());
        } catch (Exception e) {
            Log.error(
                    "Cannot get PO ratingMatrix for contract = "
                            + marginCall.getName(), e);
            return null;
        }

        // check po rating matrix & rating type
        if (mccRatingConfig == null) {
            return null;
        }
        if (Util.isEmpty(mccRatingConfig.getRatingType())) {
            return null;
        } else {
            ratingType = mccRatingConfig.getRatingType();
        }

        if (Util.isEmpty(agencies)) {
            // moody
            CreditRating ratingMoody = CreditRatingUtilsRegulation.getMoody(
                    ratingType, leID, date);
            if (ratingMoody != null) {
                ownerCreditRatings.add(ratingMoody);
            }
            // sp
            CreditRating ratingSP = CreditRatingUtilsRegulation.getSnP(
                    ratingType, leID, date);
            if (ratingSP != null) {
                ownerCreditRatings.add(ratingSP);
            }
            // fitch
            CreditRating ratingFitch = CreditRatingUtilsRegulation.getFitch(
                    ratingType, leID, date);
            if (ratingFitch != null) {
                ownerCreditRatings.add(ratingFitch);
            }
        } else {
            for (String agency : agencies) {
                if (agency.equals(MOODY)) {
                    CreditRating ratingMoody = CreditRatingUtilsRegulation
                            .getMoody(ratingType, leID, date);
                    if (ratingMoody != null) {
                        ownerCreditRatings.add(ratingMoody);
                    }
                }
                if (agency.equals(SYP)) {
                    CreditRating ratingSP = CreditRatingUtilsRegulation.getSnP(
                            ratingType, leID, date);
                    if (ratingSP != null) {
                        ownerCreditRatings.add(ratingSP);
                    }
                }
                if (agency.equals(FITCH)) {
                    CreditRating ratingFitch = CreditRatingUtilsRegulation
                            .getFitch(ratingType, leID, date);
                    if (ratingFitch != null) {
                        ownerCreditRatings.add(ratingFitch);
                    }
                }
            }
        }

        return ownerCreditRatings;

    }

    // Get creditRatings for given agencies an date
    public static Vector<CreditRating> getCreditRatingsForLE(
            CollateralConfig marginCall, Vector<String> agencies, int leID,
            JDate date, String ratingType) {

        Vector<CreditRating> ownerCreditRatings = new Vector<CreditRating>();

        if (Util.isEmpty(ratingType)) {
            return null;
        }

        if (Util.isEmpty(agencies)) {
            // moody
            CreditRating ratingMoody = CreditRatingUtilsRegulation.getMoody(
                    ratingType, leID, date);
            if (ratingMoody != null) {
                ownerCreditRatings.add(ratingMoody);
            }
            // sp
            CreditRating ratingSP = CreditRatingUtilsRegulation.getSnP(
                    ratingType, leID, date);
            if (ratingSP != null) {
                ownerCreditRatings.add(ratingSP);
            }
            // fitch
            CreditRating ratingFitch = CreditRatingUtilsRegulation.getFitch(
                    ratingType, leID, date);
            if (ratingFitch != null) {
                ownerCreditRatings.add(ratingFitch);
            }
        } else {
            for (String agency : agencies) {
                if (agency.equals(MOODY)) {
                    CreditRating ratingMoody = CreditRatingUtilsRegulation
                            .getMoody(ratingType, leID, date);
                    if (ratingMoody != null) {
                        ownerCreditRatings.add(ratingMoody);
                    }
                }
                if (agency.equals(SYP)) {
                    CreditRating ratingSP = CreditRatingUtilsRegulation.getSnP(
                            ratingType, leID, date);
                    if (ratingSP != null) {
                        ownerCreditRatings.add(ratingSP);
                    }
                }
                if (agency.equals(FITCH)) {
                    CreditRating ratingFitch = CreditRatingUtilsRegulation
                            .getFitch(ratingType, leID, date);
                    if (ratingFitch != null) {
                        ownerCreditRatings.add(ratingFitch);
                    }
                }
            }
        }

        return ownerCreditRatings;

    }

    // Get list of ratings (globalRating) for a specified agency
    public static GlobalRating getAgencyGlobalRating(
            GlobalRatingConfiguration globalRatingConfig, String agency) {
        List<GlobalRating> globalRatings = globalRatingConfig.getGlobalRating(
                CreditRating.CURRENT, agency, CreditRating.ANY);
        if (Util.isEmpty(globalRatings)) {
            Log.info(null, "No Rating exists for Agency=" + agency);
            return null;
        } else {
            return globalRatings.get(0);
        }
    }

    // Get Moody rating for a legalEntity
    public static CreditRating getMoody(String ratingType, int leId,
                                        JDate valDate) {
        return getLEAgencyCreditRating(ratingType, leId, MOODY, valDate);
    }

    // Get SP rating for a legalEntity
    public static CreditRating getSnP(String ratingType, int leId, JDate valDate) {
        return getLEAgencyCreditRating(ratingType, leId, SYP, valDate);
    }

    // Get Fitch rating for a legalEntity
    public static CreditRating getFitch(String ratingType, int leId,
                                        JDate valDate) {
        return getLEAgencyCreditRating(ratingType, leId, FITCH, valDate);
    }

    // Get agency rating for a specified agency and legalEntity
    public static CreditRating getLEAgencyCreditRating(String ratingType,
                                                       final int leId, final String agency, JDate valDate) {

        CreditRating cr = new CreditRating();
        cr.setLegalEntityId(leId);
        cr.setAgencyName(agency);
        cr.setRatingType(ratingType);
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

        LegalEntityAttribute useLongTerm = BOCache.getLegalEntityAttribute(
                DSConnection.getDefault(), 0, leId, "ALL", USE_LONG_TERM
                        + agency);
        if (useLongTerm != null) {
            String useLongTermValue = useLongTerm.getAttributeValue();
            return !Util.isEmpty(useLongTermValue)
                    && useLongTermValue.equals("true");
        } else {
            return false;
        }

    }

    // Get agency priority for a given rating value
    public static int getAgencyRatingPriority(GlobalRating globalRating,
                                              String ratingValue) {
        List<Integer> priorities = globalRating.getPriorityOf(ratingValue);
        if (Util.isEmpty(priorities)) {
            return -1;
        }
        return priorities.get(0);
    }

    // Get rating value for a given agency priority
    public static String getAgencyRatingValue(GlobalRating globalRating,
                                              int priority) {
        GlobalRatingValue globalRatingValue = globalRating
                .getGlobalRatingValue(priority);
        if (globalRatingValue != null) {
            return globalRatingValue.getValue();
        }
        return null;
    }

    // For given creditRatings, look for marginCallCreditRatings in ratingMatrix
    // indicated by ratingsConfigId and get
    // the
    // higher/lower depending on ratingDirection
    public static MarginCallCreditRating getMccrt(
            Vector<CreditRating> creditRatings, int ratingsConfigId,
            String ratingDirection, JDate date) {

        if (!Util.isEmpty(ratingDirection)) {
            Vector<MarginCallCreditRating> mccRatings = getMarginCallCreditRatings(
                    creditRatings, ratingsConfigId, date);
            //MIG V16 CREDIT RATING SORT
            return new DefaultRatingSelector(ratingDirection).selectRating(mccRatings).getRating();
			/*return MarginCallCreditRating.filterPriority(mccRatings,
					ratingDirection);*/
        } else {
            return null;
        }
    }

    // Get marginCallCreditRatings related to creditRatings given from
    // ratingMatrix indicated by ratingConfigId
    public static Vector<MarginCallCreditRating> getMarginCallCreditRatings(
            Vector<CreditRating> ratings, int ratingConfigId, JDate valDate) {
        Vector<MarginCallCreditRating> mccRatings = new Vector<MarginCallCreditRating>();
        try {
            for (CreditRating rating : ratings) {
                MarginCallCreditRating mccCreditRating = CollateralUtilities
                        .getLatestMCCreditRating(ratingConfigId,
                                rating.getRatingValue(),
                                rating.getAgencyName(), valDate);

                if (mccCreditRating != null) {
                    mccRatings.add(mccCreditRating);
                }
            }
        } catch (Exception e) {
            Log.error(CreditRatingUtilsRegulation.class, e); //sonar
        }
        return mccRatings;
    }

    // *** Threshold/MTA/IA dependencies on *** //

    // Check if owner threshold depends on type recieved
    public static boolean isThresholdDependingOn(CollateralConfig marginCall,
                                                 String type) {
        final String value = marginCall.getPoNewThresholdType();
        if (!Util.isEmpty(value)) {
            if (value.equals(type)) {
                return true;
            }
        }
        return false;
    }

    // Check if cpty threshold depends on type recieved
    public static boolean isCptyThresholdDependingOn(
            CollateralConfig marginCall, String type) {
        final String value = marginCall.getLeNewThresholdType();
        if (!Util.isEmpty(value)) {
            if (value.equals(type)) {
                return true;
            }
        }
        return false;
    }

    // Check if owner IA depends on type recieved
    public static boolean isIADependingOnRating(CollateralConfig marginCall) {
        if (!Util.isEmpty(marginCall.getPoIARatingDirection())
                && !marginCall.getPoIARatingDirection().equals(NONE)) {
            return true;
        } else {
            return false;
        }
    }

    // Check if cpty IA depends on type recieved
    public static boolean isCptyIADependingOnRating(CollateralConfig marginCall) {
        if (!Util.isEmpty(marginCall.getLeIARatingDirection())
                && !marginCall.getLeIARatingDirection().equals(NONE)) {
            return true;
        } else {
            return false;
        }
    }

    // Check if owner MTA depends on type recieved
    public static boolean isMTADependingOn(CollateralConfig marginCall,
                                           String type) {
        final String value = marginCall.getPoMTAType();
        if (!Util.isEmpty(value)) {
            if (value.equals(type)) {
                return true;
            }
        }
        return false;
    }

    // Check if cpty MTA depends on type recieved
    public static boolean isCptyMTADependingOn(CollateralConfig marginCall,
                                               String type) {
        final String value = marginCall.getLeMTAType();
        if (!Util.isEmpty(value)) {
            if (value.equals(type)) {
                return true;
            }
        }
        return false;
    }

    // *** Owner/Cpty dependencies on *** //

    // Check if KGR contract owner depends on rating
    public static boolean isOwnerKGRcontractDependingOnRating(
            CollateralConfig marginCall) {
        return isThresholdDependingOn(marginCall,
                CollateralConfig.GLOBAL_RATING)
                || isIADependingOnRating(marginCall)
                || isMTADependingOn(marginCall, CollateralConfig.GLOBAL_RATING);
    }

    // Check if KGR contract cpty depends on rating
    public static boolean isCptyKGRcontractDependingOnRating(
            CollateralConfig marginCall) {
        return isCptyThresholdDependingOn(marginCall,
                CollateralConfig.GLOBAL_RATING)
                || isCptyIADependingOnRating(marginCall)
                || isCptyMTADependingOn(marginCall,
                CollateralConfig.GLOBAL_RATING);
    }

    // Check if KGR contract depends on rating
    public static boolean isKGRcontractDependingOnRating(
            CollateralConfig marginCall) {
        return isOwnerKGRcontractDependingOnRating(marginCall)
                || isCptyKGRcontractDependingOnRating(marginCall);
    }

    // Check if ELBE contract owner depends on rating
    public static boolean isOwnerELBEcontractDependingOnRating(
            CollateralConfig marginCall) {
        return isThresholdDependingOn(marginCall,
                CollateralConfig.GLOBAL_RATING)
                || isIADependingOnRating(marginCall);
    }

    // Cheak if ELBE contract cpty depends on rating
    public static boolean isCptyELBEcontractDependingOnRating(
            CollateralConfig marginCall) {
        return isCptyIADependingOnRating(marginCall);
    }

    // Check if ELBE contract depends on rating
    public static boolean isELBEcontractDependingOnRating(
            CollateralConfig marginCall) {
        return isOwnerELBEcontractDependingOnRating(marginCall)
                || isCptyELBEcontractDependingOnRating(marginCall);
    }

    // *** CollateralManager Contract Info *** //

    // Get total exposure from contract and date
    public static double getMccTotalExposure(final CollateralConfig marginCall,
                                             JDate date) {

        final List<Integer> mccID = new ArrayList<Integer>();
        mccID.add(marginCall.getId());
        double total = 0.00;
        try {
            // if there was any movement, we have one entry per contract/date
            final List<MarginCallEntryDTO> entries = CollateralManagerUtil
                    .loadMarginCallEntriesDTO(mccID, date);
            if ((entries != null) && (entries.size() > 0)) {
                total = entries.get(0).getNetBalance();
            }
        } catch (final RemoteException e) {
            Log.error("Cannot get total exposure from contract", e);
        }
        return total;

    }

    // Get total nominal from contract and date (pick all trades linked to
    // contract and sum principalAmounts)
    public static double getMccTotalNominal(final CollateralConfig marginCall,
                                            JDate date) {

        final List<Integer> mccID = new ArrayList<Integer>();
        mccID.add(marginCall.getId());
        double total = 0.00;
        try {
            // if there was any movement, we have one entry per contract/date
            final List<MarginCallEntryDTO> entries = CollateralManagerUtil
                    .loadMarginCallEntriesDTO(mccID, date);
            if (!Util.isEmpty(entries)) {
                List<MarginCallDetailEntryDTO> detailEntries = entries.get(0)
                        .getDetailEntries();
                if (!Util.isEmpty(detailEntries)) {
                    for (MarginCallDetailEntryDTO detailEntry : detailEntries) {
                        total += detailEntry.getPrincipal();
                    }
                }
            }
        } catch (final RemoteException e) {
            Log.error("Cannot get total nominal from contract", e);
        }
        return total;

    }

    // *** Threshold management stuff *** //

    // Get threshold depending on rating: for a given marginCallCreditRating,
    // check threshold type and get threshold
    // properly
    public static double getThresholdDependingOnRating(
            CollateralConfig marginCall,
            MarginCallCreditRating mccCreditRating, int ratingsConfigId,
            JDate processDate, JDate valueDate) {

        if (mccCreditRating != null) {
            // check threshold type
            if (mccCreditRating.getThresholdType().equals(
                    CollateralConfig.AMOUNT)) {
                return CreditRatingUtilsRegulation
                        .getThresholdAmountFromMccMatrix(ratingsConfigId,
                                mccCreditRating, marginCall.getCurrency(),
                                valueDate);
            }
            // MIG_V14
            // if
            // (mccCreditRating.getThresholdType().equals(CollateralConfig.MC_PERCENT))
            // {
            // return
            // CreditRatingUtilsRegulation.getThresholdMCPercentFromMccMatrix(marginCall,
            // mccCreditRating, processDate);
            // }
            if (mccCreditRating.getThresholdType().equals(
                    CollateralConfig.PERCENT)) {
                return CreditRatingUtilsRegulation
                        .getThresholdPercentFromMccMatrix(marginCall,
                                mccCreditRating, processDate);
            }
            if (mccCreditRating.getThresholdType()
                    .equals(CollateralConfig.BOTH)) {
                double thresholdAmount = CreditRatingUtilsRegulation
                        .getThresholdAmountFromMccMatrix(ratingsConfigId,
                                mccCreditRating, marginCall.getCurrency(),
                                valueDate);
                double thresholdPercent = CreditRatingUtilsRegulation
                        .getThresholdPercentFromMccMatrix(marginCall,
                                mccCreditRating, processDate);
                if (thresholdAmount < thresholdPercent) {
                    return thresholdAmount;
                } else {
                    return thresholdPercent;
                }
            }
        }
        return 0.00;

    }

    // Get threshold depending on amount: get threshold amount specified on
    // legalEntity side of contract
    public static double getThresholdDependingOnAmount(
            CollateralConfig marginCall, String legalEntity, JDate date) {

        double thresholdAmount = 0.00;
        String thresholdCcy = BLANK;

        if (legalEntity.equals("PO")) { // ProcessingOrg
            thresholdAmount = marginCall.getPoNewThresholdAmount();
            thresholdCcy = marginCall.getPoNewThresholdCurrency();
        } else { // Counterparty
            thresholdAmount = marginCall.getLeNewThresholdAmount();
            thresholdCcy = marginCall.getLeNewThresholdCurrency();
        }

        return thresholdAmount
                * CollateralUtilities.getFXRate(date, thresholdCcy,
                marginCall.getCurrency());

    }

    // Get threshold depending on mc_percent: get threshold percentage specified
    // on legalEntity side of contract and
    // multiply it by contract totalExposure
    public static double getThresholdDependingOnMcPercent(
            CollateralConfig marginCall, String legalEntity, JDate date) {

        double percentage = 0.00;

        if (legalEntity.equals("PO")) { // ProcessingOrg
            percentage = marginCall.getPoNewThresholdPercentage();
        } else { // Counterparty
            percentage = marginCall.getLeNewThresholdPercentage();
        }
        double totalExposure = CreditRatingUtilsRegulation.getMccTotalExposure(
                marginCall, date);

        return totalExposure * percentage;

    }

    // Get threshold depending on percent: get threshold percentage specified on
    // legalEntity side of contract and
    // multiply it by contract totalNominal
    public static double getThresholdDependingOnPercent(
            CollateralConfig marginCall, String legalEntity, JDate date) {

        double percentage = 0.00;

        if (legalEntity.equals("PO")) { // ProcessingOrg
            percentage = marginCall.getPoNewThresholdPercentage();
        } else { // Counterparty
            percentage = marginCall.getLeNewThresholdPercentage();
        }
        double totalNominal = CreditRatingUtilsRegulation.getMccTotalNominal(
                marginCall, date);

        return totalNominal * percentage;

    }

    // Get threshold depending on both: get contract threshold amount and
    // contract threshold percent value, compare
    // them, and return lower one
    public static double getThresholdDependingOnBoth(
            CollateralConfig marginCall, String legalEntity, JDate processDate,
            JDate valueDate) {

        double percentage = 0.00;
        double thresholdAmount = 0.00;

        if (legalEntity.equals("PO")) { // ProcessingOrg
            thresholdAmount = marginCall.getPoNewThresholdAmount()
                    * CollateralUtilities.getFXRate(valueDate,
                    marginCall.getPoNewThresholdCurrency(),
                    marginCall.getCurrency());
            percentage = marginCall.getPoNewThresholdPercentage();
        } else { // Counterparty
            thresholdAmount = marginCall.getLeNewThresholdAmount()
                    * CollateralUtilities.getFXRate(valueDate,
                    marginCall.getLeNewThresholdCurrency(),
                    marginCall.getCurrency());
            percentage = marginCall.getLeNewThresholdPercentage();
        }

        double totalNominal = CreditRatingUtilsRegulation.getMccTotalNominal(
                marginCall, processDate);
        double percentValue = totalNominal * percentage;

        // get lower
        if (thresholdAmount < percentValue) {
            return thresholdAmount;
        } else {
            return percentValue;
        }

    }

    // Get threshold amount from marginCallCreditRating
    public static double getThresholdAmountFromMccMatrix(int ratingsConfigId,
                                                         MarginCallCreditRating mccCreditRating, String baseCcy, JDate date) {

        MarginCallCreditRatingConfiguration mcRatingConfiguration;
        try {
            mcRatingConfiguration = CollateralUtilities
                    .getMCRatingConfiguration(ratingsConfigId);
            if (mcRatingConfiguration != null) {
                double thresholdValue = new Amount(
                        mccCreditRating.getThreshold(), 2).get();
                String thresholdCcy = mcRatingConfiguration
                        .getThresholdCurrency();
                return thresholdValue
                        * CollateralUtilities.getFXRate(date, thresholdCcy,
                        baseCcy);
            }
        } catch (Exception e) {
            Log.error(CreditRatingUtilsRegulation.class, e); //sonar
        }
        return 0.00;

    }

    // Get threshold mc_percent value from marginCallCreditRating
    public static double getThresholdMCPercentFromMccMatrix(
            final CollateralConfig marginCall,
            final MarginCallCreditRating mccCreditRating, JDate date) {

        double totalExposure = getMccTotalExposure(marginCall, date);
        double percentage = mccCreditRating.getThresholdPercent();
        return totalExposure * percentage;

    }

    // Get threshold percent value from marginCallCreditRating
    public static double getThresholdPercentFromMccMatrix(
            final CollateralConfig marginCall,
            final MarginCallCreditRating mccCreditRating, JDate date) {

        double totalNominal = getMccTotalNominal(marginCall, date);
        double percentage = mccCreditRating.getThresholdPercent();
        return totalNominal * percentage;

    }

    // *** Ind. Amount management stuff *** //

    // Get IA depending on rating: for a given marginCallCreditRating, check IA
    // type and get IA
    // properly
    public static double getIndAmountDependingOnRating(
            CollateralConfig marginCall,
            MarginCallCreditRating mccCreditRating, int ratingsConfigId,
            JDate processDate, JDate valueDate) {

        if (mccCreditRating != null) {
            // check IA type
            if (mccCreditRating.getIaType().equals(CollateralConfig.AMOUNT)) {
                return getIaAmountFromMccMatrix(ratingsConfigId,
                        mccCreditRating, marginCall.getCurrency(), valueDate);
            }
            if (mccCreditRating.getIaType().equals("MC_PERCENT")) {
                return getIaMCPercentFromMccMatrix(marginCall, mccCreditRating,
                        processDate);
            }
            if (mccCreditRating.getIaType().equals("PERCENT")) {
                return getIaPercentFromMccMatrix(marginCall, mccCreditRating,
                        processDate);
            }
            if (mccCreditRating.getIaType().equals("BOTH")) {
                double IaAmount = getIaAmountFromMccMatrix(ratingsConfigId,
                        mccCreditRating, marginCall.getCurrency(), valueDate);
                double IaPercent = getIaPercentFromMccMatrix(marginCall,
                        mccCreditRating, processDate);
                if (IaAmount < IaPercent) {
                    return IaAmount;
                } else {
                    return IaPercent;
                }
            }
        }
        return 0.00;

    }

    // Get IA amount from marginCallCreditRating
    public static double getIaAmountFromMccMatrix(int ratingsConfigId,
                                                  MarginCallCreditRating mccCreditRating, String baseCcy, JDate date) {
        MarginCallCreditRatingConfiguration mcRatingConfiguration;
        try {
            mcRatingConfiguration = CollateralUtilities
                    .getMCRatingConfiguration(ratingsConfigId);
            if (mcRatingConfiguration != null) {
                double iaValue = Double.valueOf(mccCreditRating
                        .getIndependentAmount());
                String iaCcy = mcRatingConfiguration.getIaCurrency();
                return iaValue
                        * CollateralUtilities.getFXRate(date, iaCcy, baseCcy);
            }
        } catch (Exception e) {
            Log.error(CreditRatingUtilsRegulation.class, e); //sonar
        }
        return 0.00;
    }

    // Get IA mc_percent value from marginCallCreditRating
    public static double getIaMCPercentFromMccMatrix(
            final CollateralConfig marginCall,
            final MarginCallCreditRating mccCreditRating, JDate date) {

        double totalExposure = getMccTotalExposure(marginCall, date);
        double percentage = mccCreditRating.getIaPercent();
        return Math.abs(totalExposure * percentage);

    }

    // Get IA percent value from marginCallCreditRating
    public static double getIaPercentFromMccMatrix(
            final CollateralConfig marginCall,
            final MarginCallCreditRating mccCreditRating, JDate date) {

        double totalNominal = getMccTotalNominal(marginCall, date);
        double percentage = mccCreditRating.getIaPercent();
        return totalNominal * percentage;

    }

    // Get IA fixed on contract additional info (when not depends on rating)
    public static double getContractIA(final CollateralConfig marginCall) {
        return new Amount(marginCall.getAdditionalField(IA_AMOUNT), 2).get();

    }

    // Get IA currency fixed on contract additional info (when not depends on
    // rating)
    public static String getContractIAccy(CollateralConfig marginCall) {
        String ccy = marginCall.getAdditionalField(CONTRACT_IA_CCY);
        if (!Util.isEmpty(ccy)) {
            return ccy;
        } else {
            return BLANK;
        }
    }

    // *** Mta stuff *** //

    // Get MTA depending on rating: for a given marginCallCreditRating, check
    // MTA type and get MTA
    // properly
    public static double getMtaDependingOnRating(CollateralConfig marginCall,
                                                 MarginCallCreditRating mccCreditRating, int ratingsConfigId,
                                                 JDate processDate, JDate valueDate) {

        if (mccCreditRating != null) {
            // check threshold type
            if (mccCreditRating.getMtaType().equals(CollateralConfig.AMOUNT)) {
                return CreditRatingUtilsRegulation.getMtaAmountFromMccMatrix(
                        ratingsConfigId, mccCreditRating,
                        marginCall.getCurrency(), valueDate);
            }
            // MIG_V14
            // if
            // (mccCreditRating.getMtaType().equals(CollateralConfig.MC_PERCENT))
            // {
            // return
            // CreditRatingUtilsRegulation.getMtaMCPercentFromMccMatrix(marginCall,
            // mccCreditRating, processDate);
            // }
            if (mccCreditRating.getMtaType().equals(CollateralConfig.PERCENT)) {
                return CreditRatingUtilsRegulation.getMtaPercentFromMccMatrix(
                        marginCall, mccCreditRating, processDate);
            }
            if (mccCreditRating.getMtaType().equals(CollateralConfig.BOTH)) {
                double mtaAmount = CreditRatingUtilsRegulation
                        .getMtaAmountFromMccMatrix(ratingsConfigId,
                                mccCreditRating, marginCall.getCurrency(),
                                valueDate);
                double mtaPercent = CreditRatingUtilsRegulation
                        .getMtaPercentFromMccMatrix(marginCall,
                                mccCreditRating, processDate);
                if (mtaAmount < mtaPercent) {
                    return mtaAmount;
                } else {
                    return mtaPercent;
                }
            }
        }
        return 0.00;

    }

    // Get MTA depending on amount: get MTA amount specified on legalEntity side
    // of contract
    public static double getMtaDependingOnAmount(CollateralConfig marginCall,
                                                 String legalEntity, JDate date) {

        double mtaAmount = 0.00;
        String mtaCcy = BLANK;

        if (legalEntity.equals("PO")) { // ProcessingOrg
            mtaAmount = marginCall.getPoMTAAmount();
            mtaCcy = marginCall.getPoMTACurrency();
        } else { // Counterparty
            mtaAmount = marginCall.getLeMTAAmount();
            mtaCcy = marginCall.getLeMTACurrency();
        }

        return mtaAmount
                * CollateralUtilities.getFXRate(date, mtaCcy,
                marginCall.getCurrency());

    }

    // Get MTA depending on mc_percent: get MTA mc_percent value specified on
    // legalEntity side of contract
    public static double getMtaDependingOnMcPercent(
            CollateralConfig marginCall, String legalEntity, JDate date) {

        double percentage = 0.00;

        if (legalEntity.equals("PO")) { // ProcessingOrg
            percentage = marginCall.getPoMTAPercentage();
        } else { // Counterparty
            percentage = marginCall.getLeMTAPercentage();
        }
        double totalExposure = getMccTotalExposure(marginCall, date);

        return totalExposure * percentage;

    }

    // Get MTA depending on percent: get MTA percent value specified on
    // legalEntity side of contract
    public static double getMtaDependingOnPercent(CollateralConfig marginCall,
                                                  String legalEntity, JDate date) {

        double percentage = 0.00;

        if (legalEntity.equals("PO")) { // ProcessingOrg
            percentage = marginCall.getPoMTAPercentage();
        } else { // Counterparty
            percentage = marginCall.getLeMTAPercentage();
        }
        double totalExposure = getMccTotalNominal(marginCall, date);

        return totalExposure * percentage;

    }

    // Get MTA depending on both: get contract MTA amount and contract MTA
    // percent value, compare
    // them, and return lower one
    public static double getMtaDependingOnBoth(CollateralConfig marginCall,
                                               String legalEntity, JDate processDate, JDate valueDate) {

        double percentage = 0.00;
        double mtaAmount = 0.00;

        if (legalEntity.equals("PO")) { // ProcessingOrg
            mtaAmount = marginCall.getPoMTAAmount()
                    * CollateralUtilities.getFXRate(valueDate,
                    marginCall.getPoMTACurrency(),
                    marginCall.getCurrency());
            percentage = marginCall.getPoMTAPercentage();
        } else { // Counterparty
            mtaAmount = marginCall.getLeMTAAmount()
                    * CollateralUtilities.getFXRate(valueDate,
                    marginCall.getLeMTACurrency(),
                    marginCall.getCurrency());
            percentage = marginCall.getLeMTAPercentage();
        }

        double totalNominal = CreditRatingUtilsRegulation.getMccTotalNominal(
                marginCall, processDate);
        double percentValue = totalNominal * percentage;

        // get lower
        if (mtaAmount < percentValue) {
            return mtaAmount;
        } else {
            return percentValue;
        }

    }

    // Get MTA amount from marginCallCreditRating
    public static double getMtaAmountFromMccMatrix(int ratingsConfigId,
                                                   MarginCallCreditRating mccCreditRating, String baseCcy, JDate date) {

        MarginCallCreditRatingConfiguration mcRatingConfiguration;
        try {
            mcRatingConfiguration = CollateralUtilities
                    .getMCRatingConfiguration(ratingsConfigId);
            if (mcRatingConfiguration != null) {
                double mtaValue = Double.valueOf(mccCreditRating.getMta());
                String mtaCcy = mcRatingConfiguration.getMtaCurrency();
                return mtaValue
                        * CollateralUtilities.getFXRate(date, mtaCcy, baseCcy);
            }
        } catch (Exception e) {
            Log.error(CreditRatingUtilsRegulation.class, e); //sonar
        }
        return 0.00;

    }

    // Get MTA mc_percent value from marginCallCreditRating
    public static double getMtaMCPercentFromMccMatrix(
            final CollateralConfig marginCall,
            final MarginCallCreditRating mccCreditRating, JDate date) {

        double totalExposure = getMccTotalExposure(marginCall, date);
        double percentage = mccCreditRating.getMtaPercent();
        return totalExposure * percentage;

    }

    // Get MTA percent value from marginCallCreditRating
    public static double getMtaPercentFromMccMatrix(
            final CollateralConfig marginCall,
            final MarginCallCreditRating mccCreditRating, JDate date) {

        double totalNominal = getMccTotalNominal(marginCall, date);
        double percentage = mccCreditRating.getMtaPercent();
        return totalNominal * percentage;

    }

}
