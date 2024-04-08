/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.engine;

import static calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.NEW_LINE;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.TradeArray;

import calypsox.tk.bo.JMSQueueMessage;
import calypsox.tk.event.PSEventPhoenixIncoming;
import calypsox.tk.util.SantanderIEAdapter;
import calypsox.tools.santfilesaver.SantFileSaver;
import calypsox.tools.santfilesaver.SantFileSaverException;
import calypsox.util.TradeInterfaceUtils;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.DFA_INPUT_SIMULATED_FIELDS;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.DFA_OUTPUT_FIELDS;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.RESPONSES;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationException;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationInputBean;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationLogic;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationMessageHandler;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationOutputBean;

/**
 * This class implements the Trade Collateralization service. On an input Deal/trade, it will generate a fake deal,
 * retrieve the contract for that trade (if there any) and check if the trade matches the contract. Based on the
 * different results, it generates an output based on the results. This online service has been requested to satisfy the
 * Dodd-Frank collateralization trades.
 * 
 */
public class TradeCollateralizationServiceEngine extends BaseIEEngine {
	public final static String SEPARATOR = "|";

	@Override
	public boolean process(PSEvent event) {
		if (event instanceof PSEventPhoenixIncoming) {
			return this.handlePSEventPhoenixIncoming(event);
		} else {
			return super.process(event);
		}
	}
	
	private boolean handlePSEventPhoenixIncoming(PSEvent event) {
		PSEventPhoenixIncoming phoenixEvent = (PSEventPhoenixIncoming)event;

		boolean eventHandled = handleIncomingMessage(phoenixEvent);
		if (eventHandled) {
			performEventProcess(event);
		}
		
		return eventHandled;
	}
	
	private boolean performEventProcess(PSEvent event) {
		boolean result = true;

		try {
			CalypsoIDAPIUtil.eventProcessed(this._ds.getRemoteTrade(), CalypsoIDAPIUtil.getId(event), this.getEngineName());
		} catch (RemoteException e) {
			Log.error(TradeCollateralizationConstants.ENGINE_NAME, "UploaderImportMessageEngine.performEventProcess(): Error setting event " + CalypsoIDAPIUtil.getId(event) + " to processed.", e);
			result = false;
		}

		return result;
	}

	/**
	 * Default Constructor of the service
	 * 
	 * @param configName
	 * @param dsCon
	 * @param hostName
	 * @param esPort
	 */
	public TradeCollateralizationServiceEngine(DSConnection dsCon, String hostName, int port) {
		super(dsCon, hostName, port);
	}

	/**
	 * New message reception. Starts de Incoming handling message method
	 * 
	 * @param adapter
	 *            of the queue
	 * @param message
	 *            reception
	 * @return if a new message has arrived
	 */
	@Override
	public boolean newMessage(final IEAdapter adapter, final ExternalMessage message) {
		if (message == null) {
			return false;
		}

		if (getIEAdapter() == null) {
			setIEAdapter(adapter);
		}

		boolean proc = false;
		PSEventPhoenixIncoming newEvent = new PSEventPhoenixIncoming(message);
        try {
            DSConnection.getDefault().getRemoteTrade().saveAndPublish(newEvent);
            proc = true;
        } catch (RemoteException e) {
            Log.error(TradeCollateralizationConstants.ENGINE_NAME, "Could not publish Incoming Phoenix Event " + e.toString());
        }

		return proc;
	}

	/**
	 * Defines de logic of the service. builds a fake trade, retrieves a suitable Mrg Contract and checks if it matches
	 * the contract. Finally it returns a response in a ExternalMessage format (content is a String line).
	 * 
	 * @param the
	 *            external message received and passed by the newMessage Method
	 * @return if the incoming message has been handle
	 */
	@Override
	public boolean handleIncomingMessage(final ExternalMessage externalMessage) {
		PSEventPhoenixIncoming phoenixEvent = new PSEventPhoenixIncoming(externalMessage);
		
		return handleIncomingMessage(phoenixEvent);
	}
	
