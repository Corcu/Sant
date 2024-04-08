/**
 *
 */
package calypsox.tk.regulation;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.*;

/**
 * @author Guillermo
 *
 */
public class CollateralizationDegree {


    //Felipe, pon el nombre correcto que esperas de los grados aqu? en el enum

    /**
     * Constants to define the name of the possible collateralization degree and the integer value associated
     */
    public enum COL_DEGREE {

        ONE_WAY_COLLATERALIZED("OneWay", 0),
        PARTIAL_COLLATERALIZED("Partially", 1),
        FULL_COLLATERALIZED("Fully", 2),
        UNCOLLATERALIZED("Uncollateralized", 3);

        private final String name;
        private final Integer value;

        private COL_DEGREE(String description, Integer v) {
            this.name = description;
            this.value = v;
        }

        public String getName() {
            return this.name;
        }

        public Integer getValue() {
            return this.value;
        }
    }

    /**
     * caches previous responses for a specific contract
     */
    private static Map<Integer, COL_DEGREE> cacheContractsDegree = null; //new HashMap<Integer, COL_DEGREE> ();
    private static Map<JDate, Map<Integer, COL_DEGREE>> cacheDateContractsDegree = new HashMap<JDate, Map<Integer, COL_DEGREE>>();

    private JDate today = null;


    /**
     * Constructor
     */
    public CollateralizationDegree() {

        //this.cacheContractsDegree = new HashMap<Integer, COL_DEGREE> ();
        this.today = JDate.getNow();
    }


    /**
     * When a trade has matched a MCContract and is considered collateralized, this methods returns the value of the
     * appropiate collateralization category type (0 ONE_WAY_COLLATERALIZED ,1 PARTIAL COLLATERALIZED or 2 FULL COLLATERALIZED)
     *
     * @param contract
     * @return String name of the collateralization degree
     */
    public String getTodayCollaterizedDegree(final CollateralConfig contract) {

        COL_DEGREE degree = buildCollaterizedDegree(contract, this.today);
        return degree.getName();
    }


    /**
     * When a trade has matched a MCContract and is considered collateralized, this methods returns the value of the
     * appropiate collateralization category type (0 ONE_WAY_COLLATERALIZED ,1 PARTIAL COLLATERALIZED or 2 FULL COLLATERALIZED)
     *
     * @param contract
     * @param day
     * @return String name of the collateralization degree
     */
    public String getCollaterizedDegree(final CollateralConfig contract, final JDate day) {

        COL_DEGREE degree = buildCollaterizedDegree(contract, day);
        return degree.getName();
    }


    /**
     * When a trade has matched a MCContract and is considered collateralized, this methods returns the value of the
     * appropiate collateralization category type (0 ONE_WAY_COLLATERALIZED ,1 PARTIAL COLLATERALIZED or 2 FULL COLLATERALIZED)
     *
     * @param contract
     * @param day
     * @return COL_DEGREE
     */
    public COL_DEGREE buildCollaterizedDegree(final CollateralConfig contract, final JDate day) {


        final int idContract = contract.getId();

        if (cacheDateContractsDegree.get(day) != null) {
            if (cacheDateContractsDegree.get(day).containsKey(idContract)) {
                return cacheDateContractsDegree.get(day).get(idContract);
            }
        }

        COL_DEGREE degree = COL_DEGREE.UNCOLLATERALIZED;
        /* Perform the collateralizetion degree generation */
        if (contract.getContractDirection().equals(CollateralConfig.NET_UNILATERAL)) {
            // degree 0 - UNILATERAL -> ONE_WAY_COLLATERALIZED
            degree = COL_DEGREE.ONE_WAY_COLLATERALIZED;

        } else { // must be BILATERAL

            if (checkBilateralHasThreshold(contract, day)) {
                // degree 1 - PARTIAL COLLATERALIZED
                degree = COL_DEGREE.PARTIAL_COLLATERALIZED;

            } else {
                // degree 2 - FULL COLLATERALIZED
                degree = COL_DEGREE.FULL_COLLATERALIZED;

            }
        }
        // cache the response for CONTRACT + VALUE_DATE
        if (cacheDateContractsDegree.get(day) != null) {
            cacheDateContractsDegree.get(day).put(idContract, degree);

        } else {
            cacheContractsDegree = new HashMap<Integer, COL_DEGREE>();
            cacheContractsDegree.put(idContract, degree);
            cacheDateContractsDegree.put(day, cacheContractsDegree);
        }

        // return result
        return degree;
    }

    // ///////////////////////////////////////////////////////////////
    // ///////PRIVATE METHODS COLLATERALIZATION DEGREE///////////////
    // /////////////////////////////////////////////////////////////

    /**
     * Implementation of the threshold logic implemented by GBO COL when the value 1 is returned.
     */
    private static boolean checkBilateralHasThreshold(CollateralConfig contract, JDate processDate) {

        final String poThresholdType = contract.getPoNewThresholdType();
        final String leThresholdType = contract.getLeNewThresholdType();

        // never means maximum threshold, so we return true
        if (poThresholdType.contains(CollateralConfig.NEVER) || leThresholdType.contains(CollateralConfig.NEVER)) {
            return true;
        }

        // check now credit rating:
        // if we have both option, we must check credit and after amount/percentage
        if (leHasThreshold(poThresholdType) || leHasThreshold(leThresholdType)) {

            final boolean hasRatingThreshold = checkRatingThresholdAndDifferentZero(contract, processDate);

            if (hasRatingThreshold) {
                return true;
                // otherwise, continue checking!
            }
        }

        // finally we check if amount or percentage is, at least one, different than zero
        final double poPer = contract.getPoNewThresholdPercentage();
        final double lePer = contract.getLeNewThresholdPercentage();
        final double poAmo = contract.getPoNewThresholdAmount();
        final double leAmo = contract.getLeNewThresholdAmount();

        return ((poPer != 0) || (lePer != 0) || (poAmo != 0) || (leAmo != 0));
    }

