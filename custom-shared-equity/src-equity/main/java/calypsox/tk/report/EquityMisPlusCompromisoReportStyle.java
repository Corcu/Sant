package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

public class EquityMisPlusCompromisoReportStyle extends TradeReportStyle {

    public static final String LOG_CATEGORY = "EquityMisPlusCompromisoReportStyle";
    private static final List<String> contactTypes = Arrays.asList("Default", "ALL");

    public static final String ORIGIN = "ORIGIN";
    public static final String PROCESSDATE = "PROCESSDATE";
    public static final String PROCESS_DATE = "PROCESS_DATE";
    public static final String ENTITY = "ENTITY";
    public static final String ACCOUNTING_CENTER = "ACCOUNTING_CENTER";
    public static final String BRANCH = "BRANCH";
    public static final String BRANCH_ID = "BRANCH_ID";
    public static final String DEAL_ID = "DEAL_ID";
    public static final String INTERNALREF = "INTERNALREF";
    public static final String ISIN = "ISIN";
    public static final String ISINDESC = "ISINDESC";
    public static final String STATUS = "STATUS";
    public static final String STATUSDEAL = "STATUSDEAL";
    public static final String FODEALFO = "FODEALFO";
    public static final String CONTRACTORIGIN = "CONTRACTORIGIN";
    public static final String SOURCESYSTEM = "SOURCESYSTEM";
    public static final String INSTRUMENT = "INSTRUMENT";
    public static final String INSTRUMENT_ID = "INSTRUMENT_ID";
    public static final String INSTRTYPE = "INSTRTYPE";
    public static final String DIRECTION = "DIRECTION";
    public static final String COUNTERPARTY_ID = "COUNTERPARTY_ID";
    public static final String GLSCOUNTERPARTY = "GLSCOUNTERPARTY";
    public static final String COUNTERPARTYDESC = "COUNTERPARTYDESC";
    public static final String NIFCOUNTERPARTY = "NIFCOUNTERPARTY";
    public static final String COUNTERPARTYSECTOR = "COUNTERPARTYSECTOR";
    public static final String COUNTERPARTYCOUNTRY = "COUNTERPARTYCOUNTRY";
    public static final String INPUTDATE = "INPUTDATE";
    public static final String TRADEDATE = "TRADEDATE";
    public static final String VALUEDATE = "VALUEDATE";
    public static final String SETTLEDATE = "SETTLEDATE";
    public static final String STRATEGY = "STRATEGY";
    public static final String PORTFOLIO = "PORTFOLIO";
    public static final String FOLDER = "FOLDER";
    public static final String CURRENCY = "CURRENCY";
    public static final String SETTLECURR = "SETTLECURR";
    public static final String QUANTITY = "QUANTITY";
    public static final String NOMINALQUANTITY = "NOMINALQUANTITY";
    public static final String INIITIALNOMINALQUANTITY = "INIITIALNOMINALQUANTITY";
    public static final String SETTLEMENTAMOUNTCOMISIONDIVLIQ = "SETTLEMENTAMOUNTCOMISIONDIVLIQ";
    public static final String SETTLEMENTAMOUNTDIVLIQ = "SETTLEMENTAMOUNTDIVLIQ";
    public static final String SETTLEMENTAMOUNTDIVEQ = "SETTLEMENTAMOUNTDIVEQ";
    public static final String LOTSIZE = "LOTSIZE";
    public static final String PRICEATSTART = "PRICEATSTART";
    public static final String BROKERCODE = "BROKERCODE";
    public static final String BROKERDESC = "BROKERDESC";
    public static final String PARTENONID = "PARTENONID";
    public static final String BOINDICATOR = "BOINDICATOR";
    public static final String ISSUERGLS = "ISSUERGLS";
    public static final String ISSUERCODE = "ISSUERCODE";
    public static final String ISSUERDESC = "ISSUERDESC";
    public static final String ISSUERSECTOR = "ISSUERSECTOR";
    public static final String ISSUERCOUNTRY = "ISSUERCOUNTRY";
    public static final String ISSUERNIF = "ISSUERNIF";
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
    public static final String SETTLEMENTTYPE = "SETTLEMENTTYPE";
    public static final String PRODUCT = "PRODUCT";
    public static final String LEGALENTITY = "LEGALENTITY";
    public static final String SYSTEM_BO = "SYSTEM_BO";
    public static final String TIPOCAMBIO = "TIPOCAMBIO";
    public static final String PRICE = "PRICE";
    public static final String YESTERDAYPRICE = "YESTERDAYPRICE";
    public static final String REVALAMOUNT = "REVALAMOUNT";
    public static final String AVGPRINCIPAL = "AVGPRINCIPAL";
    public static final String MARKETVALUEMAN = "MARKETVALUEMAN";
    public static final String MARKETVALUEACC = "MARKETVALUEACC";
    public static final String HEDGINGTYPE = "HEDGINGTYPE";
    public static final String EQUITY_TYPE = "EQUITY_TYPE";
    public static final String EQUITY_TYPE_DESC = "EQUITY_TYPE_DESC";
    public static final String AUTOCARTERA = "AUTOCARTERA";
    public static final String ADDITIONALFEEFLOW = "ADDITIONALFEEFLOW";
    public static final String COMMON = "COMMON";
    public static final String MARKET = "MARKET";
    public static final String STATUSDEALCOMP = "STATUSDEALCOMP";
    public static final String FO_SOURCE = "FO_SOURCE";
    public static final String IS_SPOT = "IS_SPOT";
    public static final String PRODUCTO_CONCRETO = "PRODUCTO_CONCRETO";
    public static final String FILLER1 = "FILLER1";
    public static final String FILLER2 = "FILLER2";
    public static final String FILLER3 = "FILLER3";
    public static final String FILLER4 = "FILLER4";
    public static final String FILLER5 = "FILLER5";
    //ALM
    public static final String ENTITY_ALM = "ENTITY_ALM";
    public static final String ACCOUNTING_CENTER_ALM = "ACCOUNTING_CENTER_ALM";

