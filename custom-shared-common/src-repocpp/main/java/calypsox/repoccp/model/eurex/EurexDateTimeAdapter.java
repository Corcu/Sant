package calypsox.repoccp.model.eurex;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class EurexDateTimeAdapter   extends XmlAdapter<String, XMLGregorianCalendar> {

    private static final String CUSTOM_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.SS";

    @Override
    public String marshal(XMLGregorianCalendar xmlGregorianCalendar) {
        return new SimpleDateFormat(CUSTOM_FORMAT_STRING).format(xmlGregorianCalendar.toGregorianCalendar().getTime());
    }

    @Override
    public XMLGregorianCalendar unmarshal(String v) throws ParseException {
        Date dv = new SimpleDateFormat(CUSTOM_FORMAT_STRING).parse(v);
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(dv );
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException e) {
            throw new ParseException("Failed to create instance of XMLGregorianCalendar", 0);
        }

    }

}
