//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.02.03 at 11:18:08 AM CET
//

package calypsox.engine.medusa.utils.xml;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * <p>
 * Java class for botransfer complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="botransfer">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="transferId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="eventType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="transferStatus" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="tradeId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="murexId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="productType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="transferType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="entity" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="settleAmount" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="settleCurrency" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="valueDate" type="{http://www.w3.org/2001/XMLSchema}date"/>
 *         &lt;element name="settleDate" type="{http://www.w3.org/2001/XMLSchema}date"/>
 *         &lt;element name="payerCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="payerRole" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="payerInst" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receiverCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receiverRole" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="receiverInst" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="poSettleMethod" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="nettedTransfer" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="counterparty" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="counterpartyDescription" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="originalCpty" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="tag20" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="internalExternal" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="iban" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="timestamp" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="transferVersion" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="product_desc" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="trader" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="parent_id" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
// Cash management T99A Counterparty Description
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "botransfer", propOrder = {"transferId", "eventType",
        "transferStatus", "tradeId", "murexId", "productType", "transferType",
        "entity", "settleAmount", "settleCurrency", "valueDate", "settleDate",
        "payerCode", "payerRole", "payerInst", "receiverCode", "receiverRole",
        "receiverInst", "poSettleMethod", "nettedTransfer", "counterparty",
        "counterpartyDescription", "originalCpty", "tag20", "internalExternal",
        "iban", "timestamp", "transferVersion", "product_desc", "trader",
        "parent_id","book","businessReason"})
// Cash management T99A Counterparty Description - End
/**
 * BoTransfer class
 *
 * @author xIS15793
 *
 */
public class Botransfer {

    protected long transferId;
    @XmlElement(required = true)
    protected String eventType;
    @XmlElement(required = true)
    protected String transferStatus;
    protected long tradeId;
    @XmlElement(required = true)
    protected String murexId;
    @XmlElement(required = true)
    protected String productType;
    @XmlElement(required = true)
    protected String transferType;
    @XmlElement(required = true)
    protected String entity;
    @XmlElement(required = true, type = String.class)
    @XmlJavaTypeAdapter(BotransferAdapter.class)
    @XmlSchemaType(name = "double")
    protected Double settleAmount;
    @XmlElement(required = true)
    protected String settleCurrency;
    @XmlElement(required = true)
    @XmlSchemaType(name = "date")
    protected String valueDate;
    @XmlElement(required = true)
    @XmlSchemaType(name = "date")
    protected String settleDate;
    @XmlElement(required = true)
    protected String payerCode;
    @XmlElement(required = true)
    protected String payerRole;
    @XmlElement(required = true)
    protected String payerInst;
    @XmlElement(required = true)
    protected String receiverCode;
    @XmlElement(required = true)
    protected String receiverRole;
    @XmlElement(required = true)
    protected String receiverInst;
    @XmlElement(required = true)
    protected String poSettleMethod;
    protected long nettedTransfer;
    @XmlElement(required = true)
    protected String counterparty;
    // Cash management T99A Counterparty Description
    protected String counterpartyDescription;
    // Cash management T99A Counterparty Description - End
    @XmlElement(required = true)
    protected String originalCpty;
    @XmlElement(required = true)
    protected String tag20;
    @XmlElement(required = true)
    protected String internalExternal;
    @XmlElement(required = true)
    protected String iban;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected String timestamp;
    protected int transferVersion;
    protected String product_desc;
    protected String trader;
    protected long parent_id;
    protected String book;
    @XmlElement(required = true)
    protected String businessReason;

    /**
     * Gets the value of the transferId property.
     *
     * @return transfer Id
     */
    public long getTransferId() {
        return transferId;
    }

    /**
     * Sets the value of the transferId property.
     *
     * @param value value
     */
    public void setTransferId(final long value) {
        transferId = value;
    }

    /**
     * Gets the value of the eventType property.
     *
     * @return possible object is {@link String }
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Sets the value of the eventType property.
     *
     * @param value allowed object is {@link String }
     */
    public void setEventType(final String value) {
        eventType = value;
    }

    /**
     * Gets the value of the transferStatus property.
     *
     * @return possible object is {@link String }
     */
    public String getTransferStatus() {
        return transferStatus;
    }

    /**
     * Sets the value of the transferStatus property.
     *
     * @param value allowed object is {@link String }
     */
    public void setTransferStatus(final String value) {
        transferStatus = value;
    }

    /**
     * Gets the value of the tradeId property.
     *
     * @return tarde id
     */
    public long getTradeId() {
        return tradeId;
    }

