package calypsox.tk.report;



import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.DisplayValue;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Product;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.ReportRow;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.core.SantanderUtil;
import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;


public class EquityContaCarteraReportStyle extends EquityMisPlusCarteraReportStyle {


    private static final long serialVersionUID = 1L;
    public static final String FIXING = "FIXING";
    public static final String EOD_PRICE = "EOD_PRICE";
    public static final String EOD_VALO = "EOD_VALO";
    public static final String LIQ_PRICE = "LIQ_PRICE";
    public static final String LIQ_VALO = "LIQ_VALO";
    public static final String B_VEN_DIV = "B_VEN_DIV";
    public static final String P_VEN_DIV = "P_VEN_DIV";
    public static final String B_VENTAS = "B_VENTAS";
    public static final String P_VENTAS = "P_VENTAS";
    public static final String UNREALIZED_PL_EUR = "UNREALIZED_PL_EUR";
    public static final String STRATEGY = "STRATEGY";
    public static final String CENTRO_CONTABLE = "CENTRO_CONTABLE";
    public static final String EQUITY_NAME = "EQUITY_NAME";

    @SuppressWarnings({ "rawtypes" })
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
        final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
        
        if (columnName.equals(FIXING)) {
        	Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
        	return formatResult(getFixing(product, row, valDate, errors));
        }
        if (columnName.equals(EOD_PRICE)) {
			HashMap<String, Object> defaultProperties = (HashMap<String, Object>)row.getProperty(ReportRow.DEFAULT);
        	return formatRate(defaultProperties.get("Current Quote"), '.');
        }
        if (columnName.equals(EOD_VALO)) {
        	HashMap<String, Object> defaultProperties = (HashMap<String, Object>)row.getProperty(ReportRow.DEFAULT);
        	double quote = Double.valueOf((String)formatRate(defaultProperties.get("Current Quote"), '.'));
        	double quantity = getSuperAmount(row, "Quantity", errors);
            
            return formatResult(quote * quantity);
        }
        if (columnName.equals(LIQ_PRICE)) {
        	HashMap<String, Object> defaultProperties = (HashMap<String, Object>)row.getProperty(ReportRow.DEFAULT);
        	return formatRate(defaultProperties.get("Average Price"), '.');
        }
        if (columnName.equals(LIQ_VALO)) {
//        	double quantity = getSuperAmount(row, "Quantity", errors);
//        	double averagePrice = getSuperAmount(row, "Average Price", errors);
//
//			return formatResult(quantity * averagePrice);
        	return super.getColumnValue(row, "Amount", errors);
        }
        if (columnName.equals(B_VEN_DIV) ||
        		columnName.equals(P_VEN_DIV) ||
        		columnName.equals(B_VENTAS) ||
        		columnName.equals(P_VENTAS)) {
        	Double amount = row.getProperty(columnName);
        	if (amount == null) {
        		amount = 0.0d;
        	}
            return formatResult(amount);
        }
        if (columnName.equals(UNREALIZED_PL_EUR)) {
        	Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
        	double unrealized = getSuperAmount(row, "Unrealized", errors);
        	double fixing = getFixing(product, row, valDate, errors);
        	if (fixing > 0.0) {
        		return formatResult(unrealized / fixing);
        	}
        	return "NULL";
        }
        if (columnName.equals(STRATEGY)) {
        	return row.getProperty(STRATEGY);
        }
        if (columnName.equals(CENTRO_CONTABLE)) {
        	Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
			String bookName = (String)((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get("Book");
			String entity = BOCreUtils.getInstance().getEntity(bookName);
			if (product!=null){
				return BOCreUtils.getInstance().getCentroContable(product, entity, false);
			}
			return "";
        }
        if (columnName.equals(EQUITY_NAME)) {
        	Product	product = (Product)getColumnValue(row, CALYPSO_PRODUCT, errors);
        	return ((Equity)product).getCorporateName();
        }

        return super.getColumnValue(row, columnName, errors);
    }
    
    private double getSuperAmount(ReportRow row, String columnName, Vector errors) {
    	String amountS = (String)super.getColumnValue(row, columnName, errors);
    	if (Util.isEmpty(amountS)) {
    		return 0.0d;
    	}
    	
    	return Double.valueOf(amountS);
    }

    private double getFixing(Product product, ReportRow row, JDate valDate, Vector errors) {
    	String productCurrency = product.getCurrency();
    	
    	if (!productCurrency.equals(SantanderUtil.EUR)) {
    		return CollateralUtilities.getFXRate(valDate, SantanderUtil.EUR, productCurrency);
    	}
    	return 1.0d;
    }

    public Object formatResult(Object o) {
        return UtilReport.formatResult(o, '.');
    }
    
	public static Object formatRate(Object o, char decimalSeparator) {
		if (o == null) {
			return "NULL";
		}
		
		if (o instanceof Number) {
			return formatRate((Number)o, decimalSeparator);
		}
		if (o instanceof DisplayValue) {
			double value = ((DisplayValue)o).get();
			if (Double.isNaN(value) || Double.isInfinite(value)) {
				return "NULL";
			}
			return formatRate(value, decimalSeparator);
		}
		if (o instanceof String){
			if (Util.isEmpty((String)o)) {
				return "NULL";
			}
		    return ((String) o).replace(String.valueOf(decimalSeparator),"").replace(',',decimalSeparator);
        }
		
		return o;
	}
	
	public static Object formatRate(double d, char decimalSeparator) {
		return formatRate(new Double(d), decimalSeparator);
	}
	
	
	public static Object formatRate(Number number, char decimalSeparator) {
		if(number instanceof Double) {
			DecimalFormat df = new DecimalFormat("0.00000");
			df.setGroupingUsed(false);
			DecimalFormatSymbols newSymbols = new DecimalFormatSymbols();
			newSymbols.setDecimalSeparator(decimalSeparator);
			df.setDecimalFormatSymbols(newSymbols);
			return df.format(number);
		}
		return number;
	}
}
