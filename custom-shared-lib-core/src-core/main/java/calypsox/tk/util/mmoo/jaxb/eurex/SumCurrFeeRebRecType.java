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
 * <p>Classe Java pour sumCurrFeeRebRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumCurrFeeRebRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sumCurrSavFeeAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumCurrSavFeeAmntType" minOccurs="0"/>
 *         &lt;element name="sumCurrPctSav" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumCurrPctSavType" minOccurs="0"/>
 *         &lt;element name="sumCurrFeeAmntX" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumCurrFeeAmntXType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumCurrFeeRebRecType", propOrder = {
    "sumCurrSavFeeAmnt",
    "sumCurrPctSav",
    "sumCurrFeeAmntX"
})
public class SumCurrFeeRebRecType {

    protected BigDecimal sumCurrSavFeeAmnt;
    protected BigDecimal sumCurrPctSav;
    protected BigDecimal sumCurrFeeAmntX;

    /**
     * Obtient la valeur de la propri?t? sumCurrSavFeeAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumCurrSavFeeAmnt() {
        return sumCurrSavFeeAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumCurrSavFeeAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumCurrSavFeeAmnt(BigDecimal value) {
        this.sumCurrSavFeeAmnt = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumCurrPctSav.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumCurrPctSav() {
        return sumCurrPctSav;
    }

    /**
     * D?finit la valeur de la propri?t? sumCurrPctSav.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumCurrPctSav(BigDecimal value) {
        this.sumCurrPctSav = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumCurrFeeAmntX.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumCurrFeeAmntX() {
        return sumCurrFeeAmntX;
    }

    /**
     * D?finit la valeur de la propri?t? sumCurrFeeAmntX.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumCurrFeeAmntX(BigDecimal value) {
        this.sumCurrFeeAmntX = value;
    }

}
