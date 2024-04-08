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
 * <p>Classe Java pour cc060Grp3Type complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cc060Grp3Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cc060KeyGrp3" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cc060KeyGrp3Type"/>
 *         &lt;element name="cc060Rec" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cc060RecType" maxOccurs="unbounded"/>
 *         &lt;element name="sumExchMbrPrtMgbReq" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumExchMbrPrtMgbReqType"/>
 *         &lt;element name="sumExchMbrClgCurr" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumExchMbrClgCurrType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cc060Grp3Type", propOrder = {
    "cc060KeyGrp3",
    "cc060Recs",
    "sumExchMbrPrtMgbReq",
    "sumExchMbrClgCurr"
})
public class Cc060Grp3Type {

    @XmlElement(required = true)
    protected Cc060KeyGrp3Type cc060KeyGrp3;
    @XmlElement(name = "cc060Rec", required = true)
    protected List<Cc060RecType> cc060Recs;
    @XmlElement(required = true)
    protected BigDecimal sumExchMbrPrtMgbReq;
    @XmlElement(required = true)
    protected BigDecimal sumExchMbrClgCurr;

    /**
     * Obtient la valeur de la propri?t? cc060KeyGrp3.
     * 
     * @return
     *     possible object is
     *     {@link Cc060KeyGrp3Type }
     *     
     */
    public Cc060KeyGrp3Type getCc060KeyGrp3() {
        return cc060KeyGrp3;
    }

    /**
     * D?finit la valeur de la propri?t? cc060KeyGrp3.
     * 
     * @param value
     *     allowed object is
     *     {@link Cc060KeyGrp3Type }
     *     
     */
    public void setCc060KeyGrp3(Cc060KeyGrp3Type value) {
        this.cc060KeyGrp3 = value;
    }

    /**
     * Gets the value of the cc060Recs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cc060Recs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCc060Recs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cc060RecType }
     * 
     * 
     */
    public List<Cc060RecType> getCc060Recs() {
        if (cc060Recs == null) {
            cc060Recs = new ArrayList<Cc060RecType>();
        }
        return this.cc060Recs;
    }

    /**
     * Obtient la valeur de la propri?t? sumExchMbrPrtMgbReq.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumExchMbrPrtMgbReq() {
        return sumExchMbrPrtMgbReq;
    }

    /**
     * D?finit la valeur de la propri?t? sumExchMbrPrtMgbReq.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumExchMbrPrtMgbReq(BigDecimal value) {
        this.sumExchMbrPrtMgbReq = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumExchMbrClgCurr.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumExchMbrClgCurr() {
        return sumExchMbrClgCurr;
    }

    /**
     * D?finit la valeur de la propri?t? sumExchMbrClgCurr.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumExchMbrClgCurr(BigDecimal value) {
        this.sumExchMbrClgCurr = value;
    }

}
