package calypsox.tk.report;

import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author dmenendd
 */
public class BondFOBOOperReport extends TradeReport {

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
