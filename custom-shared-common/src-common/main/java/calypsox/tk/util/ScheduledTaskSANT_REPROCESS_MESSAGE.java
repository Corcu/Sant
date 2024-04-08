package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ScheduledTaskSANT_REPROCESS_MESSAGE extends ScheduledTask {


    private static final String ATTR_TEMPLATE_NAME = "Message Report Template";
    private static final String ATTR_ACTION = "Apply Action";


    @Override
    public String getTaskInformation() {
        return "Applies action to messages selected by Message Report Template.";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {

        return Arrays.asList(
                attribute(ATTR_TEMPLATE_NAME).domain(currentAttributes -> {
                    Vector<ReportTemplateName> templateNames = BOCache.getReportTemplateNames(DSConnection.getDefault(), "Message", DSConnection.getDefault().getUser());
                    return templateNames.stream().map(ReportTemplateName::getTemplateName).collect(Collectors.toList());
                }).mandatory(),
                attribute(ATTR_ACTION).domainName("messageAction")
        );

    }


    public boolean process(DSConnection ds, PSConnection ps) {
        boolean ret = true;
        if (this._publishB || this._sendEmailB) {
            ret = super.process(ds, ps);
        }

        TaskArray v = new TaskArray();
        Task task = new Task();
        task.setObjectLongId(this.getId());
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setNewDatetime(this.getDatetime());
        task.setUnderProcessingDatetime(this.getDatetime());
        task.setUndoTradeDatetime(this.getDatetime());
        task.setDatetime(this.getDatetime());
        task.setPriority(1);
        task.setId(0L);
        task.setStatus(0);
        task.setEventType("EX_INFORMATION");
        task.setSource(this.getType());
        task.setAttribute("ScheduledTask Id=" + this.getId());
        task.setComment(this.toString());
        v.add(task);
        boolean error = false;
        if (this._executeB) {
            try {
                error = !reprocessMessages(ds);

            } catch (Exception e) {
                Log.error(LOG_CATEGORY, e);
                task.setComment(String.format("%s: %s.", e.getClass().getSimpleName(), e.getMessage()));
                error = true;
            }
        }

        try {
            if (error)
                task.setEventType("EX_EXCEPTION");
            getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(v, 0L, null);
        } catch (Exception e) {
            Log.error(this, e);
            error = true;
        }

        return ret && !error;
    }

    private boolean reprocessMessages(DSConnection ds) throws CalypsoServiceException, MarketDataException {

        ReportRow[] rowsToReprocess = loadTemplate(getAttribute(ATTR_TEMPLATE_NAME));
        if (Util.isEmpty(rowsToReprocess)) {
            Log.warn(this, String.format("Report template %s returned no messages, nothing to process.", getAttribute(ATTR_TEMPLATE_NAME)));
            return true;
        } else {

            String actionStr = getAttribute(ATTR_ACTION);

            Action action = Util.isEmpty(actionStr) ? Action.UPDATE : Action.valueOf(actionStr);
            List<String> errors = Arrays.stream(rowsToReprocess).map(r -> {
                BOMessage m = null;
                try {
                    m = r.getProperty(ReportRow.DEFAULT);
                    Trade t = r.getProperty(ReportRow.TRADE);
                    if (t == null && m.getTradeLongId() > 0) {
                        t = ds.getRemoteTrade().getTrade(m.getTradeLongId());
                    }
                    BOTransfer xfer = r.getProperty(ReportRow.TRANSFER);
                    if (xfer == null && m.getTransferLongId() > 0) {
                        xfer = ds.getRemoteBO().getBOTransfer(m.getTransferLongId());
                    }

                    return reprocessMessage(m, xfer, t, action, ds);
                } catch (CalypsoServiceException e) {
                    return String.format("Error processing message %s. %s, %s.", m, e.getClass().getSimpleName(), e.getMessage());
                }

            }).filter(e -> !Util.isEmpty(e)).collect(Collectors.toList());

            if (!Util.isEmpty(errors))
                errors.forEach(e -> Log.error(this, e));

            return Util.isEmpty(errors);
        }
    }

    private String reprocessMessage(BOMessage m, BOTransfer transfer, Trade trade, Action act, DSConnection ds) {
        try {
            BOMessage msgClone = (BOMessage) m.clone();
            msgClone.setAction(act);
            if (BOMessageWorkflow.isMessageActionApplicable(msgClone, transfer, trade, act, ds)) {

                long msgId = ds.getRemoteBO().save(msgClone, 0L, getTaskName());
                if (msgId <= 0)
                    return String.format("Error saving message %s, invalid message id of %d returned.", m, msgId);
                else
                    return null;

            }
            return null;
        } catch (Exception e) {
            return String.format("Error applying action %s to message %s. %s, %s.", act, m, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private ReportRow[] loadTemplate(String templateName) throws CalypsoServiceException, MarketDataException {
        final ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
                .getReportTemplate(ReportTemplate.getReportName("Message"), templateName);
        if (!Util.isEmpty(getHolidays())) {
            template.setHolidays(getHolidays());
        }
        Report messageReport = Report.getReport("Message");
        if (!Util.isEmpty(getPricingEnv())) {
            PricingEnv pe = getDSConnection().getRemoteMarketData().getPricingEnv(getPricingEnv(), getValuationDatetime());
            if (pe == null) {
                String err = String.format("Failed to load Pricing Env %s.", getPricingEnv());
                throw new MarketDataException(err);
            }
            messageReport.setPricingEnv(pe);
        }

        messageReport.setValuationDatetime(getValuationDatetime());
        messageReport.setReportTemplate(template);
        template.setValDate(getValuationDatetime().getJDate(getTimeZone()));

        Vector<String> errors = new Vector<>();
        DefaultReportOutput reportOutput = messageReport.load(errors);
        if (!Util.isEmpty(errors)) {
            errors.forEach(e -> Log.error(this, e));
            return null;
        }
        if (reportOutput == null) {
            Log.error(this, "Error loading Transfer Report Template %s, null returned.");
            return null;
        }
        return reportOutput.getRows();
    }
}
