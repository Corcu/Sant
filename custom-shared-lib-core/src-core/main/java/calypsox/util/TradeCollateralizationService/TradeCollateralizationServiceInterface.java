/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util.TradeCollateralizationService;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.util.IEAdapter;

/**
 * This interface implements the Trade Collateralization service methods. This online service has been requested to
 * satisfy the Dodd-Frank collateralization trades.
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 04/03/2013
 * 
 */
public interface TradeCollateralizationServiceInterface {

	/**
	 * New message reception. Starts de Incoming handling message method
	 * 
	 * @param adapter
	 *            of the queue
	 * @param message
	 *            reception
	 * @return if a new message has arrived
	 */
	public boolean newMessage(IEAdapter adapter, ExternalMessage message);

	/**
	 * Defines de logic of the service. builds a fake trade, retrieves a suitable Mrg Contract and checks if it matches
	 * the contract. Finally it returns a response in a ExternalMessage format (content is a String line).
	 * 
	 * @param the
	 *            external message received and passed by the newMessage Method
	 * @return if the incoming message has been handle
	 */
	public boolean handleIncomingMessage(ExternalMessage externalMessage);

	/**
	 * Name of the engine that offers this service
	 */
	public String getEngineName();

}
