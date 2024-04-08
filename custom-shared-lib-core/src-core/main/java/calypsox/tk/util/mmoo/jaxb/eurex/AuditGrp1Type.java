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
 * <p>Classe Java pour auditGrp1Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditGrp1Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="auditKeyGrp1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditKeyGrp1Type"/>
 *         &lt;element name="auditGrp2" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}auditGrp2Type" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditGrp1Type", propOrder = {
    "auditKeyGrp1",
    "auditGrp2S"
})
public class AuditGrp1Type {

    @XmlElement(required = true)
    protected AuditKeyGrp1Type auditKeyGrp1;
    @XmlElement(name = "auditGrp2", required = true)
    protected List<AuditGrp2Type> auditGrp2S;

    /**
     * Obtient la valeur de la propri?t? auditKeyGrp1.
     * 
     * @return
     *     possible object is
     *     {@link AuditKeyGrp1Type }
     *     
     */
    public AuditKeyGrp1Type getAuditKeyGrp1() {
        return auditKeyGrp1;
    }

    /**
     * D?finit la valeur de la propri?t? auditKeyGrp1.
     * 
     * @param value
     *     allowed object is
     *     {@link AuditKeyGrp1Type }
     *     
     */
    public void setAuditKeyGrp1(AuditKeyGrp1Type value) {
        this.auditKeyGrp1 = value;
    }

    /**
     * Gets the value of the auditGrp2S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the auditGrp2S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuditGrp2s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AuditGrp2Type }
     * 
     * 
     */
    public List<AuditGrp2Type> getAuditGrp2s() {
        if (auditGrp2S == null) {
            auditGrp2S = new ArrayList<AuditGrp2Type>();
        }
        return this.auditGrp2S;
    }

}
