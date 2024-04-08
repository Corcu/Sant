package calypsox.repoccp.model.lch;

import calypsox.repoccp.ReconCCPConstants;
import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.ReconCCPMatchingResult;
import calypsox.repoccp.model.ReconCCPTrade;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.LegalEntityTolerance;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.*;
import java.util.*;

import static calypsox.repoccp.ReconCCPConstants.TOLERANCE;
import static calypsox.repoccp.ReconCCPConstants.TRADE_KWD_BUYER_SELLER_REF;
import static calypsox.repoccp.ReconCCPUtil.isBondCalypso;
import static calypsox.repoccp.ReconCCPUtil.isRepoCalypso;

/**
 * @author aalonsop
 */

public abstract class LCHTrade extends ReconCCPTrade {

    private String dealerId;
    private String dealerName;
    private String clearerId;
    private String clearerName;
    private String registrationStatus;
    private String tradeDate;
    private String tradeTime;
    private String lchTradeReference;
    private String buyerSellerReference;
    private String tradeSourceReference;
    private String tradeSourceCode;
    private String tradeSourceName;
    private String lchNovatedTradeReference;
    private String buyerSeller;
    private String houseClient;
    private String tradeType;
    private String isin;
    private String isinName;
    private String lchMarketCode;
    private double tradePrice;
    private double nominal;
    private String nominalCurrency;
    private String cashCurrency;
    private String registeredRejectedDate;
    private String registeredRejectedTime;
    private String memberCSD;
    private double accruedCoupon;
    private String intendedSettlementDate;
    private double cashAmount;
    private String mic;
    private double haircut;
    private String uniqueTradeIdentifier;
    private String repoType;
    private String repoStartDate;
    private double repoStartCash;
    private String repoEndDate;
    private double repoEndCash;
    private double repoRate;
    private double forfeitAmount;
    private String portfolioCode;
    private String dayCountConvention;
    private double impliedRepoRate;
    private double actualRepoRate;
    LCHSetIdentifier identifier = new LCHSetIdentifier();

    private LCHNetPositions netPositionNettingSet;

    private LCHNetPositions netPositionObligationSet;

    public static final String Mx_ELECPLATID = "Mx ELECPLATID";
    public static final String Mx_Electplatid = "Mx Electplatid";

    public static final String Mx_Electplatf ="Mx Electplatf";

    public static final String Mx_GID = "Mx GID";

    public LCHNetPositions getNetPositionNettingSet() {
        return netPositionNettingSet;
    }

    public void setNetPositionNettingSet(LCHNetPositions netPosition) {
        this.netPositionNettingSet = netPosition;
    }

    public LCHNetPositions getNetPositionObligationSet() {
        return netPositionObligationSet;
    }

    public void setNetPositionObligationSet(LCHNetPositions netPosition) {
        this.netPositionObligationSet = netPosition;
    }

    public LCHSetIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(LCHSetIdentifier identifier) {
        this.identifier = identifier;
    }

    public String getDealerId() {
        return dealerId;
    }

    public void setDealerId(String dealerId) {
        this.dealerId = dealerId;
    }

    public String getDealerName() {
        return dealerName;
    }

    public void setDealerName(String dealerName) {
        this.dealerName = dealerName;
    }

    public String getClearerId() {
        return clearerId;
    }

    public void setClearerId(String clearerId) {
        this.clearerId = clearerId;
    }

    public String getClearerName() {
        return clearerName;
    }

