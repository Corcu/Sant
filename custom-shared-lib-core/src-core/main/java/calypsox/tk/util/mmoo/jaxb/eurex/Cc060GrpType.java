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
 * <p>Classe Java pour cc060GrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cc060GrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cc060KeyGrp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cc060KeyGrpType"/>
 *         &lt;element name="clgCurrTypCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}clgCurrTypCodType"/>
 *         &lt;element name="cc060Grp1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cc060Grp1Type" maxOccurs="unbounded"/>
 *         &lt;element name="sumClgMbrTotMgnClgCurr" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumClgMbrTotMgnClgCurrType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cc060GrpType", propOrder = {
    "cc060KeyGrp",
    "clgCurrTypCod",
    "cc060Grp1S",
    "sumClgMbrTotMgnClgCurr"
})
public class Cc060GrpType {

    @XmlElement(required = true)
    protected Cc060KeyGrpType cc060KeyGrp;
    @XmlElement(required = true)
    protected String clgCurrTypCod;
    @XmlElement(name = "cc060Grp1", required = true)
    protected List<Cc060Grp1Type> cc060Grp1S;
    protected BigDecimal sumClgMbrTotMgnClgCurr;

    /**
     * Obtient la valeur de la propri?t? cc060KeyGrp.
     * 
     * @return
     *     possible object is
     *     {@link Cc060KeyGrpType }
     *     
     */
    public Cc060KeyGrpType getCc060KeyGrp() {
        return cc060KeyGrp;
    }

    /**
     * D?finit la valeur de la propri?t? cc060KeyGrp.
     * 
     * @param value
     *     allowed object is
     *     {@link Cc060KeyGrpType }
     *     
     */
    public void setCc060KeyGrp(Cc060KeyGrpType value) {
        this.cc060KeyGrp = value;
    }

    /**
     * Obtient la valeur de la propri?t? clgCurrTypCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClgCurrTypCod() {
        return clgCurrTypCod;
    }

    /**
     * D?finit la valeur de la propri?t? clgCurrTypCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClgCurrTypCod(String value) {
        this.clgCurrTypCod = value;
    }

    /**
     * Gets the value of the cc060Grp1S property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cc060Grp1S property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCc060Grp1s().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cc060Grp1Type }
     * 
     * 
     */
    public List<Cc060Grp1Type> getCc060Grp1s() {
        if (cc060Grp1S == null) {
            cc060Grp1S = new ArrayList<Cc060Grp1Type>();
        }
        return this.cc060Grp1S;
    }

    /**
     * Obtient la valeur de la propri?t? sumClgMbrTotMgnClgCurr.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumClgMbrTotMgnClgCurr() {
        return sumClgMbrTotMgnClgCurr;
    }

    /**
     * D?finit la valeur de la propri?t? sumClgMbrTotMgnClgCurr.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumClgMbrTotMgnClgCurr(BigDecimal value) {
        this.sumClgMbrTotMgnClgCurr = value;
    }

}
