package calypsox.tk.bo;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.*;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.FeeGrid;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;

public class BorrowerFeeGridFeeCalculator extends FeeGridFeeCalculator {
    private static final String INCLUDED_CPTY_SDF = "BorrowerFeeCalculator";
    private Double principal;
    private Double principal2;
    private String principalCcy;
    private String principal2Ccy;

    @Override
    public double calculate(Fee fee, FeeDefinition def, Trade trade, double amount, PricingEnv env) throws PricerException {

        if ((trade == null) || (trade.getProduct() == null)) {
            return 0;
        }

        try {
            if (trade.getProduct() instanceof CollateralExposure && CollateralUtilities.isTwoLegsProductType(trade.getProductSubType())) {

                StaticDataFilter sdf = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter(INCLUDED_CPTY_SDF);

                if (sdf.accept(trade)) {
                    CollateralExposure collatExpo = (CollateralExposure) trade.getProduct();
                    String tradeDirection = "";

                    if ((collatExpo != null)) {
                        tradeDirection = collatExpo.getDirection(trade);
                    }

                    String leg1Direction = (String) collatExpo.getAttribute("DIRECTION_1");
                    String leg2Direction = (String) collatExpo.getAttribute("DIRECTION_2");

                    if ((leg1Direction == null) || (leg2Direction == null) || leg1Direction.isEmpty() || leg2Direction.isEmpty()) {
                        return 0;
                    }

                    retrieveLegs(tradeDirection, leg1Direction, leg2Direction, collatExpo);

                    //Calcula la Fee con los datos de la pata Borrower si está en distinta divisa a la de la pata Loan
                    if (!principalCcy.equalsIgnoreCase(principal2Ccy)) {
                        long externalId = fee.getExternalLongId();
                        if (externalId != 0L) {
                            FeeGrid grid = BOCache.getFeeGrid(DSConnection.getDefault(), (int) fee.getExternalLongId());
                            if (grid == null) {
                                throw new PricerException("Cannot retrieve the Fee Grid");
                            } else {
                                String calculator = grid.getCalculationType();
                                if (calculator == null) {
                                    throw new PricerException("No Calculation Type attached to the FeeGrid:" + grid.getId());
                                } else {
                                    double amountC;
                                    if (recalculate(grid, fee.getAmount())) {
                                        amountC = amount * principal2 / 100.0;
                                        amountC = this.getFinalAmount(grid, amountC);
                                        fee.setAmount(amountC);
                                        fee.setCurrency(principal2Ccy);
                                    } else {
                                        amountC = fee.getAmount();
                                    }

                                    if (grid.getMaximumAmount() != 0.0 && Math.abs(amountC) > grid.getMaximumAmount()) {
                                        return amountC < 0.0 ? -grid.getMaximumAmount() : grid.getMaximumAmount();
                                    } else if (grid.getMinimumAmount() != 0.0 && Math.abs(amountC) < grid.getMinimumAmount()) {
                                        return amountC < 0.0 ? -grid.getMinimumAmount() : grid.getMinimumAmount();
                                    } else {
                                        return CurrencyUtil.roundAmount(fee.getAmount(), fee.getCurrency());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(BorrowerFeeGridFeeCalculator.class.getSimpleName(), "Cannot retrieve the Static Data Filter " + INCLUDED_CPTY_SDF);
        }
        return super.calculate(fee, def, trade, amount, env);
    }

    private void retrieveLegs(String tradeDirection, String leg1Direction, String leg2Direction, CollateralExposure collatExpo) {
        // leg one with loan is principal
        if ("Buy".equalsIgnoreCase(tradeDirection) || "Loan".equalsIgnoreCase(tradeDirection)) {
            if (leg1Direction.equalsIgnoreCase("Loan") || leg1Direction.equalsIgnoreCase("Buy")) { // principal
                principal = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                principalCcy = (String) collatExpo.getAttribute("CCY_1");
                principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
            } else { // sell, leg1 is borrower
                principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                principal2Ccy = (String) collatExpo.getAttribute("CCY_1");
                principal = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                principalCcy = (String) collatExpo.getAttribute("CCY_2");
            }

        } else { // leg2 is principal
            if (leg2Direction.equalsIgnoreCase("Borrower") || leg2Direction.equalsIgnoreCase("Sell")) { // principal
                principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                principal2Ccy = (String) collatExpo.getAttribute("CCY_1");
                principal = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                principalCcy = (String) collatExpo.getAttribute("CCY_2");
            } else { // sell, leg1 is borrower
                principal = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                principalCcy = (String) collatExpo.getAttribute("CCY_1");
                principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
            }
        }
    }


    private boolean recalculate(FeeGrid grid, double amount) {
        boolean calculate = true;
        if (amount != 0.0 && grid != null) {
            String autoRecalculate = grid.getAttribute("MARGIN_CALL_AUTO_RECALCULATE");
            if (!Util.isEmpty(autoRecalculate)) {
                calculate = Util.toBoolean(autoRecalculate);
            }
        }

        return calculate;
    }
}
