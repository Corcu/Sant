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
 * <p>Classe Java pour cntrIdGrpRType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cntrIdGrpRType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cntrClasCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrClasCodType" minOccurs="0"/>
 *         &lt;element name="prodId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}prodIdType"/>
 *         &lt;element name="cntrDtlGrpR" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrDtlGrpRType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cntrIdGrpRType", propOrder = {
    "cntrClasCod",
    "prodId",
    "cntrDtlGrpR"
})
public class CntrIdGrpRType {

    protected CntrClasCodType cntrClasCod;
    @XmlElement(required = true)
    protected String prodId;
    @XmlElement(required = true)
    protected CntrDtlGrpRType cntrDtlGrpR;

    /**
     * Obtient la valeur de la propri?t? cntrClasCod.
     * 
     * @return
     *     possible object is
     *     {@link CntrClasCodType }
     *     
     */
    public CntrClasCodType getCntrClasCod() {
        return cntrClasCod;
    }

    /**
     * D?finit la valeur de la propri?t? cntrClasCod.
     * 
     * @param value
     *     allowed object is
     *     {@link CntrClasCodType }
     *     
     */
    public void setCntrClasCod(CntrClasCodType value) {
        this.cntrClasCod = value;
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
     * Obtient la valeur de la propri?t? cntrDtlGrpR.
     * 
     * @return
     *     possible object is
     *     {@link CntrDtlGrpRType }
     *     
     */
    public CntrDtlGrpRType getCntrDtlGrpR() {
        return cntrDtlGrpR;
    }

    /**
     * D?finit la valeur de la propri?t? cntrDtlGrpR.
     * 
     * @param value
     *     allowed object is
     *     {@link CntrDtlGrpRType }
     *     
     */
    public void setCntrDtlGrpR(CntrDtlGrpRType value) {
        this.cntrDtlGrpR = value;
    }

}
