package calypsox.camel.publisher;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * @author aalonsop
 */
public abstract class AbstractCamelEventPublisher<T extends PSEvent> implements MessageListener {


    /**
     * @return The needed PSEvent depending on it's concrete class
     */
    abstract T createEventToPublish(String data);


    /**
     * @return true if success (eventId > 0 means that it was saved) false in other case
     */
    boolean publishEvent(String data) {
        long savedEventId = 0;
        T eventToPublish = createEventToPublish(data);
        try {
            savedEventId = DSConnection.getDefault().getRemoteTrade().saveAndPublish(eventToPublish);
        } catch (CalypsoServiceException exc) {
            Log.error(this, "Exception while trying to save and publish " + eventToPublish.getClassName(), exc.getCause());
        }
        return savedEventId > 0;
    }

    /**
     * @param msg
     */
    @Override
    public void onMessage(Message msg) {
        //Convertir el camel.Message a lo que Calypso requiera
        if (msg instanceof TextMessage) {
            final TextMessage txtMessage = (TextMessage) msg;
            try {
                this.publishEvent(txtMessage.getText());
            } catch (JMSException e) {
                e.printStackTrace();
            }
            //jmsMessage.setReference(mess.getJMSMessageID());
            //jmsMessage.setText(msg.getText());
            //jmsMessage.setCorrelationId(mess.getJMSCorrelationID());
            System.out.println();

        }
    }
}
