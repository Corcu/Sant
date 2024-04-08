//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour txtGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="txtGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cust" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}custType"/>
 *         &lt;element name="userOrdrNum" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}userOrdrNumType"/>
 *         &lt;element name="text" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}textType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "txtGrpType", propOrder = {
    "cust",
    "userOrdrNum",
    "text"
})
public class TxtGrpType {

    @XmlElement(required = true)
    protected String cust;
    @XmlElement(required = true)
    protected String userOrdrNum;
    @XmlElement(required = true)
    protected String text;

    /**
     * Obtient la valeur de la propri?t? cust.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCust() {
        return cust;
    }

    /**
     * D?finit la valeur de la propri?t? cust.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCust(String value) {
        this.cust = value;
    }

    /**
     * Obtient la valeur de la propri?t? userOrdrNum.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserOrdrNum() {
        return userOrdrNum;
    }

    /**
     * D?finit la valeur de la propri?t? userOrdrNum.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserOrdrNum(String value) {
        this.userOrdrNum = value;
    }

    /**
     * Obtient la valeur de la propri?t? text.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getText() {
        return text;
    }

    /**
     * D?finit la valeur de la propri?t? text.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setText(String value) {
        this.text = value;
    }

}
