/*
 *
 * Copyright (c) 2000 by Calypso Technology, Inc.
 * 595 Market Street, Suite 1980, San Francisco, CA  94105, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Calypso Technology, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Calypso Technology.
 *
 */
package calypsox.tk.util;

import java.io.IOException;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;

import calypsox.tk.bo.JMSQueueMessage;

import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.core.Log;

/**
 * The Class ExceptionUtil.
 * 
 * Utility class handling the exception message formating
 * 
 * @author Bruno P.
 * @version 1.0
 * @since 03/21/2011
 */
public class JMSQueueAnswer extends JMSQueueMessage {
	/** OK message */
	public static final String OK = "OK";
	/** Not OK generic message */
	public static final String KO = "KO";

	/** code of the response */
	private String code = null;
	/** full description of the response */
	private String description = null;
	/** reference of the linked request */
	private String destinationKey = null;
	/** reference to the Front Office object */
	private String transactionKey = null;
	/** tradeIf for the mirror trade (slave - tradeId < mirror_tradeId ) */
	private String mirrorKey = null;
	/** Keyword Partenon-IDCONTR */
	private String partenonIdContr = null;
	/** Keyword Partenon-IDCONTR_NEAR */
	private String partenonIdContrNear = null;
	/** Keyword Partenon-IDCONTR_FAR */
	private String partenonIdContrFar = null;
	/** Keyword Partenon-IDCONTR_MIRROR */
	private String partenonIdContrMirror = null;
	/** Keyword Partenon-IDCONTR_NEAR_MIRROR */
	private String partenonIdContrNearMirror = null;
	/** Keyword Partenon-IDCONTR_FAR_MIRROR */
	private String partenonIdContrFarMirror = null;
	/** MirrorSource only true for an internal deal for the master */
	private String mirrorSource = null;

	private String ETTEventType = null;

	// Variables to generate XML String.
	/** document */
	private Document document;
	/** transactionConfElmn */
	private Element transactionConfElmn;

	/**
	 * Default constructor
	 */
	public JMSQueueAnswer() {
	}

	/**
	 * Used Constructor
	 * 
	 * @param message
	 *            external message that ask for this response
	 */
	public JMSQueueAnswer(final ExternalMessage message) {
		this.code = OK;

		if (message instanceof JMSQueueMessage) {
			setReference(((JMSQueueMessage) message).getReference());
			setCorrelationId(((JMSQueueMessage) message).getCorrelationId());
		}
	}

	/**
	 * Returns a string representation of the object.
	 * 
	 * @return a string representation of the object.
	 */
	@Override
	public String toString() {
		return generateXML();
	}

	/**
	 * Same as toString()
	 * 
	 * @return a string representation of the object.
	 */
	@Override
	public String getText() {
		return toString();
	}

	/**
	 * Set the transactionKey
	 * 
	 * @param transactionKey
	 *            new transacitonKey
	 */
	public void setTransactionKey(final String transactionKey) {
		this.transactionKey = transactionKey;
	}

	/**
	 * Get the transactionKey
	 * 
	 * @return the reference of the object in the Front Office system
	 */
	public String getTransactionKey() {
		return this.transactionKey;
	}

	/**
	 * Set the code
	 * 
	 * @param code
	 *            new code
	 */
	public void setCode(final String code) {
		this.code = code;
	}

	/**
	 * Get the code
	 * 
	 * return the code
	 */
	public String getCode() {
		return this.code;
	}

	/**
	 * set the description
	 * 
	 * @param description
	 *            new description
	 */
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * Get the message description
	 * 
	 * @return the message description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Set the destination key
	 * 
	 * @param destinationKey
	 *            new destinationKey
	 */
	public void setDestinationKey(final String destinationKey) {
		this.destinationKey = destinationKey;
	}

	/**
	 * Get the destination key
	 * 
	 * @return the destinationKey
	 */
	public String getDestinationKey() {
		return this.destinationKey;
	}

	/**
	 * Set the mirror key
	 * 
	 * @param mirrorKey
	 *            new mirror key
	 */
	public void setMirrorKey(final String mirrorKey) {
		this.mirrorKey = mirrorKey;
	}

	/**
	 * Get the mirror key
	 * 
	 * @return the mirrorKey
	 */
	public String getMirrorKey() {
		return this.mirrorKey;
	}

	/**
	 * Set the partenonIdContr
	 * 
	 * @param partenonIdContr
	 *            new partenonIdContr
	 */
	public void setPartenonIdContr(final String partenonIdContr) {
		this.partenonIdContr = partenonIdContr;
	}

