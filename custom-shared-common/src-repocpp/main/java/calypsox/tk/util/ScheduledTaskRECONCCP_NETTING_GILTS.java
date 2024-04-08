package calypsox.tk.util;

import calypsox.repoccp.ReconCCPConstants;
import calypsox.repoccp.model.lch.netting.*;
import calypsox.repoccp.reader.LCHSettlementStaxReader;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.SeedAllocSQL;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSException;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.LegalEntityTolerance;
import com.calypso.tk.refdata.TaskPriority;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.*;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static calypsox.repoccp.ReconCCPConstants.*;
import static com.calypso.tk.core.LogBase.CALYPSOX;
import static com.calypso.tk.util.ParallelExecutionException.Type.EPILOGUE;
import static com.calypso.tk.util.ParallelExecutionException.Type.INIT;
import static java.util.stream.Collectors.groupingBy;

public class ScheduledTaskRECONCCP_NETTING_GILTS extends ScheduledParallelTask<Boolean, ScheduledTaskRECONCCP_NETTING_GILTS.MatchingResultReport> {


    /**
     * **************ST Parameters*******************
     */
    //Type or reconcilliation GILTS or GC (DBV)
    private static final String RECON_TYPE = "Reconciliation Type";
    // CCP reconciliation file name
    public static final String FILE_NAME = "File Name";
    //CCP reconciliation file location
    public static final String FILE_PATH = "File Path";

    // Report file name
    public static final String REPORT_FILE_NAME = "Report File Name";
    //CReport file location
    public static final String REPORT_FILE_PATH = "Report File Path";

    //WF action to apply to matched xfers
    public static final String WF_ACTION = "WF Action";
    //Thread pool size
    public static final String THREAD_POOL_SIZE = "Pool Size";

    //Netting Method to apply to gross transfers
    public static final String NETTING_METHOD = "Netting Method";

    //Netting Method for cash only transfers
    public static final String CASH_NETTING_METHOD = "Cash Netting Method";

    public static final String CREATE_TASKS = "Create Tasks";

    public static final String FILTER_RECON_OBLIGATIONS = "Filter Reconciled Obligations";

    private static final String TOLERANCE_TYPE = "Tolerance Type";

    /*                        Constants                */

    public static final String DEFAULT_NETTING_METHOD = "CCP_Counterparty";

    private static final String RECON_TYPE_GILTS = "GILTS";
    private static final String RECON_TYPE_GS = "GS";

    private static final String REPORT_HEADER = "Payments,Obligations,Result,Message";

    /**
     * **************LOGGING*******************
     */

    public static final String LOG_CATEGORY = "CustomNettingRecon";

    private final static int MAX_RETRY = 1;

    private final static double REPORT_VALIDATION_TOLERANCE = 0.1;

    private Queue<File> fileQueue;
    private TradeFilter tf;


