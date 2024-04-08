package calypsox.tk.util;

import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Dividend;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by dmenendd.
 */
public class ScheduledTaskSTC_VALIDATE_PREDATE_IMPACT_CA extends ScheduledTask {

    private static final long serialVersionUID = -1L;

    /** The fixed Task information returned by getTaskInformation method */
    private static final String TASK_INFORMATION = "Search Trades that are Predated.";

    /** WHERE clause for not COMPLETE tasks */
    private static final StringBuilder WHERE_TASKS_NOT_COMPLETED = new StringBuilder(" AND ").append(" task_status NOT IN ('" + Task.COMPLETED + "') ");

    /** Code format */
    private static final String FORMAT_EXCEPTION_PREDATE_IMPACT_CA = "Trade %s, is predated and entered yesterday and has an impact on a CA.";
    private static final String FORMAT_EXCEPTION_CANCELED_IMPACT_CA = "Trade %s, is canceled yesterday and had an impact on a CA.";
    private static final String FORMAT_EXCEPTION_ACCOUNT_CLOSING_IMPACT_CA = "Seclending Trade %s, was ended yesterday with a past end date. This trade had rights on a CA and now it doesn't.";

    private static final String EXCEPTION_TYPE_PREDATE_IMPACT_CA = "EX_PREDATE_IMPACT_CA";
    private static final String EXCEPTION_TYPE_CANCELED = "EX_CANCELED_IMPACT_CA";
    private static final String EXCEPTION_TYPE_ACCOUNT_CLOSING = "EX_ACCOUNT_CLOSING_IMPACT_CA";

    /** Task messages */
    private static final String COMMENT_TASK_COMPLETED = "Task completed.";

    /** Price Tolerance - Domain Value */
    private static final String DV_PRODUCT_TO_VALIDATE = "productsToValidate";

    /**
     * Instantiates a new scheduled task.
     */
    public ScheduledTaskSTC_VALIDATE_PREDATE_IMPACT_CA() {
        super();
    }

    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {

        Log.debug(this, "Start processing ScheduledTask VALIDATE_PREDATE_IMPACT_CA.");
        final boolean rst = true;

        for (int i = 1; i < 4 ; i++) {
            // Get trades to validate
            final TradeArray tradesForValidation = getTradesForValidation(ds, i);

            if (tradesForValidation != null && !tradesForValidation.isEmpty()) {
                Log.debug(this, "Trades obtained for validation: " + tradesForValidation.size());
                // Validation trades
                final List<ExceptionItem> list = getTradesWithException(ds, tradesForValidation, i);
                // Generate Tasks to Task Station with type of exception
                createTasks(ds, list, i);
            } else {
                Log.debug(this, "No trades obtained for validation");
            }
        }
        return rst;
    }

    /**
     * Get trades for Validation
     *
     * @param ds
     * @param i
     * @return
     */
    protected TradeArray getTradesForValidation(final DSConnection ds, final int i) {
        TradeArray tradesForValidation = new TradeArray();

        // Trades with status different of CANCELED, products of DV_PRODUCT_TO_VALIDATED
        final TradeArray trades = getTradesPending(ds, i);
        if (trades != null && !trades.isEmpty()) {
            tradesForValidation.add(trades.getTrades(), trades.size());
        }

        return tradesForValidation;
    }

    /**
     * Get trades pending
     * values.
     *
     * @param ds
     * @param i
     * @return
     */
    protected TradeArray getTradesPending(final DSConnection ds, int i) {
        TradeArray tArray = null;
        long[] ids = null;

        switch(i) {
            case 1:
                ids = getTradesIdsEnteredToday(ds);
                break;
            case 2:
                ids = getTradesIdsCanceledToday(ds);
                break;
            case 3:
                ids = getTradesIdsClosedToday(ds);
                break;
        }

        if (ids == null || ids.length == 0) {
            Log.info(this,"Couldn't get the Trades.");
            return null;
        }
        Log.info(this, "Total Trades Ids: " + ids.length);

        try {
            tArray = SantanderUtil.getInstance().getTradesWithTradeFilter(ids);
        }
        catch (final RemoteException e) {
            Log.error(this, "Could not get the Trades.", e);
        }

        return tArray;
    }

