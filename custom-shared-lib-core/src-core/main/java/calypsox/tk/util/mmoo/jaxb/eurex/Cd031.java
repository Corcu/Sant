//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:11:42 AM CEST
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
 * CD031 Daily Collateral Valuation
 * 
 * <p>Classe Java pour cd031Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cd031Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="rptHdr" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}rptHdrType"/>
 *         &lt;element name="cd031Grp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cd031GrpType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cd031Type", propOrder = {
    "rptHdr",
    "cd031Grps"
})
@XmlRootElement(name = "cd031")
public class Cd031 {

    @XmlElement(required = true)
    protected RptHdrType rptHdr;
    @XmlElement(name = "cd031Grp")
    protected List<Cd031GrpType> cd031Grps;

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
     * Gets the value of the cd031Grps property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cd031Grps property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCd031Grps().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cd031GrpType }
     * 
     * 
     */
    public List<Cd031GrpType> getCd031Grps() {
        if (cd031Grps == null) {
            cd031Grps = new ArrayList<Cd031GrpType>();
        }
        return this.cd031Grps;
    }

}
