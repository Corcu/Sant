//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.09.29 at 03:45:16 PM BST 
//


package calypsox.repoccp.model.eurex;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for tc800Grp7Type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tc800Grp7Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="tc800KeyGrp7" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}tc800KeyGrp7Type"/>
 *         &lt;element name="rpoTrdTyp" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoTrdTypType"/>
 *         &lt;element name="ordrNum" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}ordrNumType"/>
 *         &lt;element name="rpoBankIntRef" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoBankIntRefType"/>
 *         &lt;element name="rpoUTI" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoUTIType"/>
 *         &lt;element name="rpoTrdTmStmp" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoTrdTmStmpType"/>
 *         &lt;element name="rpoClgTmStmp" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoClgTmStmpType"/>
 *         &lt;element name="rpoCmpTrd" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}rpoCmpTrdType"/>
 *         &lt;element name="tc800Rec" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}tc800RecType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tc800Grp7Type", propOrder = {
    "tc800KeyGrp7",
    "rpoTrdTyp",
    "ordrNum",
    "rpoBankIntRef",
    "rpoUTI",
    "rpoTrdTmStmp",
    "rpoClgTmStmp",
    "rpoCmpTrd",
    "tc800Rec"
})
public class Tc800Grp7Type {

    @XmlElement(required = true)
    protected Tc800KeyGrp7Type tc800KeyGrp7;
    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected RpoTrdTypType rpoTrdTyp;
    @XmlElement(required = true)
    protected String ordrNum;
    @XmlElement(required = true)
    protected String rpoBankIntRef;
    @XmlElement(required = true)
    protected String rpoUTI;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(EurexDateTimeAdapter.class)
    protected XMLGregorianCalendar rpoTrdTmStmp;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    @XmlJavaTypeAdapter(EurexDateTimeAdapter.class)
    protected XMLGregorianCalendar rpoClgTmStmp;
    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected RpoCmpTrdType rpoCmpTrd;
    @XmlElement(required = true)
    protected List<Tc800RecType> tc800Rec;

    /**
     * Gets the value of the tc800KeyGrp7 property.
     * 
     * @return
     *     possible object is
     *     {@link Tc800KeyGrp7Type }
     *     
     */
    public Tc800KeyGrp7Type getTc800KeyGrp7() {
        return tc800KeyGrp7;
    }

    /**
     * Sets the value of the tc800KeyGrp7 property.
     * 
     * @param value
     *     allowed object is
     *     {@link Tc800KeyGrp7Type }
     *     
     */
    public void setTc800KeyGrp7(Tc800KeyGrp7Type value) {
        this.tc800KeyGrp7 = value;
    }

    /**
     * Gets the value of the rpoTrdTyp property.
     * 
     * @return
     *     possible object is
     *     {@link RpoTrdTypType }
     *     
     */
    public RpoTrdTypType getRpoTrdTyp() {
        return rpoTrdTyp;
    }

    /**
     * Sets the value of the rpoTrdTyp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RpoTrdTypType }
     *     
     */
    public void setRpoTrdTyp(RpoTrdTypType value) {
        this.rpoTrdTyp = value;
    }

    /**
     * Gets the value of the ordrNum property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrdrNum() {
        return ordrNum;
    }

    /**
     * Sets the value of the ordrNum property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrdrNum(String value) {
        this.ordrNum = value;
    }

    /**
     * Gets the value of the rpoBankIntRef property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRpoBankIntRef() {
        return rpoBankIntRef;
    }

    /**
     * Sets the value of the rpoBankIntRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRpoBankIntRef(String value) {
        this.rpoBankIntRef = value;
    }

    /**
     * Gets the value of the rpoUTI property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRpoUTI() {
        return rpoUTI;
    }

    /**
     * Sets the value of the rpoUTI property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRpoUTI(String value) {
        this.rpoUTI = value;
    }

    /**
     * Gets the value of the rpoTrdTmStmp property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getRpoTrdTmStmp() {
        return rpoTrdTmStmp;
    }

    /**
     * Sets the value of the rpoTrdTmStmp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setRpoTrdTmStmp(XMLGregorianCalendar value) {
        this.rpoTrdTmStmp = value;
    }

    /**
     * Gets the value of the rpoClgTmStmp property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getRpoClgTmStmp() {
        return rpoClgTmStmp;
    }

    /**
     * Sets the value of the rpoClgTmStmp property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setRpoClgTmStmp(XMLGregorianCalendar value) {
        this.rpoClgTmStmp = value;
    }

    /**
     * Gets the value of the rpoCmpTrd property.
     * 
     * @return
     *     possible object is
     *     {@link RpoCmpTrdType }
     *     
     */
    public RpoCmpTrdType getRpoCmpTrd() {
        return rpoCmpTrd;
    }

    /**
     * Sets the value of the rpoCmpTrd property.
     * 
     * @param value
     *     allowed object is
     *     {@link RpoCmpTrdType }
     *     
     */
    public void setRpoCmpTrd(RpoCmpTrdType value) {
        this.rpoCmpTrd = value;
    }

    /**
     * Gets the value of the tc800Rec property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tc800Rec property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTc800Rec().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tc800RecType }
     * 
     * 
     */
    public List<Tc800RecType> getTc800Rec() {
        if (tc800Rec == null) {
            tc800Rec = new ArrayList<Tc800RecType>();
        }
        return this.tc800Rec;
    }

}