    public static final String LE_ATTR_SECTORCONTABLE = "SECTORCONTABLE";
    public static final String LE_ATTR_PARTENON_ACCOUNTING_ID = "PartenonAccountingID";

    public static final ArrayList<String> emptyColumns = new ArrayList<>();
    public static final HashMap<String, String> columnToKeyword = new HashMap<>();
    public static final HashMap<String, String> columnToColumn = new HashMap<>();

    static {
        emptyColumns.add(FILLER2);
        emptyColumns.add(FILLER3);
        emptyColumns.add(FILLER4);
        emptyColumns.add(FILLER5);

        columnToKeyword.put(CONTRACTORIGIN, "MurexRootContract");

        columnToColumn.put(PORTFOLIO, TradeReportStyle.BOOK);
        columnToColumn.put(FOLDER, TradeReportStyle.BOOK);
    }

    public static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

        if (emptyColumns.contains(columnName)) {
            return "";
        }
        if (columnToKeyword.containsKey(columnName)) {
            return getKeywordValue(row, columnToKeyword.get(columnName), errors);
        }
        if (columnToColumn.containsKey(columnName)) {
            return super.getColumnValue(row, columnToColumn.get(columnName), errors);
        }

        final Trade trade = row.getProperty(ReportRow.TRADE);
        final BOTransfer transfer = row.getProperty(ReportRow.TRANSFER);
        final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
        final Product product = trade.getProduct();
        Trade mirrorTrade = null;
        if (trade.getMirrorTradeLongId()!=0) {
            try {
                mirrorTrade = DSConnection.getDefault().getRemoteTrade().getTrade(trade.getMirrorTradeLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Could not get Mirror Trade");
            }
        }


        if (product instanceof Equity) {
            final Equity equity = (Equity) product;

            if (columnName.equals(FODEALFO)){
                String fodealfo = trade.getKeywordValue("Contract ID");
                if (Util.isEmpty(fodealfo) && null!=mirrorTrade) {
                    fodealfo = mirrorTrade.getKeywordValue("Contract ID");
                }


                return fodealfo;
            }

            if (columnName.equals(ORIGIN)) {
                return "800018693";
            }

            if (columnName.equals(PROCESSDATE)) {
                return formatDate(valDate);
            }

            if (columnName.equals(PROCESS_DATE)) {
                return formatDate(valDate,"/");
            }

            if (columnName.equals(ENTITY)) {
                String partenonAccountingID = trade.getKeywordValue(LE_ATTR_PARTENON_ACCOUNTING_ID);
                return (partenonAccountingID != null && partenonAccountingID.length() == 21) ? partenonAccountingID.substring(0, 4) : "";
            }

            if (columnName.equals(ACCOUNTING_CENTER)) {
                return BOCreUtils.getInstance().getCentroContable(product, trade.getBook().getLegalEntity().getCode(), false);
            }

            if (columnName.equals(ENTITY_ALM)) {
                return BOCreUtils.getInstance().getEntityCod(trade.getBook().getLegalEntity().getCode(), true);
            }

            if (columnName.equals(ACCOUNTING_CENTER_ALM)) {
                return BOCreUtils.getInstance().getCentroContable(product, trade.getBook().getLegalEntity().getCode(), true);
            }

            if (columnName.equals(BRANCH)) {
                return trade.getBook().getLegalEntity().getCode();
            }

            if (columnName.equals(BRANCH_ID)) {
                return trade.getBook().getLegalEntity().getLongId();
            }

            if (columnName.equals(DEAL_ID)) {
                return trade.getLongId();
            }

            if (columnName.equals(INTERNALREF)) {
                return trade.getCounterParty().getId() == trade.getBook().getLegalEntity().getId() ? "I" : "M";
            }

            if (columnName.equals(ISIN)) {
                return getISIN(equity);
            }

            if (columnName.equals(ISINDESC)) {
                return getISINDESC(equity);
            }

            if (columnName.equals(STATUS)) {
                return trade.getStatus().getStatus();
            }

            if (columnName.equals(SOURCESYSTEM)) {
                return "MUREX EQ";
            }

            if (columnName.equals(INSTRUMENT)) {
                return "Equity";
            }
            if (columnName.equals(INSTRUMENT_ID)) {
                String partenonID = trade.getKeywordValue(LE_ATTR_PARTENON_ACCOUNTING_ID);
                if(!Util.isEmpty(partenonID) && partenonID.length()>20) {
                    return partenonID.substring(8, 8+3);
                }
            }
            if (columnName.equals(INSTRTYPE)) {
                String partenonID = trade.getKeywordValue(LE_ATTR_PARTENON_ACCOUNTING_ID);
                if(!Util.isEmpty(partenonID) && partenonID.length()>20) {
                    return partenonID.substring(18, 18+3);
                }
            }

            if (columnName.equals(DIRECTION)) {
                int buySell = equity.getBuySell(trade);
                return buySell==1 ? "B" : "S";
            }

            if (columnName.equals(COUNTERPARTY_ID)) {
                return trade.getCounterParty().getLongId();
            }

            if (columnName.equals(GLSCOUNTERPARTY)) {
                return trade.getCounterParty().getCode();
            }

            if (columnName.equals(COUNTERPARTYDESC)) {
                return trade.getCounterParty().getName();
            }

            if (columnName.equals(NIFCOUNTERPARTY)) {
                return trade.getCounterParty().getExternalRef();
            }

            if (columnName.equals(COUNTERPARTYSECTOR)) {
                final LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, trade.getCounterParty().getId(), "ALL", LE_ATTR_SECTORCONTABLE);
                return attr!=null ? attr.getAttributeValue() : "";
            }

            if (columnName.equals(COUNTERPARTYCOUNTRY)) {
                if(trade.getCounterParty().getCountry()!=null) {
                    Country country =BOCache.getCountry(DSConnection.getDefault(), trade.getCounterParty().getCountry());
                    if(country!=null)
                        return country.getISOCode();
                    return "";
                }
            }

            if (columnName.equals(INPUTDATE)) {
                return formatDate(trade.getEnteredDate().getJDate(pricingEnv.getTimeZone()));
            }

            if (columnName.equals(TRADEDATE)) {
                return formatDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
            }

            if (columnName.equals(VALUEDATE)) {
                return formatDate(trade.getSettleDate());
            }

            if (columnName.equals(SETTLEDATE)) {
                return formatDate(trade.getSettleDate());
            }

            if (columnName.equals(STRATEGY)) {
                Book book = trade.getBook();
                if(book!=null) {
                    AccountingBook acctBook = book.getAccountingBook();
                    if(acctBook != null) {
                        String acctBookName = acctBook.getName();
                        if(acctBookName.equals("Negociacion")) {
                            return "NEG";
                        }
                        else if(acctBookName.equals("Inversion crediticia")) {
                            return "COS";
                        }
                        else if(acctBookName.equals("Otros a valor razonable")) {
                            return "OVR";
                        }
                    }
                }
                return "NEG";
            }

            if (columnName.equals(CURRENCY)) {
                return trade.getTradeCurrency();
            }

            if (columnName.equals(SETTLECURR)) {
                return trade.getSettleCurrency();
            }

            if (columnName.equals(QUANTITY)) {
                return formatResult(trade.getQuantity());
            }

            if (columnName.equals(SETTLEMENTAMOUNTCOMISIONDIVLIQ)) {
                return formatResult(equity.calcSettlementAmount(trade));
            }

            if (columnName.equals(SETTLEMENTAMOUNTDIVLIQ)
                    || columnName.equals(NOMINALQUANTITY)
                    || columnName.equals(INIITIALNOMINALQUANTITY)){
                Fee fee = getFee(trade, "BRK");
                return fee !=null ? formatResult(equity.calcSettlementAmount(trade)-fee.getAmount()) : formatResult(equity.calcSettlementAmount(trade));
            }

            if (columnName.equals(SETTLEMENTAMOUNTDIVEQ)) {
                Double settleAmount = equity.calcSettlementAmount(trade);
                //Double fx = trade.getSplitBasePrice();
                //return fx != 0 ? settleAmount*fx : settleAmount;
                return formatResult(settleAmount);
            }

            if (columnName.equals(LOTSIZE)) {
                return 1.0d;
            }

            if (columnName.equals(PRICEATSTART)) {
                return formatResult(trade.getTradePrice());
            }

            if (columnName.equals(BROKERCODE)) {
                try {
                    LegalEntity le = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(trade.getBroker());
                    return le!=null ? le.getCode() : "";
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Could not get the Legal Entity");
                }
            }

            if (columnName.equals(BROKERDESC)) {
                try {
                    LegalEntity le = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(trade.getBroker());
                    return le!=null ? le.getName() : "";
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Could not get the Legal Entity");
                }
            }


            if (columnName.equals(PARTENONID)) {
                return trade.getKeywordValue(LE_ATTR_PARTENON_ACCOUNTING_ID);
            }

            if (columnName.equals(BOINDICATOR)) {
                final Object isInternalDeal = super.getColumnValue(row, IS_INTERNAL_DEAL, errors);
                if (Util.isTrue(isInternalDeal, false)) {
                    return "S";
                }
                return "N";
            }

            if (columnName.equals(ISSUERGLS)) {
                LegalEntity le = equity.getIssuer();
                return le!=null ? le.getCode() : "";
            }

            if (columnName.equals(ISSUERCODE)) {
                LegalEntity le = equity.getIssuer();
                return le!=null ? le.getLongId() : "";
            }

            if (columnName.equals(ISSUERDESC)) {
                LegalEntity le = equity.getIssuer();
                return le!=null ? le.getName() : "";
            }

            if (columnName.equals(ISSUERSECTOR)) {
                LegalEntity le = equity.getIssuer();
                if(le!=null){
                    final LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, le.getId(), "ALL", LE_ATTR_SECTORCONTABLE);
                    return attr!=null ? attr.getAttributeValue() : "";
                }
                return "";
            }

