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
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Classe Java pour origTxnDatTimType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="origTxnDatTimType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="origTrnDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}origTrnDatType" minOccurs="0"/>
 *         &lt;element name="origTrnTim" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}origTrnTimType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "origTxnDatTimType", propOrder = {
    "origTrnDat",
    "origTrnTim"
})
public class OrigTxnDatTimType {

    protected XMLGregorianCalendar origTrnDat;
    protected XMLGregorianCalendar origTrnTim;

    /**
     * Obtient la valeur de la propri?t? origTrnDat.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getOrigTrnDat() {
        return origTrnDat;
    }

    /**
     * D?finit la valeur de la propri?t? origTrnDat.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setOrigTrnDat(XMLGregorianCalendar value) {
        this.origTrnDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? origTrnTim.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getOrigTrnTim() {
        return origTrnTim;
    }

    /**
     * D?finit la valeur de la propri?t? origTrnTim.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setOrigTrnTim(XMLGregorianCalendar value) {
        this.origTrnTim = value;
    }

}
