package calypsox.camel.publisher;

import com.calypso.tk.event.PSEventMessage;

/**
 * @aalonsop
 */
public class JMSMessagePublisher extends AbstractCamelEventPublisher<PSEventMessage> {


    /**
     * @param data
     * @return
     */
    @Override
    public PSEventMessage createEventToPublish(String data) {
        PSEventMessage testEvent = new PSEventMessage();
        testEvent.setMessageType(data);
        return testEvent;
    }
}
