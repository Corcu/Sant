/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */

package com.santander.collateral.util.email;

import com.calypso.tk.core.Log;
import com.calypso.tk.util.MailConfig;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Santander specific Mail Configuration, allowing to read from the calypso_mail_config.properties inside the resources
 * folder the configuration for secure or normal smtp connections
 * 
 * @author Guillermo Solano
 * @version 1.0
 * 
 */
public class SantanderMailConfig extends MailConfig {

	/*
	 * Constants to define the name of the properties inside the file.
	 */
	public static final String PASSWORD = "PASSWORD";
	public static final String USER = "USER";
	public static final String HOST = "HOST";
	public static final String PORT = "PORT";
	// public static final String SECURE = "MODE_AUTHENTICATION"; // 1 is true. zero or missing is false.
	private static final String FROM = "FROM";

	/**
	 * optional: setup only if the server needs authentication
	 */
	protected String user;
	/**
	 * optional: setup only if the server needs authentication
	 */
	protected String password;
	/**
	 * indicates if the config allows secure email
	 */
	private boolean secureEmail;
	/**
	 * optional: from address
	 */
	protected String from;
	/**
	 * properties read from file.
	 */
	private Properties properties;

	/**
	 * Constructor
	 * 
	 * @throws MessagingException
	 *             if we dont have the email properties file and at least server and port has been read
	 */
	public SantanderMailConfig() throws MessagingException {

		super();
		this.user = this.password = this.from = null;
		this.secureEmail = false;
		loadProperties();
	}

	/**
	 * indicates if the server needs authentication
	 * 
	 * @return true if the server needs authentication
	 */
	public boolean useAuthentication() {
		return this.secureEmail;
	}

	/**
	 * return the user
	 * 
	 * @return the user
	 */
	public String getUser() {
		return this.user;
	}

	/**
	 * return the password
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * return the secure host
	 * 
	 * @return the secure host
	 */
	public String getSecureHostName() {
		return super.getHostName();
	}

	/**
	 * 
	 * @return the secure port
	 */
	public int getSecurePort() {

		return super.getPort();
	}

	/**
	 * 
	 * @return the from sender email
	 */
	public String getFrom() {

		return this.from;
	}

	/**
	 * Read the properties from the file specified in the CALYPSO_MAIL_CONFIG_PROPERTIES, given by the method
	 * getConfigName of the super class.
	 * 
	 * @return true if the properties have been loaded, false in case of error
	 * @throws MessagingException
	 */
	private boolean loadProperties() throws MessagingException {

		final String configFileName = super.getConfigName(); // get the properties file name
		final InputStream is = super.getClass().getClassLoader().getResourceAsStream(configFileName);
		if (is != null) {
			try {
				this.properties = new Properties();
				this.properties.load(is);
			} catch (final IOException e) {
				Log.error(this, "Can not load properties from: " + configFileName);
				Log.error(this, e); //sonar
				return false;
			}
		} else {
			Log.error(this, "Can not find properties file from: " + configFileName);
			return false;
		}

		return parseProperties();
	}

	/*
	 * reads the user and pass (optional). If MainConfig hasn't read the server and port generates error
	 */
	private boolean parseProperties() throws MessagingException {

		setPassword(this.properties.getProperty(PASSWORD));
		setUser(this.properties.getProperty(USER));
		setSecureHost(this.properties.getProperty(HOST));
		setSecurePort(this.properties.getProperty(PORT));
		setFrom(this.properties.getProperty(FROM));

		final boolean result = super.isMailConfigured();
		this.secureEmail = checkIsSecureConfiguration();

		if (!result) {

			generateMessagingException("In properties file " + super.getConfigName() + "host and port are mandatory");

		}
		return result;
	}

	/**
	 * set the user
	 * 
	 * @param user
	 *            the user to set. NOT MANDATORY
	 */
	private void setUser(final String user) {
		this.user = user;
	}

	/**
	 * set the password
	 * 
	 * @param password
	 *            the password to set. NOT MANDATORY
	 */
	private void setPassword(final String password) {
		this.password = password;
	}

	/**
	 * set the SecurePort
	 * 
	 * @param SecurePort
	 *            the port to set in secure mode
	 * @throws MessagingException
	 *             if port is not a number
	 */
	private void setSecurePort(final String port) throws MessagingException {
		try {
			super.setPort(Integer.parseInt(port));

		} catch (NumberFormatException e) {

			generateMessagingException("PARSSING ERROR: In properties file " + super.getConfigName()
					+ " the attribute " + PORT + " has to be a number ");
		}
	}

	/**
	 * set the from
	 * 
	 * @param from
	 *            the from to set
	 */
	private void setFrom(final String from) {
		this.from = from;
	}

	/**
	 * set the SecureHost
	 * 
	 * @param host
	 *            the host to set in secure mode
	 * @throws MessagingException
	 *             if host is not included
	 */
	private void setSecureHost(final String host) throws MessagingException {

		if ((host == null) || host.isEmpty()) {
			generateMessagingException("PARSSING ERROR: In properties file " + super.getConfigName()
					+ " the attribute " + HOST + " is empty (expects an Address). ");
		}
		super.setHost(host);

	}

	/*
	 * generates the log and throws an exception
	 */
	private void generateMessagingException(String error) throws MessagingException {

		Log.error(this, error);
		throw new MessagingException(error);
	}

	/**
	 * 
	 * @return checks if the SECURE attribute is true or 1. If it is, and pass and user were read, this is a secure
	 *         configuration
	 */
	private boolean checkIsSecureConfiguration() {

		// final String readSec = this.properties.getProperty(SECURE);
		//
		// if ((readSec == null) || readSec.isEmpty()) {
		// Log.warn(this, "PARSSING ERROR: In properties file " + super.getConfigName() + " the attribute " + SECURE
		// + " is empty or not Present. SECURE MODE NOT ACTIVATED! ");
		// }
		//
		// if (correctSecureModeAttribute(readSec)) {
		// return false;
		// }

		return (((this.user != null) && !this.user.isEmpty()) && ((this.password != null) && !this.password.isEmpty()) && ((this.from != null) && !this.from
				.isEmpty()));
		// (readSec.equalsIgnoreCase("1") || readSec.equalsIgnoreCase("true")));

	}

	/*
	 * consideration of the what a secure method value has to be
	 */
	@SuppressWarnings("unused")
	private boolean correctSecureModeAttribute(String mode) {

		return !((mode == null) || !mode.equalsIgnoreCase("1") || !mode.equalsIgnoreCase("0")
				|| !mode.equalsIgnoreCase("true") || !mode.equalsIgnoreCase("false"));

	}

}
