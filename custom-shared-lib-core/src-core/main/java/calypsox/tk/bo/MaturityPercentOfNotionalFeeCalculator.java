/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.FeeCalculator;
import com.calypso.tk.bo.FeeDefinition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.CollateralConfig;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class MaturityPercentOfNotionalFeeCalculator implements FeeCalculator {

    @Override
    public double calculate(final Fee fee, final FeeDefinition feeDef, final Trade trade, double amount,
                            final PricingEnv pricingEnv) throws PricerException {

        Log.debug(MaturityPercentOfNotionalFeeCalculator.class, " MaturityPercentOfNotionalFeeCalculator - Start");
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

            // Need to get the Notional (Leg1/Leg2/Principal), and Ccy
            String ccy = trade.getSettleCurrency();
            Amount notional = new Amount(trade.getProduct().getPrincipal() * trade.getProduct().getBuySell(trade));

            if (trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)) {
                CollateralExposure product = (CollateralExposure) trade.getProduct();
                // ccy = trade.getSettleCurrency();
                // notional = new Amount(product.getPrincipal() * product.getBuySell(trade));

                if (CollateralUtilities.isTwoLegsProductType(product.getSubType())) {
                    // Multi Leg
                    if (marginCallConfig.getCurrency().equals(
                            product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_1))) {
                        ccy = (String) product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_1);
                        notional = (Amount) product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_NOMINAL_1);
                    } else if (marginCallConfig.getCurrency().equals(
                            product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_2))) {
                        ccy = (String) product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_2);
                        notional = (Amount) product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_NOMINAL_2);
                    } else if ("LOAN".equalsIgnoreCase((String) product
                            .getAttribute(TradeInterfaceUtils.COL_CTX_PROP_DIRECTION_1))
                            || "BUY".equalsIgnoreCase((String) product
                            .getAttribute(TradeInterfaceUtils.COL_CTX_PROP_DIRECTION_1))) {
                        // none of the legs match with MC base Ccy
                        ccy = (String) product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_1);
                        notional = (Amount) product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_NOMINAL_1);
                    } else {
                        ccy = (String) product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_CCY_2);
                        notional = (Amount) product.getAttribute(TradeInterfaceUtils.COL_CTX_PROP_NOMINAL_2);
                    }
                }
            }

            // Calculate percentage based on Maturity. If it is open term use default one specified.
            double remainingMaturity = 1.0;

            if (amount == 0.0) {
                Log.error(MaturityPercentOfNotionalFeeCalculator.class,
                        "No percentage specified in the Fee Grid Definition");
                throw new PricerException("No percentage specified in the Fee Grid Definition.");
            }

            if (trade.getMaturityDate() != null) {
                remainingMaturity = JDate.diff(processDate, trade.getMaturityDate()) / 365;
            }

            // Calculate
            calculatedAmount = (amount / 100) * notional.get() * remainingMaturity;

            JDate adjustedFeeDate = CollateralUtilities.getMTMProcessDateForFeeCalc(trade, processDate);

            fee.setCurrency(ccy);
            fee.setAmount(calculatedAmount);
            fee.setFeeDate(adjustedFeeDate);
            fee.setStartDate(adjustedFeeDate);
            fee.setEndDate(adjustedFeeDate);

        } catch (final RemoteException e) {
            Log.error(MaturityPercentOfNotionalFeeCalculator.class,
                    "Error retreiving PLMarks for the trade " + trade.getLongId(), e);
            throw new PricerException(e);
        }

        Log.debug(MaturityPercentOfNotionalFeeCalculator.class, " MaturityPercentOfNotionalFeeCalculator - End");

        return calculatedAmount;
    }

    @Override
    public double calculateInverse(final Fee fee, final FeeDefinition feeDef, final Trade trade, final double amount,
                                   final PricingEnv pricingEnv) throws PricerException {
        throw new PricerException("calculateInverse is not supported");
    }

    @Override
    public String getDescription() {
        return "Calculates a percentage of Notional based on Remaining Maturity";
    }

    @Override
    public boolean isPricingEnvRequired(final Fee arg0) {
        return true;
    }

}
