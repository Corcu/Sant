package calypsox.engine.accounting;

import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.BOAccounting;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.util.ProcessTaskUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.event.*;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.TaskArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by dmenendd on 27/10/2021.
 */
public class CreEngine extends com.calypso.engine.accounting.CreEngine{

    public CreEngine(DSConnection dsCon, String hostName, int esPort) {
        super(dsCon, hostName, esPort);
    }

    @Override
    public boolean filterEvents(BOAccounting accWEvent, PSEvent event, String productType, Trade trade, Vector exceptions, Vector noMatchingPostings, JDate today, Book book, Vector pmNoAccEvent, boolean isCancel) {
        boolean out = super.filterEvents(accWEvent, event, productType, trade, exceptions, noMatchingPostings, today, book, pmNoAccEvent, isCancel);
        if ("CA".equals(productType) && "CST_FAILED".equals(accWEvent.getEventType()) && !out){
            out = validateCSTFailedReversal(event);
        }
        return out;
    }

    private boolean validateCSTFailedReversal(PSEvent event) {
        boolean out = false;
        if (("CANCELED".equalsIgnoreCase(((PSEventTransfer)event).getStatus().getStatus()))
                && ("SETTLED".equalsIgnoreCase(((PSEventTransfer)event).getOldStatus().getStatus()))){
            out = true;
        }
        return out;
    }

