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

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.Task;
import com.calypso.tk.collateral.dto.MarginCallAllocationDTO;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.UnavailabilityTransfer;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

public class ScheduledTaskSANT_CREATE_UNAVAIL_TRADES extends ScheduledTask {
    protected static final long serialVersionUID = 123L;

    public ScheduledTaskSANT_CREATE_UNAVAIL_TRADES() {
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
            exec = createUnavailabilityTrades(ds, ps, tasks);
        }

        return exec;
    }

    @SuppressWarnings({"rawtypes"})
    private boolean createUnavailabilityTrades(DSConnection ds, PSConnection ps, TaskArray tasks) {
        JDate processDate = getValuationDatetime().getJDate(TimeZone.getDefault());

        // 1. get Allocations with trade id=0 for the day.
        ArrayList<String> tableList = new ArrayList<String>();
        tableList.add("margin_call_allocation");
        String where = "trade_id=0 and trunc(trade_date)=" + Util.date2SQLString(processDate);
        List<MarginCallAllocationDTO> marginCallAllocations = null;
        try {
            marginCallAllocations = ServiceRegistry.getDefault().getDashBoardServer()
                    .loadMarginCallAllocations(where, tableList);
        } catch (RemoteException e) {
            Log.error(ScheduledTaskSANT_CREATE_UNAVAIL_TRADES.class, "Error getting allocations with ttrade_id=0", e);
            return false;
        }

        List<Trade> tradeList = new ArrayList<Trade>();
        if (!Util.isEmpty(marginCallAllocations)) {
            for (MarginCallAllocationDTO allocationDto : marginCallAllocations) {
                if (allocationDto instanceof SecurityAllocationDTO) {
                    SecurityAllocationDTO secDto = (SecurityAllocationDTO) allocationDto;
                    Trade trade;
                    try {
                        trade = buildUnavailabilityTransfer(secDto);
                        tradeList.add(trade);
                    } catch (Exception e) {
                        Log.warn(
                                ScheduledTaskSANT_CREATE_UNAVAIL_TRADES.class,
                                "Error building UnavailabilityTransfer with Security= and ContractId="
                                        + secDto.getCollateralConfigId());
                        Log.warn(this, e); //sonar
                    }

                }
            }
        }

        for (Trade trade : tradeList) {
            try {
                Vector vect = new Vector();
                trade.isValid(vect);
                trade.getProduct().isValidInput(vect);
                if (!Util.isEmpty(vect)) {
                    System.out.println(vect);
                }
                long tradeId = getDSConnection().getRemoteTrade().save(trade);
                Log.info(ScheduledTaskSANT_CREATE_UNAVAIL_TRADES.class, "Trade has been created with id =" + tradeId);
            } catch (RemoteException e) {
                Log.warn(
                        ScheduledTaskSANT_CREATE_UNAVAIL_TRADES.class,
                        "Error creating UnavailabilityTransfer with Security= and ContractId="
                                + trade.getKeywordValue(CollateralStaticAttributes.MC_CONTRACT_NUMBER));
                Log.warn(this, e); //sonar
            }
        }

        return true;
    }


    private Trade buildUnavailabilityTransfer(SecurityAllocationDTO allocationDto) throws Exception {

        int secCode = allocationDto.getProductId();
        Product security = getDSConnection().getRemoteProduct().getProduct(secCode);
        Book book = BOCache.getBook(getDSConnection(), allocationDto.getBookId());

        UnavailabilityTransfer product = new UnavailabilityTransfer();
        Trade trade = new Trade();
        trade.setProduct(product);
        trade.setAction(Action.NEW);

        trade.setBook(book);
        trade.setCounterParty(book.getLegalEntity());
        trade.setRole("ProcessingOrg");
        trade.setTraderName("NONE");
        trade.setSalesPerson("NONE");
        trade.setAccountNumber("NONE1");
        trade.setInventoryAgent("NONE1");
        trade.setUnavailabilityReason("AutomaticOptimization");
        trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, allocationDto.getCollateralConfigId());
        trade.addKeyword("Reason", "AutomaticOptimization");
        trade.setTradeDate(allocationDto.getTradeDate());
        trade.setSettleDate(allocationDto.getTradeDate().getJDate(TimeZone.getDefault()));
        trade.setQuantity(allocationDto.getQuantity());
        trade.setTradeCurrency(security.getCurrency());
        trade.setSettleCurrency(security.getCurrency());

        product.setStartDate(allocationDto.getTradeDate().getJDate(TimeZone.getDefault()));
        // should be open term
        product.setIsOpenTerm(true);
        product.setSecurity(security);
        product.setSubType("SECURITY");
        product.setQuantity(allocationDto.getQuantity());

        return trade;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean isValidInput(final Vector messages) {
        boolean ret = super.isValidInput(messages);
        return ret;
    }

//	@SuppressWarnings("rawtypes")
//	@Override
//	public Vector getDomainAttributes() {
//		return new Vector();
//	}
//
//	@SuppressWarnings({ "rawtypes" })
//	@Override
//	public Vector getAttributeDomain(final String attr, final Hashtable currentAttr) {
//		return new Vector();
//	}

    @Override
    public String getTaskInformation() {
        return "This Scheduled Task moves trades to UNPRICE: \n";
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
