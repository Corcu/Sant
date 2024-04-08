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
 * <p>Classe Java pour sumMembFeeRebRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumMembFeeRebRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="currTypCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}currTypCodType" minOccurs="0"/>
 *         &lt;element name="sumMembSavFeeAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumMembSavFeeAmntType" minOccurs="0"/>
 *         &lt;element name="sumMembPctSav" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumMembPctSavType" minOccurs="0"/>
 *         &lt;element name="sumMembFeeAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumMembFeeAmntType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumMembFeeRebRecType", propOrder = {
    "currTypCod",
    "sumMembSavFeeAmnt",
    "sumMembPctSav",
    "sumMembFeeAmnt"
})
public class SumMembFeeRebRecType {

    protected String currTypCod;
    protected BigDecimal sumMembSavFeeAmnt;
    protected BigDecimal sumMembPctSav;
    protected BigDecimal sumMembFeeAmnt;

    /**
     * Obtient la valeur de la propri?t? currTypCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrTypCod() {
        return currTypCod;
    }

    /**
     * D?finit la valeur de la propri?t? currTypCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrTypCod(String value) {
        this.currTypCod = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumMembSavFeeAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumMembSavFeeAmnt() {
        return sumMembSavFeeAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumMembSavFeeAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumMembSavFeeAmnt(BigDecimal value) {
        this.sumMembSavFeeAmnt = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumMembPctSav.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumMembPctSav() {
        return sumMembPctSav;
    }

    /**
     * D?finit la valeur de la propri?t? sumMembPctSav.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumMembPctSav(BigDecimal value) {
        this.sumMembPctSav = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumMembFeeAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumMembFeeAmnt() {
        return sumMembFeeAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumMembFeeAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumMembFeeAmnt(BigDecimal value) {
        this.sumMembFeeAmnt = value;
    }

}
