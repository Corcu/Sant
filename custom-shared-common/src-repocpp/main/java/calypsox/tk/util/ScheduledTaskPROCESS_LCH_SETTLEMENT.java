package calypsox.tk.util;

import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.ReconCCPReportHeader;
import calypsox.repoccp.model.lch.LCHSettlement;
import calypsox.repoccp.model.lch.LCHSettlementNode;
import calypsox.repoccp.model.lch.settlement.LCHSettlementReport;
import calypsox.repoccp.reader.LCHSettlementStaxReader;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.SeedAllocSQL;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.*;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static calypsox.repoccp.ReconCCPConstants.*;
import static com.calypso.tk.util.ParallelExecutionException.Type.EPILOGUE;
import static com.calypso.tk.util.ParallelExecutionException.Type.INIT;

public class ScheduledTaskPROCESS_LCH_SETTLEMENT extends ScheduledParallelTask<Boolean, ScheduledTaskPROCESS_LCH_SETTLEMENT.SettlementReport> {
    /**
     * **************ST Parameters*******************
     */
    // CCP reconciliation file name
    public static final String FILE_NAME = "File Name";
    //CCP reconciliation file location
    public static final String FILE_PATH = "File Path";

    // Report file name
    public static final String REPORT_FILE_NAME = "Report File Name";
    //CReport file location
    public static final String REPORT_FILE_PATH = "Report File Path";

    public static final String TRANSFER_REPORT_TEMPLATE = "Transfer Report Template";

    //WF action to apply to matched xfers
    public static final String WF_ACTION = "WF Action";

    public static final String FAIL_WF_ACTION = "fail WF Action";

    public static final String CREATE_TASKS = "Create Tasks";

    private static final String TOLERANCE_TYPE = "Tolerance Type";

    private static final String COUNTERPARTY = "CounterParty";

    private static final String EXCLUDE_SETTLEMENT_TYPES = "Exclude LCH Settlement Types";

    //Thread pool size
    public static final String THREAD_POOL_SIZE = "Thread Pool Size";

    public static final String REPORT_HEADER = "Payment,Settlement,Advised Status,Recon Status,Message";

    public static final double REPORT_NOM_TOLERANCE = 0.99;
    public static final double REPORT_CASH_TOLERANCE = 0.01;

    private static final String XFER_SELECT_CORE_SQL = "transfer_id IN (SELECT transfer_id FROM xfer_attributes WHERE attr_name = ? and attr_value = ?)" +
            " AND value_date = ? AND ext_le_id = ? AND ext_le_role = ? AND  int_le_id = ? AND int_le_role= ?";
    private static final String XFER_SELECT_SQL = XFER_SELECT_CORE_SQL + " AND transfer_status NOT IN ('CANCELED', 'SPLIT')";

    private static final String PART_SETTLE_XFER_SELECT_SQL = XFER_SELECT_CORE_SQL + " AND transfer_status  = 'SPLIT'";

    private static final String XFER_SELECT_SPAWNS_SQL = " value_date = ? AND ext_le_id = ? AND ext_le_role = ? AND  int_le_id = ? " +
            "AND int_le_role= ? AND transfer_status  != 'CANCELED' AND start_time_limit = ?";

