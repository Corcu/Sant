package calypsox.tk.event;

import com.calypso.tk.event.PSEvent;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;

/**
 * @author aalonsop
 */
public class PSEventDataUploaderAck extends PSEvent {
    private final CalypsoAcknowledgement calypsoDupAck;
    private final String originalMessage;

    public PSEventDataUploaderAck(CalypsoAcknowledgement calypsoDupAck){
        this.calypsoDupAck=calypsoDupAck;
        originalMessage = null;
    }
    
    public PSEventDataUploaderAck(CalypsoAcknowledgement calypsoDupAck, String originalMessage){
        this.calypsoDupAck = calypsoDupAck;
        this.originalMessage = originalMessage;
    }

    public CalypsoAcknowledgement getCalypsoDupAck(){
        return this.calypsoDupAck;
    }
    
    public String getOriginalMessage(){
        return this.originalMessage;
    }
}
