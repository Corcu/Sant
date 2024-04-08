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
 * <p>Classe Java pour rebTypType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="rebTypType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="30"/>
 *     &lt;enumeration value="Revenue Sharing Programs"/>
 *     &lt;enumeration value="Product Group Rebates"/>
 *     &lt;enumeration value="TDP Champions Group"/>
 *     &lt;enumeration value="Trader Development Program"/>
 *     &lt;enumeration value="Trader Development Rebate"/>
 *     &lt;enumeration value="Trader Develop. Reb. 2010"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "rebTypType")
@XmlEnum
public enum RebTypType {

    @XmlEnumValue("Revenue Sharing Programs")
    REVENUE_SHARING_PROGRAMS("Revenue Sharing Programs"),
    @XmlEnumValue("Product Group Rebates")
    PRODUCT_GROUP_REBATES("Product Group Rebates"),
    @XmlEnumValue("TDP Champions Group")
    TDP_CHAMPIONS_GROUP("TDP Champions Group"),
    @XmlEnumValue("Trader Development Program")
    TRADER_DEVELOPMENT_PROGRAM("Trader Development Program"),
    @XmlEnumValue("Trader Development Rebate")
    TRADER_DEVELOPMENT_REBATE("Trader Development Rebate"),
    @XmlEnumValue("Trader Develop. Reb. 2010")
    TRADER_DEVELOP_REB_2010("Trader Develop. Reb. 2010");
    private final String value;

    RebTypType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static RebTypType fromValue(String v) {
        for (RebTypType c: RebTypType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
