/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */

package calypsox.tk.collateral.service.efsonlineservice;

/**
 * This specific exception allows to control the flow for EFS possible Exceptions
 * 
 * @author Guillermo Solano
 * @version 1.0, 07/06/2013
 * 
 */
public class EFSException extends Exception {

	/** uuid */
	private static final long serialVersionUID = 8264269297052585978L;

	/**
	 * If the exception is critic, set to true
	 */
	private final boolean critic;
	/**
	 * If the warning type requires the thread to sleep, set to true;
	 */
	private boolean wait;

	/**
	 * Basic Constructor.
	 */
	public EFSException() {
		super();
		this.critic = false;
		this.wait = false;
	}

	/**
	 * 
	 * @param message
	 * @param critic
	 * @param wait
	 */
	public EFSException(String message, boolean critic, boolean wait) {
		super(message);
		this.critic = critic;
		this.wait = wait;
	}

	/**
	 * 
	 * @param message
	 * @param critic
	 */
	public EFSException(String message, boolean critic) {
		this(message, critic, false);
	}

	/**
	 * @param message
	 */
	public EFSException(String message) {
		this(message, false, false);

	}

	/**
	 * @param cause
	 */
	public EFSException(Throwable cause) {
		super(cause);
		this.critic = this.wait = false;
	}

	/**
	 * @param message
	 * @param cause
	 */
	public EFSException(String message, Throwable cause) {
		super(message, cause);
		this.critic = false;
	}

	/**
	 * @return is the exception requires the thread to sleep
	 */
	public boolean mustWait() {
		return this.wait;
	}

	/**
	 * @return is the exception is critic and all engines must stop
	 */
	public boolean isCritical() {
		return this.critic;
	}

}
