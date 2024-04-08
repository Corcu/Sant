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
 * <p>Classe Java pour leg3GrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="leg3GrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="account" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}accountType" minOccurs="0"/>
 *         &lt;element name="opnClsCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}opnClsCodType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "leg3GrpType", propOrder = {
    "account",
    "opnClsCod"
})
public class Leg3GrpType {

    protected String account;
    protected String opnClsCod;

    /**
     * Obtient la valeur de la propri?t? account.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAccount() {
        return account;
    }

    /**
     * D?finit la valeur de la propri?t? account.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAccount(String value) {
        this.account = value;
    }

    /**
     * Obtient la valeur de la propri?t? opnClsCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOpnClsCod() {
        return opnClsCod;
    }

    /**
     * D?finit la valeur de la propri?t? opnClsCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOpnClsCod(String value) {
        this.opnClsCod = value;
    }

}
