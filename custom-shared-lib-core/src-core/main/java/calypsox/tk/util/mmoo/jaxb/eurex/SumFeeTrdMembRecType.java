//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour sumFeeTrdMembRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumFeeTrdMembRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="currTypCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}currTypCodType"/>
 *         &lt;element name="sumTrdMembAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumTrdMembAmntType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumFeeTrdMembRecType", propOrder = {
    "currTypCod",
    "sumTrdMembAmnt"
})
public class SumFeeTrdMembRecType {

    @XmlElement(required = true)
    protected String currTypCod;
    @XmlElement(required = true)
    protected BigDecimal sumTrdMembAmnt;

    /**
     * Obtient la valeur de la propri?t? currTypCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrTypCod() {
        return currTypCod;
    }

    /**
     * D?finit la valeur de la propri?t? currTypCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrTypCod(String value) {
        this.currTypCod = value;
    }

    /**
     * Obtient la valeur de la propri?t? sumTrdMembAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumTrdMembAmnt() {
        return sumTrdMembAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumTrdMembAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumTrdMembAmnt(BigDecimal value) {
        this.sumTrdMembAmnt = value;
    }

}
