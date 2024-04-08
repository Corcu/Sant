//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour spreadUndGrpTblType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="spreadUndGrpTblType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="spreadBidPrice" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}spreadBidPriceType" minOccurs="0"/>
 *         &lt;element name="spreadTickValue" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}spreadTickValueType" minOccurs="0"/>
 *         &lt;element name="spreadAbsPcntCode" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}spreadAbsPcntCodeType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "spreadUndGrpTblType", propOrder = {
    "spreadBidPrice",
    "spreadTickValue",
    "spreadAbsPcntCode"
})
public class SpreadUndGrpTblType {

    protected BigDecimal spreadBidPrice;
    protected BigDecimal spreadTickValue;
    protected SpreadAbsPcntCodeType spreadAbsPcntCode;

    /**
     * Obtient la valeur de la propri?t? spreadBidPrice.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSpreadBidPrice() {
        return spreadBidPrice;
    }

    /**
     * D?finit la valeur de la propri?t? spreadBidPrice.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSpreadBidPrice(BigDecimal value) {
        this.spreadBidPrice = value;
    }

    /**
     * Obtient la valeur de la propri?t? spreadTickValue.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSpreadTickValue() {
        return spreadTickValue;
    }

    /**
     * D?finit la valeur de la propri?t? spreadTickValue.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSpreadTickValue(BigDecimal value) {
        this.spreadTickValue = value;
    }

    /**
     * Obtient la valeur de la propri?t? spreadAbsPcntCode.
     * 
     * @return
     *     possible object is
     *     {@link SpreadAbsPcntCodeType }
     *     
     */
    public SpreadAbsPcntCodeType getSpreadAbsPcntCode() {
        return spreadAbsPcntCode;
    }

    /**
     * D?finit la valeur de la propri?t? spreadAbsPcntCode.
     * 
     * @param value
     *     allowed object is
     *     {@link SpreadAbsPcntCodeType }
     *     
     */
    public void setSpreadAbsPcntCode(SpreadAbsPcntCodeType value) {
        this.spreadAbsPcntCode = value;
    }

}
