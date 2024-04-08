//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.02.03 at 11:18:08 AM CET 
//


package calypsox.engine.medusa.utils.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for botransferResult complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="botransferResult">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="transferId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="transferVersion" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="transferStatus" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="GBOStatus" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="GBOStatusDescription" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "botransferResult", propOrder = {
        "transferId",
        "transferVersion",
        "transferStatus",
        "gboStatus",
        "gboStatusDescription"
})
public class BotransferResult {

    protected long transferId;
    protected Integer transferVersion;
    @XmlElement(required = true)
    protected String transferStatus;
    @XmlElement(name = "GBOStatus", required = true)
    protected String gboStatus;
    @XmlElement(name = "GBOStatusDescription", required = true)
    protected String gboStatusDescription;

    /**
     * Gets the value of the transferId property.
     */
    public long getTransferId() {
        return transferId;
    }

    /**
     * Sets the value of the transferId property.
     */
    public void setTransferId(int value) {
        this.transferId = value;
    }

    /**
     * Gets the value of the transferVersion property.
     *
     * @return possible object is
     * {@link Integer }
     */
    public Integer getTransferVersion() {
        return transferVersion;
    }

    /**
     * Sets the value of the transferVersion property.
     *
     * @param value allowed object is
     *              {@link Integer }
     */
    public void setTransferVersion(Integer value) {
        this.transferVersion = value;
    }

    /**
     * Gets the value of the transferStatus property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getTransferStatus() {
        return transferStatus;
    }

    /**
     * Sets the value of the transferStatus property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTransferStatus(String value) {
        this.transferStatus = value;
    }

    /**
     * Gets the value of the gboStatus property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getGBOStatus() {
        return gboStatus;
    }

    /**
     * Sets the value of the gboStatus property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setGBOStatus(String value) {
        this.gboStatus = value;
    }

    /**
     * Gets the value of the gboStatusDescription property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getGBOStatusDescription() {
        return gboStatusDescription;
    }

    /**
     * Sets the value of the gboStatusDescription property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setGBOStatusDescription(String value) {
        this.gboStatusDescription = value;
    }

}
