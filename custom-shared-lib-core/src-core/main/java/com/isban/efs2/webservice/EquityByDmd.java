//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantaci?n de la referencia de enlace (JAXB) XML v2.2.7
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perder?n si se vuelve a compilar el esquema de origen.
// Generado el: 2014.01.02 a las 03:34:20 PM CET 
//


package com.isban.efs2.webservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para equityByDmd complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="equityByDmd">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="calendar" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="currency" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="dmd" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="equity" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="market" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="publishDate" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="publishMethod" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "equityByDmd", propOrder = {
    "calendar",
    "currency",
    "dmd",
    "equity",
    "market",
    "publishDate",
    "publishMethod",
    "type"
})
@XmlSeeAlso({
    EquityGMP.class,
    EquityRepoMargin.class,
    EquityDividend.class
})
public class EquityByDmd {

    protected String calendar;
    protected String currency;
    protected String dmd;
    protected String equity;
    protected String market;
    protected Double publishDate;
    protected String publishMethod;
    protected String type;

    /**
     * Obtiene el valor de la propiedad calendar.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCalendar() {
        return calendar;
    }

    /**
     * Define el valor de la propiedad calendar.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCalendar(String value) {
        this.calendar = value;
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

    /**
     * Obtiene el valor de la propiedad dmd.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDmd() {
        return dmd;
    }

    /**
     * Define el valor de la propiedad dmd.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDmd(String value) {
        this.dmd = value;
    }

    /**
     * Obtiene el valor de la propiedad equity.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEquity() {
        return equity;
    }

    /**
     * Define el valor de la propiedad equity.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEquity(String value) {
        this.equity = value;
    }

    /**
     * Obtiene el valor de la propiedad market.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMarket() {
        return market;
    }

    /**
     * Define el valor de la propiedad market.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMarket(String value) {
        this.market = value;
    }

    /**
     * Obtiene el valor de la propiedad publishDate.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getPublishDate() {
        return publishDate;
    }

    /**
     * Define el valor de la propiedad publishDate.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setPublishDate(Double value) {
        this.publishDate = value;
    }

    /**
     * Obtiene el valor de la propiedad publishMethod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublishMethod() {
        return publishMethod;
    }

    /**
     * Define el valor de la propiedad publishMethod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublishMethod(String value) {
        this.publishMethod = value;
    }

    /**
     * Obtiene el valor de la propiedad type.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Define el valor de la propiedad type.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

}