            if (columnName.equals(ISSUERCOUNTRY)) {
                LegalEntity le = equity.getIssuer();
                if(le!=null) {
                    Country country =BOCache.getCountry(DSConnection.getDefault(), le.getCountry());
                    if(country!=null)
                        return country.getISOCode();
                }
                return "";
            }

            if (columnName.equals(ISSUERNIF)) {
                LegalEntity le = equity.getIssuer();
                if(le!=null)
                    return le.getExternalRef();
            }

            if (columnName.equals(YOURCUSTODIANCODE)) {
                final LegalEntity custodian = getYourCustodian(transfer);
                if(custodian!=null) {
                    return custodian.getId();
                }
                return null;
            }

            if (columnName.equals(YOURCUSTODIANACCOUNT)) {
                final SettleDeliveryInstruction sdi = getYourSDI(transfer);
                if(sdi!=null) {
                    return sdi.getAgentAccount();
                }
                return null;
            }

            if (columnName.equals(YOURCUSTODIANSWIFT)) {
                final LegalEntity custodian = getYourCustodian(transfer);
                if(custodian!=null) {
                    return getSwiftCode(custodian, Arrays.asList("Agent"));
                }
                return null;
            }

            if (columnName.equals(YOURCUSTODIANGLS)) {
                final LegalEntity custodian = getYourCustodian(transfer);
                if(custodian!=null) {
                    return custodian.getCode();
                }
                return null;
            }

