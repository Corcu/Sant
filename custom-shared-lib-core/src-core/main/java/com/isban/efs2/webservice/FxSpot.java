//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantaci?n de la referencia de enlace (JAXB) XML v2.2.7
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perder?n si se vuelve a compilar el esquema de origen.
// Generado el: 2014.01.02 a las 03:34:20 PM CET 
//


package com.isban.efs2.webservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para fxSpot complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="fxSpot">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="currency1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="currency2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="currencyPair" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fxSpotValue" type="{http://webservice.efs2.isban.com/}valueData" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fxSpot", propOrder = {
    "currency1",
    "currency2",
    "currencyPair",
    "fxSpotValue"
})
public class FxSpot {

    protected String currency1;
    protected String currency2;
    protected String currencyPair;
    protected ValueData fxSpotValue;

    /**
     * Obtiene el valor de la propiedad currency1.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrency1() {
        return currency1;
    }

    /**
     * Define el valor de la propiedad currency1.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrency1(String value) {
        this.currency1 = value;
    }

    /**
     * Obtiene el valor de la propiedad currency2.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrency2() {
        return currency2;
    }

    /**
     * Define el valor de la propiedad currency2.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrency2(String value) {
        this.currency2 = value;
    }

    /**
     * Obtiene el valor de la propiedad currencyPair.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrencyPair() {
        return currencyPair;
    }

    /**
     * Define el valor de la propiedad currencyPair.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrencyPair(String value) {
        this.currencyPair = value;
    }

    /**
     * Obtiene el valor de la propiedad fxSpotValue.
     * 
     * @return
     *     possible object is
     *     {@link ValueData }
     *     
     */
    public ValueData getFxSpotValue() {
        return fxSpotValue;
    }

    /**
     * Define el valor de la propiedad fxSpotValue.
     * 
     * @param value
     *     allowed object is
     *     {@link ValueData }
     *     
     */
    public void setFxSpotValue(ValueData value) {
        this.fxSpotValue = value;
    }

}
