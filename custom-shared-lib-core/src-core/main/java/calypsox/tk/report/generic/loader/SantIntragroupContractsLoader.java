/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import calypsox.tk.report.SantIntragroupPortfolioBreakdownReportTemplate;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import calypsox.util.collateral.CollateralManagerUtil;

import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

public class SantIntragroupContractsLoader extends SantEnableThread<Integer, CollateralConfig> {

	private final String agreementIds;

	public SantIntragroupContractsLoader(boolean enableThreading, final ReportTemplate template,
			final String agreementIds) {
		super(template, enableThreading);
		this.agreementIds = agreementIds;
	}

	@Override
	protected Map<Integer, CollateralConfig> getDataMapFromDataList() {
		for (CollateralConfig mcc : this.dataList) {
			this.dataMap.put(mcc.getId(), mcc);
		}
		return this.dataMap;
	}

	@Override
	protected void loadData() {

		Integer valAgentId = (Integer) this.template.get(SantGenericTradeReportTemplate.VALUATION_AGENT);
		if (valAgentId == null) {
			valAgentId = 0;
		}

		final String agrType = (String) this.template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);

		// 03/08/15. SBNA Multi-PO filter
		// final String poAgrStr = (String) this.template.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
		final String poAgrStr = CollateralUtilities.filterPoIdsByTemplate(this.template);
		Vector<Integer> poAgrIdsInt = null;

		if (!Util.isEmpty(poAgrStr)) {
			poAgrIdsInt = Util.string2IntVector(poAgrStr);
		}

		// intragroup cpty id
		final String intraLeStr = (String) this.template
				.get(SantIntragroupPortfolioBreakdownReportTemplate.INTRAGROUP_LE_ID);
		Vector<Integer> intraLeIds = null;
		if (!Util.isEmpty(intraLeStr)) {
			intraLeIds = Util.string2IntVector(intraLeStr);
		}

		Collection<CollateralConfig> marginCallConfigs = new Vector<CollateralConfig>();

		if (!(Util.isEmpty(this.agreementIds))) {
			try {
				marginCallConfigs = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
						.getMarginCallConfigByIds(Util.string2IntVector(this.agreementIds)).values();
			} catch (final Exception e) {
				Log.error(this, "Cannot retrieve Margin Call Contracts", e);
			}

		} else {
			// get contracts based on po list and intragroup cpty list
			MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
			mcFilter.setProcessingOrgIds(poAgrIdsInt);
			mcFilter.setLegalEntityIds(intraLeIds);

			try {
				marginCallConfigs = CollateralManagerUtil.loadCollateralConfigs(mcFilter);
			} catch (RemoteException e1) {
				Log.error(this, "Cannot retrieve Margin Call Contracts", e1);
			}
		}

		// Filter out contracts not matched for criteria
		if (marginCallConfigs.size() > 0) {
			for (final CollateralConfig marginCallConfig : marginCallConfigs) {
				if (valAgentId > 0) {
					if (marginCallConfig.getValuationAgentId() != valAgentId) {
						continue;
					}
				}
				if (!Util.isEmpty(agrType) && !marginCallConfig.getContractType().equals(agrType)) {
					continue;
				}
				// Add to Map if not in the map already
				if (this.dataMap.get(marginCallConfig.getId()) == null) {
					this.dataMap.put(marginCallConfig.getId(), marginCallConfig);
				}
			}
		}
	}
}
