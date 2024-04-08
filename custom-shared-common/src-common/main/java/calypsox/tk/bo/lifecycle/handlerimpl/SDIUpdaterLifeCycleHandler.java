package calypsox.tk.bo.lifecycle.handlerimpl;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.lifecycle.LifeCycleHandler;
import com.calypso.tk.bo.sql.TaskSQL;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.event.PSEventTask;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.event.sql.PSEventSQL;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.*;
import com.calypso.tk.util.AbstractObjectSaver;
import com.calypso.tk.util.BulkObjectSaver;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class SDIUpdaterLifeCycleHandler extends LifeCycleHandler {

    final static String SELECT_TRADE_FROM = "TRADE, PRODUCT_DESC";
    final static String SELECT_TRADE_WHERE = "TRADE.PRODUCT_ID=PRODUCT_DESC.PRODUCT_ID" +
            " AND CPTY_ID=?" +
            " AND TRADE.CUSTOM_XFRULE_B=0" +
            " AND PRODUCT_DESC.MATURITY_DATE >= ?";

    final static String BOOK_QUERY = "TRADE.BOOK_ID IN (SELECT BOOK_ID FROM BOOK WHERE LEGAL_ENTITY_ID = ?)";

    final static String EFFECTIVE_FROM_QUERY = "PRODUCT_DESC.MATURITY_DATE >= ?";

    final static String EFFECTIVE_TO_QUERY = "TRADE.SETTLEMENT_DATE <= ?";

    final static String PRODUCT_QUERY = "PRODUCT_DESC.PRODUCT_TYPE IN";

    final static String TRADE_TASK_QUERY = "EXISTS (SELECT TRADE_ID FROM BO_TASK WHERE BO_TASK.TRADE_ID = TRADE.TRADE_ID" +
            " AND EVENT_CLASS =? AND EVENT_TYPE = ? AND TASK_STATUS = 0)";

    @Override
    public Class<? extends PSEvent>[] subscribe() {
        return new Class[]{PSEventDomainChange.class};
    }

    @Override
    public BulkObjectSaver handleEvent(PSEvent psEvent) throws Exception {
        if (psEvent instanceof PSEventDomainChange) {
            PSEventDomainChange dc = (PSEventDomainChange) psEvent;
            List<SettleDeliveryInstruction> sdis = getSDIs(dc);

            return Util.isEmpty(sdis)?null:handleSDIUpdate(sdis);
        }
        return null;
    }

   private List<SettleDeliveryInstruction> getSDIs(PSEventDomainChange dc) throws CalypsoServiceException, CloneNotSupportedException {
        List<SettleDeliveryInstruction> sdis = new ArrayList<>();
        if (dc.getAction() == PSEventDomainChange.NEW || dc.getAction() == PSEventDomainChange.MODIFY) {
            SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), dc.getValueId());
            if (sdi == null) {
                Log.error(this, String.format("Settlement Instruction not found by id %d.", dc.getValueId()));
                return sdis;
            }
            sdis.add(sdi);
            if (dc.getAction() == PSEventDomainChange.MODIFY && sdi.getVersion() > 0) {
                SettleDeliveryInstruction oldSDI = getPreviousSDIVersion(sdi);
                if (oldSDI != null)
                    sdis.add(oldSDI);
            }
        } else if (dc.getAction() == PSEventDomainChange.REMOVE) {

            SettleDeliveryInstruction sdi = getRemoved(dc.getValueId());
            if (sdi != null)
                sdis.add(sdi);

        }
        return sdis;
    }

    private SettleDeliveryInstruction getRemoved(int sdiId) throws CalypsoServiceException {
        Vector<?> removed = DSConnection.getDefault().getRemoteTrade().getAudit("entity_id = ? AND entity_class_name = 'SettleDeliveryInstruction' AND entity_field_name = '_DELETE_'", "version_num", Collections.singletonList(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, sdiId)));
        return Util.isEmpty(removed) ? null : (SettleDeliveryInstruction)((AuditValue)removed.get(0)).getFieldObjectValue(); //(SettleDeliveryInstruction) ((AuditValue) removed.get(0)).getObject();
    }

    private SettleDeliveryInstruction getPreviousSDIVersion(SettleDeliveryInstruction sdi) throws CalypsoServiceException, CloneNotSupportedException {
        Vector<?> modified = DSConnection.getDefault().getRemoteTrade().getAudit("entity_id = ? AND entity_class_name= 'SettleDeliveryInstruction' and version_num = ?", "modif_date", Arrays.asList(
                new CalypsoBindVariable(CalypsoBindVariable.INTEGER, sdi.getId()),
                new CalypsoBindVariable(CalypsoBindVariable.INTEGER, sdi.getVersion() - 1)
        ));
        if (!Util.isEmpty(modified)) {
            SettleDeliveryInstruction sdiClone = (SettleDeliveryInstruction) sdi.clone();
            modified.forEach(a -> sdiClone.undo(DSConnection.getDefault(), (AuditValue) a));
            return sdiClone;
        }
        return null;
    }

    public BulkObjectSaver handleSDIUpdate(List<SettleDeliveryInstruction> sdis) throws Exception {
        Set<Trade> allTrades = new HashSet<>();
        for (SettleDeliveryInstruction sdi : sdis) {
            if (accept(sdi)) {

                //build tmp trade filter to find for trades with the SDI cpty

                TradeFilter tf = buildTradeFilter(sdi);

                // add additional TF
                String tfName = LocalCache.getDomainValueComment(DSConnection.getDefault(), this.getClass().getSimpleName() + ".config", "TradeFilter");

                tf.setParent(Util.isEmpty(tfName) ? null : BOCache.getTradeFilter(DSConnection.getDefault(), tfName));

                String sdfName = LocalCache.getDomainValueComment(DSConnection.getDefault(), this.getClass().getSimpleName() + ".config", "SDFilter");

                TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade().getTrades(tf, new JDatetime());

                //  Apply SD Filters and check if the SDI is applicable
                if (tradeArray != null && !tradeArray.isEmpty()) {
                    StaticDataFilter sdiFilter = Util.isEmpty(sdi.getStaticFilterSet()) ? null : BOCache.getStaticDataFilter(DSConnection.getDefault(), sdi.getStaticFilterSet());
                    StaticDataFilter filter = Util.isEmpty(sdfName) ? null : BOCache.getStaticDataFilter(DSConnection.getDefault(), sdfName);

                    List<Trade> trades = Arrays.stream(tradeArray.getTrades()).filter(t -> {
                        if (t == null || (filter != null && !filter.accept(t)) || (sdiFilter != null && !sdiFilter.accept(t)))
                            return false;

                        Vector<?> exceptions = new Vector<>();
                        Vector<?> transferRules = Util.isEmpty(t.getTransferRules()) ? BOProductHandler.buildTransferRules(t, exceptions, DSConnection.getDefault()) : t.getTransferRules();

                        if (!Util.isEmpty(exceptions) || Util.isEmpty(transferRules))
                            return false;

                        return transferRules.stream().noneMatch(r -> {
                            TradeTransferRule xferRule = (TradeTransferRule) r;
                            return BOTransfer.TBA.equals(xferRule.getPayerSDStatus()) || BOTransfer.TBA.equals(xferRule.getReceiverSDStatus()) || !xferRule.isValidForDate(xferRule.getSettleDate());

                        });
                    }).collect(Collectors.toList());

                    if (!Util.isEmpty(trades)) {
                        String domName = this.getClass().getSimpleName() + ".config";
                        String maxTradesStr = LocalCache.getDomainValueComment(DSConnection.getDefault(), domName, "MaxTrades");

                        if (!Util.isEmpty(maxTradesStr)) {
                            try {
                                int maxTrades = Integer.parseInt(maxTradesStr);

                                if (trades.size() > maxTrades) {
                                    Log.error(this, String.format("%d trades identified for update exceeds MaxTrades restriction of %d trades configured in %s domain. Ignoring SDI update event.", trades.size(), maxTrades, domName));
                                    return null;

                                }
                            } catch (NumberFormatException e) {
                                Log.error(this, String.format("Domain %s, value MaxTrades, invalid value in Domain Comment, integer expected", domName), e);
                                return null;
                            }
                        }

                        allTrades.addAll(trades);
                        /*
                        String actName = LocalCache.getDomainValueComment(DSConnection.getDefault(), this.getClass().getSimpleName() + ".config", "TradeAction");

                        final BulkTradeSaver saver = new BulkTradeSaver();
                        trades.forEach(t -> saver.add(Trade.class, t));
                        saver.setAction(Util.isEmpty(actName) ? Action.UPDATE : Action.valueOf(actName));
                        return saver; */
                    }
                }
            }
        }
        if (!Util.isEmpty(allTrades)) {
            final String actName = LocalCache.getDomainValueComment(DSConnection.getDefault(), this.getClass().getSimpleName() + ".config", "TradeAction");
            final BulkTradeSaver saver = new BulkTradeSaver();
            allTrades.forEach(t -> saver.add(Trade.class, t));
            saver.setAction(Util.isEmpty(actName) ? Action.UPDATE : Action.valueOf(actName));
            return saver;
        }
        return null;
    }

    private TradeFilter buildTradeFilter(SettleDeliveryInstruction sdi) {
        StringBuilder where = new StringBuilder(SELECT_TRADE_WHERE);
        List<CalypsoBindVariable> vars = new ArrayList<>();
        vars.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, sdi.getBeneficiaryId()));
        vars.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, JDate.getNow()));
        if (sdi.getProcessingOrgBasedId() > 0) {
            where.append(" AND ").append(BOOK_QUERY);
            vars.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, sdi.getProcessingOrgBasedId()));
        }

        if (sdi.getEffectiveFrom() != null) {
            where.append(" AND ").append(EFFECTIVE_FROM_QUERY);
            vars.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, sdi.getEffectiveFrom()));
        }

        if (sdi.getEffectiveTo() != null) {
            where.append(" AND ").append(EFFECTIVE_TO_QUERY);
            vars.add(new CalypsoBindVariable(CalypsoBindVariable.JDATE, sdi.getEffectiveTo()));
        }

        if (!Util.isEmpty(sdi.getProductList())) {
            where.append(" AND ").append(PRODUCT_QUERY).append(" ").append(expandProductList(sdi.getProductList()));
            //  vars.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, expandProductList(sdi.getProductList())));
        }

        String taskType = LocalCache.getDomainValueComment(DSConnection.getDefault(), this.getClass().getSimpleName() + ".config", "CheckOpenTaskType");

        if (!Util.isEmpty(taskType)) {
            where.append(" AND ").append(TRADE_TASK_QUERY);
            vars.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, taskType.startsWith("EX_") ? "Exception" : PSEventTrade.class.getSimpleName()));
            vars.add(new CalypsoBindVariable(CalypsoBindVariable.VARCHAR, taskType));
        }
        TradeFilter tf = new TradeFilter();
        tf.setSQLFromClause(SELECT_TRADE_FROM);
        tf.setSQLWhereClause(where.toString());
        tf.setSqlWhereClauseBindVaraibles(vars);

        return tf;
    }

    private String expandProductList(Vector<?> productList) {
        List<String> expanded = new ArrayList<>();

        productList.forEach(p -> {
            String prodType = (String) p;
            if (!Util.isEmpty(prodType)) {
                if (prodType.startsWith("G."))
                    expanded.addAll(LocalCache.getDomainValues(DSConnection.getDefault(), prodType));
                else
                    expanded.add(prodType);
            }
        });

        return Util.collectionToSQLString(expanded);
    }

    @Override
    public boolean accept(PSEvent psEvent) {
        if (psEvent instanceof PSEventDomainChange && ((PSEventDomainChange) psEvent).getType() == PSEventDomainChange.SETTLE_DELIVERY) {
            try {
                List<SettleDeliveryInstruction> sdis = getSDIs((PSEventDomainChange) psEvent);
                for (SettleDeliveryInstruction sdi : sdis) {
                    if (accept(sdi))
                        return true;
                }
            } catch (Exception e) {
                Log.error(this, e);
            }
        }
        return false;
    }

    private boolean accept(SettleDeliveryInstruction sdi) {
        return sdi != null && sdi.getPreferredB() && LegalEntity.COUNTERPARTY.equals(sdi.getRole()) && sdi.getBeneficiaryId() > 0;
    }

    public static class BulkTradeSaver extends AbstractObjectSaver {
        private static final long serialVersionUID = 5147104931263391657L;

        private Action action;

        @Override
        public Map<Class<?>, List<Long>> saveInDataServer(long eventId, String engineName, Connection con, Vector<PSEvent> events) throws Exception {
            Map<Class<?>, List<Long>> ids = new HashMap<>();
            List<Trade> tradeList = this.get(Trade.class);
            if (Util.isEmpty(tradeList)) {
                return ids;
            } else {
                ExternalArray array = new ExternalArray(tradeList.stream().map(t -> {
                    Trade clone = t.clone();
                    clone.setAction(action == null ? Action.UPDATE : action);
                    return clone;
                }).collect(Collectors.toList()));

                Vector<?> evs = TradeServerImpl.saveTrades(array, false, true, con);

                DataServer.publish(evs);

                Map<Class<?>, List<Long>> result = evs.stream().filter(e -> e instanceof Trade || e instanceof PSEventTrade).map(e -> e instanceof Trade ? ((Trade) e).getLongId() : ((PSEventTrade) e).getObjectId())
                        .collect(Collectors.groupingBy(i -> Trade.class));
                if (!Util.isEmpty(result.get(Trade.class))) {
                    TaskArray tasks = TaskSQL.getTasks(
                            String.format("trade_id in %s and event_class='Exception' and event_type ='EX_MISSING_SI' and task_status = 0",
                                    Util.collectionToSQLString(result.get(Trade.class))), con, Collections.emptyList());
                    TaskArray tasksToSave = new TaskArray();
                    Arrays.stream(tasks.getTasks()).filter(Objects::nonNull).map(t -> {
                        try {
                            Task cloneTask = (Task) t.clone();
                            cloneTask.setStatus(Task.COMPLETED);
                            return cloneTask;
                        } catch (CloneNotSupportedException e) {
                            Log.error(this, e);
                            return null;
                        }
                    }).filter(Objects::nonNull).forEach(tasksToSave::add);


                    if (TaskSQL.save(tasksToSave, con)) {
                        List<PSEvent> taskEvents = Arrays.stream(tasksToSave.getTasks()).map(t -> {
                            PSEventTask event = new PSEventTask();
                            event.setTask(t);
                            return event;
                        }).filter(e -> BackOfficeServerImpl.isEventRequired(e, con)).filter(e -> {
                            try {
                                return PSEventSQL.save(e, con);
                            } catch (PersistenceException ex) {
                                Log.error(BulkTradeSaver.class, ex);
                                return false;
                            }
                        }).collect(Collectors.toList());

                        DataServer.publish(taskEvents);

                    }
                }
                return result;
            }
        }

        private void setAction(Action act) {
            action = act;
        }
    }
}