    @Override
    public String getTaskInformation() {
        return "Reconciles and shapes payments LCH RepoClear obligations.\nLoads LCH Netting file" +
                " reconciles with Calypso transfers, adjusts netting, split transfers to match LCH obligations ";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        final Vector<String> nettingTypes = LocalCache.getDomainValues(getDSConnection(), "nettingType");
        nettingTypes.sort(Comparator.naturalOrder());
        return Arrays.asList(
                attribute(RECON_TYPE).domain(Arrays.asList("", RECON_TYPE_GILTS, RECON_TYPE_GS)).description("Type or reconciliation GILTS (default) of GC General Collateral"),
                attribute(FILE_PATH).mandatory(),
                attribute(FILE_NAME).mandatory(),
                attribute(WF_ACTION).domainName("transferAction"),
                attribute(NETTING_METHOD).domain(currentAttributes -> nettingTypes),
                attribute(CASH_NETTING_METHOD).domain(currentAttributes -> nettingTypes),
                attribute(REPORT_FILE_PATH),
                attribute(REPORT_FILE_NAME),
                attribute(TOLERANCE_TYPE).domain(currentAttributes -> {
                    List<String> types = new ArrayList<>(Arrays.asList("Automatic", "Manual", "ReceiptMsg"));
                    types.addAll(LocalCache.getDomainValues(DSConnection.getDefault(), "leToleranceType"));
                    return types.stream().distinct().sorted().collect(Collectors.toList());
                }).mandatory(),
                attribute(FILTER_RECON_OBLIGATIONS).booleanType(),
                attribute(CREATE_TASKS).booleanType(),
                attribute(THREAD_POOL_SIZE).integer().mandatory());

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
                error = !reconcileNetting(ds, ps); //

            } catch (ParallelExecutionException e) {
                Log.error(LOG_CATEGORY, e);
                String stage = e.isInitException() ? "Initialisation" : e.isInterruptionException() ? "Parallel Run" : e.isEpilogueException() ? "Epilogue" : "Unknown Stage";
                task.setComment(String.format("Error at %s: %s.", stage, e.getMessage()));
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

    public boolean reconcileNetting(DSConnection ds, PSConnection ps) throws ParallelExecutionException {
        String fileName = getAttribute(FILE_NAME);
        String filePath = getAttribute(FILE_PATH);

        if (!Util.isEmpty(getTradeFilter())) {
            tf = BOCache.getTradeFilter(ds, getTradeFilter());
            if (tf == null)
                throw new ParallelExecutionException(INIT, String.format("Trade filter %s not found", getTradeFilter()));
        }


        StrSubstitutor sub = new StrSubstitutor(getValueMap());
        // Replace
        String filePathResolved = sub.replace(filePath);
        String fileNameResolved = sub.replace(fileName);

        File[] files = new File(filePathResolved).listFiles((FileFilter) new WildcardFileFilter(fileNameResolved));
        if (Util.isEmpty(files))
            throw new ParallelExecutionException(INIT, String.format("No files found by mask %s in directory %s. Nothing to process.", fileName, filePath));

        fileQueue = new LinkedList<>(Arrays.asList(files));

        boolean error = true;
        while (!fileQueue.isEmpty()) {
            error = error && parallelRun(ds, ps);
        }
        return error;
    }

    @Override
    public IExecutionContext createExecutionContext(DSConnection ds, PSConnection ps) {
        File file = fileQueue.poll();
        String action = getAttribute(WF_ACTION);

        return new ExecutionContext(ds,
                ps,
                file,
                getIntegerAttribute(THREAD_POOL_SIZE, 1),
                getProcessingOrg() == null ? 0 : getProcessingOrg().getId(),
                tf,
                getPricingEnv(),
                Util.isEmpty(getAttribute(RECON_TYPE)) ? RECON_TYPE_GILTS : getAttribute(RECON_TYPE),
                getAttribute(NETTING_METHOD),
                getAttribute(CASH_NETTING_METHOD),
                getValuationDatetime(),
                getValuationDatetime().getJDate(getTimeZone()),
                Util.isEmpty(action) ? null : Action.valueOf(action),
                getAttribute(TOLERANCE_TYPE),
                getBooleanAttribute(FILTER_RECON_OBLIGATIONS));
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

    @Override
    public List<? extends Callable<MatchingResultReport>> split(IExecutionContext iContext) throws ParallelExecutionException {
        ExecutionContext context = (ExecutionContext) iContext;
        List<Callable<MatchingResultReport>> tasks = new ArrayList<>();
        LCHNettingReport nettingReport;

        Log.info(LOG_CATEGORY, String.format("Reading file %s.", context.getFile()));
        try {

            nettingReport = (new LCHSettlementStaxReader()).read(context.getFile().getAbsolutePath());

        } catch (Exception e) {
            throw new ParallelExecutionException(INIT, e);
        }

        if (nettingReport == null)
            throw new ParallelExecutionException(INIT, String.format("Error reading file %s.", context.getFile()));

        List<String> fileErrors = new ArrayList<>();
        if (validate(nettingReport, fileErrors)) {

            if (Boolean.parseBoolean(nettingReport.getHeader().getEmptyReport())) {
                if (nettingReport.getHeader().getTotalNoOfRecords() > 0 || (nettingReport.getNetting() != null &&
                        (!Util.isEmpty(nettingReport.getNetting().getNettingSets()) || !Util.isEmpty(nettingReport.getNetting().getObligationSets())))) {
                    throw new ParallelExecutionException(INIT, String.format("%s empty report with non-empty content: Header emptyReport is true, but number of records = %d, number of nettings = %d, number of obligations = %d.",
                            context.getFile(), nettingReport.getHeader().getTotalNoOfRecords(),
                            nettingReport.getNetting() != null && !Util.isEmpty(nettingReport.getNetting().getNettingSets()) ? nettingReport.getNetting().getNettingSets().size() : 0,
                            nettingReport.getNetting() != null && !Util.isEmpty(nettingReport.getNetting().getObligationSets()) ? nettingReport.getNetting().getObligationSets().size() : 0));
                }
                Log.info(LOG_CATEGORY, String.format("%s empty Netting  Report, nothing to process.", context.getFile()));
                return tasks;
            }


            final List<LCHNettingSet> cashDfp = new ArrayList<>();

            final Map<LCHIdentifier, LCHNettingSet> nettingSets = nettingReport.getNetting().getNettingSets().stream().collect(Collectors.toMap(LCHNettingSet::getIdentifier, n -> n));
            //CHECK GB00BN65R313
            List<Pair<LCHObligationSet, List<LCHNettingSet>>> obligationsAndNettingSets = nettingReport.getNetting().getObligationSets().stream().map(o -> {

                        if (!Util.isEmpty(o.getNettedPositions())) {
                            return Pair.of(o,
                                    o.getNettedPositions().stream().map(p -> {
                                        LCHIdentifier id = makeIdentifier(p, o.getIdentifier());
                                        return nettingSets.remove(id);
                                    }).collect(Collectors.toList()));
                        } else {
                            Pair<LCHObligationSet, List<LCHNettingSet>> p = null;
                            if (!Util.isEmpty(o.getIdentifier().getIsin()) && o.getObligations().stream().allMatch(obl -> "FoP".equals(obl.getSettlementType()))) {
                                Optional<Map.Entry<LCHIdentifier, LCHNettingSet>> netting = nettingSets.entrySet().stream().filter(n -> o.getIdentifier().getIsin().equals(n.getKey().getIsin())).findFirst();
                                if (netting.isPresent()) {
                                    LCHNettingSet dfpNettingSet = netting.get().getValue();
                                    cashDfp.add(dfpNettingSet.clone());
                                }

                                p = netting.map(lchIdentifierLCHNettingSetEntry -> Pair.of(o, Collections.singletonList(nettingSets.remove(lchIdentifierLCHNettingSetEntry.getKey())))).orElse(null);

                            }
                            if (p == null)
                                Log.error(LOG_CATEGORY, String.format("Cannot find netting for obligation set %s.", o));
                            return p;

                        }

                    }

            ).filter(Objects::nonNull).collect(Collectors.toList());

            if (!Util.isEmpty(cashDfp)) {
                long count = cashDfp.stream().map(dfp -> {
                    List<Pair<LCHObligationSet, List<LCHNettingSet>>> cashObligations = obligationsAndNettingSets.stream()
                            .filter(p -> Util.isEmpty(p.getLeft().getIdentifier().getIsin()) && dfp.getIdentifier().getSettlementCurrency().equals(p.getLeft().getIdentifier().getSettlementCurrency())).collect(Collectors.toList());

                    if (!Util.isEmpty(cashObligations) && cashObligations.size() == 1) {
                        Pair<LCHObligationSet, List<LCHNettingSet>> cashObl = cashObligations.get(0);
                        List<LCHNettingSet> newNetSets = new ArrayList<>(cashObl.getRight());
                        newNetSets.add(dfp);
                        obligationsAndNettingSets.remove(cashObl);
                        obligationsAndNettingSets.add(Pair.of(cashObl.getLeft(), newNetSets));
                        return cashObl;
                    }
                    return null;

                }).filter(Objects::nonNull).count();

                if (count != cashDfp.size()) {
                    Log.error(LOG_CATEGORY, "Obligations don't match netting positions.");
                    throw new ParallelExecutionException(INIT, "Obligations don't match netting positions.");
                }
            }

            //two phases : ISIN specific sets and cross ISIN in the second phase

            for (int phase = 0; phase < 2; phase++) {

                final int currentPhase = phase;

                List<Pair<LCHObligationSet, List<LCHNettingSet>>> sets = obligationsAndNettingSets.stream().filter(p ->
                        (currentPhase == 0) != Util.isEmpty(p.getLeft().getIdentifier().getIsin())).collect(Collectors.toList());

                int chunkSize = sets.size() / context.getThreadPoolSize();


                List<Pair<LCHObligationSet, List<LCHNettingSet>>> pairs = new ArrayList<>();
                int count = 0;
                for (Pair<LCHObligationSet, List<LCHNettingSet>> pair : sets) {
                    if (count > chunkSize) {
                        try {
                            Callable<MatchingResultReport> task = new NettingReconBatch(pairs, context, context.getFile().getAbsolutePath());
                            if (phase == 0)
                                tasks.add(task);
                            else
                                context.addEpilogueTask(task);
                        } catch (CloneNotSupportedException e) {
                            throw new ParallelExecutionException(INIT, e);
                        }
                        pairs = new ArrayList<>();
                        count = 0;
                    }
                    if (!validate(pair)) {
                        Log.error(LOG_CATEGORY, "Obligations don't match netting positions.");
                        throw new ParallelExecutionException(INIT, "Obligations don't match netting positions.");
                    }
                    pairs.add(pair);
                    count++;
                }
                if (!Util.isEmpty(pairs)) {
                    try {
                        Callable<MatchingResultReport> task = new NettingReconBatch(pairs, context, context.getFile().getAbsolutePath());
                        if (phase == 0)
                            tasks.add(task);
                        else
                            context.addEpilogueTask(task);
                    } catch (CloneNotSupportedException e) {
                        throw new ParallelExecutionException(INIT, e);
                    }
                }
            }
        } else {
            throw new ParallelExecutionException(INIT, Util.isEmpty(fileErrors) ? String.format("File %s has formatting issues.", context.getFile()) : String.join("\n", fileErrors));
        }
        return tasks;
    }

    private boolean validate(Pair<LCHObligationSet, List<LCHNettingSet>> pair) {
        double nomObl = pair.getLeft().getObligations().stream().mapToDouble(o -> "LCH".equals(o.getBondsReceiver()) ? -o.getNominalInstructed() : o.getNominalInstructed()).sum();
        double cashObl = pair.getLeft().getObligations().stream().filter(o -> !"Fop".equals(o.getSettlementType())).mapToDouble(o -> "LCH".equals(o.getCashReceiver()) ? -o.getCashAmountInstructed() : o.getCashAmountInstructed()).sum();

        final double cashOnlyMultiplier = Util.isEmpty(pair.getLeft().getIdentifier().getIsin()) ? 0 : 1;
        double nomNet = pair.getRight().stream().flatMap(n -> n.getNetPositions().stream()).mapToDouble(n -> cashOnlyMultiplier * ("LCH".equals(n.getBondsReceiver()) ? -n.getNominalAmount() : n.getNominalAmount())).sum();
        double cashNet = pair.getRight().stream().flatMap(n -> n.getNetPositions().stream()).filter(o -> cashOnlyMultiplier == 0 ||
                (!"DWP".equals(o.getNetPositionType()) && !"RWP".equals(o.getNetPositionType()))).mapToDouble(n -> "LCH".equals(n.getCashReceiver()) ? -n.getCashAmount() : n.getCashAmount()).sum();

        return Math.abs(nomObl - nomNet) <= REPORT_VALIDATION_TOLERANCE && Math.abs(cashObl - cashNet) <= REPORT_VALIDATION_TOLERANCE;
    }

    private LCHIdentifier makeIdentifier(LCHNetPosition p, LCHIdentifier i) {
        LCHIdentifier identifier = new LCHIdentifier();
        identifier.setIsin(Util.isEmpty(p.getIsin()) ? i.getIsin() : p.getIsin());
        identifier.setIsinName(Util.isEmpty(p.getIsinName()) ? i.getIsinName() : p.getIsinName());
        identifier.setSettlementDate(i.getSettlementDate());
        identifier.setSettlementCurrency(i.getSettlementCurrency());
        identifier.setHouseClient(i.getHouseClient());
        identifier.setLchMarketCode("GB");
        identifier.setMembersCsdIcsdTriPartySystem(i.getMembersCsdIcsdTriPartySystem());
        return identifier;
    }

    private boolean validate(LCHNettingReport nettingReport, List<String> errors) {
        int errorsSize = errors.size();
        int noOfRecords = nettingReport.getNetting().getNettingSets() == null ? 0 : nettingReport.getNetting().getNettingSets().size();
        noOfRecords += nettingReport.getNetting().getObligationSets() == null ? 0 : nettingReport.getNetting().getObligationSets().size();
        if (nettingReport.getHeader().getTotalNoOfRecords() != noOfRecords) {
            String errMsg = String.format("Invalid number of records in file, header %d. actual %d.", nettingReport.getHeader().getTotalNoOfRecords(), noOfRecords);
            Log.error(LOG_CATEGORY, errMsg);
            errors.add(errMsg);
        }

        return errorsSize == errors.size();
    }

    @Override
    public Boolean epilogue(List<? extends Callable<MatchingResultReport>> jobs, List<Future<MatchingResultReport>> results, IExecutionContext context) throws ParallelExecutionException {

        List<Callable<MatchingResultReport>> epilogueTasks = ((ExecutionContext) context).getEpilogueTasks();
        if (!Util.isEmpty(epilogueTasks)) {
            results.addAll(executeEpilogueJobs(epilogueTasks, context.getThreadPoolSize()));
        }


        String filePath = getAttribute(REPORT_FILE_PATH);
        String fileName = getAttribute(REPORT_FILE_NAME);
        TaskArray tasks = new TaskArray();
        boolean createTasks = getBooleanAttribute(CREATE_TASKS);

        StrSubstitutor sub = new StrSubstitutor(getValueMap(((ExecutionContext) context).getFile()));
        // Replace
        String filePathResolved = sub.replace(filePath);
        String fileNameResolved = sub.replace(fileName);
        String fullPath = filePathResolved.endsWith("\\") || filePathResolved.endsWith("/") ? filePathResolved + fileNameResolved : filePathResolved + "/" + fileNameResolved;
        try (FileOutputStream outputStream = new FileOutputStream(fullPath)) {
            outputStream.write(REPORT_HEADER.getBytes(StandardCharsets.UTF_8));
            outputStream.write('\n');
            for (Future<MatchingResultReport> report : results) {
                for (MatchingResult result : report.get().getMatchingResults()) {
                    if (!ReconResult.OK.equals(result.getResult()) && createTasks)
                        tasks.add(createTask(result));

                    outputStream.write(formatResult(result).getBytes(StandardCharsets.UTF_8));
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

    private List<Future<MatchingResultReport>> executeEpilogueJobs(List<? extends Callable<MatchingResultReport>> jobs, int poolSize) throws ParallelExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        long start = System.nanoTime();
        try {
            return executor.invokeAll(jobs);
        } catch (InterruptedException e) {
            Log.error(this, "Parallel execution interrupted.", e);
            throw new ParallelExecutionException(EPILOGUE, e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(3600000L, TimeUnit.MILLISECONDS)) {
                    Log.info(this, this.getTaskName() + " has reached the maximum waiting time of 60 minutes - will try waiting another 60 minutes or until the task has completed.");
                }
            } catch (InterruptedException e) {
                Log.error(this, "Thread pool termination interrupted.", e);
            }
            long end = System.nanoTime();
            long nanoDuration = end - start;
            long microDuration = nanoDuration / 1000L;
            Log.info(this, "Task jobs processed in " + microDuration + " microseconds");

        }
    }

    private Task createTask(MatchingResult result) {
        Task task = !Util.isEmpty(result.getTransfers()) ? createTask(result.getTransfers().get(0)) : createTask();
        task.setComment(result.getMsg());
        task.setEventType(EXCEPTION_CALYPSO_TRANSFER_UNMATCHED_RECON_CCP);
        return task;
    }

    private Task createTask() {
        Task t = new Task();

        t.setStatus(Task.NEW);
        t.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        t.setDatetime(new JDatetime());
        t.setNewDatetime(new JDatetime());
        t.setPriority(Task.PRIORITY_LOW);
        t.setNextPriority(Task.PRIORITY_LOW);
        t.setNextPriorityDatetime(null);
        return t;
    }

    private Task createTask(BOTransfer xfer) {
        Task t = new Task();
        t.setNewDatetime(new JDatetime());
        t.setDatetime(t.getDatetime());
        t.setPriority(1);
        t.setId(0L);
        t.setEventType(xfer.getStatus().toString() + "_TRANSFER");
        t.setEventClass("PSEventTransfer");

        t.setTradeLongId(xfer.getTradeLongId());
        t.setObjectLongId(xfer.getLongId());
        t.setProductId(xfer.getProductId());

        t.setUndoTradeDatetime(xfer.getEnteredDate());
        t.setBookId(xfer.getBookId());

        t.setPoId(xfer.getProcessingOrg());
        t.setObjectStatus(xfer.getStatus());
        t.setStatus(0);
        t.setLegalEntityId(xfer.getOriginalCptyId());

        t.setObjectDate(xfer.getSettleDate());
        t.setTradeVersion(xfer.getVersion());
        t.setPreviousUser(xfer.getEnteredUser());
        Trade trade = null;
        if (xfer.getTradeLongId() > 0) {
            try {
                trade = getDSConnection().getRemoteTrade().getTrade(xfer.getTradeLongId());
            } catch (CalypsoServiceException e) {
                Log.error(LOG_CATEGORY, String.format("Cannot load trade %d", xfer.getTradeLongId()), e);
            }
        }

        TaskFillInfoUtil.fill(t, trade);
        TaskPriority.setTaskPriority(t, "Transfer", trade, xfer, null, false);

        return t;

    }

    private String formatResult(MatchingResult result) {
        StringBuilder sb = new StringBuilder();
        if (!Util.isEmpty(result.getTransfers())) {
            sb.append('"');
            if (result.getTransfers().size() > 1)
                sb.append('[');
            sb.append(Util.collectionToString(result.getTransfers().stream().map(x -> Long.toString(x.getLongId())).collect(Collectors.toList())));
            if (result.getTransfers().size() > 1)
                sb.append(']');
            sb.append('"');
        }
        sb.append(",");
        if (!Util.isEmpty(result.getObligations()))
            sb.append('"').append(Util.collectionToString(result.getObligations().stream().map(LCHObligation::getSettlementReferenceInstructed).collect(Collectors.toList()))).append('"');

        sb.append(",").append(result.getResult()).append(",");

        if (!Util.isEmpty(result.getMsg()))
            sb.append('"').append(result.getMsg()).append('"');

        return sb.toString();
    }

    private static class ExecutionContext implements IExecutionContext {
        private final DSConnection ds;
        private final PSConnection ps;
        private final File file;
        private final int poolSize;

        private final int poId;
        private final TradeFilter tf;

        private final String peName;

        private final String reconType;
        private final String nettingMethod;

        private final String cashNettingMethod;
        private final JDate valDate;

        private final JDatetime valDatTime;

        private final Action wfAction;

        private final String toleranceType;

        private final boolean filterReconciledObligations;

        private final List<Callable<MatchingResultReport>> epilogueTasks = new ArrayList<>();

        private ExecutionContext(DSConnection ds, PSConnection ps, File file, int poolSize, int poId, TradeFilter tf, String peName, String reconType, String nettingMethod, String cashNettingMethod,
                                 JDatetime valDatTime, JDate valDate, Action wfAction, String toleranceType, boolean filterReconciledObligations) {
            this.ds = ds;
            this.ps = ps;
            this.file = file;
            this.poolSize = poolSize;
            this.tf = tf;
            this.poId = poId;
            this.peName = peName;
            this.reconType = reconType;
            this.nettingMethod = Util.isEmpty(nettingMethod) ? DEFAULT_NETTING_METHOD : nettingMethod;
            this.cashNettingMethod = Util.isEmpty(cashNettingMethod) ? this.nettingMethod + "Cash" : cashNettingMethod;
            this.valDatTime = valDatTime;
            this.valDate = valDate;
            this.wfAction = wfAction;
            this.toleranceType = Util.isEmpty(toleranceType) ? "Clearing" : toleranceType;
            this.filterReconciledObligations = filterReconciledObligations;

        }

        public File getFile() {
            return file;
        }

        @Override
        public int getThreadPoolSize() {
            return poolSize;
        }

        @Override
        public DSConnection getDSConnection() {
            return ds;
        }

        @Override
        public PSConnection getPSConnection() {
            return ps;
        }

        public int getProcessingId() {
            return poId;
        }

        public TradeFilter getTradeFilter() {
            return tf;
        }

        public String getReconType() {
            return reconType;
        }

        public String getNettingMethod() {
            return nettingMethod;
        }

        public String getPricingEnvName() {
            return peName;
        }

        public JDate getValDate() {
            return valDate;
        }

        public String getCashNettingMethod() {
            return cashNettingMethod;
        }

        public Action getWFAction() {
            return wfAction;
        }

        public String getToleranceType() {
            return toleranceType;
        }

        public JDatetime getValuationDateTime() {
            return valDatTime;
        }

        public void addEpilogueTask(Callable<MatchingResultReport> task) {
            epilogueTasks.add(task);
        }

        public List<Callable<MatchingResultReport>> getEpilogueTasks() {
            return epilogueTasks;
        }

        public boolean filterReconciledObligations() {
            return filterReconciledObligations;
        }
    }

    private static class NettingReconBatch implements Callable<MatchingResultReport> {

        private final static String SQL_WHERE_TRADE = "TRADE_ID IN (SELECT TRADE_ID FROM TRADE_KEYWORD WHERE KEYWORD_NAME= ? AND KEYWORD_VALUE IN %s) AND TRADE_STATUS != 'CANCELED'";

        private final static String SQL_WHERE_NET_TRANSFER = "TRADE_ID IN (SELECT TRADE_ID FROM TRADE_KEYWORD WHERE KEYWORD_NAME= ? AND KEYWORD_VALUE IN %s) " +
                " AND VALUE_DATE =? AND TRANSFER_STATUS NOT IN ('CANCELED', 'SPLIT') AND NETTED_TRANSFER_ID > 0";


        private final String KW_NAME = "BuyerSellerReference";
        private final List<Pair<LCHObligationSet, List<LCHNettingSet>>> nettedObligations;
        private final ExecutionContext context;
        private final String fileName;

        private final MatchingResultReport report = new MatchingResultReport();

        private PricingEnv pe;

        private final TradeFilter tf;


        private NettingReconBatch(List<Pair<LCHObligationSet, List<LCHNettingSet>>> nettedObligations, ExecutionContext context, String fileName) throws CloneNotSupportedException {
            this.nettedObligations = nettedObligations;
            this.context = context;
            this.fileName = fileName;
            tf = context.getTradeFilter() == null ? null : (TradeFilter) context.getTradeFilter().clone();
            if (tf != null)
                tf.setValDate(context.valDatTime);
        }

        @Override
        public MatchingResultReport call() throws Exception {

            try {
                pe = context.getDSConnection().getRemoteMarketData().getPricingEnv(context.getPricingEnvName(), context.getValuationDateTime());
            } catch (CalypsoServiceException e) {
                Log.error(LOG_CATEGORY, String.format("Cannot load Pricing env %s.: %s.", context.getPricingEnvName(), e));
                throw new ParallelExecutionException(INIT, e);
            }

            try {
                for (Pair<LCHObligationSet, List<LCHNettingSet>> obligation : context.filterReconciledObligations() ? filterReconciledObligations(nettedObligations) : nettedObligations) {
                    reconObligationSet(obligation.getRight(), obligation.getLeft(), 0);
                }
            } catch (Exception e) {
                Log.error(LOG_CATEGORY, e);
                throw e;
            }

            return report;
        }

        private List<Pair<LCHObligationSet, List<LCHNettingSet>>> filterReconciledObligations(List<Pair<LCHObligationSet, List<LCHNettingSet>>> obligations) throws CalypsoServiceException {

            List<String> refInstructed = obligations.stream().flatMap(p -> p.getLeft().getObligations().stream()).map(LCHObligation::getSettlementReferenceInstructed).collect(Collectors.toList());

            StringBuilder sql = new StringBuilder("value_date = ? AND transfer_status not in ('CANCELED', 'SPLIT') " +
                    "and transfer_id in (select transfer_id from xfer_attributes where attr_name = ? AND attr_value= ?) " +
                    "and transfer_id in (select transfer_id from xfer_attributes where attr_name = ? AND ");
            if (refInstructed.size() <= ioSQL.MAX_ITEMS_IN_LIST) {
                sql.append("attr_value in ").append(Util.collectionToSQLString(refInstructed)).append(")");
            } else {
                sql.append("(");
                int chunkSize = ioSQL.MAX_ITEMS_IN_LIST;
                for (int i = 0; i < refInstructed.size(); i += chunkSize) {
                    List<String> chunk = refInstructed.subList(i, Math.min(i + chunkSize, refInstructed.size()));
                    if (i > 0)
                        sql.append(" OR ");
                    sql.append("attr_value in ").append(Util.collectionToSQLString(chunk));

                }
                sql.append("))");
            }

            TransferArray reconciledTransfers = context.getDSConnection().getRemoteBO().getBOTransfers(sql.toString(),
                    Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.JDATE, context.getValDate()),
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, XFER_ATTR_RECON_RESULT),
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, RECON_OK),
                            new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, XFER_ATTR_SETTLEMENT_REF_INST)));

            if (reconciledTransfers == null || reconciledTransfers.isEmpty())
                return obligations;

            final Map<String, List<BOTransfer>> byRef = reconciledTransfers.stream().collect(Collectors.groupingBy(t -> t.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST)));
            List<Pair<LCHObligationSet, List<LCHNettingSet>>> filtered = new ArrayList<>();
            for (Pair<LCHObligationSet, List<LCHNettingSet>> nettingAndObligs : obligations) {

                if (nettingAndObligs.getLeft().getObligations().stream().allMatch(o -> {
                    List<BOTransfer> xfers = byRef.get(o.getSettlementReferenceInstructed());
                    if (Util.isEmpty(xfers) || xfers.size() != 1/* to do compare amounts with toll*/)
                        return false;

                    BOTransfer xfer = xfers.get(0);

                    return xfer.getSettlementCurrency().equals(o.getCashAmountCurrency());
                })) {
                    //report as reconciled
                    nettingAndObligs.getLeft().getObligations().forEach(o -> report.matched(byRef.get(o.getSettlementReferenceInstructed()).get(0), o, "Already reconciled."));
                } else {
                    filtered.add(nettingAndObligs);
                }

            }
            return filtered;
        }


        private void reconObligationSet(List<LCHNettingSet> nettingSets, LCHObligationSet obligationSet,
                                        int retryCount) throws Exception {

            if (retryCount > MAX_RETRY) {
                report.allege(obligationSet, String.format("Cannot reconcile obligation set %s, skipping.", obligationSet));
                return;
            }

            /* 1. Find tradeS */
            List<String> buyerSellerRefs = nettingSets.stream()
                    .flatMap(o -> o.getNettedTrades().stream()).map(LCHNettedTrade::getBuyerSellerReference)
                    .collect(Collectors.toList());

            Optional<String> dupRef = buyerSellerRefs.stream().filter(i -> Collections.frequency(buyerSellerRefs, i) > 1).findFirst();

            if (dupRef.isPresent()) {
                throw new Exception(String.format("Duplicated buyerSellerReference %s found in file %s.", dupRef.get(), fileName));
            }

            String buyerSellerRefString = Util.collectionToSQLString(buyerSellerRefs);
            String where = String.format(SQL_WHERE_TRADE, buyerSellerRefString);
            TradeArray trades = context.getDSConnection().getRemoteTrade().getTrades(null, where, null,
                    Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, KW_NAME)));


            final Map<String, List<Trade>> byRef = Arrays.stream(trades.getTrades()).filter(t -> (tf == null || tf.accept(t))
                    && (context.getProcessingId() <= 0 || context.getProcessingId() == t.getBook().getProcessingOrgBasedId())).collect(groupingBy(t -> t.getKeywordValue(KW_NAME)));

            Optional<String> missingRef = buyerSellerRefs.stream().filter(r -> !byRef.containsKey(r)).findFirst();

            if (missingRef.isPresent()) {
                report.allege(obligationSet, String.format("Reference %s not found in Calypso, skipping obligation set %s.", missingRef.get(), obligationSet));
                return;
            }
            //check if more than one trade found by ref

            Map<String, List<BOTransfer>> transfersByRef = new HashMap<>();
            for (Map.Entry<String, List<Trade>> refAndTrades : byRef.entrySet()) {
                List<Trade> foundTrades = refAndTrades.getValue();

                switch (foundTrades.size()) {
                    case 1:
                        break;
                    case 0:
                        report.allege(obligationSet, String.format("No Eligible trades found for reference %s, skipping obligation set %s", refAndTrades.getKey(), obligationSet));
                        return;
                    default:
                        report.allege(obligationSet, String.format("More than one trade [%s] found for for reference %s, skipping obligation set %s",
                                foundTrades.stream().map(t -> Long.toString(t.getLongId())).collect(Collectors.joining(",")),
                                refAndTrades.getKey(), obligationSet));
                        return;

                }
                TransferArray xfers = context.getDSConnection().getRemoteBO().getBOTransfers(foundTrades.get(0).getLongId(), false);

                transfersByRef.put(refAndTrades.getKey(), xfers == null ? Collections.emptyList() : Arrays.stream(xfers.getTransfers())
                        .filter(x -> x != null && !Status.S_SPLIT.equals(x.getStatus()) && !Status.S_CANCELED.equals(x.getStatus()) && !x.getNettedTransfer()).collect(Collectors.toList()));
            }
            List<BOTransfer> nettedTradeTransfers = new ArrayList<>();
            if (reconcileTradeTransfers(nettingSets, obligationSet, transfersByRef, nettedTradeTransfers)) {
                reconcileNettedTransfers(nettingSets, obligationSet, nettedTradeTransfers, retryCount);
            }
        }

        private String getNettingType(String typeAndGroup) {
            int index = typeAndGroup.lastIndexOf(47);
            if (index > 0)
                return typeAndGroup.substring(0, index);
            return typeAndGroup;
        }

        private void reconcileNettedTransfers(List<LCHNettingSet> nettingSets, LCHObligationSet
                obligationSet, List<BOTransfer> transfers, int retryCount) throws CloneNotSupportedException {

            boolean hasIsin = !Util.isEmpty(obligationSet.getIdentifier().getIsin()) && !RECON_TYPE_GS.equals(context.getReconType());
            List<BOTransfer> nettedPayments = transfers.stream().map(BOTransfer::getNettedTransferLongId)
                    .filter(nettedTransferId -> nettedTransferId > 0)
                    .distinct().map(i -> {
                        try {
                            return context.getDSConnection().getRemoteBO().getBOTransfer(i);
                        } catch (CalypsoServiceException e) {
                            Log.error(CALYPSOX, e);
                            throw new RuntimeException(e);
                        }
                    }).filter(x -> !Status.S_CANCELED.equals(x.getStatus()) && !Status.S_SPLIT.equals(x.getStatus())
                            && (context.getNettingMethod().equals(getNettingType(x.getNettingType()))
                            || context.getCashNettingMethod().equals(getNettingType(x.getNettingType()))
                            || getNettingType(x.getNettingType()).startsWith(context.getCashNettingMethod()))
                            && ((hasIsin && "SECURITY".equals(x.getTransferType())) || (!hasIsin && hasCash(x))))
                    .collect(Collectors.toList());

            List<BOTransfer> grossPayments = transfers.stream().filter(t ->
                    t.getNettedTransferLongId() == 0 && (!hasIsin || !"SECURITY".equals(t.getTransferType()) || "DAP".equals(t.getDeliveryType()))
                            || (!context.getNettingMethod().equals(getNettingType(t.getNettingType()))
                            && !context.getCashNettingMethod().equals(getNettingType(t.getNettingType())) && !getNettingType(t.getNettingType()).startsWith(context.getCashNettingMethod()))
                            && ((hasIsin && "SECURITY".equals(t.getTransferType())) || (!hasIsin && hasCash(t)))).collect(Collectors.toList());

            if (!Util.isEmpty(grossPayments)) {
                //create synthetic netting
                nettedPayments.addAll(makeNetting(grossPayments));

            }

            List<BOTransfer> unmatchedNetTransfers = new ArrayList<>(nettedPayments);
            List<LCHObligation> unmatchedObligations = new ArrayList<>(obligationSet.getObligations());

            List<Pair<BOTransfer, LCHObligation>> matched = match(unmatchedNetTransfers, unmatchedObligations, obligationSet.getIdentifier());

            if (!Util.isEmpty(unmatchedNetTransfers)) {

                double lchNom = obligationSet.getObligations().stream().mapToDouble(LCHObligation::getNominalInstructed).sum();
                Optional<BOTransfer> nonZero = unmatchedNetTransfers.stream().filter(x -> !"SECURITY".equals(x.getTransferType())
                        || x.getSettlementAmount() != 0
                        || x.getOtherAmount() != 0).findFirst();
                if (lchNom == 0 && !nonZero.isPresent()) {
                    /*
                    Cash only netting multiple ISINs NETTED to zero all DFP cash aggregated into a single net transfer
                    */

                    for (BOTransfer zeroXfer : unmatchedNetTransfers) {
                        if (!updateXferMatched(zeroXfer, obligationSet.getObligations().get(0).getSettlementReferenceInstructed(), obligationSet.getObligations().get(0).getCashAmountInstructed()))
                            return;
                    }
                    unmatchedNetTransfers.clear();

                    updateMatched(matched, nettingSets, obligationSet, retryCount);

                } else if (Util.isEmpty(unmatchedObligations)) {
                    /*
                    All obligations matched by unmatched transfer remain
                     */

                    report.mismatch(nettedPayments, obligationSet.getObligations(),
                            String.format("Cannot reconcile transfers [%s] with obligation set %s, skipping",
                                    nettedPayments.stream().map(x -> Long.toString(x.getLongId())).collect(Collectors.joining(",")), obligationSet)
                    );
                } else {
                    //check if we can split transfers -> try to match to total obligations
                    if (unmatchedNetTransfers.size() > 0) {
                        List<BOTransfer> unmatchedNetTransfersToSplit = new ArrayList<>(unmatchedNetTransfers);
                        List<LCHObligation> unmatchedObligationsGrouped = group(unmatchedObligations);
                        List<Pair<BOTransfer, LCHObligation>> matchedToSplit = match(unmatchedNetTransfersToSplit, unmatchedObligationsGrouped, obligationSet.getIdentifier());
                        boolean totalMatched = false;

                        if (!Util.isEmpty(matchedToSplit) && Util.isEmpty(unmatchedNetTransfersToSplit) && Util.isEmpty(unmatchedObligationsGrouped)) {
                            totalMatched = true;
                        } else {
                            //try to re net
                            List<BOTransfer> nettedGrouped = new ArrayList<>(makeNetting(unmatchedNetTransfers));
                            matchedToSplit = match(nettedGrouped, unmatchedObligationsGrouped, obligationSet.getIdentifier());
                            if (!Util.isEmpty(matchedToSplit) && Util.isEmpty(nettedGrouped) && Util.isEmpty(unmatchedObligationsGrouped)) {
                                totalMatched = true;
                            }
                            //all securities netted off
                            if (!totalMatched && !Util.isEmpty(unmatchedObligations) && unmatchedObligations.size() == 1 && unmatchedObligations.get(0).getNominalInstructed() == 0) {
                                double non = unmatchedNetTransfers.stream().mapToDouble(x -> "PAY".equals(x.getPayReceive()) ? -Math.abs(x.getNominalAmount()) : Math.abs(x.getNominalAmount())).sum();
                                if (non == 0)

                                    try {
                                        Optional<Map.Entry<Integer, Double>> unbalanced = unmatchedNetTransfers.stream().filter(x -> "SECURITY".equals(x.getTransferType())).collect(Collectors.groupingBy(BOTransfer::getProductId,
                                                        Collectors.summingDouble(x -> "PAY".equals(x.getPayReceive()) ? -Math.abs(x.getSettlementAmount()) : Math.abs(x.getSettlementAmount()))))
                                                .entrySet().stream().filter(e -> Math.abs(e.getValue()) > 0.01).findFirst();

                                        if (unbalanced.isPresent()) {
                                            Product p = BOCache.getExchangedTradedProduct(context.getDSConnection(), unbalanced.get().getKey());
                                            report.mismatch(unmatchedNetTransfers, unmatchedObligations,
                                                    String.format("Calypso security nominal is not zero [%f] for ISIN %s, skipping", unbalanced.get().getValue(), p == null ? unbalanced.get().getKey() : p.getSecCode("ISIN")));
                                            return;

                                        }

                                        //net if necessary

                                        List<BOTransfer> toPairOff = unmatchedNetTransfers.stream().filter(t -> t.getLongId() == 0 || (!context.getNettingMethod().equals(t.getNettingType()) && !context.getCashNettingMethod().equals(t.getNettingType()))).collect(Collectors.toList());
                                        if (!Util.isEmpty(toPairOff)) {
                                            if (!pairOff(toPairOff)) {
                                                report.error(toPairOff, String.format("Failed to net reconcile transfers [%s] , skipping",
                                                        unmatchedNetTransfers.get(0).getUnderlyingTransfers().stream().map(x -> Long.toString(x.getLongId())).collect(Collectors.joining(","))));
                                                return;
                                            }
                                        }
                                        //split transfer into security and cash
                                        crossSecNettingZeroQty(nettingSets, unmatchedObligations.get(0), obligationSet.getIdentifier());

                                        return;
                                    } catch (Exception e) {
                                        Log.error(LOG_CATEGORY, e);
                                        report.error(unmatchedNetTransfers,
                                                String.format("Failed to net reconcile transfers [%s] , skipping",
                                                        unmatchedNetTransfers.stream().map(x -> Long.toString(x.getLongId())).collect(Collectors.joining(","))));
                                        return;
                                    }
                            }
                        }

                        if (totalMatched) {

                            if (unmatchedNetTransfers.size() == 1 && unmatchedObligations.size() > 1) {
                                splitObligations(matchedToSplit.get(0).getLeft(), unmatchedObligations);
                                return;
                            }
                            try {

                                for (BOTransfer unmatchedNetXfer : unmatchedNetTransfers) {

                                    if (unmatchedNetXfer.getNettedTransfer() && context.getNettingMethod().equals(unmatchedNetXfer.getNettingType()) && unmatchedNetXfer.getLongId() > 0)
                                        continue;

                                    TransferArray xfers = unmatchedNetXfer.getUnderlyingTransfers();
                                    if (!unmatchedNetXfer.getNettedTransfer()) {
                                        xfers = new TransferArray();
                                        xfers.add(unmatchedNetXfer);
                                    }
                                    if (!pairOff(xfers)) {
                                        report.error(unmatchedNetTransfers, "Failed to net reconcile transfers, skipping");
                                        return;
                                    }
                                }
                                reconObligationSet(nettingSets, obligationSet, retryCount + 1);
                            } catch (Exception e) {
                                report.error(unmatchedNetTransfers, "Failed to net reconcile transfers, skipping");
                            }
                        }


                    } else {
                        Log.error(LOG_CATEGORY, String.format("Cannot reconcile transfers obligation set %s, skipping", obligationSet));
                        report.allege(obligationSet, "Cannot reconcile obligation set");
                    }


                }
            } else {
                if (!Util.isEmpty(unmatchedObligations)) {
                    report.mismatch(nettedPayments, obligationSet.getObligations(), "Cannot reconcile transfers with obligations, skipping");
                } else {
                    //all matched
                    updateMatched(matched, nettingSets, obligationSet, retryCount);
                }
            }

        }

        private boolean hasCash(BOTransfer xfer) {
            return !"SECURITY".equals(xfer.getTransferType()) || "DAP".equals(xfer.getDeliveryType());
        }

        private void updateMatched
                (List<Pair<BOTransfer, LCHObligation>> matched, List<LCHNettingSet> nettingSets, LCHObligationSet
                        obligationSet, int retryCount) {
            for (Pair<BOTransfer, LCHObligation> matchedPair : matched) {
                if (matchedPair.getLeft().getLongId() == 0) {
                    try {
                        List<BOTransfer> oldNetted = matchedPair.getLeft().getUnderlyingTransfers().stream().map(BOTransfer::getNettedTransferLongId)
                                .filter(nettedTransferLongId -> nettedTransferLongId > 0).distinct().map(i -> {
                                    try {
                                        return context.getDSConnection().getRemoteBO().getBOTransfer(i);
                                    } catch (CalypsoServiceException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .filter(x -> !Status.S_CANCELED.equals(x.getStatus()) && !Status.S_SPLIT.equals(x.getStatus()))
                                .map(x -> {
                                    BOTransfer xClone;
                                    try {
                                        xClone = (BOTransfer) x.clone();
                                    } catch (CloneNotSupportedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    xClone.setAction(Action.CANCEL);
                                    return xClone;
                                }).collect(Collectors.toList());

                        final TransferArray tradeTransfersToReNet = new TransferArray(matchedPair.getLeft().getUnderlyingTransfers());
                        for (BOTransfer toSplit : oldNetted) {
                            List<BOTransfer> underlings = tradeTransfersToReNet.stream().filter(u -> toSplit.getLongId() == u.getNettedTransferLongId()
                                    && !Status.S_CANCELED.equals(u.getStatus()) && !Status.S_SPLIT.equals(u.getStatus())).map(u -> {
                                try {
                                    BOTransfer cloneU = (BOTransfer) u.clone();
                                    cloneU.setNettingType(context.getNettingMethod());
                                    cloneU.setStatus(Status.S_NONE);
                                    cloneU.setAction(Action.NEW);
                                    cloneU.setLongId(0);
                                    return cloneU;
                                } catch (CloneNotSupportedException e) {
                                    throw new RuntimeException(e);
                                }
                            }).collect(Collectors.toList());

                            toSplit.setAction(Action.ASSIGN);
                            toSplit.setEnteredUser(DSConnection.getDefault().getUser());

                            correctTransferAmountsSigns(toSplit);
                            correctTransferAmountsSigns(underlings);

                            Vector<?> events = DSConnection.getDefault().getRemoteBO().splitTransfers(toSplit, new TransferArray(underlings));
                            if (context.getPSConnection() != null) {
                                context.getPSConnection().publish(events);
                            }
                            tradeTransfersToReNet.removeAll(underlings);
                        }

                        if (!Util.isEmpty(tradeTransfersToReNet)) {
                            for (BOTransfer xfer : tradeTransfersToReNet) {
                                splitTransfer(xfer, context.getNettingMethod(), null);
                            }


                        }

                        reconObligationSet(nettingSets, obligationSet, retryCount + 1);
                    } catch (Exception e) {
                        report.error(matched.stream().map(Pair::getLeft).collect(Collectors.toList()), String.format("Cannot split transfers transfers [%s] with obligation set %s, skipping",
                                matched.stream().map(Pair::getLeft).filter(x -> x.getLongId() != 0).map(x -> Long.toString(x.getLongId())).collect(Collectors.joining(",")), obligationSet));
                        return;
                    }

                } else {
                    if ("FoP".equals(matchedPair.getRight().getSettlementType()) && "DAP".equals(matchedPair.getLeft().getDeliveryType())) {
                        try {
                            BOTransfer[] siblings = splitTransferDFP(matchedPair.getLeft());
                            if (siblings == null || siblings.length != 2) {
                                report.error(Collections.singletonList(matchedPair.getLeft()),
                                        String.format("Cannot split DFP transfer %s.", matchedPair.getLeft()));
                                return;
                            }
                            if (match(siblings[0], matchedPair.getRight(), obligationSet.getIdentifier(), getTolerance(siblings[0].getOriginalCptyId(), siblings[0].getSettlementCurrency(), "Clearing")) != 0) {
                                report.error(Collections.singletonList(matchedPair.getLeft()), "Cannot split DFP.");
                                return;
                            }
                            if (updateXferMatched(siblings[0], matchedPair.getRight().getSettlementReferenceInstructed(),  matchedPair.getRight().getCashAmountInstructed()))
                                report.matched(matchedPair.getLeft(), matchedPair.getRight());

                        } catch (Exception e) {
                            report.error(matched.stream().map(Pair::getLeft).collect(Collectors.toList()), String.format("Cannot DFP split transfers transfers [%s] , skipping",
                                    matched.stream().map(Pair::getLeft).filter(x -> x.getLongId() != 0).map(x -> Long.toString(x.getLongId())).collect(Collectors.joining(","))));
                            return;
                        }

                    } else {
                        if (updateXferMatched(matchedPair.getLeft(), matchedPair.getRight().getSettlementReferenceInstructed(),  matchedPair.getRight().getCashAmountInstructed()))
                            report.matched(matchedPair.getLeft(), matchedPair.getRight());
                    }
                }
            }

        }

        private TransferArray getNettedTransfers(List<LCHNettingSet> nettingSets,
                                                 final Function<BOTransfer, Boolean> filter) throws CalypsoServiceException {
            TransferArray netted = new TransferArray();
            List<Long> nettedIds = new ArrayList<>();
            for (LCHNettingSet set : nettingSets) {
                List<String> refs = set.getNettedTrades().stream().map(LCHNettedTrade::getBuyerSellerReference).collect(Collectors.toList());

                String where = String.format(SQL_WHERE_NET_TRANSFER, Util.collectionToSQLString(refs));

                TransferArray tradeTransfers = context.getDSConnection().getRemoteBO().getTransfers(null, where,
                        Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, KW_NAME), new CalypsoBindVariable(CalypsoBindVariable.JDATE, context.getValDate())));
                nettedIds.addAll(tradeTransfers.stream().filter(filter::apply).map(BOTransfer::getNettedTransferLongId).distinct().collect(Collectors.toList()));
            }
            nettedIds = nettedIds.stream().distinct().collect(Collectors.toList());
            long[] ids = new long[nettedIds.size()];
            for (int i = 0; i < nettedIds.size(); i++)
                ids[i] = nettedIds.get(i);
            netted.addAll(context.getDSConnection().getRemoteBO().getTransfers(ids).stream().filter(filter::apply).collect(Collectors.toList()));

            return netted;
        }

        private void crossSecNettingZeroQty(List<LCHNettingSet> nettingSets, LCHObligation
                lchObligation, LCHObligationSetIdentifier identifier) throws
                CalypsoServiceException, CloneNotSupportedException, SerializationException, PSException {

            TransferArray netted = getNettedTransfers(nettingSets, x -> Util.isEmpty(identifier.getIsin()) ? hasCash(x) : "SECURITY".equals(x.getTransferType()));

            Optional<BOTransfer> notZero = netted.stream().filter(x -> "SECURITY".equals(x.getTransferType()) && x.getSettlementAmount() != 0).findFirst();
            if (notZero.isPresent()) {
                report.unmatched(netted, "Cash only obligation with non-zero nominal.");
                return;
            }

            if (isAssignApplicable(netted)) {

                for (BOTransfer xfer : netted) {

                    splitTransfer(xfer, context.getCashNettingMethod(), new String[][]{
                            {"CrossSecNettingZeroQtyAllowed", "true"},
                            {"SecurityCashMix", "true"}});

                }

                TransferArray zeroSecNettedXfers = getNettedTransfers(nettingSets, x -> Util.isEmpty(identifier.getIsin()) ? hasCash(x) : "SECURITY".equals(x.getTransferType()));
                if (Util.isEmpty(zeroSecNettedXfers) || zeroSecNettedXfers.size() != 1)
                    report.unmatched(netted, "Cannot Cross net Sec Zero Qty transfers. ");
                else {
                    double lchNom = "LCH".equals(lchObligation.getBondsReceiver()) ? -Math.abs(lchObligation.getNominalInstructed()) : Math.abs(lchObligation.getNominalInstructed());
                    double lchCash = "LCH".equals(lchObligation.getCashReceiver()) ? -Math.abs(lchObligation.getCashAmountInstructed()) : Math.abs(lchObligation.getCashAmountInstructed());
                    BOTransfer zeroSecNetted = zeroSecNettedXfers.get(0);
                    double nom, cash;
                    if ("SECURITY".equals(zeroSecNetted.getTransferType())) {
                        nom = "PAY".equals(zeroSecNetted.getPayReceive()) ? -Math.abs(zeroSecNetted.getNominalAmount()) : Math.abs(zeroSecNetted.getNominalAmount());
                        cash = "PAY".equals(zeroSecNetted.getPayReceive()) ? Math.abs(zeroSecNetted.getOtherAmount()) : -Math.abs(zeroSecNetted.getOtherAmount());
                    } else {
                        nom = 0;
                        cash = "PAY".equals(zeroSecNetted.getPayReceive()) ? -Math.abs(zeroSecNetted.getOtherAmount()) : Math.abs(zeroSecNetted.getOtherAmount());
                    }
                    double tolerance = getTolerance(zeroSecNetted.getOriginalCptyId(), zeroSecNetted.getSettlementCurrency(), context.getToleranceType());
                    if (nom == lchNom && withInTolerance(cash, lchCash, tolerance))
                        updateXferMatched(zeroSecNetted, lchObligation.getSettlementReferenceInstructed(), lchObligation.getCashAmountInstructed());
                    else {
                        report.mismatch(Collections.singletonList(zeroSecNetted), Collections.singletonList(lchObligation), "Amounts don't match.");
                    }
                }

            } else {
                report.unmatched(netted, "Cannot Cross net Sec Zero Qty transfers, ASSIGN not applicable . ");
            }
        }

        private void correctTransferAmountsSigns(List<BOTransfer> xfers) {
            for (BOTransfer xfer : xfers) {
                correctTransferAmountsSigns(xfer);
            }
        }

        private void correctTransferAmountsSigns(BOTransfer xfer) {

            double sign = "PAY".equals(xfer.getPayReceive()) ? -1 : 1;
            if ("SECURITY".equals(xfer.getTransferType())) {
                //security
                xfer.setNominalAmount(sign * Math.abs(xfer.getNominalAmount()));
                xfer.setSettlementAmount(sign * Math.abs(xfer.getSettlementAmount()));
                xfer.setRealSettlementAmount(sign * Math.abs(xfer.getRealSettlementAmount()));
                //cash
                xfer.setOtherAmount(-sign * Math.abs(xfer.getOtherAmount()));
                xfer.setRealCashAmount(-sign * Math.abs(xfer.getRealCashAmount()));
            } else {
                //cash
                xfer.setSettlementAmount(sign * Math.abs(xfer.getSettlementAmount()));
                xfer.setRealSettlementAmount(sign * Math.abs(xfer.getRealSettlementAmount()));
            }

        }

        private void splitObligations(BOTransfer nettedToSplit, List<LCHObligation> unmatchedObligations) {
            try {

                TransferArray splits = new TransferArray();
                BOTransfer clonedNettedSplit = (BOTransfer) nettedToSplit.clone();
                clonedNettedSplit.setAction(Action.SPLIT);

                double remNom = nettedToSplit.getNominalAmount();
                double remCash = nettedToSplit.getOtherAmount();
                double remAmount = nettedToSplit.getSettlementAmount();

                int nomDecimals = 2;
                String nomCcy = Util.isEmpty(nettedToSplit.getTradeCurrency()) ? nettedToSplit.getSettlementCurrency() : nettedToSplit.getTradeCurrency();
                if (nettedToSplit.getProductId() > 0) {
                    Product prod = BOCache.getExchangedTradedProduct(context.getDSConnection(), nettedToSplit.getProductId());
                    if (prod != null) {
                        nomDecimals = prod.getNominalDecimals(prod.getCurrency());
                        nomCcy = prod.getCurrency();
                    }
                }

                Map<Long, LCHObligation> toReport = new HashMap<>();

                long allocatedSeed = context.getDSConnection().getRemoteAccess().allocateLongSeed(SeedAllocSQL.TRANSFER, unmatchedObligations.size());
                long[] siblingSeeds = new long[unmatchedObligations.size()];
                int cnt = 0;

                for (int i = 0; i < unmatchedObligations.size(); i++) {
                    LCHObligation obligation = unmatchedObligations.get(i);
                    BOTransfer split = (BOTransfer) nettedToSplit.clone();
                    split.setStatus(Status.S_NONE);
                    split.setAction(Action.NEW);
                    split.setParentLongId(nettedToSplit.getLongId());
                    split.setLongId(0);
                    split.setAllocatedLongSeed(allocatedSeed--);
                    siblingSeeds[cnt++] = split.getAllocatedLongSeed();

                    if (i == unmatchedObligations.size() - 1) {
                        split.setNominalAmount(remNom);
                        split.setSettlementAmount(remAmount);
                        split.setRealSettlementAmount(remAmount);
                        split.setOtherAmount(remCash);
                        split.setRealCashAmount(remCash);
                        split.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, obligation.getSettlementReferenceInstructed());
                        split.setAttribute(XFER_ATTR_RECON_RESULT, RECON_OK);
                    } else {

                        double ratio = nettedToSplit.getNominalAmount() != 0
                                ? Math.abs(obligation.getNominalInstructed() / nettedToSplit.getNominalAmount())
                                : Math.abs(obligation.getCashAmountInstructed() / nettedToSplit.getSettlementAmount());

                        split.setNominalAmount(
                                CurrencyUtil.roundAmount(
                                        split.getNominalAmount() < 0 ? -Math.abs(obligation.getNominalInstructed()) : Math.abs(obligation.getNominalInstructed()),
                                        nomCcy, nomDecimals));
                        if (obligation.getNominalInstructed() > 0) {
                            //security quantity
                            split.setSettlementAmount(RoundingMethod.R_NEAREST.round(split.getSettlementAmount() * ratio, 0));
                        } else {
                            split.setSettlementAmount(CurrencyUtil.roundAmount(split.getSettlementAmount() * ratio, split.getSettlementCurrency()));
                        }

                        split.setOtherAmount(CurrencyUtil.roundAmount(split.getOtherAmount() * ratio, split.getSettlementCurrency()));
                        split.setRealCashAmount(CurrencyUtil.roundAmount(split.getRealCashAmount() * ratio, split.getSettlementCurrency()));
                        split.setRealSettlementAmount(CurrencyUtil.roundAmount(split.getRealSettlementAmount() * ratio, split.getSettlementCurrency()));

                        remNom -= split.getNominalAmount();
                        remCash -= split.getOtherAmount();
                        remAmount -= split.getSettlementAmount();

                        split.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, obligation.getSettlementReferenceInstructed());
                        split.setAttribute(XFER_ATTR_RECON_RESULT, RECON_OK);


                    }

                    splits.add(split);
                    toReport.put(split.getAllocatedLongSeed(), obligation);
                }
                Vector<?> events = context.getDSConnection().getRemoteBO().splitTransfers(clonedNettedSplit, splits);
                if (context.getPSConnection() != null) {
                    context.getPSConnection().publish(events);
                }

                if (context.getWFAction() != null) {
                    TransferArray siblings = context.getDSConnection().getRemoteBO().getTransfers(siblingSeeds);

                    if (!isActionApplicable(siblings, context.getWFAction()))
                        return;

                    for (BOTransfer pmt : siblings) {
                        BOTransfer pymtClone = (BOTransfer) pmt.clone();
                        pymtClone.setAction(context.getWFAction());
                        long xferId = context.getDSConnection().getRemoteBO().save(pymtClone, 0, this.getClass().getSimpleName());
                        if (xferId <= 0) {
                            report.error(Collections.singletonList(pmt), String.format("Cannot apply %s to transfer %d.", context.getWFAction(), pmt.getLongId()));
                            return;
                        }

                        report.matched(pmt, toReport.get(xferId));
                    }
                }
            } catch (Exception e) {
                report.error(Collections.singletonList(nettedToSplit),
                        String.format("Cannot split transfer %s,  obligations %s, skipping",
                                nettedToSplit, Util.collectionToString(unmatchedObligations.stream().map(LCHObligation::getSettlementReferenceInstructed).collect(Collectors.toList()))));
            }
        }

        private boolean pairOff(List<BOTransfer> xfers) throws
                SerializationException, PSException, CalypsoServiceException, CloneNotSupportedException {
            TransferArray xferArray = new TransferArray();
            xferArray.addAll(xfers);
            return pairOff(xferArray);
        }

        private boolean pairOff(TransferArray tradeTransfers) throws
                CloneNotSupportedException, CalypsoServiceException, SerializationException, PSException {

            Map<Long, List<BOTransfer>> byNettedId = tradeTransfers.stream().collect(Collectors.groupingBy(BOTransfer::getNettedTransferLongId));

            TransferArray xferToAssign = new TransferArray();

            for (Map.Entry<Long, List<BOTransfer>> group : byNettedId.entrySet()) {
                if (group.getKey() == 0)
                    xferToAssign.addAll(group.getValue());
                else {
                    xferToAssign.add(context.getDSConnection().getRemoteBO().getBOTransfer(group.getKey()));
                }
            }

            if (!Util.isEmpty(xferToAssign)) {
                if (!isAssignApplicable(xferToAssign))
                    return false;

                for (BOTransfer toAssign : xferToAssign) {
                    splitTransfer(toAssign, context.getNettingMethod(), null);
                }
            }


            return true;
        }

        private boolean isAssignApplicable(TransferArray xfers) throws CloneNotSupportedException {
            return isActionApplicable(xfers, Action.ASSIGN);
        }

        private boolean isActionApplicable(TransferArray xfers, Action action) throws CloneNotSupportedException {
            final TransferArray xfersClones = (TransferArray) xfers.clone();
            Optional<BOTransfer> unassignable = xfersClones.stream().filter(x -> {
                try {
                    return !BOTransferWorkflow.isTransferActionApplicable(x,
                            x.getTradeLongId() > 0 ? context.getDSConnection().getRemoteTrade().getTrade(x.getTradeLongId()) : null,
                            action, context.getDSConnection());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).findFirst();
            unassignable.ifPresent(transfer -> report.error(Collections.singletonList(transfer), String.format("Cannot apply %s to transfer %d, skipping.", action, transfer.getLongId())));
            return !unassignable.isPresent();
        }

        private void splitTransfer(BOTransfer xfer, String nettingType, String[][] attributes) throws
                CloneNotSupportedException, CalypsoServiceException, SerializationException, PSException {

            BOTransfer toSplit = (BOTransfer) xfer.clone();
            toSplit.setAction(Action.ASSIGN);
            toSplit.setEnteredUser(context.getDSConnection().getUser());

            BOTransfer split = (BOTransfer) xfer.clone();
            split.setLongId(0L);
            split.setStatus(Status.S_NONE);
            split.setAction(Action.NEW);
            split.setParentLongId(toSplit.getLongId());
            split.setNettingType(nettingType);
            split.setUnderlyingTransfers(null);

            if (attributes != null)
                for (String[] attr : attributes) {
                    split.setAttribute(attr[0], attr[1]);
                }

            Vector<?> events = context.getDSConnection().getRemoteBackOffice().splitTransfers(toSplit, new TransferArray(Collections.singletonList(split)));
            if (context.getPSConnection() != null) {
                context.getPSConnection().publish(events);
            }

        }

        private BOTransfer[] splitTransferDFP(BOTransfer xfer) throws
                CloneNotSupportedException, CalypsoServiceException, SerializationException, PSException {

            if (!"SECURITY".equals(xfer.getTransferType()) && !"DAP".equals(xfer.getDeliveryType())) {
                Log.error(LOG_CATEGORY, String.format("Error cannot split DFP transfer %s.", xfer));
                return null;
            }

            TransferArray underlings = xfer.getUnderlyingTransfers();
            if (Util.isEmpty(underlings))
                underlings = xfer.getNettedTransfer() && Util.isEmpty(xfer.getUnderlyingTransfers()) ? context.getDSConnection().getRemoteBO().getNettedTransfers(xfer.getLongId()) : new TransferArray();


            BOTransfer toSplit = (BOTransfer) xfer.clone();
            BOTransfer secXfer = (BOTransfer) xfer.clone();
            BOTransfer cashXfer = xfer.getCashTransfer();

            TransferArray splits = new TransferArray();

            toSplit.setAction(Action.ASSIGN);
            toSplit.setEnteredUser(context.getDSConnection().getUser());

            secXfer.setLongId(0);
            secXfer.setParentLongId(xfer.getLongId());
            secXfer.setStatus(Status.S_NONE);
            secXfer.setAction(Action.NEW);
            secXfer.setDeliveryType("DFP");
            secXfer.setOtherAmount(0);
            secXfer.setRealCashAmount(0);
            secXfer.setAttribute("SPLITREASON", "SecurityNetting");
            splits.add(secXfer);

            cashXfer.setLongId(0);
            cashXfer.setParentLongId(xfer.getLongId());
            cashXfer.setStatus(Status.S_NONE);
            cashXfer.setAction(Action.NEW);
            cashXfer.setDeliveryType("DFP");
            cashXfer.setAttribute("SPLITREASON", "SecurityNetting");
            splits.add(cashXfer);


            Vector<?> events = context.getDSConnection().getRemoteBackOffice().splitTransfers(toSplit, splits);
            if (context.getPSConnection() != null)
                context.getPSConnection().publish(events);

            String selectNetted = String.format("netted_transfer=1 and transfer_status not in ('CANCELED', 'SPLIT') and value_date = ? " +
                            "and transfer_id in (select netted_transfer_id from bo_transfer where netted_transfer_id>0 and trade_id  in (%s))",
                    underlings.stream().map(BOTransfer::getTradeLongId).filter(tradeLongId -> tradeLongId > 0).distinct().map(i -> Long.toString(i)).collect(Collectors.joining(",")));


            TransferArray netted = context.getDSConnection().getRemoteBO().getBOTransfers(selectNetted, Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.JDATE, xfer.getValueDate())));

            BOTransfer nettedSec = null, nettedCash = null;
            for (BOTransfer nett : netted) {
                if ("SECURITY".equals(nett.getTransferType())) {
                    if (nettedSec != null) {
                        Log.error(LOG_CATEGORY, String.format("Duplicated security nettings, transfers %s and %s., single netted transfer expected.", nettedSec, nett));
                        return null;
                    }
                    nettedSec = nett;
                } else {
                    if (nettedCash != null) {
                        Log.error(LOG_CATEGORY, String.format("Duplicated cash nettings, transfers %s and %s., single netted transfer expected.", nettedCash, nett));
                        return null;
                    }
                    nettedCash = nett;
                }
            }

            if (nettedSec == null || nettedCash == null) {
                Log.error(LOG_CATEGORY, String.format("Cannot find %s, transfer(s) after %s DFP split.", nettedSec == null ? (nettedCash == null ? "cash and security" : "security") : "cash", xfer));
                return null;
            }

            return new BOTransfer[]{nettedSec, nettedCash};
        }

        private List<LCHObligation> group(final List<LCHObligation> obligations) {
            List<LCHObligation> grouped = new ArrayList<>();
            for (LCHObligation item : obligations) {
                if (Util.isEmpty(grouped)) {
                    grouped.add(new LCHObligation(item));
                } else {
                    LCHObligation current = grouped.get(grouped.size() - 1);
                    if (Objects.equals(current.getLchAccount(), item.getLchAccount())
                            && Objects.equals(current.getCashAmountCurrency(), item.getCashAmountCurrency())
                            && Objects.equals(current.getNominalCurrency(), item.getNominalCurrency())) {

                        double nom = current.getNominalInstructed();
                        if ("LCH".equals(current.getBondsReceiver()))
                            nom += "LCH".equals(item.getBondsReceiver()) ? item.getNominalInstructed() : -item.getNominalInstructed();
                        else
                            nom += !"LCH".equals(item.getBondsReceiver()) ? item.getNominalInstructed() : -item.getNominalInstructed();

                        double cash = current.getCashAmountInstructed();
                        if ("LCH".equals(current.getCashReceiver()))
                            cash += "LCH".equals(item.getCashReceiver()) ? item.getCashAmountInstructed() : -item.getCashAmountInstructed();
                        else
                            cash += !"LCH".equals(item.getCashReceiver()) ? item.getCashAmountInstructed() : -item.getCashAmountInstructed();
                        current.setNominalInstructed(nom);
                        current.setCashAmountInstructed(cash);
                    } else {
                        grouped.add(new LCHObligation(item));
                    }
                }

            }
            return grouped;
        }

        private boolean updateXferMatched(BOTransfer payment, String ref, double cashAmount) {
            BOTransfer cashTransfer = payment.getCashTransfer();
            Double cashAmountInstructed = null;
            if (cashTransfer != null && Math.abs(Math.abs(payment.getCashTransfer().getSettlementAmount()) - Math.abs(cashAmount)) > Math.pow(10, -CurrencyUtil.getCcyDecimals(cashTransfer.getSettlementCurrency(), 2))) {
                cashAmountInstructed ="PAY".equals(cashTransfer.getPayReceive())? -Math.abs(cashAmount):Math.abs(cashAmount);
            }

            if (context.getWFAction() == null) {
                String[] attrNames = cashAmountInstructed == null
                        ? new String[]{XFER_ATTR_SETTLEMENT_REF_INST, XFER_ATTR_RECON_RESULT}
                        : new String[]{XFER_ATTR_SETTLEMENT_REF_INST, XFER_ATTR_RECON_RESULT, XFER_ATTR_CASH_AMOUNT_INSTRUCTED};

                String[] attrValues = cashAmountInstructed == null
                        ? new String[]{ref, RECON_OK}
                        : new String[]{ref, RECON_OK, Util.numberToString(cashAmountInstructed)};

                return updateXferAttributes(payment,
                        attrNames,
                        attrValues);

            } else {
                try {
                    BOTransfer pymtClone = (BOTransfer) payment.clone();
                    pymtClone.setAction(context.getWFAction());
                    pymtClone.setAttribute(XFER_ATTR_SETTLEMENT_REF_INST, ref);
                    pymtClone.setAttribute(XFER_ATTR_RECON_RESULT, RECON_OK);
                    String attrCashAmountInstructed = cashAmountInstructed==null?null:Util.numberToString(cashAmountInstructed, Locale.UK, false);
                    pymtClone.setAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED, attrCashAmountInstructed);
                    boolean isActionApplicable = BOTransferWorkflow.isTransferActionApplicable(pymtClone,
                            payment.getTradeLongId() > 0 ? context.getDSConnection().getRemoteTrade().getTrade(payment.getTradeLongId()) : null,
                            context.getWFAction(), context.getDSConnection());

                    if (!isActionApplicable && ref.equals(payment.getAttribute(XFER_ATTR_SETTLEMENT_REF_INST))
                            && RECON_OK.equals(payment.getAttribute(XFER_ATTR_RECON_RESULT))
                            && (payment.getAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED) == null && attrCashAmountInstructed == null
                            || attrCashAmountInstructed!=null && attrCashAmountInstructed.equals(payment.getAttribute(XFER_ATTR_CASH_AMOUNT_INSTRUCTED))))
                        return true;
                    if (!isActionApplicable) {
                        report.error(Collections.singletonList(payment),
                                String.format("Cannot apply action %s to transfer %s. Action %s not applicable from status %s.",
                                        context.getWFAction(), payment, context.getWFAction(), payment.getStatus()));
                        return false;
                    }

                    return context.getDSConnection().getRemoteBO().save(pymtClone, 0, this.getClass().getSimpleName()) > 0;

                } catch (Exception e) {
                    report.error(Collections.singletonList(payment),
                            String.format("Cannot apply %s to transfer %s.", context.getWFAction(), payment));
                    return false;
                }
            }
        }

        private boolean updateXferAttributes(BOTransfer payment, String[] attrNames, String[] attrValues) {
            for (int i = 0; i < attrNames.length; i++) {
                try {
                    String oldVal = payment.getAttribute(attrNames[i]);
                    if (Util.isEmpty(oldVal)) {
                        if (!Util.isEmpty(attrValues[i]))
                            context.getDSConnection().getRemoteBO().saveTransferAttribute(payment.getLongId(), attrNames[i], attrValues[i]);
                    } else {
                        if (!oldVal.equals(attrValues[i]))
                            context.getDSConnection().getRemoteBO().saveTransferAttribute(payment.getLongId(), attrNames[i], attrValues[i]);
                    }

                } catch (CalypsoServiceException e) {
                    report.error(Collections.singletonList(payment),
                            String.format("Cannot update transfers attribute %s=%s, of transfer %d.", attrNames[i], attrValues[i], payment.getLongId()));
                    return false;
                }
            }
            return true;
        }

        private Collection<? extends BOTransfer> makeNetting(List<BOTransfer> tradeTransfers) throws
                CloneNotSupportedException {
            HashMap<String, String> keys = BOCache.getNettingConfig(context.getDSConnection(), context.getNettingMethod());
            List<BOTransfer> nettedTransfers = new ArrayList<>(Collections.singletonList(tradeTransfers.get(0).initialize(keys)));
            nettedTransfers.get(0).setNettingType(context.getNettingMethod());
            nettedTransfers.get(0).setUnderlyingTransfers(new TransferArray());
            nettedTransfers.get(0).getUnderlyingTransfers().add(tradeTransfers.get(0));
            for (int i = 1; i < tradeTransfers.size(); i++) {
                boolean found = false;
                for (BOTransfer netted : nettedTransfers) {
                    BOTransfer tradeClone = (BOTransfer) tradeTransfers.get(i).clone();
                    tradeClone.setNettingType(context.getNettingMethod());
                    if (netted.checkKey(tradeClone, keys)) {
                        if (netted.getUnderlyingTransfers() == null)
                            netted.setUnderlyingTransfers(new TransferArray());

                        netted.getUnderlyingTransfers().add(tradeClone);
                        found = true;
                    }
                }

                if (!found) {
                    BOTransfer netted = tradeTransfers.get(i).initialize(keys);
                    netted.setNettingType(context.getNettingMethod());
                    nettedTransfers.add(netted);
                    if (netted.getUnderlyingTransfers() == null)
                        netted.setUnderlyingTransfers(new TransferArray());

                    netted.getUnderlyingTransfers().add(tradeTransfers.get(i));
                }

            }

            //COMPUTE TOTAL NOMINAL, QUANTITY, TOTAL CASH ETC
            return nettedTransfers.stream().peek(netted -> {
                double nom = 0, amt = 0, cash = 0;
                netted.setStatus(Status.S_NONE);
                netted.setAction(Action.NEW);
                for (BOTransfer unl : netted.getUnderlyingTransfers()) {
                    double sign = "PAY".equals(unl.getPayReceive()) ? -1 : 1;
                    if ("SECURITY".equals(unl.getTransferType())) {
                        nom += sign * Math.abs(unl.getNominalAmount());
                        amt += sign * Math.abs(unl.getSettlementAmount());
                        cash += -sign * Math.abs(unl.getOtherAmount());
                    } else {
                        amt += sign * Math.abs(unl.getSettlementAmount());
                        cash += sign * Math.abs(unl.getSettlementAmount());
                    }
                }
                if (nom != 0) {
                    netted.setPayReceive(nom < 0 ? "PAY" : "RECEIVE");
                    netted.setNominalAmount(nom);
                    netted.setSettlementAmount(amt);
                    netted.setRealSettlementAmount(amt);
                    netted.setOtherAmount(cash);
                    netted.setRealCashAmount(cash);
                } else {
                    netted.setPayReceive(amt < 0 ? "PAY" : "RECEIVE");
                    netted.setSettlementAmount(amt);
                    netted.setRealSettlementAmount(amt);
                    netted.setRealCashAmount(cash);
                    netted.setNominalAmount(0);
                    netted.setOtherAmount(0);
                }
            }).collect(Collectors.toList());
        }

        private List<Pair<BOTransfer, LCHObligation>> match
                (List<BOTransfer> unmatchedNetTransfers, List<LCHObligation> unmatchedObligations, LCHObligationSetIdentifier
                        identifier) {
            List<BOTransfer> xfers;
            List<LCHObligation> obligations;
            List<Pair<BOTransfer, LCHObligation>> matched;
            String ccy = !Util.isEmpty(unmatchedNetTransfers) ? unmatchedNetTransfers.get(0).getSettlementCurrency()
                    : !Util.isEmpty(unmatchedObligations) ? unmatchedObligations.get(0).getCashAmountCurrency() : "EUR";
            int le = !Util.isEmpty(unmatchedNetTransfers) ? unmatchedNetTransfers.stream().map(BOTransfer::getOriginalCptyId).filter(originalCptyId -> originalCptyId > 0).findFirst().orElse(0) : 0;

            double toleranceAmt = getTolerance(le, ccy, "Clearing");
            double tolerance = 0;
            do {
                xfers = new ArrayList<>(unmatchedNetTransfers);
                obligations = new ArrayList<>(unmatchedObligations);
                matched = match(xfers, obligations, identifier, tolerance);
                tolerance += toleranceAmt / 5;
            } while (tolerance <= toleranceAmt && !Util.isEmpty(xfers) && !Util.isEmpty(obligations));

            unmatchedNetTransfers.clear();
            unmatchedNetTransfers.addAll(xfers);
            unmatchedObligations.clear();
            unmatchedObligations.addAll(obligations);
            return matched;
        }

        private List<Pair<BOTransfer, LCHObligation>> match
                (List<BOTransfer> unmatchedNetTransfers, List<LCHObligation> unmatchedObligations, LCHObligationSetIdentifier
                        identifier, double tolerance) {
            int o = 0;
            List<Pair<BOTransfer, LCHObligation>> matched = new ArrayList<>();
            while (o < unmatchedObligations.size() && unmatchedNetTransfers.size() > 0 && unmatchedObligations.size() > 0) {
                LCHObligation obligation = unmatchedObligations.get(o);
                boolean isMatch = false;
                for (int t = 0; t < unmatchedNetTransfers.size(); t++) {
                    BOTransfer xfer = unmatchedNetTransfers.get(t);

                    if (match(xfer, obligation, identifier, tolerance) == 0) {
                        unmatchedNetTransfers.remove(xfer);
                        unmatchedObligations.remove(obligation);
                        matched.add(Pair.of(xfer, obligation));
                        isMatch = true;
                        break;
                    }
                }
                if (!isMatch)
                    o++;

            }
            return matched;
        }

        private int match(BOTransfer xfer, LCHObligation obligation, LCHObligationSetIdentifier identifier,
                          double tolerance) {
            double xferNom = 0;
            double xferCash;
            double sign = "PAY".equals(xfer.getPayReceive()) ? -1 : 1;
            if ("SECURITY".equals(xfer.getTransferType())) {
                xferNom = sign * Math.abs(xfer.getNominalAmount());
                xferCash = -sign * Math.abs(xfer.getOtherAmount());

            } else {
                xferCash = sign * Math.abs(xfer.getSettlementAmount());
            }

            sign = "LCH".equals(obligation.getBondsReceiver()) ? -1 : 1;
            double lchNom = sign * Math.abs(obligation.getNominalInstructed());
            sign = "LCH".equals(obligation.getCashReceiver()) ? -1 : 1;
            double lchCash = sign * Math.abs(obligation.getCashAmountInstructed());
            if ("FoP".equals(obligation.getSettlementType()))
                xferCash = 0;

            if (RECON_TYPE_GS.equals(context.getReconType())) {
                if (lchNom != 0 && Math.abs(lchNom + lchCash) <= REPORT_VALIDATION_TOLERANCE) {
                    lchNom = 0;
                }
            }

            return withInTolerance(xferNom, lchNom, tolerance) && withInTolerance(xferCash, lchCash, tolerance)
                    && (xferCash == 0 || xfer.getSettlementCurrency().equals(obligation.getCashAmountCurrency()))
                    && xfer.getValueDate().equals(identifier.getIntendedSettlementDate()) ? 0 : 1;
        }

        private boolean reconcileTradeTransfers(List<LCHNettingSet> nettingSets, LCHObligationSet
                obligationSet, Map<String, List<BOTransfer>> transfersByRef, List<BOTransfer> nettedTradeTransfers) {
            for (LCHNettingSet nettingSet : nettingSets) {
                double cashAmt = 0;
                double nominalAmount = 0;
                int leId = 0;
                String ccy = null;
                for (LCHNettedTrade lchTrade : nettingSet.getNettedTrades()) {

                    for (BOTransfer xfer : transfersByRef.get(lchTrade.getBuyerSellerReference())) {
                        if (!xfer.getValueDate().equals(nettingSet.getIdentifier().getSettlementDate()))
                            continue;

                        if (RECON_TYPE_GS.equals(context.getReconType()) && "ReturnLeg".equals(xfer.getAttribute("DAPMatchKey1")) && "PRINCIPAL".equals(xfer.getTransferType()))
                            continue;

                        leId = xfer.getOriginalCptyId() > 0 ? xfer.getOriginalCptyId() : leId;
                        ccy = xfer.getSettlementCurrency();
                        double sign = "PAY".equals(xfer.getPayReceive()) ? -1 : 1;
                        if ("SECURITY".equals(xfer.getTransferType())) {
                            boolean add = false;
                            Product prod = BOCache.getExchangedTradedProduct(context.getDSConnection(), xfer.getProductId());
                            String isin = prod.getSecCode("ISIN");
                            if (isin != null && prod.getSecCode("ISIN").equals(nettingSet.getIdentifier().getIsin())) {
                                nominalAmount += sign * Math.abs(xfer.getNominalAmount());
                                add = true;
                            }
                            if (xfer.getSettlementCurrency().equals(nettingSet.getIdentifier().getSettlementCurrency())) {
                                cashAmt += -sign * Math.abs(xfer.getOtherAmount());
                                add = true;
                            }
                            if (add)
                                nettedTradeTransfers.add(xfer);
                        } else {
                            if (xfer.getSettlementCurrency().equals(nettingSet.getIdentifier().getSettlementCurrency())) {
                                nettedTradeTransfers.add(xfer);
                                cashAmt += sign * Math.abs(xfer.getSettlementAmount());
                            }


                        }
                    }
                }
                double lchNominal = 0, lchCash = 0;
                for (LCHNetPosition nettedPos : nettingSet.getNetPositions()) {
                    double nomSign = "LCH".equals(nettedPos.getBondsReceiver()) ? -1 : 1;

                    double cashSign = "LCH".equals(nettedPos.getCashReceiver()) ? -1 : 1;

                    lchNominal += nomSign * Math.abs(nettedPos.getNominalAmount());
                    lchCash += cashSign * Math.abs(nettedPos.getCashAmount());
                }
                double tolerance = getTolerance(leId, ccy, context.getToleranceType());
                if (RECON_TYPE_GS.equals(context.getReconType())) {
                    if (lchNominal != 0 && Math.abs(lchNominal + lchCash) > REPORT_VALIDATION_TOLERANCE) {
                        report.mismatch(nettedTradeTransfers, obligationSet.getObligations(),
                                String.format("General Collateral Recon, expected LCH nominal equals to  LCH cash  [LCH nominal %f, LCH cash %f], Buyer Seller References [%s]",
                                        lchNominal, lchCash, nettingSet.getNettedTrades().stream().map(LCHNettedTrade::getBuyerSellerReference).collect(Collectors.joining(", "))));

                        return false;
                    }
                    lchNominal = 0;
                }

                if (!withInTolerance(lchNominal, nominalAmount, tolerance) || !withInTolerance(lchCash, cashAmt, tolerance)) {
                    report.mismatch(nettedTradeTransfers, obligationSet.getObligations(),
                            String.format("Reconciliation failed for netting set %s, Calypso totals [nominal %f, cash %f], LCH totals  [nominal %f, cash %f], Buyer Seller References [%s]", nettingSet,
                                    nominalAmount, cashAmt, lchNominal, lchCash, nettingSet.getNettedTrades().stream().map(LCHNettedTrade::getBuyerSellerReference).collect(Collectors.joining(", "))));

                    return false;
                }
                // check is sum up to obligations


            }

            return true;
        }

        private boolean withInTolerance(double d1, double d2, double tolerance) {
            return Math.abs(d1 - d2) <= tolerance;
        }

        private PricingEnv getPricingEnv() {
            return pe;
        }

        private double getTolerance(int leId, String ccy, String type) {
            double toleranceAmt = ReconCCPConstants.TOLERANCE;
            String toleranceAmtCC = "EUR";
            List<LegalEntityTolerance> tolerances = BOCache.getLegalEntityTolerances(context.getDSConnection(), leId);
            if (!Util.isEmpty(tolerances)) {
                Optional<LegalEntityTolerance> tol = tolerances.stream().filter(t -> type.equals(t.getToleranceType()) && ccy.equals(t.getCurrency())).findFirst();
                if (tol.isPresent())
                    return tol.get().getAmount();
                tol = tolerances.stream().filter(t -> type.equals(t.getToleranceType()) && "ANY".equals(t.getCurrency())).findFirst();
                if (tol.isPresent())
                    return tol.get().getAmount();

                tol = tolerances.stream().filter(t -> type.equals(t.getToleranceType())).findFirst();

                if (tol.isPresent()) {
                    toleranceAmt = tol.get().getAmount();
                    toleranceAmtCC = tol.get().getCurrency();
                }
            }
            try {
                toleranceAmt = CurrencyUtil.convertAmount(getPricingEnv(), toleranceAmt, toleranceAmtCC, ccy, context.getValDate(), getPricingEnv().getQuoteSet());
            } catch (MarketDataException e) {
                Log.error(LOG_CATEGORY, String.format("Cannot cover tolerance to from %s to %s, %s", toleranceAmtCC, ccy, e));
            }
            return toleranceAmt;
        }

    }

    private enum ReconResult {
        OK, KO, Alleged, Error, Unmatched
    }

    static class MatchingResultReport {
        private final List<MatchingResult> results = new ArrayList<>();

        private void matched(BOTransfer nettedTransfer, LCHObligation obligation) {
            matched(nettedTransfer, obligation, null);
        }

        private void matched(BOTransfer nettedTransfer, LCHObligation obligation, String comment) {

            results.add(new MatchingResult(nettedTransfer, obligation, ReconResult.OK, comment));
        }

        private void mismatch(List<BOTransfer> nettedTransfer, List<LCHObligation> obligation, String msg) {
            results.add(new MatchingResult(nettedTransfer, obligation, ReconResult.KO, msg));
        }


        public void allege(LCHObligationSet obligationSet, String msg) {
            results.add(new MatchingResult(null, obligationSet.getObligations(), ReconResult.Alleged, msg));
        }

        public void unmatched(TransferArray transfers, String msg) {
            results.add(new MatchingResult(new ArrayList<>(transfers), null, ReconResult.Unmatched, msg));
        }

        public void error(TransferArray xfers, String msg) {
            results.add(new MatchingResult(xfers == null ? null : new ArrayList<>(xfers), null, ReconResult.Error, msg));
        }

        public void error(List<BOTransfer> xfers, String msg) {
            results.add(new MatchingResult(xfers, null, ReconResult.Error, msg));
        }

        private List<MatchingResult> getMatchingResults() {
            return results;
        }
    }

    private static class MatchingResult {
        private final List<BOTransfer> xfers;
        private final List<LCHObligation> obligations;
        private final String msg;

        private final ReconResult result;

        private MatchingResult(BOTransfer xfer, LCHObligation obligations, ReconResult result, String msg) {
            this(xfer == null ? Collections.emptyList() : Collections.singletonList(xfer), obligations == null ? Collections.emptyList() : Collections.singletonList(obligations), result, msg);
        }

        private MatchingResult(List<BOTransfer> xfers, List<LCHObligation> obligations, ReconResult result, String msg) {
            this.xfers = xfers;
            this.obligations = obligations;
            this.result = result;
            this.msg = msg;
        }

        public List<LCHObligation> getObligations() {
            return obligations;
        }

        public String getMsg() {
            return msg;
        }

        public ReconResult getResult() {
            return result;
        }

        public List<BOTransfer> getTransfers() {
            return xfers;
        }
    }

}
