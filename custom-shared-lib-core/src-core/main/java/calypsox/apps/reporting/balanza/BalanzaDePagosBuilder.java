package calypsox.apps.reporting.balanza;

import calypsox.tk.util.bean.ExternalBalanzaBean;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.SimpleDateFormat;

public class BalanzaDePagosBuilder {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYYmm");

    public String buildLine(String nifEmisor, ExternalBalanzaBean bean)  throws  Exception {

        String instrumento = bean.getInstrumento();
        if (!Util.isEmpty(instrumento)
                && instrumento.length() <2) {
            instrumento = "0".concat(instrumento);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SUSI");
        sb.append(nifEmisor);
        sb.append(bean.getPeriodoStr());
        sb.append("1B");
        sb.append(formatStringWithBlankOnRight(instrumento, 2 ));
        sb.append(formatStringWithBlankOnRight(bean.getIsin(), 12 ));
        String epigrafe = bean.getEpigrafe();
        if (Util.isEmpty(epigrafe)) {
            epigrafe = "5100";
        }
        sb.append(formatStringWithBlankOnRight(epigrafe, 4));

        sb.append(formatStringWithBlankOnRight("", 9));
        sb.append(formatStringWithBlankOnRight("", 2));
        sb.append(formatStringWithBlankOnRight(bean.getNif_emmisor(), 9));
        sb.append(formatStringWithBlankOnRight("P", 1));
        sb.append(formatUnsignedNumber(bean.getSi_nominal(), 15,2 , ""));
        sb.append(formatStringWithBlankOnRight("P", 1));
        sb.append(formatUnsignedNumber(bean.getSi_valoracion(), 17,2 , ""));
        sb.append(formatUnsignedNumber(0, 15,0,""));
        sb.append(formatUnsignedNumber(0, 15,0, ""));
        sb.append(formatUnsignedNumber(bean.getEntrada_nominal(), 15,2 , ""));
        sb.append(formatUnsignedNumber(bean.getEntrada_valoracion(), 17,2 , ""));
        sb.append(formatUnsignedNumber(bean.getSalida_nominal(), 15,2 , ""));
        sb.append(formatUnsignedNumber(bean.getSalida_valoracion(), 17,2 , ""));
        sb.append(formatStringWithBlankOnRight("P", 1));
        sb.append(formatUnsignedNumber(bean.getCupon_nominal(), 15,2 , ""));
        sb.append(formatStringWithBlankOnRight("P", 1));
        sb.append(formatUnsignedNumber(bean.getCupon_valoracion(), 17,2 , ""));
        sb.append(formatStringWithBlankOnRight("P", 1));
        sb.append(formatUnsignedNumber(bean.getSf_nominal(), 15,2 , ""));
        sb.append(formatStringWithBlankOnRight("P", 1));
        sb.append(formatUnsignedNumber(bean.getSf_valoracion(), 17,2 , ""));
        // normalize tildes
        String value = getStringNormalized(bean);
        sb.append(formatStringWithBlankOnRight(value, 100));
        sb.append(System.lineSeparator());
        return sb.toString();

    }

    private String getStringNormalized(ExternalBalanzaBean bean) {
        String s = Normalizer.normalize(bean.getNombre_isin(), Normalizer.Form.NFD);

        return s.replaceAll("[^\\p{ASCII}(N\u0303)(n\u0303)(\u00A1)(\u00BF)(\u00B0)(U\u0308)(u\u0308)]", "");

    }

    public static String formatUnsignedNumber(final double value,int length,
                                              final int decimals, String separator) {
        if (decimals != 0) {
            if(Util.isEmpty(separator)){
                length = length + 1;
            }
            final String pattern = "%0" + (length) + "." + decimals + "f";
            final DecimalFormatSymbols symbols = new DecimalFormatSymbols();

            return String.format(pattern, Math.abs(value))
                    .replace(symbols.getDecimalSeparator() + "", separator);
        }

        final String pattern = "%0" + (length) + "." + decimals + "f";
        return String.format(pattern, Math.abs(value));
    }

    public static String formatStringWithBlankOnRight(final String value,
                                                      final int length) {
        if(!Util.isEmpty(value)){
            final String pattern = "%-" + length + "." + length + "s";
            return String.format(pattern, value).substring(0, length);
        }
        return formatBlank("",length );
    }


    public static String formatBlank(String value, int length){
        final StringBuilder str = new StringBuilder();

        for (int i = 0; i < length; ++i) {
            str.append(' ');
        }
        return str.toString();
    }

}
