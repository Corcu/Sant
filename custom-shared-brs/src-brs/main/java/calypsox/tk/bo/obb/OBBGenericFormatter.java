package calypsox.tk.bo.obb;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import org.apache.commons.lang.StringUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * @author acd
 */
public class OBBGenericFormatter {

    private static OBBGenericFormatter instance;

    public static synchronized OBBGenericFormatter getInstance(){
        if(null!= instance){
            return instance;
        }else {
            return new OBBGenericFormatter();
        }
    }

    public String formatLeftString(String value, int lenght){
        final String substring = StringUtils.substring(value, 0, lenght);
        return StringUtils.leftPad(substring, lenght, " ");
    }

    public String formatLeftZeroNumber(String value, int numZeroOnLeft){
        return StringUtils.leftPad(value, numZeroOnLeft, "0");
    }

    public String formatDAmounts(String value, int numZeroOnLeft){
        return StringUtils.leftPad(value, numZeroOnLeft, "0");
    }

    public String formatDate(JDate date,String dateFormat){
        JDateFormat format = new JDateFormat(dateFormat);
        return null!=date ? format.format(date) : "";
    }

    public String formatDecimal(Double value){
        if(null!=value && 0.0!=value){
            DecimalFormat decimalFormat = new DecimalFormat("000000000000000.00");
            decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
            decimalFormat.setDecimalSeparatorAlwaysShown(true);
            String format = decimalFormat.format(value);
            return format.replace(".","");
        }
        return formatLeftString("00000000000000000",17);

    }

}
