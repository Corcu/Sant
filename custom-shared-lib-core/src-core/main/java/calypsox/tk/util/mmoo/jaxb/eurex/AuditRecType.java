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
 * <p>Classe Java pour auditRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="auditRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="updtFldNam" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}updtFldNamType"/>
 *         &lt;element name="audtValBef" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtValBefType" minOccurs="0"/>
 *         &lt;element name="audtValAft" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}audtValAftType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "auditRecType", propOrder = {
    "updtFldNam",
    "audtValBef",
    "audtValAft"
})
public class AuditRecType {

    @XmlElement(required = true)
    protected String updtFldNam;
    protected String audtValBef;
    protected String audtValAft;

    /**
     * Obtient la valeur de la propri?t? updtFldNam.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUpdtFldNam() {
        return updtFldNam;
    }

    /**
     * D?finit la valeur de la propri?t? updtFldNam.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUpdtFldNam(String value) {
        this.updtFldNam = value;
    }

    /**
     * Obtient la valeur de la propri?t? audtValBef.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAudtValBef() {
        return audtValBef;
    }

    /**
     * D?finit la valeur de la propri?t? audtValBef.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAudtValBef(String value) {
        this.audtValBef = value;
    }

    /**
     * Obtient la valeur de la propri?t? audtValAft.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAudtValAft() {
        return audtValAft;
    }

    /**
     * D?finit la valeur de la propri?t? audtValAft.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAudtValAft(String value) {
        this.audtValAft = value;
    }

}
