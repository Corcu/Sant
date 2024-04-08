package calypsox.tk.util;

import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.ReconCCP;
import calypsox.repoccp.model.ReconCCPMatchingResult;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.LegalEntityTolerance;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;
import org.apache.commons.lang.text.StrSubstitutor;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static calypsox.repoccp.ReconCCPConstants.*;
import static calypsox.repoccp.ReconCCPUtil.*;

/**
 * ScheduledTaskRECONCCP
 *
 * @author aalonsop, x854118
 */
public class ScheduledTaskRECONCCP extends ScheduledTask {

    private static final long serialVersionUID = 9181867830721649677L;

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
     * ST Parameter, filter by file trade date if equals valuationDate
     */
    public static final String FILTER_BY_FILE_TRADE_DATE = "Filter by file Trade Date";
    /**
     * ST Parameter, filter by file trade date if equals valuationDate
     */
    public static final String FILE_TRADE_DATE_PATTERN = "File Trade Date Pattern";
    /**
     * ST Parameter, filter by file trade date if equals valuationDate
     */
    public static final String FILTER_BY_FILE_TRADE_RECON_OK = "Filter file trades by Recon OK";
    /**
     * The default file trade date pattern
     */
    private static final String DEFAULT_FILE_TRADE_DATE_PATTERN = "yyyy-MM-dd";


    private static final String CLOSE_TASKS_FOR_RECONCILED_TRADES = "Close Tasks of Reconciled Trades";


    private static final String SELECT_TASK_BY_TRADE_ID = "event_type =? AND trade_id  = ? AND task_status NOT IN (1, 2)";
    private static final String SELECT_TASK_BY_REF = "event_type =? AND  trade_id  = 0 AND task_status  NOT IN (1,2) AND int_reference=?";

    private static final String SELECT_GET_TASKS = "(((event_type =? OR event_type =?) AND trade_id  = ?) OR (event_type =? AND  trade_id  = 0 AND int_reference=?))" +
            " AND task_status NOT IN (1, 2)";

