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
 * <p>Classe Java pour categoryType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="categoryType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="28"/>
 *     &lt;enumeration value="Algorithmic Trading Engine"/>
 *     &lt;enumeration value="Electronic Eye"/>
 *     &lt;enumeration value="Order Routing System"/>
 *     &lt;enumeration value="Quote Machine"/>
 *     &lt;enumeration value="Trader Development Program"/>
 *     &lt;enumeration value="Trading Engine"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "categoryType")
@XmlEnum
public enum CategoryType {

    @XmlEnumValue("Algorithmic Trading Engine")
    ALGORITHMIC_TRADING_ENGINE("Algorithmic Trading Engine"),
    @XmlEnumValue("Electronic Eye")
    ELECTRONIC_EYE("Electronic Eye"),
    @XmlEnumValue("Order Routing System")
    ORDER_ROUTING_SYSTEM("Order Routing System"),
    @XmlEnumValue("Quote Machine")
    QUOTE_MACHINE("Quote Machine"),
    @XmlEnumValue("Trader Development Program")
    TRADER_DEVELOPMENT_PROGRAM("Trader Development Program"),
    @XmlEnumValue("Trading Engine")
    TRADING_ENGINE("Trading Engine");
    private final String value;

    CategoryType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CategoryType fromValue(String v) {
        for (CategoryType c: CategoryType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
