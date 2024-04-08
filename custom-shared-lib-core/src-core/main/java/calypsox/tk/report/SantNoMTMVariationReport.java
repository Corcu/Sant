package calypsox.tk.report;

import static com.calypso.tk.core.PricerMeasure.S_NPV;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.collateral.service.RemoteSantReportingService;
import calypsox.util.SantReportingUtil;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantNoMTMVariationReport extends Report {

	private static final long serialVersionUID = 1L;

	public static final String SANT_NOMTM_VARIATION_REPORT = "SantNoMTMVariationReport";
	public static final String SANT_NOMTM_VARIATION_ITEM = "SantNoMTMVariationItem";

	@SuppressWarnings({ "unused", "unchecked" })
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") Vector errorMsgsP) {

		if (this._reportTemplate == null) {
			return null;
		}
		Vector<String> errorMsgs = errorMsgsP;

		JDate valDate = getValDate();

		JDate prevDate = valDate.addBusinessDays(-1, this._reportTemplate.getHolidays());
		String sql = buildQuery(valDate, prevDate);

		ArrayList<SantNoMTMVariationItem> noMTMVariationItems = new ArrayList<SantNoMTMVariationItem>();
		try {
			RemoteSantReportingService santReportingService = SantReportingUtil.getSantReportingService(DSConnection
					.getDefault());
			noMTMVariationItems = santReportingService.getNoMTMVariationItems(sql);
		} catch (RemoteException e) {
			Log.error(SantNoMTMVariationReport.class, "Error loading MTM Audit Items", e);
		}

		// Get All user Info and create UserAuditItem objects
		ArrayList<SantMTMAuditItem> mtmAuditItems = new ArrayList<SantMTMAuditItem>();

		ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		for (SantNoMTMVariationItem item : noMTMVariationItems) {
			if (item.getMarkName().equals(S_NPV)) {
				ReportRow row = new ReportRow(item);
				row.setProperty(ReportRow.DEFAULT, item.getTrade());
				reportRows.add(row);
			}
		}

		// DefaultReportOutput output = new DefaultReportOutput(this);
		StandardReportOutput output = new StandardReportOutput(this);
		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
		return output;
	}

	private String buildQuery(JDate valDate, JDate prevDate) {
		/*
		 * Below is the Query we need to run to get the data back select trade.trade_id, product_desc.maturity_date,
		 * pl_mark_value.* from trade, product_desc, pl_mark, pl_mark_value, pl_mark pl_mark_prev, pl_mark_value
		 * pl_mark_value_prev where ( product_desc.MATURITY_DATE is null or
		 * product_desc.MATURITY_DATE>=to_date('28-12-2011','DD-MM-YYYY') ) AND trade.product_id=product_desc.product_id
		 * AND trade.trade_id in (31930, 31931) AND trade.trade_id=pl_mark.TRADE_ID AND
		 * pl_mark.MARK_ID=pl_mark_value.MARK_ID and pl_mark_value.MARK_NAME='NPV' AND
		 * pl_mark.valuation_date=to_date('27-12-2011','DD-MM-YYYY') AND trade.trade_id=pl_mark_prev.TRADE_ID AND
		 * pl_mark_prev.MARK_ID=pl_mark_value_prev.MARK_ID and pl_mark_value_prev.MARK_NAME='NPV' AND
		 * pl_mark_prev.valuation_date=to_date('28-12-2011','DD-MM-YYYY') AND
		 * pl_mark_value.mark_value=pl_mark_value_prev.mark_value
		 */

		String sql = "select trade.trade_id, pl_mark_value.mark_name, pl_mark_value.currency,  pl_mark_value.mark_value  "
				+ " from trade, product_desc, pl_mark, pl_mark_value, pl_mark pl_mark_prev, pl_mark_value pl_mark_value_prev "
				+ " where "
				+ " ( product_desc.MATURITY_DATE is null or product_desc.MATURITY_DATE>="
				+ Util.date2SQLString(valDate)
				+ ") AND trade.product_id=product_desc.product_id "
				// + " AND trade.trade_id in (31930, 31931) "
				+ " AND trade.trade_id=pl_mark.TRADE_ID AND pl_mark.MARK_ID=pl_mark_value.MARK_ID and pl_mark_value.MARK_NAME='NPV' "
				+ " AND pl_mark.valuation_date="
				+ Util.date2SQLString(valDate)
				+ " AND trade.trade_id=pl_mark_prev.TRADE_ID AND pl_mark_prev.MARK_ID=pl_mark_value_prev.MARK_ID and pl_mark_value_prev.MARK_NAME='NPV' "
				+ " AND pl_mark_prev.valuation_date="
				+ Util.date2SQLString(prevDate)
				+ " AND pl_mark_value.mark_value=pl_mark_value_prev.mark_value";

		return sql;
	}

	public String formatDate(JDatetime dateTime) {
		if (dateTime == null) {
			return null;
		}
		DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("CET"));
		return formatter.format(dateTime);
	}

}