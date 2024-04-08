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
 * <p>Classe Java pour commonClearingDataType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="commonClearingDataType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="clearingTakeUpMember" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}clearingTakeUpMemberType" minOccurs="0"/>
 *         &lt;element name="ordOriginFirm" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}ordOriginFirmType" minOccurs="0"/>
 *         &lt;element name="beneficiary" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}beneficiaryType" minOccurs="0"/>
 *         &lt;element name="customerInstr" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}customerInstrType" minOccurs="0"/>
 *         &lt;element name="regulatoryInfo" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}regulatoryInfoType" minOccurs="0"/>
 *         &lt;element name="regulatoryId" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}regulatoryIdType" minOccurs="0"/>
 *         &lt;element name="originCountryCode" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}originCountryCodeType" minOccurs="0"/>
 *         &lt;element name="freeText1" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}freeText1Type" minOccurs="0"/>
 *         &lt;element name="freeText2" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}freeText2Type" minOccurs="0"/>
 *         &lt;element name="freeText3" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}freeText3Type" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "commonClearingDataType", propOrder = {
    "clearingTakeUpMember",
    "ordOriginFirm",
    "beneficiary",
    "customerInstr",
    "regulatoryInfo",
    "regulatoryId",
    "originCountryCode",
    "freeText1",
    "freeText2",
    "freeText3"
})
public class CommonClearingDataType {

    protected String clearingTakeUpMember;
    protected String ordOriginFirm;
    protected String beneficiary;
    protected String customerInstr;
    protected String regulatoryInfo;
    protected BigInteger regulatoryId;
    protected String originCountryCode;
    protected String freeText1;
    protected String freeText2;
    protected String freeText3;

    /**
     * Obtient la valeur de la propri?t? clearingTakeUpMember.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClearingTakeUpMember() {
        return clearingTakeUpMember;
    }

    /**
     * D?finit la valeur de la propri?t? clearingTakeUpMember.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClearingTakeUpMember(String value) {
        this.clearingTakeUpMember = value;
    }

    /**
     * Obtient la valeur de la propri?t? ordOriginFirm.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrdOriginFirm() {
        return ordOriginFirm;
    }

    /**
     * D?finit la valeur de la propri?t? ordOriginFirm.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrdOriginFirm(String value) {
        this.ordOriginFirm = value;
    }

    /**
     * Obtient la valeur de la propri?t? beneficiary.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBeneficiary() {
        return beneficiary;
    }

    /**
     * D?finit la valeur de la propri?t? beneficiary.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBeneficiary(String value) {
        this.beneficiary = value;
    }

    /**
     * Obtient la valeur de la propri?t? customerInstr.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCustomerInstr() {
        return customerInstr;
    }

    /**
     * D?finit la valeur de la propri?t? customerInstr.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCustomerInstr(String value) {
        this.customerInstr = value;
    }

    /**
     * Obtient la valeur de la propri?t? regulatoryInfo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRegulatoryInfo() {
        return regulatoryInfo;
    }

    /**
     * D?finit la valeur de la propri?t? regulatoryInfo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRegulatoryInfo(String value) {
        this.regulatoryInfo = value;
    }

    /**
     * Obtient la valeur de la propri?t? regulatoryId.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRegulatoryId() {
        return regulatoryId;
    }

    /**
     * D?finit la valeur de la propri?t? regulatoryId.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRegulatoryId(BigInteger value) {
        this.regulatoryId = value;
    }

    /**
     * Obtient la valeur de la propri?t? originCountryCode.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginCountryCode() {
        return originCountryCode;
    }

    /**
     * D?finit la valeur de la propri?t? originCountryCode.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginCountryCode(String value) {
        this.originCountryCode = value;
    }

    /**
     * Obtient la valeur de la propri?t? freeText1.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFreeText1() {
        return freeText1;
    }

    /**
     * D?finit la valeur de la propri?t? freeText1.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFreeText1(String value) {
        this.freeText1 = value;
    }

    /**
     * Obtient la valeur de la propri?t? freeText2.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFreeText2() {
        return freeText2;
    }

    /**
     * D?finit la valeur de la propri?t? freeText2.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFreeText2(String value) {
        this.freeText2 = value;
    }

    /**
     * Obtient la valeur de la propri?t? freeText3.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFreeText3() {
        return freeText3;
    }

    /**
     * D?finit la valeur de la propri?t? freeText3.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFreeText3(String value) {
        this.freeText3 = value;
    }

}
