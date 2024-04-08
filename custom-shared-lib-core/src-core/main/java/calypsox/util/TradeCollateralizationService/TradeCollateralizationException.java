/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * This specific exception allows to control the flow for DFA collateralization online service possible Exceptions
 * 
 * @author Guillermo Solano
 * @version 1.0, 17/07/2013
 * 
 */
package calypsox.util.TradeCollateralizationService;

import static calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.EMPTY;
/**
 * @author Guillermo
 * 
 */
public class TradeCollateralizationException extends Exception {

	/** uuid */
	private static final long serialVersionUID = -727978300770394874L;

	/** specific log message */
	private String logMessage;

	/**
	 * Basic Constructor.
	 */
	public TradeCollateralizationException() {
		super();
		this.logMessage = EMPTY;
	}

	/**
	 * @param message
	 *            of the exception
	 */
	public TradeCollateralizationException(String message) {
		super(message);
		this.logMessage = EMPTY;
	}

	/**
	 * @param cause
	 */
	public TradeCollateralizationException(Throwable cause) {
		super(cause);
		this.logMessage = EMPTY;
	}

	/**
	 * @return the logMessage
	 */
	public String getLogMessage() {
		return this.logMessage;
	}

	/**
	 * @param logMessage
	 *            the logMessage to set
	 */
	public void setLogMessage(String logMessage) {
		this.logMessage = logMessage;
	}

}
