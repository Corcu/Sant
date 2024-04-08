package calypsox.tk.report;

import calypsox.tk.report.generic.loader.SantHedgeFundLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallDetailEntry;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SantHedgeFundActivityReport extends SantReport {

    private static final long serialVersionUID = 1L;

    @Override
    public ReportOutput loadReport(Vector<String> vector) {
        try {
            return getReportOutput();

        } catch (final Exception e) {
            Log.error(this, "Cannot load MarginCallDetailEntries", e);
            vector.add(e.getMessage());
        }

        return null;

    }

    /**
     * @return
     * @throws Exception
     */
    private ReportOutput getReportOutput() throws Exception {
        PricingEnv pe = null;
        DefaultReportOutput output = new DefaultReportOutput(this);
        SantHedgeFundLoader loader = new SantHedgeFundLoader();
        List<SantMarginCallDetailEntry> detailentries = loader.loadActivity(getReportTemplate(), getValDate());

        List<ReportRow> rows = new ArrayList<ReportRow>();
        for (SantMarginCallDetailEntry detailEntry : detailentries) {

            CollateralConfig config = detailEntry.getMarginCallConfig();

            if ((config != null) && (pe == null)) {
                pe = AppUtil.loadPE(config.getPricingEnvName(), getValuationDatetime());
                setPricingEnv(pe);
            }

            ReportRow row = new ReportRow(detailEntry, "SantMarginCallDetailEntry");
            row.setProperty("PRICING_ENV", getPricingEnv());

            // GSM: to fix principals and currencies
            Trade trade = detailEntry.getTrade();
            row.setProperty(SantHedgeFundActivityReportTemplate.TRADE_PRINCIPAL_WRAPPER, new TradePrincipalWrapper(
                    trade));

            rows.add(row);
        }
        output.setRows(rows.toArray(new ReportRow[rows.size()]));
        return output;
    }

    /*
     * Wrapper class for principals and currencies
     */
    class TradePrincipalWrapper {

        private Double principal;

        private Double principal2;

        private String principalCcy;

        private String principal2Ccy;

        private String direction;

        public TradePrincipalWrapper(Trade trade) {

            if (trade != null) {

                try {
                    // GSM: build-underlying changes
                    buildPrincipals(trade);
                } catch (Exception e) {
                    Log.error(this, "Build principals and currencies issue for trade id = " + trade.getLongId(), e);
                }
            }

        }

        private void buildPrincipals(Trade input) {

            Product product = input.getProduct();

            if ((input == null) || (product == null)) {
                Log.error(this, "Cannot retrieve producto of the trade int. ref: " + input.getInternalReference());
            }

            this.principal = product.getPrincipal();
            this.principalCcy = product.getCurrency();

            // derivative
            if ((product instanceof CollateralExposure)) {
                // take the type
                buildTwoLegs(input);

            } else if (product instanceof SecLending) {
                this.principal = ((SecLending) product).getSecuritiesNominalValue(getValDate());
                this.direction = ((SecLending) product).getDirection();
            }
        }

        /**
         * Builds the two legs of the trade, putting the data of the principal leg (nominal and ccy) on the left side
         * and the secondary data on the right.
         *
         * @param tradeBeanLeg1
         * @param tradeBeanLeg2
         * @return
         */
        private void buildTwoLegs(Trade trade) {

            CollateralExposure collatExpo = null;
            String tradeDirection = "";

            if ((trade == null) || (trade.getProduct() == null)) {
                return;
            }

            if (!(trade.getProduct() instanceof CollateralExposure)) {
                return;
            }

            collatExpo = (CollateralExposure) trade.getProduct();

            if ((collatExpo != null)) {
                tradeDirection = collatExpo.getDirection(trade);
                // small fix, lets show the direction of the product on the report
                this.direction = tradeDirection;
            }

            String leg1Direction = (String) collatExpo.getAttribute("DIRECTION_1");
            String leg2Direction = (String) collatExpo.getAttribute("DIRECTION_2");

            if ((leg1Direction == null) || (leg2Direction == null) || leg1Direction.isEmpty()
                    || leg2Direction.isEmpty()) {
                return;
            }

            // leg one with loan is principal
            if ("Buy".equalsIgnoreCase(tradeDirection) || "Loan".equalsIgnoreCase(tradeDirection)) {

                if (leg1Direction.equalsIgnoreCase("Loan") || leg1Direction.equalsIgnoreCase("Buy")) { // principal

                    this.principal = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                    this.principalCcy = (String) collatExpo.getAttribute("CCY_1");

                    this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                    this.principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
                } else { // sell, leg1 is borrower

                    this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                    this.principal2Ccy = (String) collatExpo.getAttribute("CCY_1");

                    this.principal = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                    this.principalCcy = (String) collatExpo.getAttribute("CCY_2");
                }

            } else { // leg2 is principal

                if (leg2Direction.equalsIgnoreCase("Borrower") || leg2Direction.equalsIgnoreCase("Sell")) { // principal

                    this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                    this.principal2Ccy = (String) collatExpo.getAttribute("CCY_1");

                    this.principal = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                    this.principalCcy = (String) collatExpo.getAttribute("CCY_2");
                } else { // sell, leg1 is borrower

                    this.principal = ((Amount) collatExpo.getAttribute("NOMINAL_1")).get();
                    this.principalCcy = (String) collatExpo.getAttribute("CCY_1");

                    this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
                    this.principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
                }

            }
        }

        public DisplayValue getPrincipal() {
            return ((this.principal == null) || Double.isNaN(this.principal)) ? null : CollateralUtilities
                    .formatAmount(this.principal, this.principalCcy);
        }

        public DisplayValue getPrincipal2() {
            return ((this.principal2 == null) || Double.isNaN(this.principal2)) ? null : CollateralUtilities
                    .formatAmount(this.principal2, this.principal2Ccy);
        }

        public String getPrincipalCcy() {
            return this.principalCcy;
        }

        public String getPrincipal2Ccy() {
            return this.principal2Ccy;
        }

        /**
         * @return the direction
         */
        public String getDirection() {
            return this.direction;
        }

    }

}
