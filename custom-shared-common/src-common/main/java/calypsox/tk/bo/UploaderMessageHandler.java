package calypsox.tk.bo;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.UploadMessageContext;
import com.calypso.tk.bo.UploaderMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

public class UploaderMessageHandler extends com.calypso.tk.bo.UploaderMessageHandler {

    private static final String MESSAGE_ERRORDESC_PROP_NAME = "ErrorDesc";
    private static final String MESSAGE_MXID_PROP_NAME = "IdMurex";
    private static final String MESSAGE_PRODUCT_TYPE_PROP_NAME = "ProductType";
    public static final String TASK_ERROR_TYPE = "EX_GATEWAYMSG_ERROR";

    @Override
    public boolean handleExternalMessage(ExternalMessage externalMessage, PricingEnv env, PSEvent event,
                                         String engineName, DSConnection ds, Object dbCon) throws MessageParseException {
        UploaderMessage uploaderMessage = (UploaderMessage) externalMessage;
        UploadMessageContext messageContext = uploaderMessage.getContext();

        String errorDesc = (String) messageContext.getProperty(MESSAGE_ERRORDESC_PROP_NAME);

        if (!Util.isEmpty(errorDesc)) {
            String errorMessage = messageContext.getProperty(MESSAGE_MXID_PROP_NAME) + " - " + errorDesc;
            String productType = (String) messageContext.getProperty(MESSAGE_PRODUCT_TYPE_PROP_NAME);
            Task task = buildTask(errorMessage, productType, "Uploader");

            Log.error(this, "Error importing message in uploader : " + errorDesc);
            try {
                ds.getRemoteBO().save(task);
            } catch (CalypsoServiceException e) {
                Log.error(this, "Could not save Task : " + e);
            }
            return true;
        } else {
            return super.handleExternalMessage(externalMessage, env, event, engineName, ds, dbCon);
        }
    }

    public Task buildTask(String comment, String attribute, String source) {
        Task task = new Task();
        task.setObjectLongId(0L);
        task.setTradeLongId(0L);
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setStatus(Task.NEW);
        task.setEventType(TASK_ERROR_TYPE);
        task.setSource(source);
        task.setComment(comment);
        task.setAttribute(attribute);
        return task;
    }
}
