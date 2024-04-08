package calypsox.tk.report;


import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.fiflow.builder.handler.FIFlowTradeSecurityHandler;
import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.*;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.pricer.PricerBond;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.FdnUtilProvider;
import com.calypso.tk.util.PricerMeasureUtility;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;
import org.apache.commons.lang.StringUtils;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;


public class BondMisPlusReportStyle extends TradeReportStyle {


    public static final String ORIGIN = "ORIGIN";
    public static final String PROCESSDATE = "PROCESSDATE";
    public static final String ENTITY = "ENTITY";
    public static final String ACCOUNTING_CENTER = "ACCOUNTING_CENTER";
    public static final String INTERNALREF = "INTERNALREF";
    public static final String ISIN = "ISIN";
    public static final String STATUS = "STATUS";
    public static final String SOURCESYSTEM = "SOURCESYSTEM";
    public static final String SETTLEMENTTYPE = "SETTLEMENTTYPE";
    public static final String INSTRUMENT_ID = "INSTRUMENT_ID";
    public static final String INSTRUMENT = "INSTRUMENT";
    public static final String INSTRTYPE = "INSTRTYPE";
    public static final String INSTRUMENTDESC = "INSTRUMENTDESC";
    public static final String DIRECTION = "DIRECTION";
    public static final String COUNTERPARTYCOUNTRY = "COUNTERPARTYCOUNTRY";
    public static final String FIXINGDATE = "FIXINGDATE";
    public static final String CANCELDATE = "CANCELDATE";
    public static final String STRATEGY_ID = "STRATEGY_ID";
    public static final String CURRENCYBASE = "CURRENCYBASE";
    public static final String PRINCIPAL = "PRINCIPAL";
    public static final String RATE = "RATE";
    public static final String BROKERCODE = "BROKERCODE";
    public static final String BROKERDESC = "BROKERDESC";
    public static final String OBSERVATIONSEVENT = "OBSERVATIONSEVENT";
    public static final String FECINIFL = "FECINIFL";
    public static final String INTERESTTYPE = "INTERESTTYPE";
    public static final String SPREAD = "SPREAD";
    public static final String BONDTYPE = "BONDTYPE";
    public static final String DELIVERYTYPE = "DELIVERYTYPE";
    public static final String DEBTTYPE = "DEBTTYPE";
    public static final String STATUSDEAL = "STATUSDEAL";
    public static final String ISSUERCODE = "ISSUERCODE";
    public static final String ISSUERID = "ISSUERID";
    public static final String ISSUERSECTOR = "ISSUERSECTOR";
    public static final String ISSUERCOUNTRY = "ISSUERCOUNTRY";
    public static final String SETTLEMENTFLOW = "SETTLEMENTFLOW";
    public static final String CRTYPE = "CRTYPE";
    public static final String VALPRLIM = "VALPRLIM";
    public static final String VALCPCOR = "VALCPCOR";
    public static final String CDMETLIQ = "CDMETLIQ";
    public static final String NUDIASPL = "NUDIASPL";
    public static final String IMPINTCN = "IMPINTCN";
    public static final String TIP_PER = "TIP_PER";
    public static final String CODPEOR = "CODPEOR";
    public static final String INDICADORDESGLOSE = "INDICADORDESGLOSE";
    public static final String GLSGESTORA = "GLSGESTORA";
    public static final String TOTALINTERESTACCRUAL = "TOTALINTERESTACCRUAL";
    public static final String INTERESTCASH = "INTERESTCASH";
    public static final String IMFORWARDAMOUNT = "IMFORWARDAMOUNT";
    public static final String NPV = "NPV";
    public static final String DAILYNPV1 = "DAILYNPV1";
    public static final String MARKETCLEANPRICE = "MARKETCLEANPRICE";
    public static final String MARKETYESTERDAYCLEANPRICE = "MARKETYESTERDAYCLEANPRICE";
    public static final String MARKETDIRTYPRICE = "MARKETDIRTYPRICE";
    public static final String MARKETYESTERDAYDIRTYPRICE = "MARKETYESTERDAYDIRTYPRICE";
    public static final String LEG_COMPOUNDING_FREQUENCY = "LEG_COMPOUNDING_FREQUENCY";
    public static final String INTERESTFREQUENCYFIXATION = "INTERESTFREQUENCYFIXATION";
    public static final String LEG_INCOME_PAYMENT_TYPE = "LEG_INCOME_PAYMENT_TYPE";
    public static final String COUPONTYPE = "COUPONTYPE";
    public static final String NEXTINTERESTSETTLEMENTDATE = "NEXTINTERESTSETTLEMENTDATE";
    public static final String BOINDICATOR = "BOINDICATOR";
    public static final String MARKETVALUEMAN = "MARKETVALUEMAN";
    public static final String FILLER1 = "FILLER1";
    public static final String FILLER2 = "FILLER2";
    public static final String FILLER3 = "FILLER3";
    public static final String FILLER4 = "FILLER4";
    public static final String FILLER5 = "FILLER5";
    public static final String ACCOUNTINGLAG = "ACCOUNTINGLAG";
    public static final String NEXTAMORTDATE = "NEXTAMORTDATE";

