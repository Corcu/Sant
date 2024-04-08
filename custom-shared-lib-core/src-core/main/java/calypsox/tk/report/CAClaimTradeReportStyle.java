package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;

public class CAClaimTradeReportStyle extends TradeReportStyle {
	private static final long serialVersionUID = -3736956143510496475L;

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
		Object ret = super.getColumnValue(row, columnId, errors);
		if (ret != null && columnId.equals("TradeStatus") || columnId.equals("Status")) {
			return ret.toString();
		}
		return ret;
	}

}
