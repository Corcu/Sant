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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Classe Java pour flxCntrDtlGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="flxCntrDtlGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="prodId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}prodIdType"/>
 *         &lt;element name="cntrExpMthDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrExpMthDatType" minOccurs="0"/>
 *         &lt;element name="cntrExpYrDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrExpYrDatType" minOccurs="0"/>
 *         &lt;element name="cntrExpDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrExpDatType" minOccurs="0"/>
 *         &lt;element name="cntrExerPrc" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrExerPrcType" minOccurs="0"/>
 *         &lt;element name="flxOptCntrExerPrc" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}flxOptCntrExerPrcType" minOccurs="0"/>
 *         &lt;element name="cntrVersNo" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrVersNoType" minOccurs="0"/>
 *         &lt;element name="exerStylTyp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}exerStylTypType" minOccurs="0"/>
 *         &lt;element name="setlTypCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}setlTypCodType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "flxCntrDtlGrpType", propOrder = {
    "prodId",
    "cntrExpMthDat",
    "cntrExpYrDat",
    "cntrExpDat",
    "cntrExerPrc",
    "flxOptCntrExerPrc",
    "cntrVersNo",
    "exerStylTyp",
    "setlTypCod"
})
public class FlxCntrDtlGrpType {

    @XmlElement(required = true)
    protected String prodId;
    protected BigInteger cntrExpMthDat;
    protected BigInteger cntrExpYrDat;
    protected XMLGregorianCalendar cntrExpDat;
    protected BigInteger cntrExerPrc;
    protected BigDecimal flxOptCntrExerPrc;
    protected BigInteger cntrVersNo;
    protected ExerStylTypType exerStylTyp;
    protected SetlTypCodType setlTypCod;

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
     * Obtient la valeur de la propri?t? cntrExpDat.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getCntrExpDat() {
        return cntrExpDat;
    }

    /**
     * D?finit la valeur de la propri?t? cntrExpDat.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setCntrExpDat(XMLGregorianCalendar value) {
        this.cntrExpDat = value;
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
     * Obtient la valeur de la propri?t? flxOptCntrExerPrc.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getFlxOptCntrExerPrc() {
        return flxOptCntrExerPrc;
    }

    /**
     * D?finit la valeur de la propri?t? flxOptCntrExerPrc.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setFlxOptCntrExerPrc(BigDecimal value) {
        this.flxOptCntrExerPrc = value;
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

    /**
     * Obtient la valeur de la propri?t? exerStylTyp.
     * 
     * @return
     *     possible object is
     *     {@link ExerStylTypType }
     *     
     */
    public ExerStylTypType getExerStylTyp() {
        return exerStylTyp;
    }

    /**
     * D?finit la valeur de la propri?t? exerStylTyp.
     * 
     * @param value
     *     allowed object is
     *     {@link ExerStylTypType }
     *     
     */
    public void setExerStylTyp(ExerStylTypType value) {
        this.exerStylTyp = value;
    }

    /**
     * Obtient la valeur de la propri?t? setlTypCod.
     * 
     * @return
     *     possible object is
     *     {@link SetlTypCodType }
     *     
     */
    public SetlTypCodType getSetlTypCod() {
        return setlTypCod;
    }

    /**
     * D?finit la valeur de la propri?t? setlTypCod.
     * 
     * @param value
     *     allowed object is
     *     {@link SetlTypCodType }
     *     
     */
    public void setSetlTypCod(SetlTypCodType value) {
        this.setlTypCod = value;
    }

}
