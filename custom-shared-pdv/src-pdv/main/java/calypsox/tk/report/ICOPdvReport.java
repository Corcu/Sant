package calypsox.tk.report;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TransferReport;

import java.util.*;


public class ICOPdvReport extends TransferReport {
        private static final long serialVersionUID = 1L;

        public static final String REPORT_FREQUENCY = "REPORT FREQUENCY";
        public static final String ATTR_CURRENT_MONTH = "Current Month";

        private JDate startDate = null;
        @Override
        @SuppressWarnings("rawtypes")
        public ReportOutput load(Vector vector) {

            if (getReportTemplate().get(REPORT_FREQUENCY) != null
                    && getReportTemplate().get(REPORT_FREQUENCY).equals(ATTR_CURRENT_MONTH)) {

                JDatetime valDate = getValuationDatetime();
                // Begin of previous month
                Calendar cal = valDate.getJDate(TimeZone.getDefault()).asCalendar();
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
                startDate = JDate.valueOf(cal.getTime());
                String startDateStr = JDate.valueOf(cal).toString();
                getReportTemplate().put("StartDate",  startDateStr);
                setStartDate(JDate.valueOf(startDate));

                Log.system(this.getClass().getName(), "#### Executing report Start Date = " + startDate );
            }

            DefaultReportOutput dro = (DefaultReportOutput) super.load(vector);
            filterAndCountTransfers(dro);

            return dro;
        }

    private void filterAndCountTransfers(DefaultReportOutput dro) {
        if (null == dro
                || null == dro.getRows())   {
            return;
        }
        ArrayList<ReportRow> result = new ArrayList<>();
        for (ReportRow row: dro.getRows()) {
            Trade trade = row.getProperty(ReportRow.TRADE);
            if (trade != null) {
                if (getStartDate() != null)
                    if (trade.getTradeDate().getJDate(TimeZone.getDefault()).before(getStartDate())) {
                        continue;
                    }
                    result.add(row);
                }
        }
        dro.setRows(result.stream().toArray(ReportRow[]::new));
    }
}