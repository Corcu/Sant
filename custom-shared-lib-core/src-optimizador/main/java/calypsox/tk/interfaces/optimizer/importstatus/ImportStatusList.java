//
// Este archivo ha sido generado por la arquitectura JavaTM para la implantación de la referencia de enlace (JAXB) XML v2.2.6 
// Visite <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Todas las modificaciones realizadas en este archivo se perderán si se vuelve a compilar el esquema de origen. 
// Generado el: PM.04.23 a las 12:05:49 PM CEST 
//


package calypsox.tk.interfaces.optimizer.importstatus;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para importStatusListType complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>
 * &lt;complexType name="importStatusListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="importStatus" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="importKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *                   &lt;element name="importStatus" type="{http://collateral.optimization.isban.com/}importStatus"/>
 *                   &lt;element name="errors" type="{http://collateral.optimization.isban.com/}errorsList" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "importStatusListType", propOrder = {
    "importStatuses"
})
@XmlRootElement(name = "importStatusList")
public class ImportStatusList {

    @XmlElement(name = "importStatus", required = true)
    protected List<ImportStatusList.ImportStatus> importStatuses;

    /**
     * Gets the value of the importStatuses property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the importStatuses property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getImportStatuses().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ImportStatusList.ImportStatus }
     * 
     * 
     */
    public List<ImportStatusList.ImportStatus> getImportStatuses() {
        if (importStatuses == null) {
            importStatuses = new ArrayList<ImportStatusList.ImportStatus>();
        }
        return this.importStatuses;
    }


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
     *         &lt;element name="importKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *         &lt;element name="importStatus" type="{http://collateral.optimization.isban.com/}importStatus"/>
     *         &lt;element name="errors" type="{http://collateral.optimization.isban.com/}errorsList" minOccurs="0"/>
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
        "importKey",
        "importStatus",
        "errors"
    })
    public static class ImportStatus {

        @XmlElement(required = true)
        protected String importKey;
        @XmlElement(required = true)
        protected String importStatus;
        protected ErrorsList errors;

        /**
         * Obtiene el valor de la propiedad importKey.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getImportKey() {
            return importKey;
        }

        /**
         * Define el valor de la propiedad importKey.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setImportKey(String value) {
            this.importKey = value;
        }

        /**
         * Obtiene el valor de la propiedad importStatus.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getImportStatus() {
            return importStatus;
        }

        /**
         * Define el valor de la propiedad importStatus.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setImportStatus(String value) {
            this.importStatus = value;
        }

        /**
         * Obtiene el valor de la propiedad errors.
         * 
         * @return
         *     possible object is
         *     {@link ErrorsList }
         *     
         */
        public ErrorsList getErrors() {
            return errors;
        }

        /**
         * Define el valor de la propiedad errors.
         * 
         * @param value
         *     allowed object is
         *     {@link ErrorsList }
         *     
         */
        public void setErrors(ErrorsList value) {
            this.errors = value;
        }

    }

}
