package calypsox.tk.report;

import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.TradeReport;

import java.util.Vector;

/**
 * @author dmenendd
 */
public class FXFOBOOperReport extends TradeReport {

    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ReportOutput load(Vector errorMsgs) {

        final DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
        if(output==null) {
            return null;
        }
        return output;
    }

}
