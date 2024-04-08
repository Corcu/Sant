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
 * <p>Classe Java pour txnDatTimType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="txnDatTimType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="trnDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}trnDatType" minOccurs="0"/>
 *         &lt;element name="trnTim" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}trnTimType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "txnDatTimType", propOrder = {
    "trnDat",
    "trnTim"
})
public class TxnDatTimType {

    protected XMLGregorianCalendar trnDat;
    protected XMLGregorianCalendar trnTim;

    /**
     * Obtient la valeur de la propri?t? trnDat.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTrnDat() {
        return trnDat;
    }

    /**
     * D?finit la valeur de la propri?t? trnDat.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTrnDat(XMLGregorianCalendar value) {
        this.trnDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? trnTim.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTrnTim() {
        return trnTim;
    }

    /**
     * D?finit la valeur de la propri?t? trnTim.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTrnTim(XMLGregorianCalendar value) {
        this.trnTim = value;
    }

}
