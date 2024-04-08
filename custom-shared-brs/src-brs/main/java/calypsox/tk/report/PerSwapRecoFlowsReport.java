package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.CashFlowReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;

public class PerSwapRecoFlowsReport extends CashFlowReport {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput load(Vector errorMsgs) {
		DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);
		
		return output;
	}

}
