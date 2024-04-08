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
 * <p>Classe Java pour undrFutCntrIdGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="undrFutCntrIdGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="undrFutProdId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}undrFutProdIdType"/>
 *         &lt;element name="undrFutExpMthDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}undrFutExpMthDatType"/>
 *         &lt;element name="undrFutExpYrDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}undrFutExpYrDatType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "undrFutCntrIdGrpType", propOrder = {
    "undrFutProdId",
    "undrFutExpMthDat",
    "undrFutExpYrDat"
})
public class UndrFutCntrIdGrpType {

    @XmlElement(required = true)
    protected String undrFutProdId;
    @XmlElement(required = true)
    protected BigInteger undrFutExpMthDat;
    @XmlElement(required = true)
    protected BigInteger undrFutExpYrDat;

    /**
     * Obtient la valeur de la propri?t? undrFutProdId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUndrFutProdId() {
        return undrFutProdId;
    }

    /**
     * D?finit la valeur de la propri?t? undrFutProdId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUndrFutProdId(String value) {
        this.undrFutProdId = value;
    }

    /**
     * Obtient la valeur de la propri?t? undrFutExpMthDat.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getUndrFutExpMthDat() {
        return undrFutExpMthDat;
    }

    /**
     * D?finit la valeur de la propri?t? undrFutExpMthDat.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setUndrFutExpMthDat(BigInteger value) {
        this.undrFutExpMthDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? undrFutExpYrDat.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getUndrFutExpYrDat() {
        return undrFutExpYrDat;
    }

    /**
     * D?finit la valeur de la propri?t? undrFutExpYrDat.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setUndrFutExpYrDat(BigInteger value) {
        this.undrFutExpYrDat = value;
    }

}