    public static final String LE_ATTR_SECTORCONTABLE = "SECTORCONTABLE";
    public static final String TRADE_KWRD_DELIVERY_TYPE = "DeliveryType";
    public static final String TRADE_KWRD_PARTENON_ACCOUNTING_ID = "PartenonAccountingID";
    public static final String TRADE_KWRD_MUREX_ROOT_CONTRACT = "MurexRootContract";
    public static final String TRADE_KWRD_OLD_PARTENON_ACCOUNTING_ID = "OldPartenonAccountingID";
    public static final String TRADE_KWRD_BOND_FORWARD = "BondForward";
    public static final String TRADE_KWRD_BOND_FORWARD_TYPE = "BondForwardType";
    public static final String TRADE_KWRD_BOND_FORWARD_FIXING_DATE = "BF_FixingDate";
    public static final String TRADE_KWRD_MX_LAST_EVENT = "MxLastEvent";
    public static final String TRADE_KWRD_MX_GLOBAL_ID = "Mx Global ID";
    public static final String DIRTY_PRICE_TODAY = "DirtyPriceToday";
    public static final String DIRTY_PRICE_YESTERDAY = "DirtyPriceYesterday";
    public static final String CLEAN_PRICE_TODAY = "CleanPriceToday";
    public static final String CLEAN_PRICE_YESTERDAY = "CleanPriceYesterday";
    public static final String OFFICIAL_ACCOUNTING = "OFFICIAL_ACCOUNTING";
    public static final long serialVersionUID = 1L;
    public static final String LOG_CATEGORY = "BondMisPlusReportStyle";

    // special format columns
    public static final String YIELD = "Yield";
    public static final String CLEANPRICE = "Trade Price";
    public static final String NegotiatedPrice = "Negotiated Price";
    public static final String COUPON = "Coupon";
    public static final String REF_INTERNA = "PRODUCT_CODE.REF_INTERNA";
    public static final String TITLES = "Quantity";

    // News fields CTM & MCCY
    public static final String STATUSCTMCCY = "STATUSCTMCCY";
    public static final String ALIASSUSICOUNTERPARTY = "ALIASSUSICOUNTERPARTY";
    public static final String DAYS = "DAYS";
    public static final String ISSUERCODECTMMCCY = "ISSUERCODECTMMCCY";
    public static final String ISSUERIDCTMMCCY = "ISSUERIDCTMMCCY";
    public static final String ISSUERNAME = "ISSUERNAME";
    public static final String ISSUERSECTOR2 = "ISSUERSECTOR2";
    public static final String VALPRLIF = "VALPRLIF";
    public static final String VALPRSUF = "VALPRSUF";
    public static final String TASOPERA = "TASOPERA";
    public static final String TIPINTR = "TIPINTR";
    public static final String BICPAIS1 = "BICPAIS1";
    public static final String CODCTACU = "CODCTACU";
    public static final String INTRIPTY = "INTRIPTY";
    public static final String IDEMPRM = "IDEMPRM";
    public static final String IDCENTM = "IDCENTM";
    public static final String CODPRODM = "CODPRODM";
    public static final String CDOPERBM = "CDOPERBM";
    public static final String CODSPROM = "CODSPROM";
    public static final String INTERESTACCRUAL = "INTERESTACCRUAL";
    public static final String PROVISIONMODELICA = "PROVISIONMODELICA";
    public static final String PROVISIONCALC = "PROVISIONCALC";
    public static final String STRUCTURE = "STRUCTURE";
    public static final String COUPONINDEX = "COUPONINDEX";
    public static final String CCREFER = "CCREFER";
    public static final String CONTRACTORIGIN = "CONTRACTORIGIN";
    public static final String FILLER6 = "FILLER6";
    public static final String FILLER7 = "FILLER7";
    public static final String FILLER8 = "FILLER8";
    public static final String FILLER9 = "FILLER9";
    public static final String FILLER10 = "FILLER10";
    public static final String INTERNAL_ID = "INTERNAL_ID";

    public static final String IS_FX_MULTI_CCY = "IS_FX_MULTI_CCY";
    public static final String PRE_MX3 = "Mx3-";
    private final FIFlowTradeSecurityHandler securityWrapper = new FIFlowTradeSecurityHandler();
    public static final ArrayList<String> emptyColumns = new ArrayList<>();

    static {
        emptyColumns.add(FILLER1);
        emptyColumns.add(FILLER2);
        emptyColumns.add(FILLER3);
        emptyColumns.add(FILLER4);
        emptyColumns.add(FILLER5);
        emptyColumns.add(FILLER6);
        emptyColumns.add(FILLER7);
        emptyColumns.add(FILLER8);
        emptyColumns.add(FILLER9);
        emptyColumns.add(FILLER10);
        emptyColumns.add(ALIASSUSICOUNTERPARTY);
        emptyColumns.add(ISSUERNAME);
        emptyColumns.add(ISSUERSECTOR2);
        emptyColumns.add(BICPAIS1);
        emptyColumns.add(CODCTACU);
    }


    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

        if (emptyColumns.contains(columnName)) {
            return "";
        }

        final Trade trade = row.getProperty(ReportRow.TRADE);
        final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
        String allocatedFromkw = trade.getKeywordValue("AllocatedFrom");

        if (trade != null) {
            this.securityWrapper.initRelatedSecutityData(trade.getProduct());
            this.securityWrapper.initRelatedSecPricesData(getPreviousBusinessDay(valDate));
        }

