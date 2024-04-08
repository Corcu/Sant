//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour gutRefTxtGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="gutRefTxtGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="gutRefCust" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}gutRefCustType" minOccurs="0"/>
 *         &lt;element name="gutRefUserOrdrNum" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}gutRefUserOrdrNumType" minOccurs="0"/>
 *         &lt;element name="gutRefText" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}gutRefTextType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "gutRefTxtGrpType", propOrder = {
    "gutRefCust",
    "gutRefUserOrdrNum",
    "gutRefText"
})
public class GutRefTxtGrpType {

    protected String gutRefCust;
    protected String gutRefUserOrdrNum;
    protected String gutRefText;

    /**
     * Obtient la valeur de la propri?t? gutRefCust.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGutRefCust() {
        return gutRefCust;
    }

    /**
     * D?finit la valeur de la propri?t? gutRefCust.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGutRefCust(String value) {
        this.gutRefCust = value;
    }

    /**
     * Obtient la valeur de la propri?t? gutRefUserOrdrNum.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGutRefUserOrdrNum() {
        return gutRefUserOrdrNum;
    }

    /**
     * D?finit la valeur de la propri?t? gutRefUserOrdrNum.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGutRefUserOrdrNum(String value) {
        this.gutRefUserOrdrNum = value;
    }

    /**
     * Obtient la valeur de la propri?t? gutRefText.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGutRefText() {
        return gutRefText;
    }

    /**
     * D?finit la valeur de la propri?t? gutRefText.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGutRefText(String value) {
        this.gutRefText = value;
    }

}
