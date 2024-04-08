//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour stratLegGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="stratLegGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="stratLegInfo" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}stratLegInfoType"/>
 *         &lt;element name="prodId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}prodIdType"/>
 *         &lt;element name="cntrExpMthDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrExpMthDatType"/>
 *         &lt;element name="cntrExpYrDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrExpYrDatType"/>
 *         &lt;element name="cntrExerPrc" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrExerPrcType" minOccurs="0"/>
 *         &lt;element name="cntrVersNo" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrVersNoType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "stratLegGrpType", propOrder = {
    "stratLegInfo",
    "prodId",
    "cntrExpMthDat",
    "cntrExpYrDat",
    "cntrExerPrc",
    "cntrVersNo"
})
public class StratLegGrpType {

    @XmlElement(required = true)
    protected StratLegInfoType stratLegInfo;
    @XmlElement(required = true)
    protected String prodId;
    @XmlElement(required = true)
    protected BigInteger cntrExpMthDat;
    @XmlElement(required = true)
    protected BigInteger cntrExpYrDat;
    protected BigInteger cntrExerPrc;
    protected BigInteger cntrVersNo;

    /**
     * Obtient la valeur de la propri?t? stratLegInfo.
     * 
     * @return
     *     possible object is
     *     {@link StratLegInfoType }
     *     
     */
    public StratLegInfoType getStratLegInfo() {
        return stratLegInfo;
    }

    /**
     * D?finit la valeur de la propri?t? stratLegInfo.
     * 
     * @param value
     *     allowed object is
     *     {@link StratLegInfoType }
     *     
     */
    public void setStratLegInfo(StratLegInfoType value) {
        this.stratLegInfo = value;
    }

    /**
     * Obtient la valeur de la propri?t? prodId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProdId() {
        return prodId;
    }

    /**
     * D?finit la valeur de la propri?t? prodId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProdId(String value) {
        this.prodId = value;
    }

    /**
     * Obtient la valeur de la propri?t? cntrExpMthDat.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCntrExpMthDat() {
        return cntrExpMthDat;
    }

    /**
     * D?finit la valeur de la propri?t? cntrExpMthDat.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCntrExpMthDat(BigInteger value) {
        this.cntrExpMthDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? cntrExpYrDat.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCntrExpYrDat() {
        return cntrExpYrDat;
    }

    /**
     * D?finit la valeur de la propri?t? cntrExpYrDat.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCntrExpYrDat(BigInteger value) {
        this.cntrExpYrDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? cntrExerPrc.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCntrExerPrc() {
        return cntrExerPrc;
    }

    /**
     * D?finit la valeur de la propri?t? cntrExerPrc.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCntrExerPrc(BigInteger value) {
        this.cntrExerPrc = value;
    }

    /**
     * Obtient la valeur de la propri?t? cntrVersNo.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCntrVersNo() {
        return cntrVersNo;
    }

    /**
     * D?finit la valeur de la propri?t? cntrVersNo.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCntrVersNo(BigInteger value) {
        this.cntrVersNo = value;
    }

}
