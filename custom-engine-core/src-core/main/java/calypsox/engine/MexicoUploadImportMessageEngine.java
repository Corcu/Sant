package calypsox.engine;

import calypsox.camel.CamelConnectionManagement;
import calypsox.camel.route.MexicoCamelRouteBuilder;
import calypsox.tk.bo.engine.util.SantEngineUtil;
import com.calypso.engine.Engine;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventUpload;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.upload.services.IUploadMessage;
import com.calypso.tk.upload.services.MessageInfo;
import com.calypso.tk.upload.util.DataUploadMessageHandler;
import com.calypso.tk.util.DataUploaderUtil;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

public class MexicoUploadImportMessageEngine extends Engine {

    Properties properties = new Properties();
    CamelConnectionManagement camelConnectionManagement = null;
    protected String ENGINE_NAME = "MexImportMessageEngine";
    public String configName = "";
    public String uploadMode = "Local";
    public String uploadSource = "UploaderXML";
    public String uploadFormat = "UploaderXML";
    public String persistMessages = "All";
    public String ignoreWarnings = "false";
    public String tradeCusttomKeywords = "";
    public String gateway = "";
    public boolean sendResponse;

    public MexicoUploadImportMessageEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    @Override
    protected void init(EngineContext engineContext) {
        super.init(engineContext);
        properties = SantEngineUtil.getInstance().readProperties(getEngineContext());
        gateway = SantEngineUtil.getInstance().readPropertiesType(getEngineContext(),"Mexico");
        sendResponse = SantEngineUtil.getInstance().readPropertiesSendResponse(getEngineContext(),"false");
        initialise();
    }

    @Override
    public boolean process(PSEvent event) {
        boolean eventHandled = false;
        if(event instanceof PSEventUpload){
            PSEventUpload uploadEvent = (PSEventUpload)event;
            if (uploadEvent.getMessage() != null) {
                HashMap<String, Object> messageInfoAttr = new HashMap();
                messageInfoAttr.put("uploadMode", this.uploadMode);
                messageInfoAttr.put("uploadConfig-PricingEnv", this.gateway);
                messageInfoAttr.put("UploadObjectExternalRef", uploadEvent.getKey());
                messageInfoAttr.put("UploadMessageGateway", uploadEvent.getGateway());
                messageInfoAttr.put("persistMessages", this.persistMessages);
                messageInfoAttr.put("IgnoreWarnings", this.ignoreWarnings);
                Vector<BOException> boExceptions = new Vector();
                IUploadMessage uploadMessage = DataUploaderUtil.createUploadMessage(uploadEvent.getMessage(), this.uploadSource, this.uploadFormat, messageInfoAttr, boExceptions);
                uploadMessage.setTranslatedObject(uploadEvent.getDocument());
                MessageInfo messageInfo = uploadMessage.getMessageInfo();
                if (messageInfo == null) {
                    messageInfo = new MessageInfo();
                    uploadMessage.setMessageInfo(messageInfo);
                }

                GatewayUtil.checkAndAddAttributes(uploadMessage, messageInfoAttr);
                Map<String, String> messageContext = uploadEvent.getAttributes();
                if (messageContext != null && !messageContext.isEmpty()) {
                    HashMap<String, String> jmsAttrs = new HashMap();
                    jmsAttrs.putAll(messageContext);
                    messageInfo.setAttribute("JMSAttributes", jmsAttrs);
                }

                messageInfo.setAttribute("UploadFromPlatformType", "JMS");
                messageInfo.setAttribute(GatewayUtil.CUSTOM_TRADE_KEYWORDS_IN_ACK_FILE, this.tradeCusttomKeywords);
                CalypsoAcknowledgement ack = null;

                try {
                    ack = (new DataUploadMessageHandler()).uploadMessage(uploadMessage);
                } catch (Exception var12) {
                    Log.error(this.getClass().getSimpleName(), var12);
                }

                if (sendResponse && ack != null) {
                    eventHandled = this.sendAcknowledgement(ack);
                }
            }
        }

        performEventProcess(event);

        return eventHandled;
    }

    protected boolean sendAcknowledgement(CalypsoAcknowledgement ack){
        try {
            String ackString = DataUploaderUtil.marshallAcknowledgement(ack);
            if( camelConnectionManagement.getContext() !=null && !Util.isEmpty(ackString)){
                camelConnectionManagement.sendMessage(MexicoCamelRouteBuilder.SEND_ROUTE_NAME,ackString);
                return true;
            }
        }catch (Exception e){
            Log.error(this,"Error sending ACk: " + e);
        }
        return false;
    }

    private void performEventProcess(PSEvent event) {
        try {
            CalypsoIDAPIUtil.eventProcessed(this._ds.getRemoteTrade(), CalypsoIDAPIUtil.getId(event), this.getEngineName());
        } catch (RemoteException var4) {
            Log.error("UPLOADER", "UploaderImportMessageEngine.performEventProcess(): Error setting event " + CalypsoIDAPIUtil.getId(event) + " to processed.", var4);
        }
    }

    private void initialise() {


        if (properties != null) {

            //Init Camel connection
            camelConnectionManagement = new CamelConnectionManagement();
            camelConnectionManagement.initCamelContext(properties,new MexicoCamelRouteBuilder()).start();

            Vector<String> uploadBOMessageModeSource = LocalCache.getDomainValues(DSConnection.getDefault(), "UploadBOMessageModeSource");
            if (!Util.isEmpty(uploadBOMessageModeSource) && uploadBOMessageModeSource.contains(properties.getProperty("uploadSource"))) {
                this.uploadMode = properties.getProperty("uploadMode");
            } else {
                this.uploadMode = "Local";
            }

            if (!Util.isEmpty(properties.getProperty("uploadSource"))) {
                this.uploadSource = properties.getProperty("uploadSource");
            }

            if (!Util.isEmpty(properties.getProperty("uploadFormat"))) {
                this.uploadFormat = properties.getProperty("uploadFormat");
            }

            if (!Util.isEmpty(properties.getProperty("persistMessages"))) {
                this.persistMessages = properties.getProperty("persistMessages");
            }

            if (!Util.isEmpty(properties.getProperty("ignoreWarnings"))) {
                this.ignoreWarnings = properties.getProperty("ignoreWarnings");
            }

            this.tradeCusttomKeywords = properties.getProperty(GatewayUtil.CUSTOM_TRADE_KEYWORDS_IN_ACK_FILE);

        }
    }

    @Override
    public void stop() {
        if( camelConnectionManagement.getContext() !=null){
            try {
                camelConnectionManagement.stopConnection();
            } catch (Exception exc) {
                Log.error(this.getClass().getSimpleName(), "Errors while stopping camel connection", exc.getCause());
            }
        }
        super.stop();
    }
}
