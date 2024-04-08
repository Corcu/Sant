/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util.TradeCollateralizationService;

/**
 * Input Bean of the service Trade TradeCollateralizationServiceEngine.
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 04/03/2013
 */
public class TradeCollateralizationInputBean {
	private String foExternalReference; 
	private String foSourceSystem; 
	private String instrument; 
	private String tipology; 
	private String boExternalReference; // 1
	private String boSourceSystem; // 2
	private String processingOrg; // 3
	private String counterParty; // 4
	private String productType; // 5
	private String productTypeMapped; // 5
	private String startDate; // 6
	private String endDate; // 7
	private String valueDate; // 8
	private String currency; // 9
	private String processingDate; // 10
	private String valuationDate; // 11
	// to indicate is using simulation or retrieve real trade
	private boolean simulated;
	// Phoenix service
	private boolean isPhoenix;
	// For tracking
	private Long id;

	/**
	 * @return the externalReference
	 */
	public String getBOExternalReference() {
		return this.boExternalReference;
	}

	/**
	 * @return the processingOrg
	 */
	public String getProcessingOrg() {
		return this.processingOrg;
	}

	/**
	 * @return the counterParty
	 */
	public String getCounterParty() {
		return this.counterParty;
	}

	/**
	 * @return the productType
	 */
	public String getProductType() {
		return this.productType;
	}

	/**
	 * @return the startDate
	 */
	public String getStartDate() {
		return this.startDate;
	}

	/**
	 * @return the endDate
	 */
	public String getEndDate() {
		return this.endDate;
	}

	/**
	 * @return the valueDate
	 */
	public String getValueDate() {
		return this.valueDate;
	}

	/**
	 * @return the currency
	 */
	public String getCurrency() {
		return this.currency;
	}

	/**
	 * @return the processingDate
	 */
	public String getProcessingDate() {
		return this.processingDate;
	}

	/**
	 * @return the valuationDate
	 */
	public String getValuationDate() {
		return this.valuationDate;
	}

	/**
	 * @param externalReference
	 *            the externalReference to set
	 */
	public void setBOExternalReference(String externalReference) {
		this.boExternalReference = externalReference;
	}

	/**
	 * @param processingOrg
	 *            the processingOrg to set
	 */
	public void setProcessingOrg(String processingOrg) {
		this.processingOrg = processingOrg;
	}

	/**
	 * @param counterParty
	 *            the counterParty to set
	 */
	public void setCounterParty(String counterParty) {
		this.counterParty = counterParty;
	}

	/**
	 * @param productType
	 *            the productType to set
	 */
	public void setProductType(String productType) {
		this.productType = productType;
	}

	/**
	 * @param startDate
	 *            the startDate to set
	 */
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	/**
	 * @param endDate
	 *            the endDate to set
	 */
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	/**
	 * @param valueDate
	 *            the valueDate to set
	 */
	public void setValueDate(String valueDate) {
		this.valueDate = valueDate;
	}

	/**
	 * @param currency
	 *            the currency to set
	 */
	public void setCurrency(String currency) {
		this.currency = currency;
	}

	/**
	 * @param processingDate
	 *            the processingDate to set
	 */
	public void setProcessingDate(String processingDate) {
		this.processingDate = processingDate;
	}

	/**
	 * @param valuationDate
	 *            the valuationDate to set
	 */
	public void setValuationDate(String valuationDate) {
		this.valuationDate = valuationDate;
	}

	/**
	 * @return the sourceSystem
	 */
	public String getBOSourceSystem() {
		return this.boSourceSystem;
	}

	/**
	 * @param sourceSystem
	 *            the sourceSystem to set
	 */
	public void setBOSourceSystem(String sourceSystem) {
		this.boSourceSystem = sourceSystem;
	}

	/**
	 * @return the simulated
	 */
	public boolean isSimulated() {
		return this.simulated;
	}

	/**
	 * @param simulated
	 *            the simulated to set
	 */
	public void setSimulated(boolean simulated) {
		this.simulated = simulated;
	}

	/**
	 * @return the simulated
	 */
	public boolean isPhoenix() {
		return this.isPhoenix;
	}

	/**
	 * @param simulated
	 *            the simulated to set
	 */
	public void setPhoenix(boolean isPhoenix) {
		this.isPhoenix = isPhoenix;
	}
	
	public String getFOExternalReference() {
		return foExternalReference;
	}

	public void setFOExternalReference(String foExternalReference) {
		this.foExternalReference = foExternalReference;
	}

	public String getFOSourceSystem() {
		return foSourceSystem;
	}

	public void setFOSourceSystem(String foSourceSystem) {
		this.foSourceSystem = foSourceSystem;
	}

	public String getProductTypeMapped() {
		return productTypeMapped;
	}

	public void setProductTypeMapped(String productTypeMapped) {
		this.productTypeMapped = productTypeMapped;
	}

	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public String getTipology() {
		return tipology;
	}

	public void setTipology(String tipology) {
		this.tipology = tipology;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
