package calypsox.tk.report;

import calypsox.tk.report.util.UtilReport;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeRoleFinder;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricerConfig;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Dividend;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.report.ProductReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.SecFinanceReportStyle;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.PricerMeasureUtility;
import com.calypso.tk.util.fieldentry.FieldEntry;

import java.util.*;

public class PdvMISReportStyle extends TradeReportStyle {

    public static final String LOG_CATEGORY = "PdvMISReportStyle";

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
    public static final String ISINDESC = "ISINDESC";/*
	public static final String STATUS="STATUS";*/
    public static final String FODEALFO = "FODEALFO";
    public static final String CONTRACTORIGIN = "CONTRACTORIGIN";
    public static final String SOURCESYSTEM = "SOURCESYSTEM";
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
    /*public static final String INPUTDATE="INPUTDATE";
    public static final String TRADEDATE="TRADEDATE";
    public static final String VALUEDATE="VALUEDATE";
    public static final String MATURITYDATE="MATURITYDATE";*/
    public static final String DEADDATE = "DEADDATE";/*
	public static final String STRATEGYHEDGE="STRATEGYHEDGE";*/
    public static final String PORTFOLIO = "PORTFOLIO";
    public static final String FOLDER = "FOLDER";
    public static final String YIELD = "YIELD";/*
	public static final String CURRENCY="CURRENCY";
	public static final String SETTLECURR="SETTLECURR";*/
    public static final String PRINCIPAL = "PRINCIPAL";
    public static final String PRINCIPALCUR = "PRINCIPALCUR";
    public static final String NOMINAL = "NOMINAL";
    public static final String UNITNOMINAL = "UNITNOMINAL";
    public static final String LOSTSIZE = "LOSTSIZE";
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
    public static final String ISSUERSECTOR = "ISSUERSECTOR";
    public static final String ISSUERCOUNTRY = "ISSUERCOUNTRY";
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
    public static final String COMMISSIONCURPENDINGSETTLE = "COMMISSIONCURPENDINGSETTLE";
    public static final String COMMISSION = "COMMISSION";
    public static final String COMMISSIONSETTLE = "COMMISSIONSETTLE";
    public static final String COMMISSIONPENDINGSETTLE = "COMMISSIONPENDINGSETTLE";
    public static final String TOTALCOMMISSION = "TOTALCOMMISSION";
    public static final String ANUALCOMMISSIONCURRENCY = "ANUALCOMMISSIONCURRENCY";
    public static final String ANUALCOMMISSION = "ANUALCOMMISSION";
    public static final String ISSUERNIF = "ISSUERNIF";
    public static final String CASH = "CASH";
    public static final String HAIRCUT = "HAIRCUT";
    public static final String COMMISIONSTART = "COMMISIONSTART";
    public static final String COMMISIONEND = "COMMISIONEND";
    public static final String TITLES = "TITLES";

    public static final String FILLER1 = "FILLER1";
    public static final String FILLER2 = "FILLER2";
    public static final String FILLER3 = "FILLER3";
    public static final String FILLER4 = "FILLER4";
    public static final String FILLER5 = "FILLER5";


    public static final ArrayList<String> emptyColumns = new ArrayList<String>();
    public static final HashMap<String, String> columnToKeyword = new HashMap<String, String>();
    public static final HashMap<String, String> columnToColumn = new HashMap<String, String>();

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
        columnToKeyword.put(FODEALFO, "MurexRootContract");

