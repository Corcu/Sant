package calypsox.tk.confirmation.model.jaxb;

import javax.xml.bind.annotation.*;

/**
 * Created by XI323159 on 18/03/2020.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SchemaSTPFXII")
public class ConfirmationToCalypsoBean {

    @XmlElement(name="Request")
    Request requestObject;

    // Getter Methods
    public Request getRequest() {
        return requestObject;
    }

    // Setter Methods
    public void setRequest(Request RequestObject) {
        this.requestObject = RequestObject;
    }
}

