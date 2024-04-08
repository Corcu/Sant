package calypsox.tk.report;

import calypsox.tk.anacredit.api.AnacreditConstants;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class AnacreditInventoryGenericReport extends BOSecurityPositionReport {

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
            e3.printStackTrace();
            error.append("Error generating Report.\n");
            error.append(e3.getLocalizedMessage());
            Log.error(this, e3);//Sonar
        }
        Log.error(this, error.toString());
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

        String s = getReportTemplate().get(AnacreditConstants.ANACREDIT_EXTRACTION_TYPE);
        if (Util.isEmpty(s))   {
            errorMsgs.add("Extraction Type not selected.");
        }

        DefaultReportOutput reportOutput = (DefaultReportOutput) super.load(errorMsgs);
        if (reportOutput != null) {
            processRows(reportOutput, errorMsgs);
        }
        return reportOutput;

    }

    protected void processRows(DefaultReportOutput reportOutput, Vector errorMsgs) {

    }

    private void printLog(Vector<String> errorMsgs){
        if(!Util.isEmpty(errorMsgs)){
            for(String line : errorMsgs){
                Log.info("Anacredit: ",line.trim());
            }
        }
    }
}
