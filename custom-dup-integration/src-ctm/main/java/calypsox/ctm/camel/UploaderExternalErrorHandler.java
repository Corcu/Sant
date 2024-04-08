package calypsox.ctm.camel;

import calypsox.tk.bo.UploaderMessageHandler;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.service.DSConnection;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jfree.util.Log;

import java.util.Map;
import java.util.Optional;

/**
 * @author aalonsop
 * Creates tasks from incoming external mapping errors thrown by "EMS Integration team".
 * Error data is received inside JMS Headers in a empty body message.
 */
public class UploaderExternalErrorHandler {


    /**
     * Msg's body is not checked, headers only.
     * @param exchange
     */
    public static Task createAndPublishTask(Exchange exchange){
        return Optional.ofNullable(exchange)
                .map(Exchange::getIn)
                .map(Message::getHeaders)
                .map(UploaderExternalErrorHandler::createErrorTaskFromHeader)
                .map(UploaderExternalErrorHandler::save)
                .orElse(null);
    }

    private static Task createErrorTaskFromHeader(Map<String,Object> headers){
        String errorDesc="External Uploader error: "+getStringFromObjectMap("ErrorDesc",headers);
        String productType=getStringFromObjectMap("ProductType",headers);
        String idMx=getStringFromObjectMap("IdMurex",headers);

        Task errorTask = new UploaderMessageHandler().buildTask(errorDesc,productType,"AllocationUploader");
        errorTask.setInternalReference(idMx);
        return errorTask;
    }

    private static Task save(Task task){
        try {
            long taskId=DSConnection.getDefault().getRemoteBO().save(task);
            task.setId(taskId);
        } catch (CalypsoServiceException exc) {
            Log.error("UPLOADER",exc);
        }
        return task;
    }
    private static String getStringFromObjectMap(String key, Map<String,Object> objectMap){
        return Optional.ofNullable(objectMap)
                .map(objMap -> objMap.get(key))
                .filter(objValue -> objValue instanceof String)
                .map(String.class::cast)
                .orElse("");
    }
}
