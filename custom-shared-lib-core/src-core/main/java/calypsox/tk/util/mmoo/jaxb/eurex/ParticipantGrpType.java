//
// Ce fichier a ?t? g?n?r? par l'impl?mentation de r?f?rence JavaTM Architecture for XML Binding (JAXB), v2.2.6
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport?e ? ce fichier sera perdue lors de la recompilation du sch?ma source.
// G?n?r? le : 2014.08.28 ? 10:38:25 AM CEST
//


package calypsox.tk.util.mmoo.jaxb.eurex;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour participantGrpType complex type.
 * 
 * <p>Le fragment de sch?ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="participantGrpType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="participant" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}participantType"/>
 *         &lt;element name="partLngName" type="{http://www.eurex.com/ec-en/technology/c7/system-documentation-c7}partLngNameType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "participantGrpType", propOrder = {
    "participant",
    "partLngName"
})
public class ParticipantGrpType {

    @XmlElement(required = true)
    protected String participant;
    @XmlElement(required = true)
    protected String partLngName;

    /**
     * Obtient la valeur de la propri?t? participant.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getParticipant() {
        return participant;
    }

    /**
     * D?finit la valeur de la propri?t? participant.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setParticipant(String value) {
        this.participant = value;
    }

    /**
     * Obtient la valeur de la propri?t? partLngName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPartLngName() {
        return partLngName;
    }

    /**
     * D?finit la valeur de la propri?t? partLngName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPartLngName(String value) {
        this.partLngName = value;
    }

}