    public void setClearerName(String clearerName) {
        this.clearerName = clearerName;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(String tradeDate) {
        this.tradeDate = tradeDate;
    }

    public String getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(String tradeTime) {
        this.tradeTime = tradeTime;
    }

    public String getLchTradeReference() {
        return lchTradeReference;
    }

    public void setLchTradeReference(String lchTradeReference) {
        this.lchTradeReference = lchTradeReference;
    }

    public String getBuyerSellerReference() {
        return buyerSellerReference;
    }

    public void setBuyerSellerReference(String buyerSellerReference) {
        this.buyerSellerReference = buyerSellerReference;
    }

    public String getTradeSourceReference() {
        return tradeSourceReference;
    }

    public void setTradeSourceReference(String tradeSourceReference) {
        this.tradeSourceReference = tradeSourceReference;
    }

    public String getTradeSourceCode() {
        return tradeSourceCode;
    }

    public void setTradeSourceCode(String tradeSourceCode) {
        this.tradeSourceCode = tradeSourceCode;
    }

    public String getTradeSourceName() {
        return tradeSourceName;
    }

    public void setTradeSourceName(String tradeSourceName) {
        this.tradeSourceName = tradeSourceName;
    }

    public String getLchNovatedTradeReference() {
        return lchNovatedTradeReference;
    }

    public void setLchNovatedTradeReference(String lchNovatedTradeReference) {
        this.lchNovatedTradeReference = lchNovatedTradeReference;
    }

    public String getBuyerSeller() {
        return buyerSeller;
    }

    public void setBuyerSeller(String buyerSeller) {
        this.buyerSeller = buyerSeller;
    }

    public String getHouseClient() {
        return houseClient;
    }

    public void setHouseClient(String houseClient) {
        this.houseClient = houseClient;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getIsinName() {
        return isinName;
    }

    public void setIsinName(String isinName) {
        this.isinName = isinName;
    }

    public String getLchMarketCode() {
        return lchMarketCode;
    }

    public void setLchMarketCode(String lchMarketCode) {
        this.lchMarketCode = lchMarketCode;
    }

    public double getTradePrice() {
        return tradePrice;
    }

    public void setTradePrice(double tradePrice) {
        this.tradePrice = tradePrice;
    }

    public double getNominal() {
        return nominal;
    }

    public void setNominal(double nominal) {
        this.nominal = nominal;
    }

    public String getNominalCurrency() {
        return nominalCurrency;
    }

    public void setNominalCurrency(String nominalCurrency) {
        this.nominalCurrency = nominalCurrency;
    }

    public String getCashCurrency() {
        return cashCurrency;
    }

    public void setCashCurrency(String cashCurrency) {
        this.cashCurrency = cashCurrency;
    }

    public String getRegisteredRejectedDate() {
        return registeredRejectedDate;
    }

    public void setRegisteredRejectedDate(String registeredRejectedDate) {
        this.registeredRejectedDate = registeredRejectedDate;
    }

    public String getRegisteredRejectedTime() {
        return registeredRejectedTime;
    }

    public void setRegisteredRejectedTime(String registeredRejectedTime) {
        this.registeredRejectedTime = registeredRejectedTime;
    }

    public String getMemberCSD() {
        return memberCSD;
    }

    public void setMemberCSD(String memberCSD) {
        this.memberCSD = memberCSD;
    }

    public double getAccruedCoupon() {
        return accruedCoupon;
    }

    public void setAccruedCoupon(double accruedCoupon) {
        this.accruedCoupon = accruedCoupon;
    }

    public String getIntendedSettlementDate() {
        return intendedSettlementDate;
    }

    public void setIntendedSettlementDate(String intendedSettlementDate) {
        this.intendedSettlementDate = intendedSettlementDate;
    }

    public double getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(double cashAmount) {
        this.cashAmount = cashAmount;
    }

    public String getMIC() {
        return mic;
    }

    public void setMIC(String mic) {
        this.mic = mic;
    }

    public double getHaircut() {
        return haircut;
    }

    public void setHaircut(double haircut) {
        this.haircut = haircut;
    }

    public String getUniqueTradeIdentifier() {
        return uniqueTradeIdentifier;
    }

    public void setUniqueTradeIdentifier(String uniqueTradeIdentifier) {
        this.uniqueTradeIdentifier = uniqueTradeIdentifier;
    }

    public double getImpliedRepoRate() {
        return impliedRepoRate;
    }

    public void setImpliedRepoRate(double impliedRepoRate) {
        this.impliedRepoRate = impliedRepoRate;
    }

    public double getActualRepoRate() {
        return actualRepoRate;
    }

    public void setActualRepoRate(double actualRepoRate) {
        this.actualRepoRate = actualRepoRate;
    }

    public String getDayCountConvention() {
        return dayCountConvention;
    }

    public void setDayCountConvention(String dayCountConvention) {
        this.dayCountConvention = dayCountConvention;
    }

    public String getPortfolioCode() {
        return portfolioCode;
    }

    public void setPortfolioCode(String portfolioCode) {
        this.portfolioCode = portfolioCode;
    }

    public String getRepoEndDate() {
        return repoEndDate;
    }

    public void setRepoEndDate(String repoEndDate) {
        this.repoEndDate = repoEndDate;
    }

    public double getRepoStartCash() {
        return repoStartCash;
    }

    public void setRepoStartCash(double repoStartCash) {
        this.repoStartCash = repoStartCash;
    }

    public double getForfeitAmount() {
        return forfeitAmount;
    }

    public void setForfeitAmount(double forfeitAmount) {
        this.forfeitAmount = forfeitAmount;
    }

    public String getRepoType() {
        return repoType;
    }

    public void setRepoType(String repoType) {
        this.repoType = repoType;
    }

    public double getRepoRate() {
        return repoRate;
    }

    public void setRepoRate(double repoRate) {
        this.repoRate = repoRate;
    }

    public double getRepoEndCash() {
        return repoEndCash;
    }

    public void setRepoEndCash(double repoEndCash) {
        this.repoEndCash = repoEndCash;
    }

    public String getRepoStartDate() {
        return repoStartDate;
    }

    public void setRepoStartDate(String repoStartDate) {
        this.repoStartDate = repoStartDate;
    }

    public ReconCCPMatchingResult matchFields(Trade calypsoTrade ) {
        return matchFields(calypsoTrade, TOLERANCE);
    }
    @Override
    public ReconCCPMatchingResult matchFields(Trade calypsoTrade, double amountTolerance ) {
        calypsoTrade.addKeyword(TRADE_KWD_BUYER_SELLER_REF, getBuyerSellerReference());
        ReconCCPMatchingResult result = new ReconCCPMatchingResult(true, calypsoTrade, new ArrayList<>(), new ArrayList<>());

        BigDecimal hundred = BigDecimal.valueOf(100);

        String calTradePrice = "";

        //B -> Reverse
        //S -> Repo

        //R0024483700
        //Trades 	<ns1:buyerSeller>B</ns1:buyerSeller>
        //Netting
        /*
        	<ns1:bondsReceiver>LCH</ns1:bondsReceiver>
					<ns1:bondsDeliverer>RDBSL</ns1:bondsDeliverer>
         */
        String buySell = getBuySell(calypsoTrade);
        boolean isBondExt = !this.getTradeType().equalsIgnoreCase("REPO");
        String isin = calypsoTrade.getProduct().getSecCode("ISIN");
        String settleCur = calypsoTrade.getSettleCurrency();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols();
        decimalSymbol.setDecimalSeparator('.');
        NumberFormat numberFormat = new DecimalFormat("0.00000", decimalSymbol);
        numberFormat.setGroupingUsed(false);
        numberFormat.setRoundingMode(RoundingMode.DOWN);

        //number format for accrued coupon when repo, 7 decimals
        NumberFormat repoAccrualNumberFormat = new DecimalFormat("0.0000000", decimalSymbol);
        numberFormat.setGroupingUsed(false);
        numberFormat.setRoundingMode(RoundingMode.DOWN);

        if (isBondCalypso(calypsoTrade)) {
            try {
                JDate intendedSettlementDate = JDate.valueOf(sdf.parse(this.getIntendedSettlementDate()));
                if (!intendedSettlementDate.equals(calypsoTrade.getSettleDate())) {
                    result.addError("Intended Settlement Date");
                }
            } catch (ParseException e) {
                Log.error(this, e.getCause());
                result.addError("Intended Settlement Date");
            }

            String extCashAmount = numberFormat.format(this.getCashAmount());
            Bond bond = (Bond) calypsoTrade.getProduct();
            String calCashAmount = numberFormat.format(Math.abs(bond.calcSettlementAmount(calypsoTrade)));

            if (!Objects.equals(extCashAmount, calCashAmount)) {

                Log.warn(this, "Cash Amount not equals for trade " + calypsoTrade.getLongId() +
                        " Calypso Cash Amount " + calCashAmount + " file Cash Amount " + extCashAmount +
                        ". Apply tolerance " + amountTolerance);
                if (!ReconCCPUtil.applyTolerance(numberFormat, extCashAmount, calCashAmount, amountTolerance)) {
                    result.addError("Cash Amount");
                } else {
                    result.addWarning("Cash Amount");
                }
            }

            if (calypsoTrade.getKeywords() != null) {
                String bondPrefix = calypsoTrade.getKeywordValue("MxInitialCleanPrice");
                String colId = calypsoTrade.getKeywordValue("EnteredTradeLocale");
                if (Util.isEmpty(colId)) {
                    colId = Util.getDisplayName(Locale.getDefault());
                }

                if (bondPrefix != null) {
                    calTradePrice = numberFormat.format(BigDecimal.valueOf(Util.stringToNumber(bondPrefix, Util.getLocale(colId))));
                }
            }
        } else if (isRepoCalypso(calypsoTrade)) {
            Repo product = ((Repo) calypsoTrade.getProduct());
            isin = product.getSecurity().getSecCode("ISIN");

            String calRepoSubtype = product.getSubType();
            String lchRepo = null;
            switch (getRepoType()) {
                case "Classic Repo":
                    lchRepo = "Standard";
                    break;
                case "GC":
                    lchRepo = "Triparty";
                    break;
                default:
                    lchRepo = getRepoType();
                    break;
            }

            //  if (calRepoSubtype.equalsIgnoreCase("Standard")) {
            //      calRepoSubtype = "Classic Repo";
            //  }
            if (!Objects.equals(calRepoSubtype, lchRepo)) {
                result.addError("Repo Type");
            }

            try {
                JDate calRepoStartDate = product.getStartDate();
                JDate extRepoStartDate = JDate.valueOf(sdf.parse(this.getRepoStartDate()));
                if (!calRepoStartDate.equals(extRepoStartDate)) {
                    result.addError("Start Date");
                }
            } catch (ParseException e) {
                Log.error(this, e.getCause());
                result.addError("Start Date");
            }

            try {
                JDate calRepoEndDate = product.getEndDate();
                JDate extRepoEndDate = JDate.valueOf(sdf.parse(this.getRepoEndDate()));
                if (!calRepoEndDate.equals(extRepoEndDate)) {
                    result.addError("End Date");
                }
            } catch (ParseException e) {
                Log.error(this, e.getCause());
                result.addError("End Date");
            }

            String calRepoRate = "";
            String extRepoRate = numberFormat.format(getRepoRate());

            String calStartCash = "";
            String extStartCash = numberFormat.format(getRepoStartCash());

            String calEndCash = "";
            String extEndCash = numberFormat.format(getRepoEndCash());
            try {
                JDate today = JDate.getNow();
                CashFlowSet flowSet = product.generateFlows(today);
                product.calculate(flowSet, PricingEnv.loadPE(DSConnection.getDefault().getDefaultPricingEnv(), new JDatetime()), today);
                Repo.computePrincipalAndInterest(flowSet, product);
                CashFlow lastFlow = Repo.getNetTotalFlow(flowSet);
                if (lastFlow != null && lastFlow.isKnown(today)) {
                    calEndCash = numberFormat.format(Math.abs(lastFlow.getAmount()));
                }
                if (!Objects.equals(calEndCash, extEndCash)) {
                    Log.warn(this, "End Cash not equals for trade " + calypsoTrade.getLongId() +
                            " Calypso End Cash " + calEndCash + " file End Cash " + extEndCash +
                            ". Apply tolerance " + amountTolerance);

                    if (!ReconCCPUtil.applyTolerance(numberFormat, calEndCash, extEndCash, amountTolerance)) {
                        result.addError("End Cash");
                    } else {
                        result.addWarning("End Cash");
                    }
                }
            } catch (FlowGenerationException | CloneNotSupportedException e) {
                Log.error(this, e.getCause());
                result.addError("End Cash");
            }

            if (calypsoTrade.getProduct() instanceof SecFinance) {
                SecFinance secFinance = (SecFinance) calypsoTrade.getProduct();
                if (secFinance != null) {
                    if (secFinance.hasCash()) {
                        calRepoRate = numberFormat.format(BigDecimal.valueOf(secFinance.getCash().getFixedRate()).multiply(hundred));
                        calStartCash = numberFormat.format(Math.abs(secFinance.getCash().getPrincipal()));
                    }
                    Vector<Collateral> collaterals = secFinance.getAllSecCollaterals();
                    if (!collaterals.isEmpty()) {
                        calTradePrice = numberFormat.format(BigDecimal.valueOf(collaterals.get(0).computeAllInPrice()).multiply(hundred));
                    }
                }
            }

            if (!Objects.equals(calRepoRate, extRepoRate)) {
                result.addWarning("Repo Rate");
            }

            if (!Objects.equals(calStartCash, extStartCash)) {
                Log.warn(this, "Start Cash not equals for trade " + calypsoTrade.getLongId() +
                        " Calypso Start Cash " + calStartCash + " file Start Cash " + extStartCash +
                        ". Apply tolerance " + ReconCCPConstants.TOLERANCE);
                if (!ReconCCPUtil.applyTolerance(numberFormat, calStartCash, extStartCash)) {
                    result.addError("Start Cash");
                } else {
                    result.addWarning("Start Cash");
                }
            }
        }

        String calNominal = numberFormat.format(Math.abs(calypsoTrade.computeNominal()));

        String extTradePrice = numberFormat.format(this.getTradePrice());
        String extNominal = numberFormat.format(this.getNominal());
        String calAccruedCoupon = numberFormat.format(BigDecimal.valueOf(calypsoTrade.getAccrual()).multiply(hundred));
        String extAccruedCoupon = numberFormat.format(this.getAccruedCoupon());
        if (!isBondCalypso(calypsoTrade)) {
            extAccruedCoupon = repoAccrualNumberFormat.format(this.getAccruedCoupon());
            calAccruedCoupon = repoAccrualNumberFormat.format(BigDecimal.valueOf(calypsoTrade.getAccrual()).multiply(hundred));
        }

        if (!Objects.equals(calAccruedCoupon, extAccruedCoupon)) {
            Log.warn(this, "Warning unmatched field Accrued Coupon for trade id " + calypsoTrade.getLongId());
            result.addWarning("Accrued Coupon");
        }

        if (!Objects.equals(this.getRegistrationStatus(), "Registered")) {
            result.addError("Registration Status");
        }

        //reverse buy/sell

        //  String theirBuySell = "B".equals(buySell)?"S":"B";

        if (!Objects.equals(buySell, this.getBuyerSeller())) {
            result.addError("Buyer/Seller");
        }

        if (isBondCalypso(calypsoTrade) != isBondExt) {
            result.addError("Trade Type");
        }

        if (!Objects.equals(this.getIsin(), isin)) {
            result.addError("ISIN");
        }

        if (!Objects.equals(settleCur, this.getCashCurrency())) {
            result.addError("Cash Currency");
        }

        if (!Objects.equals(calTradePrice, extTradePrice)) {
            Log.warn(this, "Warning unmatched field Trade Price for trade id " + calypsoTrade.getLongId());
            result.addWarning("Trade Price");
        }

        if (!Objects.equals(calNominal, extNominal)) {
            result.addError("Nominal");
        }

        return result;
    }

    public String getBuySell(Trade trade) {
        double qty = 0.0D;

        JDate valDate = JDate.getNow();
        if (trade.getProduct() instanceof SecFinance) {
            SecFinance secFinance = (SecFinance) trade.getProduct();
            if (valDate.before(secFinance.getStartDate())) {
                valDate = secFinance.getStartDate();
            }

            qty = trade.getProduct().computeQuantity(trade, valDate);
        }

        if (qty == 0) {
            qty = trade.getQuantity();
            if (qty == 0.0D) {
                qty = trade.getAllocatedQuantity();
            }
        }

        return qty >= 0.0D ? "B" : "S";
    }

    public abstract boolean matchReference(Trade calypsoTrade);

    protected abstract long getExternalId();

    @Override
    public String toString() {
        return "LCHTrade{" +
                "dealerId='" + dealerId + '\'' +
                ", buyerSellerReference='" + buyerSellerReference + '\'' +
                ", dealerName='" + dealerName + '\'' +
                ", clearerId='" + clearerId + '\'' +
                ", clearerName='" + clearerName + '\'' +
                ", registrationStatus='" + registrationStatus + '\'' +
                ", tradeDate='" + tradeDate + '\'' +
                ", tradeTime='" + tradeTime + '\'' +
                ", lchTradeReference='" + lchTradeReference + '\'' +
                ", tradeSourceReference='" + tradeSourceReference + '\'' +
                ", tradeSourceCode='" + tradeSourceCode + '\'' +
                ", tradeSourceName='" + tradeSourceName + '\'' +
                ", lchNovatedTradeReference='" + lchNovatedTradeReference + '\'' +
                ", buyerSeller='" + buyerSeller + '\'' +
                ", houseClient='" + houseClient + '\'' +
                ", tradeType='" + tradeType + '\'' +
                ", isin='" + isin + '\'' +
                ", isinName='" + isinName + '\'' +
                ", lchMarketCode='" + lchMarketCode + '\'' +
                ", tradePrice=" + tradePrice +
                ", nominal=" + nominal +
                ", nominalCurrency='" + nominalCurrency + '\'' +
                ", cashCurrency='" + cashCurrency + '\'' +
                ", registeredRejectedDate='" + registeredRejectedDate + '\'' +
                ", registeredRejectedTime='" + registeredRejectedTime + '\'' +
                ", memberCSD='" + memberCSD + '\'' +
                ", accruedCoupon=" + accruedCoupon +
                ", intendedSettlementDate='" + intendedSettlementDate + '\'' +
                ", cashAmount=" + cashAmount +
                ", mic='" + mic + '\'' +
                ", haircut=" + haircut +
                ", uniqueTradeIdentifier='" + uniqueTradeIdentifier + '\'' +
                ", repoType='" + repoType + '\'' +
                ", repoStartDate='" + repoStartDate + '\'' +
                ", repoStartCash=" + repoStartCash +
                ", repoEndDate='" + repoEndDate + '\'' +
                ", repoEndCash=" + repoEndCash +
                ", repoRate=" + repoRate +
                ", forfeitAmount=" + forfeitAmount +
                ", portfolioCode='" + portfolioCode + '\'' +
                ", dayCountConvention='" + dayCountConvention + '\'' +
                ", impliedRepoRate=" + impliedRepoRate +
                ", actualRepoRate=" + actualRepoRate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LCHTrade lchTrade = (LCHTrade) o;
        return Double.compare(lchTrade.tradePrice, tradePrice) == 0 && Double.compare(lchTrade.nominal, nominal) == 0
                && Double.compare(lchTrade.accruedCoupon, accruedCoupon) == 0 &&
                Double.compare(lchTrade.cashAmount, cashAmount) == 0 && Double.compare(lchTrade.haircut, haircut) == 0
                && Double.compare(lchTrade.repoStartCash, repoStartCash) == 0 &&
                Double.compare(lchTrade.repoEndCash, repoEndCash) == 0 && Double.compare(lchTrade.repoRate, repoRate) == 0
                && Double.compare(lchTrade.forfeitAmount, forfeitAmount) == 0 &&
                Double.compare(lchTrade.impliedRepoRate, impliedRepoRate) == 0 &&
                Double.compare(lchTrade.actualRepoRate, actualRepoRate) == 0 &&
                Objects.equals(dealerId, lchTrade.dealerId) &&
                Objects.equals(dealerName, lchTrade.dealerName) &&
                Objects.equals(clearerId, lchTrade.clearerId) &&
                Objects.equals(clearerName, lchTrade.clearerName) &&
                Objects.equals(registrationStatus, lchTrade.registrationStatus) &&
                Objects.equals(tradeDate, lchTrade.tradeDate) &&
                Objects.equals(tradeTime, lchTrade.tradeTime) &&
                Objects.equals(lchTradeReference, lchTrade.lchTradeReference) &&
                Objects.equals(buyerSellerReference, lchTrade.buyerSellerReference) &&
                Objects.equals(tradeSourceReference, lchTrade.tradeSourceReference) &&
                Objects.equals(tradeSourceCode, lchTrade.tradeSourceCode) &&
                Objects.equals(tradeSourceName, lchTrade.tradeSourceName) &&
                Objects.equals(lchNovatedTradeReference, lchTrade.lchNovatedTradeReference) &&
                Objects.equals(buyerSeller, lchTrade.buyerSeller) &&
                Objects.equals(houseClient, lchTrade.houseClient) &&
                Objects.equals(tradeType, lchTrade.tradeType) &&
                Objects.equals(isin, lchTrade.isin) &&
                Objects.equals(isinName, lchTrade.isinName) &&
                Objects.equals(lchMarketCode, lchTrade.lchMarketCode) &&
                Objects.equals(nominalCurrency, lchTrade.nominalCurrency) &&
                Objects.equals(cashCurrency, lchTrade.cashCurrency) &&
                Objects.equals(registeredRejectedDate, lchTrade.registeredRejectedDate) &&
                Objects.equals(registeredRejectedTime, lchTrade.registeredRejectedTime) &&
                Objects.equals(memberCSD, lchTrade.memberCSD) &&
                Objects.equals(intendedSettlementDate, lchTrade.intendedSettlementDate) &&
                Objects.equals(mic, lchTrade.mic) &&
                Objects.equals(uniqueTradeIdentifier, lchTrade.uniqueTradeIdentifier) &&
                Objects.equals(repoType, lchTrade.repoType) && Objects.equals(repoStartDate, lchTrade.repoStartDate) &&
                Objects.equals(repoEndDate, lchTrade.repoEndDate) && Objects.equals(portfolioCode, lchTrade.portfolioCode)
                && Objects.equals(dayCountConvention, lchTrade.dayCountConvention) &&
                Objects.equals(identifier, lchTrade.identifier) &&
                Objects.equals(netPositionNettingSet, lchTrade.netPositionNettingSet) &&
                Objects.equals(netPositionObligationSet, lchTrade.netPositionObligationSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dealerId, dealerName, clearerId, clearerName, registrationStatus, tradeDate, tradeTime,
                lchTradeReference, buyerSellerReference, tradeSourceReference, tradeSourceCode, tradeSourceName,
                lchNovatedTradeReference, buyerSeller, houseClient, tradeType, isin, isinName, lchMarketCode,
                tradePrice, nominal, nominalCurrency, cashCurrency, registeredRejectedDate,
                registeredRejectedTime, memberCSD, accruedCoupon, intendedSettlementDate, cashAmount,
                mic, haircut, uniqueTradeIdentifier, repoType, repoStartDate, repoStartCash, repoEndDate,
                repoEndCash, repoRate, forfeitAmount, portfolioCode, dayCountConvention, impliedRepoRate,
                actualRepoRate, identifier, netPositionNettingSet, netPositionObligationSet);
    }
}
