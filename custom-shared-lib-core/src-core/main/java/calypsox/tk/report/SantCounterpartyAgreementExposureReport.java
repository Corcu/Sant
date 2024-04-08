/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.jfree.util.Log;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.report.agrexposure.SantMCDetailEntryLight;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallDetailEntry;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

public class SantCounterpartyAgreementExposureReport extends SantReport {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 2905022293449936938L;

	public static final String TYPE = "SantCounterpartyAgreementExposure";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ReportOutput loadReport(final Vector errorMsgs) {

		final DefaultReportOutput output = new DefaultReportOutput(this);
		if (this._reportTemplate == null) {
			return null;

		}
		initDates();
		// Mature deals
		boolean matureDeals;
		if (getReportTemplate().get(SantGenericTradeReportTemplate.MATURE_DEALS).toString().equals("false")) {
			matureDeals = false;
		} else {
			matureDeals = true;
		}

		try {

			ArrayList<SantMCDetailEntryLight> detailedEntriesLight = getDetailedEntriesLight();
			Collection<AgrExposureReportItem> agrExposureItems = buildAgrExposureItemsLight(detailedEntriesLight,
					matureDeals);

			final List<ReportRow> rows = getReportRows(agrExposureItems);
			output.setRows(rows.toArray(new ReportRow[rows.size()]));
			return output;

		} catch (final Exception e) {

			String error = "Error retrieving the margin call detail entries\n";
			Log.error(this.getClass(), e);
			errorMsgs.add(error + e.getMessage());
		}
		return null;

	}

	/*
	 * A light weight DetailEntries, which replaces buildAgrExposureItems
	 * method. For now I want to keep the both methods, just in case we need to
	 * switch. -Soma
	 */
	private Collection<AgrExposureReportItem> buildAgrExposureItemsLight(ArrayList<SantMCDetailEntryLight> entries,
			boolean includeMatureDeals) {
		HashMap<String, AgrExposureReportItem> itemsMap = new HashMap<String, AgrExposureReportItem>();

		JDate currentExpDate = getEndDate();
		@SuppressWarnings("unused")
		JDate prevExpDate = getStartDate();

		for (SantMCDetailEntryLight entry : entries) {

			// IA and DISPUTE trades are excluded in the SQL, so below is not
			// required
			// boolean isContractIA = (entry.getDetailEntry().getDescription()
			// != null)
			// &&
			// entry.getDetailEntry().getDescription().startsWith("CollateralExposureCONTRACT_IA");
			// boolean isDisputeAdj = (entry.getDetailEntry().getDescription()
			// != null)
			// &&
			// entry.getDetailEntry().getDescription().startsWith("CollateralExposureDISPUTE_ADJUSTMENT");
			//
			// if (isContractIA || isDisputeAdj) {
			// continue;
			// }

			if (!includeMatureDeals && (entry.getMaturityDate() != null)
					&& entry.getMaturityDate().before(currentExpDate)) {
				continue;
			}

			String key = getKey(entry);
			AgrExposureReportItem item = itemsMap.get(key);
			if (item == null) {
				item = new AgrExposureReportItem();
				item.setAgreementName(entry.getAgreementName());
				item.setContractType(entry.getAgreementType());
				// item.setCounterParty(entry.getCounterPartyName());
				// BAU - put as counterparty ppal branch
				item.setCounterParty(CacheCollateralClient.getCollateralConfig(getDSConnection(), entry.getAgrId())
						.getLegalEntity().getCode());
				item.setAgreementCurrency(entry.getAgreementCurrency());
				item.setInstrument(entry.getInstrument());
				itemsMap.put(key, item);
			}

			JDate entryProcessDate = entry.getProcessDate();
			// BAU 5.2.0 - use margin_call instead of npv_base
			final PricerMeasure marginCall = entry.getPricerMeasure(SantPricerMeasure.MARGIN_CALL);

			if (currentExpDate.equals(entryProcessDate)) {
				if (item.getExposureDateCurrent() == null) {
					item.setExposureDateCurrent(entryProcessDate);
				}
				if (marginCall != null) {
					item.addExposureCurrent(marginCall.getValue());
				}
			} else {
				if (item.getExposureDatePrev() == null) {
					item.setExposureDatePrev(entryProcessDate);
				}
				if (marginCall != null) {
					item.addExposurePrev(marginCall.getValue());
				}
			}

		}

		return itemsMap.values();

	}

