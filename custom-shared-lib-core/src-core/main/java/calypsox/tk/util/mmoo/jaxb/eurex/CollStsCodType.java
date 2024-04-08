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
 * <p>Classe Java pour collStsCodType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="collStsCodType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="2"/>
 *     &lt;enumeration value="PA"/>
 *     &lt;enumeration value="BR"/>
 *     &lt;enumeration value="CF"/>
 *     &lt;enumeration value="DL"/>
 *     &lt;enumeration value="PO"/>
 *     &lt;enumeration value="PE"/>
 *     &lt;enumeration value="BL"/>
 *     &lt;enumeration value="PC"/>
 *     &lt;enumeration value="RJ"/>
 *     &lt;enumeration value="EX"/>
 *     &lt;enumeration value="CP"/>
 *     &lt;enumeration value="IC"/>
 *     &lt;enumeration value="ER"/>
 *     &lt;enumeration value="SC"/>
 *     &lt;enumeration value="PX"/>
 *     &lt;enumeration value="BX"/>
 *     &lt;enumeration value="BC"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "collStsCodType")
@XmlEnum
public enum CollStsCodType {

    PA,
    BR,
    CF,
    DL,
    PO,
    PE,
    BL,
    PC,
    RJ,
    EX,
    CP,
    IC,
    ER,
    SC,
    PX,
    BX,
    BC;

    public String value() {
        return name();
    }

    public static CollStsCodType fromValue(String v) {
        return valueOf(v);
    }

}
