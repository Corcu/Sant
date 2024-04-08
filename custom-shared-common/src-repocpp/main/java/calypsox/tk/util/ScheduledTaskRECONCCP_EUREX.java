package calypsox.tk.util;

import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.ReconCCPMatchingResult;
import calypsox.repoccp.model.eurex.*;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.LegalEntityTolerance;
import com.calypso.tk.service.DSCommand;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static calypsox.repoccp.ReconCCPConstants.*;
import static com.calypso.tk.util.ParallelExecutionException.Type.EPILOGUE;
import static com.calypso.tk.util.ParallelExecutionException.Type.INIT;

public class ScheduledTaskRECONCCP_EUREX extends ScheduledParallelTask<Boolean, ScheduledTaskRECONCCP_EUREX.ReconciliationReport> {

    private static final String THREAD_POOL_SIZE = "Thread Pool Size";

    public static final String FILE_NAME = "File Name";
    //CCP reconciliation file location
    public static final String FILE_PATH = "File Path";

    public static final String REPORT_FILE_PATH = "Report File Path";
    public static final String REPORT_FILE_NAME = "Report File Name";
    public static final String APPLY_ACTION = "Apply Action";

    public static final String TOLERANCE_TYPE = "Tolerance Type";

    public static final String CREATE_TASKS = "Create Tasks";

    public static final String FILTER_BY_FILE_TRADE_RECON_OK = "Filter file trades by Recon OK";

    public static final String REPORT_HEADER = "TradeId,Reference,Result,Errors,Warnings";