    /**
     * Sets the value of the tradeId property.
     *
     * @param value value
     */
    public void setTradeId(final long value) {
        tradeId = value;
    }

    /**
     * Gets the value of the murexId property.
     *
     * @return possible object is {@link String }
     */
    public String getMurexId() {
        return murexId;
    }

    /**
     * Sets the value of the murexId property.
     *
     * @param value allowed object is {@link String }
     */
    public void setMurexId(final String value) {
        murexId = value;
    }

    /**
     * Gets the value of the productType property.
     *
     * @return possible object is {@link String }
     */
    public String getProductType() {
        return productType;
    }

    /**
     * Sets the value of the productType property.
     *
     * @param value allowed object is {@link String }
     */
    public void setProductType(final String value) {
        productType = value;
    }

    /**
     * Gets the value of the transferType property.
     *
     * @return possible object is {@link String }
     */
    public String getTransferType() {
        return transferType;
    }

    /**
     * Sets the value of the transferType property.
     *
     * @param value allowed object is {@link String }
     */
    public void setTransferType(final String value) {
        transferType = value;
    }

    /**
     * Gets the value of the entity property.
     *
     * @return possible object is {@link String }
     */
    public String getEntity() {
        return entity;
    }

    /**
     * Sets the value of the entity property.
     *
     * @param value allowed object is {@link String }
     */
    public void setEntity(final String value) {
        entity = value;
    }

    /**
     * Gets the value of the settleAmount property.
     *
     * @return possible object is {@link String }
     */
    public Double getSettleAmount() {
        return settleAmount;
    }

    /**
     * Sets the value of the settleAmount property.
     *
     * @param value allowed object is {@link String }
     */
    public void setSettleAmount(final Double value) {
        settleAmount = value;
    }

    /**
     * Gets the value of the settleCurrency property.
     *
     * @return possible object is {@link String }
     */
    public String getSettleCurrency() {
        return settleCurrency;
    }

    /**
     * Sets the value of the settleCurrency property.
     *
     * @param value allowed object is {@link String }
     */
    public void setSettleCurrency(final String value) {
        settleCurrency = value;
    }

    /**
     * Gets the value of the valueDate property.
     *
     * @return possible object is {@link XMLGregorianCalendar }
     */
    public String getValueDate() {
        return valueDate;
    }

    /**
     * Sets the value of the valueDate property.
     *
     * @param value allowed object is {@link XMLGregorianCalendar }
     */
    public void setValueDate(final String value) {
        valueDate = value;
    }

    /**
     * Gets the value of the settleDate property.
     *
     * @return possible object is {@link XMLGregorianCalendar }
     */
    public String getSettleDate() {
        return settleDate;
    }

    /**
     * Sets the value of the settleDate property.
     *
     * @param value allowed object is {@link XMLGregorianCalendar }
     */
    public void setSettleDate(final String value) {
        settleDate = value;
    }

    /**
     * Gets the value of the payerCode property.
     *
     * @return possible object is {@link String }
     */
    public String getPayerCode() {
        return payerCode;
    }

    /**
     * Sets the value of the payerCode property.
     *
     * @param value allowed object is {@link String }
     */
    public void setPayerCode(final String value) {
        payerCode = value;
    }

    /**
     * Gets the value of the payerRole property.
     *
     * @return possible object is {@link String }
     */
    public String getPayerRole() {
        return payerRole;
    }

    /**
     * Sets the value of the payerRole property.
     *
     * @param value allowed object is {@link String }
     */
    public void setPayerRole(final String value) {
        payerRole = value;
    }

    /**
     * Gets the value of the payerInst property.
     *
     * @return possible object is {@link String }
     */
    public String getPayerInst() {
        return payerInst;
    }

    /**
     * Sets the value of the payerInst property.
     *
     * @param value allowed object is {@link String }
     */
    public void setPayerInst(final String value) {
        payerInst = value;
    }

    /**
     * Gets the value of the receiverCode property.
     *
     * @return possible object is {@link String }
     */
    public String getReceiverCode() {
        return receiverCode;
    }

    /**
     * Sets the value of the receiverCode property.
     *
     * @param value allowed object is {@link String }
     */
    public void setReceiverCode(final String value) {
        receiverCode = value;
    }

    /**
     * Gets the value of the receiverRole property.
     *
     * @return possible object is {@link String }
     */
    public String getReceiverRole() {
        return receiverRole;
    }

    /**
     * Sets the value of the receiverRole property.
     *
     * @param value allowed object is {@link String }
     */
    public void setReceiverRole(final String value) {
        receiverRole = value;
    }

