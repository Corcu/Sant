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
 * <p>Classe Java pour secuRemaLifeType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="secuRemaLifeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="secuRemaLifeYr" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}secuRemaLifeYrType"/>
 *         &lt;element name="secuRemaLifeMth" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}secuRemaLifeMthType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "secuRemaLifeType", propOrder = {
    "secuRemaLifeYr",
    "secuRemaLifeMth"
})
public class SecuRemaLifeType {

    @XmlElement(required = true)
    protected BigInteger secuRemaLifeYr;
    @XmlElement(required = true)
    protected BigInteger secuRemaLifeMth;

    /**
     * Obtient la valeur de la propri?t? secuRemaLifeYr.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSecuRemaLifeYr() {
        return secuRemaLifeYr;
    }

    /**
     * D?finit la valeur de la propri?t? secuRemaLifeYr.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSecuRemaLifeYr(BigInteger value) {
        this.secuRemaLifeYr = value;
    }

    /**
     * Obtient la valeur de la propri?t? secuRemaLifeMth.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSecuRemaLifeMth() {
        return secuRemaLifeMth;
    }

    /**
     * D?finit la valeur de la propri?t? secuRemaLifeMth.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSecuRemaLifeMth(BigInteger value) {
        this.secuRemaLifeMth = value;
    }

}
