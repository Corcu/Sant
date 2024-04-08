package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.util.json.JSONAmount;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.apps.appkit.presentation.format.JDatetimeFormat;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import org.codehaus.jettison.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

/**
 * @author acd
 */
public abstract class SantBOCre {

    protected Long creId;
    protected String eventType = "";
    protected String creDescription = "";
    protected String creType = "";
    protected Long tradeId;
    protected Long creLinkedId;
    protected String originalEvent = "";
    protected JDatetime creationDate;
    protected JDate effectiveDate;
    protected JDate bookingDate;
    protected String creStatus = "";
    protected JDate settleDate;
    protected JDate tradeDate;
    protected String direction = "";
    protected Double amount1 = 0.0;
    protected String currency1 = "";
    protected String fixedFloating = "";
    protected String indexDefinition = "";
    protected Double rate = 0.0;
    protected String ratePositiveNegative = "";
    protected String identifier = "";
    protected String identifierIntraEOD = "";
    protected String identifierOBB = "";
    protected String portfolio = "";
    protected String processingOrg = "";
    protected String counterparty = "";
    protected String productType = "";
    protected String productSubType = "";
    protected Integer mcContractID;
    protected String mcContractType = "";
    protected Integer accountID;
    protected Double accountBalance = 0.0;
    protected String accountCreditDebit = "";
    protected String accountCurrency = "";
    protected String tradeCurrency = "";
    protected String ccpName = "";
    protected JDate startDate;
    protected JDate expiryDate;
    protected JDate cancelationDate;
    protected String settlementMethod = "";
    protected String transferAccount = "";
    protected String stm = "";
    protected String nettingType = "";
    protected Long nettingParent;

    protected String isin;
    protected String underlyingType;
    protected Double valorization;
    protected String portfolioStrategy;

    protected String partenonId = "";

    protected String ownIssuance = "";
    protected String bondType = "";
    protected String bedulaAttribute = "";
    protected Double retentionPercentage;
    protected Double negoPercentage;
    protected Double retentionAmount;
    protected Double netAmount;
    protected Double negoAmount;
    protected String diffGrossNegoDirection = "";
    protected Double diffGrossNegoAmount;
    protected String endofMonth = "";

    protected JDate maturityDate;
    protected String internal = "";
    protected Double amount2 = 0.0;
    protected String currency2 = "";
    protected String payReceiveAmt2 = "";
    protected String tomadoPrestado = "";

    protected String deliveryType = "";
    protected Long productID;
    protected String issuerName = "";
    protected String productCurrency = "";
    protected String buySell = "";
    protected String accountingRule = "";

    protected String nettingNumber = "";
    protected JDatetime sentDateTime;

    protected String claimProductType = "";
    protected String role = "";
    protected String issuerShortName = "";
    protected String issuerISOCode = "";
    protected String caReference = "";

    protected Double amount3 = 0.0;
    protected String currency3 = "";
    protected String payReceiveAmt3 = "";
    protected Double amount4 = 0.0;
    protected String currency4 = "";
    protected String payReceiveAmt4 = "";
    protected String revaluation = "";

    // PaymentsHub
    protected String paymentsHub = "";

    // CO2
    protected String underlyingSubType = "";
    protected String underlyingDeliveryType = "";

    // CA RF
    protected String caType = "";

    //JSon and Bond
    protected String aliasIdentifier = "";
    protected JDate forwardDate;
    protected String settlementPayReceive;
    protected Double settlementAmount;
    protected String settlementCurrency;
    protected JDate fixingDate;

    //CCCT. FX BOND MULTICCY
    protected String foTradeId = "";
    protected String partenonTradeId = "";
    protected Long linkedTradeId;
    protected Long mirrorTradeId;
    protected String mirrorBook = "";
    protected String trader = "";
    protected String intragrupoId = "";
    protected String roiId = "";
    protected String foPlatformId = "";
    protected String proposito = "";
    protected String downloadKey = "";
    protected JDate fx48HDate;
    protected String buyCcy;
    protected double buyAmount = 0.0;
    protected String sellCcy;
    protected double sellAmount = 0.0;
    protected String plCcy;
    protected double exchangeRate = 0.0;
    protected double forexrate = 0.0;
    protected String buyAgent = "";
    protected String buySettleMethod = "";
    protected String sellAgent = "";
    protected String sellSettleMethod = "";
    protected String feeDirection = "";
    protected double feeAmount = 0.0;
    protected String feeCcy = "";
    protected JDate feeStartDate;
    protected JDate feeEndDate;
    protected double ndfFixingRate = 0.0;
    protected String ndfFixingDirection = "";
    protected double ndfFixingAmount = 0.0;
    protected String ndfFixingCcy = "";
    protected JDate ndfFixingDate;
    protected String dcProducto = "";
    protected String dcSubproducto = "";

