package calypsox.ctm.rx;

import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.tk.upload.pricingenv.UploaderPricingEnvHolderHandler;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventUpload;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.publish.jaxb.CalypsoTrades;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.upload.services.IUploadMessage;
import com.calypso.tk.upload.services.MessageInfo;
import com.calypso.tk.upload.util.DataUploadMessageHandler;
import com.calypso.tk.util.DataUploaderUtil;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class RxDatauploaderAdapter {


    /**
     * Perform a full datauploader stream by splitting
     * msg unmarshalling and msg uploading into separate async and reactive processes.
     *
     * @param event
     * @param engineName
     * @return Always true due to Calypso's engine api requirements.
     * @see RxJava 2.2
     */
    public static boolean performReactiveUpload(PSEventUpload event, String engineName) {
        traceLog("Starting to process event with id: " +
                Optional.of(event).map(PSEvent::getLongId).orElse(0L));


        Observable.just(event)
                .map(RxDatauploaderAdapter::unmarshallMessage)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(uploadMessage ->
                                RxDatauploaderAdapter.uploadMessage(uploadMessage, engineName, event.getLongId()),
                        t -> Log.error(RxDatauploaderAdapter.class.getSimpleName(), "Unhandled error inside RX: " + t.getCause()));
        return true;
    }

    //Calculation

    /**
     * Given a PSEventUpload event, unmarshalls its xml message into a Calypso DUP object
     *
     * @param uploadEvent Incoming PSEventUploadEvent
     * @return Unmarshalled message
     */
    public static IUploadMessage unmarshallMessage(PSEventUpload uploadEvent) {
        traceLog("Starting to unmarshall event with id: " +
                Optional.ofNullable(uploadEvent).map(PSEvent::getLongId).orElse(0L));

        return Optional.ofNullable(uploadEvent)
                .map(RxDatauploaderAdapter::fillUploaderContext)
                .map(attributes -> DataUploaderUtil.createUploadMessage(uploadEvent.getMessage(),
                        uploadEvent.getMessageSource(), CTMUploaderConstants.UPLOADERXML_STR, attributes, new Vector<>()))
                .map(uploadMsg -> RxDatauploaderAdapter.enrichMessageInfo(uploadEvent.getAttributes(), uploadMsg))
                .orElse(null);
    }

    //Action

    /**
     * Performs Calypso DataUploader logic to process and upload an incoming already parsed message
     *
     * @param uploadMessage
     * @param engineName
     * @param eventId
     * @return Calypso ACK containing upload result
     */
    public static void uploadMessage(IUploadMessage uploadMessage, String engineName, long eventId) {
        traceLog("Starting to upload unmarshalled message: " +
                Optional.ofNullable(uploadMessage).map(IUploadMessage::getMessageInfo).orElse(null));

        try {
            CalypsoAcknowledgement ack = (new DataUploadMessageHandler()).uploadMessage(uploadMessage);
            Optional.ofNullable(ack)
                    .ifPresent(calypsoAcknowledgement -> processUploadResult(calypsoAcknowledgement, engineName, eventId));
        } catch (Exception exc) {
            Log.error(RxDatauploaderAdapter.class.getSimpleName(), exc);
        }
    }

    /**
     * Applies actions to log and ack the upload result
     *
     * @param calypsoAcknowledgement
     * @param engineName
     * @param eventId
     */
    private static void processUploadResult(CalypsoAcknowledgement calypsoAcknowledgement, String engineName, long eventId) {
        logUploadResult(calypsoAcknowledgement);
        markEventAsProcessed(engineName, eventId);
        RxIONAckAdapter.sendIONErrorAck(calypsoAcknowledgement);
    }

    //Action
    public static void markEventAsProcessed(String engineName, long eventId) {
        try {
            DSConnection.getDefault().getRemoteTrade().eventProcessed(eventId, engineName);
        } catch (CalypsoServiceException exc) {
            Log.error(engineName, exc.getCause());
        }
    }

    //Data
    protected static IUploadMessage enrichMessageInfo(Map<String, String> messageContext, IUploadMessage uploadMessage) {
        if (uploadMessage.getMessageInfo() == null) {
            uploadMessage.setMessageInfo(new MessageInfo());
        }
        if (messageContext != null && !messageContext.isEmpty()) {
            uploadMessage.getMessageInfo().setAttribute("JMSAttributes", messageContext);
        }
        uploadMessage.getMessageInfo().setAttribute("UploadFromPlatformType", "JMS");
        uploadMessage.getMessageInfo().setAttribute(GatewayUtil.CUSTOM_TRADE_KEYWORDS_IN_ACK_FILE, CTMUploaderConstants.TRADE_KEYWORD_ORIGINAL_EXTERNAL_REF);
        return uploadMessage;
    }

    //Data
    protected static HashMap<String, Object> fillUploaderContext(PSEventUpload uploadEvent) {
        HashMap<String, Object> messageInfoAttr = new HashMap<>();
        messageInfoAttr.put("uploadMode", "Local");
        messageInfoAttr.put("uploadConfig-PricingEnv", UploaderPricingEnvHolderHandler.getUploaderStr());
        messageInfoAttr.put("UploadMessageGateway", uploadEvent.getGateway());
        messageInfoAttr.put("persistMessages", "ALL");
        messageInfoAttr.put("IgnoreWarnings", "true");
        return messageInfoAttr;
    }

    /**
     * Non-reactive uploader approach
     *
     * @param uploadEvent
     * @param engineName
     * @return ack
     */
    public static boolean processEvent(PSEventUpload uploadEvent) {
        Optional.ofNullable(uploadEvent)
                .map(RxDatauploaderAdapter::unmarshallMessage)
                .ifPresent(uploadMessage ->
                        RxDatauploaderAdapter.uploadMessage(uploadMessage, uploadEvent.getEngineName(), uploadEvent.getLongId()));
        return true;
    }

    private static void logUploadResult(CalypsoAcknowledgement uploadResult) {
        Optional.of(uploadResult)
                .map(CalypsoAcknowledgement::getCalypsoTrades)
                .map(CalypsoTrades::getCalypsoTrade)
                .flatMap(trades -> trades.stream().findFirst())
                .ifPresent(trade -> Log.info("UPLOADER", Thread.currentThread().getName()
                        + " : Received and processed trade with extRef : "
                        + trade.getExternalRef() + " and status " + trade.getStatus()));
    }

    private static void traceLog(String msg) {
        Log.info("UPLOADER", Thread.currentThread().getName() + " -> [RxDataUploaderAdapter] " + msg);
    }

}
