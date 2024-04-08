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
 * <p>Classe Java pour sumAcctFeeRebRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumAcctFeeRebRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sumAcctSavFeeAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumAcctSavFeeAmntType" minOccurs="0"/>
 *         &lt;element name="sumAcctPctSav" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumAcctPctSavType" minOccurs="0"/>
 *         &lt;element name="sumAcctFeeAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumAcctFeeAmntType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumAcctFeeRebRecType", propOrder = {
    "sumAcctSavFeeAmnt",
    "sumAcctPctSav",
    "sumAcctFeeAmnt"
})
public class SumAcctFeeRebRecType {

    protected BigDecimal sumAcctSavFeeAmnt;
    protected BigDecimal sumAcctPctSav;
    protected BigDecimal sumAcctFeeAmnt;

    /**
     * Obtient la valeur de la propri?t? sumAcctSavFeeAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumAcctSavFeeAmnt() {
        return sumAcctSavFeeAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumAcctSavFeeAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumAcctSavFeeAmnt(BigDecimal value) {
        this.sumAcctSavFeeAmnt = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumAcctPctSav.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumAcctPctSav() {
        return sumAcctPctSav;
    }

    /**
     * D?finit la valeur de la propri?t? sumAcctPctSav.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumAcctPctSav(BigDecimal value) {
        this.sumAcctPctSav = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumAcctFeeAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumAcctFeeAmnt() {
        return sumAcctFeeAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumAcctFeeAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumAcctFeeAmnt(BigDecimal value) {
        this.sumAcctFeeAmnt = value;
    }

}
