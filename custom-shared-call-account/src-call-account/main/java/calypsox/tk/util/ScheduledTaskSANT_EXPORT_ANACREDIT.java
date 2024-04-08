package calypsox.tk.util;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.DisplayInBrowser;
import com.calypso.tk.util.ScheduledTaskREPORT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Vector;

@Deprecated
public class ScheduledTaskSANT_EXPORT_ANACREDIT extends ScheduledTaskREPORT {

    private static final String LOG_PATH = "Log Path";
    private static final String LOG_FILE_NAME = "Log File Name";

    @Override
    public Vector getDomainAttributes() {
        Vector domainAttributes = super.getDomainAttributes();
        domainAttributes.add(LOG_FILE_NAME);
        domainAttributes.add(LOG_PATH);
        return domainAttributes;
    }

    @Override
    public boolean createReport(String type, String templateName, String format, String[] fileNames, JDatetime valDatetime, DSConnection ds, PSConnection ps, StringBuffer sb) {
        boolean ret = true;
        String html = null;

        try {
            ReportOutput output = this.generateReportOutput(type, templateName, valDatetime, ds, sb);
            if (output == null) {
                ret = false;
            } else if (fileNames != null && fileNames.length > 0) {
                this.processOutput(output, ds);
                html = this.saveReportOutput(output, format, type, fileNames, sb);
            }
        } catch (Exception var12) {
            Log.error(this, var12);
            sb.append("Can't save report " + type + ", please consult log file. " + var12.getMessage() + "\n");
            ret = false;
        }

        if (html == null) {
            Log.error(this, "Generated document is empty");
            sb.append("Can't generate report " + type + ", please consult log file. " + "\n");
            ret = false;
        }

        return ret & this.sendReportByEMail(html, type, ds, ps, sb);
    }

    @Override
    protected String saveReportOutput(ReportOutput output, String format, String type, String[] fileNames, StringBuffer sb) {
      /*  String file = super.saveReportOutput(output, format, type, fileNames, sb);
        ;*/
        String ext = "html";
        if (format != null) {
            if (format.equals("Excel")) {
                ext = "xls";
            } else if (format.equals("csv")) {
                ext = "csv";
            } else if (format.equals("pdf")) {
                ext = "pdf";
            }
        }

        ReportViewer viewer = DefaultReportOutput.getViewer(ext);
        output.format(viewer);
        if (viewer instanceof PDFReportViewer) {
            ((PDFReportViewer)viewer).setFileName(fileNames[0]);
        }
        String file = viewer.toString();

        if(file.contains("\"")){
            file = file.replace("\"",""); //TODO Optimize this
        }

        this.setFileName(DisplayInBrowser.buildDocument(file, "txt", fileNames[0], false, 1));

        return file;

    }

    private ReportOutput generateReportOutput(String type, String templateName, JDatetime valDatetime, DSConnection ds, StringBuffer sb) throws RemoteException {
        PricingEnv env = ds.getRemoteMarketData().getPricingEnv(this._pricingEnv, valDatetime);
        Report reportToFormat = this.createReport(type, templateName, sb, env);
        if (reportToFormat == null) {
            Log.error(this, "Invalid report type: " + type);
            sb.append("Invalid report type: " + type + "\n");
            return null;
        } else if (reportToFormat.getReportTemplate() == null) {
            Log.error(this, "Invalid report template: " + type);
            sb.append("Invalid report template: " + type + "\n");
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
            Vector errorMsgs = new Vector();
            ReportOutput load = reportToFormat.load(errorMsgs);
            writeLogFile(errorMsgs);
            return load;
        }
    }


    private void writeLogFile(Vector errorMsgs){
        if(!Util.isEmpty(errorMsgs)){
            String fileName = !Util.isEmpty(getAttribute(LOG_FILE_NAME)) ? getAttribute(LOG_FILE_NAME) : "";
            String logPath = !Util.isEmpty(getAttribute(LOG_PATH)) ? getAttribute(LOG_PATH) : "";

            if(!Util.isEmpty(logPath) && !Util.isEmpty(fileName)){
                JDateFormat format = new JDateFormat("ddMMyyyy");
                String date = format.format(JDate.getNow());

                File directory = new File(logPath);
                if(directory.exists()){
                    Path path = Paths.get(logPath + fileName + date + ".log");
                    //Use try-with-resource to get auto-closeable writer instance
                    try (BufferedWriter writer = Files.newBufferedWriter(path))
                    {
                        Iterator iterator = errorMsgs.iterator();
                        while (iterator.hasNext()){
                            String line = (String) iterator.next();
                            writer.write(line);
                            writer.newLine();
                        }
                    }catch (final IOException e) {
                        Log.error(this, "An error ocurred while writing the files: " + fileName, e);
                    }
                }
            }else{
                Log.warn(this,"Set log setting.");
            }
        }
    }
}