    @Override
    protected Vector matchingEvents(Vector allEvents, Trade trade, PSEvent event, String productType, Vector exceptions) throws Exception {
        Vector existingaccEvents = null;
        Vector finalEvents = new Vector();
        if (event instanceof LiquidationEvent && event.getEventType().equals("LIQUIDATED_POSITION")) {
            return allEvents;
        } else {
            boolean doIt = true;
            boolean tradeValuationEvent;
            if (!(event instanceof PSEventTransfer)) {
                if (!this._newFirst && event instanceof PSEventTrade && ((PSEventTrade)event).getOldTradeDate() == null) {
                    doIt = false;
                }

                if (doIt) {
                    Status status = trade.getStatus();
                    Long id = trade.getLongId();
                    String str = "Trade " + id;
                    Integer previousVersionInt;
                    TaskArray exceptionTasks;
                    if (this.getDoingBatch()) {
                        if (status != null && status.equals(Status.S_CANCELED)) {
                            Log.debug(Log.TRACE, "Marking " + str + " as CANCELED.");
                            this._batchCanceledTradeMap.put(id, trade.getVersion());
                        } else {
                            previousVersionInt = (Integer)this._batchCanceledTradeMap.get(id);
                            if (previousVersionInt != null && trade.getVersion() < previousVersionInt) {
                                Log.debug(Log.TRACE, str + " is CANCELED.  " + event + " ignored.");
                                if (!(event instanceof PSEventTrade)) {
                                    this.addException(str + ": " + status + " event received after CANCELED event.  Event was ignored by CreEngine.", trade, event, exceptions);
                                } else {
                                    exceptionTasks = ProcessTaskUtil.handleOutOfSequenceEventException((PSEventTrade)event, "CRE_ENGINE");
                                    DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(exceptionTasks, 0L, (String)null);
                                }

                                return new Vector();
                            }
                        }
                    } else if (status != null && status.equals(Status.S_CANCELED)) {
                        synchronized(this._canceledTradeMap) {
                            Log.debug(Log.TRACE, "Marking " + str + " as CANCELED.");
                            Long cancelId = this._cancelledTradeIds[this._tradeIndex];
                            if (cancelId != null) {
                                this._canceledTradeMap.remove(cancelId);
                            }

                            this._cancelledTradeIds[this._tradeIndex] = id;
                            this._canceledTradeMap.put(id, trade.getVersion());
                            ++this._tradeIndex;
                            if (this._tradeIndex == 100) {
                                this._tradeIndex = 0;
                            }
                        }
                    } else {
                        previousVersionInt = (Integer)this._canceledTradeMap.get(id);
                        if (previousVersionInt != null && trade.getVersion() < previousVersionInt) {
                            Log.debug(Log.TRACE, str + " is CANCELED.  " + event + " ignored.");
                            if (!(event instanceof PSEventTrade)) {
                                this.addException(str + ": " + status + " event received after CANCELED event.  Event was ignored by CreEngine.", trade, event, exceptions);
                            } else {
                                exceptionTasks = ProcessTaskUtil.handleOutOfSequenceEventException((PSEventTrade)event, "CRE_ENGINE");
                                DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(exceptionTasks, 0L, (String)null);
                            }

                            return new Vector();
                        }
                    }

                    String eventType = this.getEventType(event);
                    boolean positionValuationEvent = false;
                    tradeValuationEvent = false;
                    if (eventType.equals("POSITION_VALUATION")) {
                        positionValuationEvent = true;
                    } else if (eventType.equals("TRADE_VALUATION") || eventType.equals("VALUATION_REVERSAL")) {
                        tradeValuationEvent = true;
                    }

                    try {
                        String where;
                        ArrayList bindVariables;
                        if (trade.getLongId() != 0L) {
                            if (tradeValuationEvent) {
                                Vector types = LocalCache.getDomainValues(this._ds, "accTriggerEventCaching");
                                if (Util.isEmpty(types)) {
                                    existingaccEvents = getBOCresByEventTypeSant(trade, "TRADE_VALUATION").toVector();
                                } else {
                                    existingaccEvents = getBOCresForMatchingSant(trade, "TRADE_VALUATION",productType).toVector();
                                }
                            } else if (event instanceof PSEventCollateralValuation) {
                                bindVariables = new ArrayList();
                                bindVariables.add(new CalypsoBindVariable(3000, trade.getLongId()));
                                bindVariables.add(new CalypsoBindVariable(4, ((PSEventCollateralValuation)event).getProductId()));
                                bindVariables.add(new CalypsoBindVariable(12, eventType));
                                where = "bo_cre.trade_id=? AND bo_cre.product_id=? AND bo_cre.matching=1";
                                where = where + " AND bo_cre.original_event= ?";
                                existingaccEvents = this._ds.getRemoteBO().getBOCres((String)null, where, bindVariables).toVector();
                            } else if (this._fullCancelB && event instanceof HedgeAccountingEvent && ((HedgeAccountingEvent)event).getReclassType().equals(Status.S_CANCELED.toString())) {
                                bindVariables = new ArrayList();
                                bindVariables.add(new CalypsoBindVariable(3000, trade.getLongId()));
                                where = "bo_cre.trade_id = ?";
                                existingaccEvents = this._ds.getRemoteBO().getBOCres((String)null, where, bindVariables).toVector();
                            } else if (event instanceof HedgeAccountingEvent && !((HedgeAccountingEvent)event).doMatching()) {
                                JDate valdate = ((HedgeAccountingEvent)event).getProcessingDate();
                                List<CalypsoBindVariable> bindHedgeVariables = new ArrayList();
                                bindHedgeVariables.add(new CalypsoBindVariable(3000, trade.getLongId()));
                                bindHedgeVariables.add(new CalypsoBindVariable(12, eventType));
                                bindHedgeVariables.add(new CalypsoBindVariable(3001, valdate));
                                bindHedgeVariables.add(new CalypsoBindVariable(3001, valdate.addDays(1)));
                                String whereHedge = "bo_cre.trade_id = ? and bo_cre.original_event = ?  and bo_cre.effective_date >= ? and bo_cre.effective_date < ?";
                                existingaccEvents = this._ds.getRemoteBO().getBOCres((String)null, whereHedge, bindHedgeVariables).toVector();
                            } else if (event instanceof PSEventPositionWAC) {
                                existingaccEvents = this._ds.getRemoteBO().getWACPositionCres(trade.getLongId(), ((PSEventPositionWAC)event).getPLMarkID()).toVector();
                            } else {
                                existingaccEvents = getBOCresForMatchingSant(trade,eventType,productType).toVector();
                            }
                        } else {
                            bindVariables = new ArrayList();
                            bindVariables.add(new CalypsoBindVariable(4, trade.getProduct().getId()));
                            bindVariables.add(new CalypsoBindVariable(4, trade.getBook().getId()));
                            where = "bo_cre.trade_id=0 AND bo_cre.product_id=? AND bo_cre.book_id=? AND bo_cre.matching=1";
                            if (positionValuationEvent) {
                                where = where + " AND bo_cre.original_event='POSITION_VALUATION' AND bo_cre.cre_type <> 'CRI'";
                            }

                            existingaccEvents = this._ds.getRemoteBO().getBOCres((String)null, where, bindVariables).toVector();
                        }
                    } catch (Exception var22) {
                        Log.error(this, "Failed to load cres for event " + event, var22);
                        throw var22;
                    }
                }
            } else {
                PSEventTransfer evp = (PSEventTransfer)event;
                Status status = evp.getStatus();
                BOTransfer transfer = evp.getBoTransfer();
                Long longId = transfer.getLongId();
                String str = "Transfer " + longId;
                tradeValuationEvent = false;
                Integer previousVersionInt;
                TaskArray exceptionTasks;
                if (this.getDoingBatch()) {
                    if (status != null && status.equals(Status.S_CANCELED)) {
                        Log.debug(Log.TRACE, "Marking " + str + " as CANCELED.");
                        this._batchCanceledTransferMap.put(longId, transfer.getVersion());
                    } else {
                        previousVersionInt = (Integer)this._batchCanceledTransferMap.get(longId);
                        if (previousVersionInt != null && transfer.getVersion() < previousVersionInt) {
                            Log.debug(Log.TRACE, str + " is CANCELED.  " + evp + " ignored.");
                            exceptionTasks = ProcessTaskUtil.handleOutOfSequenceEventException(evp, "CRE_ENGINE");
                            DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(exceptionTasks, 0L, (String)null);
                            return new Vector();
                        }

                        if (status != null && !status.equals(Status.S_CANCELED) && transfer.getStatus().equals(Status.S_CANCELED)) {
                            tradeValuationEvent = true;
                        }
                    }
                } else if (status != null && status.equals(Status.S_CANCELED)) {
                    synchronized(this._canceledTransferMap) {
                        Log.debug(Log.TRACE, "Marking " + str + " as CANCELED.");
                        Long cancelId = this._cancelledTransferIds[this._transferIndex];
                        if (cancelId != null) {
                            this._canceledTransferMap.remove(cancelId);
                        }

                        this._cancelledTransferIds[this._transferIndex] = longId;
                        this._canceledTransferMap.put(longId, transfer.getVersion());
                        ++this._transferIndex;
                        if (this._transferIndex == 100) {
                            this._transferIndex = 0;
                        }
                    }
                } else {
                    previousVersionInt = (Integer)this._canceledTransferMap.get(longId);
                    if (previousVersionInt != null && transfer.getVersion() < previousVersionInt) {
                        Log.debug(Log.TRACE, str + " is CANCELED.  " + evp + " ignored.");
                        exceptionTasks = ProcessTaskUtil.handleOutOfSequenceEventException(evp, "CRE_ENGINE");
                        DSConnection.getDefault().getRemoteBO().saveAndPublishTasks(exceptionTasks, 0L, (String)null);
                        return new Vector();
                    }
                }

                if (this._newFirst || evp.getOldAction() != null || tradeValuationEvent) {
                    try {
                        existingaccEvents = this._ds.getRemoteBO().getBOTransferCres(transfer.getLongId()).toVector();
                        if ((existingaccEvents == null || existingaccEvents.size() == 0) && tradeValuationEvent) {
                            this.addException(str + ": " + status + " event received with a status not CANCELED but the object is CANCELED.Nothing to be done.", trade, evp, exceptions);
                            return new Vector();
                        }
                    } catch (Exception var21) {
                        Log.error(this, "Failed to load cres for event " + event, var21);
                        throw var21;
                    }
                }
            }

            if (existingaccEvents == null) {
                return allEvents;
            } else {
                Vector noMatchingEvents = new Vector();
                this.filterEvents(existingaccEvents, event, productType, trade, exceptions, noMatchingEvents);

                for(int i = 0; i < existingaccEvents.size(); ++i) {
                    BOCre accWEvent = (BOCre)existingaccEvents.elementAt(i);
                    AccountingEventConfig eventConfig = this.getEventConfig(accWEvent.getEventType(), productType);
                    if (eventConfig != null) {
                        if (accWEvent.getOriginalEventType().equals("LIQUIDATED_POSITION")) {
                            if (!(event instanceof LiquidationEvent)) {
                                existingaccEvents.removeElementAt(i);
                                --i;
                                continue;
                            }
                        } else if (!eventConfig.getTriggeredEvents().contains(this.getEventType(event))) {
                            existingaccEvents.removeElementAt(i);
                            --i;
                            continue;
                        }

                        if (event instanceof PSEventFXPositionValuation && (accWEvent.getBookId() != ((PSEventFXPositionValuation)event).getBookId() || accWEvent.getProductId() != ((PSEventFXPositionValuation)event).getProduct().getId())) {
                            existingaccEvents.removeElementAt(i);
                            --i;
                        }
                    }
                }

                boolean versionOK = this.versionConsistencyCheck(trade, existingaccEvents, exceptions, event);
                if (versionOK) {
                    this.performNewMatching(allEvents, existingaccEvents, finalEvents, event, productType, trade);
                    if (noMatchingEvents != null && noMatchingEvents.size() > 0) {
                        finalEvents.addAll(noMatchingEvents);
                    }

                    return finalEvents;
                } else {
                    return new Vector();
                }
            }
        }
    }


