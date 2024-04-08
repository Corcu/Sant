package calypsox.tk.report;

import calypsox.tk.anacredit.processor.AnacreditProcessorEquityBase;
import calypsox.tk.anacredit.processor.AnacreditProcessorPdv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.List;
import java.util.Vector;

public class AnacreditEquityReport extends AnacreditTradeReport
            implements IAnacreditReport {

    public AnacreditEquityReport() {
        super();
    }

    private AnacreditProcessorEquityBase _processor = new AnacreditProcessorEquityBase();

    protected void processRows(DefaultReportOutput output, List<ReportRow> reportRows, Vector errorMsgs) {
        _processor.processReportRows(this, output, reportRows, errorMsgs);
    }

}