    // EQUITY MULTICCY
    protected String multiccy = "";

    protected Trade trade;
    protected Book book;
    protected Account account;
    protected CollateralConfig collateralConfig;
    protected BOTransfer clientBoTransfer;
    protected BOTransfer creBoTransfer;
    protected BOCre boCre;
    protected BOPosting boPosting;


    public SantBOCre() {
    }

    public SantBOCre(BOCre cre, Trade trade) {
        this.trade = trade;
        this.boCre = cre;
        init();
        build();
    }

    public void setBOPosting(BOPosting posting, Trade trade) {
        this.trade = trade;
        this.boPosting = posting;
        init(posting);
        build();
    }

    /**
     * @return @String Cre Message formated
     */
    public StringBuilder getCreLine() {
        StringBuilder builder = new StringBuilder();
        builder.append(getInstance().formatStringWithZeroOnLeft(this.creId, 16));
        builder.append(getInstance().formatStringWithBlankOnRight(this.eventType, 16));
        builder.append(getInstance().formatStringWithBlankOnRight(this.creDescription, 40));
        builder.append(getInstance().formatStringWithBlankOnRight(this.creType, 10));
        builder.append(getInstance().formatStringWithZeroOnLeft(this.tradeId, 16));
        builder.append(getInstance().formatStringWithZeroOnLeft(this.creLinkedId, 16));
        builder.append(getInstance().formatStringWithBlankOnRight(this.originalEvent, 30));
        builder.append(getInstance().formatDate(this.creationDate.getJDate(TimeZone.getDefault()), 10));
        builder.append(getInstance().formatDate(this.effectiveDate, 10));
        builder.append(getInstance().formatDate(this.bookingDate, 10));
        builder.append(getInstance().formatStringWithBlankOnRight(this.creStatus, 8));
        builder.append(getInstance().formatDate(this.settleDate, 10));
        builder.append(getInstance().formatDate(this.tradeDate, 10));
        builder.append(this.direction);
        builder.append(getInstance().formatAmountValue(this.amount1, 17, 2, "."));
        builder.append(getInstance().truncateString(this.currency1, 3));

        //InterestBearing
        builder.append(getInstance().formatStringWithBlankOnRight(this.fixedFloating, 8));
        builder.append(getInstance().formatStringWithBlankOnRight(this.indexDefinition, 14));
        builder.append(getInstance().formatUnsignedNumber(this.rate, 12, 4, "."));
        builder.append(getInstance().formatStringWithBlankOnRight(this.ratePositiveNegative, 8));

        builder.append(getInstance().formatStringWithBlankOnRight(this.identifierIntraEOD, 8));
        builder.append(getInstance().formatStringWithBlankOnRight(this.portfolio, 15));
        builder.append(getInstance().formatStringWithBlankOnRight(this.processingOrg, 13));
        builder.append(getInstance().formatStringWithBlankOnRight(this.counterparty, 13));
        builder.append(getInstance().formatStringWithBlankOnRight(this.ccpName, 4));

        //InterestBearing
        builder.append(getInstance().formatDate(this.startDate, 10));
        builder.append(getInstance().formatDate(this.expiryDate, 10));

        builder.append(getInstance().formatDate(this.cancelationDate, 10));
        builder.append(getInstance().formatStringWithBlankOnRight(this.productType, 22));
        builder.append(getInstance().formatStringWithBlankOnRight(this.productSubType, 14));
        builder.append(getInstance().formatStringWithBlankOnRight(this.settlementMethod, 14));
        builder.append(getInstance().formatStringWithZeroOnLeft(this.mcContractID, 16));
        builder.append(getInstance().formatStringWithBlankOnRight(this.mcContractType, 10));
        builder.append(getInstance().formatStringWithZeroOnLeft(this.accountID, 16));
        builder.append(getInstance().formatUnsignedNumber(this.accountBalance, 17, 2, "."));
        builder.append(getInstance().formatStringWithBlankOnRight(this.accountCreditDebit, 4));
        builder.append(getInstance().truncateString(this.accountCurrency, 3));
        builder.append(getInstance().formatStringWithBlankOnRight(this.transferAccount, 30));
        builder.append(getInstance().formatStringWithBlankOnRight(this.stm, 5));
        //NETTING
        builder.append(getInstance().formatStringWithBlankOnRight(this.nettingType, 30));
        builder.append(getInstance().formatStringWithZeroOnLeft(this.nettingParent, 9));

        //COT SECURITIES
        builder.append(getInstance().formatStringWithBlankOnRight(this.isin, 12));
        builder.append(getInstance().formatStringWithBlankOnRight(this.underlyingType, 2));
        builder.append(getInstance().formatUnsignedNumber(this.valorization, 17, 2, "."));
        builder.append(getInstance().formatStringWithBlankOnRight(this.portfolioStrategy, 30));

        //POSTING LIQ
        builder.append(getInstance().formatStringWithBlankOnRight(this.partenonId, 21));

        //CORPORATE ACTION
        builder.append(getInstance().formatStringWithBlankOnRight(this.ownIssuance, 2));
        builder.append(getInstance().formatStringWithBlankOnRight(this.bondType, 10));
        builder.append(getInstance().formatStringWithBlankOnRight(this.bedulaAttribute, 2));
        builder.append(getInstance().formatUnsignedNumberZero(this.retentionPercentage, 6, 2, "."));
        builder.append(getInstance().formatUnsignedNumberZero(this.negoPercentage, 6, 2, "."));
        builder.append(getInstance().formatUnsignedNumberZero(this.retentionAmount, 17, 2, "."));
        builder.append(getInstance().formatUnsignedNumberZero(this.netAmount, 17, 2, "."));
        builder.append(getInstance().formatUnsignedNumberZero(this.negoAmount, 17, 2, "."));
        String negoAmount = getInstance().formatUnsignedNumberZero(this.diffGrossNegoAmount, 17, 2, ".");
        negoAmount = getInstance().formatStringWithBlankOnRight(this.diffGrossNegoDirection, 1) + negoAmount; //total 17 char
        builder.append(negoAmount);
        if (BOCreUtils.getInstance().sendAgrego()) {
            builder.append(getInstance().formatStringWithBlankOnRight(this.endofMonth, 2));
        }

        //SecLending / PDV
        builder.append(getInstance().formatDate(this.maturityDate, 10));
        builder.append(getInstance().formatStringWithBlankOnRight(this.internal, 1));
        builder.append(getInstance().formatUnsignedNumberZero(this.amount2, 17, 2, "."));
        builder.append(getInstance().truncateString(this.currency2, 3));
        builder.append(getInstance().formatStringWithBlankOnRight(this.payReceiveAmt2, 3));
        builder.append(getInstance().formatStringWithBlankOnRight(this.tomadoPrestado, 1));

        //EQUITY
        builder.append(getInstance().formatStringWithBlankOnRight(this.deliveryType, 3));
        builder.append(getInstance().formatStringWithZeroOnLeft(this.productID, 16));
        builder.append(getInstance().formatStringWithBlankOnRight(this.issuerName, 13));
        builder.append(getInstance().truncateString(this.productCurrency, 3));
        builder.append(getInstance().formatStringWithBlankOnRight(this.buySell, 4));
        builder.append(getInstance().formatStringWithBlankOnRight(this.accountingRule, 22));

        //Repo
        builder.append(getInstance().formatStringWithBlankOnRight(this.nettingNumber, 2));
        builder.append(getInstance().formatDateTime(this.sentDateTime, 19));

        //CA Update
        builder.append(getInstance().formatStringWithBlankOnRight(this.claimProductType, 18));
        builder.append(getInstance().formatStringWithBlankOnRight(this.role, 15));
        builder.append(getInstance().formatStringWithBlankOnRight(this.issuerShortName, 15));
        builder.append(getInstance().formatStringWithBlankOnRight(this.issuerISOCode, 2));

        //Repo
        builder.append(getInstance().formatUnsignedNumberZero(this.amount3, 17, 2, "."));
        builder.append(getInstance().truncateString(this.currency3, 3));
        builder.append(getInstance().truncateString(this.payReceiveAmt3, 3));
        builder.append(getInstance().formatUnsignedNumberZero(this.amount4, 17, 2, "."));
        builder.append(getInstance().truncateString(this.currency4, 3));
        builder.append(getInstance().truncateString(this.payReceiveAmt4, 3));

        builder.append(getInstance().formatStringWithBlankOnRight(this.caReference, 16));

        //Repo
        if (BOCreUtils.getInstance().sendRevaluation()) {
            builder.append(getInstance().formatStringWithBlankOnRight(this.revaluation, 1));
        }

        // Payments Hub
        builder.append(getInstance().formatStringWithBlankOnRight(this.paymentsHub, 1));

        // Broker Partenon Id 
        builder.append(getInstance().formatStringWithBlankOnRight("", 21));

        //CO2
        builder.append(getInstance().formatStringWithBlankOnRight(this.underlyingSubType, 14));
        builder.append(getInstance().formatStringWithBlankOnRight(this.underlyingDeliveryType, 14));

        // CA RF
        builder.append(getInstance().formatStringWithBlankOnRight(this.caType,12));

        // EQUITY MULTICCY
        builder.append(getInstance().formatStringWithBlankOnRight(this.multiccy,1));

        return builder;
    }

