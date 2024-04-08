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
 * <p>Classe Java pour auditGrp2Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditGrp2Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="auditKeyGrp2" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditKeyGrp2Type"/>
 *         &lt;element name="auditRec" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRecType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditGrp2Type", propOrder = {
    "auditKeyGrp2",
    "auditRecs"
})
public class AuditGrp2Type {

    @XmlElement(required = true)
    protected AuditKeyGrp2Type auditKeyGrp2;
    @XmlElement(name = "auditRec", required = true)
    protected List<AuditRecType> auditRecs;

    /**
     * Obtient la valeur de la propri?t? auditKeyGrp2.
     * 
     * @return
     *     possible object is
     *     {@link AuditKeyGrp2Type }
     *     
     */
    public AuditKeyGrp2Type getAuditKeyGrp2() {
        return auditKeyGrp2;
    }

    /**
     * D?finit la valeur de la propri?t? auditKeyGrp2.
     * 
     * @param value
     *     allowed object is
     *     {@link AuditKeyGrp2Type }
     *     
     */
    public void setAuditKeyGrp2(AuditKeyGrp2Type value) {
        this.auditKeyGrp2 = value;
    }

    /**
     * Gets the value of the auditRecs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditRecs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditRecs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AuditRecType }
     * 
     * 
     */
    public List<AuditRecType> getAuditRecs() {
        if (auditRecs == null) {
            auditRecs = new ArrayList<AuditRecType>();
        }
        return this.auditRecs;
    }

}