        if (trade.getProduct() instanceof Bond) {

            final Bond bond = (Bond) trade.getProduct();

            if (columnName.equals(INTERNALREF) || columnName.equals(REF_INTERNA)) {
                return super.getColumnValue(row, columnName, errors);
            }
            if (columnName.equals(NegotiatedPrice)){
                return formatResult((super.getColumnValue(row, columnName, errors)));
            }
            if (columnName.equals(CLEANPRICE)){
                return formatResult(((DisplayValue) super.getColumnValue(row, columnName, errors)).get()*100);
            }

            if (columnName.equals(YIELD) || columnName.equals(COUPON)){
                return ((DisplayValue) super.getColumnValue(row, columnName, errors)).get()*100;
            }

            if (columnName.equals(TITLES)){
                return formatResult(Math.abs(((DisplayValue) super.getColumnValue(row, columnName, errors)).get()));
            }

            if (columnName.equals(ORIGIN)) {
                return "800018693";
            }

            if (columnName.equals(PROCESSDATE)) {
                return formatDate(valDate);
            }

            if (columnName.equals(ENTITY)) {
                String partenonId = trade.getKeywordValue(TRADE_KWRD_PARTENON_ACCOUNTING_ID);
                return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(0, 4) : "";
            }

            if (columnName.equals(ACCOUNTING_CENTER)) {
                String partenonId = trade.getKeywordValue(TRADE_KWRD_PARTENON_ACCOUNTING_ID);
                return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(4, 8) : "";
            }

            if (columnName.equals(STATUS)) {
                if (Status.S_CANCELED.getStatus().equalsIgnoreCase(trade.getStatus().getStatus())){
                    return "CANCELED";
                }
                else if(valDate.gte(trade.getTradeDate().getJDate(TimeZone.getDefault())) && trade.getSettleDate().gte(valDate)) {
                    //REVISAR CAMBIO
                    return "LIVE";
                }
                else if (valDate.gte(trade.getSettleDate())){
                    return "MATURED";
                }
                return "";
            }

            if (columnName.equals(STATUSCTMCCY)) {
                if (Status.S_CANCELED.getStatus().equalsIgnoreCase(trade.getStatus().getStatus())){
                    return "ANULADA";
                }
                else if(valDate.gte(trade.getTradeDate().getJDate(TimeZone.getDefault())) && trade.getSettleDate().gte(valDate)) {
                    return "VIVA";
                }
                else if (valDate.gte(trade.getSettleDate())){
                    return "VENCIDA";
                }
                return "";
            }

            if (columnName.equals(SOURCESYSTEM)) {
                return "MUREX FXFI";
            }

            if (columnName.equals(SETTLEMENTTYPE)) {
                String deliveryType = trade.getKeywordValue(TRADE_KWRD_DELIVERY_TYPE);
                if(!Util.isEmpty(deliveryType) && "DAP".equals(deliveryType)) {
                    return "CP";
                }
                else if(!Util.isEmpty(deliveryType) && "DFP".equals(deliveryType)) {
                    return "LP";
                }
                return "";
            }

            if (columnName.equals(INSTRUMENT_ID)) {
                String partenonId = trade.getKeywordValue(TRADE_KWRD_PARTENON_ACCOUNTING_ID);
                return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(8, 11) : "";
            }

            if (columnName.equals(INSTRUMENT)) {
                return "BONOS";
            }

            if (columnName.equals(INSTRTYPE)) {
                String partenonId = trade.getKeywordValue(TRADE_KWRD_PARTENON_ACCOUNTING_ID);
                return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(18, 21) : "";
            }

            if (columnName.equals(INSTRUMENTDESC)) {
                return StringUtils.remove(bond.getComment(), '|');
            }

            if (columnName.equals(DIRECTION)) {
                int buySell = bond.getBuySell(trade);
                return buySell==1 ? "COMPRA" : "VENTA";
            }

            if (columnName.equals(COUNTERPARTYCOUNTRY)) {
                if(trade.getCounterParty().getCountry()!=null) {
                    Country country =BOCache.getCountry(DSConnection.getDefault(), trade.getCounterParty().getCountry());
                    return country!=null ? country.getISOCode(): "";
                }
                return "";
            }

            if (columnName.equals(FIXINGDATE)) {
                String bondFwrd = trade.getKeywordValue(TRADE_KWRD_BOND_FORWARD);
                String bondFwrdType = trade.getKeywordValue(TRADE_KWRD_BOND_FORWARD_TYPE);
                String fixingDate = trade.getKeywordValue(TRADE_KWRD_BOND_FORWARD_FIXING_DATE);
                return (!Util.isEmpty(bondFwrd) && "true".equalsIgnoreCase(bondFwrd) && (!Util.isEmpty(bondFwrdType)) && !bondFwrdType.equalsIgnoreCase("Delivery") && !Util.isEmpty(fixingDate) && fixingDate.length()==8) ?
                        fixingDate.substring(0,4) + "-" + fixingDate.substring(4,6) + "-" + fixingDate.substring(6,8) : "";
            }

            if (columnName.equals(CANCELDATE)) {
                JDate cancellationDate = BOCreUtils.getInstance().getCancelationDateFromTradeAudit(trade);
                return cancellationDate != null ? formatDate(cancellationDate) : "";
            }

            if (columnName.equals(STRATEGY_ID)) {
                Book book = trade.getBook();
                if(book!=null) {
                    AccountingBook acctBook = book.getAccountingBook();
                    if(acctBook != null) {
                        String acctBookName = acctBook.getName();
                        if(acctBookName.equals("Negociacion")) {
                            return "NE";
                        }
                        else if(acctBookName.equals("Inversion crediticia")) {
                            return "IC";
                        }
                        else if(acctBookName.equals("Inversion a vencimiento")) {
                            return "IV";
                        }
                        else if(acctBookName.equals("Disponible para la venta")) {
                            return "DV";
                        }
                        else if(acctBookName.equals("Designados a valor razonable")) {
                            return "DVR";
                        }
                    }
                }
                return "NEG";
            }

            if (columnName.equals(CURRENCYBASE)) {
                if(isFXRow(row)){
                    return trade.getSettleCurrency();
                }
                return trade.getTradeCurrency();
             }

            if (columnName.equals(PRINCIPAL)) {
                return formatResult(trade.computeNominal(JDate.valueOf(trade.getTradeDate())));
            }

            if (columnName.equals(RATE)) {
                if(bond.getFixedB()){
                    return bond.getCoupon()*100;
                }
                else{
                    Double rateindex = getRateIndexQuote(bond.getRateIndex(), valDate);
                    Double spread = bond.getRateIndexSpread();
                    return (rateindex > 0.0) ? (rateindex*100 + spread*100) : "";
                }
            }

            if (columnName.equals(BROKERCODE)) {
                Vector<LegalEntity> brokers = TradeRoleFinder.getTradeRoles("Broker", trade, new Vector(), DSConnection.getDefault());
                return (brokers != null && brokers.size() > 0) ? brokers.get(0).getCode() : "";
            }

            if (columnName.equals(BROKERDESC)) {
                Vector<LegalEntity> brokers = TradeRoleFinder.getTradeRoles("Broker", trade, new Vector(), DSConnection.getDefault());
                return (brokers != null && brokers.size() > 0) ? brokers.get(0).getName() : "";
            }

            if (columnName.equals(OBSERVATIONSEVENT)) {
                int buySell = bond.getBuySell(trade);
                return buySell==1 ? "COMPRA" : "VENTA";
            }

            if (columnName.equals(INTERESTTYPE)) {
                return bond.getFixedB() ? "F" : "V";
            }

            if (columnName.equals(SPREAD)) {
                return !bond.getFixedB() ? bond.getRateIndexSpread() * 100 : "";
            }

            if (columnName.equals(BONDTYPE)) {
                String bondFwrd = trade.getKeywordValue(TRADE_KWRD_BOND_FORWARD);
                return (!Util.isEmpty(bondFwrd) && "true".equalsIgnoreCase(bondFwrd)) ? "Bond Forward" : "Bond Spot";
            }

            if (columnName.equals(DAYS)) {
                String bondFwrd = trade.getKeywordValue(TRADE_KWRD_BOND_FORWARD);
                return (!Util.isEmpty(bondFwrd) && "true".equalsIgnoreCase(bondFwrd)) ? "1" : "0";
            }

            if (columnName.equals(DELIVERYTYPE)) {
                String bondFwrd = trade.getKeywordValue(TRADE_KWRD_BOND_FORWARD);
                if (!Util.isEmpty(bondFwrd) && "true".equalsIgnoreCase(bondFwrd)) {
                    String bondFwrdType = trade.getKeywordValue(TRADE_KWRD_BOND_FORWARD_TYPE);
                    return !Util.isEmpty(bondFwrdType) ? bondFwrdType : "";
                }
                else {
                    return "Delivery";
                }
            }

            if (columnName.equals(DEBTTYPE)) {
                String secCodeIssueType = bond.getSecCode("ISSUE_TYPE");
                if (!Util.isEmpty(secCodeIssueType)){
                    if (secCodeIssueType.equalsIgnoreCase("BO")){
                        return "DEUDA PUBLICA";
                    }
                    if (secCodeIssueType.equalsIgnoreCase("LT")){
                        return "LETRAS";
                    }
                    if (secCodeIssueType.equalsIgnoreCase("PG")){
                        return "PAGARES";
                    }
                }
                String secCodeIsCovered = bond.getSecCode("IS COVERED");
                //IS COVERED=Y AND (ISIN=ES* OR [ISIN=XS* "Y" COUNTRY=SPAIN])
                if (!Util.isEmpty(secCodeIsCovered) &
                        "Y".equalsIgnoreCase(secCodeIsCovered) &
                        (bond.getSecCode(ISIN).startsWith("ES") || (bond.getSecCode(ISIN).startsWith("XS") && bond.getCountry().equalsIgnoreCase("SPAIN")))){
                    return "OAF";
                }


                return "";
            }

            if (columnName.equals(STATUSDEAL)) {
                return valDateTime.before(trade.getSettleDatetime()) ? "COMPROMISO" : "VALOR";
            }

            if (columnName.equals(ISSUERCODE)) {
                if(bond.getIssuerId() > 0) {
                    LegalEntity issuer = null;
                    try {
                        issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                        if (issuer != null) {
                            return issuer.getExternalRef();
                        }
                    } catch (CalypsoServiceException e) {
                        Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                    }
                }
                return "";
            }

            if (columnName.equals(ISSUERCODECTMMCCY)) {
                if(bond.getIssuerId() > 0) {
                    LegalEntity issuer = null;
                    try {
                        issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                        if (issuer != null) {
                            return !Util.isEmpty(issuer.getExternalRef()) ? issuer.getExternalRef().substring(1) : "";
                        }
                    } catch (CalypsoServiceException e) {
                        Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                    }
                }
                return "";
            }

            if (columnName.equals(ISSUERIDCTMMCCY)) {
                if(bond.getIssuerId() > 0) {
                    LegalEntity issuer = null;
                    try {
                        issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                        if (issuer != null) {
                            return !Util.isEmpty(issuer.getExternalRef()) ? issuer.getExternalRef().substring(0,1) : "";
                        }
                    } catch (CalypsoServiceException e) {
                        Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                    }
                }
                return "";
            }

            if (columnName.equals(ISSUERID)) {
                if(bond.getIssuerId() > 0) {
                    LegalEntity issuer = null;
                    try {
                        issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                        if (issuer != null) {
                            return issuer.getId();
                        }
                    } catch (CalypsoServiceException e) {
                        Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                    }
                }
                return "";
            }

            if (columnName.equals(ISSUERSECTOR)) {
                if(bond.getIssuerId() > 0) {
                    LegalEntity issuer = null;
                    try {
                        issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                        if (issuer != null) {
                            final LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, issuer.getId(), "ALL", LE_ATTR_SECTORCONTABLE);
                            return attr!=null ? attr.getAttributeValue() : "";
                        }
                    } catch (CalypsoServiceException e) {
                        Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                    }
                }
                return "";
            }

