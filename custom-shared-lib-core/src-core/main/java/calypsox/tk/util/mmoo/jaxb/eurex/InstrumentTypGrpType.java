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
 * <p>Classe Java pour instrumentTypGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="instrumentTypGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="product" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}productType"/>
 *         &lt;element name="instrumentType" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}instrumentTypeType"/>
 *         &lt;element name="instrumentSubType" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}instrumentSubTypeType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "instrumentTypGrpType", propOrder = {
    "product",
    "instrumentType",
    "instrumentSubType"
})
public class InstrumentTypGrpType {

    @XmlElement(required = true)
    protected String product;
    @XmlElement(required = true)
    protected String instrumentType;
    protected String instrumentSubType;

    /**
     * Obtient la valeur de la propri?t? product.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProduct() {
        return product;
    }

    /**
     * D?finit la valeur de la propri?t? product.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProduct(String value) {
        this.product = value;
    }

    /**
     * Obtient la valeur de la propri?t? instrumentType.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInstrumentType() {
        return instrumentType;
    }

    /**
     * D?finit la valeur de la propri?t? instrumentType.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInstrumentType(String value) {
        this.instrumentType = value;
    }

    /**
     * Obtient la valeur de la propri?t? instrumentSubType.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInstrumentSubType() {
        return instrumentSubType;
    }

    /**
     * D?finit la valeur de la propri?t? instrumentSubType.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInstrumentSubType(String value) {
        this.instrumentSubType = value;
    }

}
