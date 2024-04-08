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
 * <p>Classe Java pour clearingDataType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="clearingDataType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="commonClearingData" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}commonClearingDataType"/>
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
@XmlType(name = "clearingDataType", propOrder = {
    "commonClearingData",
    "legClearingGrp"
})
public class ClearingDataType {

    @XmlElement(required = true)
    protected CommonClearingDataType commonClearingData;
    @XmlElement(required = true)
    protected LegClearingGrpType legClearingGrp;

    /**
     * Obtient la valeur de la propri?t? commonClearingData.
     * 
     * @return
     *     possible object is
     *     {@link CommonClearingDataType }
     *     
     */
    public CommonClearingDataType getCommonClearingData() {
        return commonClearingData;
    }

    /**
     * D?finit la valeur de la propri?t? commonClearingData.
     * 
     * @param value
     *     allowed object is
     *     {@link CommonClearingDataType }
     *     
     */
    public void setCommonClearingData(CommonClearingDataType value) {
        this.commonClearingData = value;
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
