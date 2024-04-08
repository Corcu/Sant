package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class MergedReportStyle extends ReportStyle {

	public static final String ROW_NUMBER = "ROW_NUMBER";

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		ArrayList<ReportRow> rows = row.getProperty(ReportRow.DEFAULT);

		if(ROW_NUMBER.equals(columnName)) {
			return row.getProperty(MergedReport.ROW_NUMBER);
		}

		HashMap<String, String> columnMap = (HashMap<String, String>)row.getProperty(MergedReport.REVERSE_COLUMN_MAP);

		String columnN = null;
		if(columnMap!=null)
			columnN = columnMap.get(columnName);


		if(columnN ==null)
			columnN = columnName;

		for(ReportRow aggRow : rows) {
			ReportStyle reportStyle = aggRow.getProperty(MergedReport.ROW_REPORT_STYLE);
			Object result = reportStyle.getColumnValue(aggRow, columnN, errors);
			if(result!=null)
				return result;
		}


		return null;
	}

}
