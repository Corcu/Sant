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
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;

import java.util.*;

public class ScheduledTaskSANT_MATURE_TRADES extends ScheduledTask {
    private static final long serialVersionUID = 123L;

    private static Map<String, String> KEYWORD_CM_AFTER_MATURITY;

    public ScheduledTaskSANT_MATURE_TRADES() {
    }

    @Override
    public boolean process(final DSConnection ds, final PSConnection ps) {

        @SuppressWarnings("unused")
        boolean ret = true;
        if (this._publishB || this._sendEmailB) {
            ret = super.process(ds, ps);
        }
        final TaskArray tasks = new TaskArray();
        boolean exec = false;
        if (this._executeB) {
            exec = handleProcessTrade(ds, ps, tasks);
        }

        return exec;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean isValidInput(final Vector messages) {
        boolean ret = super.isValidInput(messages);
        if (Util.isEmpty(getAttribute(TRADE_FROM_STATUS))) {
            messages.add("Specify value for attribute " + TRADE_FROM_STATUS);
            ret = false;
        }
        if (Util.isEmpty(getAttribute(TRADE_ACTION_TO_APPLY))) {
            messages.add("Specify value for attribute " + TRADE_ACTION_TO_APPLY);
            ret = false;
        }
        if (Util.isEmpty(getAttribute(NO_OF_DAYS_AFTER_MATURITY))) {
            messages.add("Specify value for attribute " + TRADE_ACTION_TO_APPLY);
            ret = false;
        } else {
            try {
                @SuppressWarnings("unused")
                int temp = Integer.parseInt(getAttribute(NO_OF_DAYS_AFTER_MATURITY));
            } catch (Exception e) {
                Log.error(this, e); //sonar
                messages.add("Specify a valid integer for attribute " + TRADE_ACTION_TO_APPLY);
                ret = false;
            }
        }

        return ret;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();

        attributeList.add(attribute(TRADE_ACTION_TO_APPLY).domainName("tradeAction"));
        attributeList.add(attribute(NO_OF_DAYS_AFTER_MATURITY).integer());
        attributeList.add(attribute(TRADE_FROM_STATUS).domainName("tradeStatus"));
        return attributeList;
    }

    public static final String TRADE_FROM_STATUS = "Trade From Status";
    public static final String TRADE_ACTION_TO_APPLY = "TradeAction To Apply";
    public static final String NO_OF_DAYS_AFTER_MATURITY = "No Of Days After Maturity";

    /*
        @Override
        public Vector<String> getDomainAttributes() {
            final Vector<String> v = new Vector<String>();
            v.add(TRADE_FROM_STATUS);
            v.add(TRADE_ACTION_TO_APPLY);
            v.add(NO_OF_DAYS_AFTER_MATURITY);
            return v;
        }
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Vector getAttributeDomain(final String attr, final Hashtable currentAttr) {
            Vector vector = new Vector();
            if (attr.equals(TRADE_ACTION_TO_APPLY)) {
                try {
                    vector.addAll(DSConnection.getDefault().getRemoteReferenceData().getDomainValues("tradeAction"));
                } catch (final RemoteException e) {
                    Log.error(ScheduledTaskSANT_MATURE_TRADES.class, "Error retrieving Trade Actions from domain. ", e);
                }
            } else if (attr.equals(TRADE_FROM_STATUS)) {
                try {
                    vector.addAll(DSConnection.getDefault().getRemoteReferenceData().getDomainValues("tradeStatus"));
                } catch (final RemoteException e) {
                    Log.error(ScheduledTaskSANT_MATURE_TRADES.class, "Error retrieving Trade Statuses from domain. ", e);
                }
            } else {
                vector = super.getAttributeDomain(attr, currentAttr);
            }

            return vector;
        }
    */
    @Override
    public String getTaskInformation() {
        return "This Scheduled Task moves trades to MATURED status & removes CM keyword control: \n";
    }

    /**
     * loads the trades
     */
    protected TradeArray loadTrades(final DSConnection ds, final TaskArray tasks) throws Exception {
        final JDate valDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        int dasyAfterMaturity = Integer.parseInt(getAttribute(NO_OF_DAYS_AFTER_MATURITY));
        JDate matDate = valDate.addDays(-1 * dasyAfterMaturity);

        StringBuffer where = new StringBuffer();
        where.append("trade.trade_status='READY_TO_PRICE' and product_desc.product_type in ('Repo','SecLending','CollateralExposure')");
        where.append(" and product_desc.maturity_date is not null and product_desc.maturity_date<"
                + Util.date2SQLString(matDate));
        where.append(" and not exists(select 1 from pl_mark where trade.trade_id=pl_mark.trade_id and pl_mark.VALUATION_DATE>"
                + Util.date2SQLString(matDate) + ") ");

        TradeArray trades = DSConnection.getDefault().getRemoteTrade().getTrades(null, where.toString(), null, null);
        Log.info(ScheduledTaskSANT_MATURE_TRADES.class, "Trade loaded: " + trades.size());

        return trades;

    }

    protected boolean handleProcessTrade(final DSConnection ds, final PSConnection ps, final TaskArray tasks) {
        TradeArray trades = null;

        //v14 - Recover Keyword
        KEYWORD_CM_AFTER_MATURITY = CollateralUtilities.getCMAfterMaturityKeywordFromDV();
        try {
            trades = loadTrades(ds, tasks);
        } catch (final Exception e1) {
            Log.error(ScheduledTaskSANT_MATURE_TRADES.class, e1);
            return false;
        }

        if ((trades == null) || (trades.size() == 0)) {
            tasks.add(createException("Nothing to process", false));
            return true;
        }

        List<Trade> tradesToSave = new ArrayList<Trade>();
        for (int i = 0; i < trades.size(); i++) {

            Trade trade = trades.elementAt(i);
            trade.setAction(Action.valueOf(getAttribute(TRADE_ACTION_TO_APPLY)));
            //GSM 10/05/2016 - Adaptation to module 2.7.9 - Mig 14.
            Log.debug(this, "trade id:" + trade.getLongId() + " removed CM Maturity keyword ");
            removeMaturityKeywordCM(trade);
            tradesToSave.add(trade);

            // Save if it is last trade or there are 100 trades in the list
            if ((tradesToSave.size() >= 100) || ((i + 1) == trades.size())) {
                try {
                    long[] saveTradesResult = DSConnection.getDefault().getRemoteTrade()
                            .saveTrades(new ExternalArray(tradesToSave));

                    Log.info(this, "saveTradesResult - " + saveTradesResult);
                } catch (Exception e) {
                    Log.error(ScheduledTaskSANT_MATURE_TRADES.class, "Error while maturing trades.", e);
                }

                // re-initialise the list
                tradesToSave = new ArrayList<Trade>();
            }

        }

        return true;
    }

    /**
     * Removes keyword CM acceptance for mature trades
     *
     * @param oldTrade
     */
    private void removeMaturityKeywordCM(final Trade trade) {

        if (KEYWORD_CM_AFTER_MATURITY != null) {
            final String keywordValue = KEYWORD_CM_AFTER_MATURITY.keySet().iterator().next();
            trade.addKeyword(keywordValue, "false");
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

}
