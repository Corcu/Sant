/**
 *
 */
package calypsox.tk.util;

import calypsox.tk.collateral.manager.worker.impl.RePriceTaskWorker;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.CollateralManager;
import com.calypso.tk.collateral.MarginCallDetailEntry;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralContext;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.MarginCallReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.santander.collateral.util.email.EmailMessage;
import com.santander.collateral.util.email.EmailSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Custom margin call scheduled task to calculated and publish notifications
 * events
 *
 * @author aela
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ScheduledTaskSANT_MARGIN_CALL extends ScheduledTask {

    private static final long serialVersionUID = 1L;

    public static final String ATTRIBUTE_SEND_PORTFOLIO = "Send Portfolio";
    public static final String CALCULATION_RESULT_FILE = "Calculation result file";
    public static final String ATTRIBUTE_SEND_RECONCILIATION = "Send Reconciliation";
    public static final String ATTRIBUTE_MCE_REPORT_TEMPLATE = "Entries template name";
    public static final String ATTRIBUTE_CALCULATE = "Calculate";
    //GSM 21/07/16 - ilas fix
    public static final String ATTRIBUTE_NEXT_WORKING_DAY = "Next Business if Non-Working";
    public static final String ATTRIBUTE_SEND_NOTIFICATION = "Send Notification";
    public static final String ATTRIBUTE_EMAIL_ADDRESS_TO = "Send Email to";
    public static final String ATTRIBUTE_PRICE_CHANGED_CONRTACTS = "Calculate only modified MC";
    public static final String ATTRIBUTE_EMAIL_DOMINE_VALUE = "emails from domain value";
    // BAU 5.2.0 - Temporary attribute to enable netted position fix
    public static final String ATTRIBUTE_ENABLE_NP_FIX = "Enable CM netted position FIX";
    protected List<String> processErrorLogs = new ArrayList<String>();
    protected List<CalculationTracker> processTracks = Collections.synchronizedList(new ArrayList<CalculationTracker>());
    public static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
    public static final String EMAIL_SEPARATOR = ";";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
    protected int nbKoContracts = 0;
    protected int nbOkContracts = 0;

    // Calculation CSA Facade
    private static final String CSA_FACADE = "CONTRACTS_CSA";

    //Optimization 13/11/2017
    private CollateralContext _collateralContext;

    // private List<CalculationTracker> calculationLogs = new
    // ArrayList<CalculationTracker>();

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {

        StringBuffer scheduledTaskExecLogs = new StringBuffer();
        Task task = new Task();
        task.setObjectLongId(getId());
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setNewDatetime(getValuationDatetime());
        task.setUnderProcessingDatetime(getDatetime());
        task.setUndoTradeDatetime(getUndoDatetime());
        task.setDatetime(getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setSource(getType());

        // *******************
        // * Launch the main handling service
        // *******************
        try {

            boolean handlingOk = performCalculation(ds, scheduledTaskExecLogs);
            if (handlingOk) {
                task.setComment(scheduledTaskExecLogs.toString());
                task.setEventType("EX_" + BOException.INFORMATION);
            } else {
                task.setComment(scheduledTaskExecLogs.toString());
                task.setEventType("EX_" + BOException.EXCEPTION);
            }
            // get the email address list from the domainValue
            // MCCalculationLogRecipients
            String emailsListDomainName = getAttribute(ATTRIBUTE_EMAIL_DOMINE_VALUE);
            List<String> to = new ArrayList<String>();
            if (!Util.isEmpty(emailsListDomainName)) {
                to = getCalculationRecipientsEmails(ds, emailsListDomainName);
            }

            if (Util.isEmpty(to)) {
                to = new ArrayList<String>();
            }
            // add the scheduledTask attribute emails
            String emails = getAttribute(ATTRIBUTE_EMAIL_ADDRESS_TO);
            if (!Util.isEmpty(emails)) {
                to.addAll(Arrays.asList(emails.split(";")));
            }

            if (!Util.isEmpty(to)) {
                String calculationDate = dateFormat.format(new Date());
                String logFileName = "MarginCallCalculation_" + getAttribute(ATTRIBUTE_MCE_REPORT_TEMPLATE) + "_"
                        + calculationDate + ".csv";
                String logFilePath = getAttribute(CALCULATION_RESULT_FILE);
                if (!Util.isEmpty(logFilePath)) {
                    logFileName = logFilePath + getAttribute(ATTRIBUTE_MCE_REPORT_TEMPLATE) + "_" + calculationDate
                            + ".csv";
                }
                EmailMessage email = new EmailMessage();
                // List<String> racArray =
                // Arrays.asList(getAttribute(ATTRIBUTE_EMAIL_ADDRESS_TO).split(EMAIL_SEPARATOR));
                email.setTo(to);
                email.setFrom(DEFAULT_FROM_EMAIL);
                email.setSubject("Margin Calls (" + getAttribute(ATTRIBUTE_MCE_REPORT_TEMPLATE)
                        + ") Calculation result for " + calculationDate);

                File logFile = flushLogs(this.processTracks, logFileName);

                String calculationStatus = "Calculation successfuly finished:<br>";
                if (this.nbKoContracts > 0) {
                    calculationStatus = "Calculation finished with errors:<br> ";
                }
                calculationStatus += " For more details, please find attached the calculation result file.<br>Regards,";

                if (logFile != null) {
                    List<String> logAttachement = new ArrayList<String>();
                    logAttachement.add(logFile.getAbsolutePath());
                    email.addAttachment(logAttachement);
                }
                email.setText(calculationStatus);
                EmailSender.send(email);
            }
            task.setCompletedDatetime(new JDatetime());
            task.setStatus(Task.NEW);

            TaskArray v = new TaskArray();
            v.add(task);
            getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(v, 0, null);
        } catch (Exception e) {
            Log.error(this, e);
        }
        return true;
    }

    private File flushLogs(List<CalculationTracker> processTracks, String fileName) throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(fileName, true);
            fileWriter.write("Contract id; calculation status;\n");
            for (CalculationTracker log : this.processTracks) {
                StringBuffer processDetailedResult = new StringBuffer("");
                if (log.getCalculationStatus() == CalculationTracker.OK) {
                    this.nbOkContracts += 1;
                } else {
                    this.nbKoContracts += 1;
                }
                processDetailedResult.append(log.getContractId());
                processDetailedResult.append(";");
                processDetailedResult.append(log.getCalculationComment());
                processDetailedResult.append(";");
                processDetailedResult.append("\n");
                fileWriter.write(processDetailedResult.toString());
            }

        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
        return new File(fileName);
    }

    /**
     * @param ds
     * @param scheduledTaskExecLogs
     * @return true if the tasks were handled without errors.
     */
    private boolean performCalculation(DSConnection ds, StringBuffer scheduledTaskExecLogs) {
        try {
            // load the margin call entries using the scheduledTask template
            MarginCallReport marginCallReport = new MarginCallReport();

            marginCallReport.setPricingEnv(ds.getRemoteMarketData().getPricingEnv(getPricingEnv()));
            // MarginCallReportTemplate
            MarginCallReportTemplate template = (MarginCallReportTemplate) DSConnection.getDefault()
                    .getRemoteReferenceData()
                    .getReportTemplate("MarginCall", getAttribute(ATTRIBUTE_MCE_REPORT_TEMPLATE));

            if (template != null) {
                // get the offset to apply from the collateral context
                _collateralContext = ServiceRegistry.getDefaultContext();//Optimization 13/11/2017
                int calculationOffSet = _collateralContext.getValueDateDays();//Optimization 13/11/2017
                if (calculationOffSet != 0) {
                    setValDateOffset(calculationOffSet);
                }

                //GSM 21/07/16 - ilas fix
                JDate processDate = JDate.valueOf(getValuationDatetime(false));

                if (Util.isTrue(getAttribute(ATTRIBUTE_NEXT_WORKING_DAY), false)) {

                    //default system as calendar
                    String calendar = "SYSTEM";
                    if (!Util.isEmpty(super.getHolidays())) {

                        calendar = super.getHolidays().firstElement();
                        Log.info(this, "Using Calendar " + calendar);
                    }

                    if (processDate.isWeekEndDay()) {
                        processDate = processDate.addBusinessDays(1, Util.string2Vector(calendar));
                        Log.info(this, getAttribute(ATTRIBUTE_NEXT_WORKING_DAY) + " attribute is activated and is NON-Working day. Process date moved to " + processDate);
                    }
                }

                //AAP MIG 14.4
                template.put(MarginCallReportTemplate.PROCESS_DATE, processDate);
                template.put(MarginCallReportTemplate.IS_VALIDATE_SECURITIES, Boolean.FALSE);
                template.put(MarginCallReportTemplate.IS_CHECK_ALLOCATION_POSITION, Boolean.FALSE);
                // Calculation CSA & CSA FACADE
                if (getAttribute(ATTRIBUTE_MCE_REPORT_TEMPLATE).equals(CSA_FACADE)) {
                    template.put(MarginCallReportTemplate.CONTRACT_TYPES, "CSA,CSA_FACADE");
                }

            }
            marginCallReport.setReportTemplate(template);
            marginCallReport.setFilterSet(getTradeFilter());
            marginCallReport.setUndoDatetime(getUndoDatetime());

            // load the report
            Vector errors = new Vector();
            ExecutionContext context = ExecutionContext.getInstance(_collateralContext,
                    ServiceRegistry.getDefaultExposureContext(), template);//Optimization 13/11/2017
            final CollateralManager marginCallManager = CollateralManager.getInstance(context);
            List<MarginCallEntry> entries = marginCallManager.createEntries(context.getFilter(), errors);

            appendErrors(scheduledTaskExecLogs, errors);

            CollateralTaskWorker rePriceTaskWorker = new RePriceTaskWorker(context,entries);
            // CollateralTaskWorker.getInstance(CollateralTaskWorker.TASK_REPRICE, context, entries);
            rePriceTaskWorker.process();

        } catch (Exception e) {
            this.processErrorLogs.add("Unexpected error while calculating. The reason is:  " + e.getMessage());
            scheduledTaskExecLogs.append("Unexpected error while calculating. The reason is:  " + e.getMessage());
            Log.error(this, e);
            return false;
        }
        return true;
    }

    /**
     * Append the list of errors to the given StrigBuffer
     *
     * @param scheduledTaskExecLogs
     * @param errors
     */
    private void appendErrors(StringBuffer scheduledTaskExecLogs, List<String> errors) {
        if ((errors != null) && (errors.size() > 0)) {
            this.processErrorLogs.addAll(errors);
            for (int i = 0; i < errors.size(); i++) {
                scheduledTaskExecLogs.append(errors.get(i));
            }
        }
    }

    /**
     * Append the list of errors to the given StrigBuffer
     *
     * @param scheduledTaskExecLogs
     * @param errors
     */
    @SuppressWarnings("unused")
    private String errorsToString(List<String> errors) {
        StringBuffer errorsString = new StringBuffer("");
        if ((errors != null) && (errors.size() > 0)) {
            for (int i = 0; i < errors.size(); i++) {
                if (errorsString.length() > 0) {
                    errorsString.append(", ");
                }
                errorsString.append(errors.get(i));
            }
        }
        return errorsString.toString();
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();

        attributeList.add(attribute(ATTRIBUTE_MCE_REPORT_TEMPLATE));
        attributeList.add(attribute(CALCULATION_RESULT_FILE));
        //GSM 21/07/16 - ilas fix
        attributeList.add(attribute(ATTRIBUTE_NEXT_WORKING_DAY).booleanType());
        attributeList.add(attribute(ATTRIBUTE_EMAIL_ADDRESS_TO));
        attributeList.add(attribute(ATTRIBUTE_PRICE_CHANGED_CONRTACTS).booleanType());
        attributeList.add(attribute(ATTRIBUTE_EMAIL_DOMINE_VALUE));
        attributeList.add(attribute(ATTRIBUTE_ENABLE_NP_FIX).booleanType());
        return attributeList;
    }
    //
    // @Override
    // public Vector getDomainAttributes() {
    // Vector attributes = new Vector();
    // attributes.add(ATTRIBUTE_MCE_REPORT_TEMPLATE);
    // attributes.add(CALCULATION_RESULT_FILE);
    // // attributes.add(ATTRIBUTE_SEND_PORTFOLIO);
    // // attributes.add(ATTRIBUTE_SEND_RECONCILIATION);
    // // attributes.add(ATTRIBUTE_SEND_NOTIFICATION);
    // // attributes.add(ATTRIBUTE_CALCULATE);
    // attributes.add(ATTRIBUTE_EMAIL_ADDRESS_TO);
    // attributes.add(ATTRIBUTE_PRICE_CHANGED_CONRTACTS);
    // attributes.add(ATTRIBUTE_EMAIL_DOMINE_VALUE);
    // attributes.add(ATTRIBUTE_ENABLE_NP_FIX);
    // return attributes;
    // }
    //
    // @Override
    // public Vector getAttributeDomain(String attribute, Hashtable currentAttr)
    // {
    // if (ATTRIBUTE_SEND_PORTFOLIO.equals(attribute)) {
    // Vector attrValues = new Vector();
    // attrValues.add(Boolean.toString(true));
    // attrValues.add(Boolean.toString(false));
    // return attrValues;
    // } else if (ATTRIBUTE_SEND_NOTIFICATION.equals(attribute)) {
    // Vector attrValues = new Vector();
    // attrValues.add(Boolean.toString(true));
    // attrValues.add(Boolean.toString(false));
    // return attrValues;
    // } else if (ATTRIBUTE_CALCULATE.equals(attribute)) {
    // Vector attrValues = new Vector();
    // attrValues.add(Boolean.toString(true));
    // attrValues.add(Boolean.toString(false));
    // return attrValues;
    // } else if (ATTRIBUTE_SEND_RECONCILIATION.equals(attribute)) {
    // Vector attrValues = new Vector();
    // attrValues.add(Boolean.toString(true));
    // attrValues.add(Boolean.toString(false));
    // return attrValues;
    // } else if (ATTRIBUTE_PRICE_CHANGED_CONRTACTS.equals(attribute)) {
    // Vector attrValues = new Vector();
    // attrValues.add(Boolean.toString(true));
    // attrValues.add(Boolean.toString(false));
    // return attrValues;
    // } else if (ATTRIBUTE_ENABLE_NP_FIX.equals(attribute)) {
    // Vector attrValues = new Vector();
    // attrValues.add(Boolean.toString(true));
    // attrValues.add(Boolean.toString(false));
    // return attrValues;
    // }
    // return super.getAttributeDomain(attribute, currentAttr);
    // }

    @Override
    public boolean isValidInput(Vector messages) {
        boolean isValid = super.isValidInput(messages);
        String templateName = getAttribute(ATTRIBUTE_MCE_REPORT_TEMPLATE);
        if (Util.isEmpty(templateName)) {
            isValid = false;
            messages.addElement("entries template name should not be empty");
        }
        return isValid;
    }

    @Override
    public String getTaskInformation() {
        return "ScheduledTask to calculate and publish margin call notifications";
    }

    // class CalculationTracker {
    // public static final int OK = 0;
    // public static final int KO = 1;
    //
    // int contractId;
    // int calculationStatus;
    // String calculationComment;
    //
    // public CalculationTracker(int contractId, int calculationStatus, String
    // calculationComment) {
    // this.contractId = contractId;
    // this.calculationStatus = calculationStatus;
    // this.calculationComment = calculationComment;
    // }
    //
    // public String getStatusAsString() {
    // return this.calculationStatus == OK ? "OK" : "KO";
    // }
    //
    // /**
    // * @return the contractId
    // */
    // public int getContractId() {
    // return this.contractId;
    // }
    //
    // /**
    // * @param contractId
    // * the contractId to set
    // */
    // public void setContractId(int contractId) {
    // this.contractId = contractId;
    // }
    //
    // /**
    // * @return the calculationStatus
    // */
    // public int getCalculationStatus() {
    // return this.calculationStatus;
    // }
    //
    // /**
    // * @param calculationStatus
    // * the calculationStatus to set
    // */
    // public void setCalculationStatus(int calculationStatus) {
    // this.calculationStatus = calculationStatus;
    // }
    //
    // /**
    // * @return the calculationComment
    // */
    // public String getCalculationComment() {
    // return this.calculationComment;
    // }
    //
    // /**
    // * @param calculationComment
    // * the calculationComment to set
    // */
    // public void setCalculationComment(String calculationComment) {
    // this.calculationComment = calculationComment;
    // }
    // }

    /**
     * @param entry
     * @return true if this margin call needs to be repriced because of a change
     *         in the underlying trades (new trade, trade not priced, trade
     *         updated...)
     */
    boolean isEntryNeedToBePriced(MarginCallEntry entry) {
        boolean toReprice = false;
        List<MarginCallDetailEntry> details = entry.getDetailEntries();
        if (!Util.isEmpty(details)) {
            for (MarginCallDetailEntry detail : details) {
                if (!MarginCallDetailEntry.STATUS_CALCULATED.equals(detail.getStatus())) {
                    toReprice = true;
                    break;
                }
            }
        }
        return toReprice;
    }

    boolean priceAllContracts() {
        boolean priceAll = true;
        String priceOnlyWhenNeeded = getAttribute(ATTRIBUTE_PRICE_CHANGED_CONRTACTS);
        if (!Util.isEmpty(priceOnlyWhenNeeded)) {
            priceAll = !Boolean.parseBoolean(priceOnlyWhenNeeded);
        }

        return priceAll;

    }

    public List<String> getCalculationRecipientsEmails(DSConnection dsConn, String domainName) {

        List<String> emailRecipients = new ArrayList<>();

        Vector emailsRecs = LocalCache.getDomainValues(dsConn, domainName);
        if (!Util.isEmpty(emailsRecs)) {
            emailRecipients.addAll(emailsRecs);
        }
        return emailRecipients;
    }
}
