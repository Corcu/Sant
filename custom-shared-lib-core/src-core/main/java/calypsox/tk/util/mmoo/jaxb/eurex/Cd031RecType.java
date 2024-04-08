//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:11:42 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour cd031RecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cd031RecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="isinCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}isinCodType"/>
 *         &lt;element name="secuId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}secuIdType" minOccurs="0"/>
 *         &lt;element name="secuBlkCollQty" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}secuBlkCollQtyType"/>
 *         &lt;element name="unUsedSecQty" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}unUsedSecQtyType"/>
 *         &lt;element name="secuLstClsPrc" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}secuLstClsPrcType" minOccurs="0"/>
 *         &lt;element name="secuMktVal" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}secuMktValType"/>
 *         &lt;element name="secuEvalPct" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}secuEvalPctType"/>
 *         &lt;element name="secuCollVal" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}secuCollValType"/>
 *         &lt;element name="csdId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}csdIdType" minOccurs="0"/>
 *         &lt;element name="membCsdAct" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}membCsdActType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cd031RecType", propOrder = {
    "isinCod",
    "secuId",
    "secuBlkCollQty",
    "unUsedSecQty",
    "secuLstClsPrc",
    "secuMktVal",
    "secuEvalPct",
    "secuCollVal",
    "csdId",
    "membCsdAct"
})
public class Cd031RecType {

    @XmlElement(required = true)
    protected String isinCod;
    protected String secuId;
    @XmlElement(required = true)
    protected BigDecimal secuBlkCollQty;
    @XmlElement(required = true)
    protected BigDecimal unUsedSecQty;
    protected BigDecimal secuLstClsPrc;
    @XmlElement(required = true)
    protected BigInteger secuMktVal;
    @XmlElement(required = true)
    protected BigDecimal secuEvalPct;
    @XmlElement(required = true)
    protected BigInteger secuCollVal;
    protected String csdId;
    protected String membCsdAct;

    /**
     * Obtient la valeur de la propri?t? isinCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIsinCod() {
        return isinCod;
    }

    /**
     * D?finit la valeur de la propri?t? isinCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIsinCod(String value) {
        this.isinCod = value;
    }

    /**
     * Obtient la valeur de la propri?t? secuId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecuId() {
        return secuId;
    }

    /**
     * D?finit la valeur de la propri?t? secuId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecuId(String value) {
        this.secuId = value;
    }

    /**
     * Obtient la valeur de la propri?t? secuBlkCollQty.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSecuBlkCollQty() {
        return secuBlkCollQty;
    }

    /**
     * D?finit la valeur de la propri?t? secuBlkCollQty.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSecuBlkCollQty(BigDecimal value) {
        this.secuBlkCollQty = value;
    }

    /**
     * Obtient la valeur de la propri?t? unUsedSecQty.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getUnUsedSecQty() {
        return unUsedSecQty;
    }

    /**
     * D?finit la valeur de la propri?t? unUsedSecQty.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setUnUsedSecQty(BigDecimal value) {
        this.unUsedSecQty = value;
    }

    /**
     * Obtient la valeur de la propri?t? secuLstClsPrc.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSecuLstClsPrc() {
        return secuLstClsPrc;
    }

    /**
     * D?finit la valeur de la propri?t? secuLstClsPrc.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSecuLstClsPrc(BigDecimal value) {
        this.secuLstClsPrc = value;
    }

    /**
     * Obtient la valeur de la propri?t? secuMktVal.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSecuMktVal() {
        return secuMktVal;
    }

    /**
     * D?finit la valeur de la propri?t? secuMktVal.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSecuMktVal(BigInteger value) {
        this.secuMktVal = value;
    }

    /**
     * Obtient la valeur de la propri?t? secuEvalPct.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSecuEvalPct() {
        return secuEvalPct;
    }

    /**
     * D?finit la valeur de la propri?t? secuEvalPct.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSecuEvalPct(BigDecimal value) {
        this.secuEvalPct = value;
    }

    /**
     * Obtient la valeur de la propri?t? secuCollVal.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSecuCollVal() {
        return secuCollVal;
    }

    /**
     * D?finit la valeur de la propri?t? secuCollVal.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSecuCollVal(BigInteger value) {
        this.secuCollVal = value;
    }

    /**
     * Obtient la valeur de la propri?t? csdId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCsdId() {
        return csdId;
    }

    /**
     * D?finit la valeur de la propri?t? csdId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCsdId(String value) {
        this.csdId = value;
    }

    /**
     * Obtient la valeur de la propri?t? membCsdAct.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMembCsdAct() {
        return membCsdAct;
    }

    /**
     * D?finit la valeur de la propri?t? membCsdAct.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMembCsdAct(String value) {
        this.membCsdAct = value;
    }

}
