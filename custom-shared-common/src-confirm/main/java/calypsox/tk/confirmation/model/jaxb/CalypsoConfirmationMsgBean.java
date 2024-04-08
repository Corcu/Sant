package calypsox.tk.confirmation.model.jaxb;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.*;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author aalonsop
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "product-instance")
public class CalypsoConfirmationMsgBean {

    @XmlTransient
    private BOMessage boMessage;

    @XmlElement(name = "variable")
    private List<CalypsoConfirmationXmlField> elements = new LinkedList<>();


    public CalypsoConfirmationMsgBean() {
    }

    //Mirar la clase MessageFormatter de Java
    public CalypsoConfirmationMsgBean(BOMessage boMessage) {
        this.boMessage = boMessage;
    }

    public List<CalypsoConfirmationXmlField> getElements() {
        return elements;
    }

    public void setElements(List<CalypsoConfirmationXmlField> elements) {
        this.elements = elements;
    }

    public void addElement(AbstractMap.SimpleEntry<String, String> fieldValues) {
        CalypsoConfirmationXmlField field = new CalypsoConfirmationXmlField(fieldValues.getKey(), fieldValues.getValue());
        this.elements.add(field);
    }

    public String getField(String name){
      String fieldValue="";
      for(CalypsoConfirmationXmlField element:elements){
          if(element.getName().equals(name)){
              fieldValue=element.getValue();
              break;
          }
      }
      return fieldValue;
    }

    /**
     * @return
     */
    @Override
    public String toString() {
        JAXBContext jaxbContext;
        StringWriter sw = new StringWriter();

        try {
            jaxbContext = JAXBContext.newInstance(CalypsoConfirmationMsgBean.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.marshal(this, sw);
        } catch (JAXBException exc) {
            Log.error(this, "Couldn't marshaLl CalypsoConfirmation msg with id:" + boMessage.getLongId(), exc.getCause());
        }
        return sw.toString();
    }
}
