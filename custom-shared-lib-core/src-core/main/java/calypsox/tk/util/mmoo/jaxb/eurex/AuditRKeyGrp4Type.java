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
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Classe Java pour auditRKeyGrp4Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditRKeyGrp4Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="audtRUpdId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtRUpdIdType"/>
 *         &lt;element name="audtUpdDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtUpdDatType"/>
 *         &lt;element name="audtUpdTim" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtUpdTimType"/>
 *         &lt;element name="audtRApprId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtRApprIdType" minOccurs="0"/>
 *         &lt;element name="audtApprDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtApprDatType" minOccurs="0"/>
 *         &lt;element name="audtApprTim" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtApprTimType" minOccurs="0"/>
 *         &lt;element name="audtUpdCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtUpdCodType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditRKeyGrp4Type", propOrder = {
    "audtRUpdId",
    "audtUpdDat",
    "audtUpdTim",
    "audtRApprId",
    "audtApprDat",
    "audtApprTim",
    "audtUpdCod"
})
public class AuditRKeyGrp4Type {

    @XmlElement(required = true)
    protected String audtRUpdId;
    @XmlElement(required = true)
    protected XMLGregorianCalendar audtUpdDat;
    @XmlElement(required = true)
    protected XMLGregorianCalendar audtUpdTim;
    protected String audtRApprId;
    protected XMLGregorianCalendar audtApprDat;
    protected XMLGregorianCalendar audtApprTim;
    @XmlElement(required = true)
    protected AudtUpdCodType audtUpdCod;

    /**
     * Obtient la valeur de la propri?t? audtRUpdId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAudtRUpdId() {
        return audtRUpdId;
    }

    /**
     * D?finit la valeur de la propri?t? audtRUpdId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAudtRUpdId(String value) {
        this.audtRUpdId = value;
    }

    /**
     * Obtient la valeur de la propri?t? audtUpdDat.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getAudtUpdDat() {
        return audtUpdDat;
    }

    /**
     * D?finit la valeur de la propri?t? audtUpdDat.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setAudtUpdDat(XMLGregorianCalendar value) {
        this.audtUpdDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? audtUpdTim.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getAudtUpdTim() {
        return audtUpdTim;
    }

    /**
     * D?finit la valeur de la propri?t? audtUpdTim.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setAudtUpdTim(XMLGregorianCalendar value) {
        this.audtUpdTim = value;
    }

    /**
     * Obtient la valeur de la propri?t? audtRApprId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAudtRApprId() {
        return audtRApprId;
    }

    /**
     * D?finit la valeur de la propri?t? audtRApprId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAudtRApprId(String value) {
        this.audtRApprId = value;
    }

    /**
     * Obtient la valeur de la propri?t? audtApprDat.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getAudtApprDat() {
        return audtApprDat;
    }

    /**
     * D?finit la valeur de la propri?t? audtApprDat.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setAudtApprDat(XMLGregorianCalendar value) {
        this.audtApprDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? audtApprTim.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getAudtApprTim() {
        return audtApprTim;
    }

    /**
     * D?finit la valeur de la propri?t? audtApprTim.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setAudtApprTim(XMLGregorianCalendar value) {
        this.audtApprTim = value;
    }

    /**
     * Obtient la valeur de la propri?t? audtUpdCod.
     * 
     * @return
     *     possible object is
     *     {@link AudtUpdCodType }
     *     
     */
    public AudtUpdCodType getAudtUpdCod() {
        return audtUpdCod;
    }

    /**
     * D?finit la valeur de la propri?t? audtUpdCod.
     * 
     * @param value
     *     allowed object is
     *     {@link AudtUpdCodType }
     *     
     */
    public void setAudtUpdCod(AudtUpdCodType value) {
        this.audtUpdCod = value;
    }

}
