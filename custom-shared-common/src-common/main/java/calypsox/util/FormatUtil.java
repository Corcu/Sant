package calypsox.util;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

public class FormatUtil {

    /**
     * @param date
     * @param dateFormat
     * @return
     */
    public static String formatDate(JDate date,String dateFormat){
        final Optional<JDate> optionalJDate = Optional.ofNullable(date);
        if(optionalJDate.isPresent()){
            JDateFormat format = new JDateFormat(dateFormat);
            return format.format(date);
        }
        return null;
    }

    /**
     * 15 integer + "." + 2 decimals
     * @param value
     * @return
     */
    public static String formatAmount(final Double value) {
        final DecimalFormat myFormatter = new DecimalFormat("000000000000000.00");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        if (value != null) {
            return myFormatter.format(Math.abs(value));
        } else {
            return "";
        }
    }

    public static String formatRate(final Double value) {
        final DecimalFormat myFormatter = new DecimalFormat("#.000000");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        if (value != null) {
            return myFormatter.format(Math.abs(value));
        } else {
            return "";
        }
    }

    public static String formatRate(String value) {
        if(!Util.isEmpty(value) && value.contains(",")){
            return value.replace(",",".");
        }
        return value;
    }


    /**
     * @param value
     * @param lenght
     * @return
     */
    public static String splitString(Object value, int lenght){
        final Optional<Object> object = Optional.ofNullable(value);
        String result = "";
        if(object.isPresent()){
            if(object.get() instanceof String ) {
                if(((String) object.get()).length()>=lenght){
                    result = ((String) object.get()).substring(0, lenght);
                }else{
                    result = (String)object.get();
                }
            }else {
                try {
                    final String stringObject = String.valueOf(object.get());
                    if(stringObject.length()>=lenght){
                        result = stringObject.substring(0, lenght);
                    }else{
                        result = stringObject;
                    }
                }catch (Exception e){
                    Log.error(FormatUtil.class.getName(),"Error converting to String: " + e.getCause());
                }
            }
        }
        return result;
    }

	/**
	 *
	 * @param value
	 * @param nDecimals
	 * @return
	 */
	public static String formatAmount(final Double value, int nDecimals) {
		if (value != null) {
			return String.format(Locale.ENGLISH, "%." + nDecimals + "f", value);
		}
		return "";

	}

}
