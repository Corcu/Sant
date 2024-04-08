package calypsox.tk.util;

import calypsox.tk.report.OptCustAgreementParamsLogic;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author aalonsop
 */
public class ScheduledTask_RESAVE_MTA extends ScheduledTask {

    private static final String DESCRIPTION = "Updates all system CreditRating's MTA values with a parsed value";
    private static final long serialVersionUID = 1473563387271693076L;

    /*
     * @see com.calypso.tk.util.ScheduledTask#getTaskInformation()
     */
    @Override
    public String getTaskInformation() {
        return DESCRIPTION;
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
        MarginCallCreditRatingConfiguration creditRatingConfig = ServiceRegistry.getDefault().getCollateralServer()
                .getMarginCallCreditRatingById(config.getId());
        if (creditRatingConfig != null) {
            List<MarginCallCreditRating> creditRatings = creditRatingConfig.getRatings();
            if (!Util.isEmpty(creditRatings)) {
                Vector<MarginCallCreditRating> creditRatingVector = new Vector<>();
                for (MarginCallCreditRating rating : creditRatings) {
                    try {
                        String parsedMTA = removeCientNotationFromMTA(parseStringAmountToDouble(rating.getMta()));
                        rating.setMta(parsedMTA);
                        creditRatingVector.addElement(rating);
                    } catch (NullPointerException e) {
                        Log.error(this, "Error while trying to set MTA \n" + e); //sonar
                    }
                }
                ServiceRegistry.getDefault().getCollateralServer().save(config, creditRatingVector);
            }
        }
    }

    /**
     * @param value
     * @return
     */
    private String removeCientNotationFromMTA(Double value) {
        String stringValue = value.toString();
        if (stringValue.contains("E"))
            stringValue = BigDecimal.valueOf(value).toPlainString();
        return stringValue.replace(".", ",");
    }

    /**
     * @param amount
     * @return The parsed double value
     * @description Handles different incoming number formats and nullPointers
     * to avoid crashes
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
            Log.warn(OptCustAgreementParamsLogic.class, "Null MTA value received, a zero value double is returned \n" + e); //sonar
            return 0.0D;
        }
        return null;
    }

    /**
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
            } catch (InterruptedException exc) {
                Log.error(this, "Error while getting config ids from CreditRatingConfigList");
            }
        }
        return ids;
    }
}
