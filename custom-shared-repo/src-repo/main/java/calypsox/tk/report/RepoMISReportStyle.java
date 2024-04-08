package calypsox.tk.report;

import calypsox.tk.confirmation.builder.repo.RepoReportUtil;
import calypsox.tk.core.SantanderUtil;
import calypsox.tk.report.util.UtilReport;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.bo.swift.MT527Helper;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.product.flow.CashFlowPrincipal;
import com.calypso.tk.product.flow.flowDefinition.impl.FdnCashFlowCouponDefinitionImpl;
import com.calypso.tk.product.flow.flowDefinition.impl.FdnCashFlowOptionCouponDefinitionImpl;
import com.calypso.tk.product.util.CollateralBasedUtil;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.SecFinanceReportStyle;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.fieldentry.FieldEntry;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static calypsox.tk.report.RepoInformesReport.FATHER_TRIPARTY_REPO;
import static calypsox.tk.report.RepoMISReportTemplate.PLEDGE_NOMINAL;
import static calypsox.tk.report.RepoMISReportTemplate.PLEDGE_PRINCIPAL;

public class RepoMISReportStyle extends TradeReportStyle {

    public static final String LOG_CATEGORY = "RepoMISReportStyle";

    private static final List<String> contactTypes = Arrays.asList("Default", "ALL");

    public static final String ORIGIN = "ORIGIN";
    public static final String PROCESSDATE = "PROCESSDATE";
    public static final String BLOCK_ID = "BLOCK_ID";
    /*public static final String ENTITY="ENTITY";*/
    public static final String ACCOUNTING_CENTER = "ACCOUNTING_CENTER";/*
	public static final String BRANCH="BRANCH";
	public static final String BRANCH_ID="BRANCH_ID";
	public static final String DEAL_ID="DEAL_ID";*/
    public static final String INTERNALREF = "INTERNALREF";
    public static final String ISIN = "ISIN";
    public static final String ISINDESC = "ISINDESC";
    public static final String STATUS = "STATUS";
    public static final String STATUS_CALC = "STATUS_CALC";
    public static final String FODEALFO = "FODEALFO";
    public static final String CONTRACTORIGIN = "CONTRACTORIGIN";
    public static final String SOURCESYSTEM = "SOURCESYSTEM";
    public static final String INSTRUMENT_ID = "INSTRUMENT_ID";
    public static final String INSTRUMENT = "INSTRUMENT";
    public static final String INSTRTYPE = "INSTRTYPE";
    public static final String DIRECTION = "DIRECTION";
    /*	public static final String COUNTERPARTY_ID="COUNTERPARTY_ID";
        public static final String GLSCOUNTERPARTY="GLSCOUNTERPARTY";
        public static final String COUNTERPARTYDESC="COUNTERPARTYDESC";*/
    public static final String ALIASCOUNTERPARTY = "ALIASCOUNTERPARTY";/*
	public static final String NIFCOUNTERPARTY="NIFCOUNTERPARTY";
	public static final String COUNTERPARTYSECTOR="COUNTERPARTYSECTOR";*/
    public static final String COUNTERPARTYCOUNTRY="COUNTERPARTYCOUNTRY";
    public static final String CSB = "CSB";
    public static final String OFFICIAL = "OFFICIAL";
    public static final String RFR = "RFR";
    /*public static final String INPUTDATE="INPUTDATE";
    public static final String TRADEDATE="TRADEDATE";
    */
    public static final String MATURITYDATE="MATURITYDATE";
    public static final String MATURITYTYPE="Maturity Type";
    public static final String VALUEDATE="VALUEDATE";
    public static final String DEADDATE = "DEADDATE";
    public static final String STRATEGY_ID="STRATEGY_ID";
    public static final String STRATEGYHEDGE="STRATEGYHEDGE";
    public static final String PORTFOLIO = "PORTFOLIO";
    public static final String FOLDER = "FOLDER";
    public static final String YIELD = "YIELD";
    public static final String CURRENCY = "CURRENCY";
    public static final String CURRENCY_BASE = "CURRENCY_BASE";
    public static final String SETTLECURR="SETTLECURR";
    public static final String PRINCIPAL = "PRINCIPAL";
    public static final String PRINCIPALCUR = "PRINCIPALCUR";
    public static final String NOMINAL = "NOMINAL";
    public static final String UNITNOMINAL = "UNITNOMINAL";
    public static final String LOTSIZE = "LOTSIZE";
    public static final String PRICEATSTART = "PRICEATSTART";
    public static final String RATE = "RATE";
    //public static final String BROKERCODE="BROKERCODE";
    public static final String BROKERDESC = "BROKERDESC";
    public static final String OBSERVATIONSTRADER = "OBSERVATIONSTRADER";
    public static final String PARTENONID = "PARTENONID";
    public static final String INTERESTTYPE = "INTERESTTYPE";
    public static final String REFINDEX = "REFINDEX";
    public static final String BOINDICATOR = "BOINDICATOR";
    public static final String SPREAD = "SPREAD";
    public static final String ISSUERGLS = "ISSUERGLS";
    public static final String ISSUERCODE = "ISSUERCODE";
    public static final String ISSUERDESC = "ISSUERDESC";
    public static final String ISSUERID = "ISSUERID";
    public static final String ISSUERSECTOR = "ISSUERSECTOR";
    public static final String ISSUERCOUNTRY = "ISSUERCOUNTRY";
    public static final String ISSUERNAME = "ISSUERNAME";
    public static final String INPUTDATEISSUER = "INPUTDATEISSUER";
    public static final String MATURITYDATEISSUER = "MATURITYDATEISSUER";
    public static final String FWDINDICATOR = "FWDINDICATOR";
    public static final String INFLACTIONINDICATOR = "INFLACTIONINDICATOR";
    public static final String INFLACTIONFACTOR = "INFLACTIONFACTOR";
    public static final String VALUEINDICATOR = "VALUEINDICATOR";
    public static final String TRIPARTYINDICATOR = "TRIPARTYINDICATOR";
    public static final String CALCBASIS = "CALCBASIS";
    public static final String GUARANTEETYPE = "GUARANTEETYPE";
    public static final String YOURCUSTODIANCODE = "YOURCUSTODIANCODE";
    public static final String YOURCUSTODIANACCOUNT = "YOURCUSTODIANACCOUNT";
    public static final String YOURCUSTODIANSWIFT = "YOURCUSTODIANSWIFT";
    public static final String YOURCUSTODIANGLS = "YOURCUSTODIANGLS";
    public static final String YOURCUSTODIANDESC = "YOURCUSTODIANDESC";
    public static final String YOURCUSTODIANCOUNTRY = "YOURCUSTODIANCOUNTRY";
    public static final String OURCUSTODIANCODE = "OURCUSTODIANCODE";
    public static final String OURCUSTODIANACCOUNT = "OURCUSTODIANACCOUNT";
    public static final String OURCUSTODIANSWIFT = "OURCUSTODIANSWIFT";
    public static final String OURCUSTODIANGLS = "OURCUSTODIANGLS";
    public static final String OURCUSTODIANDESC = "OURCUSTODIANDESC";
    public static final String OURCUSTODIANCOUNTRY = "OURCUSTODIANCOUNTRY";
    public static final String NEGREPOINDICATOR = "NEGREPOINDICATOR";
    public static final String MARTAINDICATOR = "MARTAINDICATOR";
    public static final String HAIRCUTORI = "HAIRCUTORI";
    public static final String CODEFORMULAHARICUT = "CODEFORMULAHARICUT";
    public static final String FORMULAHARICUT = "FORMULAHARICUT";
    public static final String SETTLEMENTCAMARA = "SETTLEMENTCAMARA";
    public static final String SETTLEMENTCAMARATYPE = "SETTLEMENTCAMARATYPE";
    public static final String SETTLEMENTTYPE = "SETTLEMENTTYPE";
    public static final String SEGREGATIONINDICATOR = "SEGREGATIONINDICATOR";
    public static final String COUPONTYPE = "COUPONTYPE";
    public static final String TOTALINTEREST = "TOTALINTEREST";
    public static final String TYPEPAPER = "TYPEPAPER";
    public static final String ISSUEPROSPECTUSES = "ISSUEPROSPECTUSES";
    public static final String INPUTDATEFLOWCURR = "INPUTDATEFLOWCURR";
    public static final String ENDDATEFLOWCURR = "ENDDATEFLOWCURR";
    public static final String PERSONTYPE = "PERSONTYPE";
    public static final String INITIALMARGININDICATOR = "INITIALMARGININDICATOR";
    public static final String PRODUCT = "PRODUCT";
    public static final String LEGALENTITY = "LEGALENTITY";
    public static final String SYSTEM_BO = "SYSTEM_BO";
    public static final String FIXING = "FIXING";
    public static final String IMGROSSAMOUNT = "IMGROSSAMOUNT";
    public static final String IMFORWARDAMOUNT = "IMFORWARDAMOUNT";
    public static final String PRICE = "PRICE";
    public static final String YESTERDAYPRICE = "YESTERDAYPRICE";
    public static final String REVALAMOUNT = "REVALAMOUNT";
    public static final String ACCRUEDREVALAMOUNT = "ACCRUEDREVALAMOUNT";
    public static final String PROVISION = "PROVISION";
    public static final String RECUPERATION = "RECUPERATION";
    public static final String HEDGINGTYPE = "HEDGINGTYPE";
    public static final String AVGPRINCIPAL = "AVGPRINCIPAL";
    public static final String NEXTINTERESTSETTLEMENTDATE = "NEXTINTERESTSETTLEMENTDATE";
    public static final String INTERESTACCRUAL = "INTERESTACCRUAL";
    public static final String INTERESTFREQUENCYFIXATION = "INTERESTFREQUENCYFIXATION";
    public static final String INTERESTSETTLEMENTFREQUENCY = "INTERESTSETTLEMENTFREQUENCY";
    public static final String MARKETVALUE = "MARKETVALUE";
    public static final String MARKETVALUEMAN = "MARKETVALUEMAN";
    public static final String MARKETVALUEACC = "MARKETVALUEACC";
    public static final String NEXTFIXINGDATE = "NEXTFIXINGDATE";
    public static final String FREQUENCECAPITALINT = "FREQUENCECAPITALINT";
    public static final String BASEBOND = "BASEBOND";
    public static final String DIRTYPRICE = "DIRTYPRICE";
    public static final String CLEANPRICE = "CLEANPRICE";
    public static final String TRANSFERINTRATE = "TRANSFERINTRATE";
    public static final String SETTLEINTTYPE = "SETTLEINTTYPE";
    public static final String ISSUERNIF = "ISSUERNIF";
    public static final String CASH = "CASH";
    public static final String HAIRCUT = "HAIRCUT";
    public static final String COMMISIONSTART = "COMMISIONSTART";
    public static final String COMMISIONEND = "COMMISIONEND";
    public static final String TITLES = "TITLES";
    public static final String IDISSUEFINAN = "IDISSUEFINAN";
    public static final String PREMIUMINTTRANSFTYPE = "PREMIUMINTTRANSFTYPE";
    public static final String PERFINANCOST = "PERFINANCOST";
    public static final String DAYSTOMAT = "DAYSTOMAT";
    public static final String CDMETLIQ = "CDMETLIQ";
    public static final String NUDIASPL = "NUDIASPL";
    public static final String ECONOMICGROUP = "ECONOMICGROUP";
    public static final String STRUCTURALSEGMENT = "STRUCTURALSEGMENT";
    public static final String CONTRACTCHANNEL = "CONTRACTCHANNEL";
    public static final String COUPONCURRENTTERM = "COUPONCURRENTTERM";
    public static final String LEG_COMPOUNDING_FREQUENCY = "LEG_COMPOUNDING_FREQUENCY";
    public static final String LEG_INCOME_PAYMENT_TYPE = "LEG_INCOME_PAYMENT_TYPE";
    public static final String OBSERVATIONSEVENT = "OBSERVATIONSEVENT";
    public static final String SETTLEMENTFLOW = "SETTLEMENTFLOW";
    public static final String CRTYPE = "CRTYPE";
    public static final String COUPONBASECALC = "COUPONBASECALC";
    public static final String VALPRLIM = "VALPRLIM";
    public static final String VALPRSUC = "VALPRSUC";
    public static final String FECINIFL = "FECINIFL";
    public static final String FECFINFL = "FECFINFL";
    public static final String INTERESTCASH = "INTERESTCASH";
    public static final String NPV = "NPV";

