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
 * <p>Classe Java pour sumMembClgFeeGrp1Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumMembClgFeeGrp1Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sumMembClgFeeRec1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumMembClgFeeRec1Type" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumMembClgFeeGrp1Type", propOrder = {
    "sumMembClgFeeRec1S"
})
public class SumMembClgFeeGrp1Type {

    @XmlElement(name = "sumMembClgFeeRec1")
    protected List<SumMembClgFeeRec1Type> sumMembClgFeeRec1S;

    /**
     * Gets the value of the sumMembClgFeeRec1S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sumMembClgFeeRec1S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSumMembClgFeeRec1s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SumMembClgFeeRec1Type }
     * 
     * 
     */
    public List<SumMembClgFeeRec1Type> getSumMembClgFeeRec1s() {
        if (sumMembClgFeeRec1S == null) {
            sumMembClgFeeRec1S = new ArrayList<SumMembClgFeeRec1Type>();
        }
        return this.sumMembClgFeeRec1S;
    }

}