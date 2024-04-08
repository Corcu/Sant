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
 *         &lt;element ref="{}listaPoolFactor"/>
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
    "listaPoolFactor"
})
@XmlRootElement(name = "servicePoolFactor")
public class ServicePoolFactor {

    @XmlElement(required = true)
    protected ListaPoolFactor listaPoolFactor;

    /**
     * Obtiene el valor de la propiedad listaPoolFactor.
     * 
     * @return
     *     possible object is
     *     {@link ListaPoolFactor }
     *     
     */
    public ListaPoolFactor getListaPoolFactor() {
        return listaPoolFactor;
    }

    /**
     * Define el valor de la propiedad listaPoolFactor.
     * 
     * @param value
     *     allowed object is
     *     {@link ListaPoolFactor }
     *     
     */
    public void setListaPoolFactor(ListaPoolFactor value) {
        this.listaPoolFactor = value;
    }

}
