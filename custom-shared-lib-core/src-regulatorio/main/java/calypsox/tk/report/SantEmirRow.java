/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.io.Serializable;

/**
 * Row for message CVM and CLM reported for EMIR.
 * 
 * @author xIS16241
 * 
 */
public class SantEmirRow implements Serializable {

	/**
	 * serial version ID
	 */
	private static final long serialVersionUID = 1L;

	/** external id */
	protected String externalId;

	/** source system */
	protected String sourceSystem;

	/** message type */
	protected String messageType;

	/** activity */
	protected String activity;

	/** transaction type */
	protected String transactionType;

	/** product */
	protected String product;

	/** Tag */
	protected String tag;

	/** Value */
	protected String value;

	/**
	 * constructor
	 */
	public SantEmirRow() {
		// nothing to do
	}

	public String getExternalId() {
		return this.externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getSourceSystem() {
		return this.sourceSystem;
	}

	public void setSourceSystem(String sourceSystem) {
		this.sourceSystem = sourceSystem;
	}

	public String getMessageType() {
		return this.messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getActivity() {
		return this.activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}

	public String getTransactionType() {
		return this.transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public String getProduct() {
		return this.product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getTag() {
		return this.tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