            if (columnName.equals(ISSUERCOUNTRY)) {
                if(bond.getIssuerId() > 0) {
                    LegalEntity issuer = null;
                    try {
                        issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                        if (issuer != null) {
                            Country country = BOCache.getCountry(DSConnection.getDefault(), issuer.getCountry());
                            return country!=null ? country.getISOCode(): "";
                        }
                    } catch(CalypsoServiceException e){
                        Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                    }
                }
                return "";
            }

            if (columnName.equals(SETTLEMENTFLOW)) {
                if (Status.S_CANCELED.getStatus().equalsIgnoreCase(trade.getStatus().getStatus())){
                    return "BAJA";
                }
                else {
                    try {
                        TransferArray transferArray = DSConnection.getDefault().getRemoteBO().getBOTransfers(trade.getLongId());
                        if (transferArray != null && transferArray.size() > 0){
                            for (int i = 0; i < transferArray.size(); i++) {
                                BOTransfer xfer = transferArray.get(i);
                                if (Status.S_SETTLED.getStatus().equalsIgnoreCase(xfer.getStatus().getStatus())) {
                                    return "LIQUIDADA";
                                }
                            }
                        }
                        return "NO LIQUIDADA";
                    }
                    catch (CalypsoServiceException e) {
                        Log.error(this,"Error loading Trade: " + trade.getLongId());
                    }
                }
                return "";
            }

