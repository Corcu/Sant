/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.riskparameters;

import calypsox.tk.util.riskparameters.SantRiskParameter;

import com.calypso.tk.core.JDate;

public class SantRiskParameterWrapper {

	private int contractId;
	private String contractName;
	private final String cpOwner;
	private String contractCcy;

	private String currentThresholdRiskLevel1;
	private String currentThresholdRiskLevel2;
	private String currentThresholdRiskLevel3;
	private String currentThresholdType;
	private String currentThreshold;
	private String currentThresholdCurrency;
	private String currentMTARiskLevel1;
	private String currentMTARiskLevel2;
	private String currentMTARiskLevel3;
	private String currentMTAType;
	private String currentMTA;
	private String currentMTACurrency;
	private String currentRounding;

	private String previousThresholdRiskLevel1;
	private String previousThresholdRiskLevel2;
	private String previousThresholdRiskLevel3;
	private String previousThresholdType;
	private String previousThreshold;
	private String previousThresholdCurrency;
	private String previousMTARiskLevel1;
	private String previousMTARiskLevel2;
	private String previousMTARiskLevel3;
	private String previousMTAType;
	private String previousMTA;
	private String previousMTACurrency;
	private String previousRounding;

	private final JDate currentDate;
	private final JDate previousDate;

	private SantRiskParameter current;
	private SantRiskParameter previous;

	public SantRiskParameterWrapper(JDate currentDate, JDate previousDate, String cpOwner) {
		this.currentDate = currentDate;
		this.previousDate = previousDate;
		this.cpOwner = cpOwner;
	}

	public Object getPrevious() {
		return this.previous;
	}

	public Object getCurrent() {
		return this.current;
	}

	public void setCurrent(SantRiskParameter rp) {
		this.current = rp;
	}

	public void setPrevious(SantRiskParameter rp) {
		this.previous = rp;

	}

	public void build() {
		int contractId = this.current.getContractId() != 0 ? this.current.getContractId() : this.previous
				.getContractId();

		String agreementName = this.current.getCollateralAgreement() != null ? this.current.getCollateralAgreement()
				: this.previous.getCollateralAgreement() != null ? this.previous.getCollateralAgreement() : null;

		String agreementCcy = this.current.getCurrencyAgreement() != null ? this.current.getCurrencyAgreement()
				: this.previous.getCurrencyAgreement() != null ? this.previous.getCurrencyAgreement() : null;

		this.contractId = contractId;
		this.contractName = agreementName;
		this.contractCcy = agreementCcy;

		if ("Owner".equals(this.cpOwner)) {
			buildOwner();
		} else {
			buildCpty();
		}
	}

	private void buildOwner() {

		if (this.current != null) {
			this.currentThresholdRiskLevel1 = this.current.getPoThresholdRiskLevel1();
			this.currentThresholdRiskLevel2 = this.current.getPoThresholdRiskLevel2();
			this.currentThresholdRiskLevel3 = this.current.getPoThresholdRiskLevel3();
			this.currentThresholdType = this.current.getPoThresholdType();
			this.currentThreshold = this.current.getPoThreshold();
			this.currentThresholdCurrency = this.current.getPoThresholdCurrency();
			this.currentRounding = this.current.getPoRounding();

			this.currentMTARiskLevel1 = this.current.getPoMTARiskLevel1();
			this.currentMTARiskLevel2 = this.current.getPoMTARiskLevel2();
			this.currentMTARiskLevel3 = this.current.getPoMTARiskLevel3();
			this.currentMTAType = this.current.getPoMTAType();
			this.currentMTA = this.current.getPoMTA();
			this.currentMTACurrency = this.current.getPoMTACurrency();
			this.currentRounding = this.current.getPoRounding();
		}
		if (this.previous != null) {
			this.previousThresholdRiskLevel1 = this.previous.getPoThresholdRiskLevel1();
			this.previousThresholdRiskLevel2 = this.previous.getPoThresholdRiskLevel2();
			this.previousThresholdRiskLevel3 = this.previous.getPoThresholdRiskLevel3();
			this.previousThresholdType = this.previous.getPoThresholdType();
			this.previousThreshold = this.previous.getPoThreshold();
			this.previousThresholdCurrency = this.previous.getPoThresholdCurrency();
			this.previousRounding = this.previous.getPoRounding();

			this.previousMTARiskLevel1 = this.previous.getPoMTARiskLevel1();
			this.previousMTARiskLevel2 = this.previous.getPoMTARiskLevel2();
			this.previousMTARiskLevel3 = this.previous.getPoMTARiskLevel3();
			this.previousMTAType = this.previous.getPoMTAType();
			this.previousMTA = this.previous.getPoMTA();
			this.previousMTACurrency = this.previous.getPoMTACurrency();
			this.previousRounding = this.previous.getPoRounding();
		}

	}

