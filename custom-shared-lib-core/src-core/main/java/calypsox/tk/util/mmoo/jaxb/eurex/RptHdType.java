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
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Classe Java pour rptHdType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="rptHdType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="exchNam" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}exchNamType"/>
 *         &lt;element name="envText" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}envTextType"/>
 *         &lt;element name="rptCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}rptCodType"/>
 *         &lt;element name="rptNam" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}rptNamType"/>
 *         &lt;element name="rptPrntEffDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}rptPrntEffDatType"/>
 *         &lt;element name="rptPrntRunDat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}rptPrntRunDatType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rptHdType", propOrder = {
    "exchNam",
    "envText",
    "rptCod",
    "rptNam",
    "rptPrntEffDat",
    "rptPrntRunDat"
})
public class RptHdType {

    @XmlElement(required = true)
    protected ExchNamType exchNam;
    @XmlElement(required = true)
    protected String envText;
    @XmlElement(required = true)
    protected String rptCod;
    @XmlElement(required = true)
    protected String rptNam;
    @XmlElement(required = true)
    protected XMLGregorianCalendar rptPrntEffDat;
    @XmlElement(required = true)
    protected XMLGregorianCalendar rptPrntRunDat;

    /**
     * Obtient la valeur de la propri?t? exchNam.
     * 
     * @return
     *     possible object is
     *     {@link ExchNamType }
     *     
     */
    public ExchNamType getExchNam() {
        return exchNam;
    }

    /**
     * D?finit la valeur de la propri?t? exchNam.
     * 
     * @param value
     *     allowed object is
     *     {@link ExchNamType }
     *     
     */
    public void setExchNam(ExchNamType value) {
        this.exchNam = value;
    }

    /**
     * Obtient la valeur de la propri?t? envText.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEnvText() {
        return envText;
    }

    /**
     * D?finit la valeur de la propri?t? envText.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEnvText(String value) {
        this.envText = value;
    }

    /**
     * Obtient la valeur de la propri?t? rptCod.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRptCod() {
        return rptCod;
    }

    /**
     * D?finit la valeur de la propri?t? rptCod.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRptCod(String value) {
        this.rptCod = value;
    }

    /**
     * Obtient la valeur de la propri?t? rptNam.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRptNam() {
        return rptNam;
    }

    /**
     * D?finit la valeur de la propri?t? rptNam.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRptNam(String value) {
        this.rptNam = value;
    }

    /**
     * Obtient la valeur de la propri?t? rptPrntEffDat.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getRptPrntEffDat() {
        return rptPrntEffDat;
    }

    /**
     * D?finit la valeur de la propri?t? rptPrntEffDat.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setRptPrntEffDat(XMLGregorianCalendar value) {
        this.rptPrntEffDat = value;
    }

    /**
     * Obtient la valeur de la propri?t? rptPrntRunDat.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getRptPrntRunDat() {
        return rptPrntRunDat;
    }

    /**
     * D?finit la valeur de la propri?t? rptPrntRunDat.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setRptPrntRunDat(XMLGregorianCalendar value) {
        this.rptPrntRunDat = value;
    }

}
