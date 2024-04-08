package calypsox.tk.report;

import calypsox.tk.report.quotes.FXQuoteHelper;
import calypsox.util.collateral.*;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.util.CurrencyUtil;
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class PdvConciliacionReportStyle extends TradeReportStyle {

    public static final String PROCESS_DATE = "PROCESSDATE";
    public static final String IDOPER = "IDOPER";
    public static final String IDROOTID = "IDROOTID";
    public static final String CALYPSOID = "CALYPSOID";
    public static final String MATURITY_DATE = "MATURITYDATE";
    public static final String TRADE_CPTY_NAME = "TRADECPTY";
    public static final String MIRROR_BOOK_NAME = "MIRRORBOOK";
    public static final String DIRECTION = "DIRECTION";
    public static final String INTERNAL = "INTERNAL";
    public static final String INITIAL_CAPITAL = "INITIALCAPITAL";
    public static final String CURRENT_CAPITAL = "CURRENTCAPITAL";
    public static final String NEXT_CAPITAL = "NEXTCAPITAL";
    public static final String LOT_SIZE = "LOTSIZE";
    public static final String LIQ_METHOD = "LIQUIDATIONMETHOD";
    public static final String OPEN_REPO = "OPEN";
    public static final String EVENT_DATE = "EVENTDATE";
    public static final String EVENT_TYPE = "LASTEVENT";
    public static final String SLA = "SLA";
    public static final String FEED = "FEED";
    public static final String EFF_INI = "EFFINITIAL";
    public static final String EFF_FIN = "EFFFINAL";
    public static final String FEE_RATE = "FEERATE";


    //Precios
    public static final String QUANTITY = "QUANTITY";
    public static final String CLEAN_PRICE = "CLEANPRICE";
    public static final String DIRTY_PRICE = "DIRTYPRICE";

    private static final String SEC_MARGIN_VALUE="Sec. Margin Value";
    private static final String SEC_EQ_PRICE_INITIAL="Sec. Price (Initial)";
    private static final String TRADE_PRICE ="Trade Price";
    private static final String SEC_NOMINAL_INITIAL="Sec. Nominal (Initial)";

    private static final String K_MXINITIAL_DIRTYPRICE =  "MXInitialDirtyPrice";
    private static final String K_MXINITIAL_PRICE =  "MXInitialEquityPrice";
    private static final String K_MXINITIAL_MARGIN =  "MXInitialMargin";
    private static final String K_MXINITIAL_CELAN_PRICE =  "MxInitialCleanPrice";





    private final FXQuoteHelper fxQuoteHelper = new FXQuoteHelper("OFFICIAL");


    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        Trade trade = null!=row && row.getProperty("Trade") instanceof Trade ? (Trade)row.getProperty("Trade") : null;
        JDatetime valuationDatetime = null != row ? (JDatetime) row.getProperty("ValuationDatetime") : null;
        JDate jDate = null != valuationDatetime ? valuationDatetime.getJDate(TimeZone.getDefault()) : null;

        if(PROCESS_DATE.equalsIgnoreCase(columnId)){
            return jDate;
        }else if(IDOPER.equalsIgnoreCase(columnId)){
            return null!=trade ? trade.getKeywordValue("Contract ID") : "";
        }else if(IDROOTID.equalsIgnoreCase(columnId)){
            return getIdRootId(trade);
        }else if(CALYPSOID.equalsIgnoreCase(columnId)){
            return null!=trade ? trade.getKeywordValue("CalypsoId") : "";
        }else if(MATURITY_DATE.equalsIgnoreCase(columnId)){
            return !isOpen(trade) ? super.getColumnValue(row, "End Date", errors) : "";
        }else if(TRADE_CPTY_NAME.equalsIgnoreCase(columnId)){
            return !isInternal(trade) ? super.getColumnValue(row, "CounterParty.Short Name", errors) : "";
        }else if(MIRROR_BOOK_NAME.equalsIgnoreCase(columnId)){
            return isInternal(trade) ? trade.getMirrorBook().getName() : "";
        }else if(DIRECTION.equalsIgnoreCase(columnId)){
            return getDirection(trade);
        }else if(INTERNAL.equalsIgnoreCase(columnId)){
            return getInternal(trade);
        }else if(INITIAL_CAPITAL.equalsIgnoreCase(columnId)){
            return getInitialCapital(trade);
        }else if(CURRENT_CAPITAL.equalsIgnoreCase(columnId)){
            return getCurrentCapital(trade,jDate);
        }else if(LOT_SIZE.equalsIgnoreCase(columnId)){
           return getLotSize(trade);
        }else if(NEXT_CAPITAL.equalsIgnoreCase(columnId)){
            return getCurrentCapital(trade,jDate);
        }else if(LIQ_METHOD.equalsIgnoreCase(columnId)){
            return getLiqMethod(trade);
        }else if(OPEN_REPO.equalsIgnoreCase(columnId)){
            return isOpen(trade) ? "Y":"N";
        }else if(EVENT_DATE.equalsIgnoreCase(columnId)){
            return getEventDate(trade);
        }else if(EVENT_TYPE.equalsIgnoreCase(columnId)){
            return getEvetType(trade);
        }else if(SLA.equalsIgnoreCase(columnId)){
            return getSla(trade);
        }else if(FEED.equalsIgnoreCase(columnId)){
            return "";
        }else if(EFF_INI.equalsIgnoreCase(columnId)){
            return getFormatAmount(buildEfecInicio(trade,row,errors));
        }else if(EFF_FIN.equalsIgnoreCase(columnId)){
            return getFormatAmount(getEfectivofinal(trade,jDate,row,errors));
        }else if(QUANTITY.equalsIgnoreCase(columnId)){
            return getFormatAmount(getAmount(row, "Quantity", errors));
        }else if(CLEAN_PRICE.equalsIgnoreCase(columnId)){
            return getCleanPrice(trade,row, errors);
        }else if(DIRTY_PRICE.equalsIgnoreCase(columnId)){
            return getDirtyPrice(trade,row, errors);
        }else if(FEE_RATE.equalsIgnoreCase(columnId)){
            return formatDecimal(getAmount(row, "Fee. Rate (Last)", errors));
        }else {
            return super.getColumnValue(row, columnId, errors);
        }

    }

    private boolean isInternal(Trade trade){
        return Optional.ofNullable(trade.getMirrorBook()).isPresent();
    }

    private String getInternal(Trade trade){
        return isInternal(trade) ? "Y": "N";
    }

    private String getDirection(Trade trade){
        String direction = Optional.ofNullable(trade).map(t -> ((SecLending) t.getProduct()).getDirection()).orElse("");
        switch (direction){
            case "Borrow" :
                return "Compra";
            case "Lend" :
                return "Venta";
            default:
                return "";
        }
    }

    private boolean isOpen(Trade trade){
        return null!=trade && "OPEN".equalsIgnoreCase(((SecLending) trade.getProduct()).getMaturityType());
    }

    private String getLiqMethod(Trade trade){
        String subType = Optional.ofNullable(trade).map(t -> ((SecLending) t.getProduct()).getSubType()).orElse("");
        String result  = "";
        switch (subType){
            case "Fee Non Cash Pool":
                result = "DFP";
                break;
            case "Fee Cash Pool":
                result = Optional.ofNullable(trade).map(t -> ((SecLending) t.getProduct()).getDeliveryType()).orElse("");
                break;
        }

        switch (result){
            case "DFP":
                return "FOP";
            case "DAP":
                return "DVP";
            default:
                return result;
        }
    }

    private String getSla (Trade trade){
        Vector<EventTypeAction> eventTypeActions = ((SecLending) trade.getProduct()).getEventTypeActions();
        boolean present = eventTypeActions.stream().anyMatch(event -> "Partial Return".equalsIgnoreCase(event.getActionType()));
        if(present){
            return "Y";
        }
        return "N";
    }

    public Double getAmount(ReportRow row, String value, Vector errors){
        Object amount = super.getColumnValue(row, value, errors);
        if(amount instanceof Amount){
            return ((Amount) amount).get();
        }else if(amount instanceof DisplayValue){
            return ((DisplayValue)amount).get();
        }
        return 0.00;
    }

    public String getFormatAmount(final double value) {
        if(!Double.isNaN(value)){
            final DecimalFormat myFormatter = new DecimalFormat("###0.00");
            final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
            tmp.setDecimalSeparator('.');
            myFormatter.setDecimalFormatSymbols(tmp);
            String format = myFormatter.format(value);
            return !"NaN".equalsIgnoreCase(format) && !"-0.00".equalsIgnoreCase(format) ? format : "0.00";
        }
        return "0.00";
    }

    public String formatDecimal(final double value) {
        String decimal = String.valueOf(value);
        if(null!=decimal && decimal.contains(",")){
            decimal.replace(",",".");
        }
        return decimal;
    }

    private String getCurrentCapital(Trade trade, JDate jDate){
        if(null!=trade){
            Product security = ((SecLending) trade.getProduct()).getSecurity();
            double remainingQuantity = null!=security ? ((SecLending) trade.getProduct()).getRemainingQuantity(jDate) : 0.00;

            JDate endDate = ((SecLending) trade.getProduct()).getEndDate();
            if((null!=endDate && endDate.after(jDate)) || "OPEN".equalsIgnoreCase(((SecLending) trade.getProduct()).getMaturityType())){
                if(security instanceof Bond){
                     remainingQuantity = remainingQuantity * ((Bond) security).getFaceValue();
                }
            }else{
                remainingQuantity = 0.00D;
            }

            return getFormatAmount(remainingQuantity);
        }
        return "0.00";
    }

    private double getPrice(String quoteSetName, Trade trade, JDate date){
        Product security = ((SecLending) trade.getProduct()).getSecurity();
        PricingEnv env = new PricingEnv();
        if(security instanceof Bond ){
            env.setQuoteSetName(quoteSetName);
        }
        return CollateralUtilities.getDirtyPrice(security, date, env, null);
    }

    private String getIdRootId(Trade trade){
        String murexRootContract = "";
        if(null!=trade){
            murexRootContract = trade.getKeywordValue("MurexRootContract");
            if(isInternal(trade)){
                String contract_id = trade.getKeywordValue("Contract ID");
                if(Util.isEmpty(contract_id)){
                    murexRootContract = "-"+murexRootContract;
                }
            }
        }
        return murexRootContract;
    }


    private String getInitialCapital(Trade trade){
        double cumulativeOriginalQuantity = 0.0;

        if(null!=trade){
            Vector collaterals = ((SecLending) trade.getProduct()).getCollaterals();
            if(!Util.isEmpty(collaterals)){
                Collateral collateral = (Collateral) collaterals.get(0);
                cumulativeOriginalQuantity = collateral.getQuantity();

                Product security = ((SecLending) trade.getProduct()).getSecurity();
                if( security instanceof Bond){
                    cumulativeOriginalQuantity = cumulativeOriginalQuantity * ((Bond) security).getFaceValue();
                }
            }
        }

        return getFormatAmount(cumulativeOriginalQuantity);
    }

    private String getLotSize(Trade trade){
        Product underlyingProduct = null;
        String lotsize = "";
        if (trade != null) {
            underlyingProduct = ((SecLending) trade.getProduct()).getSecurity();
            if(underlyingProduct != null){
                if(underlyingProduct instanceof Bond){
                    lotsize = String.valueOf(((Bond) underlyingProduct).getFaceValue());
                }else if (underlyingProduct instanceof Equity){
                    lotsize = String.valueOf(((Equity) underlyingProduct).getTradingSize());
                }
            }
        }
        return lotsize;
    }

    private String getEvetType(Trade trade){
        String actionresult = "";
        if(null!=trade){
            Vector <EventTypeAction>eventTypeActions = ((SecLending) trade.getProduct()).getEventTypeActions();
            if(!Util.isEmpty(eventTypeActions)){
                Collections.sort(eventTypeActions, Comparator.comparingInt(EventTypeAction::getId));
                int size = eventTypeActions.size();
                for(int i = size-1;i>=0;i--){
                    EventTypeAction action = eventTypeActions.get(i);
                    if("Termination".equalsIgnoreCase(action.getActionType()) || "Partial Return".equalsIgnoreCase(action.getActionType())){
                        actionresult =  action.getActionType();
                    }
                }
            }
        }
        return actionresult;
    }

    private JDate getEventDate(Trade trade){
        JDate jDateresult = null;
        if(null!=trade){
            Vector <EventTypeAction>eventTypeActions = ((SecLending) trade.getProduct()).getEventTypeActions();
            if(!Util.isEmpty(eventTypeActions)){
                Collections.sort(eventTypeActions, Comparator.comparingInt(EventTypeAction::getId));
                int size = eventTypeActions.size();
                for(int i = size-1;i>=0;i--){
                    EventTypeAction action = eventTypeActions.get(i);
                    if("Termination".equalsIgnoreCase(action.getActionType()) || "Partial Return".equalsIgnoreCase(action.getActionType())){
                        jDateresult = action.getEffectiveDate();
                    }
                }
            }
        }
        return jDateresult;
    }

    private double buildEfecInicio(Trade trade,ReportRow row,Vector errors){
        double result = 0.0D;
        SecLending secLending = (SecLending) trade.getProduct();
        result = buildEfecInicioDFP(trade,row,errors,secLending);

        String direct = Optional.ofNullable(trade).map(t -> ((SecLending) t.getProduct()).getDirection()).orElse("");
        if("Borrow".equalsIgnoreCase(direct)){
            result =  Math.abs(result);
        }else if ("Lend".equalsIgnoreCase(direct) && result>0){
            result = result*(-1);
        }

        return result;
    }

    private double buildEfecInicioDFP(Trade trade,ReportRow row,Vector errors,SecLending secLending){
        double result = 0.0D;
        double quantity = 0.0D;
        Product security = ((SecLending) trade.getProduct()).getSecurity();

        Vector collaterals = ((SecLending) trade.getProduct()).getCollaterals();
        if(!Util.isEmpty(collaterals)) {
            Collateral collateral = (Collateral) collaterals.get(0);
            quantity = collateral.getQuantity();
        }

        double mxInitialMargin = trade.getKeywordAsDouble(K_MXINITIAL_MARGIN);

        if(security instanceof Bond){
            double faceValue = ((Bond) security).getFaceValue();
            double mxInitialDirtyPrice = trade.getKeywordAsDouble(K_MXINITIAL_DIRTYPRICE);
            double initial = (mxInitialDirtyPrice/100) * (mxInitialMargin/100);
            result = quantity * faceValue * initial;
        }else{
            double mxInitialEquityPrice = trade.getKeywordAsDouble(K_MXINITIAL_PRICE);
            double initial = (mxInitialEquityPrice/100) * (mxInitialMargin/100);
            result = quantity * initial;
        }

        return result;
    }

    private double getFxClose(String secCur,String settleCurr,QuoteValue quote){
        CurrencyPair currencyPair=new CurrencyPair(CurrencyUtil.getCurrencyDefault(secCur),
                CurrencyUtil.getCurrencyDefault(settleCurr));
        QuoteValue convertedQuote=currencyPair.convertQuoteToMarket(quote);
        return Optional.ofNullable(convertedQuote).map(QuoteValue::getClose)
                .orElse(1.0D);
    }


    private Object getSecurityPrice(Trade trade, Product product,ReportRow row,Vector errors){

        Object computeMargin = null;
        Product sec=Optional.ofNullable((SecLending)product).map(SecLending::getSecurities)
                .filter(secs->!Util.isEmpty(secs)).map(secs->secs.get(0))
                .map(Collateral::getUnderlyingProduct).orElse(null);
        if(sec instanceof Equity){
            computeMargin = computeMarginedPrice(super.getProductColumnValue(row, SEC_EQ_PRICE_INITIAL, errors ,product),row,errors,product);
        }else{
            computeMargin = computeMarginedPrice(super.getColumnValue(row, TRADE_PRICE, errors),row,errors,product);
        }
        return computeMargin;
    }

    private Object computeMarginedPrice(Object price,ReportRow row, Vector errors,Product product){
        Object marginValue=super.getProductColumnValue(row, SEC_MARGIN_VALUE, errors ,product);
        if(marginValue instanceof Rate) {
            if (price instanceof DisplayValue) {
                double v = ((Rate) marginValue).get();
                double v1 = ((DisplayValue) price).get();
                ((DisplayValue) price).set( v1 * v);
            }
        }
        return price;
    }


    private double getEfectivofinal(Trade trade,JDate valDate, ReportRow row, Vector errors){

        Double effectivoFinal = 0.0;
        Object value = super.getProductColumnValue(row, "Sec. Value", errors , ((SecLending) trade.getProduct()));
        if(null!=value){
            if (value instanceof DisplayValue) {
                effectivoFinal = ((DisplayValue) value).get();
            }
        }

        JDate endDate = ((SecLending) trade.getProduct()).getEndDate();
        if(!"TERMINATED".equalsIgnoreCase(trade.getStatus().getStatus())){
            if((null!=endDate && endDate.after(valDate))
                    || "OPEN".equalsIgnoreCase(((SecLending) trade.getProduct()).getMaturityType())) {
                String direct = Optional.ofNullable(trade).map(t -> ((SecLending) t.getProduct()).getDirection()).orElse("");
                if ("Borrow".equalsIgnoreCase(direct)) {
                    effectivoFinal = Math.abs(effectivoFinal);
                } else if ("Lend".equalsIgnoreCase(direct) && effectivoFinal > 0) {
                    effectivoFinal = effectivoFinal * (-1);
                }
            } else {
                effectivoFinal = 0.0D;
            }
        } else {
            effectivoFinal = 0.0D;
        }

        return effectivoFinal;
    }

    private String getCleanPrice(Trade trade, ReportRow row, Vector errors){
        if(null!=trade){
            Product security = ((SecLending) trade.getProduct()).getSecurity();
            if(security!=null){
                if(security instanceof Bond){
                    return formatDecimal(trade.getKeywordAsDouble(K_MXINITIAL_CELAN_PRICE));
                }else if(security instanceof Equity){
                    return formatDecimal(trade.getKeywordAsDouble(K_MXINITIAL_PRICE));
                }
            }
        }
        return "";
    }

    private String getDirtyPrice(Trade trade, ReportRow row, Vector errors){
        if(null!=trade){
            Product security = ((SecLending) trade.getProduct()).getSecurity();
            if(security!=null){
                if(security instanceof Bond){
                    return formatDecimal(trade.getKeywordAsDouble(K_MXINITIAL_DIRTYPRICE));
                }else if(security instanceof Equity){
                    return formatDecimal(trade.getKeywordAsDouble(K_MXINITIAL_PRICE));
                }
            }
        }
        return "";
    }
}
