//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:11:42 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour cd031Grp4Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cd031Grp4Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cd031KeyGrp4" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cd031KeyGrp4Type"/>
 *         &lt;element name="cd031Rec" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cd031RecType" maxOccurs="unbounded"/>
 *         &lt;element name="sumCurrSecuMktVal" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumCurrSecuMktValType"/>
 *         &lt;element name="sumCurrSecuCollVal" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumCurrSecuCollValType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cd031Grp4Type", propOrder = {
    "cd031KeyGrp4",
    "cd031Recs",
    "sumCurrSecuMktVal",
    "sumCurrSecuCollVal"
})
public class Cd031Grp4Type {

    @XmlElement(required = true)
    protected Cd031KeyGrp4Type cd031KeyGrp4;
    @XmlElement(name = "cd031Rec", required = true)
    protected List<Cd031RecType> cd031Recs;
    @XmlElement(required = true)
    protected BigInteger sumCurrSecuMktVal;
    @XmlElement(required = true)
    protected BigInteger sumCurrSecuCollVal;

    /**
     * Obtient la valeur de la propri?t? cd031KeyGrp4.
     * 
     * @return
     *     possible object is
     *     {@link Cd031KeyGrp4Type }
     *     
     */
    public Cd031KeyGrp4Type getCd031KeyGrp4() {
        return cd031KeyGrp4;
    }

    /**
     * D?finit la valeur de la propri?t? cd031KeyGrp4.
     * 
     * @param value
     *     allowed object is
     *     {@link Cd031KeyGrp4Type }
     *     
     */
    public void setCd031KeyGrp4(Cd031KeyGrp4Type value) {
        this.cd031KeyGrp4 = value;
    }

    /**
     * Gets the value of the cd031Recs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cd031Recs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCd031Recs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cd031RecType }
     * 
     * 
     */
    public List<Cd031RecType> getCd031Recs() {
        if (cd031Recs == null) {
            cd031Recs = new ArrayList<Cd031RecType>();
        }
        return this.cd031Recs;
    }

    /**
     * Obtient la valeur de la propri?t? sumCurrSecuMktVal.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSumCurrSecuMktVal() {
        return sumCurrSecuMktVal;
    }

    /**
     * D?finit la valeur de la propri?t? sumCurrSecuMktVal.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSumCurrSecuMktVal(BigInteger value) {
        this.sumCurrSecuMktVal = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumCurrSecuCollVal.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSumCurrSecuCollVal() {
        return sumCurrSecuCollVal;
    }

    /**
     * D?finit la valeur de la propri?t? sumCurrSecuCollVal.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSumCurrSecuCollVal(BigInteger value) {
        this.sumCurrSecuCollVal = value;
    }

}