    private Queue<File> fileQueue;

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        final Vector<String> nettingTypes = LocalCache.getDomainValues(getDSConnection(), "nettingType");
        nettingTypes.sort(Comparator.naturalOrder());
        return Arrays.asList(attribute(FILE_PATH).mandatory().description("Path to input report file."),
                attribute(FILE_NAME).mandatory().description("Input report file mask."),
                attribute(TRANSFER_REPORT_TEMPLATE).domain(currentAttributes -> {
                    Vector<ReportTemplateName> templateNames = BOCache.getReportTemplateNames(DSConnection.getDefault(), "Transfer", DSConnection.getDefault().getUser());
                    return templateNames.stream().map(ReportTemplateName::getTemplateName).collect(Collectors.toList());
                }).mandatory().description(""),
                attribute(WF_ACTION).domainName("transferAction").description("Action to apply to settled transfers, default SETTLE"),
                attribute(FAIL_WF_ACTION).domainName("transferAction").description("Action to apply to failed transfers, default no action applied."),
                attribute(REPORT_FILE_PATH).description("Path to reconciliation report file."),
                attribute(REPORT_FILE_NAME).description("Report file name."),
                attribute(COUNTERPARTY).domain(currentAttributes -> BOCache.getLegalEntitieNamesForRole(DSConnection.getDefault(), LegalEntity.COUNTERPARTY)).mandatory(),
                attribute(EXCLUDE_SETTLEMENT_TYPES).domainName("LCHSettlementType").multipleSelection(true),
                attribute(TOLERANCE_TYPE).domain(currentAttributes -> {
                    List<String> types = new ArrayList<>(Arrays.asList("Automatic", "Manual", "ReceiptMsg"));
                    types.addAll(LocalCache.getDomainValues(DSConnection.getDefault(), "leToleranceType"));
                    return types.stream().distinct().sorted().collect(Collectors.toList());
                }).mandatory().description("Legal Entity Tolerance type to use for recon."),
                attribute(CREATE_TASKS).booleanType().description("True to create exception tasks for entries failed reconciliation."),
                attribute(THREAD_POOL_SIZE).integer().mandatory());

    }

    @Override
    public boolean isValidInput(Vector<String> messages) {
        boolean result = true;
        if (getProcessingOrg() == null) {
            messages.add("Processing Org is mandatory.");
            result = false;
        }

        if (Util.isEmpty(getPricingEnv())) {
            messages.add("Pricing Environment is mandatory.");
            result = false;
        }
        return super.isValidInput(messages) && result;
    }

    public boolean process(DSConnection ds, PSConnection ps) {
        boolean ret = true;
        if (this._publishB || this._sendEmailB) {
            ret = super.process(ds, ps);
        }

        TaskArray v = new TaskArray();
        Task task = new Task();
        task.setObjectLongId(this.getId());
        task.setEventClass("Exception");
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
                error = !processSettlement(ds, ps); //
            } catch (ParallelExecutionException e) {
                Log.error(LOG_CATEGORY, e);
                task.setComment(e.getLocalizedMessage());
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

    public boolean processSettlement(DSConnection ds, PSConnection ps) throws ParallelExecutionException {
        String fileName = getAttribute(FILE_NAME);
        String filePath = getAttribute(FILE_PATH);

        StrSubstitutor sub = new StrSubstitutor(getValueMap());
        // Replace
        String filePathResolved = sub.replace(filePath);
        String fileNameResolved = sub.replace(fileName);

        File[] files = new File(filePathResolved).listFiles((FileFilter) new WildcardFileFilter(fileNameResolved, IOCase.INSENSITIVE));
        if (Util.isEmpty(files))
            throw new ParallelExecutionException(INIT, String.format("No files found by mask %s in directory %s. Nothing to process.", fileNameResolved, filePathResolved));

        fileQueue = Arrays.stream(files).sorted(Comparator.comparing(File::getName)).collect(Collectors.toCollection(LinkedList::new));

        boolean ret = true;
        while (!fileQueue.isEmpty()) {
            ret = ret && parallelRun(ds, ps);
        }
        return ret;
    }

    @Override
    public IExecutionContext createExecutionContext(DSConnection ds, PSConnection ps) throws ParallelExecutionException {
        String wfAction = getAttribute(WF_ACTION);
        Action settleAction = Util.isEmpty(wfAction) ? Action.SETTLE : Action.valueOf(wfAction);
        wfAction = getAttribute(FAIL_WF_ACTION);
        Action failAction = Util.isEmpty(wfAction) ? null : Action.valueOf(wfAction);
        LegalEntity cpty = BOCache.getLegalEntity(ds, getAttribute(COUNTERPARTY));
        if (cpty == null)
            throw new ParallelExecutionException(INIT, String.format("CounterParty Legal Entity %s not found.", getAttribute(COUNTERPARTY)));
        return new ExecutionContext(ds,
                ps,
                getProcessingOrg().getId(),
                cpty.getId(),
                fileQueue.poll(),
                settleAction,
                failAction,
                getAttribute(TOLERANCE_TYPE),
                getPricingEnv(),
                getValuationDatetime().getJDate(getTimeZone()),
                getValuationDatetime(),
                getIntegerAttribute(THREAD_POOL_SIZE, 1));
    }

    @Override
    public List<? extends Callable<SettlementReport>> split(IExecutionContext iContext) throws ParallelExecutionException {
        try {
            ExecutionContext context = (ExecutionContext) iContext;
            List<Callable<SettlementReport>> tasks = new ArrayList<>();
            LCHSettlementReport settlementReport = (new LCHSettlementStaxReader()).readSettlementReport(context.getFile());

            Collection<String> errors = new ArrayList<>();
            if (!validate(settlementReport, errors))
                throw new ParallelExecutionException(INIT, "Invalid report: " + Util.collectionToString(errors, "/n"));

            context.setSettlementReport(settlementReport);
            if (Boolean.parseBoolean(settlementReport.getHeader().getEmptyReport())) {
                Log.warn(LOG_CATEGORY, String.format("Empty report %s, nothing to process.", context.getFile().getName()));
                return tasks;
            }

            List<LCHSettlementNode> settlementNodes = buildSettlementNodes(settlementReport.getSettlements());

            int chunkSize = settlementNodes.size() / context.getThreadPoolSize();

            List<LCHSettlementNode> chunk = new ArrayList<>();
            int count = 0;
            for (LCHSettlementNode node : settlementNodes) {
                if (count > chunkSize) {
                    Callable<SettlementReport> task = new SettlementBatch(chunk, context);
                    tasks.add(task);

                    chunk = new ArrayList<>();
                    count = 0;
                }
                if (!validate(node)) {
                    Log.error(LOG_CATEGORY, "Obligations don't match netting positions.");
                    throw new ParallelExecutionException(INIT, "Obligations don't match netting positions.");
                }
                chunk.add(node);
                count++;
            }
            if (!Util.isEmpty(chunk)) {
                Callable<SettlementReport> task = new SettlementBatch(chunk, context);
                tasks.add(task);
            }
            return tasks;
        } catch (ParallelExecutionException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ParallelExecutionException(INIT, e);
        }
    }

    private boolean validate(LCHSettlementReport report, Collection<String> errors) {
        if (report.getHeader().getTotalNoOfRecords() != report.getSettlements().size()) {
            errors.add(String.format("Total number of records in report header %d does not match actual number of settlements %d.",
                    report.getHeader().getTotalNoOfRecords(), report.getSettlements().size()));
            return false;
        }
        if (!this.getValuationDatetime().getJDate(getTimeZone()).equals(report.getHeader().getBusinessDate())) {
            errors.add(String.format("Invalid report Business Date %s, Valuation Date %s expected.",
                    report.getHeader().getBusinessDate(), this.getValuationDatetime().getJDate(getTimeZone())));
            return false;
        }

        if (Boolean.parseBoolean(report.getHeader().getEmptyReport()) && !Util.isEmpty(report.getSettlements())) {
            errors.add(String.format("Non empty report marked as empty, %d actual number of settlements in the report.",
                    report.getSettlements().size()));
            return false;
        }
        return true;
    }

    private boolean validate(final LCHSettlementNode node) {
        return validate(node, new HashSet<>());
    }

    private boolean validate(final LCHSettlementNode node, Collection<String> refs) {

        if (!refs.add(node.getSettlement().getSettlementReferenceInstructed())) {
            Log.error(LOG_CATEGORY, String.format("Cyclical ref %s found.", node.getSettlement().getSettlementReferenceInstructed()));
            return false;
        }

        if (Util.isEmpty(node.getSpawns()))
            return true;

        if (Math.abs(getNominal(Collections.singletonList(node)) - getNominal(node.getSpawns())) > REPORT_NOM_TOLERANCE) {
            Log.error(LOG_CATEGORY, String.format("Node %s, sum of spawn nominal is not equal to parent nominal.", node.getSettlement().getSettlementReferenceInstructed()));
            return false;
        }

        if (Math.abs(getCash(Collections.singletonList(node)) - getCash(node.getSpawns())) > REPORT_CASH_TOLERANCE) {
            Log.error(LOG_CATEGORY, String.format("Node %s, sum of spawn nominal is not equal to parent nominal.", node.getSettlement().getSettlementReferenceInstructed()));
            return false;
        }

        return node.getSpawns().stream().allMatch(n -> {
                    if (!node.getSettlement().getSettlementReferenceInstructed().equals(n.getSettlement().getParentInstructionReference())) {
                        Log.error(LOG_CATEGORY, String.format("Invalid Parent reference %s in node %s.", n.getSettlement().getParentInstructionReference(), node.getSettlement().getSettlementReferenceInstructed()));
                        return false;
                    }
                    return validate(n, refs);
                }
        );
    }

    private static double getSecAmount(BOTransfer xfer, Function<BOTransfer, Double> getter) {
        return getSecAmount(Collections.singletonList(xfer), getter);
    }


    private static double getSecAmount(Collection<BOTransfer> xfers, Function<BOTransfer, Double> getter) {
        return xfers.stream().filter(BOTransfer::isSecurity).mapToDouble(x -> {
            double amt = Math.abs(getter.apply(x));
            return "PAY".equals(x.getPayReceive()) ? -amt : amt;
        }).sum();
    }

    private static double getCashAmount(BOTransfer xfer, Function<BOTransfer, Double> getter) {
        return getCashAmount(Collections.singletonList(xfer), getter);
    }

    private static double getCashAmount(Collection<BOTransfer> xfers, Function<BOTransfer, Double> getter) {
        return xfers.stream().mapToDouble(x -> {
            double amt = Math.abs(getter.apply(x));
            return x.isSecurity() ? "PAY".equals(x.getPayReceive()) ? amt : -amt : "PAY".equals(x.getPayReceive()) ? -amt : amt;
        }).sum();
    }

    private static double getNominal(LCHSettlementNode node) {
        return getNominal(Collections.singletonList(node));
    }

    private static double getNominal(List<LCHSettlementNode> nodes) {
        return nodes.stream().mapToDouble(n -> "LCH".equals(n.getSettlement().getBondsReceiver())
                ? -Math.abs(n.getSettlement().getNominalInstructed())
                : Math.abs(n.getSettlement().getNominalInstructed())).sum();

    }

    private static double getCash(LCHSettlementNode node) {
        return getCash(Collections.singletonList(node));
    }

    private static double getCash(List<LCHSettlementNode> nodes) {
        return nodes.stream().mapToDouble(n -> "LCH".equals(n.getSettlement().getCashReceiver())
                ? -Math.abs(n.getSettlement().getCashAmountInstructed())
                : Math.abs(n.getSettlement().getCashAmountInstructed())).sum();

    }

    private List<LCHSettlementNode> buildSettlementNodes(List<LCHSettlement> settlements) throws ParallelExecutionException {
        String settleTypes = getAttribute(EXCLUDE_SETTLEMENT_TYPES);
        Collection<String> settleTypeFilter = Util.isEmpty(settleTypes) ? null : new HashSet<>(Util.stringToList(settleTypes));

        Map<String, LCHSettlementNode> byRef = settlements.stream().map(LCHSettlement.class::cast)
                .filter(s -> settleTypeFilter == null || !settleTypeFilter.contains(s.getSettlementType()))
                .collect(Collectors.toMap(LCHSettlement::getSettlementReferenceInstructed, LCHSettlementNode::new, (k, v) -> {
                    throw new IllegalStateException(String.format("Settlement reference instructed %s.", k.getSettlement().getSettlementReferenceInstructed()));
                }));

        for (LCHSettlement lchSettlement : settlements) {

            if (!Util.isEmpty(lchSettlement.getParentInstructionReference())) {
                LCHSettlementNode parentNode = byRef.get(lchSettlement.getParentInstructionReference());

                LCHSettlementNode childNode = byRef.get(lchSettlement.getSettlementReferenceInstructed());

                if (parentNode == null) {
                    throw new ParallelExecutionException(INIT, String.format("Parent settlement for node %s node found.", lchSettlement.getParentInstructionReference()));
                }
                if (childNode == null) {
                    //cannot happen
                    throw new ParallelExecutionException(INIT, String.format("Node %s node found.", lchSettlement.getParentInstructionReference()));
                }

                if (!parentNode.addSpawn(childNode)) {
                    throw new ParallelExecutionException(INIT, String.format("Duplicated spawn settlement %s for parent %s.", childNode.getSettlement().getSettlementReferenceInstructed(), childNode.getSettlement().getParentInstructionReference()));

                }
            }
        }
        return byRef.values().stream().filter(n -> Util.isEmpty(n.getSettlement().getParentInstructionReference())).collect(Collectors.toList());
    }

    @Override
    public Boolean epilogue(List<? extends Callable<SettlementReport>> jobs, List<Future<SettlementReport>> results, IExecutionContext iContext) throws ParallelExecutionException {
        ExecutionContext context = (ExecutionContext) iContext;
        SettlementReport settleReport = settleOtherTransfers(context);

        String filePath = getAttribute(REPORT_FILE_PATH);
        String fileName = getAttribute(REPORT_FILE_NAME);

        //     fileProcessed(context);
        TaskArray tasks = new TaskArray();
        boolean createTasks = getBooleanAttribute(CREATE_TASKS);

        StrSubstitutor sub = new StrSubstitutor(getValueMap(context.getFile()));
        // Replace
        String filePathResolved = sub.replace(filePath);
        String fileNameResolved = sub.replace(fileName);
        String fullPath = filePathResolved.endsWith("\\") || filePathResolved.endsWith("/") ? filePathResolved + fileNameResolved : filePathResolved + "/" + fileNameResolved;
        try (FileOutputStream outputStream = new FileOutputStream(fullPath)) {
            outputStream.write(REPORT_HEADER.getBytes(StandardCharsets.UTF_8));
            outputStream.write('\n');
            List<SettlementReport> reports = new ArrayList<>();
            for (Future<SettlementReport> f : results) {
                reports.add(f.get());
            }
            reports.add(settleReport);
            for (SettlementReport report : reports) {
                for (SettlementResult result : report.getResults()) {
                    if (!result.getSettlementStatus().isSuccess() && createTasks)
                        tasks.add(createTask(result));

                    outputStream.write(result.toCSV().getBytes(StandardCharsets.UTF_8));
                    outputStream.write('\n');
                }
            }
        } catch (Exception e) {
            Log.error(LOG_CATEGORY, e);
            return false;
        }
        if (!tasks.isEmpty()) {
            try {
                getDSConnection().getRemoteBackOffice().saveAndPublishTasks(tasks, 0L, null);
            } catch (CalypsoServiceException e) {
                throw new ParallelExecutionException(EPILOGUE, e);
            }
        }
        return true;
    }

    private SettlementReport settleOtherTransfers(ExecutionContext context) {

        SettlementReport report = new SettlementReport();
        String reportTemplate = getAttribute(TRANSFER_REPORT_TEMPLATE);
        try {
            if (!Util.isEmpty(reportTemplate)) {

                final Collection<String> refsInstructed = context.getSettlementReport() != null && !Util.isEmpty(context.getSettlementReport().getSettlements())
                        ? context.getSettlementReport().getSettlements().stream()
                        .map(LCHSettlement::getSettlementReferenceInstructed).collect(Collectors.toSet())
                        : Collections.emptyList();

                final ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
                        .getReportTemplate(ReportTemplate.getReportName("Transfer"), reportTemplate);
                if (!Util.isEmpty(getHolidays())) {
                    template.setHolidays(getHolidays());
                }
                Report transferReport = Report.getReport("Transfer");
                if (!Util.isEmpty(getPricingEnv())) {
                    PricingEnv pe = context.getDSConnection().getRemoteMarketData().getPricingEnv(getPricingEnv(), getValuationDatetime());
                    if (pe == null) {
                        String err = String.format("Failed to load Pricing Env %s.", getPricingEnv());
                        Log.error(LOG_CATEGORY, String.format("Failed to load Pricing Env %s.", getPricingEnv()));
                        report.error(null, null, err);
                        return report;
                    }
                    transferReport.setPricingEnv(pe);
                }

                transferReport.setValuationDatetime(getValuationDatetime());
                transferReport.setReportTemplate(template);
                template.setValDate(getValuationDatetime().getJDate(getTimeZone()));

                Vector<String> errors = new Vector<>();
                DefaultReportOutput reportOutput = transferReport.load(errors);
                if (!Util.isEmpty(errors)) {
                    errors.forEach(e -> report.error(null, null, e));
                    return report;
                }

                if (reportOutput.getRows() != null) {
                    for (ReportRow row : reportOutput.getRows()) {
                        BOTransfer xferToSettle = row.getProperty(ReportRow.DEFAULT);
                        if (xferToSettle.isPayment()) {
                            if (!Util.isEmpty(xferToSettle.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST)) && !refsInstructed.contains(xferToSettle.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST))) {
                                settleTransfer(xferToSettle, row.getProperty(ReportRow.TRADE), report, context);
                            }
                        } else {
                            report.error(xferToSettle, null, "Underling transfer cannot be settled, payment transfer expected.");
                        }
                    }
                }

            }
        } catch (Exception e) {
            report.error(null, null, String.format("%s: %s.", e.getClass().getSimpleName(), e.getLocalizedMessage()));
        }
        return report;
    }

    private void settleTransfer(BOTransfer xferToSettle, Trade trade, SettlementReport report, ExecutionContext context) {
        try {
            BOTransfer cloneXfer = (BOTransfer) xferToSettle.clone();
            cloneXfer.setAction(context.getSettleAction());

            Trade t = trade == null && cloneXfer.getTradeLongId() > 0 ? context.getDSConnection().getRemoteTrade().getTrade(cloneXfer.getTradeLongId()) : trade;

            if (BOTransferWorkflow.isTransferActionApplicable(cloneXfer, t, context.getSettleAction(), context.getDSConnection())) {
                if (!Util.isEmpty(cloneXfer.getAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED))) {
                    double cashAmount = Util.stringToNumber(cloneXfer.getAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED), Locale.UK);
                    if ("SECURITY".equals(cloneXfer.getTransferType())) {
                        if (!"DAP".equals(cloneXfer.getDeliveryType())) {
                            report.settled(cloneXfer, null, "Attempt to settle cash of DFP SECURITY transfer");
                            return;
                        }
                    }
                    if (context.isSecurityPartialCash()) {
                        cloneXfer.setRealCashAmount(cashAmount);
                        cloneXfer.setAttribute("ExpectedStatus", Status.SETTLED);

                        if (context.isSecurityWriteOff()) {
                            cloneXfer.setAttribute("WriteOff", "true");
                            if (cloneXfer.getNettedTransfer())
                                cloneXfer.setAttribute("SPLITREASON", "SecurityNetting");
                        }
                        context.getDSConnection().getRemoteBO().partialSettleTransfer(cloneXfer);
                        TransferArray settledXfers = getSpawnTransfers(cloneXfer, context);
                        settledXfers.forEach(x -> {
                            if (Status.S_SETTLED.equals(x.getStatus())) {
                                if ("WRITE_OFF".equals(x.getTransferType())) {
                                    report.writeOff(x);
                                } else {
                                    report.settled(x, null, String.format("Transfer is not in settlement file - SETTLED partial cash. Parent transfer %d.", x.getParentLongId()));
                                }
                            } else {
                                report.error(x, null, String.format("Transfer is not in settlement file, partial settlement, unexpected transfer status %s, . Parent transfer %d.", x.getStatus(), x.getParentLongId()));
                            }

                        });

                        return;
                    }

                }

                if (cloneXfer.getLongId() == context.getDSConnection().getRemoteBO().save(cloneXfer, 0L, null)) {
                    report.settled(cloneXfer, null, "Transfer is not in settlement file - SETTLED");
                } else {
                    report.error(cloneXfer, null, "Error saving transfer");
                }
            }
        } catch (Exception e) {
            report.error(xferToSettle, null, String.format("Error saving transfer %s: %s.", e.getClass().getSimpleName(), e.getLocalizedMessage()));
        }
    }

    private TransferArray getSpawnTransfers(BOTransfer parentXfer, ExecutionContext context) throws CalypsoServiceException {
        return context.getDSConnection().getRemoteBO().getBOTransfers(
                "start_time_limit = ? AND orig_cpty_id = ? AND transfer_status != 'CANCELED'",
                Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.LONG, parentXfer.getLongId()), new CalypsoBindVariable(CalypsoBindVariable.INTEGER, parentXfer.getOriginalCptyId())));
    }

    @Override
    public String getTaskInformation() {
        return "Reconciles Calypso payments with LCH Settlements. Perform partial settlements of partially settles flows and settles transfer not included into the LCH Settlement Report.";
    }

    private Map<String, String> getValueMap(File file) {
        Map<String, String> valueMap = getValueMap();
        int pos = file.getName().lastIndexOf('.');
        String name = pos < 0 ? file.getName() : file.getName().substring(0, pos - 1);
        String ext = pos < 0 || file.getName().length() == pos + 1 ? "" : file.getName().substring(pos + 1);
        valueMap.put("FILE_NAME", name);
        valueMap.put("FILE_EXTENSION", ext);
        return valueMap;
    }

    private Map<String, String> getValueMap() {
        Map<String, String> valueMap = new HashMap<>();
        Date vald = this.getValuationDatetime().getJDate(getTimeZone()).getDate();
        valueMap.put("VALUATION_DATE_YYYYMMDD", (new SimpleDateFormat("yyyyMMdd")).format(vald));
        valueMap.put("VALUATION_DATE_DDMMYYYY", (new SimpleDateFormat("ddMMyyyy")).format(vald));
        valueMap.put("VALUATION_DATE_DDMMYY", (new SimpleDateFormat("ddMMyy")).format(vald));
        valueMap.put("VALUATION_DATE_YYYY-MM-DD", (new SimpleDateFormat("yyyy-MM-dd")).format(vald));
        valueMap.put("VALUATION_DATE_DD-MM-YYYY", (new SimpleDateFormat("dd-MM-yyyy")).format(vald));
        return valueMap;
    }

    private Task createTask(SettlementResult result) {
        Task task = new Task();

        task.setStatus(Task.NEW);
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setDatetime(new JDatetime());
        task.setNewDatetime(new JDatetime());
        task.setPriority(Task.PRIORITY_LOW);
        task.setNextPriority(Task.PRIORITY_LOW);
        task.setNextPriorityDatetime(null);

        if (!Util.isEmpty(result.getTransfers())) {
            BOTransfer xfer = result.getTransfers().iterator().next();
            task.setTradeLongId(xfer.getTradeLongId());
            task.setObjectLongId(xfer.getLongId());
            task.setProductId(xfer.getProductId());
            task.setObjectClassName("Transfer");
        }

        task.setComment(result.toString());
        task.setEventType(EXCEPTION_CALYPSO_TRANSFER_UNMATCHED_RECON_CCP);
        return task;
    }

    private static class ExecutionContext implements IExecutionContext {
        private final DSConnection dsCon;
        private final PSConnection psCon;
        private final File file;
        private final int poId;
        private final int cptyId;
        private final Action settleAction;
        private final Action failAction;
        private final String toleranceType;

        private final boolean isSecWriteOff;

        private final boolean isSecPartialCash;
        private final String peName;
        private final JDate valDate;

        private final JDatetime valDateTime;
        private final int threadPoolSize;

        private LCHSettlementReport settlementReport;

        private ExecutionContext(DSConnection dsCon, PSConnection psCon, int poId, int cptyId, File file, Action settleAction, Action failAction, String toleranceType, String peName, JDate valDate, JDatetime valDateTime, int threadPoolSize) {
            this.dsCon = dsCon;
            this.psCon = psCon;
            this.file = file;
            this.poId = poId;
            this.cptyId = cptyId;
            this.settleAction = settleAction;
            this.failAction = failAction;
            this.toleranceType = Util.isEmpty(toleranceType) ? "Clearing" : toleranceType;
            this.peName = peName;
            this.valDate = valDate;
            this.valDateTime = valDateTime;
            this.threadPoolSize = threadPoolSize;

            this.isSecPartialCash = DefaultsBase.getBooleanProperty("PARTIAL_CASH_FOR_SECURITY_MATCHING", false);
            this.isSecWriteOff = DefaultsBase.getBooleanProperty("WRITE_OFF_FOR_SECURITY_MATCHING", false);
        }

        @Override
        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        @Override
        public DSConnection getDSConnection() {
            return dsCon;
        }

        @Override
        public PSConnection getPSConnection() {
            return psCon;
        }

        public File getFile() {
            return file;
        }

        public Action getSettleAction() {
            return settleAction;
        }

        public int getCounterPartyId() {
            return cptyId;
        }

        public int getPOId() {
            return poId;
        }

        public String getToleranceType() {
            return toleranceType;
        }

        public JDate getValDate() {
            return valDate;
        }

        public String getPeName() {
            return peName;
        }

        public JDatetime getValDatetime() {
            return valDateTime;
        }

        public void setSettlementReport(LCHSettlementReport settlementReport) {
            this.settlementReport = settlementReport;
        }

        public ReconCCPReportHeader getHeader() {
            return settlementReport.getHeader();
        }

        public Action getFailAction() {
            return failAction;
        }

        public LCHSettlementReport getSettlementReport() {
            return settlementReport;
        }

        public boolean isSecurityWriteOff() {
            return isSecWriteOff;
        }

        public boolean isSecurityPartialCash() {
            return isSecPartialCash;
        }
    }

    private static class SettlementBatch implements Callable<SettlementReport> {
        private final List<LCHSettlementNode> settlementNodes;
        private final ExecutionContext context;

        private PricingEnv pe;

        private SettlementReport report;

        private SettlementBatch(List<LCHSettlementNode> settlementNodes, ExecutionContext context) {
            this.settlementNodes = settlementNodes;
            this.context = context;
        }

        @Override
        public SettlementReport call() throws Exception {

            pe = context.getDSConnection().getRemoteMarketData().getPricingEnv(context.getPeName(), context.getValDatetime());
            if (pe == null) {
                throw new ParallelExecutionException(INIT, String.format("Cannot load Pricing Env %s.", context.getPeName()));
            }
            report = new SettlementReport();
            for (LCHSettlementNode node : settlementNodes) {
                processNode(node);
            }
            return report;
        }

        private void processNode(LCHSettlementNode node) throws CalypsoServiceException, ParseException, CloneNotSupportedException, SerializationException, PSException {
            //1 find transfer
            JDate valDate = JDate.valueOf((new SimpleDateFormat("yyyy-MM-dd")).parse(node.getSettlement().getIntendedSettlementDate()));
            TransferArray xfers = context.getDSConnection().getRemoteBO().getBOTransfers(XFER_SELECT_SQL,
                    Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, XFER_ATTR_SETTLEMENT_REF_INST),
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, node.getSettlement().getSettlementReferenceInstructed()),
                            new CalypsoBindVariable(CalypsoBindVariable.JDATE, valDate),
                            new CalypsoBindVariable(CalypsoBindVariable.INTEGER, context.getCounterPartyId()),
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, LegalEntity.COUNTERPARTY),
                            new CalypsoBindVariable(CalypsoBindVariable.INTEGER, context.getPOId()),
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, LegalEntity.PROCESSINGORG)));

            if ((xfers == null || xfers.isEmpty()) && "Partialled".equals(node.getSettlement().getSettlementStatus())) {
                //try to find previously processed (SPLIT) transfer
                xfers = context.getDSConnection().getRemoteBO().getBOTransfers(PART_SETTLE_XFER_SELECT_SQL,
                        Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, XFER_ATTR_SETTLEMENT_REF_INST),
                                new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, node.getSettlement().getSettlementReferenceInstructed()),
                                new CalypsoBindVariable(CalypsoBindVariable.JDATE, valDate),
                                new CalypsoBindVariable(CalypsoBindVariable.INTEGER, context.getCounterPartyId()),
                                new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, LegalEntity.COUNTERPARTY),
                                new CalypsoBindVariable(CalypsoBindVariable.INTEGER, context.getPOId()),
                                new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, LegalEntity.PROCESSINGORG)));

                if (xfers != null && xfers.size() > 1) {

                    for (BOTransfer xfer : xfers) {
                        TransferArray siblings = context.getDSConnection().getRemoteBO().getBOTransfers(
                                XFER_SELECT_SPAWNS_SQL,
                                Arrays.asList(
                                        new CalypsoBindVariable(CalypsoBindVariable.JDATE, valDate),
                                        new CalypsoBindVariable(CalypsoBindVariable.INTEGER, context.getCounterPartyId()),
                                        new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, LegalEntity.COUNTERPARTY),
                                        new CalypsoBindVariable(CalypsoBindVariable.INTEGER, context.getPOId()),
                                        new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, LegalEntity.PROCESSINGORG),
                                        new CalypsoBindVariable(CalypsoBindVariable.LONG, xfer.getLongId())));

                        List<LCHSettlementNode> spawnNodes = new ArrayList<>(node.getSpawns());
                        List<BOTransfer> matched = siblings.stream().filter(x -> {
                            for (LCHSettlementNode sNode : spawnNodes) {
                                if (sNode.getSettlement().getSettlementReferenceInstructed().equals(x.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST))) {
                                    boolean[] amtMatch = matchAmounts(x, sNode, new double[4], context, pe);
                                    if (amtMatch[0] && amtMatch[1]) {
                                        spawnNodes.remove(sNode);
                                        return true;
                                    }
                                }

                            }

                            return false;
                        }).collect(Collectors.toList());

                        if (!Util.isEmpty(matched) && matched.size() == node.getSpawns().size() && Util.isEmpty(spawnNodes)) {
                            xfers = new TransferArray();
                            xfers.add(xfer);
                            break;
                        }

                    }

                }
            }

            if (xfers == null || xfers.isEmpty()) {
                report.notFound(node.getSettlement());
                return;
            }
            if (xfers.size() > 1) {
                report.mismatch(xfers, node.getSettlement(), String.format("More than one transfer found for reference %s.", node.getSettlement().getSettlementReferenceInstructed()));
                return;
            }
            BOTransfer xfer = xfers.get(0);
            if (!xfer.isPayment()) {
                report.mismatch(xfer, node.getSettlement(), String.format("Underling transfer found for reference %s, expected payment.", node.getSettlement().getSettlementReferenceInstructed()));
                return;
            }

            double[] amounts = new double[4];
            boolean[] matchResult = matchAmounts(xfer, node, amounts, context, pe);

            if (!matchResult[0] || !matchResult[1]) {
                StringBuilder msg = new StringBuilder();
                if (!matchResult[0])
                    msg.append(String.format("Nominal amounts do not match PO: %f,  LCH:%f.", amounts[0], amounts[2]));

                if (!matchResult[1]) {
                    if (msg.length() > 0)
                        msg.append(" and ");
                    msg.append(String.format("Cash amounts do not match PO: %f,  LCH:%f.", amounts[1], amounts[3]));
                }
                report.mismatch(xfer, node.getSettlement(), msg.toString());
                return;
            }

            processNode(xfer, node);
        }

        private boolean processNode(final BOTransfer xfer, final LCHSettlementNode node) throws ParseException, CalypsoServiceException, SerializationException, PSException, CloneNotSupportedException {
            switch (node.getSettlement().getSettlementStatus()) {
                case "Partialled":
                    if (Status.S_SPLIT.equals(xfer.getStatus()))
                        return processPartial(xfer, node.getSpawns(), node.getSettlement());
                    else
                        return splitTransfer(xfer, node.getSpawns(), node.getSettlement());
                case "In-Settlement":
                    if (actionNotApplicable(xfer, context.getSettleAction())) {
                        report.mismatch(xfer, node.getSettlement(), String.format("Action %s is not applicable to transfer.", context.getSettleAction()));
                        return false;
                    } else {
                        report.inSettlement(xfer, node.getSettlement());
                        return true;
                    }
                case "Failed":
                    BOTransfer failedXfer = xfer;
                    if (context.getFailAction() != null && !actionNotApplicable(xfer, context.getFailAction())) {
                        BOTransfer toSave = (BOTransfer) failedXfer.clone();
                        toSave.setAction(context.getFailAction());
                        if (context.getDSConnection().getRemoteBO().save(toSave, 0, ScheduledTaskPROCESS_LCH_SETTLEMENT.class.getSimpleName()) != failedXfer.getLongId()) {
                            report.error(failedXfer, node.getSettlement(), String.format("Error applying action %s to transfer %s.", context.getSettleAction(), failedXfer));
                            return false;
                        } else {
                            failedXfer = context.getDSConnection().getRemoteBO().getBOTransfer(xfer.getLongId());
                        }

                        if (actionNotApplicable(failedXfer, context.getSettleAction())) {
                            report.mismatch(failedXfer, node.getSettlement(), String.format("Action %s is not applicable to transfer.", context.getSettleAction()));
                            return false;
                        } else {
                            report.Failed(failedXfer, node.getSettlement());
                            return true;
                        }
                    }

                case "Settled":
                    if (Status.S_SETTLED.equals(xfer.getStatus())) {
                        report.settled(xfer, node.getSettlement(), "Already settled.");
                        return true;
                    }

                    if (actionNotApplicable(xfer, context.getSettleAction())) {
                        report.mismatch(xfer, node.getSettlement(), String.format("Action %s is not applicable to transfer.", context.getSettleAction()));
                        return false;
                    }
                    BOTransfer toSave = (BOTransfer) xfer.clone();
                    toSave.setAction(context.getSettleAction());

                    if (context.getDSConnection().getRemoteBO().save(toSave, 0, ScheduledTaskPROCESS_LCH_SETTLEMENT.class.getSimpleName()) != xfer.getLongId()) {
                        report.error(xfer, node.getSettlement(), String.format("Error applying action %s to transfer %s.", context.getSettleAction(), xfer));
                        return false;
                    }
                default:
                    report.error(xfer, node.getSettlement(), String.format("Unaexpected settlements status %s.", node.getSettlement().getSettlementStatus()));
                    return false;

            }

        }

        private boolean processPartial(BOTransfer parent, List<LCHSettlementNode> spawns, LCHSettlement settlement) throws CalypsoServiceException, ParseException, SerializationException, PSException, CloneNotSupportedException {
            JDate valDate = JDate.valueOf((new SimpleDateFormat("yyyy-MM-dd")).parse(settlement.getIntendedSettlementDate()));
            TransferArray siblings = context.getDSConnection().getRemoteBO().getBOTransfers(
                    XFER_SELECT_SPAWNS_SQL,
                    Arrays.asList(
                            new CalypsoBindVariable(CalypsoBindVariable.JDATE, valDate),
                            new CalypsoBindVariable(CalypsoBindVariable.INTEGER, context.getCounterPartyId()),
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, LegalEntity.COUNTERPARTY),
                            new CalypsoBindVariable(CalypsoBindVariable.INTEGER, context.getPOId()),
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, LegalEntity.PROCESSINGORG),
                            new CalypsoBindVariable(CalypsoBindVariable.LONG, parent.getLongId())));

            List<Pair<BOTransfer, LCHSettlementNode>> matched = new ArrayList<>();
            List<BOTransfer> unmatchedTransfers = new ArrayList<>(siblings);
            List<LCHSettlementNode> unmatchedSettlements = new ArrayList<>(spawns);

            for (BOTransfer xfer : siblings) {
                for (int s = 0; s < unmatchedSettlements.size(); s++) {
                    LCHSettlementNode settlementNode = unmatchedSettlements.get(s);
                    if (settlementNode.getSettlement().getSettlementReferenceInstructed().equals(xfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST))) {
                        boolean[] matchResult = matchAmounts(xfer, settlementNode, new double[4], context, pe);
                        if (matchResult[0] && matchResult[1]) {
                            matched.add(Pair.of(xfer, settlementNode));
                            unmatchedSettlements.remove(settlementNode);
                            unmatchedTransfers.remove(xfer);
                            break;
                        }
                    }

                }
            }
            if (Util.isEmpty(unmatchedTransfers) && Util.isEmpty(unmatchedSettlements)) {
                for (Pair<BOTransfer, LCHSettlementNode> matchedPair : matched) {
                    if (!processNode(matchedPair.getLeft(), matchedPair.getRight()))
                        return false;
                }
                return true;
            } else {
                report.error(parent, settlement, "Failed to reconcile siblings.");
                return false;
            }

        }

        private static boolean[] matchAmounts(BOTransfer xfer, LCHSettlementNode node, double[] amounts, ExecutionContext context, PricingEnv pe) {

            boolean[] result = new boolean[]{false, false};

            amounts[0] = "SECURITY".equals(xfer.getTransferType()) ? ("PAY".equals(xfer.getPayReceive()) ? -Math.abs(xfer.getNominalAmount()) : Math.abs(xfer.getNominalAmount())) : 0;
            amounts[1] = "SECURITY".equals(xfer.getTransferType()) ? ("PAY".equals(xfer.getPayReceive()) ? Math.abs(xfer.getRealCashAmount()) : -Math.abs(xfer.getRealCashAmount()))
                    : "PAY".equals(xfer.getPayReceive()) ? -Math.abs(xfer.getSettlementAmount()) : Math.abs(xfer.getSettlementAmount());

            if ("SECURITY".equals(xfer.getTransferType()) && "DFP".equals(xfer.getDeliveryType()))
                amounts[1] = 0;

            amounts[2] = getNominal(Collections.singletonList(node));
            amounts[3] = getCash(Collections.singletonList(node));
            String nomCcy = xfer.getSettlementCurrency();
            if ("SECURITY".equals(xfer.getTransferType())) {
                Product prod = BOCache.getExchangedTradedProduct(context.getDSConnection(), xfer.getProductId());
                nomCcy = prod.getCurrency();
            }

            double nomTol = ReconCCPUtil.getToleranceAmount(context.getCounterPartyId(), nomCcy, context.getToleranceType(), pe, context.getValDate());
            if (withInTolerance(amounts[0], amounts[2], nomTol)) {
                result[0] = true;
            }

            if (withInTolerance(amounts[1], amounts[3], nomTol)) {
                result[1] = true;
            }

            return result;
        }

        private boolean splitTransfer(BOTransfer xfer, List<LCHSettlementNode> spawns, LCHSettlement parent) throws CloneNotSupportedException, CalypsoServiceException, SerializationException, PSException {

            int nomDecimals = 2;
            String nomCcy = Util.isEmpty(xfer.getTradeCurrency()) ? xfer.getSettlementCurrency() : xfer.getTradeCurrency();
            if (xfer.getProductId() > 0) {
                Product prod = BOCache.getExchangedTradedProduct(context.getDSConnection(), xfer.getProductId());
                if (prod != null) {
                    nomDecimals = prod.getNominalDecimals(prod.getCurrency());
                    nomCcy = prod.getCurrency();
                }
            }
            boolean isPartialSettlement = spawns.stream().anyMatch(s -> "Settled".equals(s.getSettlement().getSettlementStatus()));
            BOTransfer toSplit = (BOTransfer) xfer.clone();
            toSplit.setAction(isPartialSettlement ? Action.PARTIAL_SETTLE : Action.SPLIT);
            toSplit.setEnteredUser(context.getDSConnection().getUser());

            if (actionNotApplicable(xfer, toSplit.getAction())) {
                report.error(xfer, parent, String.format("Action %s is not applicable to transfer %s.", toSplit.getAction(), xfer));
                return false;
            }


            double remNom = getSecAmount(xfer, BOTransfer::getNominalAmount);
            double remAmount = xfer.isSecurity() ? getSecAmount(xfer, BOTransfer::getSettlementAmount) : getCashAmount(xfer, BOTransfer::getSettlementAmount);
            double remOtherAmount = xfer.isSecurity() ? getCashAmount(xfer, BOTransfer::getOtherAmount) : 0;
            double remRealAmount = xfer.isSecurity() ? getSecAmount(xfer, BOTransfer::getRealSettlementAmount) : getCashAmount(xfer, BOTransfer::getRealSettlementAmount);
            double remRealCashAmount = xfer.isSecurity() ? getCashAmount(xfer, BOTransfer::getRealCashAmount) : 0;

            //    long allocatedSeed = context.getDSConnection().getRemoteAccess().allocateLongSeed(SeedAllocSQL.TRANSFER, spawns.size());
            //    long[] xferIds = new long[spawns.size()];
            int cnt = 0;
            TransferArray spawnXfers = new TransferArray();
            for (int i = 0; i < spawns.size(); i++) {
                LCHSettlementNode spawnNode = spawns.get(i);
                double lchNom = getNominal(spawnNode);
                double lchCash = getCash(spawnNode);

                LCHSettlement spawn = spawnNode.getSettlement();

                BOTransfer spawnXfer = (BOTransfer) xfer.clone();
                spawnXfer.setStatus(Status.S_NONE);
                spawnXfer.setAction(Action.NEW);
                spawnXfer.setParentLongId(xfer.getLongId());
                spawnXfer.setLongId(0);
                //      spawnXfer.setAllocatedLongSeed(allocatedSeed--);
                //      xferIds[cnt++] = spawnXfer.getAllocatedLongSeed();

                if (i == spawns.size() - 1) {
                    spawnXfer.setNominalAmount(remNom);
                    spawnXfer.setSettlementAmount(remAmount);
                    spawnXfer.setRealSettlementAmount(remRealAmount);
                    spawnXfer.setOtherAmount(remOtherAmount);
                    spawnXfer.setRealCashAmount(remRealCashAmount);
                } else {
                    double ratio = xfer.getNominalAmount() != 0
                            ? Math.abs(lchNom / xfer.getNominalAmount())
                            : Math.abs(lchCash / xfer.getSettlementAmount());
                    spawnXfer.setNominalAmount(
                            CurrencyUtil.roundAmount(
                                    spawnXfer.getNominalAmount() < 0 ? -Math.abs(spawn.getNominalInstructed()) : Math.abs(spawn.getNominalInstructed()),
                                    nomCcy, nomDecimals));
                    if (spawn.getNominalInstructed() > 0) {
                        //security quantity
                        spawnXfer.setSettlementAmount(RoundingMethod.R_NEAREST.round(spawnXfer.getSettlementAmount() * ratio, 0));
                    } else {
                        spawnXfer.setSettlementAmount(CurrencyUtil.roundAmount(spawnXfer.getSettlementAmount() * ratio, spawnXfer.getSettlementCurrency()));
                    }

                    spawnXfer.setOtherAmount(CurrencyUtil.roundAmount(spawnXfer.getOtherAmount() * ratio, spawnXfer.getSettlementCurrency()));
                    spawnXfer.setRealCashAmount(CurrencyUtil.roundAmount(spawnXfer.getRealCashAmount() * ratio, spawnXfer.getSettlementCurrency()));
                    spawnXfer.setRealSettlementAmount(CurrencyUtil.roundAmount(spawnXfer.getRealSettlementAmount() * ratio, spawnXfer.getSettlementCurrency()));

                    remNom -= spawnXfer.getNominalAmount();
                    remOtherAmount -= spawnXfer.getOtherAmount();
                    remAmount -= spawnXfer.getSettlementAmount();
                    remRealAmount -= spawnXfer.getRealSettlementAmount();
                    remRealCashAmount -= spawnXfer.getRealCashAmount();
                }

                Double cashAmountInstructed = null;
                double ourCashAmount = "PAY".equals(spawnXfer.getPayReceive()) ? Math.abs(spawnXfer.getRealCashAmount()) : -Math.abs(spawnXfer.getRealCashAmount());
                double theirCashAmount = "PAY".equals(spawnXfer.getPayReceive()) ? Math.abs(spawn.getCashAmountInstructed()) : -Math.abs(spawn.getCashAmountInstructed());
                if ("SECURITY".equals(xfer.getTransferType()) && "DAP".equals(xfer.getDeliveryType()) && context.isSecurityWriteOff() && context.isSecurityPartialCash()) {
                    if (Math.abs(theirCashAmount - ourCashAmount) > Math.pow(10, -CurrencyUtil.getCcyDecimals(spawnXfer.getSettlementCurrency(), 2))) {
                        cashAmountInstructed = theirCashAmount;
                    }
                }
                spawnXfer.setAttribute(BOTransfer.BUSINESS_REASON, Action.S_PARTIAL_SETTLE);
                spawnXfer.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, spawn.getSettlementReferenceInstructed());
                if ("Settled".equals(spawn.getSettlementStatus())) {
                    spawnXfer.setAttribute("ExpectedStatus", Status.SETTLED);
                    if (cashAmountInstructed != null) {
                        BOTransfer wo = spawnXfer.getCashTransfer();
                        wo.setTransferType("WRITE_OFF");
                        wo.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, wo.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST) + "_WRITE_OFF");
                        double woAmount = ourCashAmount - cashAmountInstructed;
                        wo.setSettlementAmount(woAmount);
                        wo.setRealSettlementAmount(woAmount);
                        wo.setPayReceive(woAmount < 0 ? "PAY" : "RECEIVE");
                        wo.setEventType(woAmount < 0 ? BOTransfer.toString(BOTransfer.PAYMENT) : BOTransfer.toString(BOTransfer.RECEIPT));
                        wo.setDeliveryType("DFP");
                        wo.setAttribute("ExpectedStatus", Status.FAILED);
                        if (wo.getNettedTransfer())
                            wo.setAttribute("SPLITREASON", "SecurityNetting"); //workaround to stop Calypso from generating DAP cash trade transfers.

                        spawnXfer.setOtherAmount(theirCashAmount);
                        spawnXfer.setRealCashAmount(theirCashAmount);
                        spawnXfer.setAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED, null);

                        spawnXfers.add(wo);
                    }
                } else if ("In-Settlement".equals(spawn.getSettlementStatus())) {
                    spawnXfer.setAttribute("ExpectedStatus", Status.FAILED);
                    spawnXfer.setAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED, cashAmountInstructed == null ? null : Util.numberToString(cashAmountInstructed, Locale.UK, false));

                }
                spawnXfers.add(spawnXfer);
            }


            final long allocatedSeed = context.getDSConnection().getRemoteAccess().allocateLongSeed(SeedAllocSQL.TRANSFER, spawnXfers.size());
            final long[] xferIds = new long[spawnXfers.size()];

            for (int i = spawnXfers.size() - 1; i >= 0; i--) {
                spawnXfers.get(i).setAllocatedLongSeed(allocatedSeed - i);
                xferIds[i] = allocatedSeed - i;
            }

            Vector<?> events = context.getDSConnection().getRemoteBO().splitTransfers(toSplit, spawnXfers);
            if (context.getPSConnection() != null) {
                context.getPSConnection().publish(events);
            }

            //report

            TransferArray xfers = context.getDSConnection().getRemoteBO().getTransfers(xferIds);
            if (xfers.size() != xferIds.length) {
                report.error(xfer, parent, "Error splitting transfer.");
                return false;
            }

            List<BOTransfer> matched = xfers.stream().filter(x -> {
                if ("WRITE_OFF".equals(x.getTransferType())) {
                    final String woRef = x.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST);
                    final String orgiRef = woRef != null && woRef.endsWith("_WRITE_OFF") ? woRef.substring(0, woRef.length() - "_WRITE_OFF".length()) : woRef;
                    Optional<LCHSettlementNode> spawn = spawns.stream().filter(s -> s.getSettlement().getSettlementReferenceInstructed().equals(orgiRef)).findFirst();
                    if (spawn.isPresent()) {
                        if (Status.S_SETTLED.equals(x.getStatus())) {
                            report.writeOff(x);
                        } else {
                            report.error(x, null, String.format("Unexpected write-off transfer status %s, expected SETTLED", x.getStatus()));
                        }
                    }
                    return spawn.isPresent();
                } else {
                    Optional<LCHSettlementNode> spawn = spawns.stream().filter(s -> s.getSettlement().getSettlementReferenceInstructed().equals(x.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST))).findFirst();
                    if (spawn.isPresent()) {
                        LCHSettlement lchSettlement = spawn.get().getSettlement();
                        switch (lchSettlement.getSettlementStatus()) {
                            case "In-Settlement":
                                if (Status.S_SETTLED.equals(x.getStatus()) || Status.isCanceled(x.getStatus())) {
                                    report.error(x, lchSettlement, String.format("Unexpected transfer status %s, expected VERIFIED", x.getStatus()));
                                    return false;
                                } else {
                                    report.inSettlement(x, lchSettlement);
                                    return true;
                                }
                            case "Settled":
                                if (Status.S_SETTLED.equals(x.getStatus())) {
                                    report.settled(x, lchSettlement);
                                    return true;
                                } else {
                                    report.error(x, lchSettlement, String.format("Unexpected transfer status %s, expected SETTLED", x.getStatus()));
                                    return false;
                                }
                            default:
                                report.error(x, lchSettlement, String.format("Unexpected LCH settlement status  status %s", lchSettlement.getSettlementStatus()));
                                return false;
                        }
                    } else {
                        report.error(x, null, String.format("Unexpected error, ref %s not found.", x.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST)));
                        return false;
                    }
                }
            }).collect(Collectors.toList());

            if (matched.size() != xfers.size()) {
                report.error(xfer, null, String.format("Unexpected error, number of spawns saved (%d) does not equal to the number of spawns generated (%d), parent referencer %s", matched.size(), xfers.stream().filter(x -> !"WRITE_OFF".equals(x.getTransferType())).count(), parent.getSettlementReferenceInstructed()));
                return false;
            }
            return true;
        }

        private boolean actionNotApplicable(BOTransfer xfer, Action action) {
            return !actionApplicable(Collections.singletonList(xfer), action);
        }

        private boolean actionApplicable(Collection<BOTransfer> xfers, Action action) {
            return xfers.stream().allMatch(x -> {
                try {
                    BOTransfer cloneXfer = (BOTransfer) x.clone();
                    cloneXfer.setAction(action);
                    Trade t = cloneXfer.getTradeLongId() > 0 ? context.getDSConnection().getRemoteTrade().getTrade(cloneXfer.getTradeLongId()) : null;
                    return BOTransferWorkflow.isTransferActionApplicable(cloneXfer, t, action, context.getDSConnection());
                } catch (Exception e) {
                    Log.error(this, e);
                }
                return false;
            });
        }


        private static boolean withInTolerance(double d1, double d2, double tolerance) {
            return Math.abs(d1 - d2) <= tolerance;
        }
    }

    private enum SettlementReconStatus {
        Settled(true), Mismatch(false), NotFound(false), Error(false), InSettlement(true), Failed(true), WrittenOff(true);
        private final boolean isSuccess;

        SettlementReconStatus(boolean isSuccess) {
            this.isSuccess = isSuccess;
        }

        public boolean isSuccess() {
            return isSuccess;
        }
    }

    private static class SettlementResult {
        private final SettlementReconStatus settlementStatus;
        private final Collection<BOTransfer> xfers;
        private final LCHSettlement settlementAdv;
        private final String message;

        private SettlementResult(SettlementReconStatus settlementStatus, Collection<BOTransfer> xfers, LCHSettlement settlementAdv, String message) {
            this.settlementStatus = settlementStatus;
            this.xfers = xfers;
            this.settlementAdv = settlementAdv;
            this.message = message;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if (!Util.isEmpty(xfers)) {
                if (xfers.size() == 1)
                    sb.append("Transfer ").append(xfers.iterator().next());
                else
                    sb.append("Transfers ").append("[").append(Util.collectionToString(xfers)).append("]");
            }
            sb.append(',');

            if (settlementAdv != null) {
                sb.append("Ref ").append(settlementAdv.getSettlementReferenceInstructed()).append(" LCH Status").append(settlementAdv.getSettlementStatus());
            }

            sb.append(", Settlement Result ").append(settlementStatus);

            if (!Util.isEmpty(message))
                sb.append(',').append(message);

            return sb.toString();
        }

        private String toCSV() {
            //"Payment,Settlement,LCHStatus,Result,Message"
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            if (!Util.isEmpty(xfers)) {
                if (xfers.size() == 1)
                    sb.append(xfers.iterator().next().getLongId());
                else
                    sb.append("[").append(Util.collectionToString(xfers.stream().map(x -> Long.toString(x.getLongId())).collect(Collectors.toList()))).append("]");
            }
            sb.append('"').append(',').append('"');

            if (settlementAdv != null) {
                sb.append(settlementAdv.getSettlementReferenceInstructed());
                sb.append('"').append(',').append('"');
                sb.append(settlementAdv.getSettlementStatus());
            } else {
                List<String> refsInst = xfers == null ? null : xfers.stream().map(x -> x.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST)).collect(Collectors.toList());
                if (!Util.isEmpty(refsInst)) {
                    if (refsInst.size() == 1)
                        sb.append(refsInst.get(0));
                    else
                        sb.append("[").append(Util.collectionToString(refsInst)).append("]");
                }
                sb.append('"').append(',').append('"');
            }

            sb.append('"').append(',').append('"');

            sb.append(settlementStatus);
            sb.append('"').append(',').append('"');
            if (!Util.isEmpty(message))
                sb.append(message);

            sb.append('"');

            return sb.toString();

        }

        private SettlementReconStatus getSettlementStatus() {
            return settlementStatus;
        }

        private Collection<BOTransfer> getTransfers() {
            return xfers;
        }
    }

    static class SettlementReport {
        private final List<SettlementResult> results = new ArrayList<>();

        public void settled(BOTransfer xfer, LCHSettlement settlement) {
            results.add(new SettlementResult(SettlementReconStatus.Settled, Collections.singletonList(xfer), settlement, null));
        }

        public void settled(BOTransfer xfer, LCHSettlement settlement, String msg) {
            results.add(new SettlementResult(SettlementReconStatus.Settled, Collections.singletonList(xfer), settlement, msg));
        }

        public void inSettlement(BOTransfer xfer, LCHSettlement settlement) {
            results.add(new SettlementResult(SettlementReconStatus.InSettlement, Collections.singletonList(xfer), settlement, null));
        }

        public void Failed(BOTransfer xfer, LCHSettlement settlement) {
            results.add(new SettlementResult(SettlementReconStatus.Failed, Collections.singletonList(xfer), settlement, null));
        }

        public void mismatch(Collection<BOTransfer> xfers, LCHSettlement settlement, String message) {
            results.add(new SettlementResult(SettlementReconStatus.Mismatch, xfers, settlement, message));
        }

        public void mismatch(BOTransfer xfer, LCHSettlement settlement, String message) {
            results.add(new SettlementResult(SettlementReconStatus.Mismatch, Collections.singletonList(xfer), settlement, message));
        }

        public void notFound(LCHSettlement settlement) {
            results.add(new SettlementResult(SettlementReconStatus.NotFound, null, settlement,
                    String.format("Transfer not found for reference %s.", settlement.getSettlementReferenceInstructed())));
        }

        public void error(BOTransfer xfer, LCHSettlement settlement, String message) {
            results.add(new SettlementResult(SettlementReconStatus.Error, xfer == null ? null : Collections.singletonList(xfer), settlement, message));
        }

        public void writeOff(BOTransfer xfer) {
            String ref = xfer == null ? null : xfer.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST);
            if (ref != null && ref.endsWith("_WRITE_OFF"))
                ref = ref.substring(0, ref.length() - "_WRITE_OFF".length());

            results.add(new SettlementResult(SettlementReconStatus.WrittenOff, xfer == null ? null : Collections.singletonList(xfer), null,
                    String.format("Amount %s%s written off, settlement reference %s ", xfer == null ? "" : xfer.getSettlementCurrency(), new Amount(xfer == null ? 0 : xfer.getSettlementAmount(), 2), Util.isEmpty(ref) ? "no fer" : ref)));
        }

        public List<SettlementResult> getResults() {
            return results;
        }


    }
}
