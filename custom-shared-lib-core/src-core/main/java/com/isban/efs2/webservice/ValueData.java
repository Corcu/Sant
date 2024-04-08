//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantaci?n de la referencia de enlace (JAXB) XML v2.2.7
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perder?n si se vuelve a compilar el esquema de origen.
// Generado el: 2014.01.02 a las 03:34:20 PM CET 
//


package com.isban.efs2.webservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Clase Java para valueData complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="valueData">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ask" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="askTimeStamp" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="bid" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="bidTimeStamp" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="mid" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="midTimeStamp" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "valueData", propOrder = {
    "ask",
    "askTimeStamp",
    "bid",
    "bidTimeStamp",
    "mid",
    "midTimeStamp"
})
public class ValueData {

    protected Double ask;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar askTimeStamp;
    protected Double bid;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar bidTimeStamp;
    protected Double mid;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar midTimeStamp;

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
     * Obtiene el valor de la propiedad askTimeStamp.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getAskTimeStamp() {
        return askTimeStamp;
    }

    /**
     * Define el valor de la propiedad askTimeStamp.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setAskTimeStamp(XMLGregorianCalendar value) {
        this.askTimeStamp = value;
    }

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
     * Obtiene el valor de la propiedad bidTimeStamp.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getBidTimeStamp() {
        return bidTimeStamp;
    }

    /**
     * Define el valor de la propiedad bidTimeStamp.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setBidTimeStamp(XMLGregorianCalendar value) {
        this.bidTimeStamp = value;
    }

    /**
     * Obtiene el valor de la propiedad mid.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getMid() {
        return mid;
    }

    /**
     * Define el valor de la propiedad mid.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setMid(Double value) {
        this.mid = value;
    }

    /**
     * Obtiene el valor de la propiedad midTimeStamp.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getMidTimeStamp() {
        return midTimeStamp;
    }

    /**
     * Define el valor de la propiedad midTimeStamp.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setMidTimeStamp(XMLGregorianCalendar value) {
        this.midTimeStamp = value;
    }

}
