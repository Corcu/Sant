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
 * <p>Classe Java pour trnTypGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="trnTypGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ordrTrnTyp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}ordrTrnTypType"/>
 *         &lt;element name="ordrMtnCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}ordrMtnCodType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "trnTypGrpType", propOrder = {
    "ordrTrnTyp",
    "ordrMtnCod"
})
public class TrnTypGrpType {

    @XmlElement(required = true)
    protected String ordrTrnTyp;
    @XmlElement(required = true)
    protected String ordrMtnCod;

    /**
     * Obtient la valeur de la propri?t? ordrTrnTyp.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrdrTrnTyp() {
        return ordrTrnTyp;
    }

    /**
     * D?finit la valeur de la propri?t? ordrTrnTyp.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrdrTrnTyp(String value) {
        this.ordrTrnTyp = value;
    }

    /**
     * Obtient la valeur de la propri?t? ordrMtnCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrdrMtnCod() {
        return ordrMtnCod;
    }

    /**
     * D?finit la valeur de la propri?t? ordrMtnCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrdrMtnCod(String value) {
        this.ordrMtnCod = value;
    }

}