            if (columnName.equals(CRTYPE)) {
                if (("mxContractEventICANCEL_REISSUE").equalsIgnoreCase(trade.getKeywordValue(TRADE_KWRD_MX_LAST_EVENT))){
                    if (!Util.isEmpty(trade.getKeywordValue(TRADE_KWRD_OLD_PARTENON_ACCOUNTING_ID))){
                        return "CANCEL&REISSUE CONTABLE";
                    }
                    return "CANCEL&REISSUE";
                }
                return "";
            }

            if (columnName.equals(VALPRLIM)) {
                return formatResult(Optional.of(trade)
                        .map(Trade::getTradePrice)
                        .map(this.securityWrapper::getSecurityPriceDisplayValue)
                        .map(this::adjustPriceBase)
                        .orElse(0.0D));
            }

            if (columnName.equals(VALCPCOR)) {
                double pricerMeasureAmt = 0.0D;
                try {
                    PricerBond pricer = (PricerBond) PricingEnv.loadPE(OFFICIAL_ACCOUNTING, valDateTime).getPricerConfig().getPricerInstance(trade.getProduct());
                    pricerMeasureAmt = pricer.accrual((Bond) trade.getProduct(), valDate, PricingEnv.loadPE(OFFICIAL_ACCOUNTING, valDateTime));
                    return formatResult(pricerMeasureAmt*100);
                } catch(PricerException pException){
                    Log.error(this, "Missing Quote");
                }catch (Exception ex) {
                    Log.error(this, "Could not retrieve ACCRUAL for trade id " + trade.getLongId());
                }
                return pricerMeasureAmt;
            }

            if (columnName.equals(CDMETLIQ)) {
                Vector<TradeTransferRule> trs = trade.getTransferRules();
                if (trs == null) {
                    Vector errorMsgs = new Vector<String>();
                    trs = BOProductHandler.buildTransferRules(trade, errorMsgs, DSConnection.getDefault(), trade.isArchived());
                }
                if (trs == null) {
                    return "";
                }
                for (TradeTransferRule tr : trs) {
                    if (tr.getTransferType().equals("PRINCIPAL") && tr.getSettlementMethod().equals("Direct")) {
                        return "CUENTA";
                    }
                }
                return "";
            }

            if (columnName.equals(NUDIASPL)) {
                return Math.abs(JDate.diff(trade.getTradeDate().getJDate(TimeZone.getDefault()), trade.getSettleDate()));
            }

            if (columnName.equals(IMPINTCN)) {
                return "0";
            }

