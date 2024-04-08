package calypsox.tk.confirmation.model.jaxb;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Request")
public class Request {
    private String IdEvent;
    private String State;
    private String DateTime;
    private String System;


    // Getter Methods
    public String getIdEvent() {
        return IdEvent;
    }

    public String getState() {
        return State;
    }

    public String getDateTime() {
        return DateTime;
    }

    public String getSystem() {
        return System;
    }

    // Setter Methods

    public void setIdEvent(String IdEvent) {
        this.IdEvent = IdEvent;
    }

    public void setState(String State) {
        this.State = State;
    }

    public void setDateTime(String DateTime) {
        this.DateTime = DateTime;
    }

    public void setSystem(String System) {
        this.System = System;
    }
}
