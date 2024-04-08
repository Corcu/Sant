package calypsox.tk.confirmation.builder.bond;

import calypsox.tk.confirmation.builder.CalConfirmationFinantialDataBuilder;
import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Security;
import com.calypso.tk.report.BondReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.util.FdnUtilProvider;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;
import java.util.Optional;
import java.util.Vector;

/**
 * @author dmenendd
 */
public class BondConfirmFinantialDataBuilder extends CalConfirmationFinantialDataBuilder {

    Bond bond;
    JDate msgCreationDate;
    MathContext ctx = MathContext.DECIMAL64;

    PricingEnv pricingEnv;

    private final String mxAdditionalChargesKwd = "Mx ADDITIONAL CHARGES";

    public BondConfirmFinantialDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        if (trade.getProduct() instanceof Bond) {
            bond = (Bond) trade.getProduct();
            this.referenceSecurity = (Security) bond.getSecurity();
            this.msgCreationDate = JDate.valueOf(this.boMessage.getCreationDate());
            this.pricingEnv = PricingEnv.loadPE("OFFICIAL_ACCOUNTING", this.boMessage.getCreationDate());
        }
    }

    public String buildOperationDate() {
        return Optional.ofNullable(trade).map(Trade::getTradeDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }

    public String buildCptyContractDate() {
        return buildOperationDate();
    }

    public String buildEntryDate() {
        return Optional.ofNullable(trade).map(Trade::getTradeDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }

    public String buildPortfolio() {
        return trade.getBook().getName();
    }

    public String buildDirection() {
        String buySell = "Buy";
        int buySellInd = Optional.ofNullable(bond).map(bono -> bono.getBuySell(trade))
                .orElse(1);
        if (buySellInd != 1) {
            buySell = "Sell";
        }
        return buySell;

    }

    public String buildSettlementDate() {
        return Optional.ofNullable(trade).map(Trade::getSettleDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }

    public String buildQuantity() {
        return Optional.ofNullable(trade).map(Trade::getQuantity).map(this::formatNumberAbs).orElse("");
    }

    public String buildNominalValueAmount() {
        double quantity = Optional.ofNullable(trade).map(Trade::getQuantity).orElse(0.0);
        double faceValue = Optional.ofNullable(bond).map(Bond::getFaceValue).orElse(0.0);

        return formatNumberAbs(quantity * faceValue);
    }


    public String buildDirtyPrice() {
        return Optional.ofNullable(this.trade)
                .map(Trade::getNegociatedPrice)
                .map(this::getSecurityPriceDisplayValue)
                .map(this::adjustPriceBase)
                .map(this::formatNumber)
                .orElse("0");
    }

    public String buildCleanPrice() {
        return Optional.ofNullable(this.trade)
                .map(Trade::getTradePrice)
                .map(p -> p /getNotionalIndexFactor())
                .map(this::getSecurityPriceDisplayValue)
                .map(this::adjustPriceBase)
                .map(this::formatNumber)
                .orElse("0");
    }

    public String buildTradeCurrency() {
        return Optional.ofNullable(trade).map(Trade::getTradeCurrency).orElse("");
    }

    public String buildLotSize() {
        return Optional.ofNullable(bond).map(Bond::getFaceValue)
                .map(this::formatNumber).orElse(String.valueOf(0));
    }

    public String buildDescriptionOfTheSecurityCoupon() {
        return Optional.ofNullable(bond).map(Bond::getCoupon).map(this::formatPercentage)
                .map(this::formatNumber).orElse(String.valueOf(0));
    }

    public String buildSBSDProduct() {
        return trade != null ? trade.getKeywordValue("BondForwardType") != null ? "Delivery".equalsIgnoreCase(trade.getKeywordValue("BondForwardType")) ?
                "Bond Fwd Delivery" : "Bond Fwd Cash" : "" : "";
    }

    public String buildSUTI() {
        return trade.getKeywordValue("UTI_REFERENCE") != null ? trade.getKeywordValue("UTI_REFERENCE") : trade.getKeywordValue("TempUTITradeId");
    }

    public String buildReferenceId() {
        return Optional.ofNullable(referenceSecurity)
                .map(Security::getSecurity)
                .map(sec -> sec.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_REF_INTERNA))
                .orElse("");
    }

    public String buildAccrual() {
        BigDecimal unitAccrual = BigDecimal.valueOf(trade.getAccrual());
        Bond product = (Bond) trade.getProduct();
        double bondFaceValue = Optional.ofNullable(product).map(bond -> bond.getFaceValue(trade.getSettleDate()))
                .orElse(0.0D);
        BigDecimal notional = BigDecimal.valueOf(bondFaceValue * Math.abs(trade.getQuantity()));
        BigDecimal totalAccrual = unitAccrual.multiply(notional);
        return Optional.of(totalAccrual).map(BigDecimal::doubleValue)
                .map(this::formatNumber).orElse(String.valueOf(0));
    }

    public String buildAccruedInterest() {
        String accrual = "0";
        TradeReportStyle rStyle = new TradeReportStyle();
        ReportRow row = new ReportRow(this.trade);
        row.setProperty("ValuationDatetime", trade.getTradeDate());
        PricerMeasureSet measureset = new PricerMeasureSet();
        measureset.add(new PricerMeasure(PricerMeasure.ACCRUAL_UNIT));
        row.setProperty("PricerMeasures", measureset);
        PricingEnv pricingEnv = PricingEnv.loadPE("DirtyPrice", this.msgCreationDate.getJDatetime());
        if (pricingEnv != null) {
            row.setProperty("PricingEnv", pricingEnv);
        }
        String accrualUnit = "Pricer.ACCRUAL_UNIT";
        String[] columns = {accrualUnit};
        rStyle.precalculateColumnValues(row, columns, new Vector<String>());
        Object amt = rStyle.getColumnValue(row, accrualUnit, new Vector<String>());
        if (amt instanceof Rate) {
            BigDecimal amtDecimal = BigDecimal.valueOf(((Rate) amt).get());
            amtDecimal = amtDecimal.multiply(BigDecimal.valueOf(100));
            accrual = amtDecimal.round(ctx).toString();
        }
        return accrual;
    }

    //Next coupon
    public String buildExCouponDate() {
        return Optional.ofNullable(bond).map(b -> b.getNextCouponDate(this.msgCreationDate))
                .map(JDate::toString).orElse("");
    }

    public String buildYield() {
        ReportRow row = new ReportRow(this.trade);
        BondReportStyle rStyle = new BondReportStyle();
        return Optional.ofNullable(this.trade).map(t -> rStyle.getColumnValue(row, BondReportStyle.YIELD, new Vector<String>()))
                .filter(amt -> amt instanceof DisplayValue)
                .map(amt -> BigDecimal.valueOf(((DisplayValue) amt).get()))
                .map(d -> d.multiply(BigDecimal.valueOf(100)))
                .map(d -> d.round(ctx))
                .map(BigDecimal::toString)
                .orElse("0");
    }


    //Accrued Cash Amount in bond currency
    public String buildTotalAmount() {
        return Optional.ofNullable(this.trade)
                .map(Trade::getProduct)
                .filter(product -> product instanceof Bond)
                .map(bond -> ((Bond)bond).calcPurchasedAmount(trade))
                .map(Math::abs)
                .map(principalAmt ->principalAmt + getAccrual(trade))
                .map(this::formatNumber)
                .orElse("0");
    }
    private double getAccrual(Trade trade) {
        TradeReportStyle tradeReportStyle=new TradeReportStyle();
        ReportRow row=new ReportRow(trade);
        row.setProperty(ReportRow.VALUATION_DATETIME,new JDatetime());
        return Optional.ofNullable(tradeReportStyle.getColumnValue(row, TradeReportStyle.TRADE_ACCRUAL, new Vector<>()))
                .filter(accrualRaw -> accrualRaw instanceof SignedAmount)
                .map(accrualAmt -> ((SignedAmount) accrualAmt).get())
                .orElse(0.0d);
    }


    public String buildPrincipal() {
        return Optional.ofNullable(trade).map(t -> t.computeNominal(this.msgCreationDate))
                .map(p -> p * getNotionalIndexFactor())
                .map(BigDecimal::new)
                .map(d -> d.round(ctx))
                .map(BigDecimal::abs)
                .map(BigDecimal::toString)
                .orElse("0");
    }

    public String buildDualTotalSettlementAmt() {
        return Optional.ofNullable(this.trade)
                .map(Trade::getProduct)
                .map(bond -> bond.calcSettlementAmount(trade))
                .map(Math::abs)
                .map(this::formatNumber)
                .orElse("0");
    }

    public String buildDualTotalSettlementCurrency() {
        return Optional.ofNullable(trade).map(Trade::getSettleCurrency)
                .orElse("");
    }

    public String buildMethodOfPayment() {
        return "1";
    }

    @Override
    public String buildMifidCostsExpensesAmount() {
        // USD#CT=1000#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0#0
        String mifidAmt = getMifidKwdParsedValue(1);
        int index = mifidAmt.lastIndexOf("=") + 1;
        if (index < mifidAmt.length()) {
            mifidAmt = mifidAmt.substring(mifidAmt.lastIndexOf("=") + 1);
        }
        return mifidAmt;
    }

    @Override
    public String buildMifidCostsExpensesCurr() {
        return getMifidKwdParsedValue(0);
    }

    private String getMifidKwdParsedValue(int columnIndex) {
        String mifidValue = "";
        String[] parsedMifidInfo = getMxAdditionalChargesKwd().split("#");
        if (parsedMifidInfo.length > 1 && columnIndex < 2) {
            mifidValue = parsedMifidInfo[columnIndex];
        }
        return mifidValue;
    }

    private String getMxAdditionalChargesKwd() {
        return Optional.ofNullable(trade)
                .map(t -> t.getKeywordValue(mxAdditionalChargesKwd))
                .orElse("");
    }

    public DisplayValue getSecurityPriceDisplayValue(double price) {
        return Optional.ofNullable(this.bond)
                .map(Bond::getPriceDisplayValue)
                .map(displayValue -> {
                    displayValue.set(price);
                    return displayValue;
                })
                .orElse(new BondPrice(price, 100));
    }

    private double adjustPriceBase(DisplayValue displayValue) {
        return Optional.ofNullable(displayValue)
                .map(DisplayValue::toString)
                .map(stringValue -> FdnUtilProvider.getNumberFormattingUtil().stringToNumber(stringValue, null, null))
                .orElse(1.00D);
    }

   /** protected String adjustAndFormatPrice(DisplayValue displayValue){
        return Optional.ofNullable(displayValue)
                .map(DisplayValue::toString)
                .map(stringValue -> FdnUtilProvider.getNumberFormattingUtil().stringToNumber(stringValue, null, null))
                .orElse(1.00D);
    }**/

    protected String formatNumberAbs(double number) {
        return String.format(Locale.ENGLISH, "%.6f", Math.abs(number));
    }

    protected double formatPercentage(double number) {
        return number * 100;
    }


    public double getNotionalIndexFactor() {
        return Optional.ofNullable(this.bond)
                .map(bond -> {
                    double notionalFactor = 1.0d;
                    try {
                        if (pricingEnv != null) {
                            notionalFactor = bond.getNotionalIndexFactor(this.trade.getSettleDate(), pricingEnv.getQuoteSet());
                        }
                    } catch (FlowGenerationException exc) {
                        Log.error(this, exc.getCause());
                    }
                    return notionalFactor;
                })
                .orElse(1.0d);
    }

}
