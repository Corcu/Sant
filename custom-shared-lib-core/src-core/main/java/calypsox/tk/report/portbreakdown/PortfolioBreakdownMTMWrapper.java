package calypsox.tk.report.portbreakdown;

import calypsox.tk.core.SantPricerMeasure;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.SecLending;
import org.apache.commons.lang.StringUtils;

public class PortfolioBreakdownMTMWrapper {

        private Double npv;

        private Double npvBase;

        private Double marginCall;

        @SuppressWarnings("unused")
        private double marginCallBase;

        private double indepAmount;

        private double indepAmountBase;

        private String npvCcy;

        private String npvBaseCcy;

        private String indepAmountCcy;

        private String indepAmountBaseCcy;

        protected double closingPrice;

        // for RepoEnhacements
        protected double repoAccruedInterest;

        protected double repoAccruedBOInterest;
        protected double bondAccruedInterest;
        protected double cleanPrice;
        private double capitalFactor;
        protected String repoAccruedInterestCcy;
        protected String bondAccruedInterestCcy;
        protected String cleanPriceCcy;
        private String capitalFactorCcy;

        private String boReference;
        private final String boSystem;
        private final String foSystem;


        JDatetime valDate;

        public PortfolioBreakdownMTMWrapper(PLMark plMark, Trade trade,JDatetime valDate) {
                this.valDate=valDate;
                PLMarkValue plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_NPV);
                if (plValue != null) {
                    this.npv = plValue.getMarkValue();
                    this.npvCcy = plValue.getCurrency();
                }
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_NPV_BASE);
                if (plValue != null) {
                    this.npvBase = plValue.getMarkValue();
                    this.npvBaseCcy = plValue.getCurrency();
                }
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_INDEPENDENT_AMOUNT);
                if (plValue != null) {
                    this.indepAmount = plValue.getMarkValue();
                    this.indepAmountCcy = plValue.getCurrency();
                }
                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE);
                if (plValue != null) {
                    this.indepAmountBase = plValue.getMarkValue();
                    this.indepAmountBaseCcy = plValue.getCurrency();
                }
                // For ISMA Repo
                this.closingPrice=buildClosingPrice(trade,plMark);
                // closingPrice = null if CSA

                // To be changed but for a quick fix:
                // Use of PLMARK MARGIN_CALL for SecLending because haircut is taken into account
                if (trade.getProduct() instanceof SecLending) {
                    plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_MARGIN_CALL);
                    if (plValue != null) {
                        // Margin Call PLMark represents the value in term of CASH and not Security
                        // Therefore for a Loan of Security this value should be positive and negative for a Borrow of
                        // Security
                        this.marginCall = plValue.getMarkValue();
                    }
                }

            buildCleanPrice(trade,plMark);

                // for RepoEnhacements
                buildRepoAccruedInterest(trade,plMark);

                buildRepoAccruedBOInterest(trade, plMark);

                buildBondAccruedInterest(trade,plMark);

                plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_CAPITAL_FACTOR);
                if (plValue != null) {
                    this.capitalFactor = plValue.getMarkValue();
                    this.capitalFactorCcy = plValue.getCurrency();
                }
                this.boReference=buildBoReference(trade);
                this.boSystem=buildBOSystem(trade);
                this.boReference=buildBoReference(trade);
                this.foSystem=buildFOSystem(trade);
        }

        protected String buildBoReference(Trade trade){
            String boRef = trade.getKeywordValue("BO_REFERENCE");
            if(StringUtils.isBlank(boRef) && Product.PERFORMANCESWAP.equalsIgnoreCase(trade.getProductType())){
                boRef=String.valueOf(trade.getLongId());
            }
           return boRef;
        }

    protected void buildRepoAccruedInterest(Trade trade,PLMark plMark) {
        PLMarkValue plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_REPO_ACCRUED_INTEREST);
        if (plValue != null) {
            this.repoAccruedInterest = plValue.getMarkValue();
            this.repoAccruedInterestCcy = plValue.getCurrency();
        }
    }

    protected void buildRepoAccruedBOInterest(Trade trade,PLMark plMark) {
        buildRepoAccruedInterest(trade, plMark);
    }

    protected void buildBondAccruedInterest(Trade trade,PLMark plMark) {
        PLMarkValue plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_BOND_ACCRUED_INTEREST);
        if (plValue != null) {
            this.bondAccruedInterest = plValue.getMarkValue();
            this.bondAccruedInterestCcy = plValue.getCurrency();
        }
    }
        protected void buildCleanPrice(Trade trade,PLMark plMark){
            PLMarkValue plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_CLEAN_PRICE);
            if (plValue != null) {
                this.cleanPrice = plValue.getMarkValue();
                this.cleanPriceCcy = plValue.getCurrency();
            }
        }

        protected double buildClosingPrice(Trade trade,PLMark plMark){
            double closingPrice=0.0D;
            PLMarkValue plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_CLOSING_PRICE);
            if (plValue != null) {
                closingPrice=plValue.getMarkValue();
            }
            return closingPrice;
        }

        protected String buildBOSystem(Trade trade){
            return trade.getKeywordValue("BO_SYSTEM");
        }

        protected String buildFOSystem(Trade trade){
            return trade.getKeywordValue("FO_SYSTEM");
        }

        public DisplayValue getMarginCall() {
            return this.marginCall == null ? null : CollateralUtilities.formatAmount(this.marginCall, this.npvCcy);
        }

        public DisplayValue getNpv() {
            return this.npv == null ? null : CollateralUtilities.formatAmount(this.npv, this.npvCcy);
        }

        public DisplayValue getNpvBase() {
            return this.npvBase == null ? null : CollateralUtilities.formatAmount(this.npvBase, this.npvBaseCcy);
        }

        public DisplayValue getIndepAmount() {
            return CollateralUtilities.formatAmount(this.indepAmount,
                    this.indepAmountCcy);
        }

        public DisplayValue getIndepAmountBase() {
            return CollateralUtilities.formatAmount(this.indepAmountBase,
                    this.indepAmountBaseCcy);
        }

        // for RepoEnhacements
        public DisplayValue getRepoAccruedInterest() {
            return CollateralUtilities.formatAmount(this.repoAccruedInterest,
                    this.repoAccruedInterestCcy);
        }

        public DisplayValue getBondAccruedInterest() {
            return CollateralUtilities.formatAmount(this.bondAccruedInterest,
                    this.bondAccruedInterestCcy);
        }

    public double getBondAccruedInterestRaw() {
        return this.bondAccruedInterest;
    }
        public DisplayValue getCleanPrice() {
            return CollateralUtilities.formatAmount(this.cleanPrice,
                    this.cleanPriceCcy);
        }

    public double getCleanPriceRaw() {
        return this.cleanPrice;
    }

    public double getClosingPriceRaw() {
        return this.closingPrice;
    }

        public DisplayValue getCapitalFactor(Trade trade,JDate valDate) {
            return CollateralUtilities.formatAmount(this.capitalFactor,
                    this.capitalFactorCcy);
        }

        public String getClosingPrice() {
            // No rounding for closing price, displayed like it comes from SUSI
            return (new Amount(this.closingPrice)).toString();
        }

        public String getNpvCcy() {
            return this.npvCcy;
        }

        public String getNpvBaseCcy() {
            return this.npvBaseCcy;
        }

        public String getIndepAmountCcy() {
            return this.indepAmountCcy;
        }

        public String getIndepAmountBaseCcy() {
            return this.indepAmountBaseCcy;
        }

    public String getBoReference() {
        return boReference;
    }

    public String getBoSystem() {
        return boSystem;
    }

    public String getFoSystem() {
        return foSystem;
    }

    public DisplayValue getRepoAccruedBOInterest() {
        return CollateralUtilities.formatAmount(this.repoAccruedBOInterest,
                this.repoAccruedInterestCcy);
    }
}
