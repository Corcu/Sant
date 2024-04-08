//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour fulfillIdType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="fulfillIdType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="6"/>
 *     &lt;enumeration value="AMM"/>
 *     &lt;enumeration value="ADM"/>
 *     &lt;enumeration value="DMM"/>
 *     &lt;enumeration value="RMM"/>
 *     &lt;enumeration value="PMM"/>
 *     &lt;enumeration value="PML"/>
 *     &lt;enumeration value="PMS"/>
 *     &lt;enumeration value="FAILED"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "fulfillIdType")
@XmlEnum
public enum FulfillIdType {

    AMM,
    ADM,
    DMM,
    RMM,
    PMM,
    PML,
    PMS,
    FAILED;

    public String value() {
        return name();
    }

    public static FulfillIdType fromValue(String v) {
        return valueOf(v);
    }

}
