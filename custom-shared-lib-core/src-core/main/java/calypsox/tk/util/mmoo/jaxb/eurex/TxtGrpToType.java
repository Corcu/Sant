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
 * <p>Classe Java pour txtGrpToType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="txtGrpToType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="custTo" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}custToType"/>
 *         &lt;element name="userOrdrNumTo" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}userOrdrNumToType"/>
 *         &lt;element name="textTo" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}textToType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "txtGrpToType", propOrder = {
    "custTo",
    "userOrdrNumTo",
    "textTo"
})
public class TxtGrpToType {

    @XmlElement(required = true)
    protected String custTo;
    @XmlElement(required = true)
    protected String userOrdrNumTo;
    @XmlElement(required = true)
    protected String textTo;

    /**
     * Obtient la valeur de la propri?t? custTo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCustTo() {
        return custTo;
    }

    /**
     * D?finit la valeur de la propri?t? custTo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCustTo(String value) {
        this.custTo = value;
    }

    /**
     * Obtient la valeur de la propri?t? userOrdrNumTo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserOrdrNumTo() {
        return userOrdrNumTo;
    }

    /**
     * D?finit la valeur de la propri?t? userOrdrNumTo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserOrdrNumTo(String value) {
        this.userOrdrNumTo = value;
    }

    /**
     * Obtient la valeur de la propri?t? textTo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTextTo() {
        return textTo;
    }

    /**
     * D?finit la valeur de la propri?t? textTo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTextTo(String value) {
        this.textTo = value;
    }

}
