/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader.margincall;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import calypsox.tk.core.SantPricerMeasure;

import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.refdata.CollateralConfig;

public class SantMarginCallEntry {

	private final MarginCallEntryDTO entry;

	private double sumIndepAmountBase = 0;

	private double sumNPVBase = 0;

	private int nbTrades = 0;

	private CollateralConfig marginCallConfig;

	private JDate reportDate;

	private final Set<String> allocationsCurrencies = new TreeSet<String>();

	private final List<MarginCallDetailEntryDTO> excludedDetailEntries = new ArrayList<MarginCallDetailEntryDTO>();

	private final List<MarginCallDetailEntryDTO> includedDetailEntries = new ArrayList<MarginCallDetailEntryDTO>();

	public SantMarginCallEntry(final MarginCallEntryDTO entry) {
		this.entry = entry;
		compute();
	}

	public MarginCallEntryDTO getEntry() {
		return this.entry;
	}

	public int getNbTrades() {
		return this.nbTrades;
	}

	public double getSumIndepAmountBase() {
		return this.sumIndepAmountBase;
	}

	public double getSumNPVBase() {
		return this.sumNPVBase;
	}

	// Not Settled COLLATERAL
	public double getCollateralInTransitBase() {
		return getCashInTransitBase() + getSecurityInTransitBase();
	}

	// Not Settled CASH
	public double getCashInTransitBase() {
		return this.entry.getPreviousCashMargin() - this.entry.getPreviousActualCashMargin();
	}

	// Not Settled SECURITY
	public double getSecurityInTransitBase() {
		return this.entry.getPreviousSecurityMargin() - this.entry.getPreviousActualSecurityMargin();
	}

	public void setMarginCallConfig(final CollateralConfig marginCallConfig) {
		this.marginCallConfig = marginCallConfig;
	}

	public CollateralConfig getMarginCallConfig() {
		return this.marginCallConfig;
	}

	public JDate getReportDate() {
		return this.reportDate;
	}

	public void setReportDate(final JDate reportDate) {
		this.reportDate = reportDate;
	}

	public List<MarginCallDetailEntryDTO> getExcludedDetailEntries() {
		return this.excludedDetailEntries;
	}

	public List<MarginCallDetailEntryDTO> getIncludedDetailEntries() {
		return this.includedDetailEntries;
	}

	public Set<String> getAllocationsCurrencies() {
		return this.allocationsCurrencies;
	}

	public void addAllocationCurrency(final String currency) {
		this.allocationsCurrencies.add(currency);
	}

	public void compute() {
		computeDetails();
	}

	private void computeDetails() {
		boolean isContractIA = false;
		boolean isDisputeAdj = false;
		for (final MarginCallDetailEntryDTO detail : this.entry.getDetailEntries()) {
			if (detail.isExcluded()) {
				this.excludedDetailEntries.add(detail);
				continue;
			}
			isContractIA = (detail.getDescription() != null)
					&& detail.getDescription().startsWith("CollateralExposureCONTRACT_IA");
			isDisputeAdj = (detail.getDescription() != null)
					&& detail.getDescription().startsWith("CollateralExposureDISPUTE_ADJUSTMENT");

			// 1- We don't want to show CONTRACT_IA & DISPUTE_ADJUSTEMENT
			if (!isContractIA && !isDisputeAdj) {
				this.nbTrades++;
				this.includedDetailEntries.add(detail);
			}

			// 2- For NPV & INDEP AMOUNT calculation we need CONTRACT_IA only (exclude NPV of DISPUTE_ADJUSTEMENT)
			// DISPUTE_ADJUSTEMENT NPV but no INDEP AMOUNT
			// CONTRACT_IA INDEP AMOUNT but no NPV

			if (isDisputeAdj) {
				continue;
			}

			// Indep Amount Base ccy
			final PricerMeasure indepAmountBase = detail.getMeasure(SantPricerMeasure
					.toString(SantPricerMeasure.INDEPENDENT_AMOUNT_BASE));

			double indepAmountBaseCcy = 0;
			if ((indepAmountBase != null) && !Double.isNaN(indepAmountBase.getValue())) {
				indepAmountBaseCcy = indepAmountBase.getValue();
			}
			this.sumIndepAmountBase += indepAmountBaseCcy;

			// NPV base ccy
			final PricerMeasure npvBase = detail.getMeasure(SantPricerMeasure.toString(SantPricerMeasure.NPV_BASE));
			double npvBaseCcy = 0;
			if ((npvBase != null) && !Double.isNaN(npvBase.getValue())) {
				npvBaseCcy = npvBase.getValue();
			}

			this.sumNPVBase += npvBaseCcy;
		}
	}

}
