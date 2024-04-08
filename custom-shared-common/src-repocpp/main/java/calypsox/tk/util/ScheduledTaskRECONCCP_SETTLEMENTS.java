package calypsox.tk.util;

import calypsox.repoccp.MTSPlatformReferenceHandler;
import calypsox.repoccp.model.ReconCCP;
import calypsox.repoccp.model.ReconCCPMatchingResult;
import calypsox.repoccp.model.lch.LCHSettlement;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TransferArray;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static calypsox.repoccp.ReconCCPConstants.*;
import static calypsox.repoccp.ReconCCPUtil.*;

/**
 * ScheduledTaskRECONCCP_SETTLEMENTS
 *
 * @author x854118
 */
public class ScheduledTaskRECONCCP_SETTLEMENTS extends ScheduledTask {

    private static final long serialVersionUID = 7492977099695580965L;

    /**
     * ST Parameter, name of the XML file that contains external trades to recon
     */
    public static final String FILE_NAME = "File Name";
    /**
     * ST Parameter, path of the XML file that contains external trades to recon
     */
    public static final String FILE_PATH = "File Path";
    /**
     * ST Parameter, path of the XLM file that contains file order
     */
    public static final String ORDER_FILES_BY = "Order Files By";
    /**
     * ST Parameter, true if execute action to move next status
     */
    public static final String MOVE_NEXT_STATUS = "Move Next Status";
    /**
     * ST Parameter, workflow bond action
     */
    public static final String WF_BOND_ACTION = "WF Bond Action";
    /**
     * ST Parameter, workflow repo action
     */
    public static final String WF_REPO_ACTION = "WF Repo Action";
    /**
     * ST Parameter, workflow repo action, if product family is none
     */
    public static final String WF_ALL_ACTION = "WF ALL Action";

    /**
     * ST Parameter, Transfer Report Template Name
     */
    public static final String TEMPLATE_NAME = "Template Name";

    /**
     * ST Parameter, Transfer Report Counterparty
     */
    public static final String COUNTERPARTY = "CounterParty";


    /**
     * Reactive processing is not yet even fully designed, so for now, imperative calls are enough
     */
    protected boolean process(DSConnection ds, PSConnection ps) {
        String fileName = getAttribute(FILE_NAME);
        String filePath = getAttribute(FILE_PATH);
        String orderBy = getAttribute(ORDER_FILES_BY);

        try {
            //Read file and store results in list of ReconCCP interface objects
            List<ReconCCP> fileObjects = readAndParseFile(fileName, filePath, orderBy);

            //Load trades from calypso with trade filter
            //List<Trade> calypsoTradesFromFilter = ReconCCPUtil.loadAndFilterTrades(ds, this.getValuationDatetime(), this._timeZone, this._tradeFilter, this._filterSet);

            if (fileObjects != null) {
                List<LCHSettlement> settlements = extractSettlements(fileObjects);

                //Try to match trades and then their transfers with the file transfers
                List<ReconCCPMatchingResult> matchingResults = matchSettlements(settlements);

                //Post process, create tasks and assign keywords to transfers and save them
                postProcessResult(matchingResults);
            }

            return true;
        } catch (FileNotFoundException | CalypsoServiceException | MarketDataException e) {
            Log.error(this, e.getCause());
        }

        return false;
    }

    @Override
    public String getTaskInformation() {
        return "Runs the CCP RECON process taking the file from attributes.";
    }

