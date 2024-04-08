package calypsox.tk.report;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.processor.AnacreditProcessorPdv;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public abstract class AnacreditTradeReport extends TradeReport
            implements IAnacreditReport {

    abstract protected void processRows(DefaultReportOutput output, List<ReportRow> reportRows, Vector errorMsgs);

    public AnacreditTradeReport() {
        super();
    }

  /**
     * Load Report Output Rows
     *
     * @param errorMsgs
     *
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ReportOutput load(final Vector errorMsgs) {
        StringBuilder error = new StringBuilder();
        try {
            errorMsgs.clear();
            return getReportOutput(errorMsgs);
        } catch (Exception e3) {
            error.append("Error generating Report.\n");
            error.append(e3.getLocalizedMessage());
            Log.error(this, e3);//Sonar
        }
        errorMsgs.add(error.toString());
        printLog(errorMsgs);
        return null;
    }

    /**
     * Generates report output by running
     * @param errorMsgs error messages
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ReportOutput getReportOutput(Vector errorMsgs) {
        DefaultReportOutput reportOutput = (DefaultReportOutput) super.load(errorMsgs);
        if (reportOutput != null) {
            if (!_countOnly) {
                ReportRow[] rows = reportOutput.getRows();
                List<ReportRow> reportRows = Arrays.asList(rows);
                processRows(reportOutput, reportRows, errorMsgs);
            }
        }
        return reportOutput;
    }

    /**
     * Build and return one ReportOutput object from a ReportRow provided list.
     * @param items
     * @param errorMsgs
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ReportOutput buildReportOutputFrom(ReportRow[] items, Vector errorMsgs) {

        getReportTemplate().put(AnacreditConstants.USE_CACHED_ROWS, Boolean.TRUE);

        DefaultReportOutput output = new DefaultReportOutput(this);
        List<ReportRow> cachedItems = Arrays.asList(items);
        processRows(output, cachedItems, errorMsgs);
        return output;
    }

    private void printLog(Vector<String> errorMsgs){
        if(!Util.isEmpty(errorMsgs)){
            for(String line : errorMsgs){
                Log.info("Anacredit: ",line.trim());
            }
        }
    }
}
