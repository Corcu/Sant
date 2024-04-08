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
 * <p>Classe Java pour mgnClsGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="mgnClsGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="mgnClsCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}mgnClsCodType"/>
 *         &lt;element name="mgnClsExpMthDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}mgnClsExpMthDatType" minOccurs="0"/>
 *         &lt;element name="mgnClsExpYrDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}mgnClsExpYrDatType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mgnClsGrpType", propOrder = {
    "mgnClsCod",
    "mgnClsExpMthDat",
    "mgnClsExpYrDat"
})
public class MgnClsGrpType {

    @XmlElement(required = true)
    protected String mgnClsCod;
    protected BigInteger mgnClsExpMthDat;
    protected BigInteger mgnClsExpYrDat;

    /**
     * Obtient la valeur de la propri?t? mgnClsCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMgnClsCod() {
        return mgnClsCod;
    }

    /**
     * D?finit la valeur de la propri?t? mgnClsCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMgnClsCod(String value) {
        this.mgnClsCod = value;
    }

    /**
     * Obtient la valeur de la propri?t? mgnClsExpMthDat.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMgnClsExpMthDat() {
        return mgnClsExpMthDat;
    }

    /**
     * D?finit la valeur de la propri?t? mgnClsExpMthDat.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMgnClsExpMthDat(BigInteger value) {
        this.mgnClsExpMthDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? mgnClsExpYrDat.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMgnClsExpYrDat() {
        return mgnClsExpYrDat;
    }

    /**
     * D?finit la valeur de la propri?t? mgnClsExpYrDat.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMgnClsExpYrDat(BigInteger value) {
        this.mgnClsExpYrDat = value;
    }

}
