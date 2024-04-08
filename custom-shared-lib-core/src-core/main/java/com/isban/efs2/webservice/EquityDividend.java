//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantaci?n de la referencia de enlace (JAXB) XML v2.2.7
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perder?n si se vuelve a compilar el esquema de origen.
// Generado el: 2014.01.02 a las 03:34:20 PM CET 
//


package com.isban.efs2.webservice;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para equityDividend complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="equityDividend">
 *   &lt;complexContent>
 *     &lt;extension base="{http://webservice.efs2.isban.com/}equityByDmd">
 *       &lt;sequence>
 *         &lt;element name="dividend" type="{http://webservice.efs2.isban.com/}dividend" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "equityDividend", propOrder = {
    "dividend"
})
public class EquityDividend
    extends EquityByDmd
{

    protected List<Dividend> dividend;

    /**
     * Gets the value of the dividend property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dividend property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDividend().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Dividend }
     * 
     * 
     */
    public List<Dividend> getDividend() {
        if (dividend == null) {
            dividend = new ArrayList<Dividend>();
        }
        return this.dividend;
    }

}
