package calypsox.engine.im.export;

import calypsox.tk.bo.JMSQueueMessage;

/**
 * @author aalonsop
 */
public class QEFJMSMessageWrapper {

    private JMSQueueMessage qefJmsMessage;
    private int marginCallEntryId;
    private boolean isEndOfMessage;

    public QEFJMSMessageWrapper(JMSQueueMessage qefJmsMessage, int marginCallEntryId,boolean isEndOfMessage){
        this.qefJmsMessage=qefJmsMessage;
        this.marginCallEntryId=marginCallEntryId;
        this.isEndOfMessage=isEndOfMessage;
    }

    /**
     * @return
     */
    public JMSQueueMessage getQefJmsMessage() {
        return qefJmsMessage;
    }

    /**
     * @return
     */
    public int getMarginCallEntryId() {
        return marginCallEntryId;
    }

    /**
     * @return
     */
    public boolean isEndOfMessage() {
        return isEndOfMessage;
    }
}