    /**
     * @param leThresholdType
     * @return if threshold type if CREDIT_RATING or GLOBAL_RATING
     */
    private static boolean leHasThreshold(String leThresholdType) {

        if ((leThresholdType == null) || leThresholdType.isEmpty()) {
            return false;
        }

        //MIG V16 CONST
        //return (leThresholdType.contains(CollateralConfig.CREDIT_RATING)
        return (leThresholdType.contains("CREDIT RATING")
                || leThresholdType.contains(CollateralConfig.GLOBAL_RATING) || leThresholdType
                .contains(CollateralConfig.BOTH));
    }

    /*
     * returns true if the rating of the Colateral contract is different than zero in the matrix
     */
    private static boolean checkRatingThresholdAndDifferentZero(final CollateralConfig contract, JDate processDate) {

        final String poRating = contract.getPoThresholdRatingDirection();// contract.getPoMTARatingDirection();
        final String leRating = contract.getLeThresholdRatingDirection();
        boolean rating = false;

        final Set<String> ratingOptions = new TreeSet<String>(Arrays.asList(new String[]{CollateralConfig.HIGHER,
                CollateralConfig.LOWER}));

        if (poRating != null) {
            rating = ratingOptions.contains(poRating);
        }
        if (!rating && (leRating != null)) {
            rating = ratingOptions.contains(leRating);
        }
        // we have the ratting option, but the contract has not a stored direction. This Should not happen
        if (!rating) {
            return rating;
        }

        // now we access the rating matrix and check it's different from zero
        rating = checkRatingMatrixMCCDifferentZero(contract, processDate);

        return rating;
    }

    /*
     * access the rating matrix and check it's different from zero
     */
    private static boolean checkRatingMatrixMCCDifferentZero(final CollateralConfig contract, JDate processDate) {

        final int idRatingMatrixPo = contract.getPoRatingsConfigId();
        final int idRatingMatrixLe = contract.getLeRatingsConfigId();
        final String ratingDirectionPo = contract.getPoThresholdRatingDirection();
        final String ratingDirectionLe = contract.getLeThresholdRatingDirection();
        final String poThresholdType = contract.getPoNewThresholdType();
        final String leThresholdType = contract.getLeNewThresholdType();
        final int ctpyId = contract.getLegalEntity().getEntityId();
        final int poId = contract.getPoId();
        String amountPo = null;
        String amountLe = null;

        // take the amount for the PO
        if (leHasThreshold(poThresholdType) || poThresholdType.contains(CollateralConfig.BOTH)) {
            amountPo = getRatingThresholdAmount(contract, processDate, idRatingMatrixPo, ratingDirectionPo, poId);
        }

        if (amountPo != null) {
            try {
                Amount amount = new Amount(amountPo);
                if (amount.get() > 0.0) {
                    return true;
                }
            } catch (NumberFormatException e) {
                Log.error(CollateralizationDegree.class, e); //sonar
            }
        }

        if (leHasThreshold(leThresholdType) || leThresholdType.contains(CollateralConfig.BOTH)) {
            amountLe = getRatingThresholdAmount(contract, processDate, idRatingMatrixLe, ratingDirectionLe, ctpyId);
        }

        if ((amountLe == null) || amountLe.isEmpty()) {
            return false;
        }

        try {
            Amount amount = new Amount(amountLe);
            if (amount.get() > 0.0) {
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        // both are empty or equal zero
        return false;
    }


    /**
     * @param contract
     * @param processDate
     *            process date
     * @param idRatingMatrix
     *            matrix id
     * @param ratingDirection
     *            lower or higher
     * @param idLe
     *            LE (PO or CTPY) id
     * @return the threshold of the rating matrix as a String
     */
    private static String getRatingThresholdAmount(CollateralConfig contract, JDate processDate, int idRatingMatrix,
                                                   String ratingDirection, final int idLe) {

        final Vector<String> agencies = contract.getEligibleAgencies();

        if (Util.isEmpty(agencies) || (processDate == null) || Util.isEmpty(ratingDirection)) {
            return null;
        }

        if (idRatingMatrix <= 0) {
            return null;
        }

        if ((contract == null) || (contract.getLegalEntity() == null)) {
            return null;
        }

        if (idLe < 0) {
            return null;
        }
        Vector<CreditRating> creditRatings = CreditRatingUtilsRegulation.getCreditRatingsForLE(contract, agencies, idLe,
                processDate);
        if ((creditRatings == null) || creditRatings.isEmpty()) {
            return null;
        }

        MarginCallCreditRating ratingToday = CreditRatingUtilsRegulation.getMccrt(creditRatings, idRatingMatrix,
                ratingDirection, processDate);

        if (ratingToday == null) {
            Log.error(CollateralizationDegree.class, "Contract rating configuration error. Contract Name: "
                    + contract.getName());
            return null;
        }

        return ratingToday.getThreshold();
    }


}
