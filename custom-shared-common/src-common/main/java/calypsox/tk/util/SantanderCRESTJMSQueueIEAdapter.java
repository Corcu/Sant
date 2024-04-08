package calypsox.tk.util;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;

import javax.jms.JMSException;
import javax.jms.TextMessage;

public class SantanderCRESTJMSQueueIEAdapter extends JMSQueueIEAdapter{



    /**
     * Set whether this is a sender or receiver to a queue.
     *
     * @param opMode
     */
    public SantanderCRESTJMSQueueIEAdapter(int opMode) {
        super(opMode);
    }

    public boolean write(final String message, BOMessage boMessage) {
        try {
            final TextMessage msg = this._session.createTextMessage();
            msg.setText(message);
            Log.debug("QueueAdapter", "Sending msg Text:" + message);
            this._sender.send(msg);
            boMessage.setAttribute("JMSMessageID", msg.getJMSMessageID());
        } catch (final Exception e) {
            Log.error("QueueAdapter", "Unable to send Msg", e);
            onException(new JMSException("Deconnected from Message Adapter"));
            return false;
        } // end of try-catch
        return true;
    }

}
