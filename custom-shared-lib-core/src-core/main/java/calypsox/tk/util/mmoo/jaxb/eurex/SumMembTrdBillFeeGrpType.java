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
 * <p>Classe Java pour sumMembTrdBillFeeGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumMembTrdBillFeeGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sumMembTrdBillFeeRec" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumMembTrdBillFeeRecType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumMembTrdBillFeeGrpType", propOrder = {
    "sumMembTrdBillFeeRecs"
})
public class SumMembTrdBillFeeGrpType {

    @XmlElement(name = "sumMembTrdBillFeeRec")
    protected List<SumMembTrdBillFeeRecType> sumMembTrdBillFeeRecs;

    /**
     * Gets the value of the sumMembTrdBillFeeRecs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sumMembTrdBillFeeRecs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSumMembTrdBillFeeRecs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SumMembTrdBillFeeRecType }
     * 
     * 
     */
    public List<SumMembTrdBillFeeRecType> getSumMembTrdBillFeeRecs() {
        if (sumMembTrdBillFeeRecs == null) {
            sumMembTrdBillFeeRecs = new ArrayList<SumMembTrdBillFeeRecType>();
        }
        return this.sumMembTrdBillFeeRecs;
    }

}
