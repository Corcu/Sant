/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.kpiwatchlist;

import static calypsox.tk.core.CollateralStaticAttributes.MC_ENTRY_PO_MTM;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.SantKPIWatchListReportStyle;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.tk.report.kpidailytask.SantFrequencyHelper;
import calypsox.tk.report.quotes.FXQuoteHelper;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;

public class SantKPIWatchListItem {

	private final Map<String, Object> columnMap = new HashMap<String, Object>();

	private JDate reportDate;
	private String owner;
	private String id;
	private String description;
	private String type;
	private String frequency;
	private String baseCurrency;
	private double marginCall;
	private double mtmEUR;
	private double independent;
	private double threshold;
	private double mta;
	private double balance;
	private int unpaidDays;
	private String marginCallSituation;

	private final SantFrequencyHelper freqHelper;

	public SantKPIWatchListItem(SantMarginCallEntry santEntry, PricingEnv env, Vector<String> holidays,
			SantFrequencyHelper freqHelper) {
		this.freqHelper = freqHelper;
		FXQuoteHelper.setPricingEnv(env);
		build(santEntry, holidays);
		buildMap();
	}

	private void build(SantMarginCallEntry santEntry, Vector<String> holidays) {
		MarginCallEntryDTO entryDTO = santEntry.getEntry();
		CollateralConfig contract = santEntry.getMarginCallConfig();

		this.reportDate = entryDTO.getProcessDatetime().getJDate(TimeZone.getDefault());
		this.owner = contract.getProcessingOrg().getCode();
		this.id = getContractNameWithoutPrefix(contract.getName());
		this.description = contract.getLegalEntity().getName();
		this.type = contract.getContractType();
		this.frequency = this.freqHelper.getFrequency(contract.getAdditionalField("FREQUENCY"));
		this.baseCurrency = contract.getCurrency();
		this.marginCall = getAmountInEUR(entryDTO.getGlobalRequiredMargin(), entryDTO.getContractCurrency());
		if (entryDTO.getAttribute(MC_ENTRY_PO_MTM) != null) {
			this.mtmEUR = getAmountInEUR((Double) entryDTO.getAttribute(MC_ENTRY_PO_MTM),
					entryDTO.getContractCurrency());
		} else {
			this.mtmEUR = getAmountInEUR(entryDTO.getNetBalance(), entryDTO.getContractCurrency());
		}
		this.independent = getAmountInEUR(entryDTO.getIndependentAmount(), entryDTO.getContractCurrency());
		this.threshold = getAmountInEUR(Math.abs(entryDTO.getThresholdAmount()), entryDTO.getContractCurrency());
		this.balance = getAmountInEUR(entryDTO.getPreviousTotalMargin(), entryDTO.getContractCurrency());
		this.mta = getAmountInEUR(entryDTO.getMTAAmount(), entryDTO.getContractCurrency());

		JDate delinquent = CollateralUtilities.getEntryAttributeAsJDate(entryDTO,
				CollateralStaticAttributes.DELINQUENT_SINCE);

		JDate currentDay = this.reportDate;
		while (currentDay.gte(delinquent)) {
			currentDay = currentDay.addBusinessDays(-1, holidays);
			this.unpaidDays++;
		}
		this.marginCallSituation = "PRICING".contains(entryDTO.getStatus()) ? "Pending" : "Valid";
	}

	private String getContractNameWithoutPrefix(String contractName) {
		if (contractName.startsWith("OSLA -")) {
			return contractName.substring("OSLA -".length()).trim();
		}
		if (contractName.startsWith("ISMA -")) {
			return contractName.substring("ISMA -".length()).trim();
		}
		if (contractName.startsWith("CSA -")) {
			return contractName.substring("CSA -".length()).trim();
		}
		return contractName;
	}

	private double getAmountInEUR(Double amountToConvert, String fromCCY) {
		if (amountToConvert == null) {
			return Double.NaN;
		}
		if ("EUR".equals(fromCCY)) {
			return amountToConvert;
		}
		Double amountEUR = null;
		try {
			amountEUR = FXQuoteHelper.convertAmountInEUR(amountToConvert, fromCCY);
		} catch (MarketDataException e) {
			Log.error(this, "Cannot retrieve FX quote", e);
		}
		if (amountEUR == null) {
			return Double.NaN;
		}
		return amountEUR;
	}

	public Object getColumnValue(String columnName) {
		return this.columnMap.get(columnName);
	}

	private void buildMap() {
		this.columnMap.put(SantKPIWatchListReportStyle.REPORT_DATE, this.reportDate);
		this.columnMap.put(SantKPIWatchListReportStyle.OWNER, this.owner);
		this.columnMap.put(SantKPIWatchListReportStyle.ID, this.id);
		this.columnMap.put(SantKPIWatchListReportStyle.DESCRIPTION, this.description);
		this.columnMap.put(SantKPIWatchListReportStyle.TYPE, this.type);
		this.columnMap.put(SantKPIWatchListReportStyle.FREQUENCY, this.frequency);
		this.columnMap.put(SantKPIWatchListReportStyle.MARGIN_CALL, format(this.marginCall));
		this.columnMap.put(SantKPIWatchListReportStyle.BASE_CURRENCY, this.baseCurrency);
		this.columnMap.put(SantKPIWatchListReportStyle.MTM_EUR, format(this.mtmEUR));
		this.columnMap.put(SantKPIWatchListReportStyle.INDEPENDENT, format(this.independent));
		this.columnMap.put(SantKPIWatchListReportStyle.THRESHOLD, format(this.threshold));
		this.columnMap.put(SantKPIWatchListReportStyle.MTA, format(this.mta));
		this.columnMap.put(SantKPIWatchListReportStyle.BALANCE, format(this.balance));
		this.columnMap.put(SantKPIWatchListReportStyle.UNPAID_DAYS, this.unpaidDays);
		this.columnMap.put(SantKPIWatchListReportStyle.STATUS, this.marginCallSituation);

	}

	private Object format(Object value) {
		if (value instanceof Double) {
			return new Amount((Double) value, 2);
		}
		return value;
	}

}
