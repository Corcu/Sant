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
 * <p>Java class for rpoTrdStatType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="rpoTrdStatType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="4"/>
 *     &lt;enumeration value="BIBL"/>
 *     &lt;enumeration value="IBL"/>
 *     &lt;enumeration value="BLCK"/>
 *     &lt;enumeration value="LATE"/>
 *     &lt;enumeration value="PART"/>
 *     &lt;enumeration value="PEND"/>
 *     &lt;enumeration value="STLD"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "rpoTrdStatType")
@XmlEnum
public enum RpoTrdStatType {

    BIBL,
    IBL,
    BLCK,
    LATE,
    PART,
    PEND,
    STLD;

    public String value() {
        return name();
    }

    public static RpoTrdStatType fromValue(String v) {
        return valueOf(v);
    }

}
