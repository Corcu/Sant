package calypsox.tk.util;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.rmi.RemoteException;
import java.util.*;


public class ScheduledTaskCA_NOTIF_CPTY_GROUP_CLAIMS_EMAIL extends ScheduledTaskCSVREPORT {


    private String reportType = "";
    private String reportTemplate = "";
    private String pricingEnv = "";
    private String holidays = "";
    private String datetime = "";
    JDatetime paramDatetime = null;


    @Override
    public String getTaskInformation() {
        return null;
    }


    @Override
    public boolean process(final DSConnection dsCon, final PSConnection connPS) {
        reportType = getAttribute("REPORT TYPE");
        reportTemplate = getAttribute("REPORT TEMPLATE NAME");
        pricingEnv = getPricingEnv();
        datetime = getValuationDatetime().toString();
        holidays = getHolidays().toString();
        paramDatetime = getValuationDatetime();
        Vector<String> paramHolidays = getHolidays();
        DefaultReportOutput reportOutput = null;

        try {
            // Obtiene todas las filas del reporte y plantilla configurados como parametros en la ST
            reportOutput = (DefaultReportOutput) generateReportOutput(dsCon, pricingEnv, paramHolidays, reportType, reportTemplate, paramDatetime);
        } catch (RemoteException e) {
            Log.error(LOG_CATEGORY, e);
        }

        if (reportOutput == null) {
            return true;
        }
        ReportRow[] rows = reportOutput.getRows();
        if (rows == null && rows.length == 0) {
            return true;
        }

        // Estructura en un map todas las claims segregadas por contrapatidas
        Map<String, ArrayList<BOTransfer>> map = getInfoMapByCpty(rows);

        // Genera un BOMessage de tipo CA_NOTIF que envia posteriormente por mail
        generateMessages(dsCon, map);

        return true;
    }


    private HashMap<String, ArrayList<BOTransfer>> getInfoMapByCpty(ReportRow[] rows) {
        HashMap<String, ArrayList<BOTransfer>> map = new HashMap<String, ArrayList<BOTransfer>>();
        for (int i = 0; i < rows.length; i++) {
            ReportRow row = rows[i];
            BOTransfer xfer = (BOTransfer) row.getProperty("BOTransfer");
            Trade trade = (Trade) row.getProperty("Trade");
            String cpty = trade.getCounterParty().getCode();

            if (map.containsKey(cpty)) {
                ArrayList<BOTransfer> xferList = map.get(cpty);
                xferList.add(xfer);
            } else {
                ArrayList<BOTransfer> xferList = new ArrayList<BOTransfer>();
                xferList.add(xfer);
                map.put(cpty, (ArrayList<BOTransfer>) xferList);
            }
            Log.system(this.getClass().toString(), i + 1 + " of " + rows.length + ": " + cpty + " - " + xfer.getLongId());
        }
        return map;
    }


    private void generateMessages(DSConnection dsCon, Map<String, ArrayList<BOTransfer>> map) {
        for (Map.Entry entry : map.entrySet()) {
            String cpty = (String) entry.getKey();
            ArrayList<BOTransfer> xferList = (ArrayList<BOTransfer>) entry.getValue();
            System.out.println(": " + cpty + " & Value: " + xferList.toString());
            if(xferList!=null && xferList.size()>0){
                BOMessage message = getNofitcationMessage(dsCon, xferList);
                if(message!=null) {
                    JDatetime datetimeNow = JDatetime.currentTimeValueOf(JDate.getNow(),TimeZone.getDefault());
                    message.setLongId(0);
                    message.setTransferLongId(0);
                    message.setTradeLongId(0);
                    message.setEventType("GROUP_NOTIF");
                    message.setStatus(Status.S_NONE);
                    message.setAction(Action.NEW);
                    message.setTemplateName("CorporateEventGroupNotice.html");
                    message.setCreationDate(datetimeNow);
                    message.setCreationSystemDate(datetimeNow);
                    message.setUpdateDatetime(datetimeNow);
                    message.setSettleDate(null);
                    message.setTradeUpdateDatetime(null);
                    message.setAttribute("ReportType", reportType);
                    message.setAttribute("ReportTemplate", reportTemplate);
                    message.setAttribute("PricingEnv", pricingEnv);
                    message.setAttribute("Holidays", holidays);
                    message.setAttribute("Datetime", datetime);
                    message.setAttribute("CptyId", String.valueOf(xferList.get(0).getExternalLegalEntityId()));
                    try {
                        long msgId = DSConnection.getDefault().getRemoteBO().save(message, 0, null);
                    } catch (CalypsoServiceException e) {
                        Log.system(this.getClass().toString(), "Could not save the new message.");
                    }
                }
                updateGroupCounterInXferList(xferList);
            }
        }
    }