	public String getKey(SantMarginCallDetailEntry entry) {
		String key = "";
		if (entry == null) {
			return null;
		}

		key = entry.getMarginCallConfig().getContractType() + "-" + entry.getMarginCallConfig().getCurrency() + "-"
				+ entry.getTrade().getCounterParty().getName();
		return key;
	}

	// public String getKey(SantMCDetailEntryLight entry) {
	// String key = "";
	// if (entry == null) {
	// return null;
	// }
	//
	// key = entry.getAgreementCurrency() + "-" + entry.getAgreementCurrency() +
	// "-" + entry.getCounterPartyName();
	// return key;
	// }

	// BAU - use as key agreement_id to accumulate trade exposure for trades
	// belonging to same agreement
	public String getKey(SantMCDetailEntryLight entry) {
		String key = "";
		if (entry == null) {
			return null;
		}

		key = Integer.toString(entry.getAgrId());
		return key;
	}

	private List<ReportRow> getReportRows(Collection<AgrExposureReportItem> agrExposureItems) {
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
		for (AgrExposureReportItem item : agrExposureItems) {
			if ((item.getTradeCountCurrent() != 0) || (item.getTradeCountPrev() != 0)) {
				ReportRow row = new ReportRow(item);
				reportRows.add(row);
			}
		}
		return reportRows;
	}

	protected String getInstrument(Trade trade) {
		if (trade != null) {
			if (trade.getProductType().equals(Product.REPO)) {
				return CollateralStaticAttributes.INSTRUMENT_TYPE_REPO;
			} else if (trade.getProductType().equals(Product.SEC_LENDING)) {
				return CollateralStaticAttributes.INSTRUMENT_TYPE_SEC_LENDING;
			} else {
				return trade.getProductSubType();
			}
		}
		return null;
	}

	private ArrayList<SantMCDetailEntryLight> getDetailedEntriesLight() throws Exception {
		ArrayList<SantMCDetailEntryLight> detailedEntriesLight = new ArrayList<SantMCDetailEntryLight>();

		List<Integer> mcIds = getMCIds(getReportTemplate());

		// 03/08/15. SBNA Multi-PO filter
		// Vector<String> posIdsAllowed =
		// Util.string2Vector(CollateralUtilities.filterPoByTemplate(getReportTemplate()));

		if (!Util.isEmpty(mcIds)) {
			int start = 0;
			int end = 0;
			int limit = 999;

			while (start < mcIds.size()) {
				end = start + limit;
				if (end > mcIds.size()) {
					end = mcIds.size();
				}

				List<Integer> subList = mcIds.subList(start, end);
				start = end;

				String entriesSQL = "select agr.mrg_call_def, agr.DESCRIPTION, agr.CONTRACT_TYPE, agr.CURRENCY_CODE, entries.id, entries.PROCESS_DATE, entries.TRADE_DATETIME, "
						+ "(select short_name from legal_entity where legal_entity_id=cpty_id) le, "
						+ "(select short_name from legal_entity where legal_entity_id=b.legal_entity_id) po, "
						+ "decode(pd.product_type, 'SecLending', 'SEC_LENDING', 'Repo', 'REPO', product_sub_type) instrument, pd.MATURITY_DATE, det.COLLATERAL_MEASURES "
						+ "from margin_call_detail_entries det, margin_call_entries entries, mrgcall_config agr, trade t, product_desc pd, book b "
						+ "where entries.id=det.MC_ENTRY_ID and entries.mcc_id=agr.MRG_CALL_DEF AND t.trade_id=det.TRADE_ID and t.PRODUCT_ID=pd.product_id "
						+ "AND t.BOOK_ID=b.book_id and product_sub_type not in ('DISPUTE_ADJUSTMENT', 'CONTRACT_IA') "
						+ " and det.is_excluded=0 and ( trunc(entries.process_date)="
						+ Util.date2SQLString(getStartDate()) + " OR trunc(entries.process_date)="
						+ Util.date2SQLString(getEndDate()) + " ) " + " AND agr.mrg_call_def in ("
						+ Util.collectionToString(subList) + ") ";

				// 03/08/15. SBNA Multi-PO filter
				// if (!Util.isEmpty(posIdsAllowed)) {
				//
				// final String poFilter = " AND mrgcall_config.PROCESS_ORG_ID
				// IN "
				// + Util.collectionToSQLString(posIdsAllowed);
				//
				// entriesSQL += poFilter;
				// }

				ArrayList<SantMCDetailEntryLight> temp = SantReportingUtil
						.getSantReportingService(DSConnection.getDefault()).getDetailedEntriesLight(entriesSQL);
				detailedEntriesLight.addAll(temp);

			}
		}
		return detailedEntriesLight;
	}

