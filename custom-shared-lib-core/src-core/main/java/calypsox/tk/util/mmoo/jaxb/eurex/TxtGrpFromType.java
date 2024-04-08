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
 * <p>Classe Java pour txtGrpFromType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="txtGrpFromType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="custFrom" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}custFromType"/>
 *         &lt;element name="userOrdrNumFrom" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}userOrdrNumFromType"/>
 *         &lt;element name="textFrom" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}textFromType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "txtGrpFromType", propOrder = {
    "custFrom",
    "userOrdrNumFrom",
    "textFrom"
})
public class TxtGrpFromType {

    @XmlElement(required = true)
    protected String custFrom;
    @XmlElement(required = true)
    protected String userOrdrNumFrom;
    @XmlElement(required = true)
    protected String textFrom;

    /**
     * Obtient la valeur de la propri?t? custFrom.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCustFrom() {
        return custFrom;
    }

    /**
     * D?finit la valeur de la propri?t? custFrom.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCustFrom(String value) {
        this.custFrom = value;
    }

    /**
     * Obtient la valeur de la propri?t? userOrdrNumFrom.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserOrdrNumFrom() {
        return userOrdrNumFrom;
    }

    /**
     * D?finit la valeur de la propri?t? userOrdrNumFrom.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserOrdrNumFrom(String value) {
        this.userOrdrNumFrom = value;
    }

    /**
     * Obtient la valeur de la propri?t? textFrom.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTextFrom() {
        return textFrom;
    }

    /**
     * D?finit la valeur de la propri?t? textFrom.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTextFrom(String value) {
        this.textFrom = value;
    }

}
