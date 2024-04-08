package calypsox.tk.util;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;

import java.util.ArrayList;
import java.util.HashMap;

import static com.calypso.tk.core.PricerMeasure.S_NPV;

/**
 * @author aela
 */
public class SantImportMTMUtil {

    public static PLMarkValue convertPLMarkValueToTargetCurrency(final PLMarkValue baseMarkValue,
                                                                 final PricingEnv pricingEnv, final String targetCcy, final JDate processDate,
                                                                 final ArrayList<String> errorMsgs, boolean changeMarkName) {
        PLMarkValue convertedMarkValue = null;

        if (baseMarkValue == null) {
            errorMsgs.add("MarkValue passed in is null.");
            return convertedMarkValue;
        }

        final double amount = baseMarkValue.getMarkValue();
        final String ccy = baseMarkValue.getCurrency();
        String requiredMarkName = null;

        try {
            final double convertedAmount = CollateralUtilities.convertCurrency(ccy, amount, targetCcy, processDate,
                    pricingEnv);
            if (baseMarkValue.getMarkName().equals(SantPricerMeasure.S_NPV_BASE)) {
                requiredMarkName = S_NPV;
            } else if (baseMarkValue.getMarkName().equals(SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE)) {
                requiredMarkName = SantPricerMeasure.S_INDEPENDENT_AMOUNT;
            }
            if (!changeMarkName)
                requiredMarkName = baseMarkValue.getMarkName();

            convertedMarkValue = CollateralUtilities.buildPLMarkValue(requiredMarkName, targetCcy, convertedAmount,
                    baseMarkValue.getAdjustmentType());

        } catch (final MarketDataException e) {
            errorMsgs.add(e.getMessage());
            Log.error(CollateralUtilities.class, e);
        }

        return convertedMarkValue;
    }

    public static PLMarkValue convertPLMarkValueToTradeCcy(final PLMarkValue baseMarkValue, final PricingEnv pricingEnv,
                                                           final String tradeCurrency, final JDate processDate, final ArrayList<String> errorMsgs) {
        return convertPLMarkValueToTargetCurrency(baseMarkValue, pricingEnv, tradeCurrency, processDate,
                errorMsgs, true);
    }

    /**
     * Checks if the current PLMark contains the a PLMarkValue with the passed
     * in pLMarkValue's Markname If one exists it changes the currency and value
     * same as pLMarkValue otherwise it adds the passed in pLMarkValue to the
     * PLMark
     *
     * @param plMarks
     * @param trade
     * @param pLMarkValue
     * @param pricingEnv
     * @param processDate
     * @version 2.0 Now checks if the PLMark new value is the same as the
     * entered value, and throws the error.
     */
    public static void adjustAndAddPLMarkValue(final HashMap<Trade, PLMark> plMarks, final Trade trade,
                                               final PLMarkValue pLMarkValue, final String pricingEnv, final JDate processDate) {

        final PLMark plMark = plMarks.get(trade);
        final PLMarkValue currentPLMarkValue = CollateralUtilities.retrievePLMarkValue(plMark,
                pLMarkValue.getMarkName());

        if (currentPLMarkValue != null) {
            currentPLMarkValue.setCurrency(pLMarkValue.getCurrency());
            // Migration V14 - 04012016
            currentPLMarkValue.setOriginalCurrency(pLMarkValue.getCurrency());
            currentPLMarkValue.setMarkValue(pLMarkValue.getMarkValue());
            currentPLMarkValue.setAdjustmentType(pLMarkValue.getAdjustmentType());
        }
        plMark.addPLMarkValue(pLMarkValue);
    }

    /**
     * @param newValue
     * @param actualValue
     * @return True if the new currency and markValue it's the same as the old
     * one
     */
    protected static boolean checkPLMarkValueIsChanged(PLMarkValue newValue, PLMarkValue actualValue) {
        if (newValue.getCurrency().equals(actualValue.getCurrency())) {
            if (newValue.getMarkValue() == newValue.getMarkValue())
                return true;
        }
        return false;
    }

    public static void adjustPLMarkValues(PLMark plMark, final PLMarkValue pLMarkValue) {

        final PLMarkValue currentPLMarkValue = CollateralUtilities.retrievePLMarkValue(plMark,
                pLMarkValue.getMarkName());

        if (currentPLMarkValue != null) {
            currentPLMarkValue.setCurrency(pLMarkValue.getCurrency());
            // Migration V14 - 04012016
            currentPLMarkValue.setOriginalCurrency(pLMarkValue.getCurrency());
            currentPLMarkValue.setMarkValue(pLMarkValue.getMarkValue());
            currentPLMarkValue.setAdjustmentType(pLMarkValue.getAdjustmentType());
        } else if (plMark != null) {
            plMark.addPLMarkValue(pLMarkValue);
        }
    }

}