    /**
     * Try to match the external file objects to calypso objects and generate all the matched or unmatched results
     */
    private List<ReconCCPMatchingResult> matchSettlements(List<LCHSettlement> settlements) throws CalypsoServiceException, MarketDataException {
        List<ReconCCPMatchingResult> matchingResults = new ArrayList<>();
        TransferArray processedTransfers = new TransferArray();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        JDate valuationDate = this.getValuationDatetime().getJDate(TimeZone.getDefault());

        for (LCHSettlement settlement : settlements) {
            JDate jd;
            try {
                jd = JDate.valueOf(sdf.parse(settlement.getIntendedSettlementDate()));
            } catch (ParseException e) {
                //Default value ST Valuation date + 1, so it does not pass the filter
                jd = valuationDate.addDays(1);
                Log.error(this, e.getCause());
            }

            if (jd.before(valuationDate) || jd.equals(valuationDate)) {
                String settlementReferenceInstructed = settlement.getSettlementReferenceInstructed();
                BOTransfer xfer = getTransferBySettlementReferenceInstructedAndValDate(settlementReferenceInstructed,
                        valuationDate);
                if (xfer != null && settlement.matchReference(xfer)) {
                    ReconCCPMatchingResult result = settlement.match(xfer);
                    matchingResults.add(result);
                    processedTransfers.add(xfer);
                }
            }
        }

        String reportTemplate = getAttribute(TEMPLATE_NAME);
        String cptyCode = getAttribute(COUNTERPARTY);


        Collection<BOTransfer> allTransfers = !Util.isEmpty(reportTemplate) ? loadTemplate(reportTemplate) : getAllTransfersWithSettlementReferenceInstructed(getDSConnection(), valuationDate);
        if (allTransfers!=null) {
            allTransfers.removeAll(processedTransfers);
            //Move not file xfers to SETTLED

            if (getProcessingOrg() != null || !Util.isEmpty(cptyCode)) {
                LegalEntity cpty = Util.isEmpty(cptyCode) ? null : BOCache.getLegalEntity(getDSConnection(), cptyCode);
                allTransfers = allTransfers.stream().filter(x ->
                        (getProcessingOrg() == null || x.getProcessingOrg() == getProcessingOrg().getId())
                                && (cpty == null || cpty.getId() == x.getOriginalCptyId())).collect(Collectors.toList());
            }
            processUnreferencedTransfers(allTransfers);
        }
        return matchingResults;
    }

    private Collection<BOTransfer> loadTemplate(String templateName) throws CalypsoServiceException, MarketDataException {
        final ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
                .getReportTemplate(ReportTemplate.getReportName("Transfer"), templateName);
        if (!Util.isEmpty(getHolidays())) {
            template.setHolidays(getHolidays());
        }
        Report transferReport = Report.getReport("Transfer");
        if (!Util.isEmpty(getPricingEnv())) {
            PricingEnv pe =getDSConnection().getRemoteMarketData().getPricingEnv(getPricingEnv(), getValuationDatetime());
            if (pe == null) {
                String err = String.format("Failed to load Pricing Env %s.", getPricingEnv());
               throw new MarketDataException(err);
            }
            transferReport.setPricingEnv(pe);
        }

        transferReport.setValuationDatetime(getValuationDatetime());
        transferReport.setReportTemplate(template);
        template.setValDate(getValuationDatetime().getJDate(getTimeZone()));

        Vector<String> errors = new Vector<>();
        DefaultReportOutput reportOutput = transferReport.load(errors);
        if (!Util.isEmpty(errors)) {
            errors.forEach(e->Log.error(this, e));
           return null;
        }
        if (reportOutput ==null) {
            Log.error(this, "Error loading Transfer Report Template %s, null returned.");
            return null;
        }
        return reportOutput.getRows() != null? Arrays.stream(reportOutput.getRows()).map(r->(BOTransfer)r.getProperty(ReportRow.DEFAULT)).collect(Collectors.toList()):Collections.emptyList();
    }

    /**
     * Get the BOTransfer by valuation date and SettlementReferenceInstructed if not
     * MTS or SettlementReferenceInstructed2 if MTS
     *
     * @param settlementReferenceInstructed the settlementReferenceInstructed in file
     * @param valuationDate                 the ST valuation date
     * @return the BOTransfer
     */
    private BOTransfer getTransferBySettlementReferenceInstructedAndValDate(String settlementReferenceInstructed,
                                                                            JDate valuationDate) {
        if (!Util.isEmpty(settlementReferenceInstructed) && valuationDate != null) {
            TransferArray ta = getTransfersByAttribute(this.getDSConnection(), XFER_ATTR_SETTLEMENT_REF_INST,
                    settlementReferenceInstructed, valuationDate);
            if (Util.isEmpty(ta)) {
                //MTS
                ta = getTransfersByAttribute(this.getDSConnection(), XFER_ATTR_SETTLEMENT_REF_INST_2,
                        settlementReferenceInstructed, valuationDate);
            }
            return !Util.isEmpty(ta) && ta.size() == 1 ? ta.get(0) : null;
        }
        return null;
    }

