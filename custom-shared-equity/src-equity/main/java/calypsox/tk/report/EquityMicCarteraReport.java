package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;

public class EquityMicCarteraReport extends EquityMisPlusCarteraReport {


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
    @SuppressWarnings("rawtypes")
	@Override
    public ReportOutput load(Vector errorMsgs) {
    	StandardReportOutput output = new StandardReportOutput(this);
    	DefaultReportOutput coreOutput = (DefaultReportOutput)super.load(errorMsgs);
    	output.setRows(coreOutput.getRows());
    	return output;
    }


}