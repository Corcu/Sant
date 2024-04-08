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
 * <p>Classe Java pour auditKeyGrp2Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditKeyGrp2Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="audtUpdTim" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtUpdTimType"/>
 *         &lt;element name="audtUpdCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtUpdCodType"/>
 *         &lt;element name="audtUpdId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtUpdIdType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditKeyGrp2Type", propOrder = {
    "audtUpdTim",
    "audtUpdCod",
    "audtUpdId"
})
public class AuditKeyGrp2Type {

    @XmlElement(required = true)
    protected XMLGregorianCalendar audtUpdTim;
    @XmlElement(required = true)
    protected AudtUpdCodType audtUpdCod;
    @XmlElement(required = true)
    protected String audtUpdId;

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

    /**
     * Obtient la valeur de la propri?t? audtUpdId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAudtUpdId() {
        return audtUpdId;
    }

    /**
     * D?finit la valeur de la propri?t? audtUpdId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAudtUpdId(String value) {
        this.audtUpdId = value;
    }

}
