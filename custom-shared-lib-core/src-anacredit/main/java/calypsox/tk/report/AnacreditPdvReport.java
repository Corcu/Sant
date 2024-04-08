package calypsox.tk.report;

import calypsox.tk.anacredit.processor.AnacreditProcessorPdv;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.*;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class AnacreditPdvReport extends AnacreditTradeReport
            implements IAnacreditReport {

    public AnacreditPdvReport() {
        super();
    }

    private AnacreditProcessorPdv _processor = new AnacreditProcessorPdv();

    protected void processRows(DefaultReportOutput output, List<ReportRow> reportRows, Vector errorMsgs) {
      _processor.processReportRows(this, output, reportRows, errorMsgs);
    }

}
