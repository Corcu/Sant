package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class KPIMarginCallReportStyle extends ReportStyle {
	private static final long serialVersionUID = 1L;
	public static final String DATE = "Date";
	public static final String NAME = "Name";
	public static final String VALUE = "Value";

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {
		return row.getProperty(columnName);
	}

}