    /**
     * Get trades Ids with status different of CANCELED, products of DV_PRODUCT_TO_VALIDATED.
     *
     * @param ds
     * @return
     */
    protected long[] getTradesIdsEnteredToday(final DSConnection ds) {
        Log.debug(this, "Get Trades Ids with status different of CANCELED, products of DV_PRODUCT_TO_VALIDATED.");
        long[] ids = null;
        final TradeFilter filter = new TradeFilter();

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append(" trade.trade_status <> 'CANCELED'");
        whereClause.append(" AND ");
        whereClause.append(" trade.entered_date >= TO_DATE('" + getDay(true) + "', 'DD/MM/YYYY') ");
        whereClause.append(" AND ");
        whereClause.append(" trade.entered_date < TO_DATE('" + getDay(false) + "', 'DD/MM/YYYY') ");
        whereClause.append(" AND ");
        whereClause.append(" trade.product_id = product_desc.product_id ");
        whereClause.append(" AND ");
        whereClause.append(" product_desc.product_type IN ('");

        Vector<String> productsToValidate = getProductToValidateFromDV(ds);
        for(int i = 0; i < productsToValidate.size(); i++){
            if(i + 1 == productsToValidate.size())
                whereClause.append(" ,'" + productsToValidate.get(i) + "')");
            else if (i > 0)
                whereClause.append(" ,'" + productsToValidate.get(i) + "'");
            else
                whereClause.append(productsToValidate.get(i) + "'");
        }
        final String fromClause = " trade, product_desc ";

        filter.setSQLFromClause(fromClause);
        filter.setSQLWhereClause(whereClause.toString());

        try {
            ids = ds.getRemoteTrade().getTradeIds(filter, getValuationDatetime(), true);
        } catch (final RemoteException e) {
            Log.error(this, "Could not get the Trades Ids with status different of CANCELED.", e);
        }

        return ids;
    }

    /**
     * Get trades Ids with status CANCELED, products of DV_PRODUCT_TO_VALIDATED.
     *
     * @param ds
     * @return
     */
    protected long[] getTradesIdsCanceledToday(final DSConnection ds) {
        Log.debug(this, "Get Trades Ids with status CANCELED, products of DV_PRODUCT_TO_VALIDATED.");
        long[] ids = null;
        final TradeFilter filter = new TradeFilter();

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append(" trade.trade_status = 'CANCELED'");
        whereClause.append(" AND ");
        whereClause.append(" trade.update_date_time >= TO_DATE('" + getDay(true) + "', 'DD/MM/YYYY') ");
        whereClause.append(" AND ");
        whereClause.append(" trade.update_date_time < TO_DATE('" + getDay(false) + "', 'DD/MM/YYYY') ");
        whereClause.append(" AND ");
        whereClause.append(" trade.product_id = product_desc.product_id ");
        whereClause.append(" AND ");
        whereClause.append(" product_desc.product_type IN ('");

        Vector<String> productsToValidate = getProductToValidateFromDV(ds);
        for(int i = 0; i < productsToValidate.size(); i++){
            if(i + 1 == productsToValidate.size())
                whereClause.append(" ,'" + productsToValidate.get(i) + "')");
            else if (i > 0)
                whereClause.append(" ,'" + productsToValidate.get(i) + "'");
            else
                whereClause.append(productsToValidate.get(i) + "'");
        }
        final String fromClause = " trade, product_desc ";

        filter.setSQLFromClause(fromClause);
        filter.setSQLWhereClause(whereClause.toString());

        try {
            ids = ds.getRemoteTrade().getTradeIds(filter, getValuationDatetime(), true);
        } catch (final RemoteException e) {
            Log.error(this, "Could not get the Trades Ids with status CANCELED.", e);
        }

        return ids;
    }

