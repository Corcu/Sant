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
 * <p>Classe Java pour cc060RecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="cc060RecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="acctTypGrp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}acctTypGrpType"/>
 *         &lt;element name="prtMgnUnadj" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}prtMgnUnadjType"/>
 *         &lt;element name="prtMgnReqt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}prtMgnReqtType"/>
 *         &lt;element name="exchRat" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}exchRatType"/>
 *         &lt;element name="totMgnClgCurr" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}totMgnClgCurrType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cc060RecType", propOrder = {
    "acctTypGrp",
    "prtMgnUnadj",
    "prtMgnReqt",
    "exchRat",
    "totMgnClgCurr"
})
public class Cc060RecType {

    @XmlElement(required = true)
    protected AcctTypGrpType acctTypGrp;
    @XmlElement(required = true)
    protected BigDecimal prtMgnUnadj;
    @XmlElement(required = true)
    protected BigDecimal prtMgnReqt;
    @XmlElement(required = true)
    protected BigDecimal exchRat;
    @XmlElement(required = true)
    protected BigDecimal totMgnClgCurr;

    /**
     * Obtient la valeur de la propri?t? acctTypGrp.
     * 
     * @return
     *     possible object is
     *     {@link AcctTypGrpType }
     *     
     */
    public AcctTypGrpType getAcctTypGrp() {
        return acctTypGrp;
    }

    /**
     * D?finit la valeur de la propri?t? acctTypGrp.
     * 
     * @param value
     *     allowed object is
     *     {@link AcctTypGrpType }
     *     
     */
    public void setAcctTypGrp(AcctTypGrpType value) {
        this.acctTypGrp = value;
    }

    /**
     * Obtient la valeur de la propri?t? prtMgnUnadj.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getPrtMgnUnadj() {
        return prtMgnUnadj;
    }

    /**
     * D?finit la valeur de la propri?t? prtMgnUnadj.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setPrtMgnUnadj(BigDecimal value) {
        this.prtMgnUnadj = value;
    }

    /**
     * Obtient la valeur de la propri?t? prtMgnReqt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getPrtMgnReqt() {
        return prtMgnReqt;
    }

    /**
     * D?finit la valeur de la propri?t? prtMgnReqt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setPrtMgnReqt(BigDecimal value) {
        this.prtMgnReqt = value;
    }

    /**
     * Obtient la valeur de la propri?t? exchRat.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getExchRat() {
        return exchRat;
    }

    /**
     * D?finit la valeur de la propri?t? exchRat.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setExchRat(BigDecimal value) {
        this.exchRat = value;
    }

    /**
     * Obtient la valeur de la propri?t? totMgnClgCurr.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getTotMgnClgCurr() {
        return totMgnClgCurr;
    }

    /**
     * D?finit la valeur de la propri?t? totMgnClgCurr.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setTotMgnClgCurr(BigDecimal value) {
        this.totMgnClgCurr = value;
    }

}
