//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantaci?n de la referencia de enlace (JAXB) XML v2.2.7
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perder?n si se vuelve a compilar el esquema de origen.
// Generado el: 2014.01.02 a las 03:34:20 PM CET 
//


package com.isban.efs2.webservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para dividend complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="dividend">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="bid" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="ask" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="exDate" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="payDate" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="proportional" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="updateDate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dividend", propOrder = {
    "bid",
    "ask",
    "exDate",
    "payDate",
    "proportional",
    "type",
    "updateDate"
})
public class Dividend {

    protected Double bid;
    protected Double ask;
    protected Double exDate;
    protected Double payDate;
    @XmlElement(defaultValue = "-1")
    protected Integer proportional;
    protected String type;
    protected String updateDate;

    /**
     * Obtiene el valor de la propiedad bid.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getBid() {
        return bid;
    }

    /**
     * Define el valor de la propiedad bid.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setBid(Double value) {
        this.bid = value;
    }

    /**
     * Obtiene el valor de la propiedad ask.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getAsk() {
        return ask;
    }

    /**
     * Define el valor de la propiedad ask.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setAsk(Double value) {
        this.ask = value;
    }

    /**
     * Obtiene el valor de la propiedad exDate.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getExDate() {
        return exDate;
    }

    /**
     * Define el valor de la propiedad exDate.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setExDate(Double value) {
        this.exDate = value;
    }

    /**
     * Obtiene el valor de la propiedad payDate.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getPayDate() {
        return payDate;
    }

    /**
     * Define el valor de la propiedad payDate.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setPayDate(Double value) {
        this.payDate = value;
    }

    /**
     * Obtiene el valor de la propiedad proportional.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getProportional() {
        return proportional;
    }

    /**
     * Define el valor de la propiedad proportional.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setProportional(Integer value) {
        this.proportional = value;
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

    /**
     * Obtiene el valor de la propiedad updateDate.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUpdateDate() {
        return updateDate;
    }

    /**
     * Define el valor de la propiedad updateDate.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUpdateDate(String value) {
        this.updateDate = value;
    }

}