    /**
     * Get trades Ids with status TERMINATED, products of DV_PRODUCT_TO_VALIDATED.
     *
     * @param ds
     * @return
     */
    protected long[] getTradesIdsClosedToday(final DSConnection ds) {
        Log.debug(this, "Get Trades Ids with status TERMINATED, products of DV_PRODUCT_TO_VALIDATED.");
        long[] ids = null;
        final TradeFilter filter = new TradeFilter();

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append(" trade.trade_status =  'TERMINATED'");
        whereClause.append(" AND ");
        whereClause.append(" trade.update_date_time >= TO_DATE('" + getDay(true) + "', 'DD/MM/YYYY') ");
        whereClause.append(" AND ");
        whereClause.append(" trade.update_date_time < TO_DATE('" + getDay(false) + "', 'DD/MM/YYYY') ");
        whereClause.append(" AND ");
        whereClause.append(" trade.product_id = product_desc.product_id ");
        whereClause.append(" AND ");
        whereClause.append(" product_desc.product_type = 'SecLending'");
        final String fromClause = " trade, product_desc ";

        filter.setSQLFromClause(fromClause);
        filter.setSQLWhereClause(whereClause.toString());

        try {
            ids = ds.getRemoteTrade().getTradeIds(filter, getValuationDatetime(), true);
        } catch (final RemoteException e) {
            Log.error(this, "Could not get the Trades Ids with status TERMINATED.", e);
        }

        return ids;
    }

    /**
     * Search for trades with Exception or Validation.
     *
     * @param ds
     * @param tradesForValidation
     * @param i
     *
     * @return
     */
    protected List<ExceptionItem> getTradesWithException(final DSConnection ds, final TradeArray tradesForValidation, final int i) {
        Log.debug(this, "Start to check the validation for trades.");
        final List<ExceptionItem> result = new ArrayList<>();

        for (final Trade trade : tradesForValidation.getTrades()){
            switch(i) {
                case 1:
                    //Validate if the trade is Predate
                    if ((trade.getSettleDate()).before(getDay(true))) {
                        //Validate if the trade impact on CA
                        if (impactOnCA(ds, trade, false)) {
                            ExceptionItem e = new ExceptionItem();

                            e.setTradeIdException(trade.getLongId());
                            e.setDescriptionException(EXCEPTION_TYPE_PREDATE_IMPACT_CA);

                            result.add(e);
                        }
                    }
                    break;
                case 2:
                    //Validate if the trade impact on CA
                    if (impactOnCA(ds, trade, false)) {
                        ExceptionItem e = new ExceptionItem();

                        e.setTradeIdException(trade.getLongId());
                        e.setDescriptionException(EXCEPTION_TYPE_CANCELED);

                        result.add(e);
                    }
                    break;
                case 3:
                    //Validate if the end Date of the trade is before Today
                    if (endDateBeforeToday(trade)) {
                        if (impactOnCA(ds, trade, true)) {
                            ExceptionItem e = new ExceptionItem();

                            e.setTradeIdException(trade.getLongId());
                            e.setDescriptionException(EXCEPTION_TYPE_ACCOUNT_CLOSING);

                            result.add(e);
                        }
                    }
                    break;
            }
        }
        return result;
    }