    /**
     * Reactive processing is not yet even fully designed, so for now, imperative calls are enough
     */
    protected boolean process(DSConnection ds, PSConnection ps) {
        String fileName = getAttribute(FILE_NAME);
        String filePath = getAttribute(FILE_PATH);
        String orderBy = getAttribute(ORDER_FILES_BY);

        try {
            StrSubstitutor sub = new StrSubstitutor(getValueMap());
            // Replace
            String filePathResolved = sub.replace(filePath);
            String fileNameResolved = sub.replace(fileName);

            //Read file and store results in list of ReconCCP interface objects
            List<ReconCCP> clearingTrades = readAndParseFile(fileNameResolved, filePathResolved, orderBy);

            if (getBooleanAttribute(FILTER_BY_FILE_TRADE_DATE)) {
                String pattern = getAttribute(FILE_TRADE_DATE_PATTERN);
                if (Util.isEmpty(pattern)) {
                    pattern = DEFAULT_FILE_TRADE_DATE_PATTERN;
                }
                clearingTrades = filterByFileTradeDateAndSTValDate(clearingTrades, this.getValuationDatetime(), pattern);
            }

            if (getBooleanAttribute(FILTER_BY_FILE_TRADE_RECON_OK)) {
                clearingTrades = filterFileTradesReconOK(ds, clearingTrades, getTradeFilter(), getFilterSet(), getValuationDatetime().getJDate(getTimeZone()), getValuationDatetime());
            }

            //Load trades from calypso with trade filter
            List<Trade> calypsoTradesFromFilter = loadAndFilterTrades(ds, this.getValuationDatetime(), this._timeZone, this._tradeFilter, this._filterSet);

            PricingEnv pe = getDSConnection().getRemoteMarketData().getPricingEnv(getPricingEnv(), getValuationDatetime());

            //Try to match trades and then their transfers with the file transfers
            if (clearingTrades != null) {
                List<ReconCCPMatchingResult> matchingResults = matchTrades(clearingTrades, calypsoTradesFromFilter, pe);

                //Post process, create tasks and assign keywords to transfers and save them
                processTrades(matchingResults);
            }

            return true;
        } catch (Exception e) {
            Log.error(this, e.getCause());
        }

        return false;
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
    public String getTaskInformation() {
        return "Runs the CCP RECON process taking the file from attributes.";
    }

    /**
     * Try to match
     *
     * @param clearingTrades list of external trades to recon
     * @param calypsoTrades  list of calypso trades to recon
     * @param pe
     */
    private List<ReconCCPMatchingResult> matchTrades(List<ReconCCP> clearingTrades, List<Trade> calypsoTrades, PricingEnv pe) {
        List<ReconCCPMatchingResult> matchingResults = new ArrayList<>();
        Map<ToleranceKey, Double> tolerances = new HashMap<>();
        JDate valDate = getValuationDatetime().getJDate(getTimeZone());
        //This may be different between clearings.
        //Creating a simple class hierarchy to easily add functionality in the future is not a bad idea
        for (ReconCCP clearingTrade : clearingTrades) {
            boolean matchedClearingTrade = false;
            for (Trade calypsoTrade : calypsoTrades) {
                double tolerance = tolerances.computeIfAbsent(new ToleranceKey(calypsoTrade.getCounterParty().getId(), calypsoTrade.getSettleCurrency()), k -> {
                    LegalEntityTolerance leTol = ReconCCPUtil.getTolerance(calypsoTrade, "Clearing");

                    try {
                        if (leTol == null)
                            return CurrencyUtil.convertAmount(pe, TOLERANCE, TOLERANCE_CCY,
                                    calypsoTrade.getSettleCurrency(), valDate, pe.getQuoteSet());
                        else {
                            if (leTol.getPct() != 0) {
                                if (leTol.getAmount() == 0) {
                                    Log.error(this, String.format("Percentage tolerance not supported, using default tolerance %s%f.", TOLERANCE_CCY, TOLERANCE));
                                    return CurrencyUtil.convertAmount(pe, TOLERANCE, TOLERANCE_CCY,
                                            calypsoTrade.getSettleCurrency(), valDate, pe.getQuoteSet());
                                } else {
                                    Log.warn(this, String.format("Percentage tolerance not supported, using tolerance amount %f from LE tolerance %s. ", leTol.getAmount(), leTol));
                                }
                            }
                            return "ANY".equals(leTol.getCurrency()) ? leTol.getAmount() : CurrencyUtil.convertAmount(pe, leTol.getAmount(), leTol.getCurrency(),
                                    calypsoTrade.getSettleCurrency(), valDate, pe.getQuoteSet());

                        }
                    } catch (MarketDataException e) {
                        Log.error(this, e);
                    }
                    return TOLERANCE;
                });
                //Execute simple match
                if (clearingTrade.matchReference(calypsoTrade)) {
                    ReconCCPMatchingResult matchingResult = clearingTrade.match(calypsoTrade, tolerance);
                    calypsoTrades.remove(calypsoTrade);
                    //Collect results to do the final processing
                    matchingResults.add(matchingResult);
                    matchedClearingTrade = true;
                    break;
                }
            }

            if (!matchedClearingTrade) {
                ReconCCPMatchingResult matchingResult = ReconCCPMatchingResult.buildEmptyUnmatchedResult();
                matchingResult.addTradeNotFound(clearingTrade);
                matchingResults.add(matchingResult);
            }
        }

        for (Trade trade : calypsoTrades) {
            ReconCCPMatchingResult matchingResult = new ReconCCPMatchingResult(false, trade, new ArrayList<>(), new ArrayList<>());
            matchingResult.addCalypsoTradeNotMatchError(trade);
            matchingResults.add(matchingResult);
        }

        return matchingResults;
    }

    /**
     * Create tasks for each:
     * - Unmatched trade from file
     * - Unmatched trade from calypso
     * - Matched trade but any of the recon fields not matched
     * <p>
     * Update keywords:
     * - BuyerSellerReference keyword with its value from file for all matched trades
     * - Recon keyword with value OK for matched calypso trades and KO for unmatched calypso trades
     */
    private void processTrades(List<ReconCCPMatchingResult> matchingResults) {
        TaskArray taskArray = new TaskArray();
        List<Trade> tradeArray = new ArrayList<>();
        Boolean closeTasks = getBooleanAttribute(CLOSE_TASKS_FOR_RECONCILED_TRADES);
        //Trade KWD updating, error task creation etc...
        for (ReconCCPMatchingResult result : matchingResults) {
            Trade trade = result.getTrade();
            if (result.isMatched()) {
                if (result.hasErrors()) {
                    Task matchingErrorsTask = new Task(trade);
                    matchingErrorsTask.setComment(result.getMatchingErrors());
                    matchingErrorsTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP);
                    matchingErrorsTask.setStatus(Task.NEW);
                    matchingErrorsTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingErrorsTask.setDatetime(new JDatetime());
                    matchingErrorsTask.setNewDatetime(new JDatetime());
                    matchingErrorsTask.setTradeLongId(trade.getLongId());
                    matchingErrorsTask.setPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setNextPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setNextPriorityDatetime(null);
                    matchingErrorsTask.setAttribute("ERROR");
                    if (!closeTasks || exceptionTaskNotFound(matchingErrorsTask))
                        taskArray.add(matchingErrorsTask);
                }

                if (result.hasWarnings()) {
                    Task matchingWarnTask = new Task(trade);
                    matchingWarnTask.setComment(result.getMatchingWarnings());
                    matchingWarnTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP);
                    matchingWarnTask.setStatus(Task.NEW);
                    matchingWarnTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingWarnTask.setDatetime(new JDatetime());
                    matchingWarnTask.setNewDatetime(new JDatetime());
                    matchingWarnTask.setTradeLongId(trade.getLongId());
                    matchingWarnTask.setPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriorityDatetime(null);
                    matchingWarnTask.setAttribute("WARN");
                    if (!closeTasks || exceptionTaskNotFound(matchingWarnTask))
                        taskArray.add(matchingWarnTask);
                }

                String reconValue = trade.getKeywordValue(TRADE_KEYWORD_RECON);
                if (!RECON_OK.equalsIgnoreCase(reconValue) && !result.hasErrors()) {
                    trade.addKeyword(TRADE_KEYWORD_RECON, RECON_OK);
                    taskArray.addAll(getTasksToClose(trade, result.hasWarnings()?t->!EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP.equals(t.getEventType()):null));
                } else if (!RECON_KO.equalsIgnoreCase(reconValue) && result.hasErrors()) {
                    trade.addKeyword(TRADE_KEYWORD_RECON, RECON_KO);
                    taskArray.addAll(getTasksToClose(trade,  t->!EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP.equals(t.getEventType())));
                }
                trade.setAction(Action.valueOf(WF_AMEND_RECON));
                tradeArray.add(trade);
            } else {
                if (result.hasWarnings()) {
                    Task matchingWarnTask = new Task(trade);
                    matchingWarnTask.setComment(result.getMatchingWarnings());
                    matchingWarnTask.setEventType(EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP);
                    matchingWarnTask.setStatus(Task.NEW);
                    matchingWarnTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingWarnTask.setDatetime(new JDatetime());
                    matchingWarnTask.setNewDatetime(new JDatetime());
                    matchingWarnTask.setTradeLongId(trade.getLongId());
                    matchingWarnTask.setPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriority(Task.PRIORITY_LOW);
                    matchingWarnTask.setNextPriorityDatetime(null);
                    matchingWarnTask.setAttribute("WARN");
                    if (!closeTasks || exceptionTaskNotFound(matchingWarnTask)) {
                        taskArray.addAll(getTasksToClose(trade, t->!EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP.equals(t.getEventType())));
                        taskArray.add(matchingWarnTask);
                    }
                }
                if (result.hasErrors()) {
                    Task matchingErrorsTask = new Task();
                    matchingErrorsTask.setComment(result.getUnmatchedErrors());
                    if (trade == null) {
                        matchingErrorsTask.setEventType(EXCEPTION_MISSING_TRADE_RECON_CCP);
                        matchingErrorsTask.setInternalReference(result.getReference());
                    } else {
                        matchingErrorsTask.setEventType(EXCEPTION_CALYPSO_TRADE_UNMATCHED_RECON_CCP);
                        matchingErrorsTask.setTradeLongId(result.getTrade().getLongId());
                    }
                    matchingErrorsTask.setStatus(Task.NEW);
                    matchingErrorsTask.setEventClass(Task.EXCEPTION_EVENT_CLASS);
                    matchingErrorsTask.setPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setNextPriority(Task.PRIORITY_HIGH);
                    matchingErrorsTask.setDatetime(new JDatetime());
                    matchingErrorsTask.setNewDatetime(new JDatetime());
                    matchingErrorsTask.setAttribute("ERROR");
                    if (!closeTasks || exceptionTaskNotFound(matchingErrorsTask))
                        taskArray.add(matchingErrorsTask);
                }
            }
        }
        try {
            this.getDSConnection().getRemoteBackOffice().saveAndPublishTasks(taskArray, 0L, null);
            this.getDSConnection().getRemoteTrade().saveTrades(new ExternalArray(tradeArray));
        } catch (CalypsoServiceException | InvalidClassException e) {
            Log.error(this, e.getCause());
        }

