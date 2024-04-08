//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour bfrAfrDecType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="bfrAfrDecType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="6"/>
 *     &lt;enumeration value="BEFORE"/>
 *     &lt;enumeration value="AFTER "/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "bfrAfrDecType")
@XmlEnum
public enum BfrAfrDecType {

    BEFORE("BEFORE"),
    @XmlEnumValue("AFTER ")
    AFTER("AFTER ");
    private final String value;

    BfrAfrDecType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static BfrAfrDecType fromValue(String v) {
        for (BfrAfrDecType c: BfrAfrDecType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
