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
 * <p>Classe Java pour worstCaseScenarioType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="worstCaseScenarioType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="19"/>
 *     &lt;enumeration value="PriceUpVolaUp      "/>
 *     &lt;enumeration value="PriceUpVolaNeut    "/>
 *     &lt;enumeration value="PriceUpVolaDown    "/>
 *     &lt;enumeration value="PriceDownVolaUp    "/>
 *     &lt;enumeration value="PriceDownVolaNeut  "/>
 *     &lt;enumeration value="PriceDownVolaDown  "/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "worstCaseScenarioType")
@XmlEnum
public enum WorstCaseScenarioType {

    @XmlEnumValue("PriceUpVolaUp      ")
    PRICE_UP_VOLA_UP("PriceUpVolaUp      "),
    @XmlEnumValue("PriceUpVolaNeut    ")
    PRICE_UP_VOLA_NEUT("PriceUpVolaNeut    "),
    @XmlEnumValue("PriceUpVolaDown    ")
    PRICE_UP_VOLA_DOWN("PriceUpVolaDown    "),
    @XmlEnumValue("PriceDownVolaUp    ")
    PRICE_DOWN_VOLA_UP("PriceDownVolaUp    "),
    @XmlEnumValue("PriceDownVolaNeut  ")
    PRICE_DOWN_VOLA_NEUT("PriceDownVolaNeut  "),
    @XmlEnumValue("PriceDownVolaDown  ")
    PRICE_DOWN_VOLA_DOWN("PriceDownVolaDown  ");
    private final String value;

    WorstCaseScenarioType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static WorstCaseScenarioType fromValue(String v) {
        for (WorstCaseScenarioType c: WorstCaseScenarioType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