            if (columnName.equals(TIP_PER)) {
                String j_minorista = row.getProperty("J_MINORISTA");
                return !Util.isEmpty(j_minorista) ? j_minorista.substring(0,1) : "";
            }

            if (columnName.equals(CODPEOR)) {
                String j_minorista = row.getProperty("J_MINORISTA");
                return !Util.isEmpty(j_minorista) ? j_minorista.substring(1) : "";
            }

            if (columnName.equals(INDICADORDESGLOSE)) {
                if (!Util.isEmpty(allocatedFromkw)){
                    return "HIJA";
                }
                TradeArray tradeArray = getTradeByKeyword("AllocatedFrom", String.valueOf(trade.getLongId()));
                return tradeArray!=null && tradeArray.size()>0 ? "MADRE" : "";
            }

            /** Reunion 03/07/23. se quita informar este campo
            if (!Util.isEmpty(allocatedFromkw)){
                try {
                    Trade tradeMadre = DSConnection.getDefault().getRemoteTrade().getTrade(Long.parseLong(allocatedFromkw));
                    if (tradeMadre!=null){
                        return tradeMadre.getKeywordValue(TRADE_KWRD_PARTENON_ACCOUNTING_ID);
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Could not get Partenon Accounting Id from Trade 'madre': " + allocatedFromkw);
                    return "";
                }
            }*/
            if (columnName.equals(IDEMPRM)) {
                return "";
            }

            if (columnName.equals(IDCENTM)) {
                return "";
            }

            if (columnName.equals(CODPRODM)) {
                return "";
            }

            if (columnName.equals(CDOPERBM)) {
                return "";
            }

            if (columnName.equals(CODSPROM)) {
                return "";
            }

            if (columnName.equals(GLSGESTORA)) {
                if (!Util.isEmpty(allocatedFromkw)){
                    try {
                        Trade tradeMadre = DSConnection.getDefault().getRemoteTrade().getTrade(Long.parseLong(allocatedFromkw));
                        if (tradeMadre!=null){
                            return tradeMadre.getCounterParty().getCode();
                        }
                    } catch (CalypsoServiceException e) {
                        Log.error(this, "Could not get GLS from Trade 'madre': " + allocatedFromkw);
                        return "";
                    }
                }
                return "";
            }

            if (columnName.equals(TOTALINTERESTACCRUAL)) {
                return "";  //formatResult(calculatePM(valDateTime, trade, "ACCUMULATED_ACCRUAL", OFFICIAL_ACCOUNTING));
            }

            if (columnName.equals(INTERESTCASH)) {
                String deliveryType = trade.getKeywordValue(TRADE_KWRD_DELIVERY_TYPE);
                if(!Util.isEmpty(deliveryType) && "DAP".equals(deliveryType)) {
                    return 0;
                }
                else if(!Util.isEmpty(deliveryType) && "DFP".equals(deliveryType)) {
                    double settlementAmount = calculatePM(valDateTime, trade, "SETTLEMENT_AMOUNT", OFFICIAL_ACCOUNTING);
                    return formatResult(settlementAmount);
                }
                return 0;
            }

            if (columnName.equals(IMFORWARDAMOUNT)) {
                String deliveryType = trade.getKeywordValue(TRADE_KWRD_DELIVERY_TYPE);
                if(!Util.isEmpty(deliveryType) && "DFP".equals(deliveryType)){
                    if (!Util.isEmpty(trade.getKeywordValue("SettlementAmount"))){
                        double settlementAmount = calculatePM(valDateTime, trade, "SETTLEMENT_AMOUNT", OFFICIAL_ACCOUNTING);
                        return formatResult(settlementAmount *2);

                    }
                }
                return 0;
            }

            if (columnName.equals(NPV)) {
                return "0";
            }


            if (columnName.equals(DAILYNPV1)) {
                JDate valDateD1 = valDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
                PLMark plMarkValueNPVD = getPLMarkValue(PricingEnv.loadPE(OFFICIAL_ACCOUNTING, valDateD1.getJDatetime()), trade, valDateD1);
                return formatResult(getPLMark(plMarkValueNPVD, "MTM_NET_MUREX"));
            }

            if (columnName.equals(MARKETCLEANPRICE)) {
                double cleanPrice = row.getProperty(CLEAN_PRICE_TODAY);
                return formatResult(cleanPrice);
            }

            if (columnName.equals(MARKETYESTERDAYCLEANPRICE)) {
                double yesterdayCleanPrice = row.getProperty(CLEAN_PRICE_YESTERDAY);
                return formatResult(yesterdayCleanPrice);
            }

            if (columnName.equals(MARKETDIRTYPRICE)) {
                double dirtyPrice = row.getProperty(DIRTY_PRICE_TODAY);
                return formatResult(dirtyPrice);
            }

            if (columnName.equals(MARKETYESTERDAYDIRTYPRICE)) {
                double yesterdayDirtyPrice = row.getProperty(DIRTY_PRICE_YESTERDAY);
                return  formatResult(yesterdayDirtyPrice);
            }

            if (columnName.equals(LEG_COMPOUNDING_FREQUENCY)) {
                Frequency cmpFreq = bond.getCompoundFrequency();
                if (cmpFreq != null && !cmpFreq.equals(Frequency.F_NONE)) {
                    return bond.getCompoundFrequency();
                }
                return "";
            }

