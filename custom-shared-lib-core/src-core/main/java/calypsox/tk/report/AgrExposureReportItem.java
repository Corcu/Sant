/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.core.JDate;

/**
 * Encapsulates the info for reports Agreement Exposure by CounterParty and Instrument.
 * 
 * @author Soma
 * 
 */
public class AgrExposureReportItem {

	private JDate ExposureDateCurrent;
	private JDate ExposureDatePrev;
	private String instrument;
	private String contractType;
	private String counterParty;
	private String agreementCurrency;

	private String agreementName;

	private int tradeCountCurrent;
	private int tradeCountPrev;

	private double exposureCurrent;
	private double exposurePrev;

	public String getCounterParty() {
		return this.counterParty;
	}

	public void setCounterParty(String counterParty) {
		this.counterParty = counterParty;
	}

	public String getAgreementCurrency() {
		return this.agreementCurrency;
	}

	public void setAgreementCurrency(String agreementCurrency) {
		this.agreementCurrency = agreementCurrency;
	}

	public JDate getExposureDateCurrent() {
		return this.ExposureDateCurrent;
	}

	public void setExposureDateCurrent(JDate exposureDateCurrent) {
		this.ExposureDateCurrent = exposureDateCurrent;
	}

	public JDate getExposureDatePrev() {
		return this.ExposureDatePrev;
	}

	public void setExposureDatePrev(JDate exposureDatePrev) {
		this.ExposureDatePrev = exposureDatePrev;
	}

	public String getInstrument() {
		return this.instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public String getContractType() {
		return this.contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}

	public String getAgreementName() {
		return this.agreementName;
	}

	public void setAgreementName(String agreement) {
		this.agreementName = agreement;
	}

	public int getTradeCountCurrent() {
		return this.tradeCountCurrent;
	}

	public int getTradeCountPrev() {
		return this.tradeCountPrev;
	}

	public double getExposureCurrent() {
		return this.exposureCurrent;
	}

	public double getExposurePrev() {
		return this.exposurePrev;
	}

	public void addExposureCurrent(double exp) {
		if (!Double.isNaN(exp)) {
			this.exposureCurrent += exp;
		}
		this.tradeCountCurrent++;
	}

	public void addExposurePrev(double exp) {
		if (!Double.isNaN(exp)) {
			this.exposurePrev += exp;
		}
		this.tradeCountPrev++;
	}

}
