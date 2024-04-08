package calypsox.tk.report.portbreakdown;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.SantPortfolioBreakdownReport;
import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.util.TimeZone;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class PortBreakdownTradeWrapper{

    Double principal;

    Double principal2;

    String principalCcy;

    String principal2Ccy;

    String mccCcy;

    Double fxRate1;

    Double fxRate2;

    String underlying1;

    String underlying2;

    String instrument;

    String direction;

    Double initialCollateral;

    JDate tradeDate;

    // for repo enhacements
    double pool_factor;

    private JDate valDate;
    private SantGenericQuotesLoader quotesLoader;

    public PortBreakdownTradeWrapper(Trade trade, CollateralConfig mcc,JDate valDate,SantGenericQuotesLoader quotesLoader) {
        this.mccCcy = mcc.getCurrency();
        this.valDate=valDate;
        this.quotesLoader=quotesLoader;
        buildTradeDate(trade);

        try {
            // GSM: build-underlying changes
            buildPrincipals(trade);
        } catch (Exception e) {
            Log.error(this, "Build principal issue for trade id = " + trade.getLongId(), e);
        }
        buildFXRate();
        buildUnderlyings(trade.getProduct());
        buildInstrument(trade);
        buildInitialCollateral(trade, mcc);
        buildFactors(trade); // for repo enhacements
    }

    private void buildTradeDate(Trade trade) {
        if (trade.getProductType().equals(Product.SEC_LENDING)) {
            String kw = trade.getKeywordValue("REAL_TRADE_DATE");
            if (!Util.isEmpty(kw)) {
                this.tradeDate = JDate.valueOf(kw);
            }
        } else {
            this.tradeDate = trade.getTradeDate().getJDate(TimeZone.getDefault());
        }

    }

    // for testing purpose should not be used
    PortBreakdownTradeWrapper(Trade trade) {
        buildInitialCollateral(trade, null);
    }

    private void buildInitialCollateral(Trade trade, CollateralConfig mcc) {
        Collateral collat = null;
        if (Product.REPO.equals(trade.getProductType())) {
            Repo repo = (Repo) trade.getProduct();
            if ((repo.getCollaterals() != null) && (repo.getCollaterals().size() > 0)) {
                collat = (Collateral) repo.getCollaterals().get(0);
            }

        } else if (Product.SEC_LENDING.equals(trade.getProductType())) {
            SecLending secLending = (SecLending) trade.getProduct();
            if ((secLending.getCollaterals() != null) && (secLending.getCollaterals().size() > 0)) {
                // System.out.println(trade.getLongId());
                // GSM: error in CERT - OSLA
                if ((secLending.getCollaterals() != null) && (secLending.getCollaterals().size() >= 1)) {
                    collat = (Collateral) secLending.getCollaterals().get(0);
                }
            }
        }

        if (collat == null) {
            this.initialCollateral = null;
        } else {
            collat.setInitialFXRate(1.0d);
            double allInPrice = collat.computeAllInPrice();
            if (Product.BOND.equals(collat.getSecurityType())) {
                allInPrice = allInPrice / 100;
            }
            this.initialCollateral = allInPrice * collat.getQuantity();
            if (Product.SEC_LENDING.equals(trade.getProductType())) {
                double oslaFactor = 1;
                try {
                    if (!Util.isEmpty(trade.getKeywordValue(CollateralStaticAttributes.FO_HAIRCUT))) {
                        oslaFactor = Double.valueOf(trade.getKeywordValue(CollateralStaticAttributes.FO_HAIRCUT)) / 100;
                    } else {
                        oslaFactor = Double.valueOf(mcc.getAdditionalField(CollateralStaticAttributes.MCC_HAIRCUT)) / 100;
                    }
                } catch (Exception e) {
                    Log.info(this, e); //sonar
                    oslaFactor = 1;
                }
                // initialCollateral represents the value in term of CASH and not Security
                // Therefore for a Loan of Security this value should be positive and negative for a Borrow of
                // Security
                this.initialCollateral = this.initialCollateral * oslaFactor * -1;
            }
        }
    }

    private void buildFactors(Trade trade) {
        if (Product.REPO.equals(trade.getProductType())) {
            Repo repo = (Repo) trade.getProduct();
            if (repo != null) {
                Product p = BOCache.getExchangedTradedProduct(DSConnection.getDefault(),
                        repo.getUnderlyingSecurityId());
                if ((p != null) && (p instanceof Bond)) {
                    Bond b = (Bond) p;
                    this.pool_factor = b.getPoolFactor(this.valDate);
                }
            }
        }
    }

    protected void buildInstrument(Trade trade) {
        if ("CollateralExposure".equals(trade.getProductType())) {
            this.instrument = trade.getProductSubType();
        }else{
            this.instrument = trade.getProductType();
        }
    }

    void buildPrincipals(Trade input) {

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

            // if (is2Legs(((CollateralExposure) product).getUnderlyingType())) {
            //
            // CollateralExposure collatExpo = (CollateralExposure) product;
            // this.principal2 = ((Amount) collatExpo.getAttribute("NOMINAL_2")).get();
            // this.principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
            // // GSM: fix currency
            // this.principalCcy = (String) collatExpo.getAttribute("CCY_1");
            // }

        } else if (product instanceof SecLending) {
            this.principal = ((SecLending) product).getSecuritiesNominalValue(this.valDate);
            this.direction = ((SecLending) product).getDirection();

        } else if (product instanceof PerformanceSwap) {

            this.direction = loadPrimaryLegDirection(((PerformanceSwap) product), input);

            this.principal = ((PerformanceSwap) product).getPrimaryLeg().getPrincipal();
            this.principalCcy = ((PerformanceSwap) product).getPrimaryLeg().getCurrency();
            this.principal2 = ((PerformanceSwap) product).getSecondaryLeg().getPrincipal();
            this.principal2Ccy = ((PerformanceSwap) product).getSecondaryLeg().getCurrency();


        }
    }

    public String loadPrimaryLegDirection(PerformanceSwap product, Trade trade) {
        String primayLegDesc = "";

        if (null != trade && null != product) {
            PerformanceSwappableLeg primaryLeg = product.getPrimaryLeg();
            PerformanceSwapLeg primLeg = null;
            boolean perfLeg = false;

            if (primaryLeg instanceof PerformanceSwapLeg) {
                perfLeg = true;
                primLeg = (PerformanceSwapLeg) primaryLeg;
            }
            if (perfLeg) {
                if (primLeg.getNotional() < 0.0D) {
                    primayLegDesc = "B";
                } else if (primLeg.getNotional() == 0.0D && (trade.isAllocationParent() || trade.isAllocationChild()) && trade.getQuantity() < 0.0D) {
                    primayLegDesc = "B";
                } else {
                    primayLegDesc = "S";
                }
            }
        }
        return primayLegDesc;
    }

    private void buildFXRate() {
        QuoteValue qv;
        if (this.mccCcy.equals(this.principalCcy)) {
            this.fxRate1 = 1.0d;
        } else {
            qv = this.quotesLoader.fetchFXQuoteValue(this.mccCcy, this.principalCcy);
            if (qv != null) {
                this.fxRate1 = qv.getClose();
            }
        }

        if (Util.isEmpty(this.principal2Ccy)) {
            return;
        }
        qv = null;
        if (this.mccCcy.equals(this.principal2Ccy)) {
            this.fxRate2 = 1.0d;
        } else {
            qv = this.quotesLoader.fetchFXQuoteValue(this.mccCcy, this.principal2Ccy);
            if (qv != null) {
                this.fxRate2 = qv.getClose();
            }
        }
    }

    /*
     * GSM: fix building underlyings -> take in two-legs trades, each trade based on the leg attribute. Also take
     * into consideration the trade direction to take the principal and the secondary leg data.
     */
    @SuppressWarnings("unchecked")
    void buildUnderlyings(Product product) {

        // derivative
        if ((product instanceof CollateralExposure)) {
            CollateralExposure collatExpo = (CollateralExposure) product;
            if (SantPortfolioBreakdownReport.is2Legs(((CollateralExposure) product).getUnderlyingType())) {
                this.underlying1 = (String) collatExpo.getAttribute("UNDERLYING_1");
            } else {
                this.underlying1 = (String) collatExpo.getAttribute("UNDERLYING");
            }
            this.underlying2 = (String) collatExpo.getAttribute("UNDERLYING_2");

        } else if (product instanceof Repo) {
            try {
                Product secUnderlying = DSConnection.getDefault().getRemoteProduct()
                        .getProduct(((Repo) product).getUnderlyingSecurityId());
                this.underlying1 = secUnderlying.getDescription();
            } catch (Exception e) {
                Log.error(this, "Cannot retrieve security", e);
            }
        } else if (product instanceof SecLending) {
            SecLending secLending = (SecLending) product;
            final Vector<Collateral> leftCollaterals = secLending.getLeftCollaterals();
            if (leftCollaterals.size() > 0) {
                this.underlying1 = leftCollaterals.get(0).getDescription();
            }
        } else if (product instanceof PerformanceSwap) {
            PerformanceSwap brs = (PerformanceSwap) product;
            buildPerformanceSwapUnderlying(brs);
        }
    }

    private void buildPerformanceSwapUnderlying(PerformanceSwap brs) {
        //Fill underlyng_1
        PerformanceSwapLeg leg1 = (PerformanceSwapLeg) brs.getPrimaryLeg();
        this.underlying1 = leg1.getReferenceProduct().getDescription();

        //Fill underlying_2
        SwapLeg leg2 = (SwapLeg) brs.getSecondaryLeg();
        this.underlying2 = leg2.getLegType() + leg2.getFixedRate();
        String legType = leg2.getLegType();
        switch (legType) {
            case "Fixed":
                this.underlying2 = legType + " - " + leg2.getFixedRate();
                break;
            case "Float":
                this.underlying2 = leg2.getRateIndex().toString();
                break;

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

        CollateralExposure collatExpo;
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

    public JDate getTradeDate() {
        return this.tradeDate;
    }

    public String getUndelying1() {
        return this.underlying1;
    }

    public String getUndelying2() {
        return this.underlying2;
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

    public DisplayValue getFXRate1() {
        return ((this.fxRate1 == null) || Double.isNaN(this.fxRate1)) ? null : CollateralUtilities.formatFXQuote(
                this.fxRate1, this.principalCcy, this.mccCcy);
    }

    public DisplayValue getFXRate2() {
        return ((this.fxRate2 == null) || Double.isNaN(this.fxRate2)) ? null : CollateralUtilities.formatFXQuote(
                this.fxRate2, this.principal2Ccy, this.mccCcy);
    }

    public DisplayValue getInitialCollateral() {
        return ((this.initialCollateral == null) || Double.isNaN(this.initialCollateral)) ? null
                : CollateralUtilities.formatAmount(this.initialCollateral, this.principalCcy);
    }

    public String getInstrument() {
        return this.instrument;
    }

    /**
     * @return the direction
     */
    public String getDirection() {
        return this.direction;
    }

    // for repo enhacements
    public double getPoolFactor() {
        return this.pool_factor;
    }

}