    /**
     * @return @String Cre Message formated to JSon
     */
    public String getCreLineJSon() {
        try {
            JSONObject reportRowsBody = new JSONObject();

            reportRowsBody.put("aliasIdentifier", this.aliasIdentifier);
            reportRowsBody.put("productType", this.productType);
            reportRowsBody.put("subtype", getSubType());
            reportRowsBody.put("eventType", this.eventType);
            reportRowsBody.put("creId", this.creId);
            reportRowsBody.put("creDescription", this.creDescription);
            reportRowsBody.put("creType", this.creType);
            reportRowsBody.put("tradeId", this.tradeId);
            // Only when creType is REVERSAL
            if ("REVERSAL".equalsIgnoreCase(this.creType)) {
                reportRowsBody.put("creLinkedId", this.creLinkedId);
            }
            reportRowsBody.put("originalEvent", this.originalEvent);
            reportRowsBody.put("creationDate", formatJDTime(this.creationDate));
            reportRowsBody.put("effectiveDate", formatJDate(this.effectiveDate));
            reportRowsBody.put("bookingDate", formatJDate(this.bookingDate));
            reportRowsBody.put("settleDate", formatJDate(this.settleDate));
            reportRowsBody.put("tradeDate", formatJDate(this.tradeDate));

            //solo NOM_FWD
            reportRowsBody.put("forwardDate", formatJDate(this.forwardDate));

            //solo FWD_CASH_FIXING
            reportRowsBody.put("fixingDate", formatJDate(this.fixingDate));

            reportRowsBody.put("payReceive", this.direction);
            reportRowsBody.put("amount", new JSONAmount(formatAmount(Math.abs(this.amount1))));
            reportRowsBody.put("currency", this.currency1);

            //solo NOM_FWD
            reportRowsBody.put("settlementPayReceive", this.settlementPayReceive);
            if ("NOM_FWD".equalsIgnoreCase(this.eventType)) {
                reportRowsBody.put("settlementAmount", new JSONAmount(formatAmount(Math.abs(this.settlementAmount))));
            }
            reportRowsBody.put("settlementCurrency", this.settlementCurrency);

            reportRowsBody.put("identifierIntraEOD", this.identifierIntraEOD);
            reportRowsBody.put("portfolio", this.portfolio);

            //MTM_NET
            reportRowsBody.put("portfolioStrategy", this.portfolioStrategy);

            reportRowsBody.put("processingOrg", this.processingOrg);
            reportRowsBody.put("counterparty", this.counterparty);
            reportRowsBody.put("cancelationDate", this.cancelationDate);
            reportRowsBody.put("ISIN", this.isin);
            reportRowsBody.put("partenonID", this.partenonId);
            reportRowsBody.put("internal", this.internal);
            reportRowsBody.put("deliveryType", this.deliveryType);
            reportRowsBody.put("productId", this.productID);
            reportRowsBody.put("issuerName", this.issuerName);
            reportRowsBody.put("productCurrency", this.productCurrency);
            reportRowsBody.put("buySell", this.buySell);
            reportRowsBody.put("sentDateTime", formatJDTime(this.sentDateTime));

            return reportRowsBody.toString();

        } catch (Exception e) {
            Log.error(this, "Error:" + e);
        }
        return "";
    }

