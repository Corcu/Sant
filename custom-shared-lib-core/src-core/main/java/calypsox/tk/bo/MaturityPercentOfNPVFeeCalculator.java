/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.FeeCalculator;
import com.calypso.tk.bo.FeeDefinition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class MaturityPercentOfNPVFeeCalculator implements FeeCalculator {

    @Override
    public double calculate(final Fee fee, final FeeDefinition feeDef, final Trade trade, double amount,
                            final PricingEnv pricingEnv) throws PricerException {

        Log.debug(MaturityPercentOfNPVFeeCalculator.class, " MaturityPercentOfNPVFeeCalculator - Start");
        double calculatedAmount = 0.0;

        final JDate processDate = CollateralUtilities.getMTMProcessDate(trade);
        try {
            ArrayList<String> errorMsgs = new ArrayList<String>();
            CollateralConfig marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
            if (errorMsgs.size() > 0) {
                throw new PricerException(errorMsgs.get(0));
            } else if (marginCallConfig == null) {
                throw new PricerException("MarginCallContract not found for the trade.");
            }

            final PLMark plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(),
                    marginCallConfig.getPricingEnvName(), processDate);

            final PLMarkValue npvValue = CollateralUtilities.retrievePLMarkValue(plMark, PricerMeasure.S_NPV);

            if (npvValue == null) {
                Log.error(MaturityPercentOfNPVFeeCalculator.class, "No NPV Available for this trade for the date"
                        + processDate + " and for the Pricing Env " + marginCallConfig.getPricingEnvName());
                throw new PricerException("No NPV Available for this trade for the date " + processDate
                        + " and for the Pricing Env " + marginCallConfig.getPricingEnvName());
            }

            // Calculate percentage based on Maturity. If it is open term use default one specified.
            double percentage = amount;//
            double remainingMaturity = 1.0;
            if (amount == 0.0) {
                Log.error(MaturityPercentOfNPVFeeCalculator.class, "No percentage specified in the Fee Grid Definition");
                throw new PricerException("No percentage specified in the Fee Grid Definition.");
            }

            if (trade.getMaturityDate() != null) {
                remainingMaturity = JDate.diff(processDate, trade.getMaturityDate()) / 365;
            }

            // Calculate
            final String ccy = npvValue.getCurrency();
            final double npv = npvValue.getMarkValue();

            calculatedAmount = npv * remainingMaturity * (percentage / 100);

            JDate adjustedFeeDate = CollateralUtilities.getMTMProcessDateForFeeCalc(trade, processDate);

            fee.setCurrency(ccy);
            fee.setAmount(calculatedAmount);
            fee.setFeeDate(adjustedFeeDate);
            fee.setStartDate(adjustedFeeDate);
            fee.setEndDate(adjustedFeeDate);

        } catch (final RemoteException e) {
            Log.error(MaturityPercentOfNPVFeeCalculator.class,
                    "Error retreiving PLMarks for the trade " + trade.getLongId(), e);
            throw new PricerException(e);
        }

        Log.debug(MaturityPercentOfNPVFeeCalculator.class, " MaturityPercentOfNPVFeeCalculator - End");

        return calculatedAmount;
    }

    @Override
    public double calculateInverse(final Fee fee, final FeeDefinition feeDef, final Trade trade, final double amount,
                                   final PricingEnv pricingEnv) throws PricerException {
        throw new PricerException("calculateInverse is not supported");
    }

    @Override
    public String getDescription() {
        return "Calculates a percentage of NPV based on Remaining Maturity";
    }

    @Override
    public boolean isPricingEnvRequired(final Fee arg0) {
        return true;
    }

}
