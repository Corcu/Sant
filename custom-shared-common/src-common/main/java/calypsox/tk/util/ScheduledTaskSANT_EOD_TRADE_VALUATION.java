package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CustomValuationUtil;
import com.calypso.tk.util.ScheduledTaskEOD_TRADE_VALUATION;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.ValuationUtil;

import java.util.*;

public class ScheduledTaskSANT_EOD_TRADE_VALUATION extends ScheduledTaskEOD_TRADE_VALUATION {

    @Override
    public String getTaskInformation() {
        return "Compute Valuation with CORE Repo fixed";
    }

    @Override
    protected boolean handleTradeValuation(DSConnection ds, PSConnection ps, Task task, long maxWaitTime, TaskArray tasks) {


        JDatetime valDatetime = this.getValuationDatetime2(this.getCurrentDate());
        JDatetime undoDatetime = this.getUndoTime() > 0 ? this.getUndoTime(this.getCurrentDate()) : null;
        if (Log.isCategoryLogged(Log.OLD_TRACE)) {// 146
            Log.debug(Log.OLD_TRACE, "ScheduledTask TradeValuation " + this.getId() + " ValDatetime: " + valDatetime + " UndoDatetime: " + undoDatetime + " PE: " + this._pricingEnv);
        }

        boolean checkMktDataOnlyB = this.getBooleanAttribute("Check Market Data Only");
        String keepCF = this.getAttribute("KEEP_CASH_FLOWS");
        boolean keepCFB = keepCF != null ? Boolean.parseBoolean(keepCF) : false;
        ValuationUtil valuationUtil = new CustomValuationUtil();
      //  CustomValuationUtil customValuationUtil = new CustomValuationUtil();
        valuationUtil.setTimeout(maxWaitTime);
        if (!valuationUtil.setPricingEnv(this._pricingEnv, valDatetime, ds)) {
            Log.error(this, "Invalid pricingEnv " + this._pricingEnv);
            return false;
        } else if (this._tradeFilter == null) {
            task.setComment("Trade Filter is not defined");
            return false;
        } else if (!valuationUtil.setTradeFilter(this._tradeFilter, ds)) {
            task.setComment("Error Loading Trade Filter " + this._tradeFilter + " For:" + this);
            return false;
        } else {
            if (this.preLoadProducts()) {
                TradeFilter tf = valuationUtil.getTradeFilter();
                if (tf != null) {
                    try {
                        BOCache.getProductsFromPLPosition(ds, tf, true);
                    } catch (Exception var35) {
                        Log.error(this, var35);
                    }
                }
            }

            String v = this.getAttribute("STATIC DATA FILTER");
            if (v != null && v.trim().length() > 0) {
                valuationUtil.setStaticDataFilter(v, ds);
            }

            boolean removePosition = this.getIsRemovePosition();
            boolean includeMatured = this.getMaturedTrades();
            if (includeMatured && removePosition) {
                task.setComment("Include Matured Trade and Remove Position are not compatible.Remove Position must be false when select Matured Trade" + this);
                return false;
            } else if (!valuationUtil.loadTrades(valDatetime, undoDatetime, removePosition, this.getUseTradeDatePosition(), this.getEOD(), true, includeMatured, ds)) {
                task.setComment("Error Loading Trades for Trade Filter " + this._tradeFilter + " For:" + this);
                return false;
            } else if (valuationUtil.getNumberOfTrades() == 0) {
                task.setComment("Nothing to process for TRADE VALUATION FOR: " + this);
                return true;
            } else {
                v = this.getAttribute("PRELOAD_POSTINGS");
                if (v != null && (v.toLowerCase().equals("y") || v.toLowerCase().equals("true") || v.toLowerCase().equals("yes"))) {
                    try {
                        ds.getRemoteAccounting().ensurePostingsCached(valuationUtil.getTradeFilter());
                        ds.getRemoteAccounting().ensureCresCached(valuationUtil.getTradeFilter());
                    } catch (Exception var34) {
                        Log.error(this, var34);
                    }
                }

                if (this.getPricerMeasures() != null && this.getPricerMeasures().size() != 0) {
                    valuationUtil.setPricerMeasure(this.getPricerMeasures());
                    valuationUtil.setDailyPM(this.isDailyPM());
                    Hashtable exceptions = new Hashtable();
                    String disp = this.getAttribute("DISPATCHER");
                    if (Util.isEmpty(disp)) {
                        disp = null;
                    }

                    String stradePerJob = this.getAttribute("DISPATCHER_TRADE_PER_JOB");
                    int jobPerTrade = stradePerJob != null ? Integer.parseInt(stradePerJob) : 0;
                    int eventsPerPublish = this.getEventsPerPublish();
                    JDatetime eventDatetime = valDatetime;
                    if (!this.isValDateEOM()) {
                        eventDatetime = this.getValuationDatetime();
                    }

                    DSConnection rwDs = null;

                    try {
                        rwDs = getReadWriteDS(ds);
                    } catch (Exception var33) {
                        Log.error(this, var33);
                    }

                    if (rwDs == null || checkMktDataOnlyB) {
                        eventsPerPublish = 0;
                    }

                    int eventsPerArray = this.getEventsPerArray();
                    boolean useEventArray = this.usePSEventArray();
                    if (!this.postSQLLoadedFiltering(valuationUtil, ds, tasks)) {
                        task.setComment("No Eligible Trades Selected for TRADE VALUATION: " + this);
                        return false;
                    } else {
                        boolean allOk = valuationUtil.priceTrades(valDatetime, exceptions, this.getAttribute("VALUATION"), this.getAttribute("VALCCY"), disp, jobPerTrade, keepCFB, eventsPerPublish, useEventArray, eventsPerArray, eventDatetime, undoDatetime, rwDs);
                        if (exceptions.size() > 0) {
                            StringBuffer buffer = new StringBuffer();
                            int counter = 0;

                            String message;
                            for(Enumeration e = exceptions.keys(); e.hasMoreElements(); buffer.append(message)) {
                                Trade trade = (Trade)e.nextElement();
                                message = (String)exceptions.get(trade);
                                this.addException(message, trade, tasks);
                                if (counter > 0) {
                                    buffer.append(System.getProperty("line.separator"));
                                }
                            }

                            Log.error(this, buffer.toString());
                            task.setComment("Error Pricing Trade for " + this);
                            int commentId = 0;
                            List messages = new ArrayList(valuationUtil.items.keySet());
                            if (!Util.isEmpty(messages)) {
                                List msg = new ArrayList();
                                Iterator var30 = messages.iterator();

                                while(var30.hasNext()) {
                                    Object key = var30.next();
                                    String value = (String)valuationUtil.items.get(key);
                                    msg.add(key + ", " + value);
                                }

                                commentId = this.saveGenericComment(msg).getId();
                            }

                            task.setLinkId((long)commentId);
                        }

                        if (eventsPerPublish <= 0 && !checkMktDataOnlyB) {
                            boolean publishedOK = this.publishTradeEvents(valuationUtil, ds, ps, task, tasks, valDatetime, undoDatetime);
                            allOk = allOk && !this.containError(valuationUtil);
                            return allOk && publishedOK;
                        } else {
                            return allOk;
                        }
                    }
                } else {
                    task.setComment("No Measures Selected for TRADE VALUATION: " + this);
                    return false;
                }
            }
        }
    }

    void addException(String message, Trade trade, TaskArray tasks) {
        Task task = new Task();
        task.setObjectLongId((long)this.getId());
        task.setEventClass("Exception");
        task.setNewDatetime(this.getValuationDatetime2(this.getCurrentDate()));
        task.setUnderProcessingDatetime(this.getDatetime());
        task.setUndoTradeDatetime(this.getUndoDatetime());
        task.setDatetime(this.getDatetime());
        task.setPriority(1);
        task.setId(0L);
        task.setStatus(0);
        task.setSource(this.getType());
        task.setAttribute("ScheduledTask Id=" + String.valueOf(this.getId()));
        task.setComment(this.toString() + " " + trade.getLongId() + " " + trade.getProduct().getType() + " " + message);
        task.setEventType("EX_" + this.getExceptionString());
        tasks.add(task);
    }
}