    /**
     * Save transfers that are not in the settlements file
     *
     * @param unprocessedXfers the list of transfers
     */
    private void processUnreferencedTransfers(Collection<BOTransfer> unprocessedXfers) {
        if (unprocessedXfers != null) {
            unprocessedXfers = new TransferArray(new HashSet<>(unprocessedXfers));
            for (BOTransfer transfer : unprocessedXfers) {
                saveTransferAttribute(this.getDSConnection(), transfer.getLongId(), XFER_ATTR_RECON_SETTLEMENTS_EOD, "OK");
            }
            if (!Util.isEmpty(getAttribute(MOVE_NEXT_STATUS)) && getBooleanAttribute(MOVE_NEXT_STATUS)) {
                moveTransfersNextStatus(this.getDSConnection(), new ArrayList<>(unprocessedXfers), getAttribute(WF_BOND_ACTION),
                        getAttribute(WF_REPO_ACTION), getAttribute(WF_ALL_ACTION));
            }
        }
    }


    /**
     * Create tasks for each:
     * - Unmatched trade from file
     * - Unmatched trade from calypso
     * - Unmatched transfer from file
     * - Unmatched transfer from Calypso
     * - Matched transfer but any of the recon fields not matched
     * <p>
     * Update keywords:
     * - BuyerSellerReference keyword with its value from file for all matched trades
     * - SettlementReferenceInstructed keyword with value OK for matched calypso trades and KO for unmatched calypso trades
     */
    private void postProcessResult(List<ReconCCPMatchingResult> matchingResults) {
        TaskArray taskArray = new TaskArray();
        List<BOTransfer> transferArray = new ArrayList<>();
        JDatetime jdt = new JDatetime();
        //Trade KWD updating, error task creation etc...
        for (ReconCCPMatchingResult result : matchingResults) {
            BOTransfer transfer = result.getTransfer();
            if (result.isMatched()) {
                if (result.hasWarnings()) {
                    Task matchingWarnTask = new Task();
                    matchingWarnTask.setComment(result.getTransferMatchingWarnings());
                    matchingWarnTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_SETTLEMENTS);
                    matchingWarnTask.setStatus(Task.NEW);
                    matchingWarnTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingWarnTask.setDatetime(jdt);
                    matchingWarnTask.setNewDatetime(jdt);
                    matchingWarnTask.setTradeLongId(transfer.getTradeLongId());
                    matchingWarnTask.setPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriorityDatetime(null);
                    taskArray.add(matchingWarnTask);
                }
                if (result.hasErrors()) {
                    Task matchingErrorsTask = new Task();
                    matchingErrorsTask.setComment(result.getTransferMatchingErrors());
                    matchingErrorsTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_SETTLEMENTS);
                    matchingErrorsTask.setStatus(Task.NEW);
                    matchingErrorsTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingErrorsTask.setDatetime(jdt);
                    matchingErrorsTask.setNewDatetime(jdt);
                    matchingErrorsTask.setTradeLongId(transfer.getTradeLongId());
                    matchingErrorsTask.setPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setNextPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setNextPriorityDatetime(null);
                    taskArray.add(matchingErrorsTask);
                    transfer.setAttribute(XFER_ATTR_RECON_SETTLEMENTS_EOD, RECON_KO);
                } else {
                    transfer.setAttribute(XFER_ATTR_RECON_SETTLEMENTS_EOD, RECON_OK);
                }

                transfer.setAction(Action.valueOf("UPDATE_XFER_ATTR"));
                transferArray.add(transfer);
            } else {
                if (result.hasWarnings()) {
                    Task matchingWarnTask = new Task();
                    matchingWarnTask.setComment(result.getTransferMatchingWarnings());
                    matchingWarnTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP_SETTLEMENTS);
                    matchingWarnTask.setStatus(Task.NEW);
                    matchingWarnTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingWarnTask.setDatetime(jdt);
                    matchingWarnTask.setNewDatetime(jdt);
                    matchingWarnTask.setTradeLongId(transfer.getTradeLongId());
                    matchingWarnTask.setPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriorityDatetime(null);
                    taskArray.add(matchingWarnTask);
                }
                if (result.hasErrors()) {
                    Task matchingErrorsTask = new Task();
                    matchingErrorsTask.setComment(result.getUnmatchedErrors());

                    if (transfer == null) {
                        matchingErrorsTask.setEventType(EXCEPTION_MISSING_SETTLEMENT_RECON_CCP);
                    } else {
                        matchingErrorsTask.setEventType(EXCEPTION_CALYPSO_TRANSFER_UNMATCHED_RECON_CCP);
                        matchingErrorsTask.setObjectLongId(transfer.getLongId());
                        transfer.setAttribute(TRADE_KEYWORD_RECON, RECON_KO);
                        transferArray.add(transfer);
                    }

                    matchingErrorsTask.setStatus(Task.NEW);
                    matchingErrorsTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingErrorsTask.setDatetime(jdt);
                    matchingErrorsTask.setNewDatetime(jdt);
                    taskArray.add(matchingErrorsTask);
                }
            }
        }
        try {
            this.getDSConnection().getRemoteBackOffice().saveAndPublishTasks(taskArray, 0L, null);

            for (BOTransfer transfer : transferArray) {
                String reconSettlements = transfer.getAttribute(XFER_ATTR_RECON_SETTLEMENTS_EOD);
                if (!Util.isEmpty(reconSettlements)) {
                    this.getDSConnection().getRemoteBackOffice().saveTransferAttribute(transfer.getLongId(), XFER_ATTR_RECON_SETTLEMENTS_EOD, reconSettlements);
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, e.getCause());
        }
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(FILE_NAME));
        attributeList.add(attribute(FILE_PATH));
        attributeList.add(attribute(ORDER_FILES_BY));
        attributeList.add(attribute(MOVE_NEXT_STATUS));
        attributeList.add(attribute(WF_BOND_ACTION));
        attributeList.add(attribute(WF_REPO_ACTION));
        attributeList.add(attribute(WF_ALL_ACTION));
        attributeList.add(attribute(COUNTERPARTY).domain(BOCache.getLegalEntitieNamesForRole(DSConnection.getDefault(), LegalEntity.COUNTERPARTY)));
        attributeList.add(attribute(TEMPLATE_NAME).domain(currentAttributes -> {
            Vector<ReportTemplateName> templateNames = BOCache.getReportTemplateNames(DSConnection.getDefault(), "Transfer", DSConnection.getDefault().getUser());
            return templateNames.stream().map(ReportTemplateName::getTemplateName).collect(Collectors.toList());
        }));
        return attributeList;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean isValidInput(Vector messages) {
        if (Util.isEmpty(getAttribute(FILE_NAME))) {
            messages.add("File name should be filled");
        }

        if (Util.isEmpty(getAttribute(FILE_PATH))) {
            messages.add("File path should be filled");
        }

        if (Util.isEmpty(getAttribute(ORDER_FILES_BY))) {
            messages.add(ORDER_FILES_BY + " should be filled");
        }

        if (Util.isEmpty(getAttribute(MOVE_NEXT_STATUS))) {
            messages.add(MOVE_NEXT_STATUS + " cannot be empty");
        } else {
            boolean moveNextStatus = getBooleanAttribute(MOVE_NEXT_STATUS);
            if (moveNextStatus) {
                if (Util.isEmpty(getAttribute(WF_BOND_ACTION))) {
                    messages.add(WF_BOND_ACTION + " cannot be empty");
                }
                if (Util.isEmpty(getAttribute(WF_REPO_ACTION))) {
                    messages.add(WF_REPO_ACTION + " cannot be empty");
                }
                if (Util.isEmpty(getAttribute(WF_ALL_ACTION))) {
                    messages.add(WF_ALL_ACTION + " cannot be empty");
                }
            }
        }

        if (Util.isEmpty(this._tradeFilter)) {
            messages.add("Please, fill the trade filter");
        }

        return Util.isEmpty(messages);
    }

    @Override
    public Vector<String> getAttributeDomain(String attr, Hashtable<String, String> currentAttr) {
        Vector<String> v = new Vector<>();
        if (!Util.isEmpty(attr)) {
            if (MOVE_NEXT_STATUS.equals(attr)) {
                v.add(Boolean.TRUE.toString());
                v.add(Boolean.FALSE.toString());
                return v;
            } else if (WF_REPO_ACTION.equals(attr) || WF_BOND_ACTION.equals(attr) || WF_ALL_ACTION.equals(attr)) {
                return LocalCache.getDomainValues(this.getDSConnection(), "transferAction");
            } else if (ORDER_FILES_BY.equals(attr)) {
                v.add(ORDER_BY_NAME);
                v.add(ORDER_BY_DATE);
                return v;
            }
        }
        return super.getAttributeDomain(attr, currentAttr);
    }
}