            if (columnName.equals(YOURCUSTODIANDESC)) {
                final LegalEntity custodian = getYourCustodian(transfer);
                if(custodian!=null) {
                    return custodian.getName();
                }
                return null;
            }

            if (columnName.equals(YOURCUSTODIANCOUNTRY)) {
                final LegalEntity custodian = getYourCustodian(transfer);
                if(custodian!=null) {
                    Country country =BOCache.getCountry(DSConnection.getDefault(), custodian.getCountry());
                    if(country!=null)
                        return country.getISOCode();
                }
                return null;
            }

            if (columnName.equals(OURCUSTODIANCODE)) {
                final LegalEntity custodian = getOurCustodian(transfer);
                if(custodian!=null) {
                    return custodian.getId();
                }
                return null;
            }

            if (columnName.equals(OURCUSTODIANACCOUNT)) {
                final SettleDeliveryInstruction sdi = getOurSDI(transfer);
                if(sdi!=null) {
                    return sdi.getAgentAccount();
                }
                return null;
            }

            if (columnName.equals(OURCUSTODIANSWIFT)) {
                final LegalEntity custodian = getOurCustodian(transfer);
                if(custodian!=null) {
                    return getSwiftCode(custodian, Arrays.asList("Agent"));
                }
                return null;
            }

