package calypsox.tk.export.ack;

import calypsox.tk.event.PSEventDataUploaderAck;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.publish.jaxb.CalypsoAcknowledgement;
import com.calypso.tk.publish.jaxb.Error;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.DataExportServiceUtil;
import com.calypso.tk.util.DataUploaderUtil;
import com.calypso.tk.util.MessageArray;

import java.util.*;

/**
 * @author aalonsop
 */
public abstract class CalypsoAcknowledgementProcessor<T> implements DUPAckProcessor{

    protected String messageType="DATAEXPORTERMSG";
    protected String engineName="DataExportServiceEngine";

    @Override
    public boolean process(ExternalMessage message) {
        CalypsoAcknowledgement ack = unmarshallCalypsoAcknowledgement(message);
        createAndPublishEvent(ack, message.getText());
        return true;
    }

    public void processAckEvent(CalypsoAcknowledgement calypsoAcknowledgement){
        List<T> objectList=getAckObjectList(calypsoAcknowledgement);
        if(!Util.isEmpty(objectList)&& objectList.get(0)!=null){
            T ackObject=objectList.get(0);
            String objectId=getSourceObjectId(ackObject);
            String objectVersion= Optional.ofNullable(getSourceObject(objectId))
                    .map(VersionedObject::getVersion).map(String::valueOf).orElse("");
            BOMessage mess= getSourceMsg(objectId,objectVersion);
            if(mess!=null) {
                setMsgAction(mess,getAckObjectStatus(ackObject), getErrorsFromObject(ackObject));
                saveMessage(mess);
            }
        }
    }

    protected abstract VersionedObject getSourceObject(String objectId);

    protected abstract String getSourceObjectId(T ackObject);

    protected abstract List<T> getAckObjectList(CalypsoAcknowledgement calypsoAcknowledgement);

    protected abstract String getAckObjectStatus(T ackObject);

    protected abstract List<Error> getErrorsFromObject(T ackObject);


    protected CalypsoAcknowledgement unmarshallCalypsoAcknowledgement(ExternalMessage message){
        String ackText=Optional.ofNullable(message).map(ExternalMessage::getText).orElse("");
        CalypsoAcknowledgement ackObj=DataUploaderUtil.unmarshallAcknowledgement(ackText);
        Log.info("UPLOADER", ackText);
        return ackObj;
    }


    protected void createAndPublishEvent(CalypsoAcknowledgement calypsoAcknowledgement, String message){
        PSEventDataUploaderAck event = new PSEventDataUploaderAck(calypsoAcknowledgement, message);
        publishEvent(event);
    }

    protected void publishEvent(PSEventDataUploaderAck event) {
        try {
            DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
        } catch (CalypsoServiceException exc) {
            Log.error(this.getClass().getSimpleName(), "Couldn't publish event for "+ this.engineName+" msg ack");
        }
    }

    protected BOMessage getSourceMsgWithRetry(Map<String, String> attributemap, List<String> status){
        BOMessage message = null;
        int counter=0;
        do {
            MessageArray messages = DataExportServiceUtil.getAllMessages(0, this.messageType, attributemap, status);
            if (messages != null && !messages.isEmpty()) {
                message = messages.get(0);
            }
            counter++;
        }while(counter<3 && !isValidMsg(message));

        return message;
    }


    protected boolean isValidMsg(BOMessage boMessage){
        return boMessage!=null&&
                BOMessageWorkflow.isMessageActionApplicable(boMessage, null, null, boMessage.getAction(), DSConnection.getDefault(), null);
    }


    protected BOMessage getSourceMsg(String objectId,String objectVersion){
        Map<String, String> attributemap = new HashMap<>();
        attributemap.put("ObjectId", objectId);
        attributemap.put("SourceName",  this.engineName);
        if(!Util.isEmpty(objectVersion)) {
            attributemap.put("ObjectVersion", objectVersion);
        }

        List<String> status = new ArrayList<>();
        status.add(Status.SENT);

        return getSourceMsgWithRetry(attributemap,status);
    }

    protected void saveMessage(BOMessage mess){
        if(BOMessageWorkflow.isMessageActionApplicable(mess, null, null, mess.getAction(), DSConnection.getDefault(), null)){
            try {
                DSConnection.getDefault().getRemoteBO().save(mess, 0, this.engineName);
            } catch (CalypsoServiceException exc) {
                Log.error( this.engineName,exc.getMessage());
            }
        }
    }

    protected void addErrorMsgOnNack(BOMessage mess, List<Error> errors){
        String errorMsg=Optional.ofNullable(errors).map(errs -> errs.get(0))
                .map(Error::getMessage).orElse("Error description not available");
        mess.setAttribute("NackReason",errorMsg);
    }

    protected void setMsgAction(BOMessage mess,String ackStatus, List<Error> errors){
        if("Success".equalsIgnoreCase(ackStatus)||"ACK".equalsIgnoreCase(ackStatus)){
            mess.setAction(Action.ACK);
        }else{
            mess.setAction(Action.NACK);
            addErrorMsgOnNack(mess,errors);
        }
    }
}
