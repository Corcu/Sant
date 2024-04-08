package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReport;
import com.calypso.tk.report.CollateralConfigReportTemplate;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

/**
 * Report class for the sanatander margin call configuration report
 * 
 * @author aela
 * 
 */
@SuppressWarnings("rawtypes")
public class SantMCConfigReport extends SantReport {

	public static final String TYPE = "SantMCConfig";
	private final Map<Integer, CollateralConfig> contracts = new HashMap<Integer, CollateralConfig>();
	private final List<String> MARGIN_CALL_NOT_CALCULTAED_STATUS = new ArrayList<String>();

	@Override
	public ReportOutput loadReport(final Vector arg0) {

		try {
			return getReportOutput();

		} catch (final Exception e) {
			Log.error(this, "Cannot load contracts", e);
		}

		return null;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.tk.report.MarginCallConfigReport#load(java.util.Vector)
	 */
	public ReportOutput getReportOutput() {

		initNotCalculatedStatus();
		DefaultReportOutput output = new DefaultReportOutput(this);
		CollateralConfigReport mccreport = new CollateralConfigReport();
		// GSM: 28/06/2013 - deprecated new core.
		// MarginCallConfigReport mccreport = new MarginCallConfigReport();
		SantMCConfigReportTemplate customTemplate = (SantMCConfigReportTemplate) getReportTemplate();
		mccreport.setReportTemplate(buildReportTemplate(customTemplate));

		// 03/08/15. SBNA Multi-PO filter. Report filter only allows ONE PO, added by code for multiple
		Set<String> posIdsAllowed = new HashSet<String>(Util.string2Vector(CollateralUtilities
				.filterPoIdsByTemplate(customTemplate)));

		Vector errorMsgs = new Vector();

		ReportOutput out = mccreport.load(errorMsgs);
		ReportRow[] rows = ((DefaultReportOutput) out).getRows();
		StringBuffer contractsId = new StringBuffer("");
		this.contracts.clear();

		for (ReportRow row : rows) {

			CollateralConfig mcc = (CollateralConfig) row.getProperty(ReportRow.DEFAULT);

			// 03/08/15. SBNA Multi-PO filter
			if (mcc == null) {
				continue;
			}

			if (CollateralUtilities.filterOwners(posIdsAllowed, mcc)) {
				continue;
			}

			if (contractsId.length() == 0) {
				contractsId.append(mcc.getId());
			} else {
				contractsId.append(", " + mcc.getId());
			}
			this.contracts.put(mcc.getId(), mcc);
		}
		output.setRows(getReportOutput(customTemplate, contractsId.toString()));
		return output;
	}

	/**
	 * @param customTemplate
	 * @return
	 */
	private CollateralConfigReportTemplate buildReportTemplate(SantMCConfigReportTemplate customTemplate) {

		String agrType = (String) customTemplate.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
		if (Util.isEmpty(agrType)) {
			agrType = "ALL";
		}
		String counterparty = (String) customTemplate.get(SantGenericTradeReportTemplate.COUNTERPARTY);
		if (Util.isEmpty(counterparty)) {
			counterparty = "ALL";
		} else {
			try {
				int poId = Integer.parseInt(counterparty);
				LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), poId);
				if (le != null) {
					counterparty = le.getCode();
				} else {
					counterparty = "ALL";
				}
			} catch (Exception e) {
				Log.info(this, e); //sonar
				counterparty = "ALL";
			}
		}
		String po = (String) customTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
		if (Util.isEmpty(po)) {
			po = "ALL";
		} else {
			try {
				int poId = Integer.parseInt(po);
				LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), poId);
				if (le != null) {
					po = le.getCode();
				} else {
					po = "ALL";
				}
			} catch (Exception e) {
				Log.info(this, e); //sonar
				po = "ALL";
			}
		}
		JDate processStartDate = getDate(customTemplate, JDate.getNow(), TradeReportTemplate.START_DATE,
				TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

		if (processStartDate == null) {
			processStartDate = JDate.getNow();
		}

		customTemplate.put(SantMCConfigReportTemplate.EXTRACTION_DATE, processStartDate);
		customTemplate.put(CollateralConfigReportTemplate.PROCESSING_ORG, po);
		customTemplate.put(CollateralConfigReportTemplate.ROLE, "ALL");

		customTemplate.put(CollateralConfigReportTemplate.LEGAL_ENTITY, counterparty);
		customTemplate.put(CollateralConfigReportTemplate.CONTRACT_TYPE, agrType);
		customTemplate.put(CollateralConfigReportTemplate.STATUS, "OPEN");
		customTemplate.put(CollateralConfigReportTemplate.DISCOUNT_CURRENCY, "ALL");

		return customTemplate;
	}

	/**
	 * @param customTempalte
	 * @param contractIds
	 * @return
	 */
	private ReportRow[] getReportOutput(SantMCConfigReportTemplate customTempalte, String contractIds) {
		StringBuffer whereClause = new StringBuffer("");
		StringBuffer fromClause = new StringBuffer("");

		if (Util.isEmpty(contractIds)) {
			return null;
		}

		JDate processStartDate = getDate(customTempalte, JDate.getNow(), TradeReportTemplate.START_DATE,
				TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

		if (processStartDate == null) {
			processStartDate = JDate.getNow();
		}

		JDate processEndDate = getDate(customTempalte, JDate.getNow(), TradeReportTemplate.END_DATE,
				TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

		if (processEndDate == null) {
			processEndDate = JDate.getNow();
		}

		if (processStartDate.after(processEndDate)) {
			processEndDate = processStartDate;
		}

		// SELECT mrg_call_def, process_date
		fromClause.append(" mrgcall_config");
		fromClause.append(" ,");
		fromClause.append(" ( SELECT   " + Util.date2SQLString(processStartDate) + " + LEVEL - 1 AS process_date ");
		fromClause.append(" FROM DUAL");
		fromClause.append(" CONNECT BY LEVEL <= " + (JDate.diff(processStartDate, processEndDate) + 1) + ") dates ");

		whereClause.append(" NOT EXISTS (");
		whereClause.append(" SELECT 1");
		whereClause.append(" FROM margin_call_entries");
		whereClause.append(" WHERE mcc_id = mrg_call_def");
		whereClause.append(" AND dates.process_date = process_date ");
		whereClause.append(" AND margin_call_entries.STATUS NOT IN  "
				+ Util.collectionToSQLString(this.MARGIN_CALL_NOT_CALCULTAED_STATUS) + " ");
		whereClause.append(") ");

		final Vector<String> ids = Util.string2Vector(contractIds);

		if (ids.size() < ioSQL.MAX_ITEMS_IN_LIST) {
			whereClause.append(" AND mrgcall_config.mrg_call_def IN (").append(Util.collectionToString(ids))
					.append(")");
		} else {
			@SuppressWarnings("unchecked")
			final List<String> idsStrList = ioSQL.returnStringsOfStrings(ids);
			whereClause.append(" AND mrgcall_config.contract_type <> 'CSA_FACADE' ");
			whereClause.append(" AND (mrgcall_config.mrg_call_def IN (").append(idsStrList.get(0)).append(")");
			for (int i = 1; i < idsStrList.size(); i++) {
				whereClause.append(" OR mrgcall_config.mrg_call_def IN (").append(idsStrList.get(i)).append(")");
			}
			whereClause.append(") ");
		}
		List<Map<Integer, JDate>> result = null;
		try {
			result = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
					.getNotCalculatedMarginCallConfigs(fromClause.toString(), whereClause.toString());
		} catch (RemoteException e) {
			Log.error(this, e);
			result = null;
		}

		ArrayList<ReportRow> notCalculatedMcc = new ArrayList<ReportRow>();
		Vector holidaysList = getReportTemplate().getHolidays();
		if ((result != null) && (result.size() > 0)) {
			for (Map<Integer, JDate> element : result) {
				for (Integer id : element.keySet()) {
					JDate date = element.get(id);
					if (Holiday.getCurrent().isBusinessDay(date, holidaysList)) {
						CollateralConfig mcc = this.contracts.get(id);
						ReportRow row = new ReportRow(mcc);
						row.setProperty(SantMCConfigReportTemplate.EXTRACTION_DATE, element.get(id));
						// GSM: 28/06/2013 - new core fix.
						row.setProperty("MarginCallConfig", mcc);

						notCalculatedMcc.add(row);
					}
				}
			}
		}
		ReportRow[] finalRows = new ReportRow[notCalculatedMcc.size()];
		return notCalculatedMcc.toArray(finalRows);
	}

	private void initNotCalculatedStatus() {
		if ((this.MARGIN_CALL_NOT_CALCULTAED_STATUS != null) && (this.MARGIN_CALL_NOT_CALCULTAED_STATUS.size() > 0)) {
			return;
		}
		Vector notCalculatedStatusDomain = LocalCache.getDomainValues(DSConnection.getDefault(),
				"MarginCallNotCalculatedStatus");
		if ((notCalculatedStatusDomain != null) && (notCalculatedStatusDomain.size() > 0)) {
			for (int i = 0; i < notCalculatedStatusDomain.size(); i++) {
				String domainValue = (String) notCalculatedStatusDomain.get(i);
				if (domainValue != null) {
					this.MARGIN_CALL_NOT_CALCULTAED_STATUS.add(domainValue);
				}
			}
		}
	}
}