        columnToColumn.put(PORTFOLIO, TradeReportStyle.BOOK);
        columnToColumn.put(FOLDER, TradeReportStyle.BOOK);
        columnToColumn.put(DEADDATE, ProductReportStyle.MATURITY_DATE);


    }


    PdvContingenciaReportStyle contingenciaReportStyle = new PdvContingenciaReportStyle();
    public static final String SEC_EQ_PRICE_INITIAL = "Sec. Price (Initial)";

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

        if (emptyColumns.contains(columnName))
            return "";
        if (columnToKeyword.containsKey(columnName)) {
            return getKeywordValue(row, columnToKeyword.get(columnName), errors);
        }
        if (columnToColumn.containsKey(columnName)) {
            return super.getColumnValue(row, columnToColumn.get(columnName), errors);
        }

        Trade trade = row.getProperty(ReportRow.TRADE);
        BOTransfer transfer = row.getProperty(ReportRow.TRANSFER);
        PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

        Product product = trade.getProduct();

        if (product instanceof SecLending) {

            SecLending secLending = (SecLending) product;

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
                if (secLending.getSecurity() instanceof Bond)
                    return "MUREX FXFI";
                if (secLending.getSecurity() instanceof Equity)
                    return "MUREX EQ";
            }

            if (columnName.equals(ORIGIN)) {
                return "800018693";
            }
            if (columnName.equals(PROCESSDATE)) {
                return valDate;
            }
            if (columnName.equals(DIRECTION)) {
                String direction = (String) super.getColumnValue(row, "Direction", errors);
                if (direction.equals("Sec. Lending"))
                    return "S";
                if (direction.equals("Sec. Borrowing"))
                    return "B";
            }
            if (columnName.equals(PRINCIPALCUR)) {
                return "";
            }
            if (columnName.equals(PRINCIPAL)) {
                if (secLending.getSecurity() instanceof Equity) {
                    return formatResult(getNominal(trade, secLending, valDate, row, errors));
                } else {
                    return formatResult(secLending.computeNominal(trade, valDate));
                }
            }
            if (columnName.equals(NOMINAL)) {
                return formatResult(getNominal(trade, secLending, valDate, row, errors));
            }
            if (columnName.equals(PRICEATSTART)) {
                return super.getColumnValue(row, SEC_EQ_PRICE_INITIAL, errors);
            }
            if (columnName.equals(RATE)) {
                return formatResult(getRate(secLending));
            }
            if (columnName.equals(BROKERDESC)) {
                Vector<LegalEntity> brokers = TradeRoleFinder.getTradeRoles("Broker", trade, new Vector(), DSConnection.getDefault());

                if (brokers != null && brokers.size() > 0)
                    return brokers.get(0).getName();

                return null;

            }
            if (columnName.equals(INTERESTTYPE)) {
                return getInterestType(secLending);
            }
            if (columnName.equals(REFINDEX)) {
                return getRefIndex(secLending);
            }
            if (columnName.equals(BOINDICATOR)) {
                Object isInternalDeal = super.getColumnValue(row, IS_INTERNAL_DEAL, errors);
                if (Util.isTrue(isInternalDeal, false))
                    return "S";// INTERNA
                return "N";
            }
            if (columnName.equals(ISSUERGLS)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(secLending);
                if (underlyingIssuer != null)
                    return underlyingIssuer.getCode();
            }
            if (columnName.equals(ISSUERCODE)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(secLending);
                if (underlyingIssuer != null) {
                    LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, underlyingIssuer.getId(),
                            "ALL", "SECTORCONTABLE");
                    if (attr != null)
                        return attr.getAttributeValue();
                }
            }
            if (columnName.equals(ISSUERDESC)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(secLending);
                if (underlyingIssuer != null)
                    return underlyingIssuer.getName();
            }
            if (columnName.equals(ISSUERSECTOR)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(secLending);
                if (underlyingIssuer != null) {
                    LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, underlyingIssuer.getId(),
                            "ALL", "SECTORCONTABLE");
                    if (attr != null)
                        return attr.getAttributeValue();
                }
            }
            if (columnName.equals(ISSUERCOUNTRY)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(secLending);
                if (underlyingIssuer != null) {
                    return getIsoCode(underlyingIssuer.getCountry());
                }

            }
            if (columnName.equals(INPUTDATEISSUER)) {
                return getIssueDate(secLending);
            }
            if (columnName.equals(MATURITYDATEISSUER)) {
                return getSecMaturityDate(secLending, valDate);
            }
            if (columnName.equals(INSTRUMENT)) {
                return getInstrument(secLending);
            }
            if (columnName.equals(LOSTSIZE) || columnName.equals(UNITNOMINAL)) {
                return getLostSize(secLending);
            }
            if (columnName.equals(INFLACTIONINDICATOR)) {
                if (getInflationIndicator(secLending))
                    return "S";
                return "N";
            }
            if (columnName.equals(SPREAD)) {
                return formatResult(getSpread(secLending));
            }
            if (columnName.equals(TRIPARTYINDICATOR)) {
                if (secLending.isTriparty())
                    return "S";
                return "N";
            }
            if (columnName.equals(HAIRCUT)) {
                FieldEntry<?> fieldEntry = null;
                fieldEntry = SecFinanceReportStyle.getSecFinanceTradeEntry(row).get("Sec. Margin Value");
                Object value = fieldEntry.getValue();

                if (value instanceof Rate) {
                    Rate rate = (Rate) value;
                    return formatResult(rate.get() / 100);
                }
                return null;

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
                    return getIsoCode(custodian.getCountry());
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
                    return getIsoCode(custodian.getCountry());
            }
            if (columnName.equals(LEGALENTITY)) {
                return "00001";
            }
            if (columnName.equals(SYSTEM_BO)) {
                return "CALYPSO STC";
            }

            if (columnName.equals(IMGROSSAMOUNT)) {
                return row.getProperty(PdvMISReport.CASH_COLLATERAL);
            }
            if (columnName.equals(IMFORWARDAMOUNT)) {
                return row.getProperty(PdvMISReport.CASH_COLLATERAL);
            }
            if (columnName.equals(NEXTINTERESTSETTLEMENTDATE)) {
                return getNextInterestDate(secLending, valDate);
            }
            if (columnName.equals(INTERESTFREQUENCYFIXATION)) {
                Frequency freq = getFrequency(secLending);
                if (freq != null)
                    return freq.getTenor();
            }
            if (columnName.equals(INTERESTSETTLEMENTFREQUENCY)) {
                Frequency freq = getFrequency(secLending);
                if (freq != null)
                    return freq.getTenor();
            }
            if (columnName.equals(ISIN)) {
                return getISIN(secLending);
            }
            if (columnName.equals(ISINDESC)) {
                return getISINDESC(secLending);
            }
            if (columnName.equals(GUARANTEETYPE)) {
                return "PDV";
            }
            if (columnName.equals(INSTRTYPE)) {
                return "SecLending";
            }
            if (columnName.equals(SETTLEMENTTYPE)) {
                if (transfer != null && "DAP".equals(transfer.getDeliveryType())) {
                    return "CP";
                }
                if (transfer != null && "DFP".equals(transfer.getDeliveryType())) {
                    return "LP";
                }
            }
            if (columnName.equals(TYPEPAPER)) {
                return getType(secLending);
            }
            if (columnName.equals(INFLACTIONFACTOR)) {
                return getInflationFactor(secLending);
            }
            if (columnName.equals(CALCBASIS)) {
                return getCalcBasis(secLending);
            }
            if (columnName.equals(INPUTDATEFLOWCURR)) {
                CashFlow cashFlow = getCurrentCashFlow(secLending, valDate);
                if (cashFlow != null)
                    return cashFlow.getStartDate();
            }
            if (columnName.equals(ENDDATEFLOWCURR)) {
                CashFlow cashFlow = getCurrentCashFlow(secLending, valDate);
                if (cashFlow != null)
                    return cashFlow.getEndDate();
            }
            if (columnName.equals(DIRTYPRICE)) {

                Double price = row.getProperty(PdvMISReport.DIRTY_PRICE_STR);
                if (price != null)
                    return formatResult(new BondPrice(price, getUnderlyingQuoteBase(secLending)).toString());
            }
            if (columnName.equals(CLEANPRICE)) {
                Double price = row.getProperty(PdvMISReport.CLEAN_PRICE_STR);
                if (price != null) {
                    if (secLending.getSecurity() instanceof Bond) {
                        return formatResult(new BondPrice(price, getUnderlyingQuoteBase(secLending)).toString());
                    } else {
                        return formatResult(price);
                    }
                }
            }
            if (columnName.equals(AVGPRINCIPAL)) {
                Boolean isBuy = (Boolean) super.getColumnValue(row, IS_BUY, errors);
                if (isBuy) {
                    return formatResult(secLending.getPrincipal());
                } else
                    return formatResult(-secLending.getPrincipal());

            }

            if (columnName.equals(MARKETVALUE)) {
				/*Double price = row.getProperty(PdvMISReport.DIRTY_PRICE_STR);
				if(price!=null) {
					double notional = secLending.getNotional(valDate);
					return formatResult(price*getUnderlyingQuoteBase(secLending)*notional);
					}*/
                return "";
            }

            if (columnName.equals(MARKETVALUEMAN) || columnName.equals(MARKETVALUEACC)) {
                Double price = row.getProperty(PdvMISReport.DIRTY_PRICE_STR);
                if (price != null) {
                    if (secLending.getSecurity() instanceof Bond && !"Negociacion".equalsIgnoreCase(trade.getBook().getAccountingBook().getName())) {
                        return formatResult(secLending.computeNominal(trade, valDate));
                    } else {
                        return formatResult(price * secLending.computeNominal(trade, valDate));
                    }
                }
            }

            if (columnName.equals(NEXTFIXINGDATE)) {
                JDate nextFixingDate = getUnderlyingNextFixingDate(secLending, valDate);
                if (nextFixingDate != null)
                    return nextFixingDate;

                return super.getColumnValue(row, ProductReportStyle.MATURITY_DATE, errors);
            }

            if (columnName.equals(ISSUERNIF)) {
                LegalEntity underlyingIssuer = getUnderlyingIssuer(secLending);
                if (underlyingIssuer != null)
                    return underlyingIssuer.getExternalRef();
            }


            if (columnName.equals(FREQUENCECAPITALINT)) {
                Frequency freq = getPaymentFrequency(secLending);
                if (freq != null)
                    return freq.getTenor();
            }

            if (columnName.equals(SETTLEINTTYPE)) {
                return getSettlementType(secLending);
            }

            if (columnName.equals(COMMISSIONCURPENDINGSETTLE)) {
                return secLending.getFeeCurrency();
            }

            if (columnName.equals(COMMISSION) || columnName.equals(COMMISSIONPENDINGSETTLE)) {
                try{
                    CashFlow cashFlow = UtilReport.getCashFlow(secLending.getFlows(valDate), valDate, CashFlow.SECLENDING_FEE, "Date", true);

                    if (null != cashFlow) {
                        //return formatResult(cashFlow.getAmount()); //-> doCalculation de pdvInformesReportStyle (NO FUNCIONA GENERA UN AMOUNT DE 0.0)
                        return formatResult(PdvInformesReportStyle.doCalculation(valDateTime, trade, new PricerMeasure(PricerMeasure.INDEMNITY_ACCRUAL), "PdvMIS"));
                    }else {
                        JDate endPeriod = getEndFeeBillingPeriod(trade, valDate);
                        if (endPeriod != null) {
                            PricerConfig config = pricingEnv.getPricerConfig();
                            Product p = trade.getProduct();
                            Pricer pricer = config.getPricerInstance(p);

                            Vector<String> pmNames = new Vector<String>();

                            pmNames.add(PricerMeasure.S_INDEMNITY_ACCRUAL);
                            PricerMeasure[] measures = PricerMeasureUtility.makeMeasures(DSConnection.getDefault(), pmNames, new Vector());
                            try {
                                pricer.price(trade, JDatetime.currentTimeValueOf(endPeriod, pricingEnv.getTimeZone()), pricingEnv, (PricerMeasure[]) measures);
                                return formatResult(measures[0].getDisplayValue());
                            } catch (PricerException e) {
                                Log.error(LOG_CATEGORY, e);
                            }
                            return null;
                        }
                    }
                }catch(FlowGenerationException fge){
                    Log.error(LOG_CATEGORY, fge);
                }

            }
            if (columnName.equals(COMMISSIONSETTLE)) {
                CashFlow cashFlow = UtilReport.getCashFlow(secLending.getFlows(), valDate, CashFlow.SECLENDING_FEE, "Date", false);
                if (cashFlow != null)
                    return formatResult(cashFlow.getAmount());
            }
            if (columnName.equals(TOTALCOMMISSION)) {
                return 0;
            }
            if (columnName.equals(ANUALCOMMISSIONCURRENCY)) {
                return secLending.getFeeCurrency();
            }
            if (columnName.equals(ANUALCOMMISSION)) {
                CashFlowSet cashFlows = secLending.getFlows();
                double annualCommission = 0;

                if (cashFlows != null) {

                    List<CashFlow> allFlows = Arrays.asList(cashFlows.getFlows());
                    for (CashFlow cf : allFlows) {


                        if (cf.getType().equals(CashFlow.SECLENDING_FEE)) {
                            if (cf.getDate().getYear() == valDate.getYear() && cf.getDate().lte(valDate))
                                annualCommission += cf.getAmount();
                        }
                    }
                }
                return formatResult(annualCommission);
            }
            if (columnName.equals(COUPONTYPE)) {
                return "S";
            }
            if (columnName.equals(TOTALINTEREST)) {
                return 0;
            }
            if (columnName.equals(INTERESTACCRUAL)) {

            }

            if (columnName.equals(YESTERDAYPRICE)) {
                Double yesterdayPrice = row.getProperty(PdvMISReport.YESTERDAY_DIRTY_PRICE_STR);
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
                CashFlow cashFlow = UtilReport.getCashFlow(secLending.getFlows(), valDate, CashFlow.SECLENDING_FEE, "Date", true);
                if (cashFlow != null)
                    return cashFlow.getStartDate();
                else {
                    return getStartFeeBillingPeriod(trade, valDate);
                }
            }

            if (columnName.equals(COMMISIONEND)) {
                CashFlow cashFlow = UtilReport.getCashFlow(secLending.getFlows(), valDate, CashFlow.SECLENDING_FEE, "Date", true);
                if (cashFlow != null)
                    return cashFlow.getStartDate();
                else {
                    return getEndFeeBillingPeriod(trade, valDate);
                }
            }

            if (columnName.equals(TITLES)) {
                SignedAmount quantityAmount = (SignedAmount) super.getColumnValue(row, TradeReportStyle.QUANTITY, errors);
                if (quantityAmount != null) {
                    quantityAmount.set(Math.abs(quantityAmount.get()));
                    return formatResult(quantityAmount);
                }
            }

            if (columnName.equals(COUNTERPARTYCOUNTRY)){
                return getIsoCode(trade.getCounterParty().getCountry());
            }

            if (columnName.equals(FILLER1)){
                return getInternalPortfolioMirror(trade);
            }


        }

        return formatResult(super.getColumnValue(row, columnName, errors));

    }

    public String getIsoCode (String countryS){
        Country country = BOCache.getCountry(DSConnection.getDefault(), countryS);
        return country!=null ? country.getISOCode() : countryS;
    }

    public JDate getStartFeeBillingPeriod(Trade trade, JDate valDate) {

        JDate startDate = ((SecLending) trade.getProduct()).getStartDate();
        JDate endDate = ((SecLending) trade.getProduct()).getEndDate();
        DateRule feeBillingPeriodDateRule = ((SecLending) trade.getProduct()).getFeeBillingPeriodDateRule();
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

        JDate startDate = ((SecLending) trade.getProduct()).getStartDate();
        JDate endDate = ((SecLending) trade.getProduct()).getEndDate();
        DateRule feeBillingPeriodDateRule = ((SecLending) trade.getProduct()).getFeeBillingPeriodDateRule();
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

    public JDate getUnderlyingNextFixingDate(SecLending secLending, JDate valDate) {
        return UtilReport.getCashFlowDate(secLending.getSecurity().getFlows(), valDate, CashFlow.INTEREST, "ResetDate", true);
    }


    public Integer getUnderlyingQuoteBase(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getQuoteBase();
        }
        return 1;
    }

    public String getSettlementType(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return "POST";
        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
        }
        return null;
    }

    public CashFlow getCurrentCashFlow(SecLending secLending, JDate valDate) {
        CashFlowSet cashFlows = secLending.getFlows();
        CashFlow cashFlow = (CashFlow) cashFlows.findEnclosingCashFlow(valDate, CashFlow.SECURITY);
        return cashFlow;
    }

    public String getKeywordValue(ReportRow row, String keyword, Vector errors) {
        Object value = super.getColumnValue(row, TRADE_KEYWORD_PREFIX + keyword, errors);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public String getISINDESC(SecLending secLending) {
        return secLending.getSecurity().getDescription();
    }

    public String getISIN(SecLending secLending) {
        return secLending.getSecurity().getSecCode(SecCode.ISIN);
    }


    public Frequency getPaymentFrequency(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getCouponFrequency();
        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            return equity.getDividendFrequency();
        }
        return null;
    }

    public DayCount getCalcBasis(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getDaycount();
        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
        }
        return null;
    }

    public String getInflationFactor(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            if (bond.getNotionalIndex() != null)
                return bond.getNotionalIndex().getName();
        }
        return null;
    }

    public String getType(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getType();
        }
        return null;
    }

    public double getNominal(Trade trade, SecLending secLending, JDate valDate, ReportRow row, Vector errors) {
        double nominal = secLending.getRemainingQuantity(valDate);
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            double faceValue = Optional.ofNullable(bond).map(Bond::getFaceValue).orElse(1.0D);
            nominal = nominal * faceValue;
        } else {
            Object initialPrice = super.getColumnValue(row, SEC_EQ_PRICE_INITIAL, errors);
            if (initialPrice instanceof DisplayValue) {
                nominal = nominal * ((DisplayValue) initialPrice).get();
            }
        }
        return nominal;
    }

    public Double getRate(SecLending secLending) {
        //If not RF, value must be null
        Double rate = null;
        if (secLending.getSecurity() instanceof Bond) {
            rate = 0.0D;
            Bond bond = (Bond) secLending.getSecurity();
            if (bond.getFixedB()) {
                rate = bond.getCoupon() * 100;
            }
        }
        return rate;
    }

    public Double getSpread(SecLending secLending) {
        //If not RF, value must be null
        Double spread = null;
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond)secLending.getSecurity();
            spread=bond.getRateIndexSpread()*100;
            //UNTIL 22-08 SPREAD AS 0
            //spread = 0.0D;
        }
        return spread;
    }


    public String getInterestType(SecLending secLending) {
        String intType = "F";
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            if (!bond.getFixedB()) {
                intType = "V";
            }
        }
        return intType;
    }

    public String getRefIndex(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            if (!bond.getFixedB() && bond.getRateIndex() != null) {
                return bond.getRateIndex().getKeyCurrencyIndexTenor().replace('.', '#');
            }

        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            if (equity.getRateIndex() != null)
                return equity.getRateIndex().getName();
        }
        return null;
    }

    public LegalEntity getUnderlyingIssuer(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return BOCache.getLegalEntity(DSConnection.getDefault(), bond.getIssuerId());
        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            return BOCache.getLegalEntity(DSConnection.getDefault(), equity.getIssuerId());
        }
        return null;
    }

    public JDate getIssueDate(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getIssueDate();
        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            return equity.getActiveDate();
        }
        return null;
    }

    public JDate getSecMaturityDate(SecLending secLending, JDate valDate) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getMaturityDate();
        } else if (secLending.getSecurity() instanceof Equity) {

            JDate thirdFridayDecember = this.contingenciaReportStyle.getThirdFridayDecemberDate(valDate);
            if(valDate.after(thirdFridayDecember)||valDate.equals(thirdFridayDecember)){
                JDate thirFridayDecemberNextYear = this.contingenciaReportStyle.getThirdFridayDecemberDate(valDate.addMonths(12));
                return thirFridayDecemberNextYear;

            }
            return thirdFridayDecember;
        }
        return null;
    }

    public Double getLostSize(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getFaceValue();
        } else if (secLending.getSecurity() instanceof Equity) {
            return 1.0d;
        }
        return null;
    }

    public Boolean getInflationIndicator(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getNotionalIndex() != null;
        }
        return false;
    }

    public JDate getNextInterestDate(SecLending secLending, JDate valDate) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getNextCouponDate(valDate);
        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            Dividend d = equity.getNextDividend(valDate);
            if (d != null)
                return d.getPaymentDate();
        }
        return null;
    }

    public Frequency getFrequency(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getCouponFrequency();
        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            return equity.getDividendFrequency();
        }
        return null;
    }


    public String getInstrument(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            return "SLR";
        } else if (secLending.getSecurity() instanceof Equity) {
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

    public String getInternalPortfolioMirror(Trade trade) {
        return trade.getMirrorBook() != null ? trade.getMirrorBook().toString() : "";
    }

}
