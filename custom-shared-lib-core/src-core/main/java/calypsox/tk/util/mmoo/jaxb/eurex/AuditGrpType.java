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
 * <p>Classe Java pour auditGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="auditKeyGrp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditKeyGrpType"/>
 *         &lt;element name="auditGrp1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditGrp1Type" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditGrpType", propOrder = {
    "auditKeyGrp",
    "auditGrp1S"
})
public class AuditGrpType {

    @XmlElement(required = true)
    protected AuditKeyGrpType auditKeyGrp;
    @XmlElement(name = "auditGrp1", required = true)
    protected List<AuditGrp1Type> auditGrp1S;

    /**
     * Obtient la valeur de la propri?t? auditKeyGrp.
     * 
     * @return
     *     possible object is
     *     {@link AuditKeyGrpType }
     *     
     */
    public AuditKeyGrpType getAuditKeyGrp() {
        return auditKeyGrp;
    }

    /**
     * D?finit la valeur de la propri?t? auditKeyGrp.
     * 
     * @param value
     *     allowed object is
     *     {@link AuditKeyGrpType }
     *     
     */
    public void setAuditKeyGrp(AuditKeyGrpType value) {
        this.auditKeyGrp = value;
    }

    /**
     * Gets the value of the auditGrp1S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditGrp1S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditGrp1s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AuditGrp1Type }
     * 
     * 
     */
    public List<AuditGrp1Type> getAuditGrp1s() {
        if (auditGrp1S == null) {
            auditGrp1S = new ArrayList<AuditGrp1Type>();
        }
        return this.auditGrp1S;
    }

}
