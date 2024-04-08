package calypsox.tk.util;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.*;

import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.sql.StringUtils;
/**
 * Created by x379335.
 */
public class ScheduledTaskSTC_PROCESS_EQUITY_EOM extends ScheduledTask {

    private static final long serialVersionUID = -1L;

    /** The fixed Task information returned by getTaskInformation method */
    private static final String TASK_INFORMATION = "Search Trades that does not pass EOM validation.";

    /** WHERE clause for not COMPLETE tasks */
    private static final StringBuilder WHERE_TASKS_NOT_COMPLETED = new StringBuilder(" AND ").append(" task_status NOT IN ('" + Task.COMPLETED + "') ");

    /** Code format */
    private static final String FORMAT_EXCEPTION_PRICE = "Trade %s, does not pass the validation for being a trade with price out of market.";
    private static final String FORMAT_EXCEPTION_CURRENCY = "Trade %s, does not pass the validation for being a trade with different trade and settlement currency.";
    private static final String FORMAT_EXCEPTION_DEFAULT= "Trade %s, does not pass the validation, without identifying case.";

    private static final String EXCEPTION_TYPE_PRICE = "EX_PRICE_OUT_MARKET";
    private static final String EXCEPTION_TYPE_CURRENCY = "EX_DIFFERENT_CURRENCY";

    /** Task messages */
    private static final String COMMENT_TASK_COMPLETED = "Task completed.";

    /** Text Messages */
    private static final String EMPTY_SPACE = "";

    /** Action */
    public static final Action VALIDATE = Action.valueOf("VALIDATE");

    /** Price Tolerance - Domain Value */
    private static final String DV_PRICE_TOLERANCE = "priceTolerance";

    /**
     * Instantiates a new scheduled task.
     */
    public ScheduledTaskSTC_PROCESS_EQUITY_EOM() {
        super();
    }

    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {

        Log.debug(this, "Start processing ScheduledTask PROCESS_EQUITY_EOM.");
        final boolean rst = true;

        // Get trades by time range
        final TradeArray tradesForValidation = getTradesForValidation(ds);

        if (tradesForValidation!=null && !tradesForValidation.isEmpty()) {
            Log.debug(this, "Trades obtained for validation: " + tradesForValidation.size());
            // Validation trades
            final List<ExceptionItem> list = getTradesWithException(ds, tradesForValidation);
            // Generate Tasks to Task Station with type of exception
            createTasks(ds, list);
        }
        else {
            Log.debug(this, "No trades obtained for validation");
        }

        return rst;
    }

    /**
     * Get trades with status PENDING, product Equity.
     *
     * @param ds
     * @return
     */
    protected TradeArray getTradesForValidation(final DSConnection ds) {
        TradeArray tradesForValidation = new TradeArray();

        // Trades with status PENDING, product Equity
        final TradeArray trades = getTradesPending(ds);
        if (trades != null && !trades.isEmpty()) {
            tradesForValidation.add(trades.getTrades(), trades.size());
        }

        return tradesForValidation;
    }

    /**
     * Get trades with status PENDING, product Equity
     * values.
     *
     * @param ds
     * @return
     */
    protected TradeArray getTradesPending(final DSConnection ds) {
        TradeArray tArray = null;
        final long[] ids = getTradesIdsPending(ds);

        if (ids == null || ids.length == 0) {
            Log.info(this,"Couldn't get the Trades with status PENDING, product Equity.");
            return null;
        }
        Log.info(this, "Total Trades Ids: " + ids.length);

        try {
            tArray = SantanderUtil.getInstance().getTradesWithTradeFilter(ids);
        }
        catch (final RemoteException e) {
            Log.error(this, "Could not get the Trades with status PENDING, product Equity", e);
        }

        return tArray;
    }

    /**
     * Get trades Ids with status PENDING, product Equity.
     *
     * @param ds
     * @return
     */
    protected long[] getTradesIdsPending(final DSConnection ds) {

        Log.debug(this, "Get Trades Ids with status PENDING, product Equity.");
        long[] ids = null;
        final TradeFilter filter = new TradeFilter();

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append(" trade.trade_status = 'PENDING'");
        whereClause.append(" AND ");
        whereClause.append(" trade.product_id = product_desc.product_id ");
        whereClause.append(" AND ");
        whereClause.append(" product_desc.product_type = 'Equity'");

        final String fromClause = " trade, product_desc ";

        filter.setSQLFromClause(fromClause);
        filter.setSQLWhereClause(whereClause.toString());

        try {
            ids = ds.getRemoteTrade().getTradeIds(filter, getValuationDatetime(), true);
        } catch (final RemoteException e) {
            Log.error(this, "Could not get the Trades Ids with status PENDING, product Equity.", e);
        }

        return ids;
    }

