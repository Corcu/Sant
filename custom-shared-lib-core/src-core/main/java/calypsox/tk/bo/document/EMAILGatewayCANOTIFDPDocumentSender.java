package calypsox.tk.bo.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.DocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformAction;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformInput;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformRecipient;

import calypsox.tk.bo.CANotifDigitalPlatformHelper;

public class EMAILGatewayCANOTIFDPDocumentSender implements DocumentSender {

	public static final String EMAIL_SEPARATOR = ";";

	@Override
	public boolean isOnline() {
		return true;
	}

	@Override
	public boolean send(DSConnection ds, SenderConfig config, SenderCopyConfig copyConfig, long eventId,
			AdviceDocument document, Vector copies, BOMessage message, Vector errors, String engineName,
			boolean[] saved) {
		boolean respt = false;

		try {
			String tittle = DomainValues.comment("CANotifDigitalPlatform", "tittle");
			String application = DomainValues.comment("CANotifDigitalPlatform", "application"); 
			boolean isImportant = true;
			String actionType = DomainValues.comment("CANotifDigitalPlatform", "action.type");
			String actionTarget = DomainValues.comment("CANotifDigitalPlatform", "action.target");
			
			String content = buildContent(message, ds);
			
			Map<String, Object> action = new HashMap<String, Object>();
			action.put("type", actionType); 
			action.put("target", actionTarget);

			Map<String, Object> recipients = new HashMap<String, Object>();
			// Get the recipients object
			getRecipients(message, recipients,application, errors, ds);

			try {
				respt = CANotifDigitalPlatformHelper.getInstance().callService("NotifDigitalPlatform", tittle, content,
						application, action, recipients, isImportant);
				try {
					if(respt) {
						NotifDigitalPlatformInput in = getInput(tittle, content, application, action, recipients, isImportant);
						if(in != null) {
							document.setDocument(new StringBuffer(printJsonString(in)));
						}
						saveMessage(message, engineName, Action.S_SEND, "Updated by EMAILCADPNOTIFDocumentSender.");
					} else {
						saveMessage(message, engineName, "ERROR_SEND", "Failed to send Digital Platform Notification.");
					}
					
				} catch (Exception e) {
					Log.error(this, "Error while saving message with action " + Action.S_SEND, e);
					errors.add("Notification was sent but error saving the message with action " + Action.S_SEND);
				}
			} catch (Exception e1) {
				Log.error(this, e1);
				errors.add("EMAILGatewayCADPNOTIFDocumentSender Error " + e1.getMessage() + " "
						+ Util.exceptionToString(e1));
				try {
					saveMessage(message, engineName, "ERROR_SEND", "Updated by EMAILCADPNOTIFDocumentSender.");
				} catch (Exception e2) {
					Log.error(this, "Error while saving message with action " + "ERROR_SEND", e2);
				}
			}
		} catch (Exception e) {
			Log.error(this, "Error sending notification to Digital Platform", e);
		}

		return true;
	}

	/**
	 * Get the receiver contact and get the email address
	 * 
	 * @param message
	 * @param toAddress
	 * @param errors
	 * @param dsCon
	 */
	private void getReceiverAddress(BOMessage message, List<String> toAddress, Vector errors, DSConnection dsCon) {
		final LEContact receiverContact = BOCache.getLegalEntityContact(dsCon, message.getReceiverContactId());
		if (receiverContact == null) {
			errors.add("Sender or Receiver Contact does not exist for message with id " + message.getLongId());
			return;
		}

		manageGenericToAdresses(message, receiverContact, toAddress, errors);

	}

	/**
	 * Obtain the receiver email address list
	 * 
	 * @param message
	 * @param receiverContact
	 * @param toAddress
	 * @param errors
	 */
	private void manageGenericToAdresses(BOMessage message, LEContact receiverContact, List<String> toAddress,
			Vector errors) {
		final String email = receiverContact.getAddressCode("EMAIL");
		if (Util.isEmpty(email)) {
			errors.add("No destination address was defined for message with id " + message.getLongId());
		}
		if (!email.contains(EMAIL_SEPARATOR)) {
			toAddress.add(email);
		} else {
			toAddress.addAll(Arrays.asList(email.split(EMAIL_SEPARATOR)));
		}
	}

