//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour priceStepTblType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="priceStepTblType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="priceStepCode" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}priceStepCodeType" minOccurs="0"/>
 *         &lt;element name="upperLimitPrice" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}upperLimitPriceType" minOccurs="0"/>
 *         &lt;element name="priceStep" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}priceStepType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "priceStepTblType", propOrder = {
    "priceStepCode",
    "upperLimitPrice",
    "priceStep"
})
public class PriceStepTblType {

    protected BigInteger priceStepCode;
    protected BigDecimal upperLimitPrice;
    protected BigDecimal priceStep;

    /**
     * Obtient la valeur de la propri?t? priceStepCode.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getPriceStepCode() {
        return priceStepCode;
    }

    /**
     * D?finit la valeur de la propri?t? priceStepCode.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setPriceStepCode(BigInteger value) {
        this.priceStepCode = value;
    }

    /**
     * Obtient la valeur de la propri?t? upperLimitPrice.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getUpperLimitPrice() {
        return upperLimitPrice;
    }

    /**
     * D?finit la valeur de la propri?t? upperLimitPrice.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setUpperLimitPrice(BigDecimal value) {
        this.upperLimitPrice = value;
    }

    /**
     * Obtient la valeur de la propri?t? priceStep.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getPriceStep() {
        return priceStep;
    }

    /**
     * D?finit la valeur de la propri?t? priceStep.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setPriceStep(BigDecimal value) {
        this.priceStep = value;
    }

}
