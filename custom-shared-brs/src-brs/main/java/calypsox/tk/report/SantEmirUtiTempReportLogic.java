package calypsox.tk.report;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.PerformanceSwap;


public class SantEmirUtiTempReportLogic  {
    
	
    private Trade trade;
    private PerformanceSwap product;
    private JDatetime valDatetime;
    private static final String KEYWORD_TEMP_UTI_TRADE_ID = "TempUTITradeId";
    
    
    public SantEmirUtiTempReportLogic(final Trade trade, final JDatetime valDatetime) {
        this.trade = trade;
        this.valDatetime = valDatetime;
        this.product = (PerformanceSwap) this.trade.getProduct(); 
    }
  
    
    /**
     * Fills the item with the logic.
     * 
     * @param item
     *            Item to be filled.
     */
    public void fillItem(final SantEmirUtiTempReportItem item) {
        item.setColumnValue(SantEmirUtiTempReportStyle.TEMPORARY_UTI, getLogicTemporaryUti());
        item.setColumnValue(SantEmirUtiTempReportStyle.PRODUCT_TYPE, getLogicProductType());
        item.setColumnValue(SantEmirUtiTempReportStyle.MATURITY_DATE, getLogicMaturityDate());
        item.setColumnValue(SantEmirUtiTempReportStyle.VALUE_DATE, getLogicValueDate());
        item.setColumnValue(SantEmirUtiTempReportStyle.PRIMARY_LEG_AMOUNT, getLogicPrimaryLegAmount());
        item.setColumnValue(SantEmirUtiTempReportStyle.PRIMARY_LEG_CURRENCY, getLogicPrimaryLegCurrency());
        item.setColumnValue(SantEmirUtiTempReportStyle.SECONDARY_LEG_AMOUNT, getLogicSecondaryLegAmount());
        item.setColumnValue(SantEmirUtiTempReportStyle.SECONDARY_LEG_CURRENCY, getLogicSecondaryLegCurrency());
    }

    
    private String getLogicProductType() {
        return this.trade.getProductType();
    }

    
    private String getLogicTemporaryUti() {
        return this.trade.getKeywordValue(KEYWORD_TEMP_UTI_TRADE_ID);
    }
    
    
    private String getLogicMaturityDate() {
    	SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(this.trade.getMaturityDate().getDate(TimeZone.getDefault()));
    }
    
    
    private String getLogicValueDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(this.valDatetime.getJDate(TimeZone.getDefault()).getDate(TimeZone.getDefault()));
    }

    
    private String getLogicPrimaryLegAmount() {
    	return getFormatAmount(product.getPrimaryLeg().getPrincipal());
    }
    
    
    private String getLogicPrimaryLegCurrency() {
    	return this.product.getPrimaryCurrency();
    }
    
    
    private String getLogicSecondaryLegAmount() {
        return getFormatAmount(this.product.getPrincipal());
    }
    
    
    private String getLogicSecondaryLegCurrency() { 
    	return this.product.getSecondaryLegCurrency();
    }
    
    
    public String getFormatAmount(final Double value) {
        final DecimalFormat myFormatter = new DecimalFormat("###0.00");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        if (value != null) {
            return myFormatter.format(value);
        } else {
            return "";
        }
    }
    
}
