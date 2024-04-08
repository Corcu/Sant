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
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour expiryGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="expiryGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
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
@XmlType(name = "expiryGrpType", propOrder = {
    "mgnClsExpMthDat",
    "mgnClsExpYrDat"
})
public class ExpiryGrpType {

    protected BigInteger mgnClsExpMthDat;
    protected BigInteger mgnClsExpYrDat;

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
