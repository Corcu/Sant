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
 * <p>Classe Java pour auditRGrp3Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditRGrp3Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="auditRKeyGrp3" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRKeyGrp3Type"/>
 *         &lt;element name="auditRGrp4" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRGrp4Type" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditRGrp3Type", propOrder = {
    "auditRKeyGrp3",
    "auditRGrp4S"
})
public class AuditRGrp3Type {

    @XmlElement(required = true)
    protected AuditRKeyGrp3Type auditRKeyGrp3;
    @XmlElement(name = "auditRGrp4", required = true)
    protected List<AuditRGrp4Type> auditRGrp4S;

    /**
     * Obtient la valeur de la propri?t? auditRKeyGrp3.
     * 
     * @return
     *     possible object is
     *     {@link AuditRKeyGrp3Type }
     *     
     */
    public AuditRKeyGrp3Type getAuditRKeyGrp3() {
        return auditRKeyGrp3;
    }

    /**
     * D?finit la valeur de la propri?t? auditRKeyGrp3.
     * 
     * @param value
     *     allowed object is
     *     {@link AuditRKeyGrp3Type }
     *     
     */
    public void setAuditRKeyGrp3(AuditRKeyGrp3Type value) {
        this.auditRKeyGrp3 = value;
    }

    /**
     * Gets the value of the auditRGrp4S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditRGrp4S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditRGrp4s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AuditRGrp4Type }
     * 
     * 
     */
    public List<AuditRGrp4Type> getAuditRGrp4s() {
        if (auditRGrp4S == null) {
            auditRGrp4S = new ArrayList<AuditRGrp4Type>();
        }
        return this.auditRGrp4S;
    }

}
