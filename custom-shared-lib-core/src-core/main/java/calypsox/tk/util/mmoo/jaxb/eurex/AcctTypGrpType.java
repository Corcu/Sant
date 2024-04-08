//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour acctTypGrpType.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="acctTypGrpType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="2"/>
 *     &lt;enumeration value="A1"/>
 *     &lt;enumeration value="A2"/>
 *     &lt;enumeration value="A3"/>
 *     &lt;enumeration value="A4"/>
 *     &lt;enumeration value="A5"/>
 *     &lt;enumeration value="A6"/>
 *     &lt;enumeration value="A7"/>
 *     &lt;enumeration value="A8"/>
 *     &lt;enumeration value="A9"/>
 *     &lt;enumeration value="AA"/>
 *     &lt;enumeration value="A "/>
 *     &lt;enumeration value="AL"/>
 *     &lt;enumeration value="G1"/>
 *     &lt;enumeration value="G2"/>
 *     &lt;enumeration value="M "/>
 *     &lt;enumeration value="M1"/>
 *     &lt;enumeration value="M2"/>
 *     &lt;enumeration value="P "/>
 *     &lt;enumeration value="P1"/>
 *     &lt;enumeration value="P2"/>
 *     &lt;enumeration value="PP"/>
 *     &lt;enumeration value="TT"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "acctTypGrpType")
@XmlEnum
public enum AcctTypGrpType {

    @XmlEnumValue("A1")
    A_1("A1"),
    @XmlEnumValue("A2")
    A_2("A2"),
    @XmlEnumValue("A3")
    A_3("A3"),
    @XmlEnumValue("A4")
    A_4("A4"),
    @XmlEnumValue("A5")
    A_5("A5"),
    @XmlEnumValue("A6")
    A_6("A6"),
    @XmlEnumValue("A7")
    A_7("A7"),
    @XmlEnumValue("A8")
    A_8("A8"),
    @XmlEnumValue("A9")
    A_9("A9"),
    AA("AA"),
    @XmlEnumValue("A ")
    A("A "),
    AL("AL"),
    @XmlEnumValue("G1")
    G_1("G1"),
    @XmlEnumValue("G2")
    G_2("G2"),
    @XmlEnumValue("M ")
    M("M "),
    @XmlEnumValue("M1")
    M_1("M1"),
    @XmlEnumValue("M2")
    M_2("M2"),
    @XmlEnumValue("P ")
    P("P "),
    @XmlEnumValue("P1")
    P_1("P1"),
    @XmlEnumValue("P2")
    P_2("P2"),
    PP("PP"),
    TT("TT");
    private final String value;

    AcctTypGrpType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AcctTypGrpType fromValue(String v) {
        for (AcctTypGrpType c: AcctTypGrpType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
