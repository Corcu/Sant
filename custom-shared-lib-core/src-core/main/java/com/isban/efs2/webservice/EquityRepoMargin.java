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
 * <p>Clase Java para equityRepoMargin complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="equityRepoMargin">
 *   &lt;complexContent>
 *     &lt;extension base="{http://webservice.efs2.isban.com/}equityByDmd">
 *       &lt;sequence>
 *         &lt;element name="dateFormat" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="fixedTermFormat" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="repoMargin" type="{http://webservice.efs2.isban.com/}repoMargin" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="termsFormat" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "equityRepoMargin", propOrder = {
    "dateFormat",
    "fixedTermFormat",
    "repoMargin",
    "termsFormat"
})
public class EquityRepoMargin
    extends EquityByDmd
{

    protected String dateFormat;
    protected String fixedTermFormat;
    protected List<RepoMargin> repoMargin;
    protected String termsFormat;

    /**
     * Obtiene el valor de la propiedad dateFormat.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * Define el valor de la propiedad dateFormat.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDateFormat(String value) {
        this.dateFormat = value;
    }

    /**
     * Obtiene el valor de la propiedad fixedTermFormat.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFixedTermFormat() {
        return fixedTermFormat;
    }

    /**
     * Define el valor de la propiedad fixedTermFormat.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFixedTermFormat(String value) {
        this.fixedTermFormat = value;
    }

    /**
     * Gets the value of the repoMargin property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the repoMargin property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRepoMargin().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RepoMargin }
     * 
     * 
     */
    public List<RepoMargin> getRepoMargin() {
        if (repoMargin == null) {
            repoMargin = new ArrayList<RepoMargin>();
        }
        return this.repoMargin;
    }

    /**
     * Obtiene el valor de la propiedad termsFormat.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTermsFormat() {
        return termsFormat;
    }

    /**
     * Define el valor de la propiedad termsFormat.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTermsFormat(String value) {
        this.termsFormat = value;
    }

}
