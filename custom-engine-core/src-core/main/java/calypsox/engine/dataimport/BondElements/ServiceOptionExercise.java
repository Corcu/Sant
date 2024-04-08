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
 *         &lt;element ref="{}divisaLiquid"/>
 *         &lt;element ref="{}formaEjercicioOp"/>
 *         &lt;element ref="{}listaCall"/>
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
    "divisaLiquid",
    "formaEjercicioOp",
    "listaCall"
})
@XmlRootElement(name = "serviceOptionExercise")
public class ServiceOptionExercise {

    @XmlElement(required = true)
    protected String divisaLiquid;
    @XmlElement(required = true)
    protected String formaEjercicioOp;
    @XmlElement(required = true)
    protected ListaCall listaCall;

    /**
     * Obtiene el valor de la propiedad divisaLiquid.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDivisaLiquid() {
        return divisaLiquid;
    }

    /**
     * Define el valor de la propiedad divisaLiquid.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDivisaLiquid(String value) {
        this.divisaLiquid = value;
    }

    /**
     * Obtiene el valor de la propiedad formaEjercicioOp.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFormaEjercicioOp() {
        return formaEjercicioOp;
    }

    /**
     * Define el valor de la propiedad formaEjercicioOp.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFormaEjercicioOp(String value) {
        this.formaEjercicioOp = value;
    }

    /**
     * Obtiene el valor de la propiedad listaCall.
     * 
     * @return
     *     possible object is
     *     {@link ListaCall }
     *     
     */
    public ListaCall getListaCall() {
        return listaCall;
    }

    /**
     * Define el valor de la propiedad listaCall.
     * 
     * @param value
     *     allowed object is
     *     {@link ListaCall }
     *     
     */
    public void setListaCall(ListaCall value) {
        this.listaCall = value;
    }

}
