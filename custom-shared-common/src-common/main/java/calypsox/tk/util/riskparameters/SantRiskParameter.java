/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.riskparameters;

import com.calypso.tk.core.JDate;

import java.io.Serializable;

public class SantRiskParameter implements Serializable {

	private static final long serialVersionUID = 6899075141909290997L;

	private int contractId;
	private String collateralAgreement;
	private String currencyAgreement;
	private JDate valDate;

	/****** Processing Org ******/
	// Threshold
	private String poThresholdRiskLevel1;
	private String poThresholdRiskLevel2;
	private String poThresholdRiskLevel3;
	private String poThresholdType;
	private String poThreshold;
	private String poThresholdCurrency;

	// MTA
	private String poMTARiskLevel1;
	private String poMTARiskLevel2;
	private String poMTARiskLevel3;
	private String poMTAType;
	private String poMTA;
	private String poMTACurrency;

	// Rounding
	private String poRounding;

	/****** Counterparty ******/
	// Threshold
	private String cptyThresholdRiskLevel1;
	private String cptyThresholdRiskLevel2;
	private String cptyThresholdRiskLevel3;
	private String cptyThresholdType;
	private String cptyThreshold;
	private String cptyThresholdCurrency;

	// MTA
	private String cptyMTARiskLevel1;
	private String cptyMTARiskLevel2;
	private String cptyMTARiskLevel3;
	private String cptyMTAType;
	private String cptyMTA;
	private String cptyMTACurrency;

	// Rounding
	private String cptyRounding;

	public int getContractId() {
		return this.contractId;
	}

	public void setContractId(int contractId) {
		this.contractId = contractId;
	}

	public String getCollateralAgreement() {
		return this.collateralAgreement;
	}

	public void setCollateralAgreement(String collateralAgreement) {
		this.collateralAgreement = collateralAgreement;
	}

	public String getCurrencyAgreement() {
		return this.currencyAgreement;
	}

	public void setCurrencyAgreement(String currencyAgreement) {
		this.currencyAgreement = currencyAgreement;
	}

	public JDate getValDate() {
		return this.valDate;
	}

	public void setValDate(JDate valDate) {
		this.valDate = valDate;
	}

	public String getPoThresholdRiskLevel1() {
		return this.poThresholdRiskLevel1;
	}

	public void setPoThresholdRiskLevel1(String poThresholdRiskLevel1) {
		this.poThresholdRiskLevel1 = poThresholdRiskLevel1;
	}

	public String getPoThresholdRiskLevel2() {
		return this.poThresholdRiskLevel2;
	}

	public void setPoThresholdRiskLevel2(String poThresholdRiskLevel2) {
		this.poThresholdRiskLevel2 = poThresholdRiskLevel2;
	}

	public String getPoThresholdRiskLevel3() {
		return this.poThresholdRiskLevel3;
	}

	public void setPoThresholdRiskLevel3(String poThresholdRiskLevel3) {
		this.poThresholdRiskLevel3 = poThresholdRiskLevel3;
	}

	public String getPoThresholdType() {
		return this.poThresholdType;
	}

	public void setPoThresholdType(String poThresholdType) {
		this.poThresholdType = poThresholdType;
	}

	public String getPoThreshold() {
		return this.poThreshold;
	}

	public void setPoThreshold(String poThreshold) {
		this.poThreshold = poThreshold;
	}

	public String getPoThresholdCurrency() {
		return this.poThresholdCurrency;
	}

	public void setPoThresholdCurrency(String poThresholdCurrency) {
		this.poThresholdCurrency = poThresholdCurrency;
	}

	public String getPoMTARiskLevel1() {
		return this.poMTARiskLevel1;
	}

	public void setPoMTARiskLevel1(String poMTARiskLevel1) {
		this.poMTARiskLevel1 = poMTARiskLevel1;
	}

	public String getPoMTARiskLevel2() {
		return this.poMTARiskLevel2;
	}

	public void setPoMTARiskLevel2(String poMTARiskLevel2) {
		this.poMTARiskLevel2 = poMTARiskLevel2;
	}

