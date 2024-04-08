//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour stratLegInfoType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="stratLegInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="buyCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}buyCodType"/>
 *         &lt;element name="stratLegVol" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}stratLegVolType"/>
 *         &lt;element name="cntrClasCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrClasCodType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "stratLegInfoType", propOrder = {
    "buyCod",
    "stratLegVol",
    "cntrClasCod"
})
public class StratLegInfoType {

    @XmlElement(required = true)
    protected BuyCodType buyCod;
    @XmlElement(required = true)
    protected BigInteger stratLegVol;
    protected CntrClasCodType cntrClasCod;

    /**
     * Obtient la valeur de la propri?t? buyCod.
     * 
     * @return
     *     possible object is
     *     {@link BuyCodType }
     *     
     */
    public BuyCodType getBuyCod() {
        return buyCod;
    }

    /**
     * D?finit la valeur de la propri?t? buyCod.
     * 
     * @param value
     *     allowed object is
     *     {@link BuyCodType }
     *     
     */
    public void setBuyCod(BuyCodType value) {
        this.buyCod = value;
    }

    /**
     * Obtient la valeur de la propri?t? stratLegVol.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getStratLegVol() {
        return stratLegVol;
    }

    /**
     * D?finit la valeur de la propri?t? stratLegVol.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setStratLegVol(BigInteger value) {
        this.stratLegVol = value;
    }

    /**
     * Obtient la valeur de la propri?t? cntrClasCod.
     * 
     * @return
     *     possible object is
     *     {@link CntrClasCodType }
     *     
     */
    public CntrClasCodType getCntrClasCod() {
        return cntrClasCod;
    }

    /**
     * D?finit la valeur de la propri?t? cntrClasCod.
     * 
     * @param value
     *     allowed object is
     *     {@link CntrClasCodType }
     *     
     */
    public void setCntrClasCod(CntrClasCodType value) {
        this.cntrClasCod = value;
    }

}