    /**
     * Search for trades with Exception or Valitation.
     *
     * @param ds
     * @param tradesForValidation
     * @return
     */
    protected List<ExceptionItem> getTradesWithException(final DSConnection ds, final TradeArray tradesForValidation) {

        Log.debug(this, "Start to check the validation for trades.");
        final List<ExceptionItem> result = new ArrayList<>();

        for (final Trade trade : tradesForValidation.getTrades()){
            boolean validationOk = true;

            //1º Exception: Price Out of Market
            String tradeClassification = trade.getKeywordValue("TradeClassification");

            if (tradeClassification == null || !"EXERCISE".equals(tradeClassification)) {
                if (isPriceOutOfMarket(ds, trade)) {

                    ExceptionItem e = new ExceptionItem();

                    e.setTradeIdException(trade.getLongId());
                    e.setDescriptionException(EXCEPTION_TYPE_PRICE);

                    result.add(e);
                    validationOk = false;
                }
            }

            //2º Exception: Different Trade and Settle Currency
            if (isDifferentCurrency(trade)){

                ExceptionItem e = new ExceptionItem();

                e.setTradeIdException(trade.getLongId());
                e.setDescriptionException(EXCEPTION_TYPE_CURRENCY);

                result.add(e);
                validationOk = false;
            }

            //No exception, pass the trade to status PENDING_SDI
            if (validationOk){
                trade.setAction(VALIDATE);
                try {
                    DSConnection.getDefault().getRemoteTrade().save(trade);
                } catch (CalypsoServiceException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Validated is Trade with Different Trade and Settle Currency
     *
     * @param ds
     * @param trade
     * @return
     */
    private boolean isPriceOutOfMarket(final DSConnection ds, final Trade trade) {
        boolean out = false;

        String[] priceTolerances = getPriceToleranceFromDV(ds);
        if (priceTolerances == null || priceTolerances.length == 0) {
            Log.error(this, "Couldn't get the Price Tolerance from Domain Values.");
            return false;
        }

        PricingEnv pricingEnv = instanciatePricingEnv();
        BigDecimal previousDatePrice;

        QuoteValue quote = new QuoteValue();
        quote.setQuoteSetName(getPricingEnv());
        quote.setName("Equity.ISIN_" + trade.getProduct().getSecCode("Common"));
        quote.setQuoteType(QuoteValue.PRICE);
        quote.setDate(getPreviousDay());

        QuoteValue quoteMarket = pricingEnv.getQuoteSet().getQuote(quote);

        if ((quoteMarket != null) && !Double.isNaN(quoteMarket.getClose())) {
            previousDatePrice = BigDecimal.valueOf(quoteMarket.getClose());
        } else{
            Log.error(this,"Cannot retrieve previous date price " + getPreviousDay() + " of Trade: " + trade.getLongId()) ;
            return false;
        }

        final String priceTolerance = StringUtils.join(priceTolerances, "', '");
        final BigDecimal priceToleranceBD = BigDecimal.valueOf(Double.parseDouble(priceTolerance)/100);

        BigDecimal priceUp = previousDatePrice.multiply(BigDecimal.valueOf(1).add(priceToleranceBD)) ;
        BigDecimal priceDown = previousDatePrice.multiply(BigDecimal.valueOf(1).add(priceToleranceBD.multiply(BigDecimal.valueOf(-1)))) ;

        if ( BigDecimal.valueOf(trade.getTradePrice()).compareTo(priceDown) == -1 || BigDecimal.valueOf(trade.getTradePrice()).compareTo(priceUp) == 1 ){
            out = true;
        }

        return out;
    }

    /**
     * Take the previous Date of ValuationDate
     *
     * @return
     */
    private JDate getPreviousDay() {
        Calendar cal = GregorianCalendar.getInstance();

        cal.setTime(getValuationDatetime());
        cal.add(GregorianCalendar.DAY_OF_MONTH,-1);
        return JDate.valueOf(cal);
    }

    /**
     * Validated is trade with Different Currency
     *
     * @param trade
     * @return
     */
    private boolean isDifferentCurrency(final Trade trade) {
        boolean out = false;

        if (!trade.getTradeCurrency().equals(trade.getSettleCurrency())){
            out = true;
        }
        return out;
    }

    /**
     * Take the Price Tolerance from DV
     *
     * @param ds
     * @return
     */
    protected String[] getPriceToleranceFromDV(final DSConnection ds) {
        String[] rst = new String[0];
        final Vector<String> domainValues = LocalCache.getDomainValues(ds, DV_PRICE_TOLERANCE);
        if (!Util.isEmpty(domainValues)) {
            rst = domainValues.toArray(new String[domainValues.size()]);
        }
        return rst;
    }

    /**
     * createTasks if is necessary
     *
     * @param ds
     * @param listTradeException
     */
    protected void createTasks(final DSConnection ds, final List<ExceptionItem> listTradeException) {
        if (!listTradeException.isEmpty()) {
            for (int i = 0 ; i < listTradeException.size(); i++) {
                final Long tradeId = listTradeException.get(i).getTradeIdException();
                final String descriptionException = listTradeException.get(i).getDescriptionException();
                // Check if exists active Tasks state != COMPLETED
                final TaskArray tasksNotCompleted = getActiveTasksEquityException(ds, tradeId, descriptionException, WHERE_TASKS_NOT_COMPLETED.toString());
                if (tasksNotCompleted != null && !tasksNotCompleted.isEmpty()) {
                    completeAndSaveTask(ds, tasksNotCompleted, COMMENT_TASK_COMPLETED);
                }
                // Create new task exception
                createTaskException(ds, tradeId, listTradeException.get(i));
            }
        }
    }

    /**
     * Create Task Exception.
     *
     * @param tradeId
     */
    protected void createTaskException(final DSConnection ds, final long tradeId, final ExceptionItem item) {

        Trade trade = null;
        try {
            trade = ds.getRemoteTrade().getTrade(tradeId);
        }
        catch (final RemoteException e) {
            Log.error(this, String.format("Could not get the trade \"%s\"", tradeId), e);
        }

        if(trade==null) {
            Log.error(this, "Trade is null");
            return;
        }

        String message = getTaskComment(tradeId, item);
        Task taskException = new Task();
        taskException.setStatus(Task.NEW);
        taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        taskException.setEventType(item.getDescriptionException());
        taskException.setComment(message);
        taskException.setTradeId(trade.getLongId());
        taskException.setBookId(trade.getBookId());

        TaskArray task = new TaskArray();
        task.add(taskException);
        try {
            ds.getRemoteBackOffice().saveAndPublishTasks(task,0L,null);
        }
        catch (CalypsoServiceException e) {
            Log.error(this, "Could not save the exception task.");
        }

    }

    /**
     * Get the message for the Task Comment.
     *
     * @param tradeId
     * @param item
     * @return
     */
    protected String getTaskComment(final long tradeId, final ExceptionItem item) {

        String taskComment = EMPTY_SPACE;

        int i = getCase(item);

        switch (i) {
            case 1:  taskComment = String.format(FORMAT_EXCEPTION_PRICE, tradeId);
                break;
            case 2:  taskComment = String.format(FORMAT_EXCEPTION_CURRENCY, tradeId);
                break;
            case 3:  taskComment = String.format(FORMAT_EXCEPTION_DEFAULT, tradeId);
                break;
        }

        return taskComment;
    }

    /**
     * Get the type of Exception
     *
     * @param item
     * @return
     */
    private int getCase(ExceptionItem item) {
        // Default case
        int out = 3;
        String description = item .getDescriptionException();

        if(EXCEPTION_TYPE_PRICE.equals(description)){
            out = 1;
        }
        if(EXCEPTION_TYPE_CURRENCY.equals(description)){
            out = 2;
        }
        return out;
    }

    /**
     * Get tasks the trade have just created a Task previously
     *
     * @param tradeId
     * @return
     */
    protected TaskArray getActiveTasksEquityException(final DSConnection ds, final long tradeId, final String descriptionException, final String addClause) {
        TaskArray tasks = new TaskArray();

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append("trade_id = ").append(tradeId);
        whereClause.append(" AND ");
        whereClause.append("event_class = 'Exception'");
        whereClause.append(" AND ");
        whereClause.append("event_type = '").append(descriptionException).append("'");

        if (addClause != null && !addClause.isEmpty()) {
            whereClause.append(addClause);
        }

        try {
            tasks = ds.getRemoteBO().getTasks(whereClause.toString(), null);
        }
        catch (final RemoteException e) {
            Log.error(this, String.format("Error retrieving tasks from BBDD."), e);
        }

        return tasks;
    }

    /**
     * complete tasks and save it
     *
     * @param tasks
     *            task to complete
     * @param comment
     *            comment
     * @throws RemoteException
     */
    public void completeAndSaveTask(final DSConnection ds, final TaskArray tasks, final String comment) {

        if (tasks != null) {
            for (int i = 0; i < tasks.size(); i++) {
                final Task task = tasks.get(i);
                task.setOwner(ds.getUser());
                task.setCompletedDatetime(new JDatetime());
                task.setStatus(Task.COMPLETED);
                task.setUserComment(comment);
                saveTask(ds, task);
            }
        }
    }

    /**
     * Save one task
     *
     * @param task
     */
    protected void saveTask(final DSConnection ds, final Task task) {
        try {
            ds.getRemoteBO().save(task);
        } catch (final RemoteException e) {
            Log.error(this, e);
        }
    }

    /**
     * Instanciate a new PricingEnv based on ScheduledTask attributes
     *
     * @return a new PricingEnv
     */
    private PricingEnv instanciatePricingEnv() {
        PricingEnv result = null;
        try {
            result = DSConnection.getDefault().getRemoteMarketData()
                    .getPricingEnv(getPricingEnv(), getValuationDatetime());

        } catch (Exception e) {
            Log.error(ScheduledTaskSTC_PROCESS_EQUITY_EOM.class, e);
        }
        return result;
    }


    class ExceptionItem {

        long tradeIdException;
        String descriptionException;

        public ExceptionItem() {
            tradeIdException = 0;
            descriptionException = "";
        }

        public long getTradeIdException() {
            return tradeIdException;
        }

        public String getDescriptionException() {
            return descriptionException;
        }

        public void setTradeIdException(long tradeIdException) {
            this.tradeIdException = tradeIdException;
        }

        public void setDescriptionException(String descriptionException) {
            this.descriptionException = descriptionException;
        }

    }
}
