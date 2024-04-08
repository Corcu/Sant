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
 * <p>Classe Java pour cntrExpDatGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cntrExpDatGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cntrExpMthDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrExpMthDatType" minOccurs="0"/>
 *         &lt;element name="cntrExpYrDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrExpYrDatType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cntrExpDatGrpType", propOrder = {
    "cntrExpMthDat",
    "cntrExpYrDat"
})
public class CntrExpDatGrpType {

    protected BigInteger cntrExpMthDat;
    protected BigInteger cntrExpYrDat;

    /**
     * Obtient la valeur de la propri?t? cntrExpMthDat.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCntrExpMthDat() {
        return cntrExpMthDat;
    }

    /**
     * D?finit la valeur de la propri?t? cntrExpMthDat.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCntrExpMthDat(BigInteger value) {
        this.cntrExpMthDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? cntrExpYrDat.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCntrExpYrDat() {
        return cntrExpYrDat;
    }

    /**
     * D?finit la valeur de la propri?t? cntrExpYrDat.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCntrExpYrDat(BigInteger value) {
        this.cntrExpYrDat = value;
    }

}
