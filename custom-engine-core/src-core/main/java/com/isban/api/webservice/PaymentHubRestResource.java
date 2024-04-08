package com.isban.api.webservice;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import calypsox.tk.bo.util.PaymentsHubCallback;
import calypsox.tk.event.PSEventPaymentsHubImport;
import calypsox.tk.bo.util.PaymentHubImportUtil;
import calypsox.webservices.annotations.SgtEndpoint;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.service.DSConnection;


@SgtEndpoint
@Path("/ppt/payments")
public class PaymentHubRestResource extends SantanderRestWebServices {


	public static final String DOMAIN_PH_CALLBACK_SOURCE = "PHCallBackSource";


	@POST
	@Path("{payment_id}/status")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response paymentHubImportJson(@PathParam("payment_id") String paymentId, InputStream inputJson) {
		Response response = null;
		StringBuffer inputJsonBuffer = PaymentHubImportUtil.getInstance().inputStreamToStringBuffer(inputJson);
		List<String> errors = new ArrayList<String>();
		boolean jsonOK = checkInputJson(inputJsonBuffer, errors);
		if (jsonOK) {
			boolean saveOK = saveEvent(inputJsonBuffer);
			if (saveOK) {
				response = Response.status(Response.Status.NO_CONTENT).build();
			} else {
				String jsonMessage = buildJsonResponse("Error while saving");
				response = Response.status(Response.Status.BAD_REQUEST).entity(jsonMessage).build();
			}
		} else {
			String jsonMessage = buildJsonResponse("Could not parse received JSON message", errors);
			response = Response.status(Response.Status.BAD_REQUEST).entity(jsonMessage).build();
		}
		return response;
	}


	private boolean checkInputJson(StringBuffer inputJson, List<String> errors) {
		boolean jsonOK = false;
		String jsonString = inputJson.toString();
		PaymentsHubCallback phCallback = PaymentsHubCallback.parseText(jsonString);
		jsonOK = checkCallback(phCallback, errors);
		return jsonOK;
	}


	private boolean checkCallback(PaymentsHubCallback phCallback, List<String> errors) {
		boolean callbackOK = true;
		if (phCallback == null) {
			errors.add("Received message is empty");
			callbackOK = false;
		} else {
			if (Util.isEmpty(phCallback.getIdempotentReference())) {
				errors.add("Field \"idempotentReference\" is missing");
				callbackOK = false;
			}
			if (Util.isEmpty(phCallback.getStatus())) {
				errors.add("Field \"status\" is missing");
				callbackOK = false;
			}
			if (Util.isEmpty(phCallback.getCommunicationStatus())) {
				errors.add("Field \"communicationStatus\" is missing");
				callbackOK = false;
			}
			if (Util.isEmpty(phCallback.getSource())) {
				errors.add("Field \"source\" is missing");
				callbackOK = false;
			} else {
				String source = phCallback.getSource();
				if (!checkSource(source, errors)) {
					callbackOK = false;
				}
			}
			if (phCallback.getTime() == null) {
				errors.add("Field \"timestamp\" is missing or has invalid format");
				callbackOK = false;
			}
		}
		return callbackOK;
	}


	private boolean checkSource(String source, List<String> errors) {
		boolean sourceOK = false;
		List<String> validSources = DomainValues.values(DOMAIN_PH_CALLBACK_SOURCE);
		if (validSources != null) {
			if (!Util.isEmpty(source) && validSources.contains(source)) {
				sourceOK = true;
			} else {
				StringBuilder error = new StringBuilder();
				error.append(String.format("Field \"source\" has invalid value \"%s\". ", source));
				error.append(String.format("Valid values are: %s", validSources.toString()));
				errors.add(error.toString());
			}
		}
		return sourceOK;
	}


	private boolean saveEvent(StringBuffer inputJson) {
		boolean savedOK = false;
		try {
			PSEventPaymentsHubImport event = PaymentHubImportUtil.getInstance().buildEvent(inputJson);
			long eventId = DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
			String infoMessage = String.format("Saved PSEventPaymentsHubImport %d", eventId);
			Log.info(this, infoMessage);
			savedOK = true;
		} catch (CalypsoServiceException e) {
			String errorMessage = "Could not save event";
			Log.error(this, errorMessage, e);
		}
		return savedOK;
	}


}