    public StringBuilder getCCCTLine(){
        StringBuilder builder = new StringBuilder();

        builder.append(getInstance().formatStringWithZeroOnLeft(this.creId, 15));
        builder.append(getInstance().formatStringWithBlankOnRight(this.eventType, 32));
        builder.append(getInstance().formatDate(this.effectiveDate, 10));
        builder.append(getInstance().formatStringWithZeroOnLeft(this.tradeId, 25));
        builder.append(getInstance().formatStringWithBlankOnRight(this.foTradeId, 15));
        builder.append(getInstance().formatStringWithBlankOnRight(this.partenonId, 15));
        builder.append(getInstance().formatStringWithZeroOnLeft(this.linkedTradeId, 25));
        builder.append(getInstance().formatStringWithZeroOnLeft(this.mirrorTradeId, 25));
        builder.append(getInstance().formatStringWithBlankOnLeft(this.counterparty, 11));
        builder.append(getInstance().formatStringWithBlankOnRight(this.productType, 30));
        builder.append(getInstance().formatDate(this.tradeDate, 10));
        builder.append(getInstance().formatStringWithBlankOnRight(this.portfolio, 15));
        builder.append(getInstance().formatStringWithBlankOnRight(this.mirrorBook, 15));
        builder.append(getInstance().formatStringWithBlankOnRight(this.trader, 8));
        builder.append(getInstance().formatStringWithBlankOnRight(this.intragrupoId, 50));
        builder.append(getInstance().formatStringWithBlankOnRight(this.roiId, 30));
        builder.append(getInstance().formatStringWithBlankOnRight(this.foPlatformId, 25));
        builder.append(getInstance().formatStringWithBlankOnRight(this.proposito, 3));
        builder.append(getInstance().formatStringWithBlankOnRight(this.downloadKey, 40));
        builder.append(getInstance().formatDate(this.fx48HDate, 10));
        builder.append(getInstance().formatDate(this.maturityDate, 10));
        builder.append(getInstance().formatStringWithBlankOnRight(this.buyCcy, 3));
        builder.append(getInstance().formatUnsignedNumberZero(this.buyAmount, 17, 2, ""));
        builder.append(getInstance().formatStringWithBlankOnRight(this.sellCcy, 3));
        builder.append(getInstance().formatUnsignedNumberZero(this.sellAmount, 17, 2, ""));
        builder.append(getInstance().formatStringWithBlankOnRight(this.plCcy, 3));
        builder.append(getInstance().formatUnsignedNumberZero(this.exchangeRate, 17, 11, ""));
        builder.append(getInstance().formatUnsignedNumberZero(this.forexrate, 17, 11, ""));
        builder.append(getInstance().formatStringWithBlankOnRight(this.buyAgent, 11));
        builder.append(getInstance().formatStringWithBlankOnRight(this.buySettleMethod, 3));
        builder.append(getInstance().formatStringWithBlankOnRight(this.sellAgent, 11));
        builder.append(getInstance().formatStringWithBlankOnRight(this.sellSettleMethod, 3));
        builder.append(getInstance().formatStringWithBlankOnRight(this.feeDirection, 1));
        builder.append(getInstance().formatUnsignedNumberZero(this.feeAmount, 17, 2, ""));
        builder.append(getInstance().formatStringWithBlankOnRight(this.feeCcy, 3));
        builder.append(getInstance().formatDate(this.feeStartDate, 10));
        builder.append(getInstance().formatDate(this.feeEndDate, 10));
        builder.append(getInstance().formatUnsignedNumberZero(this.ndfFixingRate, 17, 11, ""));
        builder.append(getInstance().formatStringWithBlankOnRight(this.ndfFixingDirection, 1));
        builder.append(getInstance().formatUnsignedNumberZero(this.ndfFixingAmount, 17, 2, ""));
        builder.append(getInstance().formatStringWithBlankOnRight(this.ndfFixingCcy, 3));
        builder.append(getInstance().formatDate(this.ndfFixingDate, 10));
        builder.append(getInstance().formatStringWithBlankOnRight(this.foTradeId, 15));
        builder.append(getInstance().formatStringWithBlankOnRight(this.dcProducto, 3));
        builder.append(getInstance().formatStringWithBlankOnRight(this.dcSubproducto, 3));
        return builder;
    }