	/**
	 * Obtain the recipients JSON object
	 * 
	 * @param message
	 * @param recipient
	 * @param errors
	 * @param dsCon
	 */
	private void getRecipients(BOMessage message, Map<String, Object> recipient,String code, Vector errors, DSConnection dsCon) {

		LegalEntity cpty = null;
		// Obtain the list of counterparty email address
		cpty = BOCache.getLegalEntity(dsCon, message.getReceiverId());
		Map<String, String> subsidiary = new HashMap<String, String>();
		Map<String, String> app = new HashMap<String, String>();

		//recipient.put("emailAddress", String.join(";", toAddress));
		
		if (cpty != null) {
			subsidiary.put("glcs", cpty.getCode());
		} else {
			subsidiary.put("glcs", "");
		}
		recipient.put("subsidiary", subsidiary);

		app.put("code", code);
		recipient.put("application", app);

	}

	/**
	 * Saves the message allowing to jump into the next state: SENT or ERROR_SENT
	 *
	 * @param message    message to save
	 * @param engineName engine name
	 * @param action     action to apply
	 * @param comment    comment to add
	 * @throws Exception
	 */
	private void saveMessage(BOMessage message, String engineName, String action, String comment)
			throws CalypsoServiceException, CloneNotSupportedException {
		// apply the send action on the message
		BOMessage msg = (BOMessage) message.clone();
		if (isBOMessageActionApplicable(msg, Action.valueOf(action))) {
			msg.setAction(Action.valueOf(action));
			long savedId = DSConnection.getDefault().getRemoteBO().save(msg, 0, engineName, comment);
			if (savedId > 0) {
				Log.info(this, "Saved BOMessage with id=" + savedId);
			} else {
				Log.error(this, "Could not save BOMessage with id=" + msg.getLongId());
			}
		}
	}
	
	/**
	 * Buils the input object
	 * @param tittle
	 * @param content
	 * @param application
	 * @param action
	 * @param recipients
	 * @param isImportant
	 * @return
	 */
	private NotifDigitalPlatformInput getInput(String tittle, String content, String application,
			Map<String, Object> action, Map<String, Object> recipients, boolean isImportant) {
		
		NotifDigitalPlatformInput input = new NotifDigitalPlatformInput();
		NotifDigitalPlatformAction actionDP = new NotifDigitalPlatformAction();
		NotifDigitalPlatformRecipient recipientsDP = new NotifDigitalPlatformRecipient();
		List<NotifDigitalPlatformRecipient> array = new ArrayList<>();
		actionDP.load(action);
		recipientsDP.load(recipients);
		input.setTittle(tittle);
		input.setApplication(application);
		input.setImportant(isImportant);
		input.setContent(content);
		input.setAction(actionDP);
		array.add(recipientsDP);
		input.setRecipients(array);
		
		return input;
	}

	/**
	 * Checks if the BO message action is applicable.
	 *
	 * @return true if sucess
	 */
	protected boolean isBOMessageActionApplicable(final BOMessage message, final Action action) {
		return BOMessageWorkflow.isMessageActionApplicable(message, null, null, action, DSConnection.getDefault(),
				null);
	}
	
	private String printJsonString(ApiRestModel model) {
		String out = null;

		ObjectMapper mapper = null;

		String debug = null;

		try {
			mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);

			if (model != null)
				out = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model);
		} catch (JsonProcessingException e) {
			debug = (new StringBuilder()).append("Error printing json ").append("with exception ").append(e.toString())
					.append(" - ").append(e.getMessage()).append(" ").toString();

			Log.error(this, debug);
		}

		return out;
	}
	
	/**
	 * Build content field
	 * @param message
	 * @param ds
	 * @return
	 * @throws CalypsoServiceException
	 */
	private String buildContent(BOMessage message, DSConnection ds) throws CalypsoServiceException {
		StringBuilder content = new StringBuilder();
		String fixContent = DomainValues.comment("CANotifDigitalPlatform", "content");
		if(fixContent != null) {
			content.append(fixContent);
			content.append(" ");
		}
		long xferId = message.getTransferLongId();
		BOTransfer xfer = ds.getRemoteBackOffice().getBOTransfer(xferId);
		if(xfer!=null) {
			content.append("Claim ID: ");
			content.append(xfer.getTradeLongId());
			content.append(". ");
			content.append("Payment date: ");
			content.append(xfer.getValueDate());
			content.append(". ");
			content.append("Amount: ");
			content.append(xfer.getSettlementAmount()).append(" ").append(xfer.getSettlementCurrency());
		}
		return content.toString();
		
	}


}
