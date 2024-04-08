package calypsox.tk.util;

import calypsox.tk.report.BODisponibleSecurityPositionReportStyle;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskREPORT;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScheduledTaskMERGE_CSVREPORT extends ScheduledTaskBODISPONIBLE_PARTENON_MERGE {

    public static final String SECOND_REPORT_TEMPLATE_NAME = "SECOND REPORT TEMPLATE NAME";

    private static final int THREAD_POOL_SIZE = 2;
    private static final int AWAIT_TERMINATION_TIMEOUT = 120;
    @Override
    public boolean createReport(String type, String templateName, String format, String[] fileNames, JDatetime valDatetime, DSConnection ds, PSConnection ps, StringBuffer sb) {
        if (Log.isCategoryLogged(Log.OLD_TRACE)) {
            Log.debug(Log.OLD_TRACE, "in the second create report");
        }

        boolean ret = true;
        String html = null;
        ReportOutput output = null;
        boolean saveReport = true;
        boolean sendReport = true;

        try {

            output = generateReports(type,valDatetime,ds,sb);

            if (output == null) {
                ret = false;
                saveReport = false;
                sendReport = false;
            } else if (output.getNumberOfRows() == 0) {
                saveReport = false;
                sendReport = false;
            }

            if (Log.isCategoryLogged(Log.OLD_TRACE)) {
                Log.debug(Log.OLD_TRACE, "we have a report output = " + (output == null ? " null " : output));
            }

            if (output != null && "true".equalsIgnoreCase(this.getAttribute("Save or Email Blank Report"))) {
                saveReport = true;
            }

            if ("true".equalsIgnoreCase(this.getAttribute("Save or Email Blank Report"))) {
                sendReport = true;
            }

            if (saveReport && fileNames != null && fileNames.length > 0) {
                this.processOutput(output, ds);
                html = this.saveReportOutput(output, format, type, fileNames, sb);
                if (html == null) {
                    Log.error(this, "Generated document is empty");
                    sb.append("Can't generate report " + type + ", please consult log file. \n");
                    ret = false;
                }
            } else {
                Log.info(this, "Generated document is empty/null or file names are empty : will not save. type : " + type + " fileNames : " + fileNames);
                sb.append("Generated document is empty/null or file names are empty : will not save. type : " + type + " fileNames : " + fileNames);
            }

            if (sendReport) {
                ret &= this.sendReportByEMail(html, type, ds, ps, sb);
            } else {
                Log.info(this, "Generated document is empty : will not send by email. type : " + type);
                sb.append("Generated document is empty : will not send by email. type : " + type);
            }
        } catch (Exception var15) {
            Log.error(this, var15);
            sb.append("Can't save report " + type + ", please consult log file. " + var15.getMessage() + "\n");
            ret = false;
        }

        return ret;
    }

    private ReportOutput generateReports (String type, JDatetime valDatetime, DSConnection ds, StringBuffer sb) throws Exception {
        String firstReportTemplateName = getAttribute(REPORT_TEMPLATE_NAME);
        String secondReportTemplateName = getAttribute(SECOND_REPORT_TEMPLATE_NAME);

        if(!Util.isEmpty(secondReportTemplateName)){
            return mergeReports(type,valDatetime,ds,sb);
        }else {
            return generateSantReportOutput(type,firstReportTemplateName,valDatetime,ds,sb);
        }
    }

    private ReportOutput mergeReports(String type, JDatetime valDatetime, DSConnection ds, StringBuffer sb) throws InterruptedException {
        List<String> reportTemplateNames = new ArrayList<>();
        String firstReportTemplateName = getAttribute(REPORT_TEMPLATE_NAME);
        reportTemplateNames.add(firstReportTemplateName);
        reportTemplateNames.add(getAttribute(SECOND_REPORT_TEMPLATE_NAME));

        ConcurrentLinkedQueue<ReportOutput> outputs = new ConcurrentLinkedQueue<>();

        ExecutorService exec = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try {
            List<Callable<Void>> tasks = reportTemplateNames.stream()
                    .map(template -> (Callable<Void>) () -> {
                        try {
                            outputs.add(generateSantReportOutput(type, template, valDatetime, ds, sb));
                        } catch (RemoteException e) {
                            Log.error(this.getClass().getSimpleName(), "Error generating report output: " + e.getMessage());
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

            exec.invokeAll(tasks);

        } finally {
            exec.shutdown();
            exec.awaitTermination(AWAIT_TERMINATION_TIMEOUT, TimeUnit.MINUTES);
        }

        return processReportOutputs(firstReportTemplateName, outputs);
    }

    private ReportOutput processReportOutputs(String firstReportTemplateName, ConcurrentLinkedQueue<ReportOutput> outputs){
        if(outputs.size()>1){
            // Merge two reports outputs
            List<ReportOutput> collect = new ArrayList<>(outputs);
            DefaultReportOutput reportOutput = (DefaultReportOutput) collect.get(0);
            DefaultReportOutput reportOutput1 = null;
            String templateName = Optional.ofNullable(reportOutput).map(DefaultReportOutput::getReport).map(Report::getReportTemplate).map(t -> ((ReportTemplate) t).getTemplateName()).orElse("");
            if(firstReportTemplateName.equalsIgnoreCase(templateName)){
                reportOutput1 = (DefaultReportOutput) collect.get(1);
            }else {
                reportOutput = (DefaultReportOutput) collect.get(1);
                reportOutput1 = (DefaultReportOutput) collect.get(0);
            }

            if(reportOutput1!=null && reportOutput!=null){
                ReportRow[] mergedRows = Stream.concat(
                        Arrays.stream(reportOutput.getRows()),
                        Arrays.stream(reportOutput1.getRows())
                ).toArray(ReportRow[]::new);
                reportOutput.setRows(mergedRows);
                reorderRowNumbers(reportOutput);
            }
            return reportOutput;
        }else {
            return outputs.peek();
        }
    }

    private void reorderRowNumbers(DefaultReportOutput reportOutput){
        AtomicInteger numbOfRow = new AtomicInteger(0);
        Optional.ofNullable(reportOutput).map(DefaultReportOutput::getRows)
                .ifPresent(reportRows -> Arrays.stream(reportRows)
                        .forEach(row -> row.setProperty(BODisponibleSecurityPositionReportStyle.ROW_NUMBER,numbOfRow.addAndGet(1))));
    }

    private ReportOutput generateSantReportOutput (String type, String templateName, JDatetime valDatetime, DSConnection ds, StringBuffer sb) throws RemoteException{
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
            return reportToFormat.load(errorMsgs);
        }
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(SECOND_REPORT_TEMPLATE_NAME).description("Second Template Name"));
        return attributeList;
    }

    @Override
    public Vector getAttributeDomain(String attribute, Hashtable currentAttr) {
        String type;
        Vector v = new Vector();
        if (attribute.equals(SECOND_REPORT_TEMPLATE_NAME)) {
            if (currentAttr == null) {
                return v;
            }

            type = this.getReportType(currentAttr);
            if (type == null) {
                return v;
            }

            type = ReportTemplate.getReportName(type);
            Vector names = BOCache.getReportTemplateNames(DSConnection.getDefault(), type, (String)null);
            v.add("");
            for(int i = 0; i < names.size(); ++i) {
                ReportTemplateName r = (ReportTemplateName)names.elementAt(i);
                v.add(r.getTemplateName());
            }
            return v;
        }

        return super.getAttributeDomain(attribute, currentAttr);
    }

    @Override
    public Vector getDomainAttributes() {
        List<String> result = super.getDomainAttributes();
        List<String> finalAttList = new ArrayList<>();
        for (String att : result){
            finalAttList.add(att);
            if(ScheduledTaskREPORT.REPORT_TEMPLATE_NAME.equalsIgnoreCase(att)){
                finalAttList.add(SECOND_REPORT_TEMPLATE_NAME);
            }
        }
        return new Vector(finalAttList);
    }

}
