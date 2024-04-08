/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */

package calypsox.tk.report;

/**
 * Threshold Parameters Items.
 * 
 * @author Juan Angel Torija
 * 
 */
public class CVA_ThresholdOptParamItem {
	/**
	 * Maximum Decimal places for the control line.
	 */
	public static final int CONTROL_MAX_DECIMAL_LENGTH = 0;
	/**
	 * Length for the control line.
	 */
	public static final int CONTROL_LENGTH = 8;

	public static final String CVA_THRESHOLDPARAMETER_ITEM = "CVA_ThresholdParametersItem";

	// Contract name
	private String agreementName;
	// PO short name
	private String owner;
	// PO long name
	private String ownerName;
	// CPTY short name
	private String counterparty;
	// CPTY long name
	private String counterpartyName;
	// Threshold Type of PO
	private String thresholdTypePO;
	// Threshold Value of PO
	private String thresholdValuePO;
	// Threshold Currency of PO
	private String thresholdCurrencyPO;
	// Optional - Rating Matrix when PO threshold Type is Global Rating
	private String ratingMatrixGRPO;
	// Threshold Type of CPTY
	private String thresholdTypeCpty;
	// Threshold Value of CPTY
	private String thresholdValueCpty;
	// Threshold Currency of CPTY
	private String thresholdCurrencyCpty;
	// Optional - Rating Matrix when CPTY threshold Type is Global Rating
	private String ratingMatrixGRCpty;

	public String getAgreementName() {
		return this.agreementName;
	}

	public void setAgreementName(String agreementName) {
		this.agreementName = agreementName;
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public String getCounterparty() {
		return this.counterparty;
	}

	public void setCounterparty(String counterparty) {
		this.counterparty = counterparty;
	}

	public String getCounterpartyName() {
		return this.counterpartyName;
	}

	public void setCounterpartyName(String counterpartyName) {
		this.counterpartyName = counterpartyName;
	}

	public String getThresholdTypePO() {
		return this.thresholdTypePO;
	}

	public void setThresholdTypePO(String thresholdTypePO) {
		this.thresholdTypePO = thresholdTypePO;
	}

	public String getThresholdValuePO() {
		return this.thresholdValuePO;
	}

	public void setThresholdValuePO(String thresholdValuePO) {
		this.thresholdValuePO = thresholdValuePO;
	}

	public String getThresholdCurrencyPO() {
		return this.thresholdCurrencyPO;
	}

	public void setThresholdCurrencyPO(String thresholdCurrencyPO) {
		this.thresholdCurrencyPO = thresholdCurrencyPO;
	}

	public String getRatingMatrixGRPO() {
		return this.ratingMatrixGRPO;
	}

	public void setRatingMatrixGRPO(String ratingMatrixGRPO) {
		this.ratingMatrixGRPO = ratingMatrixGRPO;
	}

	public String getThresholdTypeCpty() {
		return this.thresholdTypeCpty;
	}

	public void setThresholdTypeCpty(String thresholdTypeCpty) {
		this.thresholdTypeCpty = thresholdTypeCpty;
	}

	public String getThresholdValueCpty() {
		return this.thresholdValueCpty;
	}

	public void setThresholdValueCpty(String thresholdValueCpty) {
		this.thresholdValueCpty = thresholdValueCpty;
	}

	public String getThresholdCurrencyCpty() {
		return this.thresholdCurrencyCpty;
	}

	public void setThresholdCurrencyCpty(String thresholdCurrencyCpty) {
		this.thresholdCurrencyCpty = thresholdCurrencyCpty;
	}

	public String getRatingMatrixGRCpty() {
		return this.ratingMatrixGRCpty;
	}

	public void setRatingMatrixGRCpty(String ratingMatrixGRCpty) {
		this.ratingMatrixGRCpty = ratingMatrixGRCpty;
	}

}
