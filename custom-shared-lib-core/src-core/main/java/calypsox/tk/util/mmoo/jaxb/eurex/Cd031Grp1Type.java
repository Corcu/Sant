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
 * <p>Classe Java pour cd031Grp1Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cd031Grp1Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cd031KeyGrp1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cd031KeyGrp1Type"/>
 *         &lt;element name="cd031Grp3" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cd031Grp3Type" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cd031Grp1Type", propOrder = {
    "cd031KeyGrp1",
    "cd031Grp3S"
})
public class Cd031Grp1Type {

    @XmlElement(required = true)
    protected Cd031KeyGrp1Type cd031KeyGrp1;
    @XmlElement(name = "cd031Grp3", required = true)
    protected List<Cd031Grp3Type> cd031Grp3S;

    /**
     * Obtient la valeur de la propri?t? cd031KeyGrp1.
     * 
     * @return
     *     possible object is
     *     {@link Cd031KeyGrp1Type }
     *     
     */
    public Cd031KeyGrp1Type getCd031KeyGrp1() {
        return cd031KeyGrp1;
    }

    /**
     * D?finit la valeur de la propri?t? cd031KeyGrp1.
     * 
     * @param value
     *     allowed object is
     *     {@link Cd031KeyGrp1Type }
     *     
     */
    public void setCd031KeyGrp1(Cd031KeyGrp1Type value) {
        this.cd031KeyGrp1 = value;
    }

    /**
     * Gets the value of the cd031Grp3S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cd031Grp3S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCd031Grp3s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cd031Grp3Type }
     * 
     * 
     */
    public List<Cd031Grp3Type> getCd031Grp3s() {
        if (cd031Grp3S == null) {
            cd031Grp3S = new ArrayList<Cd031Grp3Type>();
        }
        return this.cd031Grp3S;
    }

}
