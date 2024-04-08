//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour legClearingGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="legClearingGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="leg1Grp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}leg1GrpType"/>
 *         &lt;element name="leg2Grp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}leg2GrpType"/>
 *         &lt;element name="leg3Grp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}leg3GrpType"/>
 *         &lt;element name="leg4Grp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}leg4GrpType"/>
 *         &lt;element name="leg5Grp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}leg5GrpType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "legClearingGrpType", propOrder = {
    "leg1Grp",
    "leg2Grp",
    "leg3Grp",
    "leg4Grp",
    "leg5Grp"
})
public class LegClearingGrpType {

    @XmlElement(required = true)
    protected Leg1GrpType leg1Grp;
    @XmlElement(required = true)
    protected Leg2GrpType leg2Grp;
    @XmlElement(required = true)
    protected Leg3GrpType leg3Grp;
    @XmlElement(required = true)
    protected Leg4GrpType leg4Grp;
    @XmlElement(required = true)
    protected Leg5GrpType leg5Grp;

    /**
     * Obtient la valeur de la propri?t? leg1Grp.
     * 
     * @return
     *     possible object is
     *     {@link Leg1GrpType }
     *     
     */
    public Leg1GrpType getLeg1Grp() {
        return leg1Grp;
    }

    /**
     * D?finit la valeur de la propri?t? leg1Grp.
     * 
     * @param value
     *     allowed object is
     *     {@link Leg1GrpType }
     *     
     */
    public void setLeg1Grp(Leg1GrpType value) {
        this.leg1Grp = value;
    }

    /**
     * Obtient la valeur de la propri?t? leg2Grp.
     * 
     * @return
     *     possible object is
     *     {@link Leg2GrpType }
     *     
     */
    public Leg2GrpType getLeg2Grp() {
        return leg2Grp;
    }

    /**
     * D?finit la valeur de la propri?t? leg2Grp.
     * 
     * @param value
     *     allowed object is
     *     {@link Leg2GrpType }
     *     
     */
    public void setLeg2Grp(Leg2GrpType value) {
        this.leg2Grp = value;
    }

    /**
     * Obtient la valeur de la propri?t? leg3Grp.
     * 
     * @return
     *     possible object is
     *     {@link Leg3GrpType }
     *     
     */
    public Leg3GrpType getLeg3Grp() {
        return leg3Grp;
    }

    /**
     * D?finit la valeur de la propri?t? leg3Grp.
     * 
     * @param value
     *     allowed object is
     *     {@link Leg3GrpType }
     *     
     */
    public void setLeg3Grp(Leg3GrpType value) {
        this.leg3Grp = value;
    }

    /**
     * Obtient la valeur de la propri?t? leg4Grp.
     * 
     * @return
     *     possible object is
     *     {@link Leg4GrpType }
     *     
     */
    public Leg4GrpType getLeg4Grp() {
        return leg4Grp;
    }

    /**
     * D?finit la valeur de la propri?t? leg4Grp.
     * 
     * @param value
     *     allowed object is
     *     {@link Leg4GrpType }
     *     
     */
    public void setLeg4Grp(Leg4GrpType value) {
        this.leg4Grp = value;
    }

    /**
     * Obtient la valeur de la propri?t? leg5Grp.
     * 
     * @return
     *     possible object is
     *     {@link Leg5GrpType }
     *     
     */
    public Leg5GrpType getLeg5Grp() {
        return leg5Grp;
    }

    /**
     * D?finit la valeur de la propri?t? leg5Grp.
     * 
     * @param value
     *     allowed object is
     *     {@link Leg5GrpType }
     *     
     */
    public void setLeg5Grp(Leg5GrpType value) {
        this.leg5Grp = value;
    }

}