    public static final String FILLER1 = "FILLER1";
    public static final String FILLER2 = "FILLER2";
    public static final String FILLER3 = "FILLER3";
    public static final String FILLER4 = "FILLER4";
    public static final String FILLER5 = "FILLER5";

    public static final String TRIPARTYREPOINDICATOR = "TRIPARTYREPOINDICATOR";
    public static final String TRIPARTYREPOTYPE = "TRIPARTYREPOTYPE";
    public static final String TRIPARTYREPOID = "TRIPARTYREPOID";

    public static final String TRIPARTYAGENT = "TRIPARTYAGENT";
    public static final String TRIPARTYAGENTLONG = "TRIPARTYAGENTLONG";
    public static final String MASTERAGREEMENT = "MASTERAGREEMENT";






    public static final ArrayList<String> emptyColumns = new ArrayList<String>();
    public static final HashMap<String, String> columnToKeyword = new HashMap<String, String>();
    public static final HashMap<String, String> columnToColumn = new HashMap<String, String>();
    public static final HashMap<String, String> valueToValue = new HashMap<String, String>();

    static {
        emptyColumns.add(CSB);
        emptyColumns.add(BLOCK_ID);
        emptyColumns.add(ALIASCOUNTERPARTY);
        emptyColumns.add(INTERNALREF);
        emptyColumns.add(YIELD);
        emptyColumns.add(FWDINDICATOR);
        emptyColumns.add(VALUEINDICATOR);
        emptyColumns.add(NEGREPOINDICATOR);
        emptyColumns.add(MARTAINDICATOR);
        emptyColumns.add(HAIRCUTORI);
        emptyColumns.add(CODEFORMULAHARICUT);
        emptyColumns.add(FORMULAHARICUT);
        emptyColumns.add(SETTLEMENTCAMARA);
        emptyColumns.add(SETTLEMENTCAMARATYPE);
        emptyColumns.add(INITIALMARGININDICATOR);
        emptyColumns.add(PROVISION);
        emptyColumns.add(RECUPERATION);
        emptyColumns.add(ISSUEPROSPECTUSES);
        emptyColumns.add(SEGREGATIONINDICATOR);
        emptyColumns.add(BASEBOND);
        emptyColumns.add(FILLER2);
        emptyColumns.add(FILLER3);
        emptyColumns.add(FILLER4);
        emptyColumns.add(FILLER5);

        columnToKeyword.put(CONTRACTORIGIN, "MurexRootContract");
        columnToKeyword.put(FODEALFO, "MurexTradeID");
        columnToKeyword.put(CONTRACTCHANNEL, "Mx Origin");
        columnToKeyword.put(REFINDEX, "MX_REFINDEX");

        columnToColumn.put(PORTFOLIO, TradeReportStyle.BOOK);
        columnToColumn.put(FOLDER, TradeReportStyle.BOOK);

        valueToValue.put("Negociacion", "NE");
        valueToValue.put("Inversion crediticia", "IC");
        valueToValue.put("Inversion a vencimiento", "IV");
        valueToValue.put("Disponible para la venta", "DV");
    }

    public static final String SEC_EQ_PRICE_INITIAL = "Sec. Price (Initial)";

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

        Trade trade = row.getProperty(ReportRow.TRADE);
        BOTransfer transfer = row.getProperty(ReportRow.TRANSFER);
        PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
        Product product = trade.getProduct();
        boolean isRepo = false;
        boolean isPledge = false;
        Product security = null;
        Repo repo = null;
        Repo repoPledgeFather = null;
        Pledge pledge = null;
        Cash cash = null;
        Trade fatherRepoPledge = null;
        if(product instanceof Repo){
            isRepo = true;
            repo = ((Repo) product);
            security = repo.getSecurity();
            cash = repo.getCash();
        }else if(product instanceof Pledge){
            isPledge = true;
            pledge = ((Pledge) product);
            security = pledge.getSecurity();
            fatherRepoPledge = (Trade) row.getProperty(FATHER_TRIPARTY_REPO);
            if(Optional.ofNullable(fatherRepoPledge).isPresent() && fatherRepoPledge.getProduct() instanceof Repo){
                repoPledgeFather = (Repo)fatherRepoPledge.getProduct();
            }
        }


        if (emptyColumns.contains(columnName))
            return "";
        if (columnToKeyword.containsKey(columnName)) {
            return getKeywordValue(row, columnToKeyword.get(columnName), errors);
        }
        if (columnToColumn.containsKey(columnName)) {
            return super.getColumnValue(row, columnToColumn.get(columnName), errors);
        }


