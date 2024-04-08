package calypsox.tk.report;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.FilterSet;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TransferReport;

import java.util.*;

public class FTTXferReport extends TransferReport {

    public static final String REPORT_FREQUENCY = "REPORT FREQUENCY";
    public static final String ATTR_PREVIOUS_MONTH = "Previous Month";
    public static final String ROW_COUNTER = "ROWCOUNTER";
    @Override
    @SuppressWarnings("rawtypes")
    public ReportOutput load(Vector vector) {


        if (getReportTemplate().get(REPORT_FREQUENCY) != null
                && getReportTemplate().get(REPORT_FREQUENCY).equals(ATTR_PREVIOUS_MONTH)) {

            JDatetime valDate = getValuationDatetime();
            // Begin of previous month
            Calendar cal = valDate.getJDate(TimeZone.getDefault()).addMonths(-1).asCalendar();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
            Date startDate = cal.getTime();

            String startDateStr = JDate.valueOf(cal).toString();
            getReportTemplate().put("StartDate",  startDateStr);

            setStartDate(JDate.valueOf(startDate));
            // End of previous month
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            Date  endDate = cal.getTime();
            setEndDate(JDate.valueOf(endDate));

            String endDateStr = JDate.valueOf(cal).toString();
            getReportTemplate().put("EndDate", endDateStr);

            Log.system(this.getClass().getName(), "Executing report starDate = " + startDate + " EndDate:" + endDate);

        }


        DefaultReportOutput dro = (DefaultReportOutput) super.load(vector);
        if(dro != null){
            filterAndCountTransfers(dro);
        }
        return dro;

    }

    private void filterAndCountTransfers(DefaultReportOutput dro) {
        ArrayList<ReportRow> result = new ArrayList<>();
        int counter = 0;
        for (ReportRow row: dro.getRows()) {
            BOTransfer xfer = row.getProperty(ReportRow.TRANSFER);
            if (xfer != null) {
                if (xfer.getNettedTransferLongId() == 0) {
                    row.setProperty(ROW_COUNTER, ++counter);
                    result.add(row);
                }
            }
        }
        dro.setRows(result.stream().toArray(ReportRow[]::new));
    }
}