        moveNextStatus(tradeArray);
    }

    private TaskArray getTasksToClose(Trade trade, Function<Task, Boolean> predicate) {
        TaskArray tasks = getExceptionTasks(trade);
        if (tasks == null)
            return new TaskArray();


      return tasks.isEmpty()?tasks:new TaskArray(Arrays.stream(tasks.getTasks()).filter(t->t!=null && (predicate==null || predicate.apply(t)))
              .map(t->  {
                  try {
                      Task clone = (Task) t.clone();
                      clone.setStatus(Task.COMPLETED);
                      return clone;
                  } catch (CloneNotSupportedException e) {
                     Log.error(ScheduledTaskRECONCCP.class,String.format("Cannot clone task %s.", t), e);
                     return null;
                  }
              }).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    private boolean exceptionTaskNotFound(Task task) {
        try {
            TaskArray tasks;
            if (task.getTradeLongId() > 0) {
                String sqlWhere = SELECT_TASK_BY_TRADE_ID;
                if ("WARN".equals(task.getAttribute()))
                    sqlWhere += "AND ATTRIBUTE='WARN'";
                else    if ("ERROR".equals(task.getAttribute()))
                    sqlWhere += "AND ATTRIBUTE='ERROR'";

                tasks = getDSConnection().getRemoteBO().getTasks(sqlWhere, Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, task.getEventType()), new CalypsoBindVariable(CalypsoBindVariable.LONG, task.getTradeLongId())));
            } else  {
                tasks = getDSConnection().getRemoteBO().getTasks( SELECT_TASK_BY_REF,  Arrays.asList(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, EXCEPTION_MISSING_TRADE_RECON_CCP), new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, task.getInternalReference())));
            }
            return tasks == null || tasks.isEmpty();
        } catch (CalypsoServiceException e) {
            Log.error(this, e);
            return true;
        }
    }

    private TaskArray getExceptionTasks(Trade trade) {
        try {
            return getDSConnection().getRemoteBO().getTasks(SELECT_GET_TASKS, Arrays.asList(
                    new CalypsoBindVariable(CalypsoBindVariable.VARCHAR,EXCEPTION_FIELDS_NOT_MATCHING_RECON_CCP),
                    new CalypsoBindVariable(CalypsoBindVariable.VARCHAR,EXCEPTION_CALYPSO_TRADE_UNMATCHED_RECON_CCP),
                    new CalypsoBindVariable(CalypsoBindVariable.LONG,trade.getLongId()),
                    new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, EXCEPTION_MISSING_TRADE_RECON_CCP),
                    new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, trade.getKeywordValue(TRADE_KWD_BUYER_SELLER_REF))));

        } catch (CalypsoServiceException e) {
            Log.error(this, e);
            return null;
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
        attributeList.add(attribute(FILTER_BY_FILE_TRADE_DATE));
        attributeList.add(attribute(FILE_TRADE_DATE_PATTERN));
        attributeList.add(attribute(FILTER_BY_FILE_TRADE_RECON_OK));
        attributeList.add(attribute(CLOSE_TASKS_FOR_RECONCILED_TRADES).booleanType());
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
            }
        }
        if (Util.isEmpty(this._tradeFilter)) {
            messages.add("Please, fill the trade filter");
        }

        if (Util.isEmpty(getAttribute(FILTER_BY_FILE_TRADE_DATE))) {
            messages.add(FILTER_BY_FILE_TRADE_DATE + " should be filled");
        } else {
            boolean filterByTradeDate = getBooleanAttribute(FILTER_BY_FILE_TRADE_DATE);
            if (filterByTradeDate) {
                if (Util.isEmpty(getAttribute(FILE_TRADE_DATE_PATTERN))) {
                    messages.add(FILE_TRADE_DATE_PATTERN + " should be filled");
                }
            }
        }

        if (Util.isEmpty(getAttribute(FILTER_BY_FILE_TRADE_RECON_OK))) {
            messages.add(FILTER_BY_FILE_TRADE_RECON_OK + " should be filled");
        }
        if (Util.isEmpty(getPricingEnv())) {
            messages.add("Pricing Environment is mandatory");
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
            } else if (WF_REPO_ACTION.equals(attr) || WF_BOND_ACTION.equals(attr)) {
                return LocalCache.getDomainValues(this.getDSConnection(), "tradeAction");
            } else if (ORDER_FILES_BY.equals(attr)) {
                v.add(ORDER_BY_NAME);
                v.add(ORDER_BY_DATE);
                return v;
            } else if (FILTER_BY_FILE_TRADE_DATE.equals(attr)) {
                v.add(Boolean.TRUE.toString());
                v.add(Boolean.FALSE.toString());
                return v;
            } else if (FILTER_BY_FILE_TRADE_RECON_OK.equals(attr)) {
                v.add(Boolean.TRUE.toString());
                v.add(Boolean.FALSE.toString());
                return v;
            }
        }
        return super.getAttributeDomain(attr, currentAttr);
    }

    /**
     * Move the trades to the next status
     *
     * @param trades the list of trade
     */
    private void moveNextStatus(List<Trade> trades) {
        if (!Util.isEmpty(getAttribute(MOVE_NEXT_STATUS)) && !Util.isEmpty(trades)) {
            boolean moveNextStatus = getBooleanAttribute(MOVE_NEXT_STATUS);
            if (moveNextStatus) {
                if (!Util.isEmpty(getAttribute(WF_BOND_ACTION)) && !Util.isEmpty(WF_REPO_ACTION)) {
                    Set<Trade> tradesND = new HashSet<>(trades);
                    long[] ids = new long[tradesND.size()];
                    int i = 0;
                    for (Trade trade : tradesND) {
                        ids[i] = trade.getLongId();
                        i++;
                    }
                    TradeArray tradesNew = null;
                    try {
                        tradesNew = this.getDSConnection().getRemoteTrade().getTrades(ids);
                    } catch (CalypsoServiceException e) {
                        Log.error(this, e.getCause());
                    }
                    if (tradesNew != null && !tradesNew.isEmpty()) {
                        String bondAction = getAttribute(WF_BOND_ACTION);
                        String repoAction = getAttribute(WF_REPO_ACTION);
                        String recon;
                        Product product;
                        List<Trade> tradesToProcess = tradesNew.asList();
                        for (Trade trade : tradesToProcess) {
                            recon = trade.getKeywordValue(TRADE_KEYWORD_RECON);
                            product = trade.getProduct();
                            if (!Util.isEmpty(recon) && RECON_OK.equals(recon) && product != null) {
                                if (product instanceof Bond) {
                                    trade.setAction(Action.valueOf(bondAction));
                                    saveTrade(trade);
                                } else if (product instanceof Repo) {
                                    trade.setAction(Action.valueOf(repoAction));
                                    saveTrade(trade);
                                } else {
                                    Log.warn(this, "It is not possible to move a trade with " +
                                            "a product type " + trade.getProductType() + ". Only available for Bond " +
                                            "and Repo. Trade ID: " + trade.getLongId());
                                }
                            }
                        }
                    }
                } else {
                    Log.error(this, WF_BOND_ACTION + " or " + WF_REPO_ACTION + " is empty. Trades cannot " +
                            "be moved from one status to another without indicating the action. Please report " +
                            "the attributes of the ST");
                }
            } else {
                Log.info(this, "State trade are not moved. Attribute value " + MOVE_NEXT_STATUS + " is false");
            }
        }
    }

    /**
     * Save the trade in DB
     *
     * @param trade the trade
     */
    private void saveTrade(Trade trade) {
        if (trade != null) {
            List<Trade> trades = new ArrayList<>();
            trades.add(trade);
            try {
                this.getDSConnection().getRemoteTrade().saveTrades(new ExternalArray(trades));
            } catch (CalypsoServiceException | InvalidClassException e) {
                Log.warn(this, e.getCause());
            }
        }
    }

    private static class ToleranceKey {
        private final int cptyId;
        private final String ccy;

        private ToleranceKey(int cptyId, String ccy) {
            this.cptyId = cptyId;
            this.ccy = ccy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ToleranceKey)) return false;
            ToleranceKey that = (ToleranceKey) o;
            return cptyId == that.cptyId && ccy.equals(that.ccy);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cptyId, ccy);
        }

        @Override
        public String toString() {
            return "ToleranceKey{" +
                    "cptyId=" + cptyId +
                    ", ccy='" + ccy + '\'' +
                    '}';
        }
    }

}
