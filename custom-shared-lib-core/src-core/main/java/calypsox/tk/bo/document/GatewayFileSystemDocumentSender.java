package calypsox.tk.bo.document;

import calypsox.util.FileUtility;
import com.calypso.helper.CoreAPI;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.DocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.infosec.io.ResourceFactory;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Vector;

/**
 * gateway to get content from Advice Document en save as a file in Calypso FileSystem
 */
public class GatewayFileSystemDocumentSender implements DocumentSender {

    public static final String GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER = "GatewayFileSystemDocumentSender";

    public static final String ROOT  = "//calypso_interfaces//";
    private static final String SEPARATOR = "//";
    private static final String MESS_ATTRIB_PERIOD = "Period";
    private static final String FILENAME = "FILENAME";
    private static final String FILEWATCHER = "FILEWATCHER";

    protected boolean exists(String file) {
        return ResourceFactory.get().newFile(file).exists();
    }

    @Override
    public boolean send(DSConnection ds, SenderConfig config, SenderCopyConfig copyConfig, long eventId, AdviceDocument document, Vector copies, BOMessage message, Vector errors, String engineName, boolean[] saved) {

        Log.system(GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER, "### event id: " + eventId);
        boolean success = processMessage(eventId, document, message);
        Log.system(GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER, "### success =  " + success);
        String action = success ? Action.S_SEND : "FAIL_SEND";
        try {
            saveMessage(eventId, message, engineName, action, "Updated by GatewayFileSystemDocumentSender");
        } catch (Exception e1) {
            Log.error(this, "Error while saving message with action " + action, e1);
            if (success) {
                errors.add("File was generated but error saving the message with action " + action);
            }   else {
                errors.add("File was NOT generated due to problems processing the message. ");
            }
        }
        return success;
    }

    private boolean processMessage(long eventId, AdviceDocument document, BOMessage message) {
        boolean success = false;
        try {

            Properties fileProp = getPropertiesFromDV(message);
            String path = fileProp.getProperty(FILENAME);

            int lastIndexOf = path.lastIndexOf(SEPARATOR);
            String fileName =  path.substring(lastIndexOf+SEPARATOR.length());
            String folderName  = ROOT + path.substring(0, lastIndexOf)  + SEPARATOR;
            String period = message.getAttribute(MESS_ATTRIB_PERIOD);
            if (Util.isEmpty(path) || Util.isEmpty(fileName) ) {
                Log.error(GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER, "### File path is invalid Check Domain Values.");
            } else if (Util.isEmpty(period)) {
                Log.error(GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER, "### Period not described on message. Message id : " + message.getLongId());
            } else {
                String fullFileName = folderName + fileName + period;
                File fileOut = new File(fullFileName);
                OutputStreamWriter fWriter = new OutputStreamWriter(new FileOutputStream(fileOut), StandardCharsets.UTF_8);
                fWriter.write(document.getDocument().toString());
                fWriter.flush();
                fWriter.close();
                Log.system(GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER, "###  File generated successfully.");
                if (!Util.isEmpty(fileProp.getProperty(FILEWATCHER))) {
                    String filewatcherName  = folderName + SEPARATOR + fileProp.getProperty(FILEWATCHER);
                    fWriter = new OutputStreamWriter(new FileOutputStream(filewatcherName), StandardCharsets.UTF_8);
                    fWriter.write("### eventId:" + eventId + " messageId: " + message.getLongId() + " "  + new JDatetime());
                    fWriter.flush();
                    fWriter.close();
                    Log.system(GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER, "###  File for FileWatcher generated successfully.");
                }
                success = true;
            }
        } catch (Exception e) {
            Log.error(GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER, e);
        }
        return success;
    }

    private Properties getPropertiesFromDV(BOMessage message) throws IOException {
        String dvParams = LocalCache.getDomainValueComment(DSConnection.getDefault(), "GatewayFileSystemDocumentSender", message.getMessageType());
        Properties properties = new Properties();
        InputStream is = new ByteArrayInputStream(dvParams.replaceAll(";", "\n").getBytes());
        properties.load(is);
        return properties;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

     /*Saves the message allowing to jump into the next state: SENT or
     * SENT_FAILED
     *
     * @param message
     * @param engineName
     * @param action
     * @param comment
     * @throws Exception
     */
    private void saveMessage(long eventId, BOMessage message, String engineName, String action, String comment) throws CalypsoServiceException, CloneNotSupportedException {
        // apply the send action on the message
        BOMessage msg = (BOMessage) message.clone();
        if (isBOMessageActionApplicable(msg, Action.valueOf(action))) {
            msg.setAction(Action.valueOf(action));
            long savedId = DSConnection.getDefault().getRemoteBO().save(msg, -1, engineName);
            if (savedId > 0) {
                Log.system(GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER, "### Saved BOMessage with id=" + savedId);
            } else {
                Log.error(GATEWAY_FILE_SYSTEM_DOCUMENT_SENDER, "### Could not save BOMessage with id=" + msg.getLongId());
            }
        }
    }

    /**
     * Checks if the BO message action is applicable.
     *
     * @return true if sucess, false otherwise
     */
    protected boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
        return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, DSConnection.getDefault(), null);
    }

    public static void main(String[] args) throws IOException {

        String dvParams = "FILENAME=//balanzadepagos//host//balanza_host_;FILEWATCHER=FWCALYPBDPAGOS";
        Properties properties = new Properties();
        InputStream is = new ByteArrayInputStream(dvParams.replaceAll(";", "\n").getBytes());
        properties.load(is);

        String path = properties.getProperty(FILENAME);
        String fileWhacher =  properties.getProperty(FILEWATCHER);

    }
}
