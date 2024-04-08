/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.riskvalues;

public class RiskValuesItem {

	private String contractName;
	private String LEFullName;
	private String moodyMCrating;
	private String snpMCrating;
	private String fitchMCrating;

	private double deliveryRoundingPO;
	private double returnRoundingLE;

	private String deliveryMCRatingMTA; // DeliveryMTA
	private String returnMCRatingMTA; // ReturnMTA

	private String MCRatingThreshold;
	private String independentAmountPO;
	private String independentAmountCPTY;

	private String MCRatingCcy;
	private boolean isActive;
	private boolean isActiveMTA;

	private int contractId;

	public String getContractName() {
		return this.contractName;
	}

	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	public String getLEFullName() {
		return this.LEFullName;
	}

	public void setLEFullName(String lEFullName) {
		this.LEFullName = lEFullName;
	}

	public String getMoodyMCrating() {
		return this.moodyMCrating;
	}

	public void setMoodyMCrating(String moodyMCrating) {
		this.moodyMCrating = moodyMCrating;
	}

	public String getSnpMCrating() {
		return this.snpMCrating;
	}

	public void setSnpMCrating(String snpMCrating) {
		this.snpMCrating = snpMCrating;
	}

	public String getFitchMCrating() {
		return this.fitchMCrating;
	}

	public void setFitchMCrating(String fitchMCrating) {
		this.fitchMCrating = fitchMCrating;
	}

	public double getDeliveryRoundingPO() {
		return this.deliveryRoundingPO;
	}

	public void setDeliveryRoundingPO(double deliveryRoundingPO) {
		this.deliveryRoundingPO = deliveryRoundingPO;
	}

	public double getReturnRoundingLE() {
		return this.returnRoundingLE;
	}

	public void setReturnRoundingLE(double returnRoundingLE) {
		this.returnRoundingLE = returnRoundingLE;
	}

	public String getDeliveryMCRatingMTA() {
		return this.deliveryMCRatingMTA;
	}

	public void setDeliveryMCRatingMTA(String deliveryMCRatingMTA) {
		this.deliveryMCRatingMTA = deliveryMCRatingMTA;
	}

	public String getReturnMCRatingMTA() {
		return this.returnMCRatingMTA;
	}

	public void setReturnMCRatingMTA(String returnMCRatingMTA) {
		this.returnMCRatingMTA = returnMCRatingMTA;
	}

	public String getMCRatingThreshold() {
		return this.MCRatingThreshold;
	}

	public void setMCRatingThreshold(String mCRatingThreshold) {
		this.MCRatingThreshold = mCRatingThreshold;
	}

	public String getIndependentAmountPO() {
		return this.independentAmountPO;
	}

	public void setIndependentAmountPO(String independentAmountPO) {
		this.independentAmountPO = independentAmountPO;
	}

	public String getIndependentAmountCPTY() {
		return this.independentAmountCPTY;
	}

	public void setIndependentAmountCPTY(String independentAmountCPTY) {
		this.independentAmountCPTY = independentAmountCPTY;
	}

	public int getContractId() {
		return this.contractId;
	}

	public void setContractId(int contractId) {
		this.contractId = contractId;
	}

	public String getMCRatingCcy() {
		return this.MCRatingCcy;
	}

	public void setMCRatingCcy(String mCRatingCcy) {
		this.MCRatingCcy = mCRatingCcy;
	}

	public boolean isActive() {
		return this.isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean isActiveMTA() {
		return this.isActiveMTA;
	}

	public void setActiveMTA(boolean isActive) {
		this.isActiveMTA = isActive;
	}
}
