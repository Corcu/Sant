/**
 *
 */
package calypsox.tk.util;

import calypsox.tk.report.OptCustAgreementParamsLogic;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author aalonsop
 * @version 3.0 Resaves MTA, IndependentAmount & ThresholdAmount. Now it's
 *          capable to choose which Credit Rating Types are going to be
 *          formatted
 * @see Amount : Deprecated format methods now should use an Amount object to
 *      format the incoming String. Change this before adding new functionality
 *      to this class.
 */
public class ScheduledTaskRESAVE_MTA extends ScheduledTask {

    private static final String DESCRIPTION = "Updates all system CreditRating's MTA and IndependentAmount values with a parsed one";
    private static final String MTA_ATTR = "Format MTA";
    private static final String IA_ATTR = "Format IA";
    private static final String THR_ATTR = "Format Threshold";
    private static final long serialVersionUID = -165673253930998148L;

    /*
     * @see com.calypso.tk.util.ScheduledTask#getTaskInformation()
     */
    @Override
    public String getTaskInformation() {
        return DESCRIPTION;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.add(attribute(MTA_ATTR).booleanType());
        attributeList.add(attribute(IA_ATTR).booleanType());
        attributeList.add(attribute(THR_ATTR).booleanType());
        return attributeList;
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        boolean success = true;
        try {
            List<MarginCallCreditRatingConfiguration> ratingConfigs = ServiceRegistry.getDefault().getCollateralServer()
                    .getAllMarginCallCreditRatingConfig();
            for (MarginCallCreditRatingConfiguration config : ratingConfigs)
                resaveCreditRating(config);
        } catch (CollateralServiceException e) {
            success = false;
            Log.error(this, "Error while trying to save MTA ratings: " + e.getMessage());
            Log.error(this, e); //sonar
        }
        return success;
    }

    /**
     * Gets all CreditRatings from a CreditRatingConfig, then updates it MTA
     * with the parsed value and finally resaves it
     *
     * @param config
     * @throws CollateralServiceException
     */
    private void resaveCreditRating(MarginCallCreditRatingConfiguration config) throws CollateralServiceException {
        MarginCallCreditRatingConfiguration mccConfig = ServiceRegistry.getDefault().getCollateralServer()
                .getMarginCallCreditRatingById(config.getId());
        if (mccConfig != null && !Util.isEmpty(mccConfig.getRatings())) {
            List<MarginCallCreditRating> creditRatings = mccConfig.getRatings();
            Vector<MarginCallCreditRating> creditRatingVector = new Vector<>();
            for (MarginCallCreditRating rating : creditRatings) {
                try {
                    String parsedMTA = removeScientificNotationFromMTA(rating);
                    String parsedIndAmount = removeScientificNotationFromIndependentAmount(rating);
                    String parsedThreshold = removeScientificNotationFromThresholdAmount(rating);
                    // Only true in case of format failure
                    boolean rollback = false;
                    if (rollback) {
                        parsedMTA = formattedNumberToRaw(parsedMTA);
                        parsedIndAmount = formattedNumberToRaw(parsedIndAmount);
                        parsedThreshold = formattedNumberToRaw(parsedThreshold);
                    }
                    rating.setMta(parsedMTA);
                    rating.setIndependentAmount(parsedIndAmount);
                    rating.setThreshold(parsedThreshold);
                    creditRatingVector.addElement(rating);
                } catch (NullPointerException e) {
                    Log.error(this, "Error while trying to set MTA");
                    Log.error(this, e); //sonar
                }
            }
            ServiceRegistry.getDefault().getCollateralServer().save(config, creditRatingVector);
        }
    }

    private String formattedNumberToRaw(String number) {
        return CollateralUtilities.parseStringAmountToDouble(number).toString();
    }

    private String removeScientificNotation(Double value) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(value);
    }

    private String removeScientificNotationFromMTA(MarginCallCreditRating rating) {
        if (!checkInputAttribute(MTA_ATTR))
            return rating.getMta();
        Double value = parseStringAmountToDouble(rating.getMta());
        return removeScientificNotation(value);
    }

    private String removeScientificNotationFromIndependentAmount(MarginCallCreditRating rating) {
        if (!checkInputAttribute(IA_ATTR))
            return rating.getIndependentAmount();
        Double value = parseStringAmountToDouble(rating.getIndependentAmount());
        return removeScientificNotation(value);
    }

    private String removeScientificNotationFromThresholdAmount(MarginCallCreditRating rating) {
        if (!checkInputAttribute(THR_ATTR))
            return rating.getThreshold();
        Double value = parseStringAmountToDouble(rating.getThreshold());
        return removeScientificNotation(value);
    }

    /**
     * @description Handles different incoming number formats and nullPointers
     *              to avoid crashes
     * @param amount
     * @return The parsed double value
     * @deprecated In version 3.0, future revisions MUST return an
     *             {@link com.calypso.tk.core.Amount} object.
     */
    private Double parseStringAmountToDouble(String amount) {
        NumberFormat formatter;
        try {
            return Double.valueOf(amount);
        } catch (NumberFormatException e) {
            formatter = NumberFormat.getNumberInstance();
            try {
                return formatter.parse(amount).doubleValue();
            } catch (ParseException e1) {
                Log.error(OptCustAgreementParamsLogic.class, "Error while trying to get Double from String: " + amount);
                Log.error(this, e1); //sonar
            }
        } catch (NullPointerException e) {
            Log.warn(OptCustAgreementParamsLogic.class,
                    "Null CreditRating value received, a zero value double is returned");
            Log.warn(this, e); //sonar
            return new Double(0);
        }
        return null;
    }

    /**
     *
     * @param ST Attribute name
     * @return The attribute value
     */
    private boolean checkInputAttribute(String attrName) {
        return this.getBooleanAttribute(attrName);
    }

    /**
     *
     * @param configs
     * @return All distinct CreditRating Ids
     */
    protected Set<Integer> retrieveCreditRatingConfigIds(List<MarginCallCreditRatingConfiguration> configs) {
        final Set<Integer> ids = new HashSet<>();
        ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            for (final MarginCallCreditRatingConfiguration config : configs) {
                exec.execute(new Runnable() {
                    public void run() {
                        synchronized (ids) {
                            ids.add(config.getId());
                        }
                    }
                });
            }
        } finally {
            exec.shutdown();
            try {
                exec.awaitTermination(2, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Log.error(this, "Error while getting config ids from CreditRatingConfigList");
            }
        }
        return ids;
    }
}
