//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour shtOptMinCompGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="shtOptMinCompGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="shtOptMinCompInd" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}shtOptMinCompIndType" minOccurs="0"/>
 *         &lt;element name="shtOptMinCompTxt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}shtOptMinCompTxtType" minOccurs="0"/>
 *         &lt;element name="shtOptMinCompQty" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}shtOptMinCompQtyType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "shtOptMinCompGrpType", propOrder = {
    "shtOptMinCompInd",
    "shtOptMinCompTxt",
    "shtOptMinCompQty"
})
public class ShtOptMinCompGrpType {

    protected String shtOptMinCompInd;
    protected ShtOptMinCompTxtType shtOptMinCompTxt;
    protected BigInteger shtOptMinCompQty;

    /**
     * Obtient la valeur de la propri?t? shtOptMinCompInd.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getShtOptMinCompInd() {
        return shtOptMinCompInd;
    }

    /**
     * D?finit la valeur de la propri?t? shtOptMinCompInd.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setShtOptMinCompInd(String value) {
        this.shtOptMinCompInd = value;
    }

    /**
     * Obtient la valeur de la propri?t? shtOptMinCompTxt.
     * 
     * @return
     *     possible object is
     *     {@link ShtOptMinCompTxtType }
     *     
     */
    public ShtOptMinCompTxtType getShtOptMinCompTxt() {
        return shtOptMinCompTxt;
    }

    /**
     * D?finit la valeur de la propri?t? shtOptMinCompTxt.
     * 
     * @param value
     *     allowed object is
     *     {@link ShtOptMinCompTxtType }
     *     
     */
    public void setShtOptMinCompTxt(ShtOptMinCompTxtType value) {
        this.shtOptMinCompTxt = value;
    }

    /**
     * Obtient la valeur de la propri?t? shtOptMinCompQty.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getShtOptMinCompQty() {
        return shtOptMinCompQty;
    }

    /**
     * D?finit la valeur de la propri?t? shtOptMinCompQty.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setShtOptMinCompQty(BigInteger value) {
        this.shtOptMinCompQty = value;
    }

}
