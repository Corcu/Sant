package calypsox.tk.bo.acyg;


import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import org.apache.commons.lang.StringUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class BalancesPostingFormatter {

    private static BalancesPostingFormatter instance;

    public static synchronized BalancesPostingFormatter getInstance(){
        if(null!= instance){
            return instance;
        }else {
            return new BalancesPostingFormatter();
        }
    }

    public String formatLeftString(String value, int lenght){
        final String substring = StringUtils.substring(value, 0, lenght);
        return StringUtils.leftPad(substring, lenght, " ");
    }

    public String formatDecimal(Double value){
        if(null!=value && 0.0!=value){
            DecimalFormat decimalFormat = new DecimalFormat("000000000000000.00");
            decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.forLanguageTag("ES")));
            decimalFormat.setDecimalSeparatorAlwaysShown(true);
            String format = decimalFormat.format(value);
            return format;
        }
        return formatLeftString("000000000000000,00",17);

    }

}
