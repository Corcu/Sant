package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.collateral.service.RemoteSantReportingService;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

public class SantTradesPotentialMtmErrorReport extends SantReport {

	private static final long serialVersionUID = -5170664608446611447L;
	public static final String TEMPLATE_PROPERTY_PRODUCT_TYPE = "PRODUCT_TYPE";

	private Collection<SantTradesPotentialMtmErrorItem> items = null;//Sonar

	@Override
	protected boolean checkProcessEndDate() {
		return false;
	}

	@Override
	protected ReportOutput loadReport(Vector<String> errorMsgs) {
		DefaultReportOutput reportOutput = null;
		try {
			List<SantTradesPotentialMtmErrorItem> loadItems = loadItems(errorMsgs);
			Collection<ReportRow> rows = generateReportRows(loadItems);

			ReportRow[] rowsArray = new ReportRow[rows.size()];
			rowsArray = rows.toArray(rowsArray);
			reportOutput = new DefaultReportOutput(this);
			reportOutput.setRows(rowsArray);

		} catch (Exception e) {
			Log.error("Could not get info from database", e.getCause());
			Log.error(this, e);//Sonar
		}

		return reportOutput;
	}

	private List<SantTradesPotentialMtmErrorItem> loadItems(Vector<String> errors) throws Exception {
		@SuppressWarnings("unused")
		List<SantTradesPotentialMtmErrorItem> items = new ArrayList<SantTradesPotentialMtmErrorItem>();

		JDate valDate = getProcessStartDate().addBusinessDays(-1, getReportTemplate().getHolidays());
		JDate previousDate = valDate.addBusinessDays(-1, getReportTemplate().getHolidays());

		String agreementIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);

		if (!Util.isEmpty(agreementIds)) {
			Vector<Integer> agreementIdsV = Util.string2IntVector(agreementIds);
			if (agreementIdsV.size() >= 1000) {
				errors.add("Select less than 1000 contracts");
				return null;
			}
		}

		// GSM 05/08/15. SBNA Multi-PO filter
		final String agrOwnerIds = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());
		// String agrOwnerIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
		if (!Util.isEmpty(agrOwnerIds)) {
			Vector<Integer> agrOwnerIdsV = Util.string2IntVector(agrOwnerIds);
			if (agrOwnerIdsV.size() >= 1000) {
				errors.add("Select less than 1000 Owner(AGR)");
				return null;
			}
		}

		String cptyIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.COUNTERPARTY);
		if (!Util.isEmpty(cptyIds)) {
			Vector<Integer> cptyIdsV = Util.string2IntVector(cptyIds);
			if (cptyIdsV.size() >= 1000) {
				errors.add("Select less than 1000 CounterParty.");
				return null;
			}
		}

		String productType = (String) getReportTemplate().get(TEMPLATE_PROPERTY_PRODUCT_TYPE);
		String agreementType = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);

		String sql = "select pl1.trade_id, internal_reference, (select keyword_value from trade_keyword where trade_keyword.trade_id=t.trade_id and keyword_name='BO_REFERENCE') bo_ref,"
				+ " external_reference num_front_id, mrgcall_config.description contracto, "
				+ "(select short_name from legal_entity le where le.LEGAL_ENTITY_ID=mrgcall_config.process_org_id) owner, pd.product_type, pd.product_sub_type, pd.MATURITY_DATE, "
				+ "(select keyword_value from trade_keyword where trade_keyword.trade_id=t.trade_id and keyword_name='STRUCTURE_ID') STRUCTURE_ID, mark_value "
				+ "from pl_mark pl1, pl_mark_value mv1, trade t, product_desc pd, mrgcall_config ";

		String where = "where pl1.mark_id=mv1.mark_id and mv1.mark_name='NPV' AND mv1.mark_value is not null and mv1.mark_value<>0 "
				+ " and pl1.pricing_env_name='DirtyPrice' and trunc(pl1.valuation_date)="
				+ Util.date2SQLString(previousDate)
				+ " AND pl1.trade_id=t.trade_id and t.product_id=pd.product_id and pd.product_sub_type not in ('CONTRACT_IA','DISPUTE_ADJUSTMENT')"
				+ " and t.internal_reference=mrgcall_config.mrg_call_def and t.trade_status not in ('CANCELED', 'MATURED') ";

		if (!Util.isEmpty(agreementIds)) {
			where = where + " AND mrgcall_config.mrg_call_def in (" + agreementIds + ") ";
		}
		if (!Util.isEmpty(agreementType)) {
			where = where + " and mrgcall_config.contract_type=" + Util.string2SQLString(agreementType);
		}

		if (!Util.isEmpty(agrOwnerIds)) {
			Vector<String> agrOwnerIdsVect = Util.string2Vector(agrOwnerIds);
			where = where
					+ " AND (mrgcall_config.process_org_id in "
					+ Util.collectionToSQLString(agrOwnerIdsVect)
					+ " OR (mrg_call_def IN (SELECT mcc_id FROM mrgcall_config_le WHERE le_role = 'ProcessingOrg' AND le_id IN "
					+ Util.collectionToSQLString(agrOwnerIdsVect) + " ))  ) ";
		}

		if (!Util.isEmpty(productType) && !productType.equals("ALL")) {
			where = where + " and pd.PRODUCT_TYPE=" + Util.string2SQLString(productType);
		}
		if (!Util.isEmpty(cptyIds)) {
			where = where + " and t.cpty_id in (" + cptyIds + ") ";
		}

		where = where
				+ " and (pd.maturity_date is null or pd.maturity_date >= "
				+ Util.date2SQLString(valDate)
				+ ") "
				+ " AND not exists (select 1 from pl_mark pl2, pl_mark_value mv2 "
				+ "where pl2.mark_id=mv2.mark_id and mv2.mark_name='NPV' and pl2.pricing_env_name='DirtyPrice' and trunc(pl2.valuation_date)="
				+ Util.date2SQLString(valDate) + " AND (nvl(mv2.mark_value,0)<>0) " + "and pl2.trade_id=t.trade_id ) ";

		sql = sql + where;

		RemoteSantReportingService santReportingService = SantReportingUtil.getSantReportingService(DSConnection
				.getDefault());
		return santReportingService.getPotentialMtmErrorItems(sql);
	}

	/**
	 * Creates a report row for every item tobe shown in the report.
	 * 
	 * @param items
	 *            The collection of items to be shown
	 * @return A collection of report rows.
	 */
	private Collection<ReportRow> generateReportRows(Collection<SantTradesPotentialMtmErrorItem> items) {
		Collection<ReportRow> rows = new ArrayList<ReportRow>();

		for (SantTradesPotentialMtmErrorItem item : items) {
			ReportRow row = new ReportRow(item);
			row.setProperty(SantTradesPotentialMtmErrorReportStyle.PROCESS_DATE, getProcessStartDate());
			rows.add(row);
		}

		return rows;
	}

}