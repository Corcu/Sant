//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour sumProdFeeRebRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumProdFeeRebRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sumProdSavFeeAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumProdSavFeeAmntType" minOccurs="0"/>
 *         &lt;element name="sumProdPctSav" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumProdPctSavType" minOccurs="0"/>
 *         &lt;element name="sumProdFeeAmntX" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumProdFeeAmntXType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumProdFeeRebRecType", propOrder = {
    "sumProdSavFeeAmnt",
    "sumProdPctSav",
    "sumProdFeeAmntX"
})
public class SumProdFeeRebRecType {

    protected BigDecimal sumProdSavFeeAmnt;
    protected BigDecimal sumProdPctSav;
    protected BigDecimal sumProdFeeAmntX;

    /**
     * Obtient la valeur de la propri?t? sumProdSavFeeAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumProdSavFeeAmnt() {
        return sumProdSavFeeAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumProdSavFeeAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumProdSavFeeAmnt(BigDecimal value) {
        this.sumProdSavFeeAmnt = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumProdPctSav.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumProdPctSav() {
        return sumProdPctSav;
    }

    /**
     * D?finit la valeur de la propri?t? sumProdPctSav.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumProdPctSav(BigDecimal value) {
        this.sumProdPctSav = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumProdFeeAmntX.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumProdFeeAmntX() {
        return sumProdFeeAmntX;
    }

    /**
     * D?finit la valeur de la propri?t? sumProdFeeAmntX.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumProdFeeAmntX(BigDecimal value) {
        this.sumProdFeeAmntX = value;
    }

}
