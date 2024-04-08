//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantacion de la referencia de enlace (JAXB) XML v2.2.8-b130911.1802 
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perderan si se vuelve a compilar el esquema de origen. 
// Generado el: 2020.02.03 a las 09:32:46 AM CET 
//


package calypsox.engine.dataimport.BondElements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para anonymous complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}fechaCall"/>
 *         &lt;element ref="{}fechaCallFin"/>
 *         &lt;element ref="{}fechaEjercicio"/>
 *         &lt;element ref="{}strikePriceCall"/>
 *         &lt;element ref="{}ejercicioOp"/>
 *         &lt;element ref="{}facAmortOp"/>
 *         &lt;element ref="{}porcentajeNomOp"/>
 *         &lt;element ref="{}cantidadAmortOp"/>
 *         &lt;element ref="{}numTitulosCall"/>
 *         &lt;element ref="{}couponRedemptionPaymentCall"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "fechaCall",
    "fechaCallFin",
    "fechaEjercicio",
    "strikePriceCall",
    "ejercicioOp",
    "facAmortOp",
    "porcentajeNomOp",
    "cantidadAmortOp",
    "numTitulosCall",
    "couponRedemptionPaymentCall"
})
@XmlRootElement(name = "fecCall")
public class FecCall {

    @XmlElement(required = true)
    protected String fechaCall;
    @XmlElement(required = true)
    protected String fechaCallFin;
    @XmlElement(required = true)
    protected String fechaEjercicio;
    protected double strikePriceCall;
    @XmlElement(required = true)
    protected String ejercicioOp;
    @XmlElement(required = true)
    protected double facAmortOp;
    protected double porcentajeNomOp;
    @XmlElement(required = true)
    protected String cantidadAmortOp;
    @XmlElement(required = true)
    protected String numTitulosCall;
    @XmlElement(required = true)
    protected String couponRedemptionPaymentCall;

    /**
     * Obtiene el valor de la propiedad fechaCall.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFechaCall() {
        return fechaCall;
    }

    /**
     * Define el valor de la propiedad fechaCall.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFechaCall(String value) {
        this.fechaCall = value;
    }

    /**
     * Obtiene el valor de la propiedad fechaCallFin.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFechaCallFin() {
        return fechaCallFin;
    }

    /**
     * Define el valor de la propiedad fechaCallFin.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFechaCallFin(String value) {
        this.fechaCallFin = value;
    }

    /**
     * Obtiene el valor de la propiedad fechaEjercicio.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFechaEjercicio() {
        return fechaEjercicio;
    }

    /**
     * Define el valor de la propiedad fechaEjercicio.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFechaEjercicio(String value) {
        this.fechaEjercicio = value;
    }

    /**
     * Obtiene el valor de la propiedad strikePriceCall.
     * 
     */
    public double getStrikePriceCall() {
        return strikePriceCall;
    }

    /**
     * Define el valor de la propiedad strikePriceCall.
     * 
     */
    public void setStrikePriceCall(double value) {
        this.strikePriceCall = value;
    }

    /**
     * Obtiene el valor de la propiedad ejercicioOp.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEjercicioOp() {
        return ejercicioOp;
    }

    /**
     * Define el valor de la propiedad ejercicioOp.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEjercicioOp(String value) {
        this.ejercicioOp = value;
    }

    /**
     * Obtiene el valor de la propiedad facAmortOp.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public double getFacAmortOp() {
        return facAmortOp;
    }

    /**
     * Define el valor de la propiedad facAmortOp.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFacAmortOp(double value) {
        this.facAmortOp = value;
    }

    /**
     * Obtiene el valor de la propiedad porcentajeNomOp.
     * 
     */
    public double getPorcentajeNomOp() {
        return porcentajeNomOp;
    }

    /**
     * Define el valor de la propiedad porcentajeNomOp.
     * 
     */
    public void setPorcentajeNomOp(double value) {
        this.porcentajeNomOp = value;
    }

    /**
     * Obtiene el valor de la propiedad cantidadAmortOp.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCantidadAmortOp() {
        return cantidadAmortOp;
    }

    /**
     * Define el valor de la propiedad cantidadAmortOp.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCantidadAmortOp(String value) {
        this.cantidadAmortOp = value;
    }

    /**
     * Obtiene el valor de la propiedad numTitulosCall.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNumTitulosCall() {
        return numTitulosCall;
    }

    /**
     * Define el valor de la propiedad numTitulosCall.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNumTitulosCall(String value) {
        this.numTitulosCall = value;
    }

    /**
     * Obtiene el valor de la propiedad couponRedemptionPaymentCall.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCouponRedemptionPaymentCall() {
        return couponRedemptionPaymentCall;
    }

    /**
     * Define el valor de la propiedad couponRedemptionPaymentCall.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCouponRedemptionPaymentCall(String value) {
        this.couponRedemptionPaymentCall = value;
    }

}