	private boolean handleIncomingMessage(PSEventPhoenixIncoming phoenixEvent) {
		Log.info(TradeCollateralizationConstants.ENGINE_NAME, "A) Received message: " + phoenixEvent.getMessage() + "\n");
		
		final TradeCollateralizationMessageHandler handler = new TradeCollateralizationMessageHandler();
		final StringBuilder collateralizationResponses = new StringBuilder();
		JMSQueueMessage answer = null;
		
		try {
			SantFileSaver.saveFile(TradeCollateralizationConstants.ENGINE_NAME.toLowerCase(), "incoming", "txt", "phoenix_dfa", phoenixEvent.getLongId(), phoenixEvent.getMessage());
		} catch (SantFileSaverException e1) {
			Log.error(TradeCollateralizationConstants.ENGINE_NAME, "Error saving input message in a file.");
		}

		// gather a collection of lines with the requests
		final Collection<String> tradesRequestLines = splitRows(phoenixEvent.getMessage());

		if (messageIsEmpty(tradesRequestLines)) {
			return false;
		}
		
		int count = 1;
		for (String newLineTradeRequest : tradesRequestLines) {
			TradeCollateralizationInputBean inputBean = null;

			// receive by error empty line, jump to next one if there is any
			if (newLineTradeRequest.trim().isEmpty()) {
				continue;
			}

			Log.info(TradeCollateralizationConstants.ENGINE_NAME, phoenixEvent.getLongId() + " - Received Input : " + newLineTradeRequest);
			try {
				// retrieve input bean
				inputBean = handler.parseMessage(newLineTradeRequest);
				inputBean.setId(phoenixEvent.getLongId());
			} catch (TradeCollateralizationException e) {
				badFormatErrorLogAndResponse(collateralizationResponses, newLineTradeRequest, e);

				Log.info(TradeCollateralizationConstants.ENGINE_NAME, phoenixEvent.getLongId() + " - Input : Format error.");
				try {
					SantFileSaver.saveFile(TradeCollateralizationConstants.ENGINE_NAME.toLowerCase(), "outgoing", "format-error." + String.valueOf(count), "phoenix_dfa", phoenixEvent.getLongId(), RESPONSES.ERR_INPUT_FORMAT_INCORRECT.getResponseValue() + SEPARATOR + newLineTradeRequest);
				} catch (SantFileSaverException e2) {
					Log.error(TradeCollateralizationConstants.ENGINE_NAME, "Error saving input message in a file - " + e2.toString());
				}
				continue; // jump next petition if any still queued
			}

			if (!inputBean.isPhoenix()) {
				/*
				 * depending on the type of request done: ----------------------------------------------------------------
				 * 1. collateralization of simulated trade ---------------------------------------------------------------
				 * 2. retrieve from DB and get collateralization degree ---------------------------------------------------
				 */
				if (inputBean.isSimulated()) { // 1. trade simulation
					final String lineResponse = simulatedTradeCollateralization(inputBean);
					collateralizationResponses.append(lineResponse).append(NEW_LINE);
					try {
						SantFileSaver.saveFile(TradeCollateralizationConstants.ENGINE_NAME.toLowerCase(), "outgoing", String.valueOf(count), "dfa", inputBean.getId(), lineResponse);
					} catch (SantFileSaverException e) {
						Log.error(TradeCollateralizationConstants.ENGINE_NAME, "Error saving input message in a file - " + e.toString());
					}
				} else { // 2. get collateralization degree from BO Reference + BO system
					final String lineResponse = TradeBORefBOSystemCollateralization(inputBean);
					collateralizationResponses.append(lineResponse).append(NEW_LINE);
					try {
						SantFileSaver.saveFile(TradeCollateralizationConstants.ENGINE_NAME.toLowerCase(), "outgoing", String.valueOf(count), "dfa", inputBean.getId(), lineResponse);
					} catch (SantFileSaverException e) {
						Log.error(TradeCollateralizationConstants.ENGINE_NAME, "Error saving input message in a file - " + e.toString());
					}
				}
			}
			else {
				final String lineResponse = simulatedTradeCollateralization(inputBean);
				collateralizationResponses.append(lineResponse).append(NEW_LINE);

				try {
					SantFileSaver.saveFile(TradeCollateralizationConstants.ENGINE_NAME.toLowerCase(), "outgoing", String.valueOf(count), "phoenix", inputBean.getId(), lineResponse);
				} catch (SantFileSaverException e) {
					Log.error(TradeCollateralizationConstants.ENGINE_NAME, "Error saving input message in a file - " + e.toString());
				}
			}
			
			count++;
		} // end processing all lines, one line per trade question

		final String output = collateralizationResponses.toString();
		// send response to queue
		try {
			answer = new JMSQueueMessage();
			// set the correlation id on the message to send out
			answer.setText(output);
			answer.setCorrelationId(phoenixEvent.getCorrelationId());
			answer.setReference(phoenixEvent.getReference()); 

			sendAnswer(answer);
		} catch (final Exception e) {
			Log.error(TradeCollateralizationConstants.ENGINE_NAME, e + ": " + e.getMessage(), e);
		}

		Log.info(TradeCollateralizationConstants.ENGINE_NAME, "B) Message output: " + output + "\n");

		return true;
	}

