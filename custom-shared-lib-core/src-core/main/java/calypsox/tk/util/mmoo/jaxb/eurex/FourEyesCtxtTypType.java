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
 * <p>Classe Java pour fourEyesCtxtTypType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="fourEyesCtxtTypType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="4"/>
 *     &lt;enumeration value="CHLT"/>
 *     &lt;enumeration value="K020"/>
 *     &lt;enumeration value="MBRS"/>
 *     &lt;enumeration value="MPGA"/>
 *     &lt;enumeration value="MREL"/>
 *     &lt;enumeration value="NPOT"/>
 *     &lt;enumeration value="PGAM"/>
 *     &lt;enumeration value="PGEM"/>
 *     &lt;enumeration value="PRDG"/>
 *     &lt;enumeration value="RLSE"/>
 *     &lt;enumeration value="SLOW"/>
 *     &lt;enumeration value="STPB"/>
 *     &lt;enumeration value="USRS"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "fourEyesCtxtTypType")
@XmlEnum
public enum FourEyesCtxtTypType {

    CHLT("CHLT"),
    @XmlEnumValue("K020")
    K_020("K020"),
    MBRS("MBRS"),
    MPGA("MPGA"),
    MREL("MREL"),
    NPOT("NPOT"),
    PGAM("PGAM"),
    PGEM("PGEM"),
    PRDG("PRDG"),
    RLSE("RLSE"),
    SLOW("SLOW"),
    STPB("STPB"),
    USRS("USRS");
    private final String value;

    FourEyesCtxtTypType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static FourEyesCtxtTypType fromValue(String v) {
        for (FourEyesCtxtTypType c: FourEyesCtxtTypType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
