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

public class NPVPercentageFeeCalculator implements FeeCalculator {

    @SuppressWarnings("unused")
    @Override
    public double calculate(final Fee fee, final FeeDefinition feeDef, final Trade trade, final double amount,
                            final PricingEnv pricingEnv) throws PricerException {

        Log.debug(NPVPercentageFeeCalculator.class, " NPVPercentageFeeCalculator.calculate() - Start");
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
                Log.error(NPVPercentageFeeCalculator.class, "No NPV Available for this trade for the date"
                        + processDate + " and for the Pricing Env " + marginCallConfig.getPricingEnvName());
                throw new PricerException("No NPV Available for this trade for the date " + processDate
                        + " and for the Pricing Env " + marginCallConfig.getPricingEnvName());
            }

            if (amount == 0.0) {
                Log.error(NPVPercentageFeeCalculator.class, "No percentage specified in the Fee Grid Definition");
                throw new PricerException("No percentage specified in the Fee Grid Definition.");
            }

            // Calculate
            final String ccy = npvValue.getCurrency();
            final double npv = npvValue.getMarkValue();
            final double percentage = amount;

            calculatedAmount = npv * (amount / 100);

            JDate adjustedFeeDate = CollateralUtilities.getMTMProcessDateForFeeCalc(trade, processDate);

            fee.setCurrency(ccy);
            fee.setAmount(calculatedAmount);
            fee.setFeeDate(adjustedFeeDate);
            fee.setStartDate(adjustedFeeDate);
            fee.setEndDate(adjustedFeeDate);

        } catch (final RemoteException e) {
            Log.error(NPVPercentageFeeCalculator.class, "Error retreiving PLMarks for the trade " + trade.getLongId(), e);
            throw new PricerException(e);
        }

        Log.debug(NPVPercentageFeeCalculator.class, " NPVPercentageFeeCalculator.calculate() - Start");

        return calculatedAmount;
    }

    @Override
    public double calculateInverse(final Fee fee, final FeeDefinition feeDef, final Trade trade, final double amount,
                                   final PricingEnv pricingEnv) throws PricerException {
        throw new PricerException("calculateInverse is not supported");

    }

    @Override
    public String getDescription() {
        return "Calculates a percentage of NPV as Fee";
    }

    @Override
    public boolean isPricingEnvRequired(final Fee fee) {
        return true;
    }

}
