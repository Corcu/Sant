package calypsox.tk.util;

import calypsox.tk.event.PSEventSantInitialMarginExport;
import calypsox.tk.report.InitialMarginContractConfigReport;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskREPORT;

import java.rmi.RemoteException;
import java.util.Vector;

public class ScheduledTaskSANT_EXPORT_TO_QEF extends ScheduledTaskREPORT {
    private static final long serialVersionUID = 3855757591727676472L;

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        String templateName = getAttribute("REPORT TEMPLATE NAME");
        InitialMarginContractConfigReport report = new InitialMarginContractConfigReport();
        Vector errorMsgs = new Vector();

        if (!Util.isEmpty(templateName)) {
            try {
                ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
                        .getReportTemplate(ReportTemplate.getReportName(report.getType()), templateName);
                if (null != template) {
                    report.setReportTemplate(template);
                } else {
                    Log.error(this, "Couldn't load template: " + templateName);
                    return false;
                }
            } catch (RemoteException e) {
                Log.error(this, "Couldn't create InitialMarginContractConfig Report: " + e.getMessage());
            }
            DefaultReportOutput reportoutput = (DefaultReportOutput) report.load(errorMsgs);

            if (!sendtoqef(reportoutput)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    public boolean sendtoqef(DefaultReportOutput reportOutput) {
        if ((reportOutput != null) && (reportOutput.getRows() != null)) {
            ReportRow[] rows = reportOutput.getRows();
            JDate processDate = null;
            int contractid = 0;
            MarginCallEntryDTO entry;
            Vector<PSEventSantInitialMarginExport> allEvents = new Vector<>();

            for (ReportRow row : rows) {
                entry = row.getProperty("Default");
                if (null != entry) {
                    if (entry.getProcessDate() != null) {
                        processDate = entry.getProcessDate();
                    } else {
                        processDate = JDate.getNow();
                    }
                    contractid = entry.getCollateralConfigId();
                } else if (null != row.getProperty("MarginCallConfig")) {
                    processDate = JDate.getNow();
                    contractid = ((CollateralConfig) row.getProperty("MarginCallConfig")).getId();
                }
                if (contractid != 0) {
                    PSEventSantInitialMarginExport newEvent = new PSEventSantInitialMarginExport(
                            contractid, processDate,entry.getId());
                    allEvents.add(newEvent);
                }

            }

            try {
                // publish all events
                DSConnection.getDefault().getRemoteTrade().saveAndPublish(allEvents);
            } catch (RemoteException ex) {
                Log.error(this, "Couldn't publish the MarginCallQef Events: " + ex.getMessage());
                return false;
            }

        } else {
            return false;
        }
        return true;
    }

    @Override
    public String getTaskInformation() {
        return "Export to QEF";
    }


}
