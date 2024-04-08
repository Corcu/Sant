package calypsox.tk.event;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.event.PSEvent;

import calypsox.tk.bo.JMSQueueMessage;

public class PSEventPhoenixIncoming extends PSEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2131186111793695420L;
	String message;
	String correlationId;
	String reference;
	
    public PSEventPhoenixIncoming() {
        super();
    }

    public PSEventPhoenixIncoming(ExternalMessage extMessage) {
        super();
        this.message = extMessage.getText();
        
        if (extMessage instanceof JMSQueueMessage) {
			final JMSQueueMessage jmsMessage = (JMSQueueMessage) extMessage;

			this.correlationId = jmsMessage.getCorrelationId();
			this.reference = jmsMessage.getReference();
		}
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}
}
