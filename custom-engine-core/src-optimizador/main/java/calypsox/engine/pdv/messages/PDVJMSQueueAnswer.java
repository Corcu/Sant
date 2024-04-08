package calypsox.engine.pdv.messages;

import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.util.JMSQueueAnswer;

public class PDVJMSQueueAnswer extends JMSQueueAnswer {

	public PDVJMSQueueAnswer(String code) {
		super();
		setCode(code);
	}

	public PDVJMSQueueAnswer(JMSQueueMessage jmsMessage) {
		super(jmsMessage);
		setCode(jmsMessage.getText());
	}

	@Override
	public String toString() {
		return this.getCode();
	}
}