    private void updateGroupCounterInXferList(ArrayList<BOTransfer> xferList){
        for (int i=0; i<xferList.size(); i++) {
           BOTransfer xfer = xferList.get(i);
           if(xfer != null){
               String attr = xfer.getAttribute("NumberOfGroupedNotifications");
               if(Util.isEmpty(attr)){
                   xfer.setAttribute("NumberOfGroupedNotifications","1");
               }
               else{
                   int count = Integer.parseInt(attr);
                   count++;
                   xfer.setAttribute("NumberOfGroupedNotifications",String.valueOf(count));
               }
               try {
                   long none = DSConnection.getDefault().getRemoteBO().save(xfer, 0, "None");
               } catch (CalypsoServiceException e) {
                   Log.error(this,"Error saving BoTransfer: " + e);
               }
           }
        }
    }


    private BOMessage getNofitcationMessage(DSConnection dsCon, ArrayList<BOTransfer> xferList){
        BOMessage msg = null;
        BOMessage xferMsg = null;
        try {
            for (int i=0; i<xferList.size(); i++) {
                if (msg != null) {
                    break;
                }
                else{
                    MessageArray msgList = null;
                    msgList = dsCon.getRemoteBO().getTransferMessages(xferList.get(i).getLongId());
                    if (msgList != null && msgList.size() > 0){
                        for (int y = 0; y < msgList.size(); y++) {
                            xferMsg = msgList.get(y);
                            if ("CA_NOTIF".equalsIgnoreCase(xferMsg.getMessageType())) {
                                msg = (BOMessage) xferMsg.clone();
                                break;
                            }
                        }
                    }
                }
            }
        } catch (CalypsoServiceException e1) {
            Log.error(this.getClass().toString(), "Could not get the transfer.");
        } catch (CloneNotSupportedException e) {
            Log.error(this.getClass().toString(), "Could not clone the message " + xferMsg.getLongId());
        }
        return msg;
    }


    protected ReportOutput generateReportOutput(DSConnection dsCon, String pricingEnv, Vector<String> holidays, String reportType, String reportTemplate, JDatetime valDatetime) throws RemoteException {
        PricingEnv env = dsCon.getRemoteMarketData().getPricingEnv(pricingEnv, valDatetime);
        // Report reportToFormat = createReport(dsCon, holidays, reportType, reportTemplate, env);
        Report reportToFormat = createReport(reportType, reportTemplate, null, env);
        if (reportToFormat == null) {
            Log.info(this, "Invalid report type: " + reportType + " or no info to process.");
            return null;
        } else if (reportToFormat.getReportTemplate() == null) {
            Log.error(this, "Invalid report template: " + reportType);
            return null;
        } else {
            reportToFormat.getReportTemplate().setHolidays(holidays);
            if (TimeZone.getDefault() != null) {
                reportToFormat.getReportTemplate().setTimeZone(TimeZone.getDefault());
            }
            Vector<String> errorMsgs = new Vector<String>();
            return reportToFormat.load(errorMsgs);
        }
    }


}
