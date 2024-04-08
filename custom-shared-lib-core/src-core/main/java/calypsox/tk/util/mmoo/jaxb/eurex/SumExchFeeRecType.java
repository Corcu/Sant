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
 * <p>Classe Java pour sumExchFeeRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumExchFeeRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="exchNam" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}exchNamType" minOccurs="0"/>
 *         &lt;element name="currTypCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}currTypCodType" minOccurs="0"/>
 *         &lt;element name="sumExchFeeAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumExchFeeAmntType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumExchFeeRecType", propOrder = {
    "exchNam",
    "currTypCod",
    "sumExchFeeAmnt"
})
public class SumExchFeeRecType {

    protected ExchNamType exchNam;
    protected String currTypCod;
    protected BigDecimal sumExchFeeAmnt;

    /**
     * Obtient la valeur de la propri?t? exchNam.
     * 
     * @return
     *     possible object is
     *     {@link ExchNamType }
     *     
     */
    public ExchNamType getExchNam() {
        return exchNam;
    }

    /**
     * D?finit la valeur de la propri?t? exchNam.
     * 
     * @param value
     *     allowed object is
     *     {@link ExchNamType }
     *     
     */
    public void setExchNam(ExchNamType value) {
        this.exchNam = value;
    }

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
     * Obtient la valeur de la propri?t? sumExchFeeAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumExchFeeAmnt() {
        return sumExchFeeAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumExchFeeAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumExchFeeAmnt(BigDecimal value) {
        this.sumExchFeeAmnt = value;
    }

}
