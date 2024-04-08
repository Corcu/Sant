/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallDetailEntry;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallDetailEntryBuilder;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

public class SantHedgeFundLoader extends SantAbstractLoader {

	public List<SantMarginCallDetailEntry> loadActivity(final ReportTemplate template, final JDate valDate)
			throws Exception {
		String isFundValue = (String) template.get(SantGenericTradeReportTemplate.IS_FUND);
		if (Util.isEmpty(isFundValue)) {
			// SantMarginCallDetailEntriesLoader detailEntriesloader = new
			// SantMarginCallDetailEntriesLoader();
			// return detailEntriesloader.load(template, valDate);
			throw new Exception("PLease select a valid fund value");
		}

		Map<Integer, CollateralConfig> filteredContracts = new HashMap<Integer, CollateralConfig>();
		List<MarginCallEntryDTO> entries = load(template, valDate, isFundValue, filteredContracts);
		final SantMarginCallDetailEntryBuilder builder = new SantMarginCallDetailEntryBuilder();
		builder.build(entries, filteredContracts, template);

		return builder.getSantDetails();

	}

	public List<SantMarginCallEntry> loadExposure(final ReportTemplate template, final JDate valDate) throws Exception {
		String isFundValue = (String) template.get(SantGenericTradeReportTemplate.IS_FUND);
		if (Util.isEmpty(isFundValue)) {
			// SantMarginCallEntriesLoader entriesloader = new
			// SantMarginCallEntriesLoader();
			// return entriesloader.load(template, valDate);
			throw new Exception("PLease select a valid fund value");
		}

		Map<Integer, CollateralConfig> filteredContracts = new HashMap<Integer, CollateralConfig>();
		List<MarginCallEntryDTO> entries = load(template, valDate, isFundValue, filteredContracts);

		final List<SantMarginCallEntry> santEntries = new ArrayList<SantMarginCallEntry>();
		for (final MarginCallEntryDTO entry : entries) {
			final SantMarginCallEntry santEntry = new SantMarginCallEntry(entry);
			santEntry.setMarginCallConfig(filteredContracts.get(entry.getCollateralConfigId()));
			santEntries.add(santEntry);
		}

		return santEntries;

	}

	// @SuppressWarnings("unchecked")
	private List<MarginCallEntryDTO> load(final ReportTemplate template, final JDate valDate, String isFundValue,
			Map<Integer, CollateralConfig> filteredContracts) throws Exception {

		Collection<CollateralConfig> allContracts = null;
		try {

			final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
			allContracts = srvReg.getCollateralDataServer().getAllMarginCallConfig();

			// GSM: Not working fine since new Core 1.5.6 - deprecated
			// allContracts =
			// DSConnection.getDefault().getRemoteReferenceData().getAllMarginCallConfig(0,
			// 0);

		} catch (Exception e) {
			Log.error(this, "Cannot load CONTRACTS", e);
			return new ArrayList<MarginCallEntryDTO>();
		}

		Set<Integer> marginCallConfigIds = new HashSet<Integer>();

		for (CollateralConfig mcc : allContracts) {

			// Fund SI or NO
			if (!isFundValue.equals(mcc.getAdditionalField("HEDGE_FUNDS_REPORT"))) {
				continue;
			}

			if ("CLOSE".equals(mcc.getAgreementStatus())) {
				continue;
			}

			// Valuation agent
			final Integer valAgentId = (Integer) template.get(SantGenericTradeReportTemplate.VALUATION_AGENT);
			if ((valAgentId != null) && (valAgentId > 0)) {
				if (valAgentId != mcc.getValuationAgentId()) {
					continue;
				}
			}

			// Agreement type
			final String agrType = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
			if (!Util.isEmpty(agrType)) {
				if (!agrType.equals(mcc.getContractType())) {
					continue;
				}
			}

			// Agreement Ids
			final String agreementIds = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
			if (!Util.isEmpty(agreementIds)) {
				final Vector<Integer> agrIds = Util.string2IntVector(agreementIds);
				if (!agrIds.contains(mcc.getId())) {
					continue;
				}
			}

			// PO Owner
			// final String poAgrStr = (String)
			// template.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
			// 03/08/15. SBNA Multi-PO filter
			final String poAgrStr = CollateralUtilities.filterPoIdsByTemplate(template);

			if (!Util.isEmpty(poAgrStr)) {
				Vector<Integer> poAgrIds = Util.string2IntVector(poAgrStr);
				boolean isFound = false;
				for (Integer poId : getProcessingOrgIds(mcc)) {
					if (poAgrIds.contains(poId)) {
						isFound = true;
						break;
					}
				}
				if (!isFound) {
					continue;
				}
			}

			marginCallConfigIds.add(mcc.getId());
			filteredContracts.put(mcc.getId(), mcc);

		}

		if (marginCallConfigIds.isEmpty()) {
			return new ArrayList<MarginCallEntryDTO>();
		}

		return loadEntries(template, valDate, marginCallConfigIds);

	}

