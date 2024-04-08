//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour clearingData1Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="clearingData1Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="commonClearingData1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}commonClearingData1Type"/>
 *         &lt;element name="legClearingGrp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}legClearingGrpType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "clearingData1Type", propOrder = {
    "commonClearingData1",
    "legClearingGrp"
})
public class ClearingData1Type {

    @XmlElement(required = true)
    protected CommonClearingData1Type commonClearingData1;
    @XmlElement(required = true)
    protected LegClearingGrpType legClearingGrp;

    /**
     * Obtient la valeur de la propri?t? commonClearingData1.
     * 
     * @return
     *     possible object is
     *     {@link CommonClearingData1Type }
     *     
     */
    public CommonClearingData1Type getCommonClearingData1() {
        return commonClearingData1;
    }

    /**
     * D?finit la valeur de la propri?t? commonClearingData1.
     * 
     * @param value
     *     allowed object is
     *     {@link CommonClearingData1Type }
     *     
     */
    public void setCommonClearingData1(CommonClearingData1Type value) {
        this.commonClearingData1 = value;
    }

    /**
     * Obtient la valeur de la propri?t? legClearingGrp.
     * 
     * @return
     *     possible object is
     *     {@link LegClearingGrpType }
     *     
     */
    public LegClearingGrpType getLegClearingGrp() {
        return legClearingGrp;
    }

    /**
     * D?finit la valeur de la propri?t? legClearingGrp.
     * 
     * @param value
     *     allowed object is
     *     {@link LegClearingGrpType }
     *     
     */
    public void setLegClearingGrp(LegClearingGrpType value) {
        this.legClearingGrp = value;
    }

}
