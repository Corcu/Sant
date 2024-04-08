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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * CC060 Daily Margin Summary
 * 
 * <p>Classe Java pour cc060Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cc060Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="rptHdr" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}rptHdrType"/>
 *         &lt;element name="cc060Grp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cc060GrpType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cc060Type", propOrder = {
    "rptHdr",
    "cc060Grps"
})
@XmlRootElement(name = "cc060")
public class Cc060 {

    @XmlElement(required = true)
    protected RptHdrType rptHdr;
    @XmlElement(name = "cc060Grp")
    protected List<Cc060GrpType> cc060Grps;

    /**
     * Obtient la valeur de la propri?t? rptHdr.
     * 
     * @return
     *     possible object is
     *     {@link RptHdrType }
     *     
     */
    public RptHdrType getRptHdr() {
        return rptHdr;
    }

    /**
     * D?finit la valeur de la propri?t? rptHdr.
     * 
     * @param value
     *     allowed object is
     *     {@link RptHdrType }
     *     
     */
    public void setRptHdr(RptHdrType value) {
        this.rptHdr = value;
    }

    /**
     * Gets the value of the cc060Grps property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cc060Grps property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCc060Grps().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cc060GrpType }
     * 
     * 
     */
    public List<Cc060GrpType> getCc060Grps() {
        if (cc060Grps == null) {
            cc060Grps = new ArrayList<Cc060GrpType>();
        }
        return this.cc060Grps;
    }

}
