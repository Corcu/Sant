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
 * <p>Clase Java para equityGMP complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="equityGMP">
 *   &lt;complexContent>
 *     &lt;extension base="{http://webservice.efs2.isban.com/}equityByDmd">
 *       &lt;sequence>
 *         &lt;element name="gmpValue" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "equityGMP", propOrder = {
    "gmpValue"
})
public class EquityGMP
    extends EquityByDmd
{

    protected Double gmpValue;

    /**
     * Obtiene el valor de la propiedad gmpValue.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getGmpValue() {
        return gmpValue;
    }

    /**
     * Define el valor de la propiedad gmpValue.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setGmpValue(Double value) {
        this.gmpValue = value;
    }

}
