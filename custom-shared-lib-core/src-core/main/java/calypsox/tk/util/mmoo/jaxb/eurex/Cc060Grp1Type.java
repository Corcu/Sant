//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour cc060Grp1Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cc060Grp1Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cc060KeyGrp1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cc060KeyGrp1Type"/>
 *         &lt;element name="cc060Grp3" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cc060Grp3Type" maxOccurs="unbounded"/>
 *         &lt;element name="sumPoolIdTotal" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumPoolIdTotalType" minOccurs="0"/>
 *         &lt;element name="sumPoolIdClgCurr" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumPoolIdClgCurrType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cc060Grp1Type", propOrder = {
    "cc060KeyGrp1",
    "cc060Grp3S",
    "sumPoolIdTotal",
    "sumPoolIdClgCurr"
})
public class Cc060Grp1Type {

    @XmlElement(required = true)
    protected Cc060KeyGrp1Type cc060KeyGrp1;
    @XmlElement(name = "cc060Grp3", required = true)
    protected List<Cc060Grp3Type> cc060Grp3S;
    protected BigDecimal sumPoolIdTotal;
    protected BigDecimal sumPoolIdClgCurr;

    /**
     * Obtient la valeur de la propri?t? cc060KeyGrp1.
     * 
     * @return
     *     possible object is
     *     {@link Cc060KeyGrp1Type }
     *     
     */
    public Cc060KeyGrp1Type getCc060KeyGrp1() {
        return cc060KeyGrp1;
    }

    /**
     * D?finit la valeur de la propri?t? cc060KeyGrp1.
     * 
     * @param value
     *     allowed object is
     *     {@link Cc060KeyGrp1Type }
     *     
     */
    public void setCc060KeyGrp1(Cc060KeyGrp1Type value) {
        this.cc060KeyGrp1 = value;
    }

    /**
     * Gets the value of the cc060Grp3S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cc060Grp3S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCc060Grp3s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cc060Grp3Type }
     * 
     * 
     */
    public List<Cc060Grp3Type> getCc060Grp3s() {
        if (cc060Grp3S == null) {
            cc060Grp3S = new ArrayList<Cc060Grp3Type>();
        }
        return this.cc060Grp3S;
    }

    /**
     * Obtient la valeur de la propri?t? sumPoolIdTotal.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumPoolIdTotal() {
        return sumPoolIdTotal;
    }

    /**
     * D?finit la valeur de la propri?t? sumPoolIdTotal.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumPoolIdTotal(BigDecimal value) {
        this.sumPoolIdTotal = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumPoolIdClgCurr.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumPoolIdClgCurr() {
        return sumPoolIdClgCurr;
    }

    /**
     * D?finit la valeur de la propri?t? sumPoolIdClgCurr.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumPoolIdClgCurr(BigDecimal value) {
        this.sumPoolIdClgCurr = value;
    }

}
