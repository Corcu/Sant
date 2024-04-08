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
 * <p>Classe Java pour auditRRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditRRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="updtRFldName" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}updtRFldNameType"/>
 *         &lt;element name="auditRValBefore" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRValBeforeType" minOccurs="0"/>
 *         &lt;element name="auditRValAfter" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRValAfterType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditRRecType", propOrder = {
    "updtRFldName",
    "auditRValBefore",
    "auditRValAfter"
})
public class AuditRRecType {

    @XmlElement(required = true)
    protected String updtRFldName;
    protected String auditRValBefore;
    protected String auditRValAfter;

    /**
     * Obtient la valeur de la propri?t? updtRFldName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUpdtRFldName() {
        return updtRFldName;
    }

    /**
     * D?finit la valeur de la propri?t? updtRFldName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUpdtRFldName(String value) {
        this.updtRFldName = value;
    }

    /**
     * Obtient la valeur de la propri?t? auditRValBefore.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAuditRValBefore() {
        return auditRValBefore;
    }

    /**
     * D?finit la valeur de la propri?t? auditRValBefore.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAuditRValBefore(String value) {
        this.auditRValBefore = value;
    }

    /**
     * Obtient la valeur de la propri?t? auditRValAfter.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAuditRValAfter() {
        return auditRValAfter;
    }

    /**
     * D?finit la valeur de la propri?t? auditRValAfter.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAuditRValAfter(String value) {
        this.auditRValAfter = value;
    }

}
