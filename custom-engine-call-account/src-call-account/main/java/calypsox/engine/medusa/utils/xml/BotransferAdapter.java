//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.02.03 at 11:18:08 AM CET
//

package calypsox.engine.medusa.utils.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Adapter class
 *
 * @author xIS15793
 */
public class BotransferAdapter extends XmlAdapter<String, Double> {

    @Override
    public Double unmarshal(final String value) {
        return (parseDouble(value));
    }

    @Override
    public String marshal(final Double value) {
        return (printDouble(value));
    }


    //Utils
    public String printDouble(final Double value) {
        final NumberFormat nf = NumberFormat.getInstance(Locale.UK);
        nf.setMaximumFractionDigits(6);
        nf.setGroupingUsed(false);
        return nf.format(value);
    }

    /**
     * parse double
     *
     * @param value value to parse
     * @return value parsed
     */
    public Double parseDouble(final String value) {
        return Double.valueOf(value);
    }

}
