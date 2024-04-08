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
 * <p>Classe Java pour sumMembExchFeeRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumMembExchFeeRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="membExchIdCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}membExchIdCodType" minOccurs="0"/>
 *         &lt;element name="currTypCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}currTypCodType" minOccurs="0"/>
 *         &lt;element name="sumMembExchFeeAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumMembExchFeeAmntType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumMembExchFeeRecType", propOrder = {
    "membExchIdCod",
    "currTypCod",
    "sumMembExchFeeAmnt"
})
public class SumMembExchFeeRecType {

    protected String membExchIdCod;
    protected String currTypCod;
    protected BigDecimal sumMembExchFeeAmnt;

    /**
     * Obtient la valeur de la propri?t? membExchIdCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMembExchIdCod() {
        return membExchIdCod;
    }

    /**
     * D?finit la valeur de la propri?t? membExchIdCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMembExchIdCod(String value) {
        this.membExchIdCod = value;
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
     * Obtient la valeur de la propri?t? sumMembExchFeeAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumMembExchFeeAmnt() {
        return sumMembExchFeeAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumMembExchFeeAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumMembExchFeeAmnt(BigDecimal value) {
        this.sumMembExchFeeAmnt = value;
    }

}
