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
 * <p>Java class for penFailQtyTypType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="penFailQtyTypType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="4"/>
 *     &lt;enumeration value="AMOR"/>
 *     &lt;enumeration value="FAMT"/>
 *     &lt;enumeration value="UNIT"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "penFailQtyTypType")
@XmlEnum
public enum PenFailQtyTypType {

    AMOR,
    FAMT,
    UNIT;

    public String value() {
        return name();
    }

    public static PenFailQtyTypType fromValue(String v) {
        return valueOf(v);
    }

}