	private List<MarginCallEntryDTO> loadEntries(final ReportTemplate template, final JDate valDate,
			Set<Integer> marginCallConfigIds) throws Exception {

		final List<String> from = new ArrayList<String>();
		final StringBuilder sqlWhere = new StringBuilder();

		buildMarginCallEntriesSQLQuery(template, valDate, marginCallConfigIds, from, sqlWhere);

		return SantReportingUtil.getSantReportingService(DSConnection.getDefault())
				.getMarginCallEntriesDTO(sqlWhere.toString(), from, true);

	}

	protected void buildMarginCallEntriesSQLQuery(ReportTemplate template, JDate valDate,
			Set<Integer> marginCallConfigIds, List<String> from, StringBuilder sqlWhere) {

		from.add(" margin_call_entries ");

		// process start date
		final JDate processStartDate = getDate(template, valDate, TradeReportTemplate.START_DATE,
				TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);
		// Check that using this we obtain the entries for the day before.
		if (processStartDate != null) {
			sqlWhere.append("  TRUNC(margin_call_entries.process_datetime) >= ");
			sqlWhere.append(Util.date2SQLString(processStartDate));
		}

		// process end date
		final JDate processEndtDate = getDate(template, valDate, TradeReportTemplate.END_DATE,
				TradeReportTemplate.END_PLUS, TradeReportTemplate.END_TENOR);
		if (processEndtDate != null) {
			sqlWhere.append(" AND TRUNC(margin_call_entries.process_datetime) <= ");
			sqlWhere.append(Util.date2SQLString(processEndtDate));
		}

		// Agreement status
		final String agrStatus = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_STATUS);
		if (!Util.isEmpty(agrStatus)) {
			final Vector<String> agrStatusV = Util.string2Vector(agrStatus);
			sqlWhere.append(" AND margin_call_entries.status in ");
			sqlWhere.append(Util.collectionToSQLString(agrStatusV));
		}

		// Contracts
		List<Integer> contractIds = new ArrayList<Integer>(marginCallConfigIds);
		List<List<Integer>> contractIdsSubList = new ArrayList<List<Integer>>();

		final int SQL_IN_ITEM_COUNT = 999;
		int start = 0;

		for (int i = 0; i <= (contractIds.size() / SQL_IN_ITEM_COUNT); i++) {
			int end = (i + 1) * SQL_IN_ITEM_COUNT;
			if (end > contractIds.size()) {
				end = contractIds.size();
			}
			final List<Integer> subList = contractIds.subList(start, end);

			start = end;
			contractIdsSubList.add(subList);
		}

		sqlWhere.append(" AND margin_call_entries.mcc_id IN (");
		sqlWhere.append(Util.collectionToString(contractIdsSubList.get(0)));
		sqlWhere.append(")");

		if (contractIdsSubList.size() > 1) {
			for (int i = 1; i < contractIdsSubList.size(); i++) {
				sqlWhere.append(" OR margin_call_entries.mcc_id IN (");
				sqlWhere.append(Util.collectionToString(contractIdsSubList.get(i)));
				sqlWhere.append(")");
			}
		}
		//AAP V14 CHANGE
		//sqlWhere.append(" ORDER BY margin_call_entries.process_datetime ASC");
	}

	@SuppressWarnings("rawtypes")
	private List<Integer> getProcessingOrgIds(CollateralConfig config) {
		List<Integer> ids = new ArrayList<>();
		Vector chlidrens = config.getChildren();
		ids.add(config.getPoId());
		if (!Util.isEmpty(chlidrens)) {
			for (int i = 0; i < config.getChildren().size(); i++) {
				CollateralConfig child = (CollateralConfig) chlidrens.get(i);

				ids.add(child.getPoId());
			}
		}
		return ids;
	}

}
