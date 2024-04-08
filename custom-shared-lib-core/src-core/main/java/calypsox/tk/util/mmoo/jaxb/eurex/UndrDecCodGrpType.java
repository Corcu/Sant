//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour undrDecCodGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="undrDecCodGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="undrIdCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}undrIdCodType"/>
 *         &lt;element name="undrIdLngNam" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}undrIdLngNamType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "undrDecCodGrpType", propOrder = {
    "undrIdCod",
    "undrIdLngNam"
})
public class UndrDecCodGrpType {

    @XmlElement(required = true)
    protected String undrIdCod;
    @XmlElement(required = true)
    protected String undrIdLngNam;

    /**
     * Obtient la valeur de la propri?t? undrIdCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUndrIdCod() {
        return undrIdCod;
    }

    /**
     * D?finit la valeur de la propri?t? undrIdCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUndrIdCod(String value) {
        this.undrIdCod = value;
    }

    /**
     * Obtient la valeur de la propri?t? undrIdLngNam.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUndrIdLngNam() {
        return undrIdLngNam;
    }

    /**
     * D?finit la valeur de la propri?t? undrIdLngNam.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUndrIdLngNam(String value) {
        this.undrIdLngNam = value;
    }

}
