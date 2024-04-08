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
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class ContractBaseCcyFeeCalculator implements FeeCalculator {

    @Override
    public double calculate(final Fee fee, final FeeDefinition feeDef, final Trade trade, final double amount,
                            final PricingEnv pricingEnv) throws PricerException {

        Log.debug(ContractBaseCcyFeeCalculator.class, " ContractBaseCcyFeeCalculator - Start");
        final JDate processDate = CollateralUtilities.getMTMProcessDate(trade);
        try {
            ArrayList<String> errorMsgs = new ArrayList<String>();
            CollateralConfig marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
            if (errorMsgs.size() > 0) {
                throw new PricerException(errorMsgs.get(0));
            } else if (marginCallConfig == null) {
                throw new PricerException("MarginCallContract not found for the trade.");
            }
            JDate adjustedFeeDate = CollateralUtilities.getMTMProcessDateForFeeCalc(trade, processDate);

            fee.setCurrency(marginCallConfig.getCurrency());
            fee.setAmount(amount);
            fee.setFeeDate(adjustedFeeDate);
            fee.setStartDate(adjustedFeeDate);
            fee.setEndDate(adjustedFeeDate.addDays(1));

        } catch (final RemoteException e) {
            Log.error(ContractBaseCcyFeeCalculator.class,
                    "Error calculating ContractBaseCcy fee for trade " + trade.getLongId(), e);
            throw new PricerException(e);
        }

        Log.debug(ContractBaseCcyFeeCalculator.class, " ContractBaseCcyFeeCalculator - End");

        return amount;
    }

    @Override
    public double calculateInverse(final Fee fee, final FeeDefinition feeDef, final Trade trade, final double amount,
                                   final PricingEnv pricingEnv) throws PricerException {
        throw new PricerException("calculateInverse is not supported");

    }

    @Override
    public String getDescription() {
        return "Fixed amount Fee Calculator with Contract Base Currency";
    }

    @Override
    public boolean isPricingEnvRequired(final Fee arg0) {
        // TODO Auto-generated method stub
        return true;
    }

}