	/**
	 * Get the PartenonIdContr
	 * 
	 * @return the PartenonIdContr
	 */
	public String getPartenonIdContr() {
		return this.partenonIdContr;
	}

	/**
	 * Set the partenonIdContrNear
	 * 
	 * @param partenonIdContrNear
	 *            new partenonIdContrNear
	 */
	public void setPartenonIdContrNear(final String partenonIdContrNear) {
		this.partenonIdContrNear = partenonIdContrNear;
	}

	/**
	 * Get the PartenonIdContrNear
	 * 
	 * @return the PartenonIdContrNear
	 */
	public String getPartenonIdContrNear() {
		return this.partenonIdContrNear;
	}

	/**
	 * Set the partenonIdContrFar
	 * 
	 * @param partenonIdContrFar
	 *            new partenonIdContrFar
	 */
	public void setPartenonIdContrFar(final String partenonIdContrFar) {
		this.partenonIdContrFar = partenonIdContrFar;
	}

	/**
	 * Get the PartenonIdContrFar
	 * 
	 * @return the PartenonIdContrFar
	 */
	public String getPartenonIdContrFar() {
		return this.partenonIdContrFar;
	}

	/**
	 * Set the partenonIdContrMirror
	 * 
	 * @param partenonIdContrMirror
	 *            new partenonIdContrMirror
	 */
	public void setPartenonIdContrMirror(final String partenonIdContrMirror) {
		this.partenonIdContrMirror = partenonIdContrMirror;
	}

	/**
	 * Get the PartenonIdContrMirror
	 * 
	 * @return the PartenonIdContrMirror
	 */
	public String getPartenonIdContrMirror() {
		return this.partenonIdContrMirror;
	}

	/**
	 * Set the partenonIdContrNearMirror
	 * 
	 * @param partenonIdContrNearMirror
	 *            new partenonIdContrNearMirror
	 */
	public void setPartenonIdContrNearMirror(final String partenonIdContrNearMirror) {
		this.partenonIdContrNearMirror = partenonIdContrNearMirror;
	}

	/**
	 * Get the PartenonIdContrNearMirror
	 * 
	 * @return the PartenonIdContrNearMirror
	 */
	public String getPartenonIdContrNearMirror() {
		return this.partenonIdContrNearMirror;
	}

	/**
	 * Set the partenonIdContrFarMirror
	 * 
	 * @param partenonIdContrFarMirror
	 *            new partenonIdContrFarMirror
	 */
	public void setPartenonIdContrFarMirror(final String partenonIdContrFarMirror) {
		this.partenonIdContrFarMirror = partenonIdContrFarMirror;
	}

	/**
	 * Get the PartenonIdContrFarMirror
	 * 
	 * @return the PartenonIdContrFarMirror
	 */
	public String getPartenonIdContrFarMirror() {
		return this.partenonIdContrFarMirror;
	}

	/**
	 * Set the mirrorSource
	 * 
	 * @param partenonIdContrFarMirror
	 *            new partenonIdContrFarMirror
	 */
	public void setMirrorSource(final String mirrorSource) {
		this.mirrorSource = mirrorSource;
	}

	/**
	 * Get the mirrorSource
	 * 
	 * @return the mirrorSource
	 */
	public String getMirrorSource() {
		return this.mirrorSource;
	}

	/**
	 * Set the exception description and code
	 * 
	 * @param exception
	 *            new exception
	 */
	public void setException(final Exception exception) {
		this.description = exception.getMessage();
		this.code = getExceptionCode();
	}

	public String getETTEventType() {
		return this.ETTEventType;
	}

	public void setETTEventType(final String eTTEventType) {
		this.ETTEventType = eTTEventType;
	}

	/**
	 * Get a specific code for an exception
	 * 
	 * @return the specific code
	 */
	public String getExceptionCode() {
		if (this.description == null) {
			return KO;
		}
		if (this.description.contains("XML document structures must start and end within the same entity")) {
			return "BADFORMED_XML";
		}
		if (this.description.contains("is not valid on Status")) {
			return "INVALID_ACTION";
		}
		if (this.description.contains("Could not translate as a Book")) {
			return "NO_BOOK";
		}
		if (this.description.contains("Could not translate as a LegalEntity")) {
			return "NO_COUNTERPARTY";
		}
		if (this.description.contains("Could not translate as a FXReset ")) {
			return "NO_FX_RESET";
		}
		if (this.description.contains("No Currency pair for name ")) {
			return "NO_FX_CCY_PAIR";
		}
		if (this.description.contains("Legal Entity role:")) {
			return "BAD_COUNTERPARTY_ROLE";
		}
		if (this.description.contains("Could not translate as a CurrencyPair")) {
			return "BAD_CURRENCY_PAIR";
		}
		if (this.description.contains("java.lang.NumberFormatException")) {
			return "BADFORMED_NUMBER";
		}
		if (this.description.contains("java.lang.IllegalArgumentException")) {
			return "BADFORMED_DATE";
		}
		if (this.description.contains("java.util.NoSuchElementException")) {
			return "UNKNOWN_CURRENCY_PAIR";
		}
		return KO;
	}

