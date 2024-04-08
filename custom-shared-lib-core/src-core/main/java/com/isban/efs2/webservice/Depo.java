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
 * <p>Clase Java para depo complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="depo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="curveName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="future" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="rateSM" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "depo", propOrder = {
    "curveName",
    "future",
    "rateSM"
})
public class Depo {

    protected String curveName;
    protected String future;
    protected double rateSM;

    /**
     * Obtiene el valor de la propiedad curveName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurveName() {
        return curveName;
    }

    /**
     * Define el valor de la propiedad curveName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurveName(String value) {
        this.curveName = value;
    }

    /**
     * Obtiene el valor de la propiedad future.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFuture() {
        return future;
    }

    /**
     * Define el valor de la propiedad future.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFuture(String value) {
        this.future = value;
    }

    /**
     * Obtiene el valor de la propiedad rateSM.
     * 
     */
    public double getRateSM() {
        return rateSM;
    }

    /**
     * Define el valor de la propiedad rateSM.
     * 
     */
    public void setRateSM(double value) {
        this.rateSM = value;
    }

}