    /**
     * Fill data need to generate Cre message.
     */
    public void build() {

        this.creId = loadCreId();
        this.eventType = loadCreEventType();
        this.creDescription = loadCreDescription();
        this.creType = loadCreType();
        this.tradeId = loadTradeId();
        this.creLinkedId = loadLinkedId();
        this.originalEvent = loadOriginalEventType();
        this.creationDate = loadCreationDate();
        this.effectiveDate = loadEffectiveDate();
        this.bookingDate = loadBookingDate();
        this.creStatus = loadStatus();
        this.settleDate = loadSettlemetDate();
        this.tradeDate = loadTradeDate();
        this.amount1 = loadCreAmount();
        this.currency1 = loadCurrency();
        this.identifier = "";
        this.identifierIntraEOD = loadIdentifierIntraEOD();
        this.identifierOBB = "";
        this.portfolio = loadBookName();
        this.direction = getDirection();
        this.settlementMethod = loadSettlementMethod();
        this.processingOrg = loadProccesingOrg();
        this.counterparty = loadCounterParty();
        this.productType = loadProductType();
        this.productSubType = getSubType();
        this.tradeCurrency = loadTradeCurrency();
        this.cancelationDate = getCancelationDate();
        this.mcContractID = loadContractID();
        this.mcContractType = loadContractType();
        this.stm = loadStm();

        final Double cashPosition = getPosition();
        this.accountBalance = cashPosition;
        if (null != cashPosition) {
            this.accountCreditDebit = getDebitCredit(cashPosition);
        }

        this.accountID = loadAccountID();
        this.accountCurrency = loadAccountCurrency();
        this.endofMonth = loadEndOfMonth();

        this.paymentsHub = loadPaymentsHub();

        // CO2
        this.underlyingSubType = loadUnderlyingSubType();
        this.underlyingDeliveryType = loadUnderlyingDeliveryType();

        // EQUITY MULTICCY
        this.multiccy = loadMulticcy();

        //Set additional values
        fillValues();
    }

