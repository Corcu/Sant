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
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tc800Grp1Type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tc800Grp1Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="tc800KeyGrp1" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}tc800KeyGrp1Type"/>
 *         &lt;element name="tc800Grp2" type="{http://www.eurex.com/ec-en/support/initiatives/c7-scs-releases}tc800Grp2Type" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tc800Grp1Type", propOrder = {
    "tc800KeyGrp1",
    "tc800Grp2"
})
public class Tc800Grp1Type {

    @XmlElement(required = true)
    protected Tc800KeyGrp1Type tc800KeyGrp1;
    @XmlElement(required = true)
    protected List<Tc800Grp2Type> tc800Grp2;

    /**
     * Gets the value of the tc800KeyGrp1 property.
     * 
     * @return
     *     possible object is
     *     {@link Tc800KeyGrp1Type }
     *     
     */
    public Tc800KeyGrp1Type getTc800KeyGrp1() {
        return tc800KeyGrp1;
    }

    /**
     * Sets the value of the tc800KeyGrp1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link Tc800KeyGrp1Type }
     *     
     */
    public void setTc800KeyGrp1(Tc800KeyGrp1Type value) {
        this.tc800KeyGrp1 = value;
    }

    /**
     * Gets the value of the tc800Grp2 property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tc800Grp2 property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTc800Grp2().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Tc800Grp2Type }
     * 
     * 
     */
    public List<Tc800Grp2Type> getTc800Grp2() {
        if (tc800Grp2 == null) {
            tc800Grp2 = new ArrayList<Tc800Grp2Type>();
        }
        return this.tc800Grp2;
    }

}