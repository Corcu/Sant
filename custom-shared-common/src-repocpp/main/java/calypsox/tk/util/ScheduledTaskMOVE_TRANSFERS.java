package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.ScheduledTask;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ScheduledTaskMOVE_TRANSFERS, given a transfer report, execute actions to change their status.
 *
 * @author Ruben Garcia
 */
public class ScheduledTaskMOVE_TRANSFERS extends ScheduledTask {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 7735191504383688251L;

    /**
     * The transfer report type name
     */
    public static final String TRANSFER_REPORT_TYPE = "Transfer";

    /**
     * The transfer report class name
     */
    private static final String TRANSFER_REPORT_CLASS = "tk.report." + TRANSFER_REPORT_TYPE + "Report";

    /**
     * ST attribute, Transfer report template name
     */
    public static final String REPORT_TEMPLATE_NAME = "REPORT TEMPLATE NAME";

    /**
     * ST attribute, WF actions
     */
    public static final String WF_ACTIONS = "Workflow Action(s)";

    /**
     * ST attribute, Xfer TD offset
     */
    public static final String XFER_TRADE_DATE_OFFSET = "Xfer TD offset";

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        List<BOTransfer> transfers = getBOTransfersFromReport(ds);
        updateTransferTradeDate(transfers);
        moveTransfers(ds, transfers);
        return super.process(ds, ps);
    }

    @Override
    public String getTaskInformation() {
        return "ScheduledTask that moves a set of transfers from a report to the following states";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        return Arrays.asList(
                attribute(REPORT_TEMPLATE_NAME).mandatory().domain(getTransferReportTemplateNames()).
                        description("Name of the transfer type report that includes the transfers whose status is to be changed"),
                attribute(WF_ACTIONS).mandatory().domainName("transferAction").multipleSelection(true).
                        description("Actions, ordered that you want to execute on the transfers of the report"),
                attribute(XFER_TRADE_DATE_OFFSET).description("If it is not empty, the transfer trade date will be updated." +
                        "The offset will be applied to the value date of the ST. It can be negative (-1), zero (0), or positive (+1).")
        );
    }

    /**
     * Update transfers Trade Date if Xfer TD offset is not empty
     *
     * @param transfers the transfer array
     */
    private void updateTransferTradeDate(List<BOTransfer> transfers) {
        if (!Util.isEmpty(transfers) && !Util.isEmpty(getAttribute(XFER_TRADE_DATE_OFFSET))) {
            JDate tradeDate = getTransferTradeDateOffset();
            if (tradeDate != null) {
                transfers.forEach(t -> t.setTradeDate(tradeDate));
            }
        }
    }

    /**
     * Get the transfer trade date apply offset
     *
     * @return the transfer trade date with offset
     */
    private JDate getTransferTradeDateOffset() {
        String offset = getAttribute(XFER_TRADE_DATE_OFFSET);
        if (!Util.isEmpty(offset) && this.getValuationDatetime() != null) {
            JDatetime valuationDatetime = this.getValuationDatetime();
            TimeZone tz = this.getTimeZone() != null ? this.getTimeZone() : TimeZone.getDefault();
            JDate valDate = valuationDatetime.getJDate(tz);
            Holiday hol = Holiday.getCurrent();
            if (offset.equals("0")) {
                return valDate;
            } else if (offset.startsWith("-")) {
                Integer v = getOffsetValue(offset);
                if (v != null) {
                    for (int i = 0; i < v; ++i) {
                        valDate = hol.previousBusinessDay(valDate, this._holidays);
                    }
                    return valDate;
                }

            } else if (offset.startsWith("+")) {
                Integer v = getOffsetValue(offset);
                if (v != null) {
                    for (int i = 0; i < v; ++i) {
                        valDate = hol.nextBusinessDay(valDate, this._holidays);
                    }
                    return valDate;
                }
            }
        }
        return null;
    }

    /**
     * Get the offset int value
     *
     * @param offset the complete offset
     * @return the offset int value
     */
    private Integer getOffsetValue(String offset) {
        String valueS = offset.substring(1);
        try {
            return Integer.parseInt(valueS);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Move the transfers to the next status
     *
     * @param dsCon     the Data Server connection
     * @param transfers the list of transfers
     */
    private void moveTransfers(DSConnection dsCon, List<BOTransfer> transfers) {
        List<String> actions = getActionsList();
        if (dsCon != null && !Util.isEmpty(transfers) && !Util.isEmpty(actions)) {
            List<BOTransfer> notProcessedTransfers = new ArrayList<>();
            for (int i = 0; i < actions.size(); i++) {
                List<BOTransfer> newVersion = new ArrayList<>();
                for (BOTransfer t : transfers) {
                    t.setAction(Action.valueOf(actions.get(i)));
                    try {
                        dsCon.getRemoteBO().save(t, 0L, "");
                        if (i + 1 < actions.size()) {
                            newVersion.add(dsCon.getRemoteBackOffice().getBOTransfer(t.getLongId()));
                        }
                    } catch (CalypsoServiceException e) {
                        notProcessedTransfers.add(t);
                        Log.warn(this, e);
                    }
                }
                transfers = newVersion;
            }
            if (!Util.isEmpty(notProcessedTransfers)) {
                Log.warn(this, "The following transfers have not been processed " +
                        "(Check the log for more details): " + notProcessedTransfers);
            }
        }
    }


    /**
     * Return a list of actions
     *
     * @return a list of actions
     */
    private List<String> getActionsList() {
        String actions = getAttribute(WF_ACTIONS);
        if (!Util.isEmpty(actions)) {
            return Arrays.asList(actions.split(","));
        }
        return null;
    }

    /**
     * Get all TransferReport template names
     *
     * @return a list of Transfer report template names
     */
    private List<String> getTransferReportTemplateNames() {
        Vector<ReportTemplateName> names = BOCache.getReportTemplateNames(this.getDSConnection(),
                ReportTemplate.getReportName(TRANSFER_REPORT_TYPE), null);
        if (!Util.isEmpty(names)) {
            return names.stream().map(ReportTemplateName::getTemplateName).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Get list of BOTransfer from report
     *
     * @param dsCon the Data Server connection
     * @return the list of BOTransfers
     */
    private List<BOTransfer> getBOTransfersFromReport(DSConnection dsCon) {
        if (dsCon != null) {
            ReportOutput out = generateReportOutput(dsCon);
            if (out instanceof DefaultReportOutput) {
                DefaultReportOutput defOut = (DefaultReportOutput) out;
                if (!Util.isEmpty(defOut.getRows())) {
                    return Arrays.stream(defOut.getRows()).filter(r -> r.getProperty(ReportRow.TRANSFER) != null).
                            map(r -> r.getProperty(ReportRow.TRANSFER)).filter(BOTransfer.class::isInstance)
                            .map(BOTransfer.class::cast).collect(Collectors.toList());
                }
            }
        }
        return null;
    }

    /**
     * Load the report output
     *
     * @param dsCon the Data Server connection
     * @return the ReportOutput
     */
    private ReportOutput generateReportOutput(DSConnection dsCon) {
        Report report = generateReport(dsCon);
        if (report != null && report.getReportTemplate() != null) {
            Vector<String> holidays = this.getHolidays();
            if (!Util.isEmpty(holidays)) {
                report.getReportTemplate().setHolidays(holidays);
            }

            if (this.getTimeZone() != null) {
                report.getReportTemplate().setTimeZone(this.getTimeZone());
            }

            return report.load(new Vector<>());
        }
        return null;
    }

    /**
     * Generate the report object to load output
     *
     * @param dsCon the Data Server connection
     * @return the Report object
     */
    private Report generateReport(DSConnection dsCon) {
        String templateName = this.getAttribute(REPORT_TEMPLATE_NAME);
        Report report = null;
        if (!Util.isEmpty(templateName)) {
            try {
                report = (Report) InstantiateUtil.getInstance(TRANSFER_REPORT_CLASS, true);
            } catch (InstantiationException | IllegalAccessException e) {
                Log.error(this, "Cannot instantiate class " + TRANSFER_REPORT_CLASS + "\n" + e);
                return null;
            }

            if (report != null) {
                report.setPricingEnv(getPricingEnv(dsCon));
                report.setFilterSet(getTradeFilter());
                report.setValuationDatetime(this.getValuationDatetime());
                report.setUndoDatetime(null);
                report.setForceUndo(false);
                ReportTemplate template;
                try {
                    template = dsCon.getRemoteReferenceData().getReportTemplate(ReportTemplate.
                            getReportName(TRANSFER_REPORT_TYPE), templateName);
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Cannot get report template " + templateName + "\n" + e);
                    return null;
                }
                if (template != null) {
                    report.setReportTemplate(template);
                    template.setValDate(this.getValuationDatetime().getJDate(this._timeZone));
                    template.callBeforeLoad();
                }
            }
        }
        return report;
    }

    /**
     * Get the Pricing Environment object
     *
     * @param dsCon the Data Server connection
     * @return the PricingEnv object
     */
    private PricingEnv getPricingEnv(DSConnection dsCon) {
        String env = getPricingEnv();
        if (!Util.isEmpty(env) && dsCon != null) {
            try {
                return dsCon.getRemoteMarketData().getPricingEnv(env, this.getValuationDatetime());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Cannot get pricing environment " + env + "\n" + e);
            }
        }
        return null;
    }


}