            if (columnName.equals(OURCUSTODIANGLS)) {
                final LegalEntity custodian = getOurCustodian(transfer);
                if(custodian!=null) {
                    return custodian.getCode();
                }
                return null;
            }

            if (columnName.equals(OURCUSTODIANDESC)) {
                final LegalEntity custodian = getOurCustodian(transfer);
                if(custodian!=null) {
                    return custodian.getName();
                }
                return null;
            }

            if (columnName.equals(OURCUSTODIANCOUNTRY)) {
                final LegalEntity custodian = getOurCustodian(transfer);
                if(custodian!=null) {
                    Country country =BOCache.getCountry(DSConnection.getDefault(), custodian.getCountry());
                    if(country!=null)
                        return country.getISOCode();
                }
                return null;
            }

            if (columnName.equals(SETTLEMENTTYPE)) {
                if(transfer!=null && "DAP".equals(transfer.getDeliveryType())) {
                    return "CP";
                }
                else if(transfer!=null && "DFP".equals(transfer.getDeliveryType())) {
                    return "LP";
                }
                return "";
            }

            if (columnName.equals(PRODUCT)) {
                String equityType = equity.getSecCode(EQUITY_TYPE);
                if(!Util.isEmpty(equityType) && "CO2".equalsIgnoreCase(equityType)){
                    String productSubType = trade.getKeywordValue("Mx_Product_SubType");
                    if(!Util.isEmpty(productSubType) && "SPOT".equalsIgnoreCase(productSubType)){
                        return "CO2 - SPOT";
                    }
                    else if(!Util.isEmpty(productSubType) && "FORWARD".equalsIgnoreCase(productSubType)){
                        return "CO2 - FORWARD";
                    }
                }
                else if(!Util.isEmpty(equityType) && "ETF".equalsIgnoreCase(equityType)){
                    return "ETF";
                }
                else if(!Util.isEmpty(equityType) && "VCO2".equalsIgnoreCase(equityType)){
                    String productSubType = trade.getKeywordValue("Mx_Product_SubType");
                    if(!Util.isEmpty(productSubType) && "SPOT".equalsIgnoreCase(productSubType)){
                        return "VCO2 - SPOT";
                    }
                    else if(!Util.isEmpty(productSubType) && "FORWARD".equalsIgnoreCase(productSubType)){
                        return "VCO2 - FORWARD";
                    }
                }
                return valDate.gte(trade.getSettleDate()) ? "EQ" : "EQF";
            }

