//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantaci?n de la referencia de enlace (JAXB) XML v2.2.6
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perder?n si se vuelve a compilar el esquema de origen.
// Generado el: PM.05.08 a las 05:22:19 PM CEST 
//


package calypsox.tk.collateral.service.efsonlineservice.request;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para quote complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="quote">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="isin" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="currency" type="{}string1"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "quote", propOrder = {
    "isin",
    "currency"
})
public class QuoteRequest {

    @XmlElement(required = true)
    protected String isin;
    @XmlElement(required = true)
    protected String currency;

    /**
     * Obtiene el valor de la propiedad isin.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIsin() {
        return isin;
    }

    /**
     * Define el valor de la propiedad isin.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIsin(String value) {
        this.isin = value;
    }

    /**
     * Obtiene el valor de la propiedad currency.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Define el valor de la propiedad currency.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrency(String value) {
        this.currency = value;
    }

}
