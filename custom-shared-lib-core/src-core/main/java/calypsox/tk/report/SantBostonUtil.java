package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;

import java.util.TimeZone;
import java.util.Vector;

public class SantBostonUtil {


    private static String DEFAULT_CURRENCY = "USD";

    public static String getDate (JDatetime date){
        String valDate = "";
        if(null!=date){
            JDateFormat format = new JDateFormat("yyyy-MM-dd");
            valDate = format.format(date.getJDate(TimeZone.getDefault()));
        }
        return valDate;
    }

    //TODO get holidays form ST or Template
    public static Double convertToUSD(String origCurrency, Double value, JDatetime date, PricingEnv env){
        Double valueUSD = 0.0;
        JDate valDate = null;
        Vector holidays = new Vector();
        holidays.add("SYSTEM");

        if(null!=date){
            valDate = date.getJDate(TimeZone.getDefault()).addBusinessDays(-1,holidays);
        }
        try {
            valueUSD = CollateralUtilities.convertCurrency(origCurrency, value,DEFAULT_CURRENCY,valDate,env);
        }catch (MarketDataException e){
            Log.warn(SantBostonUtil.class.getName(), "Cannot convert to USD currency: " + e);
        }

        return valueUSD;
    }

    public static Double amountCheckToUSD(MarginCallEntryDTO mcEntryDTO,Double amount,JDatetime date,PricingEnv env) { //checking the amount and convert it to USD
    	String currency = mcEntryDTO.getContractCurrency();
    	if (!(currency.equals("USD"))) {
    		Double valueUSD = convertToUSD(currency, amount,date,env);
    		return valueUSD;
    	}
    	return amount;
    }

    public static Double amountCheckToUSD(String currency,Double amount,JDatetime date,PricingEnv env) { //checking the amount and convert it to USD
        if (!"USD".equalsIgnoreCase(currency)){
            Double valueUSD = convertToUSD(currency, amount,date,env);
            return valueUSD;
        }
        return amount;
    }
    
}
