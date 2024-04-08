/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.io.Serializable;

public class KPIMtmReportItem implements Serializable {

	// START OA 28/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = -4870965087099621663L;
	// END OA OA 28/11/2013

	private String agrOwner;
	private String dealOwner;
	private int tradeCount;

	private String economicSector;
	private String instrument;
	private String portfolio;

	private double usdMTMSum;
	private double eurMTMSum;

	private int agreementId;
	private String agreementName;

	public int getAgreementId() {
		return this.agreementId;
	}

	public void setAgreementId(int agreementId) {
		this.agreementId = agreementId;
	}

	public String getAgreementName() {
		return this.agreementName;
	}

	public void setAgreementName(String agreementName) {
		this.agreementName = agreementName;
	}

	public String getAgrOwner() {
		return this.agrOwner;
	}

	public void setAgrOwner(final String agrOwner) {
		this.agrOwner = agrOwner;
	}

	public String getDealOwner() {
		return this.dealOwner;
	}

	public void setDealOwner(final String dealOwner) {
		this.dealOwner = dealOwner;
	}

	public int getTradeCount() {
		return this.tradeCount;
	}

	public void setTradeCount(final int tradeCount) {
		this.tradeCount = tradeCount;
	}

	public String getEconomicSector() {
		return this.economicSector;
	}

	public void setEconomicSector(final String economicSector) {
		this.economicSector = economicSector;
	}

	public String getInstrument() {
		return this.instrument;
	}

	public void setInstrument(final String instrument) {
		this.instrument = instrument;
	}

	public String getPortfolio() {
		return this.portfolio;
	}

	public void setPortfolio(final String portfolio) {
		this.portfolio = portfolio;
	}

	public double getUsdMTMSum() {
		return this.usdMTMSum;
	}

	public void setUsdMTMSum(final double usdMTMSum) {
		this.usdMTMSum = usdMTMSum;
	}

	public double getEurMTMSum() {
		return this.eurMTMSum;
	}

	public void setEurMTMSum(final double eurMTMSum) {
		this.eurMTMSum = eurMTMSum;
	}

}
