package calypsox.tk.confirmation.model.jaxb;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import java.time.format.DateTimeFormatter;

/**
 * @author aalonsop
 * Host demands fixed width formatted columns
 */
public class CalypsoConfirmationXmlField {

    static final DateTimeFormatter datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    String name;

    String value;

    /**
     * JAXB Needed
     */
    public CalypsoConfirmationXmlField() {
        //Empty
    }

    public CalypsoConfirmationXmlField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @XmlValue
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