	private List<Integer> getMCIds(ReportTemplate template) throws Exception {

		String economicSector = (String) template.get(SantGenericTradeReportTemplate.ECONOMIC_SECTOR);
		// 03/08/15. SBNA Multi-PO filter
		String mcIdsSql = buildQuery(template);

		ArrayList<Integer> configIds = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
				.getMarginCallConfigIds(mcIdsSql, economicSector);

		return configIds;
	}

	public String buildQuery(ReportTemplate template) {
		StringBuilder sqlSelect = new StringBuilder();
		StringBuilder sqlFrom = new StringBuilder();
		StringBuilder sqlWhere = new StringBuilder();

		sqlSelect.append(" select distinct mrg_call_def");
		sqlFrom.append(" from mrgcall_config, margin_call_entries ");

		sqlWhere.append("where  mrgcall_config.mrg_call_def = margin_call_entries.mcc_id ");

		// process start date
		JDate prevExpDate = getStartDate();
		JDate currentExpDate = getEndDate();

		if (prevExpDate != null) {
			sqlWhere.append(" AND (TRUNC(margin_call_entries.process_datetime) = ");
			sqlWhere.append(Util.date2SQLString(prevExpDate));
		}
		if (currentExpDate != null) {
			sqlWhere.append(" OR TRUNC(margin_call_entries.process_datetime) = ");
			sqlWhere.append(Util.date2SQLString(currentExpDate));
			sqlWhere.append(")");
		}

		// Valuation agent
		final Integer valAgentId = (Integer) template.get(SantGenericTradeReportTemplate.VALUATION_AGENT);
		if ((valAgentId != null) && (valAgentId > 0)) {
			sqlWhere.append(" AND mrgcall_config.val_agent_id = ");
			sqlWhere.append(valAgentId);
		}

		// Agreement Ids
		final String agreementIds = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
		if (!Util.isEmpty(agreementIds)) {
			final Vector<String> agrIds = Util.string2Vector(agreementIds);
			sqlWhere.append(" AND mrgcall_config.mrg_call_def in ");
			sqlWhere.append(Util.collectionToSQLString(agrIds));
		}

		// Agreement type
		final String agrType = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
		if (!Util.isEmpty(agrType)) {
			sqlWhere.append(" AND mrgcall_config.contract_type = '");
			sqlWhere.append(agrType).append("'");
		}

		// PO Owner
		// final String poAgrStr = (String)
		// template.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
		// GSM 30/07/15. SBNA Multi-PO filter
		final String poAgrStr = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());
		Vector<String> poAgrIds = null;
		if (!Util.isEmpty(poAgrStr)) {
			poAgrIds = Util.string2Vector(poAgrStr);
			sqlWhere.append(" AND mrgcall_config.process_org_id in ");
			sqlWhere.append(Util.collectionToSQLString(poAgrIds));
			// sqlWhere.append("OR (mrg_call_def IN (SELECT mcc_id FROM
			// mrgcall_config_le WHERE le_role = 'ProcessingOrg' AND le_id IN
			// ");
			// sqlWhere.append(Util.collectionToSQLString(poAgrIds));
			// sqlWhere.append(" ))");
		}

		return sqlSelect.toString() + sqlFrom.toString() + sqlWhere.toString();
	}

	// private
}