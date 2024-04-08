package calypsox.tk.bo.boi;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * @author acd
 */
public class BOIGenericFormatter {

    private static BOIGenericFormatter instance;

    static final JDateFormat format = new JDateFormat("yyyyMMdd");

    public static synchronized BOIGenericFormatter getInstance(){
        if(null!= instance){
            return instance;
        }else {
            return new BOIGenericFormatter();
        }
    }

    public String formatDate(JDate date){
        if(null!=date){
            return format.format(date);
        }
        return "";
    }

    public String formatDecimal(Double value){
        if(null!=value && 0.0!=value){
            try {
                BigDecimal bdValue = BigDecimal.valueOf(Double.valueOf(value));
                String format = String.format(Locale.ENGLISH, "%.2f", bdValue);
                return format.replace(BOIStaticData.POINT,BOIStaticData.COMMA);
            } catch (NumberFormatException e) {
                return "0,00";
            }
        }
        return "0,00";
    }

    public String formatDecimal2(final Double value) {
        if(null!=value && 0.0!=value){
            BigDecimal bdValue = BigDecimal.valueOf(Double.valueOf(value));

            String format = String.format(Locale.ENGLISH, "%.8f", bdValue);
            return format.replace(BOIStaticData.POINT,BOIStaticData.COMMA);
        }
        return "0,00000000";
    }

    public String buildField(Object t){
        return String.valueOf(t);
    }

    public String checkValue(String value) {
        String output = "NA";
        if ( !((value == null) || (value.isEmpty()))) {
            return value;
        }
        return  output;
    }

    public String checkLength(String value) {
        if(!(value == null)){
            if (value.length() > BOIStaticData.CTE_30){
                return value.substring(BOIStaticData.CTE_0,BOIStaticData.CTE_30);
            }
            return value;
        }else{
            return "";
        }

    }

}
