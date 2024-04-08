//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour auditRGrp4Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditRGrp4Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="auditRKeyGrp4" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRKeyGrp4Type"/>
 *         &lt;element name="auditRRec" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRRecType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditRGrp4Type", propOrder = {
    "auditRKeyGrp4",
    "auditRRecs"
})
public class AuditRGrp4Type {

    @XmlElement(required = true)
    protected AuditRKeyGrp4Type auditRKeyGrp4;
    @XmlElement(name = "auditRRec", required = true)
    protected List<AuditRRecType> auditRRecs;

    /**
     * Obtient la valeur de la propri?t? auditRKeyGrp4.
     * 
     * @return
     *     possible object is
     *     {@link AuditRKeyGrp4Type }
     *     
     */
    public AuditRKeyGrp4Type getAuditRKeyGrp4() {
        return auditRKeyGrp4;
    }

    /**
     * D?finit la valeur de la propri?t? auditRKeyGrp4.
     * 
     * @param value
     *     allowed object is
     *     {@link AuditRKeyGrp4Type }
     *     
     */
    public void setAuditRKeyGrp4(AuditRKeyGrp4Type value) {
        this.auditRKeyGrp4 = value;
    }

    /**
     * Gets the value of the auditRRecs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditRRecs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditRRecs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AuditRRecType }
     * 
     * 
     */
    public List<AuditRRecType> getAuditRRecs() {
        if (auditRRecs == null) {
            auditRRecs = new ArrayList<AuditRRecType>();
        }
        return this.auditRRecs;
    }

}
