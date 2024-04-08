package calypsox.tk.report;

import calypsox.tk.report.quotes.FXQuoteHelper;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.product.flow.flowDefinition.FdnCashFlow;
import com.calypso.tk.product.flow.flowDefinition.impl.FdnCashFlowDefinitionImpl;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.util.CurrencyUtil;
import org.apache.commons.lang.StringUtils;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class SecLendingReportStyle extends com.calypso.tk.report.SecLendingReportStyle {

    public static final String C_TITULOS_STR=MMSR_COLUMNS.C_TITULOS.name();
    public static final String EFEC_INICIO="EFEC_INICIO";
    public static final String EFECTIVO_VTO=MMSR_COLUMNS.EFECTIVO_VTO.name();
    public static final String EST=MMSR_COLUMNS.EST.name();
    public static final String COMISION=MMSR_COLUMNS.COMISION.name();
    public static final String SEC_PRICE="Security Price";


    private static final String SEC_MARGIN_VALUE="Sec. Margin Value";
    private static final String SEC_EQ_PRICE_INITIAL="Sec. Price (Initial)";
    private static final String SEC_DIRTY_PRICE_INITIAL="Sec. Dirty Price (Initial)";
    private static final String SEC_NOMINAL_INITIAL="Sec. Nominal (Initial)";

    private final FXQuoteHelper fxQuoteHelper=new FXQuoteHelper("OFFICIAL");


    @Override
    public Object getProductColumnValue(ReportRow row, String columnId, Vector errors, Product product) throws InvalidParameterException {

        Object columnValue=null;
        if(row!=null) {
            columnValue = MMSR_COLUMNS.lookUp(columnId).getColumnValue(row.getProperty(Trade.class.getSimpleName()));
            if(SEC_PRICE.equals(columnId)){
                columnValue=getSecurityPrice(product,row,errors);
            }else if(EFEC_INICIO.equals(columnId)){
                columnValue=buildEfecInicio(row.getProperty(Trade.class.getSimpleName()),row,errors);
            }else if(DELIVERY_TYPE.equals(columnId)){
                columnValue=getDeliveryType(product);
            }
        }
        if(columnValue==null) {
            columnValue=super.getProductColumnValue(row, columnId, errors,product);
        }
        return columnValue;
    }

    String formatEnumName(MMSR_COLUMNS enumName){
        String formattedName= enumName.name().replace("_"," ");
        return StringUtils.capitalize(formattedName);

    }

    private String getDeliveryType(Product secLending){
        String deliveryType=null;
        String subType = Optional.ofNullable(secLending).map(Product::getSubType).orElse("");
        if("Fee Non Cash Pool".equals(subType)) {
            deliveryType= BOTransfer.DFP;
        }else if("Fee Cash Pool".equals(subType)){
            deliveryType= BOTransfer.DAP;
        }
       return deliveryType;
    }
    private Object getSecurityPrice(Product product,ReportRow row,Vector errors){
        String priceColumnId;
        Product sec=Optional.ofNullable((SecLending)product).map(SecLending::getSecurities)
                .filter(secs->!Util.isEmpty(secs)).map(secs->secs.get(0))
                .map(Collateral::getUnderlyingProduct).orElse(null);
        if(sec instanceof Equity){
            priceColumnId=SEC_EQ_PRICE_INITIAL;
        }else{
            priceColumnId= SEC_DIRTY_PRICE_INITIAL;
        }
        return computeMarginedPrice(super.getProductColumnValue(row, priceColumnId, errors ,product),row,errors,product);
    }

    private Object computeMarginedPrice(Object price,ReportRow row, Vector errors,Product product){
        Object marginValue=super.getProductColumnValue(row, SEC_MARGIN_VALUE, errors ,product);
        if(marginValue instanceof Rate) {
            if (price instanceof DisplayValue) {
                ((DisplayValue) price).set(((DisplayValue) price).get() * ((Rate) marginValue).get());
            }
        }
        return price;
    }

    private Amount buildEfecInicio(Trade trade,ReportRow row,Vector errors){
        Amount result;
        SecLending secLending= (SecLending) trade.getProduct();
        if(secLending.isDeliveryTypeDAP()){
            result=new Amount(secLending.getInitialMarginValue());
        }else{
          result=buildEfecInicioDFP(trade,row,errors,secLending);
        }
        return result;
    }

    private Amount buildEfecInicioDFP(Trade trade,ReportRow row,Vector errors,SecLending secLending){
        Object nominalInitial=super.getProductColumnValue(row, SEC_NOMINAL_INITIAL, errors,secLending);
        Object price=getSecurityPrice(secLending,row,errors);
        double initialPrinc=0.0D;
        if(nominalInitial instanceof DisplayValue && price instanceof DisplayValue) {
            initialPrinc = ((DisplayValue) nominalInitial).get() * ((DisplayValue) price).get();
            String secCurrecy=secLending.getSecurity().getCurrency();
            try {
                QuoteValue fxQuote = fxQuoteHelper.getMrktConventionFXQuote(secCurrecy,trade.getSettleCurrency(),
                       trade.getEnteredDate().getJDate(TimeZone.getDefault()));
                if(!Util.isEqualStrings(secCurrecy,trade.getSettleCurrency())&&fxQuote!=null) {
                    initialPrinc = initialPrinc*getFxClose(secCurrecy,trade.getSettleCurrency(),fxQuote);
                }
            } catch (MarketDataException e) {
                Log.warn(this.getClass().getSimpleName(),"Couldn't find FX Quote for trade "+trade.getLongId());
            }
        }
        return new Amount(initialPrinc);
    }

    private double getFxClose(String secCur,String settleCurr,QuoteValue quote){
        CurrencyPair currencyPair=new CurrencyPair(CurrencyUtil.getCurrencyDefault(secCur),
                    CurrencyUtil.getCurrencyDefault(settleCurr));
        QuoteValue convertedQuote=currencyPair.convertQuoteToMarket(quote);
        return Optional.ofNullable(convertedQuote).map(QuoteValue::getClose)
                    .orElse(1.0D);
    }
    enum MMSR_COLUMNS{

        C_TITULOS{
            @Override
            Object getColumnValue(Trade trade) {
                return buildCTitulos();
            }
        },
        EFECTIVO_VTO{
            @Override
            Object getColumnValue(Trade trade) {
                return buildCTitulos();
            }
        },
        EST{
            @Override
            Object getColumnValue(Trade trade) {
                return "A";
            }
        },
        DIVISA_COMISION{
            @Override
            Object getColumnValue(Trade trade) {
                return buildDivisaComision(trade);
            }
        },
        COMISION{
            @Override
            Object getColumnValue(Trade trade) {
                return buildComision(trade);
            }
        },
        INVALID;

        static MMSR_COLUMNS lookUp(String columnName) {
            MMSR_COLUMNS result;
            String enumName= Optional.ofNullable(columnName).map(String::toUpperCase)
                    .map(name->name.replace(" ","_")).orElse("");
            try {
                result =MMSR_COLUMNS.valueOf(enumName);
            } catch (IllegalArgumentException exc) {
                result=INVALID;
            }
            return result;
        }

        Object getColumnValue(Trade trade){
            return null;
        }
        String buildCTitulos(){
            return SecLending.class.getSimpleName();
        }

        Amount buildComision(Trade trade){
            SecLending secLending= (SecLending) trade.getProduct();
            double result=0.0D;
            try {
                CashFlowSet flows=getFlows(trade,secLending);
                for (CashFlow flow : flows) {
                    if (CashFlow.SECLENDING_FEE.equals(flow.getType())||"ADJUSTMENT_FEE".equals(flow.getType())) {
                        result=result+flow.getAmount();
                    }
                }
            } catch (FlowGenerationException exc) {
                exc.printStackTrace();
            }
            return new SignedAmount(result);
        }

        String buildDivisaComision(Trade trade){
            SecLending secLending= (SecLending) trade.getProduct();
            String currency="";
            try {
                CashFlowSet flows=getFlows(trade,secLending);
                for (CashFlow flow : flows) {
                    if (CashFlow.SECLENDING_FEE.equals(flow.getType())) {
                        currency=Optional.ofNullable(flow.getCashFlowDefinition())
                                .map(FdnCashFlowDefinitionImpl::getCurrency).orElse(flow.getCurrency());
                        break;
                    }
                }
            } catch (FlowGenerationException exc) {
                exc.printStackTrace();
            }
            return currency;
        }

        CashFlowSet getFlows(Trade trade, SecLending secLending) throws FlowGenerationException {
            CashFlowSet flows=secLending.getFlows(trade, JDate.getNow(), true, -1, true);
            secLending.calculateAll(flows, PricingEnv.loadPE("OFFICIAL",JDate.getNow().getJDatetime()),JDate.getNow());
            return flows;
        }
    }
}
