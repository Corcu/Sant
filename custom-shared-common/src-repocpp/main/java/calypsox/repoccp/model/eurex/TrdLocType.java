//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.09.29 at 03:45:16 PM BST 
//


package calypsox.repoccp.model.eurex;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for trdLocType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="trdLocType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="4"/>
 *     &lt;enumeration value="ECAG"/>
 *     &lt;enumeration value="XERE"/>
 *     &lt;enumeration value="XETR"/>
 *     &lt;enumeration value="XEUR"/>
 *     &lt;enumeration value="XFRA"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "trdLocType")
@XmlEnum
public enum TrdLocType {

    ECAG,
    XERE,
    XETR,
    XEUR,
    XFRA;

    public String value() {
        return name();
    }

    public static TrdLocType fromValue(String v) {
        return valueOf(v);
    }

}