package calypsox.tk.util;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskREPORT;

import java.io.*;
import java.rmi.RemoteException;
import java.util.Vector;


public class ScheduledTaskSANT_CA_MT566_MESSAGES_TO_IG extends ScheduledTaskREPORT {


    @Override
    public String getTaskInformation() {
        return "This Scheduled Task generate two reports of MT...";
    }


    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        Boolean rtn = super.process(ds, ps);
        rtn = rtn && generateTextFile(ds);
        return rtn;
    }


    private boolean generateTextFile(DSConnection ds){
        Boolean ret = true;
        String reportType = getAttribute("REPORT TYPE");
        String reportTemplate = getAttribute("REPORT TEMPLATE NAME");
        JDatetime valDatetime = getValuationDatetime();
        String newFileName = getNewFileName();
        FileWriter writer = null;
        try {
            DefaultReportOutput reportOutput = (DefaultReportOutput) generateReportOutput(reportType, reportTemplate, valDatetime, ds, new StringBuffer());
            writer = new FileWriter(newFileName);
            if (reportOutput == null) {
                ret = false;
            }
            else {
                ReportRow[] rows = reportOutput.getRows();
                for (ReportRow row : rows) {
                    BOMessage message = (BOMessage) row.getProperty("BOMessage");
                    Long messageId = message.getLongId();
                    Vector adviceDocuments = null;
                    try {
                        adviceDocuments = ds.getRemoteBackOffice().getAdviceDocuments("advice_document.advice_id=" + messageId, null, null);
                    } catch (CalypsoServiceException e) {
                        Log.error(this, "Cannot load Advice Documents for Swift message id " + messageId);
                    }
                    if (Util.isEmpty(adviceDocuments))
                        Log.error(this, "No Advice Documents found for Swift message id " + messageId);
                    else {
                        for (Object docObj : adviceDocuments) {
                            AdviceDocument document = (AdviceDocument) docObj;
                            String swiftText = document.getDocument().toString();
                            writer.write(swiftText + "\n\n\n");
                        }
                    }
                }
                writer.close();
            }
        } catch (IOException e) {
            Log.error(this.getClass().toString(), "Could not write file in path '" + newFileName + "'", e);
            ret = false;
        } catch (Exception e) {
            Log.error(this, e);
            ret = false;
        }

        return ret;
    }


    private ReportOutput generateReportOutput(String reporType, String reportTemplate, JDatetime valDatetime, DSConnection ds, StringBuffer sb) throws RemoteException {
        PricingEnv env = ds.getRemoteMarketData().getPricingEnv(this._pricingEnv, valDatetime);
        Report reportToFormat = this.createReport(reporType, reportTemplate, sb, env);
        if (reportToFormat == null) {
            Log.error(this, "Invalid report type: " + reporType);
            sb.append("Invalid report type: " + reporType + "\n");
            return null;
        } else if (reportToFormat.getReportTemplate() == null) {
            Log.error(this, "Invalid report template: " + reporType);
            sb.append("Invalid report template: " + reporType + "\n");
            return null;
        } else {
            Vector holidays = this.getHolidays();
            if (!Util.isEmpty(holidays)) {
                reportToFormat.getReportTemplate().setHolidays(holidays);
            }
            if (this.getTimeZone() != null) {
                reportToFormat.getReportTemplate().setTimeZone(this.getTimeZone());
            }
            this.modifyTemplate(reportToFormat);
            return reportToFormat.load(new Vector());
        }
    }


    public String getNewFileName() {
        String fileName = getFileName().substring(0,getFileName().length()-3).concat("txt");;
        if (fileName.startsWith("file://")) {
            fileName = fileName.substring(7);
        }
        return fileName;
    }


}
