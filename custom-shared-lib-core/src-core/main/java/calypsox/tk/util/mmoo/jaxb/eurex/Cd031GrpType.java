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
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour cd031GrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cd031GrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cd031KeyGrp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cd031KeyGrpType"/>
 *         &lt;element name="cd031Grp1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cd031Grp1Type" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cd031GrpType", propOrder = {
    "cd031KeyGrp",
    "cd031Grp1S"
})
public class Cd031GrpType {

    @XmlElement(required = true)
    protected Cd031KeyGrpType cd031KeyGrp;
    @XmlElement(name = "cd031Grp1", required = true)
    protected List<Cd031Grp1Type> cd031Grp1S;

    /**
     * Obtient la valeur de la propri?t? cd031KeyGrp.
     * 
     * @return
     *     possible object is
     *     {@link Cd031KeyGrpType }
     *     
     */
    public Cd031KeyGrpType getCd031KeyGrp() {
        return cd031KeyGrp;
    }

    /**
     * D?finit la valeur de la propri?t? cd031KeyGrp.
     * 
     * @param value
     *     allowed object is
     *     {@link Cd031KeyGrpType }
     *     
     */
    public void setCd031KeyGrp(Cd031KeyGrpType value) {
        this.cd031KeyGrp = value;
    }

    /**
     * Gets the value of the cd031Grp1S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cd031Grp1S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCd031Grp1s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cd031Grp1Type }
     * 
     * 
     */
    public List<Cd031Grp1Type> getCd031Grp1s() {
        if (cd031Grp1S == null) {
            cd031Grp1S = new ArrayList<Cd031Grp1Type>();
        }
        return this.cd031Grp1S;
    }

}
