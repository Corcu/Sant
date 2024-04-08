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
 *         &lt;element ref="{}transactionKey"/>
 *         &lt;element ref="{}action"/>
 *         &lt;element ref="{}symbol"/>
 *         &lt;element ref="{}listSubscribers"/>
 *         &lt;element ref="{}serviceOptionExercise"/>
 *         &lt;element ref="{}servicePoolFactor"/>
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
    "transactionKey",
    "action",
    "symbol",
    "listSubscribers",
    "serviceOptionExercise",
    "servicePoolFactor"
})
@XmlRootElement(name = "RDFlowTransaction")
public class RDFlowTransaction {

    protected int transactionKey;
    @XmlElement(required = true)
    protected String action;
    @XmlElement(required = true)
    protected String symbol;
    @XmlElement(required = true)
    protected ListSubscribers listSubscribers;
    @XmlElement(required = true)
    protected ServiceOptionExercise serviceOptionExercise;
    @XmlElement(required = true)
    protected ServicePoolFactor servicePoolFactor;

    /**
     * Obtiene el valor de la propiedad transactionKey.
     * 
     */
    public int getTransactionKey() {
        return transactionKey;
    }

    /**
     * Define el valor de la propiedad transactionKey.
     * 
     */
    public void setTransactionKey(int value) {
        this.transactionKey = value;
    }

    /**
     * Obtiene el valor de la propiedad action.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAction() {
        return action;
    }

    /**
     * Define el valor de la propiedad action.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAction(String value) {
        this.action = value;
    }

    /**
     * Obtiene el valor de la propiedad symbol.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Define el valor de la propiedad symbol.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSymbol(String value) {
        this.symbol = value;
    }

    /**
     * Obtiene el valor de la propiedad listSubscribers.
     * 
     * @return
     *     possible object is
     *     {@link ListSubscribers }
     *     
     */
    public ListSubscribers getListSubscribers() {
        return listSubscribers;
    }

    /**
     * Define el valor de la propiedad listSubscribers.
     * 
     * @param value
     *     allowed object is
     *     {@link ListSubscribers }
     *     
     */
    public void setListSubscribers(ListSubscribers value) {
        this.listSubscribers = value;
    }

    /**
     * Obtiene el valor de la propiedad serviceOptionExercise.
     * 
     * @return
     *     possible object is
     *     {@link ServiceOptionExercise }
     *     
     */
    public ServiceOptionExercise getServiceOptionExercise() {
        return serviceOptionExercise;
    }

    /**
     * Define el valor de la propiedad serviceOptionExercise.
     * 
     * @param value
     *     allowed object is
     *     {@link ServiceOptionExercise }
     *     
     */
    public void setServiceOptionExercise(ServiceOptionExercise value) {
        this.serviceOptionExercise = value;
    }

    /**
     * Obtiene el valor de la propiedad servicePoolFactor.
     * 
     * @return
     *     possible object is
     *     {@link ServicePoolFactor }
     *     
     */
    public ServicePoolFactor getServicePoolFactor() {
        return servicePoolFactor;
    }

    /**
     * Define el valor de la propiedad servicePoolFactor.
     * 
     * @param value
     *     allowed object is
     *     {@link ServicePoolFactor }
     *     
     */
    public void setServicePoolFactor(ServicePoolFactor value) {
        this.servicePoolFactor = value;
    }

}
