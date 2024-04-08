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
 * <p>Classe Java pour auditRGrp1Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditRGrp1Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="auditRKeyGrp1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRKeyGrp1Type"/>
 *         &lt;element name="auditRGrp2" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditRGrp2Type" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditRGrp1Type", propOrder = {
    "auditRKeyGrp1",
    "auditRGrp2S"
})
public class AuditRGrp1Type {

    @XmlElement(required = true)
    protected AuditRKeyGrp1Type auditRKeyGrp1;
    @XmlElement(name = "auditRGrp2", required = true)
    protected List<AuditRGrp2Type> auditRGrp2S;

    /**
     * Obtient la valeur de la propri?t? auditRKeyGrp1.
     * 
     * @return
     *     possible object is
     *     {@link AuditRKeyGrp1Type }
     *     
     */
    public AuditRKeyGrp1Type getAuditRKeyGrp1() {
        return auditRKeyGrp1;
    }

    /**
     * D?finit la valeur de la propri?t? auditRKeyGrp1.
     * 
     * @param value
     *     allowed object is
     *     {@link AuditRKeyGrp1Type }
     *     
     */
    public void setAuditRKeyGrp1(AuditRKeyGrp1Type value) {
        this.auditRKeyGrp1 = value;
    }

    /**
     * Gets the value of the auditRGrp2S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditRGrp2S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditRGrp2s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AuditRGrp2Type }
     * 
     * 
     */
    public List<AuditRGrp2Type> getAuditRGrp2s() {
        if (auditRGrp2S == null) {
            auditRGrp2S = new ArrayList<AuditRGrp2Type>();
        }
        return this.auditRGrp2S;
    }

}
