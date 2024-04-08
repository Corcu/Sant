package calypsox.tk.report;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.util.Vector;

public class ConcatenatedReportStyle extends ReportStyle {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
			ReportStyle reportStyle = row.getProperty(ConcatenatedReport.ROW_REPORT_STYLE);
		return reportStyle.getColumnValue(row, columnName, errors);
	}

}