    /**
     * Check if impact on CA
     *
     * @param ds
     * @param trade
     * @return
     */
    private boolean impactOnCA(DSConnection ds, Trade trade, boolean validateClosedToday) {
        boolean out = false;
        if (null != trade) {
            Product product = trade.getProduct();
            if (product instanceof Equity) {
                Vector<Dividend> dividends = ((Equity) ((Equity) trade.getProduct()).getSecurity()).getDividends();
                if (dividends.size() > 0) {
                    for (int i = 0; i < dividends.size(); i++) {
                        out = dividends.get(i).getRecordDate().before(trade.getSettleDate()) ? false : true;
                        if (out) {
                            // D > Payment Date
                            out = getDay(true).after(dividends.get(i).getPaymentDate()) ? true : false;
                            if (out)
                                break;
                        }
                    }
                }
            } else if (product instanceof SecLending) {
                if (((SecLending) product).getSecurity() instanceof Equity){
                    SecLending pdv = (SecLending) product;
                    Equity equity = ((Equity) pdv.getSecurity());
                    Vector<Dividend> dividends = ((Equity) equity.getSecurity()).getDividends();
                    if (dividends.size() > 0) {
                        for (int i = 0; i < dividends.size(); i++) {
                            out = dividends.get(i).getRecordDate().before(pdv.getStartDate()) ? false : true;
                            if("TERM".equalsIgnoreCase(pdv.getMaturityType()) && out){
                                if (!validateClosedToday){
                                    out = pdv.getEndDate().after(dividends.get(i).getRecordDate()) ? true : false;
                                } else{
                                    out = pdv.getEndDate().after(dividends.get(i).getRecordDate()) ? false : true;
                                }
                            }
                            if (out) {
                                // D-1 > Payment Date
                                out = getDay(true).after(dividends.get(i).getPaymentDate()) ? true : false;
                                if (out)
                                    break;
                            }
                        }
                    }
                }
            }
        }

        return out;
    }

    /**
     * Validate if the end Date of the trade is before Today
     *
     * @param trade
     * @return
     */
    private boolean endDateBeforeToday(final Trade trade) {
        boolean out = false;
        if (null != trade) {
            Product product = trade.getProduct();
            if (product instanceof SecLending) {
                if (((SecLending) product).getSecurity() instanceof Equity){
                    SecLending pdv = (SecLending) product;
                    //Previous Date
                    if(pdv.getEndDate().before(getDay(true))){
                        out = true;
                    }
                }
            }
        }
        return out;
    }

    /**
     * Take the ValuationDate or the previous Date of ValuationDate
     *
     * @param previous
     * @return
     */
    private JDate getDay(boolean previous) {
        Calendar cal = GregorianCalendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");

        cal.setTime(getValuationDatetime());
        if (previous) {
            cal.add(GregorianCalendar.DAY_OF_MONTH, -1);
        }

        Date date = cal.getTime();
        String date1 = format.format(date);

        return JDate.valueOf(date1);
    }

    /**
     * Take the Price Tolerance from DV
     *
     * @param ds
     * @return
     */
    protected Vector<String> getProductToValidateFromDV(final DSConnection ds) {
        Vector<String> domainValues = LocalCache.getDomainValues(ds, DV_PRODUCT_TO_VALIDATE);
        return domainValues;
    }

    /**
     * createTasks if is necessary
     *
     * @param ds
     * @param listTradeException
     * @param i
     */
    protected void createTasks(final DSConnection ds, final List<ExceptionItem> listTradeException, final int i) {
        if (!listTradeException.isEmpty()) {
            for (int j = 0 ; j < listTradeException.size(); j++) {
                final Long tradeId = listTradeException.get(j).getTradeIdException();
                final String descriptionException = listTradeException.get(j).getDescriptionException();
                // Check if exists active Tasks state != COMPLETED
                final TaskArray tasksNotCompleted = getActiveTasksEquityException(ds, tradeId, descriptionException, WHERE_TASKS_NOT_COMPLETED.toString());
                if (tasksNotCompleted != null && !tasksNotCompleted.isEmpty()) {
                    completeAndSaveTask(ds, tasksNotCompleted, COMMENT_TASK_COMPLETED);
                }
                // Create new task exception
                createTaskException(ds, tradeId, listTradeException.get(j), i);
            }
        }
    }

    /**
     * Create Task Exception.
     *
     * @param ds
     * @param tradeId
     * @param item
     * @param i
     */
    protected void createTaskException(final DSConnection ds, final long tradeId, final ExceptionItem item, final int i) {

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

        String message = null;
        switch (i){
            case 1:
                message = String.format(FORMAT_EXCEPTION_PREDATE_IMPACT_CA, tradeId);
                break;
            case 2:
                message = String.format(FORMAT_EXCEPTION_CANCELED_IMPACT_CA, tradeId);
                break;
            case 3:
                message = String.format(FORMAT_EXCEPTION_ACCOUNT_CLOSING_IMPACT_CA, tradeId);
                break;
        }

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
