//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.09.29 at 03:45:16 PM BST 
//


package calypsox.repoccp.model.eurex;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for tc800RecType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tc800RecType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="legNo" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}legNoType"/>
 *         &lt;element name="buySellInd" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}buySellIndType"/>
 *         &lt;element name="rpoRefRtCod" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoRefRtCodType" minOccurs="0"/>
 *         &lt;element name="rpoTotQty" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoTotQtyType"/>
 *         &lt;element name="rpoTotAmnt" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoTotAmntType"/>
 *         &lt;element name="rpoIntRt" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoIntRtType"/>
 *         &lt;element name="rpoBps" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoBpsType" minOccurs="0"/>
 *         &lt;element name="rpoIntAmt" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoIntAmtType"/>
 *         &lt;element name="settlDatCtrct" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}settlDatCtrctType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tc800RecType", propOrder = {
    "legNo",
    "buySellInd",
    "rpoRefRtCod",
    "rpoTotQty",
    "rpoTotAmnt",
    "rpoIntRt",
    "rpoBps",
    "rpoIntAmt",
    "settlDatCtrct"
})
public class Tc800RecType {

    @XmlElement(required = true)
    protected BigInteger legNo;
    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected BuySellIndType buySellInd;
    @XmlSchemaType(name = "string")
    protected RpoRefRtCodType rpoRefRtCod;
    @XmlElement(required = true)
    protected BigDecimal rpoTotQty;
    @XmlElement(required = true)
    protected BigDecimal rpoTotAmnt;
    @XmlElement(required = true)
    protected BigDecimal rpoIntRt;
    protected BigDecimal rpoBps;
    @XmlElement(required = true)
    protected BigDecimal rpoIntAmt;
    @XmlElement(required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar settlDatCtrct;

    /**
     * Gets the value of the legNo property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getLegNo() {
        return legNo;
    }

    /**
     * Sets the value of the legNo property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setLegNo(BigInteger value) {
        this.legNo = value;
    }

    /**
     * Gets the value of the buySellInd property.
     * 
     * @return
     *     possible object is
     *     {@link BuySellIndType }
     *     
     */
    public BuySellIndType getBuySellInd() {
        return buySellInd;
    }

    /**
     * Sets the value of the buySellInd property.
     * 
     * @param value
     *     allowed object is
     *     {@link BuySellIndType }
     *     
     */
    public void setBuySellInd(BuySellIndType value) {
        this.buySellInd = value;
    }

    /**
     * Gets the value of the rpoRefRtCod property.
     * 
     * @return
     *     possible object is
     *     {@link RpoRefRtCodType }
     *     
     */
    public RpoRefRtCodType getRpoRefRtCod() {
        return rpoRefRtCod;
    }

    /**
     * Sets the value of the rpoRefRtCod property.
     * 
     * @param value
     *     allowed object is
     *     {@link RpoRefRtCodType }
     *     
     */
    public void setRpoRefRtCod(RpoRefRtCodType value) {
        this.rpoRefRtCod = value;
    }

    /**
     * Gets the value of the rpoTotQty property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRpoTotQty() {
        return rpoTotQty;
    }

    /**
     * Sets the value of the rpoTotQty property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRpoTotQty(BigDecimal value) {
        this.rpoTotQty = value;
    }

    /**
     * Gets the value of the rpoTotAmnt property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRpoTotAmnt() {
        return rpoTotAmnt;
    }

    /**
     * Sets the value of the rpoTotAmnt property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRpoTotAmnt(BigDecimal value) {
        this.rpoTotAmnt = value;
    }

    /**
     * Gets the value of the rpoIntRt property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRpoIntRt() {
        return rpoIntRt;
    }

    /**
     * Sets the value of the rpoIntRt property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRpoIntRt(BigDecimal value) {
        this.rpoIntRt = value;
    }

    /**
     * Gets the value of the rpoBps property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRpoBps() {
        return rpoBps;
    }

    /**
     * Sets the value of the rpoBps property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRpoBps(BigDecimal value) {
        this.rpoBps = value;
    }

    /**
     * Gets the value of the rpoIntAmt property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getRpoIntAmt() {
        return rpoIntAmt;
    }

    /**
     * Sets the value of the rpoIntAmt property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setRpoIntAmt(BigDecimal value) {
        this.rpoIntAmt = value;
    }

    /**
     * Gets the value of the settlDatCtrct property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getSettlDatCtrct() {
        return settlDatCtrct;
    }

    /**
     * Sets the value of the settlDatCtrct property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setSettlDatCtrct(XMLGregorianCalendar value) {
        this.settlDatCtrct = value;
    }

}
