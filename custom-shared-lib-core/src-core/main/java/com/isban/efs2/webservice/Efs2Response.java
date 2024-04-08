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
 * <p>Clase Java para efs2Response complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="efs2Response">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="internalCode" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="internalDescription" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="objectValue" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="responseCode" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="responseDescription" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;choice minOccurs="0">
 *           &lt;element name="equityRepoMargin" type="{http://webservice.efs2.isban.com/}equityRepoMargin"/>
 *           &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *           &lt;element name="equityDividend" type="{http://webservice.efs2.isban.com/}equityDividend"/>
 *           &lt;element name="fxSpot" type="{http://webservice.efs2.isban.com/}fxSpot"/>
 *           &lt;element name="fxSwapPointForDate" type="{http://webservice.efs2.isban.com/}fxSwapPointForDate"/>
 *           &lt;element name="depo" type="{http://webservice.efs2.isban.com/}depo"/>
 *           &lt;element name="fxDates" type="{http://webservice.efs2.isban.com/}fxDates"/>
 *           &lt;element name="quotesLists" type="{http://webservice.efs2.isban.com/}quotesLists"/>
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
@XmlType(name = "efs2Response", propOrder = {
    "internalCode",
    "internalDescription",
    "objectValue",
    "responseCode",
    "responseDescription",
    "equityRepoMargin",
    "value",
    "equityDividend",
    "fxSpot",
    "fxSwapPointForDate",
    "depo",
    "fxDates",
    "quotesLists"
})
public class Efs2Response {

    protected int internalCode;
    protected String internalDescription;
    protected Object objectValue;
    protected int responseCode;
    protected String responseDescription;
    protected EquityRepoMargin equityRepoMargin;
    protected String value;
    protected EquityDividend equityDividend;
    protected FxSpot fxSpot;
    protected FxSwapPointForDate fxSwapPointForDate;
    protected Depo depo;
    protected FxDates fxDates;
    protected QuotesLists quotesLists;

    /**
     * Obtiene el valor de la propiedad internalCode.
     * 
     */
    public int getInternalCode() {
        return internalCode;
    }

    /**
     * Define el valor de la propiedad internalCode.
     * 
     */
    public void setInternalCode(int value) {
        this.internalCode = value;
    }

    /**
     * Obtiene el valor de la propiedad internalDescription.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInternalDescription() {
        return internalDescription;
    }

    /**
     * Define el valor de la propiedad internalDescription.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInternalDescription(String value) {
        this.internalDescription = value;
    }

    /**
     * Obtiene el valor de la propiedad objectValue.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getObjectValue() {
        return objectValue;
    }

    /**
     * Define el valor de la propiedad objectValue.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setObjectValue(Object value) {
        this.objectValue = value;
    }

    /**
     * Obtiene el valor de la propiedad responseCode.
     * 
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Define el valor de la propiedad responseCode.
     * 
     */
    public void setResponseCode(int value) {
        this.responseCode = value;
    }

    /**
     * Obtiene el valor de la propiedad responseDescription.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResponseDescription() {
        return responseDescription;
    }

    /**
     * Define el valor de la propiedad responseDescription.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResponseDescription(String value) {
        this.responseDescription = value;
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
     * Obtiene el valor de la propiedad fxSwapPointForDate.
     * 
     * @return
     *     possible object is
     *     {@link FxSwapPointForDate }
     *     
     */
    public FxSwapPointForDate getFxSwapPointForDate() {
        return fxSwapPointForDate;
    }

    /**
     * Define el valor de la propiedad fxSwapPointForDate.
     * 
     * @param value
     *     allowed object is
     *     {@link FxSwapPointForDate }
     *     
     */
    public void setFxSwapPointForDate(FxSwapPointForDate value) {
        this.fxSwapPointForDate = value;
    }

    /**
     * Obtiene el valor de la propiedad depo.
     * 
     * @return
     *     possible object is
     *     {@link Depo }
     *     
     */
    public Depo getDepo() {
        return depo;
    }

    /**
     * Define el valor de la propiedad depo.
     * 
     * @param value
     *     allowed object is
     *     {@link Depo }
     *     
     */
    public void setDepo(Depo value) {
        this.depo = value;
    }

    /**
     * Obtiene el valor de la propiedad fxDates.
     * 
     * @return
     *     possible object is
     *     {@link FxDates }
     *     
     */
    public FxDates getFxDates() {
        return fxDates;
    }

    /**
     * Define el valor de la propiedad fxDates.
     * 
     * @param value
     *     allowed object is
     *     {@link FxDates }
     *     
     */
    public void setFxDates(FxDates value) {
        this.fxDates = value;
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

}