    /**
     * Gets the value of the receiverInst property.
     *
     * @return possible object is {@link String }
     */
    public String getReceiverInst() {
        return receiverInst;
    }

    /**
     * Sets the value of the receiverInst property.
     *
     * @param value allowed object is {@link String }
     */
    public void setReceiverInst(final String value) {
        receiverInst = value;
    }

    /**
     * Gets the value of the poSettleMethod property.
     *
     * @return possible object is {@link String }
     */
    public String getPoSettleMethod() {
        return poSettleMethod;
    }

    /**
     * Sets the value of the poSettleMethod property.
     *
     * @param value allowed object is {@link String }
     */
    public void setPoSettleMethod(final String value) {
        poSettleMethod = value;
    }

    /**
     * Gets the value of the nettedTransfer property.
     *
     * @return netted transfer
     */
    public long getNettedTransfer() {
        return nettedTransfer;
    }

    /**
     * Sets the value of the nettedTransfer property.
     *
     * @param value value
     */
    public void setNettedTransfer(final long value) {
        nettedTransfer = value;
    }

    /**
     * Gets the value of the counterparty property.
     *
     * @return possible object is {@link String }
     */
    public String getCounterparty() {
        return counterparty;
    }

    /**
     * Sets the value of the counterparty property.
     *
     * @param value allowed object is {@link String }
     */
    public void setCounterparty(final String value) {
        counterparty = value;
    }

    // Cash management T99A Counterparty Description
    public String getCounterpartyDescription() {
        return counterpartyDescription;
    }

    public void setCounterpartyDescription(final String value) {
        counterpartyDescription = value;
    }
    // Cash management T99A Counterparty Description - End

    /**
     * Gets the value of the originalCpty property.
     *
     * @return possible object is {@link String }
     */
    public String getOriginalCpty() {
        return originalCpty;
    }

    /**
     * Sets the value of the originalCpty property.
     *
     * @param value allowed object is {@link String }
     */
    public void setOriginalCpty(final String value) {
        originalCpty = value;
    }

    /**
     * Gets the value of the tag20 property.
     *
     * @return possible object is {@link String }
     */
    public String getTag20() {
        return tag20;
    }

    /**
     * Sets the value of the tag20 property.
     *
     * @param value allowed object is {@link String }
     */
    public void setTag20(final String value) {
        tag20 = value;
    }

    /**
     * Gets the value of the internalExternal property.
     *
     * @return possible object is {@link String }
     */
    public String getInternalExternal() {
        return internalExternal;
    }

    /**
     * Sets the value of the internalExternal property.
     *
     * @param value allowed object is {@link String }
     */
    public void setInternalExternal(final String value) {
        internalExternal = value;
    }

    /**
     * Gets the value of the iban property.
     *
     * @return possible object is {@link String }
     */
    public String getIban() {
        return iban;
    }

    /**
     * Sets the value of the iban property.
     *
     * @param value allowed object is {@link String }
     */
    public void setIban(final String value) {
        iban = value;
    }

    /**
     * Gets the value of the timestamp property.
     *
     * @return possible object is {@link XMLGregorianCalendar }
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     *
     * @param value allowed object is {@link XMLGregorianCalendar }
     */
    public void setTimestamp(final String value) {
        timestamp = value;
    }

    /**
     * Gets the value of the transferVersion property.
     *
     * @return transferVersion
     */
    public int getTransferVersion() {
        return transferVersion;
    }

    /**
     * Sets the value of the transferVersion property.
     *
     * @param value value
     */
    public void setTransferVersion(final int value) {
        transferVersion = value;
    }

    /**
     * Gets the value of the product_desc property.
     *
     * @return product_desc
     */
    public String getProduct_desc() {
        return product_desc;
    }

    /**
     * Sets the value of the product_desc property.
     *
     * @param value value
     */
    public void setProduct_desc(final String product_desc) {
        this.product_desc = product_desc;
    }

    /**
     * Gets the value of the trader property.
     *
     * @return trader
     */
    public String getTrader() {
        return trader;
    }

    /**
     * Sets the value of the trader property.
     *
     * @param value value
     */
    public void setTrader(final String trader) {
        this.trader = trader;
    }

    /**
     * Gets the value of the parent_id property.
     *
     * @return parent_id
     */
    public long getParent_id() {
        return parent_id;
    }

    /**
     * Sets the value of the parent_id property.
     *
     * @param value value
     */
    public void setParent_id(final long parent_id) {
        this.parent_id = parent_id;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }
    
    public String getBusinessReason() {
        return businessReason;
    }
    
    public void setBusinessReason(String businessReason) {
        this.businessReason = businessReason;
    }
    
}