            if (columnName.equals(INTERESTFREQUENCYFIXATION)) {
                return bond.getCouponFrequency();
            }

            if (columnName.equals(LEG_INCOME_PAYMENT_TYPE)) {
                if (!bond.getFixedB() && bond.getResetTiming().equals(PeriodTiming.S_BEGIN_PERIOD)) {
                    return "POST";
                }
                return "PRE";
            }

            if (columnName.equals(COUPONTYPE)) {
                Frequency cmpFreq = bond.getCompoundFrequency();
                if (cmpFreq != null && !cmpFreq.equals(Frequency.F_NONE)) {
                    return "C";
                }
                return "S";

            }

            if (columnName.equals(NEXTINTERESTSETTLEMENTDATE)) {
                CashFlow cashFlow = getCurrentCashFlow(bond, valDate);
                if (cashFlow != null) {
                    return formatDate(cashFlow.getDate());
                }
                return "";
            }

            if (columnName.equals(BOINDICATOR)) {
                final Object isInternalDeal = super.getColumnValue(row, IS_INTERNAL_DEAL, errors);
                if (Util.isTrue(isInternalDeal, false)) {
                    return "INTERNA";
                }
                return "MERCADO";
            }

            if (columnName.equals(STRUCTURE)) {
                if(!BondMisPlusReport.isDualCcy(trade)){
                    return "";
                }
                if(isFXRow(row)){
                    return "FX";
                }
                return "BONO DUAL";
            }

            if (columnName.equals(FECINIFL)) {
                CashFlow cashFlow = getCurrentCashFlow(bond, valDate);
                return cashFlow !=null ? formatDate(cashFlow.getStartDate()) : "";
            }

            if (columnName.equals(MARKETVALUEMAN)) {
                Double titles = trade.getQuantity();
                Double faceValue = bond.getFaceValue();
                double dirtyPrice = row.getProperty(DIRTY_PRICE_TODAY);
                return (dirtyPrice != 0.0) ? formatResult(titles * faceValue * dirtyPrice) : "";
            }

            if (columnName.equals(ACCOUNTINGLAG)) {
                if ("true".equalsIgnoreCase(trade.getKeywordValue("BondForward"))) {
                    return "Delivery".equalsIgnoreCase(trade.getKeywordValue("BondForwardType")) ? JDate.getNow().before(loadForwardDate(trade, bond)) ?"Bond FWD Delivery" : "Bond Spot" : "Bond FWD Cash";
                }
                else
                    return "Bond Spot";
            }

            if (columnName.equals(NEXTAMORTDATE)) {
                JDate nextAmortDate = null;
                Vector<NotionalDate> amortSchedule = (Vector<NotionalDate>)bond.getAmortSchedule();
                if (amortSchedule != null && amortSchedule.size() > 0) {
                    for (int i = 0; i < amortSchedule.size(); i++) {
                        JDate amortDate = amortSchedule.get(i).getStartDate();
                        if(amortDate.after(JDate.getNow())){
                            if(nextAmortDate == null){
                                nextAmortDate = amortDate;
                            }
                            else if(amortDate.before(nextAmortDate)){
                                nextAmortDate = amortDate;
                            }
                        }
                    }
                    if(nextAmortDate != null) {
                        return formatDate(nextAmortDate);
                    }
                }
                return "";
            }

            if (columnName.equals(VALPRLIF)) {
                return "0";
            }

            if (columnName.equals(VALPRSUF)) {
                return "0";
            }

            if (columnName.equals(TASOPERA)) {
                return "0";
            }

            if (columnName.equals(TIPINTR)) {
                return "0";
            }

            if (columnName.equals(INTRIPTY)) {
                return "0";
            }

            if (columnName.equals(INTERESTACCRUAL)) {
                return "0";
            }

            if (columnName.equals(PROVISIONMODELICA)) {
                return "0";
            }

            if (columnName.equals(PROVISIONCALC)) {
                return "0";
            }

            if (columnName.equals(COUPONINDEX)) {
                return "";
            }

            if (columnName.equals(CCREFER)) {
                return bond.getSecCode("REF_INTERNA");
            }

            if (columnName.equals(CONTRACTORIGIN)) {
                if (!Util.isEmpty(allocatedFromkw)){
                    Trade madre = getTradeMadre(allocatedFromkw);
                    return madre.getKeywordValue(TRADE_KWRD_MUREX_ROOT_CONTRACT);
                }
                return trade.getKeywordValue(TRADE_KWRD_MUREX_ROOT_CONTRACT);
            }

