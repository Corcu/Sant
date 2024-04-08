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
 * <p>Classe Java pour sumClgSetlmtInstBillFeeRecType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="sumClgSetlmtInstBillFeeRecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="settlInst" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}settlInstType"/>
 *         &lt;element name="billTyp" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}billTypType"/>
 *         &lt;element name="currTypCod" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}currTypCodType"/>
 *         &lt;element name="sumClgSetlmtInstAmnt" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}sumClgSetlmtInstAmntType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sumClgSetlmtInstBillFeeRecType", propOrder = {
    "settlInst",
    "billTyp",
    "currTypCod",
    "sumClgSetlmtInstAmnt"
})
public class SumClgSetlmtInstBillFeeRecType {

    @XmlElement(required = true)
    protected String settlInst;
    @XmlElement(required = true)
    protected BillTypType billTyp;
    @XmlElement(required = true)
    protected String currTypCod;
    @XmlElement(required = true)
    protected BigDecimal sumClgSetlmtInstAmnt;

    /**
     * Obtient la valeur de la propri?t? settlInst.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSettlInst() {
        return settlInst;
    }

    /**
     * D?finit la valeur de la propri?t? settlInst.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSettlInst(String value) {
        this.settlInst = value;
    }

    /**
     * Obtient la valeur de la propri?t? billTyp.
     * 
     * @return
     *     possible object is
     *     {@link BillTypType }
     *     
     */
    public BillTypType getBillTyp() {
        return billTyp;
    }

    /**
     * D?finit la valeur de la propri?t? billTyp.
     * 
     * @param value
     *     allowed object is
     *     {@link BillTypType }
     *     
     */
    public void setBillTyp(BillTypType value) {
        this.billTyp = value;
    }

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
     * Obtient la valeur de la propri?t? sumClgSetlmtInstAmnt.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSumClgSetlmtInstAmnt() {
        return sumClgSetlmtInstAmnt;
    }

    /**
     * D?finit la valeur de la propri?t? sumClgSetlmtInstAmnt.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSumClgSetlmtInstAmnt(BigDecimal value) {
        this.sumClgSetlmtInstAmnt = value;
    }

}
