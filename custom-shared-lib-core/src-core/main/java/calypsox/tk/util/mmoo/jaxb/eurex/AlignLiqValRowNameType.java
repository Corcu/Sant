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
 * <p>Classe Java pour alignLiqValRowNameType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="alignLiqValRowNameType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="11"/>
 *     &lt;enumeration value="Liqu Value "/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "alignLiqValRowNameType")
@XmlEnum
public enum AlignLiqValRowNameType {

    @XmlEnumValue("Liqu Value ")
    LIQU_VALUE("Liqu Value ");
    private final String value;

    AlignLiqValRowNameType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AlignLiqValRowNameType fromValue(String v) {
        for (AlignLiqValRowNameType c: AlignLiqValRowNameType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
