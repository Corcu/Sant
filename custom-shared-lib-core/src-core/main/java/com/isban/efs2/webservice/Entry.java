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
 * <p>Clase Java para entry complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="entry">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;choice minOccurs="0">
 *           &lt;element name="equityRepoMargin" type="{http://webservice.efs2.isban.com/}equityRepoMargin"/>
 *           &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *           &lt;element name="equityDividend" type="{http://webservice.efs2.isban.com/}equityDividend"/>
 *           &lt;element name="fxSpot" type="{http://webservice.efs2.isban.com/}fxSpot"/>
 *           &lt;element name="quotesLists" type="{http://webservice.efs2.isban.com/}quotesLists"/>
 *           &lt;element name="equityGMPList" type="{http://webservice.efs2.isban.com/}equityGMPList"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "entry", propOrder = {
    "key",
    "equityRepoMargin",
    "value",
    "equityDividend",
    "fxSpot",
    "quotesLists",
    "equityGMPList"
})
public class Entry {

    protected String key;
    protected EquityRepoMargin equityRepoMargin;
    protected String value;
    protected EquityDividend equityDividend;
    protected FxSpot fxSpot;
    protected QuotesLists quotesLists;
    protected EquityGMPList equityGMPList;

    /**
     * Obtiene el valor de la propiedad key.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKey() {
        return key;
    }

    /**
     * Define el valor de la propiedad key.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKey(String value) {
        this.key = value;
    }

    /**
     * Obtiene el valor de la propiedad equityRepoMargin.
     * 
     * @return
     *     possible object is
     *     {@link EquityRepoMargin }
     *     
     */
    public EquityRepoMargin getEquityRepoMargin() {
        return equityRepoMargin;
    }

    /**
     * Define el valor de la propiedad equityRepoMargin.
     * 
     * @param value
     *     allowed object is
     *     {@link EquityRepoMargin }
     *     
     */
    public void setEquityRepoMargin(EquityRepoMargin value) {
        this.equityRepoMargin = value;
    }

    /**
     * Obtiene el valor de la propiedad value.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Define el valor de la propiedad value.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Obtiene el valor de la propiedad equityDividend.
     * 
     * @return
     *     possible object is
     *     {@link EquityDividend }
     *     
     */
    public EquityDividend getEquityDividend() {
        return equityDividend;
    }

    /**
     * Define el valor de la propiedad equityDividend.
     * 
     * @param value
     *     allowed object is
     *     {@link EquityDividend }
     *     
     */
    public void setEquityDividend(EquityDividend value) {
        this.equityDividend = value;
    }

    /**
     * Obtiene el valor de la propiedad fxSpot.
     * 
     * @return
     *     possible object is
     *     {@link FxSpot }
     *     
     */
    public FxSpot getFxSpot() {
        return fxSpot;
    }

    /**
     * Define el valor de la propiedad fxSpot.
     * 
     * @param value
     *     allowed object is
     *     {@link FxSpot }
     *     
     */
    public void setFxSpot(FxSpot value) {
        this.fxSpot = value;
    }

    /**
     * Obtiene el valor de la propiedad quotesLists.
     * 
     * @return
     *     possible object is
     *     {@link QuotesLists }
     *     
     */
    public QuotesLists getQuotesLists() {
        return quotesLists;
    }

    /**
     * Define el valor de la propiedad quotesLists.
     * 
     * @param value
     *     allowed object is
     *     {@link QuotesLists }
     *     
     */
    public void setQuotesLists(QuotesLists value) {
        this.quotesLists = value;
    }

    /**
     * Obtiene el valor de la propiedad equityGMPList.
     * 
     * @return
     *     possible object is
     *     {@link EquityGMPList }
     *     
     */
    public EquityGMPList getEquityGMPList() {
        return equityGMPList;
    }

    /**
     * Define el valor de la propiedad equityGMPList.
     * 
     * @param value
     *     allowed object is
     *     {@link EquityGMPList }
     *     
     */
    public void setEquityGMPList(EquityGMPList value) {
        this.equityGMPList = value;
    }

}
