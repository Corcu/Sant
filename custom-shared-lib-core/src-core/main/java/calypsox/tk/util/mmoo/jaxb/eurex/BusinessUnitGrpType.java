//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour businessUnitGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="businessUnitGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="businessUnit" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}businessUnitType"/>
 *         &lt;element name="busUntLngName" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}busUntLngNameType"/>
 *         &lt;element name="businessUnitId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}businessUnitIdType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "businessUnitGrpType", propOrder = {
    "businessUnit",
    "busUntLngName",
    "businessUnitId"
})
public class BusinessUnitGrpType {

    @XmlElement(required = true)
    protected String businessUnit;
    @XmlElement(required = true)
    protected String busUntLngName;
    @XmlElement(required = true)
    protected BigInteger businessUnitId;

    /**
     * Obtient la valeur de la propri?t? businessUnit.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBusinessUnit() {
        return businessUnit;
    }

    /**
     * D?finit la valeur de la propri?t? businessUnit.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBusinessUnit(String value) {
        this.businessUnit = value;
    }

    /**
     * Obtient la valeur de la propri?t? busUntLngName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBusUntLngName() {
        return busUntLngName;
    }

    /**
     * D?finit la valeur de la propri?t? busUntLngName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBusUntLngName(String value) {
        this.busUntLngName = value;
    }

    /**
     * Obtient la valeur de la propri?t? businessUnitId.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getBusinessUnitId() {
        return businessUnitId;
    }

    /**
     * D?finit la valeur de la propri?t? businessUnitId.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setBusinessUnitId(BigInteger value) {
        this.businessUnitId = value;
    }

}