            if (columnName.equals(LEGALENTITY)) {
                return "00001";
            }

            if (columnName.equals(SYSTEM_BO)) {
                return "CALYPSO STC";
            }

            if (columnName.equals(TIPOCAMBIO)) {
                Double basePrice = trade.getSplitBasePrice();
                return basePrice!=null && basePrice!=0 ? formatResult(basePrice) : "";
            }

            if (columnName.equals(PRICE)) {
                final Double todayPrice = row.getProperty(EquityMisPlusCompromisoReport.DIRTY_PRICE_STR);
                return formatResult(todayPrice);
            }

            if (columnName.equals(YESTERDAYPRICE)) {
                final Double yesterdayPrice = row.getProperty(EquityMisPlusCompromisoReport.YESTERDAY_DIRTY_PRICE_STR);
                return formatResult(yesterdayPrice);
            }

            if (columnName.equals(REVALAMOUNT)) {
                return "";
            }

            if (columnName.equals(AVGPRINCIPAL)) {
                Boolean isBuy = (Boolean)super.getColumnValue(row, IS_BUY, errors);
                if(isBuy) {
                    return formatResult(equity.getPrincipal());
                }
                else
                    return formatResult(equity.getPrincipal());
            }

            if (columnName.equals(MARKETVALUEMAN)) {
                Book book = trade.getBook();
                if(book!=null) {
                    AccountingBook acctBook = book.getAccountingBook();
                    if(acctBook != null) {
                        String acctBookName = acctBook.getName();
                        if(acctBookName.equals("Inversion crediticia")) {
                            return "0";
                        }
                    }
                }
                final PLMark plMarkValue = getPLMarkValue(pricingEnv, trade, valDate);
                if(plMarkValue!=null){
                    return formatResult(getPLMark(plMarkValue,"MTM_FULL_LAGO"));
                } else if (mirrorTrade != null){
                    final PLMark plMarkValueMirrorTrade = getPLMarkValue(pricingEnv, mirrorTrade, valDate);
                    if(plMarkValueMirrorTrade!=null){
                        return formatResult(-getPLMark(plMarkValueMirrorTrade,"MTM_FULL_LAGO"));
                    }
                }
                return "";
            }