            if (columnName.equals(INTERNAL_ID)) {
                if (Util.isEmpty(trade.getKeywordValue(TRADE_KWRD_MX_GLOBAL_ID))){
                    if (!Util.isEmpty(allocatedFromkw)) {
                        Trade madre = getTradeMadre(allocatedFromkw);
                        if (Util.isEmpty(madre.getKeywordValue(TRADE_KWRD_MX_GLOBAL_ID))) {
                            return PRE_MX3 + madre.getKeywordValue(TRADE_KWRD_MUREX_ROOT_CONTRACT);
                        }
                        return madre.getKeywordValue(TRADE_KWRD_MX_GLOBAL_ID);
                    }
                    return PRE_MX3 + trade.getKeywordValue(TRADE_KWRD_MUREX_ROOT_CONTRACT);
                }
                return trade.getKeywordValue(TRADE_KWRD_MX_GLOBAL_ID);
            }

        }
        return formatResult(super.getColumnValue(row, columnName, errors));
    }


    private String formatDate(JDate jDate){
        String date = "";
        if (jDate != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            date = format.format(jDate.getDate());
        }
        return date;
    }


    private PLMark getPLMarkValue(PricingEnv pricingEnv, Trade trade, JDate date) {
        PLMark plMark = null;
        try {
            plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(), pricingEnv.getName(), date);
            return plMark;
        } catch (RemoteException e) {
            Log.error(this, e);
            return null;

        }
    }


    private Double getPLMark(PLMark plMark, String type){
        return null!=plMark && null!=plMark.getPLMarkValueByName(type) ? plMark.getPLMarkValueByName(type).getMarkValue() : 0.0D;
    }


    public static Object formatResult(Object o) {
        return UtilReport.formatResult(o, '.');
    }


    @SuppressWarnings("rawtypes")
    public String getKeywordValue(ReportRow row, String keyword, Vector errors) {
        final Object value = super.getColumnValue(row, TRADE_KEYWORD_PREFIX + keyword, errors);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public Double getRateIndexQuote(RateIndex rateIndex, JDate valdate){
        String quoteName = rateIndex.getQuoteName();
        QuoteValue value = new QuoteValue();
        value.setQuoteSetName("OFFICIAL");
        value.setName(quoteName);
        value.setQuoteType("Yield");
        value.setDate(valdate);
        try {
            final QuoteValue quoteValue = DSConnection.getDefault().getRemoteMarketData().getQuoteValue(value);
            if(null!=quoteValue){
                return quoteValue.getClose();
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading QuoteValue for: " + quoteName + ": " + e);
        }
        return 0.0;
    }


    public CashFlow getCurrentCashFlow(Bond bond, JDate valDate) {
        try {
            CashFlowSet cashFlows = bond.getFlows(valDate);
            if (cashFlows != null && !cashFlows.isEmpty()) {
                CashFlow cashFlow = (CashFlow) cashFlows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
                if(cashFlow == null){
                    cashFlow = (CashFlow) cashFlows.findEnclosingCashFlow(JDate.getNow(), CashFlow.PRINCIPAL);
                }
                return cashFlow;
            }
        } catch (FlowGenerationException e) {
            Log.error(this, "Could not retrive Flows for Product " + bond.getId());
            return null;
        }
        return null;
    }


    public Double calculatePM(JDatetime valDatetime, Trade trade, String pm, String pEnv) {

        double pricerMeasureAmt = 0.0D;
        PricingEnv pricingEnv = PricingEnv.loadPE(pEnv, valDatetime);
        Pricer pricer = pricingEnv.getPricerConfig().getPricerInstance(trade.getProduct());
        PricerMeasure measure = PricerMeasureUtility.makeMeasure(pm);
        try {
            pricer.price(trade, valDatetime, pricingEnv, new PricerMeasure[]{measure});
            pricerMeasureAmt = Optional.ofNullable(measure).map(PricerMeasure::getValue).orElse(0.0D);

            return pricerMeasureAmt;
        } catch (Exception ex) {
            Log.error(this, "Could not retrieve Pricer Measure " + pm);
        }
        return pricerMeasureAmt;
    }


    public TradeArray getTradeByKeyword(String keywordName, String keywordValue) {
        TradeArray existingTrades = null;
        String fromClause = "trade, trade_keyword kwd";
        String whereClause = "trade.trade_id=kwd.trade_id and kwd.keyword_name='" + keywordName + "' and kwd.keyword_value='" + keywordValue + "'";
        try {
            existingTrades = DSConnection.getDefault().getRemoteTrade().getTrades(fromClause, whereClause,null,null);
        } catch (RemoteException e) {
            Log.error(this, e);
            existingTrades = null;
        }
        return existingTrades;
    }

    /**
     *
     * @param trade
     * @param bond
     * @return
     */
    private JDate loadForwardDate(Trade trade, Bond bond) {
        return trade.getSettleDate().addBusinessDays((-1) * Integer.valueOf(trade.getKeywordValue("BondSettleDays")), bond.getHolidays());
    }

    private boolean isFXRow(ReportRow row ){
        return row.getProperty(IS_FX_MULTI_CCY)!= null &&
                row.getProperty(IS_FX_MULTI_CCY).equals("true");
    }

    private double adjustPriceBase(DisplayValue displayValue) {
        return Optional.ofNullable(displayValue)
                .map(DisplayValue::toString)
                .map(stringValue -> FdnUtilProvider.getNumberFormattingUtil().stringToNumber(stringValue, null, null))
                .orElse(1.00D);
    }

    /**
     * For quote retrieval
     *
     * @return JDate
     */
    private JDate getPreviousBusinessDay(JDate valDate) {
        Vector<String> holidays = Util.string2Vector("SYSTEM");
        return valDate.addBusinessDays(-1, holidays);
    }

    /**
     * Get Trade madre from keyword allocatedFromkw
     *
     * @param allocatedFromkw
     * @return
     */
    public Trade getTradeMadre(String allocatedFromkw) {
        Trade madre = new Trade();
        try {
            madre = DSConnection.getDefault().getRemoteTrade().getTrade(Long.parseLong(allocatedFromkw));
        } catch (CalypsoServiceException e) {
            throw new RuntimeException(e);
        }
        return madre;
    }

}