    private CreArray getBOCresByEventTypeSant(Trade trade, String eventType){
        return loadBoCres(trade.getLongId(),eventType);
    }

    private CreArray getBOCresForMatchingSant(Trade trade, String eventType,String productType){
        CreArray returnArray = new CreArray();
        CreArray v = loadBoCres(trade.getLongId(),eventType);
        CreArray matchArray = new CreArray();

        int i;
        BOCre cre;
        for(i = 0; i < v.size(); ++i) {
            cre = (BOCre)v.get(i);
            if (cre.getMatchingProcess()) {
                matchArray.add(cre);
            }
        }

        v = matchArray;
        if (eventType == null) {
            return matchArray;
        } else {
            for(i = 0; i < v.size(); ++i) {
                cre = (BOCre)v.get(i);
                if (cre.getMatchingProcess() && isEventRequired(cre, eventType,productType)) {
                    returnArray.add(cre);
                }
            }

            return returnArray;
        }
    }

    private CreArray loadBoCres(Long tradeId, String eventType){
        List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(tradeId);
        String where = "bo_cre.trade_id=? AND bo_cre.matching=1";

        try {
            return this._ds.getRemoteBO().getBOCres((String)null, where, bindVariables);
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(),"Error loading Cres: " + e.getCause());
        }
        return new CreArray();
    }

    private boolean isEventRequired(BOCre cre, String triggerEvent,String productType){
        boolean required = false;
        String eventType = cre.getEventType();
        AccountingEventConfig eventConfig = this.getEventConfig(eventType, productType);
        if(null!=eventConfig){
            required = eventConfig.getTriggeredEvents().contains(triggerEvent.toUpperCase());
        }
        return required;
    }
}
