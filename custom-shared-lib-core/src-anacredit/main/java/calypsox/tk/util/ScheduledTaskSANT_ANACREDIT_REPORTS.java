package calypsox.tk.util;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.util.AnacreditFilenameUtil;
import calypsox.tk.report.IAnacreditReport;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.DisplayInBrowser;
import com.calypso.tk.util.ScheduledTaskREPORT;

import java.io.*;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduledTaskSANT_ANACREDIT_REPORTS extends ScheduledTaskREPORT {

    private static final String REPORT_OUTPUT_FOLDER = "Report Output Folder";
    private static final String REPORT_EXTRACTION_NAME = "Extraction Name";
    private static final String LOG_PATH = "Log Path";
    private static final String LOG_FILE_NAME = "Log File Name";
    private static final String LOG_CAT = "ScheduledTaskSANT_ANACREDIT_REPORTS";
    private static final String PREFIX_COPY_3 = "Copy3_";
    private static final String PREFIX_COPY_4A = "Copy4A_";
    private static final String PREFIX_COPY_4 = "Copy4_";
    private static final String PREFIX_COPY_11 = "Copy11_";
    private static final String PREFIX_COPY_13 = "Copy13_";

    private ReportRow[] _cachedReportRows = null;
    private static final SimpleDateFormat _sdf = new SimpleDateFormat("yyyyMMdd");

    private String _outputFolder = null;

    @Override
    public boolean createReport(String type, String templateName, String format, String[] fileNames, JDatetime valDatetime, DSConnection ds, PSConnection ps, StringBuffer sb) {

        _outputFolder = getOutputFolder();
        if (!_outputFolder.endsWith("/")) {
            _outputFolder = _outputFolder.concat("/");
        }
        try {
            boolean ok = executeReports(format, valDatetime, ds, ps, sb);
            if (!ok) {
                return false;
            }
        } catch (IOException e) {
            Log.error(LOG_CAT, e);
            return false;
        }
        return true;
    }

    public boolean executeReports(String format, JDatetime valDatetime, DSConnection ds, PSConnection ps, StringBuffer sb) throws IOException {
        // Execution of reports
        boolean ok = true;
        String reportType = this.getAttribute("REPORT TYPE");
        if (!Util.isEmpty(reportType)) {

            Map<String, String> reportsToExecute = AnacreditFilenameUtil.getReportIterationsMap(reportType);

            Vector<String> errors = new Vector<>();
            for (Map.Entry<String, String> entry : reportsToExecute.entrySet()) {
                String key = entry.getKey();
                String strFile = entry.getValue();
                String customTemplateName = getCustomTemplateName(key);
                String customFileName = getCustomFilename(strFile);
                ok = createSingleReport(key, customTemplateName, format, new String[]{customFileName}, valDatetime, ds, ps, sb, errors);
                if (!ok) {
                    Log.error(LOG_CAT, "Error executing extraction of : " + key + " file:" + customFileName);
                    return false;
                }

            }
            writeLogFile(errors);
        }
        return ok;
    }

    private String getCustomTemplateName(String key) {
        String extractionName = getExtractionName();
        if (Util.isEmpty(extractionName) || "ALL".equals(extractionName)) {
            return key;
        }
        if (extractionName.equals("PDV")
                || extractionName.equals("REPO")
                || extractionName.equals("EQPOS")
                || extractionName.equals("EQPLZ")) {

                return key;
        }

        return key + "_" + extractionName;
    }

    private String getCustomFilename(String fileName) {
        StringBuilder customFileName = new StringBuilder(_outputFolder);
        String extractionName = getExtractionName();
        customFileName.append(fileName)
                .append(_sdf.format(getValuationDatetime()))
                .append("_")
                .append(extractionName);
        return customFileName.toString(); // appendDateTimme(valDatetime, this, customFileName.toString());
    }

    private String getExtractionName() {
        String extractionName = getAttribute(REPORT_EXTRACTION_NAME);
        if (Util.isEmpty(extractionName)) {
            extractionName = "ALL";
        }
        return extractionName;
    }

    public boolean createSingleReport(String type, String templateName, String format, String[] fileNames, JDatetime valDatetime, DSConnection ds, PSConnection ps, StringBuffer sb, Vector<String> errors) throws IOException {

        boolean ret = true;
        String html = null;
        try {
            ReportOutput output = this.generateReportOutput(type, templateName, valDatetime, ds, sb, errors);
            if (output == null) {
                ret = false;
            } else if (fileNames != null && fileNames.length > 0) {
                this.processOutput(output, ds);
                html = this.saveReportOutput(output, format, type, fileNames, sb);
            }
        } catch (Exception e) {
            Log.error(LOG_CAT, e);
            sb.append("Can't save report " + type + ", please consult log file. " + e.getMessage() + "\n");
            ret = false;
        }

        if (html == null) {
            Log.error(this, "Generated document is empty");
            sb.append("Can't generate report " + type + ", please consult log file. " + "\n");
            ret = false;
        }

        if (ret) {
            Log.system(LOG_CAT, "File generated successfully : " + fileNames[0]);
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
            ((PDFReportViewer) viewer).setFileName(fileNames[0]);
        }
        String file = viewer.toString();

        if (file.contains("\"")) {
            file = file.replace("\"", ""); //TODO Optimize this
        }
        this.setFileName(DisplayInBrowser.buildDocument(file, "txt", fileNames[0], false, 1));
        return file;
    }

    private ReportOutput generateReportOutput(String type, String templateName, JDatetime valDatetime, DSConnection ds, StringBuffer sb, Vector errorMsgs) throws RemoteException {
        PricingEnv env = ds.getRemoteMarketData().getPricingEnv(this._pricingEnv, valDatetime);
        Report reportToFormat = this.createReportInternal(type, templateName, sb, env);
        if (reportToFormat == null) {
            Log.error(LOG_CAT, "Invalid report type: " + type);
            sb.append("Invalid report type: " + type + "\n");
            return null;
        } else if (reportToFormat.getReportTemplate() == null) {
            Log.error(LOG_CAT, "Invalid report template: " + type);
            sb.append("Invalid report template: " + type + "\n");
            return null;
        } else {

            if (type.startsWith(PREFIX_COPY_3)) {
                reportToFormat.getReportTemplate().put(AnacreditConstants.ROW_DATA_TYPE, AnacreditConstants.COPY_3);
            } else if (type.startsWith(PREFIX_COPY_4)) {
                reportToFormat.getReportTemplate().put(AnacreditConstants.ROW_DATA_TYPE, AnacreditConstants.COPY_4);
            } else if (type.startsWith(PREFIX_COPY_4A)) {
                reportToFormat.getReportTemplate().put(AnacreditConstants.ROW_DATA_TYPE, AnacreditConstants.COPY_4A);
            } else if (type.startsWith(PREFIX_COPY_11)) {
                reportToFormat.getReportTemplate().put(AnacreditConstants.ROW_DATA_TYPE, AnacreditConstants.COPY_11);
            } else if (type.startsWith(PREFIX_COPY_13)) {
                reportToFormat.getReportTemplate().put(AnacreditConstants.ROW_DATA_TYPE, AnacreditConstants.COPY_13);
            }

            Vector holidays = this.getHolidays();
            if (!Util.isEmpty(holidays)) {
                reportToFormat.getReportTemplate().setHolidays(holidays);
            }

            if (this.getTimeZone() != null) {
                reportToFormat.getReportTemplate().setTimeZone(this.getTimeZone());
            }

            this.modifyTemplate(reportToFormat);

            if (!(reportToFormat instanceof IAnacreditReport )) {
                Log.error(LOG_CAT, "Report is not Anacredit.");
                return null;
            }

            IAnacreditReport anacreditReport = (IAnacreditReport) reportToFormat;

            ReportOutput output = null;
            if (_cachedReportRows == null) {
                output = reportToFormat.load(errorMsgs);
                if (output != null) {
                    _cachedReportRows = ((DefaultReportOutput) output).getRows();
                }
            } else {
                output = anacreditReport.buildReportOutputFrom(_cachedReportRows, errorMsgs);
            }

            return output;
        }
    }

    private Report createReportInternal(String type, String templateName, StringBuffer sb, PricingEnv env) throws RemoteException {
        String templateNameFormatted = templateName;
        String typeFormatted = type;
        if (templateName.startsWith(PREFIX_COPY_3) ) {
            templateNameFormatted = templateName.replace(PREFIX_COPY_3,"");
            typeFormatted = templateNameFormatted;
        } else if (templateName.startsWith(PREFIX_COPY_4A) ) {
            templateNameFormatted = templateName.replace(PREFIX_COPY_4A,"");
            typeFormatted = templateNameFormatted;
        } else if (templateName.startsWith(PREFIX_COPY_4) ) {
            templateNameFormatted = templateName.replace(PREFIX_COPY_4,"");
            typeFormatted = templateNameFormatted;
        } else if (templateName.startsWith(PREFIX_COPY_11) ) {
            templateNameFormatted = templateName.replace(PREFIX_COPY_11,"");
            typeFormatted = templateNameFormatted;
        } else if (templateName.startsWith(PREFIX_COPY_13) ) {
            templateNameFormatted = templateName.replace(PREFIX_COPY_13,"");
            typeFormatted = templateNameFormatted;
        }

        Report report = this.createReport(typeFormatted, templateNameFormatted, sb, env);
        return report;
    }

    private void writeLogFile(Vector errorMsgs) {
        Log.system(LOG_CAT, "Prepare to write logs...");
        if (!Util.isEmpty(errorMsgs)) {
            String fileName = !Util.isEmpty(getAttribute(LOG_FILE_NAME)) ? getAttribute(LOG_FILE_NAME) : "";
            String logPath = !Util.isEmpty(getAttribute(LOG_PATH)) ? getAttribute(LOG_PATH) : "";

            if (Util.isEmpty(logPath) || Util.isEmpty(fileName)) {
                Log.system(String.valueOf(this), "Log config not set.");
                return;
            }


            JDateFormat format = new JDateFormat("ddMMyyyy");
            String date = format.format(JDate.getNow());
            File directory = new File(logPath);

            if (!directory.exists()) {
                try {
                    if (!directory.createNewFile()) {
                        Log.error(LOG_CAT, "Error creating   output log dir.");
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            Path path = Paths.get(logPath + File.separator + fileName + date + ".log");

            Log.system(LOG_CAT, "Writing Log file : " + path.toString());
            if (errorMsgs.isEmpty()){
                errorMsgs.add("No messages for this log file");
            }
            //Use try-with-resource to get auto-closeable writer instance
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                Iterator iterator = errorMsgs.iterator();
                while (iterator.hasNext()) {
                    String line = (String) iterator.next();
                    writer.write(line);
                    writer.newLine();
                }
            } catch (final IOException e) {
                Log.error(LOG_CAT, "An error occurred while writing the files: " + fileName, e);
            }
        }
    }

    @Override
    public Vector getDomainAttributes() {
        Vector domainAttributes = super.getDomainAttributes();
        domainAttributes.add(REPORT_EXTRACTION_NAME);
        domainAttributes.add(REPORT_OUTPUT_FOLDER);
        domainAttributes.add(LOG_FILE_NAME);
        domainAttributes.add(LOG_PATH);
        return domainAttributes;
    }

    private String getOutputFolder() {
        String folder = this.getAttribute(REPORT_OUTPUT_FOLDER);
        return !Util.isEmpty(folder) ? folder : "/calypso_interfaces/anacredit/data";
    }

}
