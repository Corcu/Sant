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
 * <p>Classe Java pour sumFeeTotalAmntType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumFeeTotalAmntType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sumAggQty" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumAggQtyType" minOccurs="0"/>
 *         &lt;element name="sumNomQty" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumNomQtyType" minOccurs="0"/>
 *         &lt;element name="sumSetlAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumSetlAmntType" minOccurs="0"/>
 *         &lt;element name="sumFee" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumFeeType" minOccurs="0"/>
 *         &lt;element name="sumFeeVar" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumFeeVarType" minOccurs="0"/>
 *         &lt;element name="sumFeeTotal" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumFeeTotalType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumFeeTotalAmntType", propOrder = {
    "sumAggQty",
    "sumNomQty",
    "sumSetlAmnt",
    "sumFee",
    "sumFeeVar",
    "sumFeeTotal"
})
public class SumFeeTotalAmntType {

    protected BigInteger sumAggQty;
    protected BigDecimal sumNomQty;
    protected BigDecimal sumSetlAmnt;
    protected BigDecimal sumFee;
    protected BigDecimal sumFeeVar;
    protected BigDecimal sumFeeTotal;

    /**
     * Obtient la valeur de la propri?t? sumAggQty.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSumAggQty() {
        return sumAggQty;
    }

    /**
     * D?finit la valeur de la propri?t? sumAggQty.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSumAggQty(BigInteger value) {
        this.sumAggQty = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumNomQty.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumNomQty() {
        return sumNomQty;
    }

    /**
     * D?finit la valeur de la propri?t? sumNomQty.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumNomQty(BigDecimal value) {
        this.sumNomQty = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumSetlAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumSetlAmnt() {
        return sumSetlAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumSetlAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumSetlAmnt(BigDecimal value) {
        this.sumSetlAmnt = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumFee.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumFee() {
        return sumFee;
    }

    /**
     * D?finit la valeur de la propri?t? sumFee.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumFee(BigDecimal value) {
        this.sumFee = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumFeeVar.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumFeeVar() {
        return sumFeeVar;
    }

    /**
     * D?finit la valeur de la propri?t? sumFeeVar.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumFeeVar(BigDecimal value) {
        this.sumFeeVar = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumFeeTotal.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumFeeTotal() {
        return sumFeeTotal;
    }

    /**
     * D?finit la valeur de la propri?t? sumFeeTotal.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumFeeTotal(BigDecimal value) {
        this.sumFeeTotal = value;
    }

}