	/**
	 * Generates the XML String with necessary elements.
	 * 
	 * @return XML String generated.
	 */
	private String generateXML() {
		final StringWriter strWriter = new StringWriter();
		final org.dom4j.io.OutputFormat format = new org.dom4j.io.OutputFormat();
		format.setEncoding("ISO-8859-1"); // Sets the encoding to format object.
		final XMLWriter output = new XMLWriter(strWriter, format);
		output.setEscapeText(true);

		this.document = DocumentHelper.createDocument();
		this.transactionConfElmn = this.document.addElement("transactionConfirmation");
		generateElementsConfirmation();
		try {
			output.write(this.document);
			output.setIndentLevel(2);
			output.close();
		} catch (final IOException ioe) {
			Log.error(this, "IOException ocurred: " + ioe);
		}

		return strWriter.getBuffer().toString();
	}

	/**
	 * Generates all necessary elements that depend of transactionConfirmation node.
	 */
	private void generateElementsConfirmation() {
		Element correlationElmn = this.transactionConfElmn.addElement("userCorrelation");
		final Element transactionKeyElmn = this.transactionConfElmn.addElement("transactionKey");
		final Element codeElmn = this.transactionConfElmn.addElement("code");
		final Element descElmn = this.transactionConfElmn.addElement("description");
		final Element destKeyElmn = this.transactionConfElmn.addElement("destinationKey");
		final Element mirrorKeyElmn = this.transactionConfElmn.addElement("mirrorKey");
		final Element partenonIdContrElmn = this.transactionConfElmn.addElement("partenonIdContr");
		final Element partenonIdContrNearElmn = this.transactionConfElmn.addElement("partenonIdContrNear");
		final Element partenonIdContrFarElmn = this.transactionConfElmn.addElement("partenonIdContrFar");
		final Element partenonIdContrMirrorElmn = this.transactionConfElmn.addElement("partenonIdContrMirror");
		final Element partenonIdContrNearMirrorElmn = this.transactionConfElmn.addElement("partenonIdContrNearMirror");
		final Element partenonIdContrFarMirrorElmn = this.transactionConfElmn.addElement("partenonIdContrFarMirror");
		final Element mirrorSourceElmn = this.transactionConfElmn.addElement("mirrorSource");

		final Element eTTEventTypeElmn = this.transactionConfElmn.addElement("ETTEventType");

		// Check values of all elements.
		if (null != getCorrelationId()) {
			correlationElmn.setText(getCorrelationId());
		}

		if (null != this.transactionKey) {
			transactionKeyElmn.setText(this.transactionKey);
		}

		if (null != this.code) {
			codeElmn.setText(this.code);
		}

		if (null != this.description) {
			descElmn.setText(this.description);
		}

		if (null != this.destinationKey) {
			destKeyElmn.setText(this.destinationKey);
		}

		if (null != this.mirrorKey) {
			mirrorKeyElmn.setText(this.mirrorKey);
		}

		if (null != this.partenonIdContr) {
			partenonIdContrElmn.setText(this.partenonIdContr);
		}

		if (null != this.partenonIdContrNear) {
			partenonIdContrNearElmn.setText(this.partenonIdContrNear);
		}

		if (null != this.partenonIdContrFar) {
			partenonIdContrFarElmn.setText(this.partenonIdContrFar);
		}

		if (null != this.partenonIdContrMirror) {
			partenonIdContrMirrorElmn.setText(this.partenonIdContrMirror);
		}

		if (null != this.partenonIdContrNearMirror) {
			partenonIdContrNearMirrorElmn.setText(this.partenonIdContrNearMirror);
		}

		if (null != this.partenonIdContrFarMirror) {
			partenonIdContrFarMirrorElmn.setText(this.partenonIdContrFarMirror);
		}

		if (null != this.mirrorSource) {
			mirrorSourceElmn.setText(this.mirrorSource);
		}

		if (null != this.ETTEventType) {
			eTTEventTypeElmn.setText(this.ETTEventType);
		}

	}

}
