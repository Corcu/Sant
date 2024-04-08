package calypsox.tk.bo;

import com.calypso.tk.bo.ExternalMessage;

public class JMSQueueMessage implements ExternalMessage {

	protected String text = null;
	protected String reference = null;
	protected String correlationId = null;

	@Override
	public String getType() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getSender() {
		return null;
	}

	@Override
	public String getReceiver() {
		return null;
	}

	@Override
	public String getText() {
		return this.text;
	}

	public void setText(String t) {
		this.text = t;
	}

	public boolean parse(String message, String config) {
		this.text = message;
		return true;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		JMSQueueMessage newMessage = new JMSQueueMessage();
		newMessage.text = this.text;
		newMessage.reference = this.reference;
		return newMessage;
	}

	public void setReference(String r) {
		this.reference = r;
	}

	public String getReference() {
		return this.reference;
	}

	public void setCorrelationId(String cId) {
		this.correlationId = cId;
	}

	public String getCorrelationId() {
		return this.correlationId;
	}
}
