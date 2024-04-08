package calypsox.tk.export.ack;

import java.rmi.RemoteException;
import java.util.ArrayList;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.CalypsoException;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TaskArray;

public abstract class TradeAckProcessor implements DUPAckProcessor{
	
	public static final String LOG_CATEGORY = "AckProcessor";

	public static final String UPLOAD_ADVICE_DOCID_EVENT_ATTRIBUTE = "UploadAdviceDocumentID";
	public static final String DATA_ADVICE_DOCID_EVENT_ATTRIBUTE = "DataAdviceDocumentID";
	public static final String ACK_PROCESSOR_ATTRIBUTE = "AckProcessor";
	public static final String ERRORS_ATTRIBUTE = "Error";
	public static final String ERROR_ATTRIBUTE = "OnError";
	
	protected ArrayList<String> errorMessages = new ArrayList<String>();
	protected String sourceName ="ACK";
	protected String messageType ="ACK";
	protected String gateway ="ACK";
	boolean doSaveMessage = true;
	boolean error=false;


	public ArrayList<String> getErrorMessages() {
		return errorMessages;
	}

	public void setErrorMessage(ArrayList<String> errorMessages) {
		this.errorMessages = errorMessages;
	}

	public boolean isError() {
		return error;
	}

	public abstract Trade getTrade();

	public abstract BOMessage getOriginalMessage();

	public void setError(boolean error) {
		this.error = error;
	}

	public String getSourceName() {
		return sourceName;
	}


	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}


	public String getMessageType() {
		return messageType;
	}


	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}


	public String getGateway() {
		return gateway;
	}


	public void setGateway(String gateway) {
		this.gateway = gateway;
	}

	public abstract boolean doProcess(Object message);

	public boolean updateAckMessage(BOMessage message){
		if(getTrade()!=null)
			message.setTradeLongId(getTrade().getLongId());
		if(this.getOriginalMessage()!=null)
			message.setLinkedLongId(getOriginalMessage().getLongId());

		addErrorsOnMessage(message);
		return true;
	}


	@Override
	public boolean process(ExternalMessage message) {
		this.setError(!doProcess(message));
		if(doSaveMessage) {
			return createBOMessage((ExternalMessage) message);
		}
		return isError();
	}
	
	public String getAckMessageDescription() {
		return "Ack Message";
	}
	
	public boolean addErrorsOnMessage(BOMessage message) {
		message.setAttribute(ERROR_ATTRIBUTE,""+this.isError());
		if(this.getErrorMessages().size()>0)
			message.setAttribute(ERRORS_ATTRIBUTE, getErrorMessagesAsString());
		return true;
	}

	protected boolean createBOMessage(ExternalMessage message) {
		try {
			BOMessage boMessage = GatewayUtil.createBOMessage((Object)message, (LegalEntity) null, (LegalEntity) null,
					(String) null, this.getGateway(), getMessageType());
			long allocatedSeed = CalypsoIDAPIUtil.allocateSeed(DSConnection.getDefault().getRemoteAccess(), "message", 1);
			CalypsoIDAPIUtil.setAllocatedSeed(boMessage, allocatedSeed);
			boMessage.setDescription(getAckMessageDescription());
			boMessage.setAttribute("SourceName", this.getSourceName());
			AdviceDocument aDocument = GatewayUtil.createAdviceDocument(boMessage, message.getText());
			long adviceDocId = DSConnection.getDefault().getRemoteBO().save(aDocument);
			boMessage.setAttribute(DATA_ADVICE_DOCID_EVENT_ATTRIBUTE, String.valueOf(adviceDocId));
			boMessage.setAttribute(UPLOAD_ADVICE_DOCID_EVENT_ATTRIBUTE, String.valueOf(adviceDocId));
			boMessage.setAttribute(ACK_PROCESSOR_ATTRIBUTE, this.getClass().getName());
			updateAckMessage(boMessage);
			saveBOMessage(boMessage);
			return true;
		} catch (CalypsoException arg1) {
			Log.error("EXPORTER", arg1);
			return false;
		}
		 catch (CalypsoServiceException e) {
			Log.error("EXPORTER", e);
			return false;
		 } catch (RemoteException e) {
			Log.error("EXPORTER", e);
			return false;
		}
	}
	
	protected boolean saveBOMessage(BOMessage message) {
		
		MessageArray messageArray = new MessageArray();
		messageArray.add(message);
		try {
			DSConnection.getDefault().getRemoteBO().saveMessages(0L, (String) null, messageArray, new TaskArray());
		} catch (CalypsoServiceException e) {
			this.addError(e);
			return false;
		}
		
		return true;
	}
	
	public String getErrorMessagesAsString() {
		StringBuilder sb = new StringBuilder();
		int i=0;
		for(String s : this.getErrorMessages()) {
			String trimmedString = s;
			if(trimmedString.contains(";"))
				trimmedString = trimmedString.substring(0,trimmedString.indexOf(";"));
			if(trimmedString.length()>255)
				trimmedString = trimmedString.substring(0,255);
			sb.append(trimmedString);
			i++;
			if(!(i==this.getErrorMessages().size())) {
				sb.append(", ");
			}	
		}
		return sb.toString();
	}
	
	protected void addError(Exception e) {
		this.getErrorMessages().add(e.getMessage());
	}
	
	protected void addError(String s) {
		this.getErrorMessages().add(s);
	}



}
