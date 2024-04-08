package calypsox.camel.document;

import calypsox.camel.CamelContextBean;
import calypsox.camel.CamelContextBeanBased;
import calypsox.camel.CamelContextBeanPool;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.DocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author aalonsop
 */
public abstract class CamelBasedDocumentSender implements DocumentSender, CamelContextBeanBased {

    private CamelContextBean camelContextBean;
    private final String camelContextBeanName;


    public CamelBasedDocumentSender() {
        this.camelContextBeanName=getCamelContextBeanName();
    }

    @Override
    public boolean send(DSConnection ds, SenderConfig config, SenderCopyConfig copyConfig, long eventId, AdviceDocument document, Vector copies, BOMessage message, Vector errors, String engineName, boolean[] saved) {
        if (sendMessage(document.getDocument().toString())) {
            applySendAction(message);
        }
        //Always true, we don't want to keep the event queued on sending error
        return true;
    }

    @Override
    public boolean isOnline() {
        boolean res = false;
        refreshBeanFromPool();
        if (this.camelContextBean != null) {
            res = camelContextBean.isOnline();
        }
        if (!res) {
                Log.error(this.getClass().getSimpleName(), "DocumentSender is OFFLINE, message couldn't be sent");
        }
        return res;
    }

    private void refreshBeanFromPool() {
        this.camelContextBean = CamelContextBeanPool.getInstance().getCamelContextBean(this.camelContextBeanName);
    }

    /**
     * Send Message
     *
     * @return
     */
     boolean sendMessage(String message) {
        boolean isSent = false;
        try {
            camelContextBean.sendMsgThroughDefaultRoute(message);
            isSent = true;
        } catch (Exception exc) {
            Log.error(this.getClass().getSimpleName(), "Error sending message ", exc.getCause());
        }
        return isSent;
    }

    /**
     * @param boMessage
     */
    private void applySendAction(BOMessage boMessage) {
        boMessage.setAction(Action.SEND);
        try {
            DSConnection.getDefault().getRemoteBO().save(boMessage, 0, "");
        } catch (CalypsoServiceException exc) {
            Log.error(this.getClass().getSimpleName(), "Error while applying SENT action to msg", exc.getCause());
        }
    }
}
