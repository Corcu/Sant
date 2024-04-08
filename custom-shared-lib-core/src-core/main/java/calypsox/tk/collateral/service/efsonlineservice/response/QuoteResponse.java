//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantaci?n de la referencia de enlace (JAXB) XML v2.2.6
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perder?n si se vuelve a compilar el esquema de origen.
// Generado el: PM.06.17 a las 06:32:15 PM CEST 
//

package calypsox.tk.collateral.service.efsonlineservice.response;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Clase Java para quote complex type.
 * 
 * <p>
 * El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="quote">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="isin" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="currency" type="{}string1"/>
 *         &lt;element name="bid" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="ask" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "quote", propOrder = { "isin", "currency", "bid", "ask" })
public class QuoteResponse {

	@XmlElement(required = true)
	protected String isin;
	@XmlElement(required = true)
	protected String currency;
	protected double bid;
	protected double ask;

	/**
	 * Obtiene el valor de la propiedad isin.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getIsin() {
		return this.isin;
	}

	/**
	 * Define el valor de la propiedad isin.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setIsin(String value) {
		this.isin = value;
	}

	/**
	 * Obtiene el valor de la propiedad currency.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getCurrency() {
		return this.currency;
	}

	/**
	 * Define el valor de la propiedad currency.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setCurrency(String value) {
		this.currency = value;
	}

	/**
	 * Obtiene el valor de la propiedad bid.
	 * 
	 */
	public double getBid() {
		return this.bid;
	}

	/**
	 * Define el valor de la propiedad bid.
	 * 
	 */
	public void setBid(double value) {
		this.bid = value;
	}

	/**
	 * Obtiene el valor de la propiedad ask.
	 * 
	 */
	public double getAsk() {
		return this.ask;
	}

	/**
	 * Define el valor de la propiedad ask.
	 * 
	 */
	public void setAsk(double value) {
		this.ask = value;
	}

}
