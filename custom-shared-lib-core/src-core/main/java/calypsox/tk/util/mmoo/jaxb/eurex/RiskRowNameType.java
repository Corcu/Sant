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
 * <p>Classe Java pour riskRowNameType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="riskRowNameType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="8"/>
 *     &lt;enumeration value="Risk    "/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "riskRowNameType")
@XmlEnum
public enum RiskRowNameType {

    @XmlEnumValue("Risk    ")
    RISK("Risk    ");
    private final String value;

    RiskRowNameType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static RiskRowNameType fromValue(String v) {
        for (RiskRowNameType c: RiskRowNameType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