    /**
     * Init WorkingData for fill cre message
     */
    protected void init() {
        this.book = BOCache.getBook(DSConnection.getDefault(), this.boCre.getBookId());
        this.clientBoTransfer = getClientBoTransfer();
        this.creBoTransfer = getCreBoTransfer();
        this.collateralConfig = getContract();
        this.account = getAccount();
    }


    /**
     * Init WorkingData for fill cre message
     */
    protected void init(BOPosting posting) {
        this.book = BOCache.getBook(DSConnection.getDefault(), this.boPosting.getBookId());
        this.clientBoTransfer = getClientBoTransfer();
        this.creBoTransfer = getPostingBoTransfer();
//        this.collateralConfig = getContract();
        this.account = getAccount();
    }

    /**
     * @return Cre Id
     */
    protected long loadCreId() {
        return this.boCre.getId();
    }

    /**
     * @return Cre Description
     */
    protected String loadCreDescription() {
        return this.boCre.getDescription();
    }

    /**
     * @return Cre Type
     */
    protected String loadCreType() {
        return this.boCre.getCreType();
    }

    /**
     * @return Linke dId
     */
    protected long loadLinkedId() {
        return this.boCre.getLinkedId();
    }

    /**
     * @return Original Event Type
     */
    protected String loadOriginalEventType() {
        return this.boCre.getOriginalEventType();
    }

    /**
     * @return Creation Date
     */
    protected JDatetime loadCreationDate() {
        return this.boCre.getCreationDate();
    }

    /**
     * @return Effective Date
     */
    protected JDate loadEffectiveDate() {
        return this.boCre.getEffectiveDate();
    }

    /**
     * @return Booking Date
     */
    protected JDate loadBookingDate() {
        return this.boCre.getBookingDate();
    }

    /**
     * @return Status
     */
    protected String loadStatus() {
        return this.boCre.getStatus();
    }

    /**
     * @return Product Type from trade
     */
    protected String loadProductType() {
        return null != this.trade ? this.trade.getProductType() : "";
    }

    /**
     * @return Account id from client BoTransfer
     */
    protected Integer loadAccountID() {
        return null != this.account ? this.account.getId() : 0;
    }

    /**
     * @return Currency Account
     */
    protected String loadAccountCurrency() {
        return null != this.account ? this.account.getCurrency() : "";
    }

    /**
     * Need set blank
     *
     * @return Settle Method from BoTrasnfer
     */
    protected String loadSettlementMethod() {
        return null != clientBoTransfer ? this.clientBoTransfer.getSettlementMethod() : "";
    }

    protected String loadCreEventType() {
        return null != this.boCre ? this.boCre.getEventType() : "";
    }

    /**
     * @return Currency from BoCre
     */
    protected String loadCurrency() {
        return this.boCre.getCurrency(0);
    }

    protected Double loadCreAmount() {
        return this.boCre.getAmount(0);
    }

    /**
     * @return Proccesing OR form trade
     */
    protected String loadProccesingOrg() {
        return null != trade ? this.trade.getBook().getLegalEntity().getExternalRef() : "";
    }

    protected Integer loadContractID() {
        return null != this.collateralConfig ? this.collateralConfig.getId() : 0;
    }

    protected String loadContractType() {
        return null != this.collateralConfig ? this.collateralConfig.getContractType() : "";
    }

    protected String loadStm() {
        return null != this.collateralConfig && "true".equalsIgnoreCase(this.collateralConfig.getAdditionalField("STM")) ? "true" : "";
    }

    /**
     * Need set blank
     *
     * @return
     */
    protected String loadIdentifierIntraEOD() {
        return "INTRADAY";
    }

