package calypsox.tk.util;

import com.calypso.apps.util.ProcessTradeUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.marketdata.FilterSet;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.DataServer;
import com.calypso.tk.service.TradeServerImpl;
import com.calypso.tk.util.IExecutionContext;
import com.calypso.tk.util.ParallelExecutionException;
import com.calypso.tk.util.ProcessTradeJobResult;
import com.calypso.tk.util.ScheduledTaskPROCESS_TRADE;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;
import org.apache.fop.fonts.type1.PostscriptParser;

import java.sql.Connection;
import java.util.Collections;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ScheduledTaskPROCESS_TRADE_FULLSAVE extends ScheduledTaskPROCESS_TRADE {

    public Set<Long> __mirrorIds;
    public Lock __mirrorIdsLock = new ReentrantLock();


    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        boolean ret = true;
        if (this._publishB || this._sendEmailB) {
            ret = super.process(ds, ps);
        }

        String exec = null;
        this.__mirrorIds = Collections.newSetFromMap(new ConcurrentHashMap());

        ProcessTradeJobResult res;
        try {
            res = (ProcessTradeJobResult) this.parallelRun(ds, ps);
        } catch (ParallelExecutionException var11) {
            Log.error("PROCESS_TRADE", "Exception in ParallelRun", var11);
            res = this.createJobResult((IExecutionContext) null, "Exception in ParallelRun", new TaskArray());
        }

        if (this._trades != null) {
            try {
                this._trades.discard();
            } catch (Exception var10) {
                Log.error("PROCESS_TRADE", "Exception in discard TradeFilterPage", var10);
                res = this.createJobResult((IExecutionContext) null, "Exception in discard TradeFilterPage", new TaskArray());
            }
        }

        exec = res.getFirst();
        TaskArray tasks = res.getSecond();
        Task task = new Task();
        task.setObjectLongId((long) this.getId());
        task.setEventClass("Exception");
        task.setNewDatetime(new JDatetime());
        task.setDatetime(new JDatetime());
        task.setPriority(1);
        task.setId(0L);
        task.setStatus(0);
        if (exec == null) {
            task.setCompletedDatetime(new JDatetime());
            task.setEventType("EX_INFORMATION");
            task.setStatus(2);
            task.setComment(this.toString());
        } else {
            task.setEventType("EX_EXCEPTION");
            task.setComment("Scheduled Task did not succeed " + this.toString() + "(" + exec + ")");
        }

        task.setUndoTradeDatetime(task.getDatetime());
        task.setCompletedDatetime(task.getDatetime());
        task.setNewDatetime(task.getDatetime());
        task.setUnderProcessingDatetime(task.getDatetime());
        task.setSource(this.getType());
        task.setAttribute("ScheduledTask Id=" + String.valueOf(this.getId()));

        try {
            tasks.add(task);
            getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(tasks, 0L, (String) null);
        } catch (Exception var9) {
            Log.error(this, var9);
        }

        return ret && exec == null;
    }

    @Override
    public void checkAndSaveTrades(DSConnection ds, TradeArray trades, FilterSet extraFilterSetAfterTradeFilter, Action tradeAction, boolean unAssignedRulesB, TaskArray tasks, boolean createTask, PricingEnv env, JDatetime valDatetime) {
        for (int i = 0; i < trades.size(); ++i) {
            Trade trade = trades.elementAt(i);
            if (Log.isCategoryLogged(Log.OLD_TRACE)) {
                Log.debug(Log.OLD_TRACE, "Processing Trade " + trade.getLongId());
            }

            if (trade.getProduct() == null) {
                if (createTask) {
                    tasks.add(this.createException("Invalid Trade " + trade.getLongId() + " No product found", true));
                }
            } else if (trade.getStatus() == null) {
                if (createTask) {
                    tasks.add(this.createException("Invalid Trade " + trade.getLongId() + " Null Status ", true));
                }
            } else if (extraFilterSetAfterTradeFilter != null && !extraFilterSetAfterTradeFilter.accept(trade)) {
                Log.debug("PROCESS_TRADE", "Trade " + trade + " rejected by FilterSet " + extraFilterSetAfterTradeFilter);
            } else if (!AccessUtil.isAuthorized("Trade", trade.getProduct().getType(), trade.getStatus().toString(), tradeAction.toString())) {
                if (createTask) {// 602
                    tasks.add(this.createException("Action " + tradeAction.toString() + " denied on " + trade.getStatus() + " for Trade " + trade.getLongId(), true));
                }
            } else {
                if (trade.getMirrorTradeLongId() > 0L) {
                    this.__mirrorIdsLock.lock();

                    try {
                        this.__mirrorIds.add(trade.getMirrorTradeLongId());
                        if (this.__mirrorIds.contains(trade.getLongId())) {
                            Log.info("PROCESS_TRADE", "Trade " + trade.getLongId() + " is mirror trade of a trade already processed.");
                            this.__mirrorIds.remove(trade.getLongId());
                            continue;
                        }

                        this.__mirrorIds.add(trade.getLongId());
                    } finally {
                        this.__mirrorIdsLock.unlock();
                    }
                }

                if (unAssignedRulesB && trade.getCustomTransferRuleB()) {
                    trade.setTransferRules(new Vector());
                    trade.setCustomTransferRuleB(false);
                }

                trade.setAction(tradeAction);
                trade.setEnteredUser(ds.getUser());

                try {
                    if (env != null && valDatetime != null) {
                        trade.getProduct().enrichProcessTrade(trade, valDatetime, env);
                    } else {
                        Log.info("PROCESS_TRADE", "Trade Id:" + trade.getLongId() + ", Process trade is not executed, as the PricingEnv or ValDatetime is empty");
                    }

                    if (!trade.getAction().equals(Action.REJECT) && !Action.isTradeRejectAction(trade.getAction())) {
                        getReadWriteDS(ds).getRemoteTrade().save(trade);
                        PSEventTrade psEventTrade = new PSEventTrade();
                        psEventTrade.setTrade(trade);
                        getReadWriteDS(ds).getRemoteTrade().saveAndPublish(psEventTrade);

                    } else {
                        trade = ProcessTradeUtil.buildRejectedTrade(trade, trade.getAction());
                        getReadWriteDS(ds).getRemoteTrade().save(trade);
                    }

                } catch (Exception var16) {// 646
                    Log.error("PROCESS_TRADE", var16);
                    if (createTask) {// 648
                        tasks.add(this.createException("Trade " + trade.getLongId() + " can not be saved:" + var16.getMessage(), true));
                    }
                }
            }
        }

    }

}
