package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import calypsox.tk.report.generic.loader.SantHedgeFundLoader;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;

import com.calypso.tk.core.Log;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

public class SantHedgeFundExposureAndCollateralReport extends SantReport {

	private static final long serialVersionUID = 1L;

	@Override
	public ReportOutput loadReport(final Vector<String> errorMsgs) {
		try {
			return getReportOutput();
		} catch (final Exception e) {
			Log.error(this, "Cannot load MarginCallEntry", e);
			errorMsgs.add(e.getMessage());
		}

		return null;
	}

	private ReportOutput getReportOutput() throws Exception {

		DefaultReportOutput output = new DefaultReportOutput(this);
		SantHedgeFundLoader loader = new SantHedgeFundLoader();

		Collection<SantMarginCallEntry> entries = loader.loadExposure(getReportTemplate(), getValDate());

		List<ReportRow> rows = new ArrayList<ReportRow>();
		for (SantMarginCallEntry entry : entries) {
			entry.setReportDate(getValDate());
			ReportRow row = new ReportRow(entry, "SantMarginCallEntry");
			rows.add(row);
		}
		output.setRows(rows.toArray(new ReportRow[rows.size()]));
		return output;
	}
}
