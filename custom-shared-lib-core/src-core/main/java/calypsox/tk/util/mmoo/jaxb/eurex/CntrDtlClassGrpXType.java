//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour cntrDtlClassGrpXType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cntrDtlClassGrpXType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cntrClasCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrClasCodType" minOccurs="0"/>
 *         &lt;element name="cntrDtlGrpX" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}cntrDtlGrpXType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cntrDtlClassGrpXType", propOrder = {
    "cntrClasCod",
    "cntrDtlGrpX"
})
public class CntrDtlClassGrpXType {

    protected CntrClasCodType cntrClasCod;
    protected CntrDtlGrpXType cntrDtlGrpX;

    /**
     * Obtient la valeur de la propri?t? cntrClasCod.
     * 
     * @return
     *     possible object is
     *     {@link CntrClasCodType }
     *     
     */
    public CntrClasCodType getCntrClasCod() {
        return cntrClasCod;
    }

    /**
     * D?finit la valeur de la propri?t? cntrClasCod.
     * 
     * @param value
     *     allowed object is
     *     {@link CntrClasCodType }
     *     
     */
    public void setCntrClasCod(CntrClasCodType value) {
        this.cntrClasCod = value;
    }

    /**
     * Obtient la valeur de la propri?t? cntrDtlGrpX.
     * 
     * @return
     *     possible object is
     *     {@link CntrDtlGrpXType }
     *     
     */
    public CntrDtlGrpXType getCntrDtlGrpX() {
        return cntrDtlGrpX;
    }

    /**
     * D?finit la valeur de la propri?t? cntrDtlGrpX.
     * 
     * @param value
     *     allowed object is
     *     {@link CntrDtlGrpXType }
     *     
     */
    public void setCntrDtlGrpX(CntrDtlGrpXType value) {
        this.cntrDtlGrpX = value;
    }

}