    /**
     * @return CounterParty From trade
     */
    protected String loadCounterParty() {
        String cptyIdFromCre = this.boCre.getAttributeValue("CounterParty");
        if (!Util.isEmpty(cptyIdFromCre)) {
            int cptyId = 0;
            try {
                cptyId = Integer.parseInt(cptyIdFromCre);
                if (cptyId != 0) {
                    LegalEntity cpty = BOCache.getLegalEntity(DSConnection.getDefault(), cptyId);
                    if (null != cpty) {
                        return cpty.getExternalRef();
                    }
                }
            } catch (Exception e) {
                Log.error(this, "Counterparty Attribute from CRE " + this.boCre.getId() + " is not a valid Legal Entity");
            }
        }
        return null != trade ? this.trade.getCounterParty().getExternalRef() : "";
    }

    protected String getSubType() {
        if (null != this.trade) {
            return this.trade.getProductSubType();
        }
        return "";
    }

    protected String loadBookName() {
        return null != this.book ? this.book.getName() : "";
    }

    /**
     * @return SettlementDate from BOCre
     */
    protected JDate loadSettlemetDate() {
        return this.boCre.getSettlementDate();
    }

    /**
     * @return TradeDate from BoCre
     */
    protected JDate loadTradeDate() {
        return this.boCre.getTradeDate();
    }

    /**
     * @return Trade Long ID form BoCre
     */
    protected Long loadTradeId() {
        return this.boCre.getTradeLongId();
    }

    protected String loadEndOfMonth() {
        return "";
    }

    /**
     * @return BoTransfer from trade whit externalRole `Client`
     */
    protected BOTransfer getClientBoTransfer() {
        return getInstance().getClientTransfer(this.trade);
    }

    /**
     * @return BoTransfer from Cre
     */
    protected BOTransfer getCreBoTransfer() {
        return getInstance().getTransfer(this.boCre);
    }

    /**
     * @return BoTransfer from Posting
     */
    protected BOTransfer getPostingBoTransfer() {
        return getInstance().getTransfer(this.boPosting);
    }

    /**
     * @return Amount of the current cre
     */
    protected Double getCreAmount() {
        return this.amount1;
    }

    protected String getDebitCredit(double value) {
        return getInstance().getDebitCredit(value);
    }

    public Integer getContractId() {
        return null != this.collateralConfig ? this.collateralConfig.getId() : null;
    }

    protected String getDirection() {
        return getInstance().getDirection(this.boCre.getAmount(0));
    }

    protected String loadTradeCurrency() {
        return null != this.trade ? this.trade.getTradeCurrency() : "";
    }


    protected String loadPaymentsHub() {
        if (this.creBoTransfer != null) {
            boolean isPh = "true".equalsIgnoreCase(this.creBoTransfer.getAttribute("MsgSentbyPaymentHub")) ? true : false;
            if (!isPh) {
                return "N";
            } else {
                return "Y";
            }
        }
        return "N";
    }


    /**
     * Get position for CRE (cre amount + chas position for (contract + trade currency) + sum of send cres on same day)
     *
     * @return
     */
    protected abstract Double getPosition();

    /**
     * @return @JDate whit cancelation date if tradeStatus = CANCELED_TRADE_EVENT
     */
    protected abstract JDate getCancelationDate();

    protected abstract CollateralConfig getContract();

    protected abstract Account getAccount();

    /**
     * Fill additional values for BOCre accounting events
     */
    protected abstract void fillValues();

    public Long getCreId() {
        return this.creId;
    }

    public String getEventType() {
        return this.eventType;
    }

    public String getCreDescription() {
        return this.creDescription;
    }

    public String getCreType() {
        return this.creType;
    }

    public Long getTradeId() {
        return this.tradeId;
    }

    public Long getCreLinkedId() {
        return this.creLinkedId;
    }

    public String getOriginalEvent() {
        return this.originalEvent;
    }

    public JDatetime getCreationDate() {
        return this.creationDate;
    }

    public JDate getEffectiveDate() {
        return this.effectiveDate;
    }

    public JDate getBookingDate() {
        return this.bookingDate;
    }

    public String getCreStatus() {
        return this.creStatus;
    }

    public JDate getSettleDate() {
        return this.settleDate;
    }

    public JDate getTradeDate() {
        return this.tradeDate;
    }

    public Double getAmount1() {
        return this.amount1;
    }

    public String getCurrency1() {
        return this.currency1;
    }

    public String getFixedFloating() {
        return this.fixedFloating;
    }

    public String getIndexDefinition() {
        return this.indexDefinition;
    }

    public Double getRate() {
        return this.rate;
    }

