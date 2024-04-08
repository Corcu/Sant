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
 * <p>Classe Java pour exchIdCodType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="exchIdCodType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="4"/>
 *     &lt;enumeration value="XEUR"/>
 *     &lt;enumeration value="XEEX"/>
 *     &lt;enumeration value="XETR"/>
 *     &lt;enumeration value="XFRA"/>
 *     &lt;enumeration value="XSWX"/>
 *     &lt;enumeration value="XEUB"/>
 *     &lt;enumeration value="XISX"/>
 *     &lt;enumeration value="XEPD"/>
 *     &lt;enumeration value="XEEM"/>
 *     &lt;enumeration value="XEGX"/>
 *     &lt;enumeration value="XKFE"/>
 *     &lt;enumeration value="XTAF"/>
 *     &lt;enumeration value="MKTW"/>
 *     &lt;enumeration value="XEUM"/>
 *     &lt;enumeration value="PIRM"/>
 *     &lt;enumeration value="SLXT"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "exchIdCodType")
@XmlEnum
public enum ExchIdCodType {

    XEUR,
    XEEX,
    XETR,
    XFRA,
    XSWX,
    XEUB,
    XISX,
    XEPD,
    XEEM,
    XEGX,
    XKFE,
    XTAF,
    MKTW,
    XEUM,
    PIRM,
    SLXT;

    public String value() {
        return name();
    }

    public static ExchIdCodType fromValue(String v) {
        return valueOf(v);
    }

}