        if (null!=security) {
            // Repo repo = (Repo) product;
            if (columnName.equals(PRODUCT)) {
                String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
                if (partenonAccountingID != null && partenonAccountingID.length() >= 21) {
                    String productType = partenonAccountingID.substring(8, 11);
                    String productSubType = partenonAccountingID.substring(18, 21);
                    partenonAccountingID = productType + "-" + productSubType;
                    return partenonAccountingID;
                }
                return null;
            }
            if (columnName.equals(ACCOUNTING_CENTER)) {
                String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
                if (partenonAccountingID != null && partenonAccountingID.length() >= 8) {
                    return partenonAccountingID.substring(4, 8);
                }
                return null;
            }

            if (columnName.equals(SOURCESYSTEM)) {
                if(null!=security){
                    if ( security instanceof Bond)
                        return "MUREX FXFI";
                    if (security instanceof Equity)
                        return "MUREX EQ";
                }
                return "";
            }

            if (columnName.equals(ORIGIN)) {
                return "800018693";
            }

//            if(columnName.equals(REFINDEX)) {
//				return getRefIndex(repo);
//			}

            if (columnName.equals(CURRENCY)) {
                if(isPledge){
                    if(row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)){
                        return security.getCurrency();
                    }
                    final String currency = null != fatherRepoPledge && fatherRepoPledge.getProduct() instanceof Repo ? ((Repo) fatherRepoPledge.getProduct()).getSecurity().getCurrency() : "";
                    return currency;
                }else if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                    return security.getCurrency();
                }
                else {
                    return cash!=null ? cash.getCurrency() : "";
                }
            }
            if (columnName.equals(CURRENCY_BASE)) {
                return isRepo && null!=cash ? cash.getCurrency() : "";
            }
            if (columnName.equals(VALUEDATE)) {
                return isRepo ? repo.getStartDate() : isPledge ? pledge.getStartDate() : null;
            }
            if (columnName.equals(STRATEGY_ID)) {
                Book book = trade.getBook();
                return valueToValue.get(book.getAccountingBook().getName());
            }
            if (columnName.equals(STRATEGYHEDGE)) {
                Book book = trade.getBook();
                return book.getAccountingBook().getName();
            }
            if (columnName.equals(PROCESSDATE)) {
                return valDate;
            }
            if (columnName.equals(PERFINANCOST)) {
                return ""; // always empty
            }
            if (columnName.equals(PREMIUMINTTRANSFTYPE)) {
                return ""; // always empty
            }
            if (columnName.equals(STATUS)) {
                if(isRepo){
                    if (repo.getEndDate() != null && repo.getEndDate().lte(valDate)) {
                        return "VENCIDO";
                    }
                }else if(isPledge){
                    if (pledge.getEndDate() != null && pledge.getEndDate().lte(valDate)) {
                        return "VENCIDO";
                    }
                }
                if (trade.getStatus().equals(Status.S_CANCELED)){
                    return "CANCELADO";
                }
                else {
                    return "VIVO";
                }
            }
            if (columnName.equals(STATUS_CALC)) { //TODO pendiente que poner
                if(isRepo){
                    if (valDate.before(repo.getStartDate())) {
                        return "COMPROMISO";
                    }
                }else if(isPledge){
                    if (valDate.before(pledge.getStartDate())) {
                        return "COMPROMISO";
                    }
                }
                return "VALOR";
            }
            if (columnName.equals(DAYSTOMAT)) {
                int diffDays = 0;
                if(isRepo){
                    diffDays = (int) Math.abs(JDate.diff(trade.getTradeDate().getJDate(TimeZone.getDefault()), repo.getStartDate()));
                    if (diffDays >= 6) {
                        return "1";
                    }
                }
                return "0";
            }
            if (columnName.equals(CDMETLIQ)) {
                if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                    return "";
                }

                Vector<TradeTransferRule> trs = trade.getTransferRules();
                if (trs == null) {
                    Vector errorMsgs = new Vector<String>();
                    trs = BOProductHandler.buildTransferRules(trade, errorMsgs, DSConnection.getDefault(), trade.isArchived());
                }

                if (trs == null) {
                    return "";
                }
                for (TradeTransferRule tr : trs) {
                    if (tr.getTransferType().equals(SantanderUtil.TRANSFER_TYPE_PRINCIPAL) && tr.getSettlementMethod().equals("Direct")) {
                        return "CUENTA";
                    }
                }
                return "";
            }
            if (columnName.equals(NUDIASPL)) {
                if(isRepo){
                    return (int) Math.abs(JDate.diff(trade.getTradeDate().getJDate(TimeZone.getDefault()), repo.getStartDate()));
                }
                return 0;
            }
            if (columnName.equals(ECONOMICGROUP)) {
                return ""; // always empty
            }
            if (columnName.equals(STRUCTURALSEGMENT)) {
                return ""; // always empty
            }
            if (columnName.equals(COUNTERPARTYCOUNTRY)) {
                return getCountryIso(trade.getCounterParty().getCountry());
            }
            if (columnName.equals(COUPONCURRENTTERM)) {
                if(isRepo){
                    if (cash.getRateIndex() != null) {
                        return cash.getRateIndex().getTenor();
                    }
                }else if(isPledge && isFloatingFatherRepo(repoPledgeFather)){
                    if (repoPledgeFather.getCash().getRateIndex() != null) {
                        return repoPledgeFather.getCash().getRateIndex().getTenor();
                    }
                }
                return "";
            }
            if (columnName.equals(LEG_COMPOUNDING_FREQUENCY)) {
                if(isRepo){
                    Frequency cmpFreq = null!=cash ? cash.getCompoundFrequency() : null;
                    if (cmpFreq != null && !cmpFreq.equals(Frequency.F_NONE)) {
                        return cash.getCompoundFrequency();
                    }
                }else if(isPledge && isFloatingFatherRepo(repoPledgeFather)){
                    final Cash repoCash = repoPledgeFather.getCash();
                    Frequency cmpFreq = null!=repoCash ? repoCash.getCompoundFrequency() : null;
                    if (cmpFreq != null && !cmpFreq.equals(Frequency.F_NONE)) {
                        return repoCash.getCompoundFrequency();
                    }
                }
                return "";
            }
            if (columnName.equals(LEG_INCOME_PAYMENT_TYPE)) {
                if(isRepo){
                    if (null!=cash && !cash.getFixedRateB() && cash.getResetTiming().equals(PeriodTiming.S_BEGIN_PERIOD)) {
                        return "POST";
                    }
                    return "PRE";
                }else if(isPledge && isFloatingFatherRepo(repoPledgeFather)){
                    final Cash repoCash = repoPledgeFather.getCash();
                    if (null!=repoCash && !repoCash.getFixedRateB() && repoCash.getResetTiming().equals(PeriodTiming.S_BEGIN_PERIOD)) {
                        return "POST";
                    }
                    return "PRE";
                }
                return "";
            }
            if (columnName.equals(OBSERVATIONSEVENT)) {
                return null!=cash ? cash.getPaymentFrequency() : "";
            }
            if (columnName.equals(SETTLEMENTFLOW)) {
                return "NO LIQUIDADO";
            }
            if (columnName.equals(CRTYPE)) {
                String kwValue = trade.getKeywordValue("MxLastEvent");
                if (!Util.isEmpty(kwValue) && kwValue.contains("Reissue")) {
                    return "CANCEL&REISSUE";
                }
                return "";
            }
            if (columnName.equals(COUPONBASECALC)) { //TODO Duda
                if(isRepo){
                    return repo.getDayCount();
                }else if(isPledge){
                    return null!=fatherRepoPledge ? ((Repo)fatherRepoPledge.getProduct()).getDayCount() : "";
                }
            }
            if (columnName.equals(FECINIFL)) {
                if(isRepo){
                    CashFlow cf = getCurrentCashFlow(repo, valDate);
                    if (cf != null) {
                        return cf.getStartDate();
                    }
                }
                return "";
            }
            if (columnName.equals(FECFINFL)) {
                if(isRepo){
                    CashFlow cf = getCurrentCashFlow(repo, valDate);
                    if (cf != null) {
                        return cf.getEndDate();
                    }
                }
                return "";
            }
            if (columnName.equals(INTERESTCASH)) {
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                        return "";
                    }
                    double spread = 0.0;

                    if (!repo.isFixedRate()) {
                        spread = repo.getCash().getSpread();
                    }
                    CashFlow cf = getCurrentCashFlow(repo, valDate);
                    if (cf == null) {
                        return "";
                    }
                    if (cf.getType().equals(CashFlow.INTEREST)) {
                        CashFlowInterest cfi = (CashFlowInterest)cf;
                        double rate = cfi.getRate();

                        return formatResult(cfi.calculateAmount(rate + spread, valDate));
                    }
                }
                return "";
            }
            if (columnName.equals(NPV)) {
                return "";
            }

            if (columnName.equals(DIRECTION)) {
                return mapDirection(trade);
            }
            if (columnName.equals(PRINCIPALCUR)) {
                return "";
            }
            if (columnName.equals(PRINCIPAL)) {
                if(isRepo){
                    if (repo.getEndDate() != null && valDate.after(repo.getEndDate())) {
                        return "0";
                    }
                    if (repo.getSecurity() instanceof Bond) {
                        if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                            Amount amount = (Amount) super.getColumnValue(row, "Sec. Nominal", errors);
                            if(amount != null){
                                return formatResult(amount.get());
                            }
                        }

                        Bond bond = (Bond) repo.getSecurity();
                        if (isAmortizationSinking(bond.getPrincipalStructure())) {
                            CashFlowSet cashFlows = bond.getFlows();
                            JDate datecheked = null;
                            double amount = 0.0;
                            if(null!=cashFlows){
                                for (int i = 0; i < cashFlows.size(); i++) {
                                    CashFlow cf = cashFlows.get(i);
                                    if(checkValidation(i, datecheked, cf.getStartDate())) {
                                        double poolFactor = 0;
                                        if (cashFlows.get(i).getCashFlowDefinition() instanceof FdnCashFlowCouponDefinitionImpl){
                                            poolFactor = ((FdnCashFlowCouponDefinitionImpl)cashFlows.get(i).getCashFlowDefinition()).getPoolFactor();
                                        } else if (cashFlows.get(i).getCashFlowDefinition() instanceof FdnCashFlowOptionCouponDefinitionImpl){
                                            poolFactor = ((FdnCashFlowOptionCouponDefinitionImpl)cashFlows.get(i).getCashFlowDefinition()).getPoolFactor();
                                        }
                                        if (0 != poolFactor) {
                                            amount = trade.computeNominal() * poolFactor;
                                            datecheked = cf.getStartDate();
                                        }
                                    }
                                }
                            }
                            return formatResult(amount);
                        } else if(getInflationIndicator(repo)){
                            String capitalFactor = trade.getKeywordValue("CapitalFactor") ;
                            return formatResult(trade.computeNominal(valDate) * Double.parseDouble(capitalFactor));
                        } else
                            return formatResult(trade.computeNominal(valDate));
                    }
                }else if(isPledge){
                    return formatResult(row.getProperty(PLEDGE_PRINCIPAL));
                }
                return 0.0;
            }
            if (columnName.equals(NOMINAL)) {
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                        Amount amount = (Amount) super.getColumnValue(row, "Sec. Nominal", errors);
                        if(amount != null){
                            return formatResult(Math.abs(amount.get()));
                        }
                    }

                    double fxRate = 1.0;
                    fxRate = repo.getFirstCollateral().getFxRate(repo.getCurrency(), valDate);
                    if (repo.getEndDate() != null && valDate.after(repo.getEndDate())) {
                        return "0";
                    }
                    return formatResult(Math.abs(repo.getRemainingQuantity(valDate) * repo.getSecurity().getPrincipal(valDate)) * fxRate);
                }else if(isPledge){
                    return formatResult(row.getProperty(PLEDGE_NOMINAL));
                }

            }
            if (columnName.equals(PRICEATSTART)) {
                return super.getColumnValue(row, SEC_EQ_PRICE_INITIAL, errors);
            }
            if (columnName.equals(RATE)) {
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                        return "";
                    }
                    return generateRate(repo,valDateTime,valDate);
                }else if(isPledge){
                    final String financialFatherRepo = getFinancialFatherRepo(fatherRepoPledge);
                    if (financialFatherRepo.equalsIgnoreCase(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                        return "";
                    }
                    if(null!=fatherRepoPledge){
                        return generateRate((Repo)fatherRepoPledge.getProduct(),valDateTime,valDate);
                    }
                }
                return "";

            }
            if (columnName.equals(BROKERDESC)) {
                Vector<LegalEntity> brokers = TradeRoleFinder.getTradeRoles("Broker", trade, new Vector(), DSConnection.getDefault());

                if (brokers != null && brokers.size() > 0)
                    return brokers.get(0).getName();

                return null;

            }
            if (columnName.equals(INTERESTTYPE)) {
                if(isRepo){
                    if (cash.getFixedRateB()) {
                        return "F";
                    }
                    else {
                        return "V";
                    }
                }else if(isPledge){
                    if(null!=fatherRepoPledge && ((Repo)fatherRepoPledge.getProduct()).getCash().getFixedRateB()){
                        return "F";
                    }else {
                        return "V";
                    }
                }
                return "";
            }

            if (columnName.equals(BOINDICATOR)) {
                Object isInternalDeal = super.getColumnValue(row, IS_INTERNAL_DEAL, errors);
                if (Util.isTrue(isInternalDeal, false))
                    return "INTERNA";
                return "EXTERNA";
            }
            if (columnName.equals(ISSUERGLS)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(security);
                if (underlyingIssuer != null)
                    return underlyingIssuer.getCode();
            }
            if (columnName.equals(ISSUERCODE)) {
                if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_FINANCIAL)) {
                    return "";
                }
                LegalEntity underlyingIssuer = getUnderlyingIssuer(security);
                if (underlyingIssuer != null) {
                    LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, underlyingIssuer.getId(),
                            "ALL", "SECTORCONTABLE");
                    if (attr != null)
                        return attr.getAttributeValue();
                }
            }
            if (columnName.equals(ISSUERDESC)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(security);
                if (underlyingIssuer != null)
                    return underlyingIssuer.getName();
            }
            if (columnName.equals(ISSUERID)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(security);
                if (underlyingIssuer != null)
                    return underlyingIssuer.getId();
            }
            if (columnName.equals(ISSUERSECTOR)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(security);
                if (underlyingIssuer != null) {
                    LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, underlyingIssuer.getId(),
                            "ALL", "SECTORCONTABLE");
                    if (attr != null)
                        return attr.getAttributeValue();
                }
            }
            if (columnName.equals(ISSUERCOUNTRY)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(security);
                if (underlyingIssuer != null) {
                    return getCountryIso(underlyingIssuer.getCountry());
                }
            }
            if (columnName.equals(ISSUERNAME)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(security);
                if (underlyingIssuer != null) {
                    return underlyingIssuer.getName();
                }
            }
            if (columnName.equals(INPUTDATEISSUER)) {
                if (!isPledge && row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_FINANCIAL)) {
                    return "";
                }
                return getIssueDate(security);
            }

            if (columnName.equals(MATURITYDATE)) {
                if(isRepo){
                    return new RepoReportUtil().getCallableOrProjectedDate(repo, valDate);
                }else if(isPledge){
                    if (pledge.isOpenTerm()) {
                        return valDate.addBusinessDays(1, Util.string2Vector("TARGET"));
                    } else {
                        return pledge.getEndDate();
                    }
                }
                return null;
            }

            if (columnName.equals(MATURITYTYPE)) {
                if(isRepo){
                    return formatResult(super.getColumnValue(row, columnName, errors));
                }else if(isPledge){
                    return pledge.isOpenTerm() ? "OPEN" : "TERM";
                }
                return null;
            }

            if (columnName.equals(DEADDATE)) {
                if(isRepo){
                    if (!repo.getMaturityType().equals("OPEN")) {
                        return repo.getEndDate();
                    }
                }else if(isPledge){
                    if (!pledge.isOpenTerm()) {
                        return pledge.getEndDate();
                    }
                }
                return "";
            }
            if (columnName.equals(MATURITYDATEISSUER)) {
                if (!isPledge && row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_FINANCIAL)) {
                    return "";
                }
                return security.getMaturityDate();
            }
            if (columnName.equals(INSTRUMENT)) {
                return "REPO";
            }
            if (columnName.equals(INSTRUMENT_ID)) {
                String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
                if (partenonAccountingID != null && partenonAccountingID.length() >= 21) {
                    return partenonAccountingID.substring(8, 11);
                }
                return null;
            }
            if (columnName.equals(LOTSIZE)) {
                if (!isPledge && row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_FINANCIAL)) {
                    return "";
                }
                return formatResult(getLotSize(security));
            }
            if (columnName.equals(UNITNOMINAL)) {
                if (!isPledge && row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_FINANCIAL)) {
                    return "";
                }
                if(security instanceof Bond){
                    return formatResult(getLotSize(security) * ((Bond)security).getCurrentFactor(valDate));
                }
                return 0.0;
            }
            if (columnName.equals(INFLACTIONINDICATOR)) {
                if (getInflationIndicator(repo))
                    return "S";
                return "N";
            }
            if (columnName.equals(SPREAD)) {
                if(isRepo){
                    if (cash.getFixedRateB()) {
                        return "";
                    }

                    double res = cash.getSpread() * 100;
                    return formatRate(res, '.');
                }else if(isPledge && null!=fatherRepoPledge){
                    final Cash pledgeFatherCash = ((Repo) fatherRepoPledge.getProduct()).getCash();
                    if(pledgeFatherCash.getFixedRateB()){
                        return "";
                    }
                    double res = pledgeFatherCash.getSpread() * 100;
                    return formatRate(res, '.');
                }
                return "";
            }
            if (columnName.equals(TRIPARTYINDICATOR)) {//TODO Duda
                if(isRepo){
                    if (repo.isTriparty())
                        return "S";
                    return "N";
                }
                return "";
            }
            if (columnName.equals(HAIRCUT)) {
                if(isRepo){
                    FieldEntry<?> fieldEntry = null;
                    fieldEntry = SecFinanceReportStyle.getSecFinanceTradeEntry(row).get("Sec. Haircut Value");
                    Object value = fieldEntry.getValue();

                    if (value instanceof Rate) {
                        Rate rate = (Rate) value;
                        return formatResult(rate.get());
                    }
                    return null;
                } else if(isPledge){
                    return formatResult(100 - parseVAFC(trade.getKeywordValue("92A::VAFC")));
                }

            }

            if (columnName.equals(YOURCUSTODIANCODE)) {
                LegalEntity custodian = getYourCustodian(transfer);
                if (custodian != null)
                    return custodian.getId();

            }
            if (columnName.equals(YOURCUSTODIANACCOUNT)) {
                SettleDeliveryInstruction sdi = getYourSDI(transfer);
                if (sdi != null)
                    return sdi.getAgentAccount();
            }
            if (columnName.equals(YOURCUSTODIANSWIFT)) {
                LegalEntity custodian = getYourCustodian(transfer);
                if (custodian != null)
                    return getSwiftCode(custodian, Arrays.asList("Agent"));
            }
            if (columnName.equals(YOURCUSTODIANGLS)) {
                LegalEntity custodian = getYourCustodian(transfer);
                if (custodian != null)
                    return custodian.getCode();
            }
            if (columnName.equals(YOURCUSTODIANDESC)) {
                LegalEntity custodian = getYourCustodian(transfer);
                if (custodian != null)
                    return custodian.getName();
            }
            if (columnName.equals(YOURCUSTODIANCOUNTRY)) {
                LegalEntity custodian = getYourCustodian(transfer);
                if (custodian != null)
                    return getCountryIso(custodian.getCountry());
            }
            if (columnName.equals(OURCUSTODIANCODE)) {
                LegalEntity custodian = getOurCustodian(transfer);
                if (custodian != null)
                    return custodian.getId();
            }
            if (columnName.equals(OURCUSTODIANACCOUNT)) {
                SettleDeliveryInstruction sdi = getOurSDI(transfer);
                if (sdi != null)
                    return sdi.getAgentAccount();
            }
            if (columnName.equals(OURCUSTODIANSWIFT)) {
                LegalEntity custodian = getOurCustodian(transfer);
                if (custodian != null)
                    return getSwiftCode(custodian, Arrays.asList("Agent"));
            }
            if (columnName.equals(OURCUSTODIANGLS)) {
                LegalEntity custodian = getOurCustodian(transfer);
                if (custodian != null)
                    return custodian.getCode();
            }
            if (columnName.equals(OURCUSTODIANDESC)) {
                LegalEntity custodian = getOurCustodian(transfer);
                if (custodian != null)
                    return custodian.getName();
            }
            if (columnName.equals(OURCUSTODIANCOUNTRY)) {
                LegalEntity custodian = getOurCustodian(transfer);
                if (custodian != null)
                    return getCountryIso(custodian.getCountry());
            }
            if (columnName.equals(LEGALENTITY)) {
                return "00001";
            }
            if (columnName.equals(SYSTEM_BO)) {
                return "CALYPSO STC";
            }

            if (columnName.equals(IMGROSSAMOUNT)) { //TODO Duda
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                        return "";
                    }
                    return formatResult(repo.getPrincipal(valDate));
                }else if(isPledge){
                    return formatResult(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_PRINCIPAL));
                }
                return "";
            }
            if (columnName.equals(IMFORWARDAMOUNT)) {
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                        return "";
                    }

                    try {
                        double spread = 1.0;

                        if (!repo.isFixedRate()) {
                            spread = repo.getCash().getSpread();
                        }

                        CashFlowSet cashFlows = repo.getFlows(valDate);

                        CashFlow lastInterest = null;
                        CashFlow lastPrincipal = null;
                        for (int i = 0; i < cashFlows.size(); i++) {
                            CashFlow cf = cashFlows.get(i);

                            // Avoid CashfLows type Collateral
                            if (cf.getCollateralId() > 0) {
                                continue;
                            }

                            if (cf.getType().equals(CashFlow.INTEREST) && (lastInterest == null || (lastInterest.getEndDate().before(cf.getEndDate())))) {
                                lastInterest = cf;
                            }

                            if (cf.getType().equals(CashFlow.PRINCIPAL) && (lastPrincipal == null || (lastPrincipal.getEndDate().before(cf.getEndDate())))) {
                                lastPrincipal = cf;
                            }

                        }

                        double amount = 0.0;

                        if (lastPrincipal != null) {
                            if (lastPrincipal.getAmount() != 0.0d) {
                                amount += lastPrincipal.getAmount();
                            }
                            else if (lastPrincipal instanceof CashFlowPrincipal) {
                                CashFlowPrincipal cfp = (CashFlowPrincipal)lastPrincipal;
                                if (cfp.getAmount() != 0.0d) {
                                    amount += cfp.getAmount();
                                }
                                else {
                                    amount += cfp.getNotional();
                                }
                            }
                        }

                        if (lastInterest != null && lastInterest instanceof CashFlowInterest) {
                            CashFlowInterest cfi = (CashFlowInterest)lastInterest;
                            double rate = cfi.getRate();
                            if (rate == 0.0) {
                                rate = 1.0;
                            }
                            if(null!=repo.getEndDate()){
                                amount += cfi.calculateAmount(rate * spread, repo.getEndDate());
                            }
                        }

                        return formatResult(amount);
                    } catch (FlowGenerationException e) {
                        Log.error(this, "Could not retrive Flows for Product " + repo.getId());
                        return null;
                    }
                }
                return null;
            }
            if (columnName.equals(NEXTINTERESTSETTLEMENTDATE)) {
                if(isRepo){
                    CashFlow cashFlow = getCurrentCashFlow(repo, valDate);
                    if (cashFlow != null) {
                        return cashFlow.getDate();
                    }
                }else if(isPledge && isFloatingFatherRepo(repoPledgeFather)){
                    CashFlow cashFlow = getCurrentCashFlow(repoPledgeFather, valDate);
                    if (cashFlow != null) {
                        return cashFlow.getDate();
                    }
                }
                return "";
            }
            if (columnName.equals(INTERESTFREQUENCYFIXATION)) {
                if(isRepo){
                    Frequency cmpFreq = null!= cash ? cash.getCompoundFrequency() : null;
                    if (cmpFreq != null && !cmpFreq.equals(Frequency.F_NONE)) {
                        return cash.getCompoundFrequency();
                    }
                    else {
                        return cash.getPaymentFrequency();
                    }
                }else if(isPledge && isFloatingFatherRepo(repoPledgeFather)){
                    final Cash repoCash = repoPledgeFather.getCash();
                    Frequency cmpFreq = null!= repoCash ? repoCash.getCompoundFrequency() : null;
                    if (cmpFreq != null && !cmpFreq.equals(Frequency.F_NONE)) {
                        return repoCash.getCompoundFrequency();
                    }
                    else {
                        return repoCash.getPaymentFrequency();
                    }
                }
                return "";
            }
            if (columnName.equals(INTERESTSETTLEMENTFREQUENCY)) {
                if(isRepo){
                    return null!=cash ? cash.getPaymentFrequency() : "";
                }else if(isPledge && isFloatingFatherRepo(repoPledgeFather)){
                    return null!=repoPledgeFather.getCash() ? repoPledgeFather.getCash().getPaymentFrequency() : "";
                }
            }
            if (columnName.equals(ISIN)) {
                return security.getSecCode(SecCode.ISIN);
            }
            if (columnName.equals(ISINDESC)) {
                return security.getDescription();
            }
            if (columnName.equals(GUARANTEETYPE)) {
                return "Repo";
            }
            if (columnName.equals(INSTRTYPE)) {
                String partenonAccountingID = trade.getKeywordValue("PartenonAccountingID");
                if (partenonAccountingID != null && partenonAccountingID.length() >= 21) {
                    return partenonAccountingID.substring(18, 21);
                }
                return null;
            }
            if (columnName.equals(SETTLEMENTTYPE)) { //TODO Duda deliveryType pledges
                if(isRepo){
                    if ("DAP".equals(repo.getDeliveryType())) {
                        return "CP";
                    }
                    if ("DFP".equals(repo.getDeliveryType())) {
                        return "LP";
                    }
                }
                return "";
            }
            if (columnName.equals(TYPEPAPER)) {
                return getType(security);
            }
            if (columnName.equals(INFLACTIONFACTOR)) {
                return getInflationFactor(security);
            }
            if (columnName.equals(CALCBASIS)) {
                return getCalcBasis(repo);
            }
            if (columnName.equals(INPUTDATEFLOWCURR)) {
                if(isRepo){
                    CashFlow cashFlow = getCurrentCashFlow(repo, valDate);
                    if (cashFlow != null)
                        return cashFlow.getStartDate();
                }
                return "";
            }
            if (columnName.equals(ENDDATEFLOWCURR)) {
                if(isRepo){
                    CashFlow cashFlow = getCurrentCashFlow(repo, valDate);
                    if (cashFlow != null)
                        return cashFlow.getEndDate();
                }
                return "";
            }
            if (columnName.equals(DIRTYPRICE) || columnName.equals(VALPRSUC)) { //TODO Duda pledge
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_FINANCIAL)) {
                        return "";
                    }
                    return formatResult(repo.getCollaterals().get(0).getNegociatedPrice() * 100.0);
                }
                if(isPledge){
                    final Object dirtyPrice = row.getProperty("DirtyPrice");
                    if(dirtyPrice instanceof Double){
                        return formatResult((Double)dirtyPrice*100);
                    }
                }
                return "";
            }
            if (columnName.equals(CLEANPRICE) || columnName.equals(VALPRLIM)) {
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_FINANCIAL)) {
                        return "";
                    }
                    return formatResult(repo.getCollaterals().get(0).getInitialPrice() * 100.0);
                }else if(isPledge){
                    final Object cleanPrice = row.getProperty("CleanPrice");
                    if(cleanPrice instanceof Double){
                        return formatResult((Double)cleanPrice*100);
                    }
                }
                return "";

            }
            if (columnName.equals(AVGPRINCIPAL)) { //TODO Calculo
                Boolean isBuy = (Boolean) super.getColumnValue(row, IS_BUY, errors);
                if (isBuy) {
                    return formatResult(repo.getPrincipal());
                } else
                    return formatResult(-repo.getPrincipal());

            }

            if (columnName.equals(MARKETVALUEMAN)) {
                return "TOBEDONE";
            }
            if (columnName.equals(MARKETVALUEACC)) { //TODO Calculo
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)){
                        return 0.0D;
                    }

                    SignedAmount pmMTM_NET_MUREX = (SignedAmount)super.getColumnValue(row, "Pricer.MTM_NET_MUREX", errors);
                    if (!Double.valueOf(pmMTM_NET_MUREX.get()).equals(Double.NaN)) {
                        return pmMTM_NET_MUREX.get();
                    }
                }else if(isPledge){
                    return formatResult(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MTM));
                }

                return 0.0;
            }

            if (columnName.equals(NEXTFIXINGDATE)) {
                if(isRepo){
                    return getNEXTFIXINGDATE(repo,valDate);
                }else if(isPledge && isFloatingFatherRepo(repoPledgeFather)){
                    return getNEXTFIXINGDATE(repoPledgeFather,valDate);
                }
                return "";
            }

            if (columnName.equals(ISSUERNIF)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(security);
                if (underlyingIssuer != null)
                    return underlyingIssuer.getExternalRef();
            }


            if (columnName.equals(FREQUENCECAPITALINT)) {
                Frequency freq = getPaymentFrequency(security);
                if (freq != null)
                    return freq.getTenor();
            }

            if (columnName.equals(SETTLEINTTYPE)) {
                return getSettlementType(security);
            }

            if (columnName.equals(COUPONTYPE)) {
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                        return "";
                    }
                    Frequency cmpFreq = null!=cash ? cash.getCompoundFrequency() : null;
                    if (cmpFreq != null && !cmpFreq.equals(Frequency.F_NONE)) {
                        return "C";
                    }
                    return "S";
                }
                return "";
            }
            if (columnName.equals(TOTALINTEREST)) {
                return 0;
            }
            if (columnName.equals(INTERESTACCRUAL)) { //TODO Calculo
                if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)) {
                    return "";
                }

                SignedAmount pm = (SignedAmount)super.getColumnValue(row, "Pricer.ACCRUAL_FIRST", errors);

                if (!Double.valueOf(pm.get()).equals(Double.NaN)) {
                    return formatResult(pm.get());
                }
                return 0.0;
            }

            if (columnName.equals(YESTERDAYPRICE)) {
                Double yesterdayPrice = row.getProperty(RepoMISReport.YESTERDAY_DIRTY_PRICE_STR);
                if (yesterdayPrice != null) {
                    return formatResult(yesterdayPrice);
                }
            }

            if (columnName.equals(HEDGINGTYPE)) {
                Book book = trade.getBook();
                if (book != null) {
                    AccountingBook acctBook = book.getAccountingBook();
                    if (acctBook != null) {
                        String acctBookName = acctBook.getName();
                        if (acctBookName.equals("Negociacion")) {
                            return "NEG";
                        } else if (acctBookName.equals("Disponible para la venta")) {
                            return "DV";
                        } else if (acctBookName.equals("Inversion crediticia")) {
                            return "COS";
                        } else if (acctBookName.equals("Inversion a vencimiento")) {
                            return "COS";
                        } else if (acctBookName.equals("Otros a valor razonable")) {
                            return "OVR";
                        }

                    }

                }


                return null;
            }


            if (columnName.equals(COMMISIONSTART)) {
//            	/!\
//                CashFlow cashFlow = UtilReport.getCashFlow(repo.getFlows(), valDate, CashFlow.SECLENDING_FEE, "Date", true);
//                if (cashFlow != null)
//                    return cashFlow.getStartDate();
//                else {
//                    return getStartFeeBillingPeriod(trade, valDate);
//                }
            }

            if (columnName.equals(COMMISIONEND)) {
//            	/!\
//                CashFlow cashFlow = UtilReport.getCashFlow(repo.getFlows(), valDate, CashFlow.SECLENDING_FEE, "Date", true);
//                if (cashFlow != null)
//                    return cashFlow.getStartDate();
//                else {
//                    return getEndFeeBillingPeriod(trade, valDate);
//                }
            }

            if (columnName.equals(TITLES)) {
                if (!isPledge && row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_FINANCIAL)) {
                    return "";
                }
                SignedAmount quantityAmount = (SignedAmount) super.getColumnValue(row, TradeReportStyle.QUANTITY, errors);
                if (quantityAmount != null) {
                    quantityAmount.set(Math.abs(quantityAmount.get()));
                    return formatResult(quantityAmount);
                }
            }


            if (columnName.equals(IDISSUEFINAN)) {
                return row.getProperty(IDISSUEFINAN);
            }

            if (columnName.equals("Pricer.MTM_NET_MUREX") || columnName.equals("Pricer.ACCRUAL_FIRST")
                    || columnName.equals("Pricer.MARKETVALUEMAN") || columnName.equals("Pricer.CUMULATIVE_CASH_INTEREST")){
                if(isRepo){
                    if (row.getProperty(IDISSUEFINAN).equals(RepoMISReport.IDISSUEFINAN_ISSUE)){
                        return "";
                    }
                    if(CollateralBasedUtil.isBSB(trade) && columnName.equals("Pricer.CUMULATIVE_CASH_INTEREST")){
                        columnName = "Pricer.SEC_FIN_SETTLED_INTEREST";
                    }
                    return formatResult(super.getColumnValue(row, columnName, errors));
                }else if(isPledge){
                    switch (columnName){
                        case "Pricer.ACCRUAL_FIRST":
                            return formatResult(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_ACCRUAL));
                        case "Pricer.MARKETVALUEMAN":
                            return formatResult(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MARKETVALUEMAN));
                        case "Pricer.MTM_NET_MUREX":
                            return "";
                        case "Pricer.CUMULATIVE_CASH_INTEREST":
                            return "";

                    }
                }
            }

            if (columnName.equals(FILLER1)){
                return getInternalPortfolioMirror(trade);
            }
            if(columnName.equals(TRIPARTYREPOINDICATOR)){
                if(isRepo){
                    if(repo.isTriparty()){
                        return "FINANCIAL";
                    }
                }else if(isPledge){
                    return "PLEDGE";
                }
                return "";
            }
            if(columnName.equals(TRIPARTYREPOTYPE)){
                return security instanceof Bond ? "BOND" : security instanceof Equity ? "EQUITY" : "";
            }
            if(columnName.equals(TRIPARTYREPOID)){
                return Optional.ofNullable(fatherRepoPledge).map(t -> t.getKeywordValue("PartenonAccountingID")).orElse("");
            }
            if(columnName.equals(TRIPARTYAGENT)){
                if(isPledge){
                    return trade.getKeywordValue("TripartyAgent");
                }else if(isRepo && repo.isTriparty()){
                    Trade tempTrade = MT527Helper.cloneTripartyTradeAndSetDummySecurity(trade);
                    if(Optional.ofNullable(tempTrade).isPresent()){
                        try {
                            final TradeTransferRule secXferRule = MT527Helper.getStartLegTransferRule(tempTrade, DSConnection.getDefault(), "SECURITY");
                            if(Optional.ofNullable(secXferRule).isPresent()){
                                final SettleDeliveryInstruction settleDeliveryInstruction = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), secXferRule.getProcessingOrgSDId());
                                if(Optional.ofNullable(settleDeliveryInstruction).isPresent()){
                                    final LegalEntity legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(), settleDeliveryInstruction.getAgentId());
                                    return Optional.ofNullable(legalEntity).map(LegalEntity::getCode).orElse("");
                                }
                            }
                        } catch (Exception e) {
                            Log.error(this,"Error getting Repo TripartyAgent: " + e);
                        }
                    }
                }
                return "";
            }if(columnName.equals(TRIPARTYAGENTLONG)){
                if(isPledge){
                    return Optional.ofNullable(LegalEntity.valueOf(trade.getKeywordValue("TripartyAgent"))).map(LegalEntity::getName).orElse("");
                }else if(isRepo && repo.isTriparty()){
                    Trade tempTrade = MT527Helper.cloneTripartyTradeAndSetDummySecurity(trade);
                    if(Optional.ofNullable(tempTrade).isPresent()){
                        try {
                            final TradeTransferRule secXferRule = MT527Helper.getStartLegTransferRule(tempTrade, DSConnection.getDefault(), "SECURITY");
                            if(Optional.ofNullable(secXferRule).isPresent()){
                                final SettleDeliveryInstruction settleDeliveryInstruction = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), secXferRule.getProcessingOrgSDId());
                                if(Optional.ofNullable(settleDeliveryInstruction).isPresent()){
                                    final LegalEntity legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(), settleDeliveryInstruction.getAgentId());
                                    return Optional.ofNullable(legalEntity).map(LegalEntity::getName).orElse("");
                                }
                            }
                        } catch (Exception e) {
                            Log.error(this,"Error getting Repo TripartyAgent: " + e);
                        }
                    }
                }
                return "";
            }
            if(columnName.equals(MASTERAGREEMENT)){
                return"GMRA";
            }

        }

        return formatResult(super.getColumnValue(row, columnName, errors));

    }

    public JDate getStartFeeBillingPeriod(Trade trade, JDate valDate) {

        JDate startDate = ((Repo) trade.getProduct()).getStartDate();
        JDate endDate = ((Repo) trade.getProduct()).getEndDate();
//    	/!\
        DateRule feeBillingPeriodDateRule = null; // ((Repo) trade.getProduct()).getFeeBillingPeriodDateRule();
        if (feeBillingPeriodDateRule == null)
            return null;
        Vector<JDate> feeBillingPeriods = feeBillingPeriodDateRule.generate(startDate, endDate);

        JDate previousDate = null;

        for (JDate date : feeBillingPeriods) {

            if (date.after(valDate)) {
                return previousDate;
            }
            previousDate = date;
        }

        return previousDate;


    }


    public JDate getEndFeeBillingPeriod(Trade trade, JDate valDate) {

        JDate startDate = ((Repo) trade.getProduct()).getStartDate();
        JDate endDate = ((Repo) trade.getProduct()).getEndDate();
//    	/!\
        DateRule feeBillingPeriodDateRule = null; // ((Repo) trade.getProduct()).getFeeBillingPeriodDateRule();
        if (feeBillingPeriodDateRule == null)
            return null;
        Vector<JDate> feeBillingPeriods = feeBillingPeriodDateRule.generate(startDate, endDate);

        for (JDate date : feeBillingPeriods) {
            if (date.after(valDate)) {
                return date.addDays(-1);
            }
        }

        return null;


    }

    public JDate getUnderlyingNextFixingDate(Repo repo, JDate valDate) {
        return UtilReport.getCashFlowDate(repo.getSecurity().getFlows(), valDate, CashFlow.INTEREST, "ResetDate", true);
    }


    public Integer getUnderlyingQuoteBase(Repo repo) {
        if (repo.getSecurity() instanceof Bond) {
            Bond bond = (Bond) repo.getSecurity();
            return bond.getQuoteBase();
        }
        return 1;
    }

    public String getSettlementType(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            return "POST";
        } else if (security instanceof Equity) {
            Equity equity = (Equity) security;
        }
        return null;
    }

    public CashFlow getCurrentCashFlow(Repo repo, JDate valDate) {
        CashFlowSet cashFlows;
        try {
            cashFlows = repo.getFlows(valDate);
        } catch (FlowGenerationException e) {
            Log.error(this, "Could not retrive Flows for Product " + repo.getId());
            return null;
        }

        if (cashFlows != null && !cashFlows.isEmpty()) {
            CashFlow cashFlow = (CashFlow) cashFlows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
            return cashFlow;
        }
        return null;
    }

    public String getKeywordValue(ReportRow row, String keyword, Vector errors) {
        Object value = super.getColumnValue(row, TRADE_KEYWORD_PREFIX + keyword, errors);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public String getISINDESC(Repo repo) {
        return repo.getSecurity().getDescription();
    }

    public String getISIN(Repo repo) {
        return repo.getSecurity().getSecCode(SecCode.ISIN);
    }


    public Frequency getPaymentFrequency(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            return bond.getCouponFrequency();
        } else if (security instanceof Equity) {
            Equity equity = (Equity) security;
            return equity.getDividendFrequency();
        }
        return null;
    }

    public DayCount getCalcBasis(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            return bond.getDaycount();
        } else if (security instanceof Equity) {
            Equity equity = (Equity) security;
        }
        return null;
    }

    public String getInflationFactor(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            if (bond.getNotionalIndex() != null)
                return bond.getNotionalIndex().getName();
        }
        return null;
    }

    public String getType(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            return bond.getType();
        }
        return null;
    }

    public double getNominal(Trade trade, Repo repo, JDate valDate, ReportRow row, Vector errors) {
        double nominal = repo.getRemainingQuantity(valDate);
        if (repo.getSecurity() instanceof Bond) {
            Bond bond = (Bond) repo.getSecurity();
            double faceValue = Optional.ofNullable(bond).map(Bond::getFaceValue).orElse(1.0D);
            nominal = nominal * faceValue;
        }
        return nominal;
    }

    public Double getRate(Repo repo) {
        //If not RF, value must be null
        Double rate = null;
        if (repo.getSecurity() instanceof Bond) {
            rate = 0.0D;
            Bond bond = (Bond) repo.getSecurity();
            if (bond.getFixedB()) {
                rate = bond.getCoupon() * 100;
            }
        }
        return rate;
    }

    public LegalEntity getUnderlyingIssuer(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            return BOCache.getLegalEntity(DSConnection.getDefault(), bond.getIssuerId());
        } else if (security instanceof Equity) {
            Equity equity = (Equity) security;
            return BOCache.getLegalEntity(DSConnection.getDefault(), equity.getIssuerId());
        }
        return null;
    }

    public JDate getIssueDate(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            return bond.getIssueDate();
        } else if (security instanceof Equity) {
            Equity equity = (Equity) security;
            return equity.getActiveDate();
        }
        return null;
    }


    public Double getLotSize(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            return bond.getFaceValue();
        } else if (security instanceof Equity) {
            return 1.0d;
        }
        return null;
    }

    public Boolean getInflationIndicator(Repo repo) {
        final String comment = DomainValues.comment("CodeActivationDV", "ActivateCapitalFactor");
        if(!Util.isEmpty(comment)){
            return true;
        }else if (repo.getSecurity() instanceof Bond) {
            Bond bond = (Bond) repo.getSecurity();
            return bond.getNotionalIndex() != null;
        }
        return false;
    }

    public String getInstrument(Repo repo) {
        if (repo.getSecurity() instanceof Bond) {
            return "SLR";
        } else if (repo.getSecurity() instanceof Equity) {
            return "SLV";
        }
        return null;
    }

    public LegalEntity getOurCustodian(BOTransfer transfer) {
        if (transfer == null)
            return null;
        return BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getInternalAgentId());
    }

    public LegalEntity getYourCustodian(BOTransfer transfer) {
        if (transfer == null)
            return null;
        return BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getExternalAgentId());
    }


    public String getSwiftCode(LegalEntity le, List<String> roles) {
        for (String role : roles) {
            for (String contactType : contactTypes) {
                LEContact contact = BOCache.getContact(DSConnection.getDefault(), role, le, contactType, LEContact.ALL, 0);
                if (contact != null && contact.getSwift() != null)
                    return contact.getSwift();
            }
        }
        return null;
    }


	/*  public LegalEntity getYourCustodian(Vector<TradeTransferRule> transferRules) {
		  for(TradeTransferRule transferRule : transferRules) {
			  if(transferRule.getTransferType().equals("SECURITY")) {
				  if(transferRule.getReceiverLegalEntityRole().equals("ProcessingOrg")) {
					  return BOCache.getLegalEntity(DSConnection.getDefault(),transferRule.getReceiverLegalEntityId());
				  }
				  else {
					  return BOCache.getLegalEntity(DSConnection.getDefault(),transferRule.getPayerLegalEntityId());
				  }
			  }
		  }
		  return null;

	  }

		public LegalEntity getOurCustodian(Vector<TradeTransferRule> transferRules) {
			  for(TradeTransferRule transferRule : transferRules) {
				  if(transferRule.getTransferType().equals("SECURITY")) {
					  if(transferRule.getReceiverLegalEntityRole().equals("ProcessingOrg")) {
						  return BOCache.getLegalEntity(DSConnection.getDefault(),transferRule.getPayerLegalEntityId());
					  }
					  else {
						  return BOCache.getLegalEntity(DSConnection.getDefault(),transferRule.getReceiverLegalEntityId());
					  }
				  }
			  }
			  return null;
		}*/

    public SettleDeliveryInstruction getYourSDI(BOTransfer transfer) {
        if (transfer == null)
            return null;
        return BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), transfer.getExternalSettleDeliveryId());
    }

    public SettleDeliveryInstruction getOurSDI(BOTransfer transfer) {
        if (transfer == null)
            return null;
        return BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), transfer.getInternalSettleDeliveryId());
    }


    public static Object formatResult(Object o) {
        return UtilReport.formatResult(o, '.');
    }

    private String getCountryIso(String country) {
        String isoCode = BOCache.getCountry(DSConnection.getDefault(), country).getISOCode();
        return !Util.isEmpty(isoCode) ? isoCode : "";
    }

    public String getRefIndex(Repo repo) {
        Cash cash = repo.getCash();
        if (cash.getFixedRateB()) {
            return "";
        }

        return cash.getRateIndex().getKeyCurrencyIndexTenor().replace('.','#');
    }

    public static Object formatRate(double number, char decimalSeparator) {
        DecimalFormat df = new DecimalFormat("0.00000");
        df.setGroupingUsed(false);
        DecimalFormatSymbols newSymbols = new DecimalFormatSymbols();
        newSymbols.setDecimalSeparator(decimalSeparator);
        df.setDecimalFormatSymbols(newSymbols);
        return df.format(number);
    }

    public boolean isAmortizationSinking(String strAmortizationType) {
        if(strAmortizationType ==null || "Bullet".equals(strAmortizationType)) {
            return false;
        }
        return true;
    }

    public boolean checkValidation (int i, JDate dateChecked, JDate dateToCheck){
        return i != 0 ? dateChecked != null ? dateToCheck.after(dateChecked) ?  dateToCheck.after(JDate.getNow()) ? false : true : false : true : true;
    }

    public String getInternalPortfolioMirror(Trade trade) {
        return trade.getMirrorBook() != null ? trade.getMirrorBook().toString() : "";
    }

    private String mapDirection(Trade trade){
        AtomicReference<String> direction = new AtomicReference<>("");
        if(trade.getProduct() instanceof Repo){
            Repo repo = (Repo) trade.getProduct();
            if("Repo".equalsIgnoreCase(repo.getDirection(Repo.REPO,repo.getSign()))){
                direction.set("CEDIDO");
            }else {
                direction.set("TOMADO");
            }
        }else if(trade.getQuantity() < 0.0D){
            direction.set("CEDIDO");
        }else {
            direction.set("TOMADO");
        }
        return direction.get();
    }


    private Object generateRate(Repo repo,JDatetime valDateTime, JDate valDate){
        if ("FLOATING".equalsIgnoreCase(repo.getCash().getRateType())) {
            PricingEnv OficialPricePE = AppUtil.loadPE(OFFICIAL, valDateTime);
            double closePrice = RepoReportUtil.getIndexbyQuoteSet(valDate, OficialPricePE, repo);
            CashFlow cf = (CashFlow) getCurrentCashFlow(repo, valDate);
            if (cf != null && cf instanceof CashFlowInterest) {
                CashFlowInterest cfi = (CashFlowInterest) cf;
                double res = (closePrice + cfi.getSpread()) * 100.0;
                return formatRate(res, '.');
            }
        } else if ("FIXED".equalsIgnoreCase(repo.getCash().getRateType())) {
            double res = repo.getCash().getFixedRate() * 100.0;
            return formatRate(res, '.');
        }
        return "0.00000";
    }

    private boolean isFloatingFatherRepo(Repo repo){
        return null!=repo && "FLOATING".equalsIgnoreCase(repo.getCash().getRateType());
    }


    private String getFinancialFatherRepo(Trade trade){
        String value = "";
        if(null!=trade && trade.getProduct() instanceof Repo){
            Repo repo = (Repo)trade.getProduct();
            String secCurrency = repo.getSecurity().getCurrency();
            String cashCurrency = repo.getCash().getCurrency();
            if (secCurrency.equals(cashCurrency)) {
                value =  RepoMISReport.IDISSUEFINAN_ISSUE_FINANCIAL;
            }
            else {
                value = RepoMISReport.IDISSUEFINAN_FINANCIAL;
            }
        }
        return value;
    }



    private JDate getNEXTFIXINGDATE(Repo repo,JDate valDate){
        if (repo.getCash().getFixedRateB()) {
            return null;
        }
        boolean rfr = Boolean.valueOf(repo.getCash().getRateIndex().getDefaults().getAttribute(RFR));
        if(rfr){
            return RepoReportUtil.getNextBusinessDay(valDate);
        }
        ArrayList<JDate> resetDates = new ArrayList<JDate>();
        try {
            CashFlowSet cashFlows = repo.getFlows(valDate);
            if (cashFlows != null) {
                for (int i = 0; i < cashFlows.size(); i++) {
                    CashFlow cf = cashFlows.get(i);
                    if (cf instanceof CashFlowInterest) {
                        CashFlowInterest cfi = (CashFlowInterest)cf;
                        if (cfi.getResetDate() != null) {
                            resetDates.add(cfi.getResetDate());
                        }
                    }
                }
            }
        } catch (FlowGenerationException e) {
            Log.error(this, "Could not retrive Flows for Product " + repo.getId());
            return null;
        }

        for (JDate resetDate : resetDates) {
            if (resetDate.after(valDate)) {
                return resetDate;
            }
        }
        return null;
    }

    private Double parseVAFC(String value){
        Locale locale = new Locale("es", "ES");
        return !Util.isEmpty(value) ? Util.stringToNumber(value, locale) : 0D;
    }

}