	public String getPoMTARiskLevel3() {
		return this.poMTARiskLevel3;
	}

	public void setPoMTARiskLevel3(String poMTARiskLevel3) {
		this.poMTARiskLevel3 = poMTARiskLevel3;
	}

	public String getPoMTAType() {
		return this.poMTAType;
	}

	public void setPoMTAType(String poMTAType) {
		this.poMTAType = poMTAType;
	}

	public String getPoMTA() {
		return this.poMTA;
	}

	public void setPoMTA(String poMTA) {
		this.poMTA = poMTA;
	}

	public String getPoMTACurrency() {
		return this.poMTACurrency;
	}

	public void setPoMTACurrency(String poMTACurrency) {
		this.poMTACurrency = poMTACurrency;
	}

	public String getPoRounding() {
		return this.poRounding;
	}

	public void setPoRounding(String poRounding) {
		this.poRounding = poRounding;
	}

	public String getCptyThresholdRiskLevel1() {
		return this.cptyThresholdRiskLevel1;
	}

	public void setCptyThresholdRiskLevel1(String cptyThresholdRiskLevel1) {
		this.cptyThresholdRiskLevel1 = cptyThresholdRiskLevel1;
	}

	public String getCptyThresholdRiskLevel2() {
		return this.cptyThresholdRiskLevel2;
	}

	public void setCptyThresholdRiskLevel2(String cptyThresholdRiskLevel2) {
		this.cptyThresholdRiskLevel2 = cptyThresholdRiskLevel2;
	}

	public String getCptyThresholdRiskLevel3() {
		return this.cptyThresholdRiskLevel3;
	}

	public void setCptyThresholdRiskLevel3(String cptyThresholdRiskLevel3) {
		this.cptyThresholdRiskLevel3 = cptyThresholdRiskLevel3;
	}

	public String getCptyThresholdType() {
		return this.cptyThresholdType;
	}

	public void setCptyThresholdType(String cptyThresholdType) {
		this.cptyThresholdType = cptyThresholdType;
	}

	public String getCptyThreshold() {
		return this.cptyThreshold;
	}

	public void setCptyThreshold(String cptyThreshold) {
		this.cptyThreshold = cptyThreshold;
	}

	public String getCptyThresholdCurrency() {
		return this.cptyThresholdCurrency;
	}

	public void setCptyThresholdCurrency(String cptyThresholdCurrency) {
		this.cptyThresholdCurrency = cptyThresholdCurrency;
	}

	public String getCptyMTARiskLevel1() {
		return this.cptyMTARiskLevel1;
	}

	public void setCptyMTARiskLevel1(String cptyMTARiskLevel1) {
		this.cptyMTARiskLevel1 = cptyMTARiskLevel1;
	}

	public String getCptyMTARiskLevel2() {
		return this.cptyMTARiskLevel2;
	}

	public void setCptyMTARiskLevel2(String cptyMTARiskLevel2) {
		this.cptyMTARiskLevel2 = cptyMTARiskLevel2;
	}

	public String getCptyMTARiskLevel3() {
		return this.cptyMTARiskLevel3;
	}

	public void setCptyMTARiskLevel3(String cptyMTARiskLevel3) {
		this.cptyMTARiskLevel3 = cptyMTARiskLevel3;
	}

	public String getCptyMTAType() {
		return this.cptyMTAType;
	}

	public void setCptyMTAType(String cptyMTAType) {
		this.cptyMTAType = cptyMTAType;
	}

	public String getCptyMTA() {
		return this.cptyMTA;
	}

	public void setCptyMTA(String cptyMTA) {
		this.cptyMTA = cptyMTA;
	}

	public String getCptyMTACurrency() {
		return this.cptyMTACurrency;
	}

	public void setCptyMTACurrency(String cptyMTACurrency) {
		this.cptyMTACurrency = cptyMTACurrency;
	}

	public String getCptyRounding() {
		return this.cptyRounding;
	}

	public void setCptyRounding(String cptyRounding) {
		this.cptyRounding = cptyRounding;
	}

}
