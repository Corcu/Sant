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
 * <p>Classe Java pour auditRGrp2Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditRGrp2Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="auditRKeyGrp2" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRKeyGrp2Type"/>
 *         &lt;element name="auditRGrp3" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRGrp3Type" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditRGrp2Type", propOrder = {
    "auditRKeyGrp2",
    "auditRGrp3S"
})
public class AuditRGrp2Type {

    @XmlElement(required = true)
    protected AuditRKeyGrp2Type auditRKeyGrp2;
    @XmlElement(name = "auditRGrp3", required = true)
    protected List<AuditRGrp3Type> auditRGrp3S;

    /**
     * Obtient la valeur de la propri?t? auditRKeyGrp2.
     * 
     * @return
     *     possible object is
     *     {@link AuditRKeyGrp2Type }
     *     
     */
    public AuditRKeyGrp2Type getAuditRKeyGrp2() {
        return auditRKeyGrp2;
    }

    /**
     * D?finit la valeur de la propri?t? auditRKeyGrp2.
     * 
     * @param value
     *     allowed object is
     *     {@link AuditRKeyGrp2Type }
     *     
     */
    public void setAuditRKeyGrp2(AuditRKeyGrp2Type value) {
        this.auditRKeyGrp2 = value;
    }

    /**
     * Gets the value of the auditRGrp3S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditRGrp3S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditRGrp3s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AuditRGrp3Type }
     * 
     * 
     */
    public List<AuditRGrp3Type> getAuditRGrp3s() {
        if (auditRGrp3S == null) {
            auditRGrp3S = new ArrayList<AuditRGrp3Type>();
        }
        return this.auditRGrp3S;
    }

}