            if (columnName.equals(MARKETVALUEACC)) {
                Book book = trade.getBook();
                if(book!=null) {
                    AccountingBook acctBook = book.getAccountingBook();
                    if(acctBook != null) {
                        String acctBookName = acctBook.getName();
                        if(acctBookName.equals("Inversion crediticia")) {
                            return "0";
                        }
                    }
                }
                final PLMark plMarkValue = getPLMarkValue(pricingEnv, trade, valDate);
                if(plMarkValue!=null){
                    return formatResult(getPLMark(plMarkValue,"MTM_MARKET_VALUE"));
                } else if (mirrorTrade != null){
                    final PLMark plMarkValueMirrorTrade = getPLMarkValue(pricingEnv, mirrorTrade, valDate);
                    if(plMarkValueMirrorTrade!=null){
                        return formatResult(- getPLMark(plMarkValueMirrorTrade,"MTM_MARKET_VALUE"));
                    }
                }
                return "";
            }

            if (columnName.equals(HEDGINGTYPE)) {
                Book book = trade.getBook();
                if(book!=null) {
                    AccountingBook acctBook = book.getAccountingBook();
                    if(acctBook != null) {
                        String acctBookName = acctBook.getName();
                        if(acctBookName.equals("Negociacion")) {
                            return "NE";
                        }
                        else if(acctBookName.equals("Disponible para la venta")) {
                            return "DV";
                        }
                        else if(acctBookName.equals("Inversion crediticia")) {
                            return "IC";
                        }
                        else if(acctBookName.equals("Inversion a vencimiento")) {
                            return "COS";
                        }
                        else if(acctBookName.equals("Otros a valor razonable")) {
                            return "OV";
                        }
                    }
                }
                return "";
            }

            if (columnName.equals(EQUITY_TYPE)) {
                return equity.getSecCode(EQUITY_TYPE);
            }

            if (columnName.equals(EQUITY_TYPE_DESC)) {
                String equityType = equity.getSecCode(EQUITY_TYPE);
                if(!Util.isEmpty(equityType)) {
                    switch (equityType) {
                        case "ADR":
                            return "RV ADR";
                        case "CS":
                            return "RV Acciones Ordinarias";
                        case "PROP":
                            return "RV Acciones Propias";
                        case "DERSUS":
                            return "RV Derechos de Suscripcion";
                        case "PEGROP":
                            return "RV Participaciones Empresas del Grupo";
                        case "PFI":
                            return "RV Participaciones Fondos";
                        case "PS":
                            return "RV Participaciones: Preferentes";
                        case "INSW":
                            return "Cesta indices";
                        default:
                            return "";
                    }
                }
                return "";
            }

            if (columnName.equals(AUTOCARTERA)) {
                LegalEntity issuer = equity.getIssuer();
                if(issuer != null) {
                    String issuerName = issuer.getCode() != null ? equity.getIssuer().getCode() : "";
                    return (!Util.isEmpty(issuerName) && "BSTE".equals(issuerName)) ? "SI" : "NO";
                }
                return "NO";
            }

            if (columnName.equals(ADDITIONALFEEFLOW)) {
                Fee fee = getFee(trade, "EXCHANGE_FEE");
                return fee != null ? formatResult(fee.getAmount()) : "";
            }

            if (columnName.equals(COMMON)) {
                return equity.getSecCode("Common");
            }

            if (columnName.equals(FO_SOURCE)) {
                return "MUREX";
            }

