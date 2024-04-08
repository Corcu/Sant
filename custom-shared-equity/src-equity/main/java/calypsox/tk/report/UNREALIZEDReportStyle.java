package calypsox.tk.report;

import com.calypso.tk.report.AccountEnrichmentReportStyle;
import com.calypso.tk.report.ReportRow;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Vector;


public class UNREALIZEDReportStyle extends AccountEnrichmentReportStyle {


	private static final long serialVersionUID = 1L;

	public static final String ORIGIN = "ORIGIN";


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		if (columnName.equals(ORIGIN)) {
			return "800018693";
		}

		return ((HashMap<String, Object>) row.getProperty(ReportRow.DEFAULT)).get(columnName);
	}

}