    /*
    Please note that the KW name is misspelled !
     */
    public static final String TRADE_KW_UTR = "UTI_REFRENCE";

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        final Vector<String> nettingTypes = LocalCache.getDomainValues(getDSConnection(), "nettingType");
        nettingTypes.sort(Comparator.naturalOrder());
        return Arrays.asList(
                attribute(FILE_PATH).mandatory(),
                attribute(FILE_NAME).mandatory(),
                attribute(REPORT_FILE_PATH).mandatory(),
                attribute(REPORT_FILE_NAME).mandatory(),
                attribute(APPLY_ACTION).domainName("tradeAction").mandatory(),
                attribute(TOLERANCE_TYPE).domain(currentAttributes -> {
                    List<String> types = new ArrayList<>(Arrays.asList("Automatic", "Manual", "ReceiptMsg"));
                    types.addAll(LocalCache.getDomainValues(DSConnection.getDefault(), "leToleranceType"));
                    return types.stream().distinct().sorted().collect(Collectors.toList());
                }).mandatory(),
                attribute(CREATE_TASKS).booleanType().mandatory(),
                attribute(FILTER_BY_FILE_TRADE_RECON_OK).booleanType().mandatory(),
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
                error = !parallelRun(ds, ps); //

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

    @Override
    public IExecutionContext createExecutionContext(DSConnection ds, PSConnection ps) {
        return new ExecutionContext(ds, ps, getIntegerAttribute(THREAD_POOL_SIZE, 1), getPricingEnv(), getValuationDatetime(), getTradeFilter(), getAttribute(TOLERANCE_TYPE, "Clearing"),
                Action.valueOf(getAttribute(APPLY_ACTION, "AMEND_RECON")));
    }

    private String getAttribute(String attr, String defaultVal) {
        String val = getAttribute(attr);
        return Util.isEmpty(val) ? defaultVal : val;
    }

    @Override
    public List<? extends Callable<ReconciliationReport>> split(IExecutionContext iContext) throws ParallelExecutionException {
        ExecutionContext context = (ExecutionContext) iContext;

        String fileName = getAttribute(FILE_NAME);
        String filePath = getAttribute(FILE_PATH);
        Map<String, String> valMap = ScheduledTaskUtil.getValueMap(this);
        StrSubstitutor sub = new StrSubstitutor(valMap);
        // Replace
        String filePathResolved = sub.replace(filePath);
        String fileNameResolved = sub.replace(fileName);

        File[] files = new File(filePathResolved).listFiles((FileFilter) new WildcardFileFilter(fileNameResolved, IOCase.INSENSITIVE));
        if (Util.isEmpty(files)) {
            throw new ParallelExecutionException(INIT, String.format("No files found by mask %s in directory %s. Nothing to process.", fileNameResolved, filePathResolved));
        }

        List<ReconciliationBatch> tasks = new ArrayList<>();

        List<File> sortedFiles = Arrays.stream(files).sorted((f1, f2) -> {
            int p = f1.getName().indexOf('.');
            String suffix1 = p > 0 ? f1.getName().substring(0, p) : FilenameUtils.getBaseName(f1.getName());
            p = f2.getName().indexOf('.');
            String suffix2 = p > 0 ? f2.getName().substring(0, p) : FilenameUtils.getBaseName(f2.getName());
            return suffix1.substring(Math.max(suffix1.length() - 2, 0)).compareTo(suffix2.substring(Math.max(suffix2.length() - 2, 0)));
        }).collect(Collectors.toList());

        File lastFile = sortedFiles.get(sortedFiles.size() - 1);
        context.setLastFile(lastFile);

        List<EurexReconRecord> reconRecords = new ArrayList<>();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Tc800Type.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();


            for (File file : sortedFiles) {

                Log.info(LOG_CATEGORY, String.format("Processing file %s.", file.getName()));
                List<Tc800Type> reports = new ArrayList<>();
                String extension = FilenameUtils.getExtension(file.getName());
                if ("zip".equalsIgnoreCase(extension) || "gzip".equalsIgnoreCase(extension)) {

                    try (ZipFile zipFile = new ZipFile(file)) {
                        Enumeration<? extends ZipEntry> entries = zipFile.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            // Check if entry is a directory
                            if (!entry.isDirectory()) {
                                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                                    reports.add((Tc800Type) jaxbUnmarshaller.unmarshal(inputStream));
                                }
                            }
                        }
                    }
                } else {
                    reports.add((Tc800Type) jaxbUnmarshaller.unmarshal(file));
                }

                for (Tc800Type report : reports) {
                    List<String> errors = validate(report);
                    if (!Util.isEmpty(errors)) {
                        String msg = String.format("Report %s was rejected.", file.getName());
                        Log.error(LOG_CATEGORY, msg);
                        Log.error(LOG_CATEGORY, String.join("\n", errors));
                        throw new ParallelExecutionException(INIT, String.join("\n", errors));
                    }

                    if (Util.isEmpty(report.getTc800Grp())) {
                        Log.info(LOG_CATEGORY, String.format("Empty report, %s.", file));
                    } else {
                        //flatten the report structure

                        for (Tc800GrpType gr : report.getTc800Grp()) {
                            String membClgIdCod = gr.getTc800KeyGrp().getMembClgIdCod();
                            for (Tc800Grp1Type gr1 : gr.getTc800Grp1()) {
                                String settleAcc = gr1.getTc800KeyGrp1().getSettlAcct();
                                SettlLocType settleLoco = gr1.getTc800KeyGrp1().getSettlLoc();
                                for (Tc800Grp2Type gr2 : gr1.getTc800Grp2()) {
                                    SettlCurrencyType settlCurrency = gr2.getTc800KeyGrp2().getSettlCurrency();
                                    for (Tc800Grp3Type gr3 : gr2.getTc800Grp3()) {
                                        InstTypCodType instTypCodType = gr3.getInstTypCod();
                                        String isin = gr3.getTc800KeyGrp3().getIsin();
                                        for (Tc800Grp4Type gr4 : gr3.getTc800Grp4()) {
                                            String membTrdngIdCod = gr4.getTc800KeyGrp4().getMembTrdngIdCod();
                                            for (Tc800Grp5Type gr5 : gr4.getTc800Grp5()) {
                                                AcctTypType acctTypType = gr5.getTc800KeyGrp5().getAcctTyp();
                                                for (Tc800Grp6Type gr6 : gr5.getTc800Grp6()) {


                                                    JDate tradeDate = JDate.valueOf(gr6.getTc800KeyGrp6().getTrdDat().getYear(),
                                                            gr6.getTc800KeyGrp6().getTrdDat().getMonth(),
                                                            gr6.getTc800KeyGrp6().getTrdDat().getDay()
                                                    );

                                                    for (Tc800Grp7Type gr7 : gr6.getTc800Grp7()) {
                                                        reconRecords.add(EurexReconRecord.newRecord()
                                                                .withMembClgIdCod(membClgIdCod)
                                                                .withSettlAcct(settleAcc)
                                                                .withSettlLoco(settleLoco)
                                                                .withSettlCurrency(settlCurrency)
                                                                .withInstTypCod(instTypCodType)
                                                                .withIsin(isin)
                                                                .withMembTrdngIdCod(membTrdngIdCod)
                                                                .withAcctTyp(acctTypType)
                                                                .withTradeDate(tradeDate)
                                                                .withTrdLoc(gr7.getTc800KeyGrp7().getTrdLoc())
                                                                .withTrdNum(gr7.getTc800KeyGrp7().getTrdNum())
                                                                .withRpoBankIntRef(gr7.getRpoBankIntRef())
                                                                .withRpoClgTmStmp(gr7.getRpoClgTmStmp() == null ? null : new JDatetime(gr7.getRpoClgTmStmp().toGregorianCalendar().getTime()))
                                                                .withRpoTrdTmStmp(gr7.getRpoTrdTmStmp() == null ? null : new JDatetime(gr7.getRpoTrdTmStmp().toGregorianCalendar().getTime()))
                                                                .withRpoTrdTyp(gr7.getRpoTrdTyp())
                                                                .withRpoBankIntRef(gr7.getRpoBankIntRef())
                                                                .withRpoUTI(gr7.getRpoUTI())
                                                                .withOrdrNum(gr7.getOrdrNum())
                                                                .withCmpTrd(gr7.getRpoCmpTrd())
                                                                .withTradeLegs(gr7.getTc800Rec())
                                                                .build()
                                                        );
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            throw new ParallelExecutionException(INIT, e);
        }

        if (getBooleanAttribute(FILTER_BY_FILE_TRADE_RECON_OK)) {
            reconRecords = filterReconciledRecords(getTradeFilter(), reconRecords, context);
        }
        if (Util.isEmpty(reconRecords)) {
            tasks.add(new ReconciliationBatch(context, reconRecords, null, null));
        } else {
            reconRecords.sort(Comparator.comparing(EurexReconRecord::getRpoUTI));

            int chunkSize = reconRecords.size() / context.getThreadPoolSize();
            int chunkRem = reconRecords.size() % context.getThreadPoolSize();
            String startRef = null;

            for (int x = 0; x < reconRecords.size(); x += chunkSize) {
                if (tasks.size() == context.getThreadPoolSize() - 1)
                    chunkSize += chunkRem;
                List<EurexReconRecord> chunk = reconRecords.subList(x, Math.min(x + chunkSize, reconRecords.size()));
                String endRef = x + chunkSize < reconRecords.size() ? chunk.get(chunk.size()-1).getRpoUTI() : null;
                tasks.add(new ReconciliationBatch(context, chunk, startRef, endRef));
                startRef = endRef;
            }
        }

        return tasks;
    }

    private List<String> validate(Tc800Type report) {
        List<String> errors = new ArrayList<>();
        RptHdrType hdr = report.getRptHdr();
        if (hdr == null) {
            errors.add("Missing report header.");
        } else {
            if (hdr.getRptPrntEffDat() == null) {
                errors.add("Missing report effective date.");
            } else {
                JDate valDate = getValuationDatetime().getJDate(getTimeZone());
                JDate rptEffectiveDate = JDate.valueOf(hdr.getRptPrntEffDat().getYear(), hdr.getRptPrntEffDat().getMonth(), hdr.getRptPrntEffDat().getDay());

                if (!valDate.equals(rptEffectiveDate)) {
                    errors.add(String.format("Invalid Report Effective Date %s, ST Value Date %s report effective date.", rptEffectiveDate, valDate));
                }
            }

            if (!"TC800".equals(hdr.getRptCod()))
                errors.add(String.format("Invalid Report Type %s, expected TC800.", hdr.getRptCod()));
        }
        return errors;
    }

    private List<EurexReconRecord> filterReconciledRecords(String tradeFilter, List<EurexReconRecord> reconRecords, ExecutionContext context) throws ParallelExecutionException {
        if (Util.isEmpty(reconRecords))
            return reconRecords;
        try {
            List<String> utis = reconRecords.stream().map(EurexReconRecord::getRpoUTI).collect(Collectors.toList());


            StringBuilder from = new StringBuilder();
            StringBuilder where = new StringBuilder();
            List<CalypsoBindVariable> bindVars = new ArrayList<>();
            if (!Util.isEmpty(tradeFilter)) {
                TradeFilter tf = BOCache.getTradeFilter(context.getDSConnection(), context.getTradeFilter());
                tf = (TradeFilter) tf.clone();
                tf.removeCriterion("KEYWORD_CRITERION");
                tf.setValDate(context.getValuationDatetime());

                from.append("trade, product_desc");
                String filterFrom = context.getDSConnection().getRemoteReferenceData().generateFromClause(tf);
                if (!Util.isEmpty(filterFrom))
                    from.append(", ").append(filterFrom);

                Object[] filterWhereAndVars = context.getDSConnection().getRemoteReferenceData().generateWhereClause(tf, bindVars);

                where.append("trade.product_id=product_desc.product_id");
                if (!Util.isEmpty((String) filterWhereAndVars[0]))
                    where.append(" AND ");
                where.append((String) filterWhereAndVars[0]);
                bindVars.addAll((Collection<? extends CalypsoBindVariable>) filterWhereAndVars[1]);
            }

            bindVars.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, TRADE_KW_UTR));

            from.append(from.length() > 0 ? ", trade_keyword uti" : "trade_keyword uti");
            where.append(" AND trade.trade_id = uti.trade_id and uti.keyword_name=? AND trade.trade_id in (select trade_id from trade_keyword where keyword_name='Recon' AND keyword_value='OK') AND (");

            int chunkSize = Util.getMaxItemsInQuery();
            for (int x = 0; x < reconRecords.size(); x += chunkSize) {
                List<String> chunk = utis.subList(x, Math.min(x + chunkSize, reconRecords.size()));
                if (x > 0)
                    where.append(" OR ");
                where.append("uti.keyword_value IN ").append(Util.collectionToSQLString(chunk));
            }
            where.append(")");

            DSCommand getReconciledRefs = new DSCommandSelectUTI("uti.keyword_value", from.toString(), where.toString(), bindVars);

            Set<String> found = new HashSet<String>((Collection) context.getDSConnection().getRemoteAccess().execute(getReconciledRefs));

            return reconRecords.stream().filter(r -> !found.contains(r.getRpoUTI())).collect(Collectors.toList());
        } catch (Exception e) {
            throw new ParallelExecutionException(INIT, e);
        }
    }


    private File getLastReport(File[] files) {

        return Arrays.stream(files).sorted((f1, f2) -> {
            int p = f1.getName().indexOf('.');
            String suffix1 = p > 0 ? f1.getName().substring(0, p) : FilenameUtils.getBaseName(f1.getName());
            p = f2.getName().indexOf('.');
            String suffix2 = p > 0 ? f2.getName().substring(0, p) : FilenameUtils.getBaseName(f2.getName());
            return suffix1.substring(Math.max(suffix1.length() - 2, 0)).compareTo(suffix2.substring(Math.max(suffix2.length() - 2, 0)));
        }).reduce((f, s) -> s).orElse(null);

    }

    @Override
    public Boolean epilogue(List<? extends Callable<ReconciliationReport>> jobs, List<Future<ReconciliationReport>> results, IExecutionContext context) throws ParallelExecutionException {

        String filePath = getAttribute(REPORT_FILE_PATH);
        String fileName = getAttribute(REPORT_FILE_NAME);
        TaskArray tasks = new TaskArray();
        boolean createTasks = getBooleanAttribute(CREATE_TASKS);


        Map<String, String> valueMap = ScheduledTaskUtil.getValueMap(this);
        valueMap.put("FILE_NAME", FilenameUtils.getBaseName(((ExecutionContext) context).getLastFile().getName()));
        valueMap.put("FILE_EXTENSION", FilenameUtils.getExtension(((ExecutionContext) context).getLastFile().getName()));
        StrSubstitutor sub = new StrSubstitutor(valueMap);
        // Replace
        String filePathResolved = sub.replace(filePath);
        String fileNameResolved = sub.replace(fileName);
        String fullPath = filePathResolved.endsWith("\\") || filePathResolved.endsWith("/") ? filePathResolved + fileNameResolved : filePathResolved + "/" + fileNameResolved;
        try (FileOutputStream outputStream = new FileOutputStream(fullPath)) {
            outputStream.write(REPORT_HEADER.getBytes(StandardCharsets.UTF_8));
            outputStream.write('\n');
            for (Future<ReconciliationReport> report : results) {
                for (ReconCCPMatchingResult result : report.get().getMatchingResults()) {
                    if (createTasks) {

                        if (result.isMatched() && result.getTrade() != null) {
                            Task task = createTask(result);
                            if (task != null && (result.hasWarnings() || result.hasErrors()) && ReconCCPUtil.exceptionTaskNotFound(task, context.getDSConnection()))
                                tasks.add(task);

                            tasks.addAll(ReconCCPUtil.getTasksToClose(result.getTrade().getLongId(), result.getReference(),
                                    result.hasWarnings() && task != null ? t -> !EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP.equals(t.getEventType()) || !task.getComment().equals(t.getComment()) : null, context.getDSConnection()));

                        } else {
                            Task task = createTask(result);
                            if (ReconCCPUtil.exceptionTaskNotFound(task, context.getDSConnection())) {
                                tasks.add(task);
                                tasks.addAll(ReconCCPUtil.getTasksToClose(result.getReference(),
                                        t -> !task.getEventType().equals(t.getEventType()) || !task.getComment().equals(t.getComment()), context.getDSConnection()));
                            }
                        }
                    }

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

    private Task createTask(ReconCCPMatchingResult result) {
        Task task = null;
        if (result.isMatched()) {
            if (result.hasErrors() || result.hasWarnings()) {
                task = new Task(result.getTrade());
                if (result.hasErrors()) {
                    task.setComment(StringUtils.abbreviate(result.getMatchingErrors(), 255));
                    task.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP);
                    task.setStatus(Task.NEW);
                    task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    task.setDatetime(new JDatetime());
                    task.setNewDatetime(new JDatetime());
                    task.setPriority(Task.PRIORITY_HIGH);
                    task.setNextPriority(Task.PRIORITY_HIGH);
                    task.setNextPriorityDatetime(null);
                    task.setAttribute("ERROR");
                } else {
                    task.setComment(StringUtils.abbreviate(result.getMatchingWarnings(), 255));
                    task.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP);
                    task.setStatus(Task.NEW);
                    task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    task.setDatetime(new JDatetime());
                    task.setNewDatetime(new JDatetime());
                    task.setPriority(Task.PRIORITY_LOW);
                    task.setNextPriority(Task.PRIORITY_LOW);
                    task.setNextPriorityDatetime(null);
                    task.setAttribute("WARN");
                }
                //to do check for duplicates

            }

        } else {
            task = result.getTrade() == null ? new Task() : new Task(result.getTrade());
            task.setComment(StringUtils.abbreviate(result.getUnmatchedErrors(), 255));
            if (result.getTrade() == null) {
                task.setEventType(EXCEPTION_MISSING_TRADE_RECON_CCP);
                task.setInternalReference(result.getReference());
            } else {
                task.setEventType(EXCEPTION_CALYPSO_TRADE_UNMATCHED_RECON_CCP);
                task.setTradeLongId(result.getTrade().getLongId());
            }
            task.setStatus(Task.NEW);
            task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
            task.setPriority(Task.PRIORITY_HIGH);
            task.setNextPriority(Task.PRIORITY_HIGH);
            task.setDatetime(new JDatetime());
            task.setNewDatetime(new JDatetime());
            task.setAttribute("ERROR");
        }

        return task;
    }

    private String formatResult(ReconCCPMatchingResult result) {
        StringBuilder sb = new StringBuilder();
        if (result.getTrade() != null)
            sb.append(result.getTrade().getLongId());

        sb.append(",").append(Util.isEmpty(result.getReference()) ? "" : result.getReference()).append(",");

        sb.append(result.hasErrors() || !result.isMatched() ? "KO" : "OK").append(",");

        if (!Util.isEmpty(result.getMatchingErrorsList())) {
            sb.append("\"").append(String.join(";", result.getMatchingErrorsList())).append("\"").append(",");
        } else {
            sb.append(",");
        }
        if (result.hasWarnings()) {
            sb.append("\"").append(String.join(";", result.getMatchingWarnings())).append("\"");
        }

        return sb.toString();
    }

    @Override
    public String getTaskInformation() {
        return "Reconciles Calypso trades with Eurex TC800 Trade Repo Confirmation Report.";
    }

    static class ReconciliationBatch implements Callable<ReconciliationReport> {
        private final ReconciliationReport report = new ReconciliationReport();
        private final ExecutionContext context;
        private final List<EurexReconRecord> reconRecords;

        private final String startRef;
        private final String endRef;

        Map<Pair<Integer, String>, Double> tolerances = new HashMap<>();

        public ReconciliationBatch(ExecutionContext context, List<EurexReconRecord> reconRecords, String startRef, String endRef) {
            this.context = context;
            this.reconRecords = reconRecords;
            this.startRef = startRef;
            this.endRef = endRef;
        }

        @Override
        public ReconciliationReport call() throws Exception {

            PricingEnv pe = context.getDSConnection().getRemoteMarketData().getPricingEnv(context.getPEName());


            //recon records are sorted by UTR (recon key)
            List<CalypsoBindVariable> bindVars = new ArrayList<>();
            TradeFilter tf = BOCache.getTradeFilter(context.getDSConnection(), context.getTradeFilter());
            tf = (TradeFilter) tf.clone();
            tf.setValDate(context.getValuationDatetime());

            String filterFrom = context.getDSConnection().getRemoteReferenceData().generateFromClause(tf);
            Object[] filterWhereAndVars = context.getDSConnection().getRemoteReferenceData().generateWhereClause(tf, bindVars);
            String filterWhere = (String) filterWhereAndVars[0];

            bindVars = (List<CalypsoBindVariable>) filterWhereAndVars[1];
            String where = filterWhere;
            if (startRef != null || endRef != null) {

                if (startRef == null) {

                    where = filterWhere + " AND trade.trade_id in (SELECT trade_id FROM trade_keyword WHERE " +
                            "keyword_name = '" + TRADE_KW_UTR + "' AND keyword_value <= ?)";
                    bindVars.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, endRef));
                } else {
                    if (endRef == null) {
                        where = filterWhere + " AND trade.trade_id in (SELECT trade_id FROM trade_keyword WHERE " +
                                "keyword_name = '" + TRADE_KW_UTR + "' AND keyword_value > ?)";
                        bindVars.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, startRef));
                    } else {
                        where = filterWhere + " AND trade.trade_id in (SELECT trade_id FROM trade_keyword WHERE " +
                                "keyword_name = '" + TRADE_KW_UTR + "' AND keyword_value > ? AND keyword_value <= ?)";
                        bindVars.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, startRef));
                        bindVars.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, endRef));
                    }
                }

            }


            TradeArray trades = context.getDSConnection()
                    .getRemoteTrade()
                    .getTrades(filterFrom, where, null, bindVars);

            trades.sort((Comparator<Trade>) (t1, t2) -> {
                //a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
                String uti1 = t1.getKeywordValue(TRADE_KW_UTR);
                String uti2 = t2.getKeywordValue(TRADE_KW_UTR);
                if (uti1 == null)
                    return uti2 == null ? 0 : -1;

                return uti2 == null ? 1 : uti1.compareTo(uti2);
            }); //Comparator.comparing((Trade t) -> t.getKeywordValue(TRADE_KW_UTR)));

            reconcileTrades(trades, reconRecords, pe);

            return report;
        }

        private void reconcileTrades(TradeArray trades, List<EurexReconRecord> reconRecords, PricingEnv pe) {

            int tradeIndex = 0, recordIndex = 0;

            while (tradeIndex < trades.size() || recordIndex < reconRecords.size()) {

                if (recordIndex >= reconRecords.size()) {
                    report.missing(trades.get(tradeIndex));
                    tradeIndex++;

                } else if (tradeIndex >= trades.size()) {
                    report.alleged(reconRecords.get(recordIndex));
                    recordIndex++;

                } else {

                    Trade trade = trades.get(tradeIndex);
                    EurexReconRecord rec = reconRecords.get(recordIndex);

                    String tradeUtr = trade.getKeywordValue(TRADE_KW_UTR);

                    int comp = tradeUtr == null
                            ? (rec.getRpoUTI() == null ? 0 : -1)
                            : (rec.getRpoUTI() == null ? 1 : tradeUtr.compareTo(rec.getRpoUTI()));

                    if (comp == 0) { //ref matched
                        ReconCCPMatchingResult result = matchFields(trade, rec, pe);
                        updateTrade(result);
                        report.add(result);
                        tradeIndex++;
                        recordIndex++;
                    } else if (comp < 0) { // missing in Eurex
                        report.missing(trade);
                        tradeIndex++;
                    } else {
                        report.alleged(rec);
                        recordIndex++;
                    }
                }


            }
        }

        private void updateTrade(ReconCCPMatchingResult result) {
            String kwVal = result.hasErrors() ? RECON_KO : RECON_OK;
            if (kwVal.equals(result.getTrade().getKeywordValue(TRADE_KEYWORD_RECON)))
                return;

            Trade trade = result.getTrade().clone();
            trade.setAction(context.getWfAction());
            trade.addKeyword(TRADE_KEYWORD_RECON, result.hasErrors() ? RECON_KO : RECON_OK);

            if (TradeWorkflow.isTradeActionApplicable(trade, context.getWfAction(), context.getDSConnection(), null)) {
                try {
                    context.getDSConnection().getRemoteTrade().save(trade);
                } catch (CalypsoServiceException e) {
                    Log.error(LOG_CATEGORY, String.format("Error saving trade %s.", trade), e);
                    result.addError(String.format("Error saving trade: %s.", e.getMessage()));
                }
            } else {
                result.addError(String.format("Error saving trade, action %s in not applicable.", context.getWfAction()));
            }
        }

        private ReconCCPMatchingResult matchFields(Trade ourTrade, EurexReconRecord theirTrade, PricingEnv pe) {
            ReconCCPMatchingResult result = new ReconCCPMatchingResult(true, ourTrade, new ArrayList<>(), new ArrayList<>());
            result.setReference(Util.isEmpty(theirTrade.getRpoUTI()) ? "" : theirTrade.getRpoUTI());

            JDate valDate = context.getValuationDatetime().getJDate(ourTrade.getBook().getLocation());
            int retaDec = CurrencyUtil.getRateDecimals(ourTrade.getSettleCurrency());
            RoundingMethod rm = RoundingMethod.get(CurrencyUtil.getRoundingMethod(ourTrade.getSettleCurrency()));

            String ourIsin = (ourTrade.getProduct() instanceof Security)
                    ? ((Security) ourTrade.getProduct()).getSecurity().getSecCode("ISIN")
                    : "";

            if (!Util.isSame(ourIsin, theirTrade.getIsin())) {
                result.addError(String.format("Security ISIN code Us: %s, Them %s.", ourIsin, theirTrade.getIsin()));
            }

            if (ourTrade.getProduct() instanceof Repo) {
                Repo repo = (Repo) ourTrade.getProduct();
                if (theirTrade.getTradeLegs().size() != 2) {
                    result.addError(String.format("Product Type,  Number of Legs Us: 2 (Repo), Them %d.", theirTrade.getTradeLegs().size()));
                }

                for (int i = 0; i < theirTrade.getTradeLegs().size(); i++) {
                    if (!theirTrade.getTradeLegs().get(i).getLegNo().equals(BigInteger.valueOf(i + 1))) {
                        result.addError(String.format("File format, Unexpected Leg No %d, settle date %s.", theirTrade.getTradeLegs().get(i).getLegNo(), theirTrade.getTradeLegs().get(i).getSettlDatCtrct()));
                    }
                }

                BuySellIndType theirBuySell = theirTrade.getTradeLegs().get(0).getBuySellInd();

                /*
                 *  qty =1 ->Repo, qty = -1 ->Reverse
                 *  Repo: Sell then, Buy
                 *  Reverse: Buy then, Sell
                 */

                BuySellIndType ourBuySell = ourTrade.getQuantity() < 0 ? BuySellIndType.B : BuySellIndType.S;
                if (!Util.isSame(ourBuySell,theirBuySell)) {
                    result.addError(String.format("Trade Direction Us: %s, Them %s.", ourBuySell, theirBuySell));

                }
                //Return leg
                ourBuySell = ourTrade.getQuantity() < 0 ? BuySellIndType.S : BuySellIndType.B;
                theirBuySell = theirTrade.getTradeLegs().get(1).getBuySellInd();
                if (!Util.isSame(ourBuySell, theirBuySell)) {
                    result.addError(String.format("Trade Direction Us: %s, Them %s.", ourBuySell, theirBuySell));

                }

                //repo type
                String repoType = null;
                boolean openTerm = false;
                boolean fixedRate = true;
                switch (theirTrade.getRpoTrdTyp()) {
                    case GC:
                        repoType = "Triparty";
                        break;
                    case GCOP:
                        repoType = "Triparty";
                        openTerm = true;
                        break;
                    case GCOV:
                        repoType = "Triparty";
                        openTerm = true;
                        fixedRate = false;
                        break;
                    case GCVA:
                        repoType = "Triparty";
                        fixedRate = false;
                        break;
                    case SP:
                        repoType = "Standard";
                        break;
                    case SPOP:
                        repoType = "Standard";
                        openTerm = true;
                        break;
                    case SPOV:
                        repoType = "Standard";
                        openTerm = true;
                        fixedRate = false;
                        break;
                    case SPVA:
                        repoType = "Standard";
                        fixedRate = false;
                        break;


                }

                if (!Util.isSame(repo.getSubType(),repoType)) {
                    result.addError(String.format("Repo Type, Us %s, Them %s.", repo.getSubType(), theirTrade.getRpoTrdTyp()));
                }

                if (repo.isOpen() != openTerm) {
                    result.addError(String.format("Repo Type, Us %s, Them %s.", repo.isOpen() ? "Open Term" : "Fixed Term", openTerm ? "Open Term" : "Fixed Term"));
                }

                if (repo.isFixedRate() != fixedRate) {
                    result.addError(String.format("Repo Type, Us %s, Them %s.", repo.isFixedRate() ? "Fixed Rate" : "Floating Rate", fixedRate ? "Fixed Rate" : "Floating Rate"));
                }

                //trade date
                JDate tradeDate = ourTrade.getTradeDate().getJDate(ourTrade.getBook().getLocation());
                if (!Util.isSame(tradeDate,theirTrade.getTradeDate())) {
                    result.addError(String.format("Trade Date, Us %s, Them %s.", tradeDate, theirTrade.getTradeDate()));
                }

                //currency
                if (!Util.isSame(ourTrade.getSettleCurrency(), theirTrade.getSettlCurrency().value())) {
                    result.addError(String.format("Settlement Currency, Us %s, Them %s.", ourTrade.getSettleCurrency(), theirTrade.getSettlCurrency()));
                }

                //Intended settle date

                if (!Util.isSame(ourTrade.getSettleDate(), toJDate(theirTrade.getTradeLegs().get(0).getSettlDatCtrct()))) {
                    result.addError(String.format("Settle Date,  Us %s, Them %s.", ourTrade.getSettleDate(), theirTrade.getTradeLegs().get(0).getSettlDatCtrct()));
                }


                if (!openTerm && !Util.isSame(repo.getEndDate(), toJDate(theirTrade.getTradeLegs().get(1).getSettlDatCtrct()))) {
                    result.addError(String.format("Settle Date,  Us %s, Them %s.", repo.getEndDate(), theirTrade.getTradeLegs().get(1).getSettlDatCtrct()));
                }
                /*
                 * Eurex definition :
                 * rpoTotQty: This field contains the nominal quantity of the securities in the Repo Trade. So is it Nominal Amount or Quantity ????
                 */

                //  double qty = repo.computeQuantity(repo.computeNominal(ourTrade), ourTrade.getSettleDate());
                double nom = repo.computeNominal(ourTrade, ourTrade.getSettleDate());
                int nomResult = withTolerance(Math.abs(nom), theirTrade.getTradeLegs().get(0).getRpoTotQty().doubleValue(), ourTrade, pe);
                if (nomResult < 0) {
                    result.addError(String.format("Start Date Nominal, Us %f, Them %f.", nom, theirTrade.getTradeLegs().get(0).getRpoTotQty()));
                } else if (nomResult > 0) {
                    result.addWarning(String.format("Start Date Nominal, Us %f, Them %f.", nom, theirTrade.getTradeLegs().get(0).getRpoTotQty()));
                }

                //  qty = repo.computeQuantity(repo.computeNominal(ourTrade), repo.getEndDate());

                nom = getEndDateNom(ourTrade); //repo.computeNominal(ourTrade, repo.getEndDate().addDays(-1));
                nomResult = withTolerance(Math.abs(nom), theirTrade.getTradeLegs().get(0).getRpoTotQty().doubleValue(), ourTrade, pe);
                if (nomResult < 0) {
                    result.addError(String.format("End Date Nominal, Us %f, Them %f.", nom, theirTrade.getTradeLegs().get(1).getRpoTotQty()));
                } else if (nomResult > 0) {
                    result.addWarning(String.format("End Date Nominal, Us %f, Them %f.", nom, theirTrade.getTradeLegs().get(1).getRpoTotQty()));
                }

                double principal = repo.getCash().getPrincipal(ourTrade.getSettleDate());

                int principalResult = withTolerance(theirTrade.getTradeLegs().get(0).getRpoTotAmnt().doubleValue(), Math.abs(principal), ourTrade, pe);
                if (principalResult < 0) {
                    result.addError(String.format("Opening Principal Us %s, Them %s.", toDisplayValue(Math.abs(principal), ourTrade.getTradeCurrency()), toDisplayValue(theirTrade.getTradeLegs().get(0).getRpoTotAmnt(),  ourTrade.getTradeCurrency())));
                } else if (principalResult > 0) {
                    result.addWarning(String.format("Opening Principal Us %s, Them %s.",toDisplayValue(Math.abs(principal), ourTrade.getTradeCurrency()), toDisplayValue(theirTrade.getTradeLegs().get(0).getRpoTotAmnt(),  ourTrade.getTradeCurrency())));
                }

                if (!fixedRate) {//floating rate
                    RpoRefRtCodType index = theirTrade.getTradeLegs().get(0).getRpoRefRtCod();
                    if (repo.getCash().getFixedRateB()) {
                        result.addError("Rate Type,  Us Fixed, Them Floating.");
                    }

                    if (!repo.getCash().getRateIndex().getName().equals(index.value())) {
                        try {
                            String mappedRateIndex = CalypsoMappingUtil.getRemoteDataUpload().getMappingValue("EurexRepo", "RateIndex", index.value());
                            if (Util.isEmpty(mappedRateIndex) || !mappedRateIndex.equals(index.value())) {
                                result.addError(String.format("Rate Index, Us %s, Them %s.", repo.getCash().getRateIndex().getName(), index));
                            }

                        } catch (CalypsoServiceException e) {
                            result.addError(String.format("Rate Index, failed to retrieve index mapping %s.", e.getMessage()));
                        }
                    }

                    BigDecimal rpoSpreadOpening = theirTrade.getTradeLegs().get(0).getRpoIntRt();
                    BigDecimal rpoSpreadClosing = theirTrade.getTradeLegs().get(1).getRpoIntRt();

                    if (rpoSpreadOpening.equals(rpoSpreadClosing)) {
                        result.addError(String.format("Their Spread, Different Rates on Repo legs: Opening %f, Closing %f.", rpoSpreadOpening, rpoSpreadClosing));
                    } else {
                        double spread = repo.getCash().getSpread();
                        if (rm.round(spread, retaDec) != rm.round(rpoSpreadOpening.doubleValue(), retaDec)) {
                            result.addError(String.format("Interest Rate, Us %f, Them %f.", spread, rpoSpreadOpening));
                        }
                    }

                } else { //fixed rate

                    CashFlowSet flowSet;
                    try {
                        flowSet = repo.generateFlows(valDate);
                        flowSet.calculate(pe.getQuoteSet(), valDate);

                        repo.calculate(flowSet, pe, valDate);
                        Repo.computePrincipalAndInterest(flowSet, repo);
                    } catch (Exception e) {
                        result.addError(String.format("Error generating cash flows,  %s.", e.getMessage()));
                        return result;

                    }
                    //interest amount

                    BigDecimal openLegInt = theirTrade.getTradeLegs().get(0).getRpoIntAmt();
                    BigDecimal closeLegInt = theirTrade.getTradeLegs().get(1).getRpoIntAmt();

                    if (!openLegInt.equals(closeLegInt)) {
                        result.addError(String.format("Their Interest Amount,  Different Int amounts on Repo legs: Opening %f, Closing %f.", openLegInt, closeLegInt));
                    } else {

                        if (!openTerm) {
                            double ourAmount = Arrays.stream(flowSet.getFlows()).filter(f -> CashFlow.INTEREST.equals(f.getType())).mapToDouble(CashFlow::getAmount).sum();

                            int amtResult = withTolerance(closeLegInt.doubleValue(), repo.getFixedRate() < 0 ? -Math.abs(ourAmount) : Math.abs(ourAmount), ourTrade, pe);
                            if (amtResult < 0) {
                                result.addError(String.format("Interest Amount Us %s, Them %s.", toDisplayValue(Math.abs(ourAmount), ourTrade.getTradeCurrency()), toDisplayValue(closeLegInt, ourTrade.getTradeCurrency())));
                            } else if (amtResult > 0) {
                                result.addWarning(String.format("Interest Amount diff within tolerance Us %s, Them %s.",  toDisplayValue(Math.abs(ourAmount), ourTrade.getTradeCurrency()), toDisplayValue(closeLegInt, ourTrade.getTradeCurrency())));
                            }
                        }
                    }

                    BigDecimal rpoIntRtOpening = theirTrade.getTradeLegs().get(0).getRpoIntRt();
                    BigDecimal rpoIntRtClosing = theirTrade.getTradeLegs().get(1).getRpoIntRt();

                    if (!rpoIntRtOpening.equals(rpoIntRtClosing)) {
                        result.addError(String.format("Their Interest Rate,  Different Rates on Repo legs: Opening %f, Closing %f.", rpoIntRtOpening, rpoIntRtClosing));
                    } else {
                        double intRate = (repo.getCash().getFixedRate());
                        double ourPercentRateRounded = rm.round(intRate * 100, retaDec - 2);
                        double theirPercentRateRounded = rm.round(rpoIntRtOpening.doubleValue(), retaDec - 2);
                        if (Math.abs(ourPercentRateRounded - theirPercentRateRounded) > Math.pow(10, -(retaDec - 2))) {
                            result.addError(String.format("Interest Rate, Us %f, Them %f.", ourPercentRateRounded, rpoIntRtOpening));
                        }
                    }
                    if (!openTerm) {
                        double netTotal = Repo.getNetTotalFlow(flowSet).getAmount();

                        int netTotalResult = withTolerance(theirTrade.getTradeLegs().get(1).getRpoTotAmnt().doubleValue(), Math.abs(netTotal), ourTrade, pe);
                        if (netTotalResult < 0) {
                            result.addError(String.format("Closing Principal (Net Total) Us %s, Them %s.",toDisplayValue(Math.abs(netTotal), ourTrade.getSettleCurrency()), toDisplayValue(theirTrade.getTradeLegs().get(1).getRpoTotAmnt(), ourTrade.getTradeCurrency())));
                        } else if (netTotalResult > 0) {
                            result.addWarning(String.format("Closing Principal (Net Total) Us %s, Them %s.", toDisplayValue(Math.abs(netTotal), ourTrade.getSettleCurrency()), toDisplayValue(theirTrade.getTradeLegs().get(1).getRpoTotAmnt(), ourTrade.getTradeCurrency())));
                        }
                    }
                }

          /*  } else if (ourTrade.getProduct() instanceof Bond) {
                if (theirTrade.getTradeLegs().size() != 1) {
                    result.addError(String.format("Product Type,  Number of Legs Us: 1 (Bond), Them %d.", theirTrade.getTradeLegs().size()));
                }

                BuySellIndType theirBuySell = theirTrade.getTradeLegs().get(0).getBuySellInd();
                BuySellIndType ourBuySell = ourTrade.getQuantity() < 0 ? BuySellIndType.S : BuySellIndType.B;
                if (!ourBuySell.equals(theirBuySell)) {
                    result.addError(String.format("Trade Direction Us: %s, Them %s.", ourBuySell, theirBuySell));

                } */

            } else {
                result.addError("Our Product Type, Repo expected.");
            }

            return result;
        }
        private DisplayValue toDisplayValue (BigDecimal rawVal, String ccy) {
           return toDisplayValue(rawVal==null?0D:rawVal.doubleValue(), ccy);
        }
        private DisplayValue toDisplayValue (double rawVal, String ccy) {
            CurrencyDefault ccyDef = CurrencyUtil.getCurrencyDefault(ccy);
            if (ccyDef != null) {
                RoundingMethod rm = ccyDef.getRoundingMethod()==null?RoundingMethod.R_NEAREST: RoundingMethod.valueOf(ccyDef.getRoundingMethod());
                int dec =ccyDef.getRounding()>=0?(int)ccyDef.getRounding():2;
                return new Amount(rm.round(rawVal, dec), dec);
            }
            return Util.isEmpty(ccy)?new Amount(RoundingMethod.R_NEAREST.round(rawVal, 2), 2): new Amount(CurrencyUtil.roundAmount(rawVal, ccy), ccy);
        }

        private double getEndDateNom(Trade secFinTrade) {
            final SecFinance secFin = (SecFinance) secFinTrade.getProduct();
            final JDate dte = secFin.getEndDate() == null
                    ? context.getValuationDatetime().getJDate(secFinTrade.getBook().getLocation()).addBusinessDays(secFin.getNoticeDays(), secFin.getHolidays())
                    : secFin.getEndDate();

            return secFin.getCollaterals().stream().map(Collateral.class::cast)
                    .filter(c -> dte.gte(getCollateralActiveDate(c)) && (c.getEndDate() == null || !dte.after(c.getEndDate()))).mapToDouble(c -> c.getNominal(dte))
                    .sum();
        }

        private JDate getCollateralActiveDate(Collateral col) {
            JDate colActiveDate = col.getFromSubstitutionB() ? col.getStartDate() : col.getTradeDate();
            if (col.getStartDate().before(col.getTradeDate())) {
                colActiveDate = col.getStartDate();
            }
            return colActiveDate;
        }

        private JDate toJDate(XMLGregorianCalendar cal) {
            return JDate.valueOf(cal.getYear(), cal.getMonth(), cal.getDay());
        }

        private int withTolerance(double amt1, double amt2, Trade trade, PricingEnv pe) {
            String ccy = trade.getSettleCurrency();
            if (Math.abs(Math.abs(amt1) - Math.abs(amt2)) < Math.pow(10, -CurrencyUtil.getCcyDecimals(ccy, 2)))
                return 0;

            double tolerance = tolerances.computeIfAbsent(Pair.of(trade.getCounterParty().getId(), trade.getSettleCurrency()), k -> {
                LegalEntityTolerance leTol = ReconCCPUtil.getTolerance(trade, context.getToleranceType());
                JDate valDate = context.getValuationDatetime().getJDate(trade.getBook().getLocation());
                try {
                    if (leTol == null)
                        return CurrencyUtil.convertAmount(pe, TOLERANCE, TOLERANCE_CCY,
                                trade.getSettleCurrency(), valDate, pe.getQuoteSet());
                    else {
                        if (leTol.getPct() != 0) {
                            if (leTol.getAmount() == 0) {
                                Log.error(this, String.format("Percentage tolerance not supported, using default tolerance %s%f.", TOLERANCE_CCY, TOLERANCE));
                                return CurrencyUtil.convertAmount(pe, TOLERANCE, TOLERANCE_CCY,
                                        trade.getSettleCurrency(), valDate, pe.getQuoteSet());
                            } else {
                                Log.warn(this, String.format("Percentage tolerance not supported, using tolerance amount %f from LE tolerance %s. ", leTol.getAmount(), leTol));
                            }
                        }
                        return "ANY".equals(leTol.getCurrency()) ? leTol.getAmount() : CurrencyUtil.convertAmount(pe, leTol.getAmount(), leTol.getCurrency(),
                                trade.getSettleCurrency(), valDate, pe.getQuoteSet());

                    }
                } catch (MarketDataException e) {
                    Log.error(LOG_CATEGORY, String.format("Cannot compute tolerance, using default %f", TOLERANCE), e);
                }
                return TOLERANCE;
            });
            return Math.abs(amt1 - amt2) < tolerance ? 1 : -1;
        }

    }


    static class ExecutionContext implements IExecutionContext {

        private final DSConnection dsCon;
        private final PSConnection psCon;
        private final int threadPoolSize;

        private final JDatetime valDateTime;

        private final String tradeFilter;

        private File lastFile;

        private final String peName;
        private final String toleranceType;

        private final Action wfAction;

        ExecutionContext(DSConnection dsCon, PSConnection psCon, int threadPoolSize, String peName, JDatetime valDateTime, String tradeFilter,
                         String toleranceType, Action wfAction) {
            this.dsCon = dsCon;
            this.psCon = psCon;
            this.threadPoolSize = threadPoolSize;
            this.peName = peName;
            this.valDateTime = valDateTime;
            this.tradeFilter = tradeFilter;
            this.toleranceType = toleranceType;
            this.wfAction = wfAction;
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

        public String getTradeFilter() {
            return tradeFilter;
        }

        public JDatetime getValuationDatetime() {
            return valDateTime;
        }

        public void setLastFile(File lastFile) {
            this.lastFile = lastFile;
        }

        public File getLastFile() {
            return lastFile;
        }

        public String getPEName() {
            return peName;
        }

        public String getToleranceType() {
            return toleranceType;
        }

        public Action getWfAction() {
            return wfAction;
        }
    }

    static class DSCommandSelectUTI implements DSCommand {

        private final String selColumn;
        private final String from;
        private final String where;

        private final List<CalypsoBindVariable> bindVars;

        private DSCommandSelectUTI(String selColumn, String from, String where, List<CalypsoBindVariable> bindVars) {
            this.selColumn = selColumn;
            this.from = from;
            this.where = where;
            this.bindVars = bindVars;
        }

        @Override
        public Object execute() throws Exception {
            List<String> refs = new ArrayList<>();
            Connection con = null;
            try {
                con = ioSQL.getConnection();
                try (PreparedStatement select = con.prepareStatement("SELECT " + selColumn + " FROM " + from + " WHERE " + where)) {
                    int index = 1;
                    for (CalypsoBindVariable param : bindVars)
                        ioSQL.setPreparedFieldAsObject(select, index++, param);

                    try (ResultSet rs = select.executeQuery()) {

                        while (rs.next()) {
                            refs.add(rs.getString(1));
                        }
                    }
                }
            } finally {
                ioSQL.releaseConnection(con);
            }


            return refs;
        }
    }

    static class ReconciliationReport {
        private final List<ReconCCPMatchingResult> results = new ArrayList<>();

        List<ReconCCPMatchingResult> getMatchingResults() {
            return results;
        }

        void add(ReconCCPMatchingResult result) {
            results.add(result);
        }

        public void missing(Trade trade) {
            ReconCCPMatchingResult matchingResult = new ReconCCPMatchingResult(false, trade, new ArrayList<>(), new ArrayList<>());
            matchingResult.setReference(trade.getKeywordValue(TRADE_KW_UTR));
            matchingResult.addCalypsoTradeNotMatchError(trade);
            results.add(matchingResult);
        }

        public void alleged(EurexReconRecord eurexReconRecord) {
            ReconCCPMatchingResult matchingResult = ReconCCPMatchingResult.buildEmptyUnmatchedResult();
            matchingResult.addError("Eurex Trade NOT FOUND in Calypso, UTR: " + eurexReconRecord.getRpoUTI());
            matchingResult.setReference(eurexReconRecord.getRpoUTI());
            results.add(matchingResult);
        }
    }

    static class EurexReconRecord {
        private String membClgIdCod;
        private JDate tradeDate;
        private SettlLocType settlLoco;
        private String settlAcct;
        private SettlCurrencyType settlCurrency;
        private String isin;
        private InstTypCodType instTypCod;
        private String membTrdngIdCod;
        private AcctTypType acctTyp;
        private RpoTrdTypType rpoTrdTyp;
        private String ordrNum;
        private String rpoBankIntRef;
        private String rpoUTI;
        private JDatetime rpoTrdTmStmp;
        private JDatetime rpoClgTmStmp;
        private TrdLocType trdLoc;
        private String trdNum;
        private RpoCmpTrdType rpoCmpTrd;
        private List<Tc800RecType> tradeLegs;

        private EurexReconRecord() {
        }

        public String getMembClgIdCod() {
            return membClgIdCod;
        }

        public JDate getTradeDate() {
            return tradeDate;
        }

        public SettlLocType getSettlLoco() {
            return settlLoco;
        }

        public String getSettlAcct() {
            return settlAcct;
        }

        public SettlCurrencyType getSettlCurrency() {
            return settlCurrency;
        }

        public String getIsin() {
            return isin;
        }

        public InstTypCodType getInstTypCod() {
            return instTypCod;
        }

        public String getMembTrdngIdCod() {
            return membTrdngIdCod;
        }

        public AcctTypType getAcctTyp() {
            return acctTyp;
        }

        public RpoTrdTypType getRpoTrdTyp() {
            return rpoTrdTyp;
        }

        public String getOrdrNum() {
            return ordrNum;
        }

        public String getRpoBankIntRef() {
            return rpoBankIntRef;
        }

        public String getRpoUTI() {
            return rpoUTI;
        }

        public JDatetime getRpoTrdTmStmp() {
            return rpoTrdTmStmp;
        }

        public JDatetime getRpoClgTmStmp() {
            return rpoClgTmStmp;
        }

        public RpoCmpTrdType getRpoCmpTrd() {
            return rpoCmpTrd;
        }

        public TrdLocType getTrdLoc() {
            return trdLoc;
        }

        public String getTrdNum() {
            return trdNum;
        }

        public List<Tc800RecType> getTradeLegs() {
            return tradeLegs;
        }

        static EurexReconRecordBuilder newRecord() {
            return new EurexReconRecordBuilder();
        }

        private static class EurexReconRecordBuilder {
            private EurexReconRecord record;

            private EurexReconRecordBuilder() {
                record = new EurexReconRecord();
            }

            EurexReconRecordBuilder withMembClgIdCod(String membClgIdCod) {
                record.membClgIdCod = membClgIdCod;
                return this;
            }

            EurexReconRecordBuilder withTradeDate(JDate tradeDate) {
                record.tradeDate = tradeDate;
                return this;
            }

            EurexReconRecordBuilder withSettlLoco(SettlLocType settlLoco) {
                record.settlLoco = settlLoco;
                return this;
            }

            EurexReconRecordBuilder withSettlAcct(String settlAcct) {
                record.settlAcct = settlAcct;
                return this;
            }

            EurexReconRecordBuilder withSettlCurrency(SettlCurrencyType settlCurrency) {
                record.settlCurrency = settlCurrency;
                return this;
            }

            EurexReconRecordBuilder withIsin(String isin) {
                record.isin = isin;
                return this;
            }

            EurexReconRecordBuilder withInstTypCod(InstTypCodType instTypCod) {
                record.instTypCod = instTypCod;
                return this;
            }

            EurexReconRecordBuilder withMembTrdngIdCod(String membTrdngIdCod) {
                record.membTrdngIdCod = membTrdngIdCod;
                return this;
            }

            EurexReconRecordBuilder withAcctTyp(AcctTypType acctTyp) {
                record.acctTyp = acctTyp;
                return this;
            }

            EurexReconRecordBuilder withOrdrNum(String ordrNum) {
                record.ordrNum = ordrNum;
                return this;
            }

            EurexReconRecordBuilder withRpoUTI(String rpoUTI) {
                record.rpoUTI = rpoUTI;
                return this;
            }

            EurexReconRecordBuilder withRpoTrdTmStmp(JDatetime rpoTrdTmStmp) {
                record.rpoTrdTmStmp = rpoTrdTmStmp;
                return this;
            }

            EurexReconRecordBuilder withRpoClgTmStmp(JDatetime rpoClgTmStmp) {
                record.rpoClgTmStmp = rpoClgTmStmp;
                return this;
            }

            EurexReconRecordBuilder withTrdLoc(TrdLocType trdLoc) {
                record.trdLoc = trdLoc;
                return this;
            }

            public EurexReconRecordBuilder withTrdNum(String trdNum) {
                record.trdNum = trdNum;
                return this;
            }

            public EurexReconRecordBuilder withRpoBankIntRef(String rpoBankIntRef) {
                record.rpoBankIntRef = rpoBankIntRef;
                return this;
            }

            EurexReconRecordBuilder withRpoTrdTyp(RpoTrdTypType rpoTrdTyp) {
                record.rpoTrdTyp = rpoTrdTyp;
                return this;
            }

            public EurexReconRecordBuilder withCmpTrd(RpoCmpTrdType rpoCmpTrd) {
                record.rpoCmpTrd = rpoCmpTrd;
                return this;
            }

            EurexReconRecordBuilder withTradeLegs(List<Tc800RecType> tradeLegs) {
                record.tradeLegs = tradeLegs;
                return this;
            }

            EurexReconRecord build() {
                record.getTradeLegs().sort((l1, l2) -> l1.getSettlDatCtrct().compare(l2.getSettlDatCtrct()));
                EurexReconRecord curr = record;
                record = new EurexReconRecord();
                return curr;
            }
        }

    }
}
