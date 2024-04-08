package calypsox.tk.bo.document;

import calypsox.camel.CamelConnectionManagement;
import calypsox.tk.camel.routes.CPartenonRoutesBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.DocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.services.GatewayUtil;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

/**
 * @author acd
 */
public class GatewayPARTENONDocumentSender implements DocumentSender {

    public static final String SEND_ROUTE = "direct:partenonContractMessage";
    public static final String PROPERTIES_FILENAME = "partenon.connection.properties";

    CamelConnectionManagement connectionManagement = new CamelConnectionManagement();

    public GatewayPARTENONDocumentSender() {
        Properties properties = loadProperties();
        connectionManagement.initCamelContext(properties,new CPartenonRoutesBuilder()).start();
    }


    @Override
    public boolean send(DSConnection ds, SenderConfig config, SenderCopyConfig copyConfig, long eventId, AdviceDocument document, Vector copies, BOMessage message, Vector errors, String engineName, boolean[] saved) {
        return sendMessage(document.getDocument().toString()) && applySendAction(message);
    }

    @Override
    public boolean isOnline() {
        return connectionManagement.getContext()!=null;
    }

    /**
     * Send Message
     *
     * @param message
     * @return
     */
    private boolean sendMessage(String message) {
        if (connectionManagement.getContext() != null && !Util.isEmpty(message)) {
            try {
                connectionManagement.getContext().createProducerTemplate().sendBody(SEND_ROUTE, message);
                Log.system(GatewayPARTENONDocumentSender.class.getSimpleName(),"Partenon SENT: " + message);
                return true;
            } catch (Exception e) {
                Log.error(this, "Error sending message " + e.getCause());
            }
        }
        return false;
    }

    private boolean applySendAction(BOMessage boMessage) {
        boolean res = true;
        boMessage.setAction(Action.SEND);
        try {
            DSConnection.getDefault().getRemoteBO().save(boMessage, 0, "");
        } catch (CalypsoServiceException e) {
            res = false;
            Log.error(this,"Error saving message: "+boMessage.getLongId() +" "+ e.getCause().getMessage());
        }
        return res;
    }

    /**
     * @return
     */
    private Properties loadProperties() {
        try {
            return GatewayUtil.readPropertyFile(PROPERTIES_FILENAME);
        } catch (IOException var5) {
            Log.error(this, "Error while loading partenon.connection.properties", var5);
        }
        return null;
    }

    //TEST
    @PreDestroy
    private void closeConnection(){
        if(connectionManagement.getContext()!=null){
            try {
                connectionManagement.getContext().stop();
            } catch (Exception e) {
                Log.error(this,"Error closing connection. " + e.getCause());
            }
        }
    }
}