	private void buildCpty() {
		if (this.current != null) {
			this.currentThresholdRiskLevel1 = this.current.getCptyThresholdRiskLevel1();
			this.currentThresholdRiskLevel2 = this.current.getCptyThresholdRiskLevel2();
			this.currentThresholdRiskLevel3 = this.current.getCptyThresholdRiskLevel3();
			this.currentThresholdType = this.current.getCptyThresholdType();
			this.currentThreshold = this.current.getCptyThreshold();
			this.currentThresholdCurrency = this.current.getCptyThresholdCurrency();
			this.currentRounding = this.current.getCptyRounding();

			this.currentMTARiskLevel1 = this.current.getCptyMTARiskLevel1();
			this.currentMTARiskLevel2 = this.current.getCptyMTARiskLevel2();
			this.currentMTARiskLevel3 = this.current.getCptyMTARiskLevel3();
			this.currentMTAType = this.current.getCptyMTAType();
			this.currentMTA = this.current.getCptyMTA();
			this.currentMTACurrency = this.current.getCptyMTACurrency();
			this.currentRounding = this.current.getCptyRounding();
		}
		if (this.previous != null) {
			this.previousThresholdRiskLevel1 = this.previous.getCptyThresholdRiskLevel1();
			this.previousThresholdRiskLevel2 = this.previous.getCptyThresholdRiskLevel2();
			this.previousThresholdRiskLevel3 = this.previous.getCptyThresholdRiskLevel3();
			this.previousThresholdType = this.previous.getCptyThresholdType();
			this.previousThreshold = this.previous.getCptyThreshold();
			this.previousThresholdCurrency = this.previous.getCptyThresholdCurrency();
			this.previousRounding = this.previous.getCptyRounding();

			this.previousMTARiskLevel1 = this.previous.getCptyMTARiskLevel1();
			this.previousMTARiskLevel2 = this.previous.getCptyMTARiskLevel2();
			this.previousMTARiskLevel3 = this.previous.getCptyMTARiskLevel3();
			this.previousMTAType = this.previous.getCptyMTAType();
			this.previousMTA = this.previous.getCptyMTA();
			this.previousMTACurrency = this.previous.getCptyMTACurrency();
			this.previousRounding = this.previous.getCptyRounding();
		}

	}

	public int getContractId() {
		return this.contractId;
	}

	public String getContractName() {
		return this.contractName;
	}

	public String getCpOwner() {
		return this.cpOwner;
	}

	public String getContractCcy() {
		return this.contractCcy;
	}

	public String getCurrentThresholdRiskLevel1() {
		return this.currentThresholdRiskLevel1;
	}

	public String getCurrentThresholdRiskLevel2() {
		return this.currentThresholdRiskLevel2;
	}

	public String getCurrentThresholdRiskLevel3() {
		return this.currentThresholdRiskLevel3;
	}

	public String getCurrentThresholdType() {
		return this.currentThresholdType;
	}

	public String getCurrentThreshold() {
		return this.currentThreshold;
	}

	public String getCurrentThresholdCurrency() {
		return this.currentThresholdCurrency;
	}

	public String getCurrentMTARiskLevel1() {
		return this.currentMTARiskLevel1;
	}

	public String getCurrentMTARiskLevel2() {
		return this.currentMTARiskLevel2;
	}

	public String getCurrentMTARiskLevel3() {
		return this.currentMTARiskLevel3;
	}

	public String getCurrentMTAType() {
		return this.currentMTAType;
	}

	public String getCurrentMTA() {
		return this.currentMTA;
	}

	public String getCurrentMTACurrency() {
		return this.currentMTACurrency;
	}

	public String getCurrentRounding() {
		return this.currentRounding;
	}

	public String getPreviousThresholdRiskLevel1() {
		return this.previousThresholdRiskLevel1;
	}

	public String getPreviousThresholdRiskLevel2() {
		return this.previousThresholdRiskLevel2;
	}

	public String getPreviousThresholdRiskLevel3() {
		return this.previousThresholdRiskLevel3;
	}

	public String getPreviousThresholdType() {
		return this.previousThresholdType;
	}

	public String getPreviousThreshold() {
		return this.previousThreshold;
	}

	public String getPreviousThresholdCurrency() {
		return this.previousThresholdCurrency;
	}

	public String getPreviousMTARiskLevel1() {
		return this.previousMTARiskLevel1;
	}

	public String getPreviousMTARiskLevel2() {
		return this.previousMTARiskLevel2;
	}

	public String getPreviousMTARiskLevel3() {
		return this.previousMTARiskLevel3;
	}

	public String getPreviousMTAType() {
		return this.previousMTAType;
	}

	public String getPreviousMTA() {
		return this.previousMTA;
	}

	public String getPreviousMTACurrency() {
		return this.previousMTACurrency;
	}

	public String getPreviousRounding() {
		return this.previousRounding;
	}

	public JDate getCurrentDate() {
		return this.currentDate;
	}

	public JDate getPreviousDate() {
		return this.previousDate;
	}

	public JDate getReportDate() {
		return JDate.getNow();
	}

}
