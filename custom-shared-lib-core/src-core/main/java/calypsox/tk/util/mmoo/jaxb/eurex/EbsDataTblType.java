//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour ebsDataTblType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="ebsDataTblType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="streamType" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}streamTypeType" minOccurs="0"/>
 *         &lt;element name="streamService" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}streamServiceType" minOccurs="0"/>
 *         &lt;element name="streamAddr" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}streamAddrType" minOccurs="0"/>
 *         &lt;element name="streamPort" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}streamPortType" minOccurs="0"/>
 *         &lt;element name="mktDpth" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}mktDpthType" minOccurs="0"/>
 *         &lt;element name="recoveryInterval" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}recoveryIntervalType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ebsDataTblType", propOrder = {
    "streamType",
    "streamService",
    "streamAddr",
    "streamPort",
    "mktDpth",
    "recoveryInterval"
})
public class EbsDataTblType {

    protected BigInteger streamType;
    protected StreamServiceType streamService;
    protected String streamAddr;
    protected BigInteger streamPort;
    protected BigInteger mktDpth;
    protected BigInteger recoveryInterval;

    /**
     * Obtient la valeur de la propri?t? streamType.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getStreamType() {
        return streamType;
    }

    /**
     * D?finit la valeur de la propri?t? streamType.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setStreamType(BigInteger value) {
        this.streamType = value;
    }

    /**
     * Obtient la valeur de la propri?t? streamService.
     * 
     * @return
     *     possible object is
     *     {@link StreamServiceType }
     *     
     */
    public StreamServiceType getStreamService() {
        return streamService;
    }

    /**
     * D?finit la valeur de la propri?t? streamService.
     * 
     * @param value
     *     allowed object is
     *     {@link StreamServiceType }
     *     
     */
    public void setStreamService(StreamServiceType value) {
        this.streamService = value;
    }

    /**
     * Obtient la valeur de la propri?t? streamAddr.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStreamAddr() {
        return streamAddr;
    }

    /**
     * D?finit la valeur de la propri?t? streamAddr.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStreamAddr(String value) {
        this.streamAddr = value;
    }

    /**
     * Obtient la valeur de la propri?t? streamPort.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getStreamPort() {
        return streamPort;
    }

    /**
     * D?finit la valeur de la propri?t? streamPort.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setStreamPort(BigInteger value) {
        this.streamPort = value;
    }

    /**
     * Obtient la valeur de la propri?t? mktDpth.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMktDpth() {
        return mktDpth;
    }

    /**
     * D?finit la valeur de la propri?t? mktDpth.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMktDpth(BigInteger value) {
        this.mktDpth = value;
    }

    /**
     * Obtient la valeur de la propri?t? recoveryInterval.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRecoveryInterval() {
        return recoveryInterval;
    }

    /**
     * D?finit la valeur de la propri?t? recoveryInterval.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRecoveryInterval(BigInteger value) {
        this.recoveryInterval = value;
    }

}
