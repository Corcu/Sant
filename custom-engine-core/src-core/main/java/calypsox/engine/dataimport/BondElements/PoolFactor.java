//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantacion de la referencia de enlace (JAXB) XML v2.2.8-b130911.1802 
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perderan si se vuelve a compilar el esquema de origen. 
// Generado el: 2020.02.03 a las 09:32:46 AM CET 
//


package calypsox.engine.dataimport.BondElements;

import java.math.BigDecimal;
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
 *         &lt;element ref="{}poolFactorValue"/>
 *         &lt;element ref="{}poolFactorDate"/>
 *         &lt;element ref="{}couponRedemptionPaymentPF"/>
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
    "poolFactorValue",
    "poolFactorDate",
    "couponRedemptionPaymentPF"
})
@XmlRootElement(name = "poolFactor")
public class PoolFactor {

    @XmlElement(required = true)
    protected BigDecimal poolFactorValue;
    @XmlElement(required = true)
    protected String poolFactorDate;
    @XmlElement(required = true)
    protected String couponRedemptionPaymentPF;

    /**
     * Obtiene el valor de la propiedad poolFactorValue.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getPoolFactorValue() {
        return poolFactorValue;
    }

    /**
     * Define el valor de la propiedad poolFactorValue.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setPoolFactorValue(BigDecimal value) {
        this.poolFactorValue = value;
    }

    /**
     * Obtiene el valor de la propiedad poolFactorDate.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPoolFactorDate() {
        return poolFactorDate;
    }

    /**
     * Define el valor de la propiedad poolFactorDate.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPoolFactorDate(String value) {
        this.poolFactorDate = value;
    }

    /**
     * Obtiene el valor de la propiedad couponRedemptionPaymentPF.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCouponRedemptionPaymentPF() {
        return couponRedemptionPaymentPF;
    }

    /**
     * Define el valor de la propiedad couponRedemptionPaymentPF.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCouponRedemptionPaymentPF(String value) {
        this.couponRedemptionPaymentPF = value;
    }

}
