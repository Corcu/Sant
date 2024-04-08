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
 * <p>Classe Java pour prodTypIdType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="prodTypIdType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="4"/>
 *     &lt;enumeration value="FBND"/>
 *     &lt;enumeration value="FCRD"/>
 *     &lt;enumeration value="FCUR"/>
 *     &lt;enumeration value="FENE"/>
 *     &lt;enumeration value="FINT"/>
 *     &lt;enumeration value="FINX"/>
 *     &lt;enumeration value="FSTK"/>
 *     &lt;enumeration value="FVOL"/>
 *     &lt;enumeration value="OCUR"/>
 *     &lt;enumeration value="OFBD"/>
 *     &lt;enumeration value="OFEN"/>
 *     &lt;enumeration value="OFIT"/>
 *     &lt;enumeration value="OFIX"/>
 *     &lt;enumeration value="OFVL"/>
 *     &lt;enumeration value="OINX"/>
 *     &lt;enumeration value="OSTK"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "prodTypIdType")
@XmlEnum
public enum ProdTypIdType {

    FBND,
    FCRD,
    FCUR,
    FENE,
    FINT,
    FINX,
    FSTK,
    FVOL,
    OCUR,
    OFBD,
    OFEN,
    OFIT,
    OFIX,
    OFVL,
    OINX,
    OSTK;

    public String value() {
        return name();
    }

    public static ProdTypIdType fromValue(String v) {
        return valueOf(v);
    }

}