            if (columnName.equals(IS_SPOT)) {
                Equity eq = (Equity)trade.getProduct();
                String equityType = eq.getSecCode("EQUITY_TYPE");
                if(!Util.isEmpty(equityType) && ("CO2".equalsIgnoreCase(equityType) || "VCO2".equalsIgnoreCase(equityType))){
                    String mxProductSubType = trade.getKeywordValue("Mx_Product_SubType");
                    if(!Util.isEmpty(mxProductSubType) && "SPOT".equalsIgnoreCase(mxProductSubType)){
                        return "Y";
                    }
                    else if(!Util.isEmpty(mxProductSubType) && "FORWARD".equalsIgnoreCase(mxProductSubType)){
                        return "N";
                    }
                }
                return "";
            }

            if (columnName.equals(PRODUCTO_CONCRETO)) {
                return "EUA TRANSACTION - FORWARD";
            }

            if (columnName.equals(MARKET)) {
                return equity.getExchange();
            }

            if (columnName.equals(MARKET)) {
                return equity.getExchange();
            }

            if (columnName.equals(STATUSDEALCOMP) || columnName.equals(STATUSDEAL)) {
                return valDate.gte(trade.getSettleDate()) ? "VENCIDA" : "VIVA" ;
            }

            if (columnName.equals(FILLER1)){
                return getInternalPortfolioMirror(trade);
            }

        }

        return formatResult(super.getColumnValue(row, columnName, errors));
    }


    public final String getISINDESC(final Equity equity) {
        return equity.getDescription();
    }


    public final String getISIN(final Equity equity) {
        return equity.getSecCode(SecCode.ISIN);
    }


    public LegalEntity getOurCustodian(BOTransfer transfer) {
        return transfer!=null ? BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getInternalAgentId()) : null;
    }


    public LegalEntity getYourCustodian(BOTransfer transfer) {
        return transfer!=null ? BOCache.getLegalEntity(DSConnection.getDefault(), transfer.getExternalAgentId()) : null;
    }


    public Fee getFee(Trade trade, String type) {
        Vector<Fee> feeList = trade.getFeesList();
        if(feeList!=null && feeList.size()>0) {
            for(Fee fee : trade.getFeesList()) {
                if(fee != null && type.equalsIgnoreCase(fee.getType())) {
                    return fee;
                }
            }
        }
        return null;
    }


    @SuppressWarnings("rawtypes")
    public String getKeywordValue(ReportRow row, String keyword, Vector errors) {
        final Object value = super.getColumnValue(row, TRADE_KEYWORD_PREFIX + keyword, errors);
        if (value != null) {
            return value.toString();
        }
        return null;
    }


    public String getSwiftCode(LegalEntity le, List<String> roles) {
        for (final String role : roles) {
            for (final String contactType : contactTypes) {
                final LEContact contact = BOCache.getContact(DSConnection.getDefault(), role, le, contactType, LEContact.ALL,
                        0);
                if (contact != null && contact.getSwift() != null) {
                    return contact.getSwift();
                }
            }
        }
        return null;
    }


    public SettleDeliveryInstruction getYourSDI(BOTransfer transfer) {
        if (transfer == null) {
            return null;
        }
        return BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), transfer.getExternalSettleDeliveryId());
    }


    public SettleDeliveryInstruction getOurSDI(BOTransfer transfer) {
        if (transfer == null) {
            return null;
        }
        return BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), transfer.getInternalSettleDeliveryId());
    }


    public static Object formatResult(Object o) {
        return UtilReport.formatResult(o, '.');
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


    private String formatDate(JDate jDate){
        String date = "";
        if (jDate != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            date = format.format(jDate.getDate());
        }
        return date;
    }

    private String formatDate(JDate jDate, String separator){
        String date = "";
        if (jDate != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd" + separator + "MM" + separator + "yyyy", Locale.getDefault());
            date = format.format(jDate.getDate());
        }
        return date;
    }


    public String getInternalPortfolioMirror(Trade trade) {
        return trade.getMirrorBook() != null ? trade.getMirrorBook().toString() : "";
    }


}
