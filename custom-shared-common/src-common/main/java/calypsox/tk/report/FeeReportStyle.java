package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.core.Amount;
import com.calypso.tk.report.ReportRow;

import calypsox.util.FormatUtil;

/**
 * @author x327391
 */
public class FeeReportStyle extends com.calypso.tk.report.FeeReportStyle {

	private static final long serialVersionUID = 1L;
	public static final String SOURCE_SYSTEM = "Mx3EU Source System";
	public static final String DUCO_FEE_AMOUNT = "DUCO Fee Amount";

	@Override
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
		if (SOURCE_SYSTEM.equals(columnId)) {
			return "Mx3EU";
		} else if (DUCO_FEE_AMOUNT.equals(columnId)) {
			Amount feeAmount = (Amount) super.getColumnValue(row, "Fee Amount", errors);
			Double feeAmountD = 0d;
			if (feeAmount != null) {
				feeAmountD = feeAmount.get();
			}
			return FormatUtil.formatAmount(feeAmountD, 2);

		}
		return super.getColumnValue(row, columnId, errors);
	}

}
