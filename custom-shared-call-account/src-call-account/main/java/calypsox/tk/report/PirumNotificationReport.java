package calypsox.tk.report;

import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;

import java.util.HashMap;
import java.util.Vector;

public class PirumNotificationReport extends SantInterestNotificationReport implements CheckRowsNumberReport {

    @Override
    public ReportOutput loadReport(Vector errorMsgs) {
        final DefaultReportOutput output;

        try {
            output = (DefaultReportOutput) super.loadReport(errorMsgs);

            //Generate a task is the report size is out of a defined umbral
            HashMap<String, String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
            if (!value.isEmpty() && value.keySet().iterator().next().equals("ScheduledTask: ")){
                checkAndGenerateTaskReport(output, value);
            }
            return output;
        }catch (Exception e){
            Log.error(this, e);
        }

        return null;
    }
}