	/**
	 * send the answer to the MiddleWare
	 * 
	 * @param answer
	 *            message to send back
	 * @throws Exception
	 *             if there is a sending problem
	 */
	private void sendAnswer(final JMSQueueMessage answer) throws Exception {
		if (Log.isDebug()) {
			Log.debug(TradeCollateralizationConstants.ENGINE_NAME, answer.getText().toString());
		}

		final IEAdapter sender = getIEAdapter().getIEAdapterConfig().getSenderIEAdapter();
		if (sender == null) {
			Log.error(TradeCollateralizationConstants.ENGINE_NAME, "Sender is null. Answer = " + answer.toString());
			return;
		}
		if (sender instanceof SantanderIEAdapter) {
			((SantanderIEAdapter) sender).write(answer);
		} else {
			writeMessage(getIEAdapter(), answer.toString());
		}
	}

	/**
	 * builds a fake trade, retrieves a suitable Mrg Contract and checks if it matches the contract. Finally it returns
	 * a response in a String line with the appropiate response format.
	 * 
	 * @param the
	 *            String line with a simulation request
	 * @return String with the collateralized response
	 */
	private String simulatedTradeCollateralization(final TradeCollateralizationInputBean inputBean) {
		final TradeCollateralizationMessageHandler handler = new TradeCollateralizationMessageHandler();
		String output = null;
		Map<DFA_INPUT_SIMULATED_FIELDS, String> inputMap = null;
 
		try {
			// parser to input map
			inputMap = TradeCollateralizationLogic.getMap(inputBean);
			
			StringBuilder sb = new StringBuilder("\n");
	        for (Entry<TradeCollateralizationConstants.DFA_INPUT_SIMULATED_FIELDS, String> entry : inputMap.entrySet()) {
	        	sb.append(entry.getKey().getFieldName());
	        	sb.append("=");
	        	sb.append(entry.getValue());
	        	sb.append("\n");
	        }
	        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - inputMap : " + sb.toString());

			// validate input fields of the map
			output = TradeCollateralizationLogic.validateInputTradeMap(inputMap, inputBean);
		} catch (TradeCollateralizationException e) {
			Log.error(TradeCollateralizationConstants.ENGINE_NAME, e.getLogMessage());
			return (e.getLocalizedMessage());
		}

		// Error found is output != null -> DUMMY RESPONSE with ERROR TYPE
		if (output != null) {// if there was an error we return it
			Log.error(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - Input : Error found : " + output);
			
			String outputMessage;
			if (!inputBean.isPhoenix()) {
			 outputMessage = handler.generateMessageResponse(TradeCollateralizationLogic
					.getOutputBean(TradeCollateralizationLogic.buildDummyOutputMap(output, inputMap, inputBean)));
			}
			else {
				outputMessage = handler.generateMessageResponsePhoenix(TradeCollateralizationLogic
						.getOutputBean(TradeCollateralizationLogic.buildDummyOutputMap(output, inputMap, inputBean)));
			}
			
			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - outputMessage = " + outputMessage);

			return outputMessage;
		}

		// generate fake trade to simulate the collateralization degree
		final Trade simulatedTrade = TradeCollateralizationLogic.buildTradeFromInputMap(inputMap, inputBean);
 
		Map<DFA_OUTPUT_FIELDS, String> outputMap;
		if (simulatedTrade != null) {
			// retrieve the output: ERROR, UNCOLLATERALIZED, PARTIAL, ONE-WAY, FULL_COLLATERALIZED
			outputMap = TradeCollateralizationLogic
					.tradeCollateralizationDegreeAndBuildOutput(inputMap, simulatedTrade, inputBean);
		}
		else {
			outputMap = TradeCollateralizationLogic.buildDummyOutputMap(null, inputMap, inputBean);
		}
		
		StringBuilder sb = new StringBuilder("\n");
        for (Entry<TradeCollateralizationConstants.DFA_OUTPUT_FIELDS, String> entry : outputMap.entrySet()) {
        	sb.append(entry.getKey().getFieldName());
        	sb.append("=");
        	sb.append(entry.getValue());
        	sb.append("\n");
        }
        Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - outputMap : " + sb.toString());

		// get output Bean
		final TradeCollateralizationOutputBean outputBean = TradeCollateralizationLogic.getOutputBean(outputMap);

		// generate output message
		String outputMessage = "";
		if (!inputBean.isPhoenix()) {
			outputMessage = handler.generateMessageResponse(outputBean);
		}
		else {
			outputMessage = handler.generateMessageResponsePhoenix(outputBean);
		}
		
		Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - outputMessage = " + outputMessage);
		
		return outputMessage;
	}

	
	private String TradeBORefBOSystemCollateralization(final TradeCollateralizationInputBean inputBean) {
		return TradeBORefBOSystemCollateralization(inputBean, null);
	}
	
	
	/**
	 * Retrieves a trade using it's BO Reference ID and they BO System. If found, it returns a response in a String line
	 * with the appropiate response format.
	 * 
	 * @param the
	 *            String line with a simulation request
	 * @return String with the collateralized response
	 */
	private String TradeBORefBOSystemCollateralization(final TradeCollateralizationInputBean inputBean, Trade trade) {
		final TradeCollateralizationMessageHandler handler = new TradeCollateralizationMessageHandler();
		TradeCollateralizationOutputBean outputBean = null;
		String outputMessage = "";

		// retrieve trade from DB
		if (trade == null) {
			trade = findTradeBOrefSystem(inputBean.getBOExternalReference(), inputBean.getBOSourceSystem());
		}
			
		if (trade == null) {// UNCOLLATERALIZED
			// generate output bean degree 3 - Uncoll.
			outputBean = TradeCollateralizationLogic.getUncollateralizedOutputBean(inputBean);
			// return
			outputMessage = handler.generateMessageResponse(outputBean);
			
			Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - outputMessage = " + outputMessage);
			
			return outputMessage;
		}
		
		outputBean = TradeCollateralizationLogic.getExistingTradeOutputBean(trade, inputBean);
		
		// generate output message
		
		if (!inputBean.isPhoenix()) {
			outputMessage = handler.generateMessageResponse(outputBean);
		}
		else {
			outputMessage = handler.generateMessageResponsePhoenix(outputBean);
		}
		
		Log.info(TradeCollateralizationConstants.ENGINE_NAME, inputBean.getId() + " - outputMessage = " + outputMessage);
		
		return outputMessage;
	}

	/**
	 * 
	 * @param externalReference
	 * @param sourceSystem
	 * @return
	 */
	private Trade findTradeBOrefSystem(final String externalReference, final String sourceSystem) {

		// check the existence of trades with the BO_SYSTEM and BO_REFERENCE information
		TradeArray existingTrades = TradeInterfaceUtils.getTradeByBORefAndBOSystem(sourceSystem, externalReference);
		Trade oldTrade = null;

		if ((existingTrades != null) && (existingTrades.size() > 0)) {
			oldTrade = existingTrades.get(0);

			if (existingTrades.size() > 1) {
				// this should not happen
				Log.error(TradeCollateralizationConstants.ENGINE_NAME,
						"More than one trade exist for the same BO_SYSTEM and BO_REFERENCE: " + sourceSystem + "-"
								+ externalReference);
				return oldTrade;
			}
		}
		return oldTrade;
	}

	/**
	 * Name of the engine that offers this service
	 */
	@Override
	public String getEngineName() {
		return TradeCollateralizationConstants.ENGINE_NAME;
	}

	// ///////////////////////////////////
	// //////PRIVATE METHODS /////////////
	// ///////////////////////////////////

	/*
	 * checks if a message is empty
	 */
	private boolean messageIsEmpty(Collection<String> lines) {
		return ((lines == null) || lines.isEmpty());
	}

	/*
	 * returns a collection of lines separated with \n
	 */
	private Collection<String> splitRows(String message) {
		if (Util.isEmpty(message)) {
			return new ArrayList<String>();
		}

		final String content = message;
		final String[] rows = content.split(NEW_LINE);

		return Arrays.asList(rows);
	}

	/*
	 * adds an error to the log and a response
	 */ 
	private void badFormatErrorLogAndResponse(final StringBuilder collateralizationResponses,
			final String newLineTradeRequest, final TradeCollateralizationException e) {

		if (e != null) {
			Log.error(TradeCollateralizationConstants.ENGINE_NAME, e.toString());
		}
		collateralizationResponses.append(RESPONSES.ERR_INPUT_FORMAT_INCORRECT.getResponseValue());
		if (!newLineTradeRequest.trim().startsWith(SEPARATOR)) {
			collateralizationResponses.append(SEPARATOR);
		}

		collateralizationResponses.append(newLineTradeRequest);
		collateralizationResponses.append(NEW_LINE);
	}

	@Override
	public boolean handleIncomingMessage(String message, List<Task> tasks) throws Exception {
		return false;
	}

	@Override
	public String handleOutgoingMessage(PSEvent event, List<Task> tasks) throws Exception {
		return null;
	}
}