    public String getRatePositiveNegative() {
        return this.ratePositiveNegative;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getIdentifierIntraEOD() {
        return this.identifierIntraEOD;
    }

    public String getIdentifierOBB() {
        return this.identifierOBB;
    }

    public String getPortfolio() {
        return this.portfolio;
    }

    public String getProcessingOrg() {
        return this.processingOrg;
    }

    public String getCounterparty() {
        return this.counterparty;
    }

    public String getProductType() {
        return this.productType;
    }

    public String getProductSubType() {
        return this.productSubType;
    }

    public Integer getMcContractID() {
        return this.mcContractID;
    }

    public String getMcContractType() {
        return this.mcContractType;
    }

    public Integer getAccountID() {
        return this.accountID;
    }

    public Double getAccountBalance() {
        return this.accountBalance;
    }

    public String getAccountCreditDebit() {
        return this.accountCreditDebit;
    }

    public String getAccountCurrency() {
        return this.accountCurrency;
    }

    public String getTradeCurrency() {
        return this.tradeCurrency;
    }

    public String getCcpName() {
        return this.ccpName;
    }

    public JDate getStartDate() {
        return this.startDate;
    }

    public JDate getExpiryDate() {
        return this.expiryDate;
    }

    public String getSettlementMethod() {
        return this.settlementMethod;
    }

    public Trade getTrade() {
        return this.trade;
    }

    public Book getBook() {
        return this.book;
    }

    public CollateralConfig getCollateralConfig() {
        return this.collateralConfig;
    }

    public BOCre getBoCre() {
        return this.boCre;
    }

    public String getTransferAccount() {
        return this.transferAccount;
    }

    public String getStm() {
        return this.stm;
    }

    public String getNettingType() {
        return nettingType;
    }

    public Long getNettingParent() {
        return nettingParent;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getUnderlyingType() {
        return underlyingType;
    }

    public void setUnderlyingType(String underlyingType) {
        this.underlyingType = underlyingType;
    }

    public Double getValorization() {
        return valorization;
    }

    public void setValorization(Double valorization) {
        this.valorization = valorization;
    }

    public String getPortfolioStrategy() {
        return portfolioStrategy;
    }

    public void setPortfolioStrategy(String portfolioStrategy) {
        this.portfolioStrategy = portfolioStrategy;
    }

    public void setOriginalEvent(String originalEvent) {
        this.originalEvent = originalEvent;
    }

    public String getPartenonId() {
        return partenonId;
    }

    public void setPartenonId(String partenonId) {
        this.partenonId = partenonId;
    }

    protected String loadCounterPartyCADividend(String role) {
        if (this.creType.equals("REVERSAL")) {
            String counterParty = this.boCre.getAttributeValue("CounterParty");
            if (!Util.isEmpty(counterParty)) {
                int cptyId = 0;
                cptyId = Integer.parseInt(counterParty);
                if (cptyId != 0) {
                    LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), cptyId);
                    if (null != le) {
                        if ("CounterParty".equals(role)) {
                            return null != trade ? le.getExternalRef() : "";
                        } else if ("Agent".equals(role)) {
                            return null != trade ? le.getCode() : "";
                        }
                        return "";
                    }
                }
            }
        }
        //if Counterparty cre attribute is not filled
        if ("CounterParty".equals(role)) {
            return null != trade ? this.trade.getCounterParty().getExternalRef() : "";
        } else if ("Agent".equals(role)) {
            return null != trade ? this.trade.getCounterParty().getCode() : "";
        }
        return "";
    }

    /**
     * @param value
     * @return
     */
    public String formatAmount(Double value) {
        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols();
        decimalSymbol.setDecimalSeparator('.');
        final DecimalFormat df = new DecimalFormat("#0.00", decimalSymbol);
        return df.format(value);
    }

    private String formatJDate(JDate jDate) {
        if (Optional.ofNullable(jDate).isPresent()) {
            JDateFormat jDateFormat = new JDateFormat(BOCreUtils.DATE_FORMAT);
            return jDateFormat.format(jDate);
        }
        return null;
    }

    private String formatJDTime(Object jDatetime) {
        if (Optional.ofNullable(jDatetime).isPresent()) {
            JDatetimeFormat jDateFormat = new JDatetimeFormat(BOCreUtils.DATE_FORMAT);
            return jDateFormat.format(jDatetime);
        }
        return null;
    }

    /**
     * @param t
     * @return
     */
    private Double getAmount(Object t) {
        return Optional.ofNullable(t).filter(Amount.class::isInstance).map(Amount.class::cast).map(Amount::get).orElse(0.0);
    }


    protected String loadUnderlyingSubType() {
        return "";
    }


    protected String loadUnderlyingDeliveryType() {
        return "";
    }


    protected String loadMulticcy() {
        return "N";
    }


}
