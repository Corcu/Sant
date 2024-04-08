/*
 *
 * Copyright (c) 2000 by Calypso Technology, Inc.
 * 595 Market Street, Suite 1980, San Francisco, CA  94105, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Calypso Technology, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Calypso Technology.
 *
 */

package calypsox.tk.util;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduledTaskUnPriceTrades extends ScheduledTask {
    private static final long serialVersionUID = 123L;

    public static final String TRADE_ACTION_TO_APPLY = "TradeAction To Apply";
    public static final String NO_OF_THREADS = "No Of Threads";
    public static final String USE_VALDATE_CONTRACTIA = "Use ST ValDate CONTRACT_IA";

    public ScheduledTaskUnPriceTrades() {
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();

        attributeList.add(attribute(TRADE_ACTION_TO_APPLY).domainName("tradeAction"));
        attributeList.add(attribute(NO_OF_THREADS).integer());
        attributeList.add(attribute(USE_VALDATE_CONTRACTIA).booleanType());
        return attributeList;
    }

    @Override
    public boolean process(final DSConnection ds, final PSConnection ps) {

        buildAttributeDefinition();
        boolean exec = false;
        if (this._publishB || this._sendEmailB) {
            exec = super.process(ds, ps);
        }
        final TaskArray tasks = new TaskArray();
        if (this._executeB) {
            exec = handleProcessTrade(ds, ps, tasks);
        }

        return exec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean isValidInput(final Vector messages) {
        boolean ret = super.isValidInput(messages);

        if (Util.isEmpty(this._tradeFilter)) {
            messages.addElement("Must select Trade Filter.");
            ret = false;
        }
        if (Util.isEmpty(getAttribute(USE_VALDATE_CONTRACTIA))) {
            messages.addElement("Specify value for attribute " + USE_VALDATE_CONTRACTIA);
            ret = false;
        }
        if (Util.isEmpty(getAttribute(NO_OF_THREADS))) {
            messages.add("Specify value for attribute " + NO_OF_THREADS);
            ret = false;
        } else {
            try {
                Integer.parseInt(getAttribute(NO_OF_THREADS));
            } catch (Exception exc) {
                Log.error(this, exc); //sonar
                messages.add("Specify a vlaid number for attribute " + NO_OF_THREADS);
                ret = false;
            }
        }
        return ret;
    }

    @Override
    public String getTaskInformation() {
        return "This Scheduled Task moves trades to UNPRICE: \n";
    }

    /**
     * <code>initTrade</code> loads the trades from trade filter
     *
     * @throws Exception
     */
    protected TradeArray initTrade(final DSConnection ds, final TaskArray tasks) throws Exception {
        TradeArray trades = new TradeArray();

        if (this._tradeFilter != null) {
            TradeFilter tf = null;
            tf = BOCache.getTradeFilter(ds, this._tradeFilter);
            if (tf == null) {
                throw new Exception("Could not load Trade Filter: " + this._tradeFilter);
            }
            trades = ds.getRemoteTrade().getTrades(tf, this.getDatetime());
            Log.debug(ScheduledTaskUnPriceTrades.class, "Trade loaded: " + trades.size());
        }
        return trades;
    }

    protected boolean handleProcessTrade(final DSConnection ds, final PSConnection ps, final TaskArray tasks) {
        TradeArray trades = null;
        try {
            trades = initTrade(ds, tasks);
        } catch (final Exception e1) {
            Log.error(ScheduledTaskUnPriceTrades.class, e1);
            return false;
        }

        if (trades == null) {
            tasks.add(createException("Nothing to process", false));
            return true;
        }

        int noOfThreads = Integer.parseInt(getAttribute(NO_OF_THREADS));
        ExecutorService exec = getBoundedQueueThreadPoolExecutor(noOfThreads);

        try {
            for (int i = 0; i < trades.size(); i++) {
                final Trade trade = trades.elementAt(i);
                // Submit to the executor
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        String action = getAttribute(TRADE_ACTION_TO_APPLY);
                        applyActionToTrade(trade, Action.valueOf(action), ds, tasks);
                    }
                });
            }
            // Await until all the tasks are completed
        } finally {
            exec.shutdown();
            try {
                exec.awaitTermination(5, TimeUnit.MINUTES);
                Log.info(ScheduledTaskUnPriceTrades.class, "Has Executor been terminated=" + exec.isTerminated());
                Log.info(ScheduledTaskUnPriceTrades.class, "Has Executor been shutdown=" + exec.isShutdown());
            } catch (InterruptedException e) {
                Log.error(ScheduledTaskUnPriceTrades.class, e);
                exec.shutdownNow();
            }
        }
        return true;
    }

    private void applyActionToTrade(final Trade trade, final Action action, DSConnection ds, final TaskArray tasks) {
        if (CollateralUtilities.isTradeActionApplicable(trade, action)) {
            try {
                trade.setAction(action);
                trade.setEnteredUser(ds.getUser());
                trade.removeKeyword(TradeInterfaceUtils.TRD_IMP_FIELD_MTM_DATE);
                removeCurrentIndAmountFees(trade);
                // removeContractInfo(trade);
                if (CollateralUtilities.isIAExposureTrade(trade)
                        && (Boolean.valueOf(getAttribute(USE_VALDATE_CONTRACTIA)))) {
                    JDate valDate = getValuationDatetime().getJDate(TimeZone.getDefault());
                    String mtmDate = CollateralUtilities.getMTMDate(valDate);
                    trade.addKeyword(TradeInterfaceUtils.TRD_IMP_FIELD_MTM_DATE, mtmDate);
                }
                DSConnection.getDefault().getRemoteTrade().save(trade);
            } catch (final Exception e) {
                Log.error(ScheduledTaskUnPriceTrades.class, e);
                tasks.add(createException("Trade " + trade.getLongId() + " can not be saved:" + e.getMessage(), true));
            }
        } else {
            tasks.add(createException(
                    "Could not apply action " + action.toString() + " to Trade with Id: " + trade.getLongId(), true));
        }
    }

    /**
     * <code>createException</code> create an Exception in the TStation.
     *
     * @param comment       Exception comment
     * @param criticalError if true, the Task Status is NEW, otherwise its COMPLETED.
     * @return the Exception to be saved
     */
    protected Task createException(final String comment, final boolean criticalError) {
        final Task task = new Task();
        task.setObjectLongId(getId());
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setNewDatetime(new JDatetime());
        task.setDatetime(new JDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setObjectLongId(getId());
        task.setStatus(criticalError ? Task.NEW : Task.COMPLETED);
        task.setEventType("EX_" + BOException.EXCEPTION);
        task.setComment(comment);

        task.setUndoTradeDatetime(task.getDatetime());
        task.setCompletedDatetime(task.getDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setUnderProcessingDatetime(task.getDatetime());
        task.setSource(getType());
        return task;
    }

    private ExecutorService getBoundedQueueThreadPoolExecutor(int noOfThreads) {
        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(noOfThreads * 3);
        ExecutorService executor = new ThreadPoolExecutor(noOfThreads, noOfThreads, 0, TimeUnit.SECONDS, blockingQueue,
                new UnPriceThreadPolFactory("UnPriceTradeThreadPool"), new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * This method removes any Fee with type starting with IND_AMOUNT
     *
     * @param trade
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void removeCurrentIndAmountFees(Trade trade) {

        Vector feeVect = trade.getFees();
        if (!Util.isEmpty(feeVect)) {
            Vector finalFeeVect = new Vector();
            for (int i = 0; i < feeVect.size(); i++) {
                final Fee fee = (Fee) feeVect.get(i);
                finalFeeVect.add(fee);
            }
            if (!Util.isEmpty(finalFeeVect)) {
                trade.setFees(finalFeeVect);
            } else {
                trade.setFees(null);
            }

        }
    }

}

class UnPriceThreadPolFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(0);
    protected String poolName;

    UnPriceThreadPolFactory(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread t = new Thread(runnable, this.poolName + "_" + this.threadNumber.incrementAndGet());

        t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.error("ScheduledTaskUnPriceTrades", "Error in thread " + thread.getName(), throwable);
            }
        });

        return t;
    }
}
