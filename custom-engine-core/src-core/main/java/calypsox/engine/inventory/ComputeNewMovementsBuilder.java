/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.inventory;

import calypsox.engine.inventory.SantPositionConstants.*;
import calypsox.engine.inventory.util.FetchCurrentPositionHelper;
import calypsox.engine.inventory.util.PositionLogHelper;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;
import com.calypso.tk.util.importexport.JDateComparator;

import java.rmi.RemoteException;
import java.util.*;

import static calypsox.engine.inventory.SantPositionConstants.*;

/**
 * Computes the new positions movements. Release 2.1: added Check Trade and Transfer status, added future treatment as
 * accumulative flows added check to ensure that the position value in DB has changed
 *
 * @author Patrice Guerrido & Guillermo Solano
 * @version 2.1, added Check Trade and Transfer status, added future treatment as accumulative flows added check to
 * ensure that the position value in DB has changed
 */
public class ComputeNewMovementsBuilder {

    /**
     * Static access to current DS
     */
    protected final static DSConnection ds = DSConnection.getDefault();
    /**
     * Aggregate movements per type (THEOR, ACTUAL, BLOQUEO)
     */
    private final AggregatedMovementWrapper aggregateMovementWrapper;
    /**
     * HelperToBuildTrades
     */
    private final BOPositionAdjustmentTradeBuilder builder;
    /**
     * Fetch the system positions
     */
    private final FetchCurrentPositionHelper helper;

    /**
     * Track Postions bean and trade builded from it. Key is the SantPositionBean.getBeanKey() method (unique)
     */
    private final Map<String, Trade> tradesMap;

    /**
     * Message reference use for internally build message
     **/
    public String messageReference = null;

    public void setMessageReference(String messageReference) {
        this.messageReference = messageReference;
        if (this.builder != null) {
            this.builder.setMessageReference(messageReference);
        }
    }

    public String getMessageReference() {
        return this.messageReference;
    }

    /**
     * Constructor
     */
    public ComputeNewMovementsBuilder() {

        this.aggregateMovementWrapper = new AggregatedMovementWrapper();
        this.builder = new BOPositionAdjustmentTradeBuilder();
        this.helper = new FetchCurrentPositionHelper();
        this.tradesMap = new LinkedHashMap<String, Trade>();
    }

    /**
     * Constructor for testing
     */
    // for unit test purpose only
    public ComputeNewMovementsBuilder(FetchCurrentPositionHelper helper) {

        this.aggregateMovementWrapper = new AggregatedMovementWrapper();
        this.builder = new BOPositionAdjustmentTradeBuilder();
        this.helper = helper;
        this.tradesMap = new LinkedHashMap<String, Trade>();
    }

    /**
     * @return trades to be inserted
     */
    public List<Trade> getTrades() {

        ArrayList<Trade> list = new ArrayList<Trade>(this.tradesMap.values());
        return list;
    }

    /**
     * @return the tradesMap
     */
    public Map<String, Trade> getTradesMap() {
        return this.tradesMap;
    }

    /**
     * Buils the Present and future Movements that will create the trades (to impact the positions). Present are
     * considered the new positions beans of today, future any day longer than today.
     *
     * @param map          will all the Positions beans ordered by Date
     * @param parser
     * @param messLogTrack
     */
    @SuppressWarnings({"unchecked"})
    public void build(final Map<JDate, SantPositionBean> map, final SantPositionParser parser,
                      final List<PositionLogHelper> messLogTrack) {

        if (map.isEmpty()) {
            return;
        }

        final TreeMap<JDate, SantPositionBean> treeMapPresent = new TreeMap<JDate, SantPositionBean>(
                new JDateComparator());
        final TreeMap<JDate, SantPositionBean> treeMapFuture = new TreeMap<JDate, SantPositionBean>(
                new JDateComparator());
        // take today from system
        final JDate today = JDate.getNow();

        for (Map.Entry<JDate, SantPositionBean> entry : map.entrySet()) {

            JDate dayKey = entry.getKey();
            SantPositionBean trade = entry.getValue();

            // past, no process it and log the reason
            if (isPastDay(today, dayKey)) {
                pastPositionDiscarded(trade, dayKey, messLogTrack);
            }
            // present positions
            else if (isSameDay(today, dayKey)) {
                treeMapPresent.put(dayKey, trade);

                // future positions
            } else {
                treeMapFuture.put(dayKey, trade);

            }
        }
        // goes to present, where it will count the total number of titles for each pos
        if (!treeMapPresent.isEmpty()) {
            buildPresent(treeMapPresent, messLogTrack);
        }
        // goes to future, it counts as accumulatives flows
        if (!treeMapFuture.isEmpty()) {
            buildFuture(treeMapFuture, parser, messLogTrack);
        }
    }

    /**
     * Buils the future Movements that will create the trade to impact the positions. These PositionTrades are based on
     * the accumulative flows/trades of the same day, for the same type. For example, In position today is 1000 Tomorrow
     * they send M1: 10 -> 1010 (adds trade of +10) Tomorrow they send M2: 15 -> 1015 (adds trade of +5) Tomorrow they
     * send M2: 100 -> 1100 (adds trade of +85)
     *
     * @param map          with THEO, BLOQUES & ACT positions, for future dates
     * @param parser
     * @param messLogTrack
     */
    private void buildFuture(final Map<JDate, SantPositionBean> map, SantPositionParser parser,
                             final List<PositionLogHelper> messLogTrack) {

        for (JDate date : map.keySet()) {

            final SantPositionBean bean = map.get(date);

            try {

                final double movementFlowSum = bean.getQuantity() - getSumTransferDays(bean);
                // sumOfPreviousMovement;
                bean.updateQuantity(movementFlowSum);

                // if this pos is ACTUAL, must rest BLOQUEOS
                if (bean.getPositionType().equals(ACTUAL)) {
                    // we have BLOQUEOS, rest to the total
                    if (!parser.getBloqueoMap().isEmpty()) {

                        final SantPositionBean beanBloqueo = parser.getBloqueoMap().get(date);
                        if (bean.equalWithDiffType(beanBloqueo)) {
                            final double recalculateBloqueo = bean.getQuantity() - beanBloqueo.getQuantity();
                            bean.updateQuantity(recalculateBloqueo);
                        }
                    }
                }
                // end calculating movement, build trade:
                buildTrade(bean);

            } catch (Exception e) {
                Log.error(this, e);
                // GSM: 24/01/2014 - added, in case DB critic error, request resend
                dbErrorBuildingFuture(bean, date, messLogTrack);
            }
        }
    }

    /**
     * Buils the Present Movements that will create the trades (to impact the positions) Only for today: If position
     * send is 1000 -> Calypso pos will be 1000 If position send again is 500 -> Calypso pos will be 500
     *
     * @param map          with the position beans for today
     * @param messLogTrack
     */
    private void buildPresent(final Map<JDate, SantPositionBean> map, final List<PositionLogHelper> messLogTrack) {

        JDate previousDate = null;
        for (JDate date : map.keySet()) {

            JDate currentDate = null;
            SantPositionBean bean = map.get(date);

            double sumOfPreviousMovement = 0.0;
            currentDate = bean.getPositionDate();
            if (previousDate != null) {
                sumOfPreviousMovement = getAggregatedMovement(previousDate, bean.getPositionKey());
            }
            try {

                /** changes under construction **/
                // SantPositionBean beanActual = bean.clone();
                // beanActual.getPositionKey().replace(THEORETICAL, ACTUAL);
                // this.helper.get(beanActual);
                // Double actualPos = beanActual.getQuantity();
                /** changes under construction **/

                double currentMovement = bean.getQuantity() - this.helper.get(bean) - sumOfPreviousMovement;

                if (THEORETICAL.equals(bean.getPositionType())) {
                    SantPositionBean beanActual = bean.clone();
                    beanActual.setPositionType(ACTUAL_MAPPING);
                    double currentActualPos = this.helper.get(beanActual);
                    // As THEO is NOT SETTLED
                    // DELTA NOT SETTLED = new NOT SETTLED - current THEORETICAL + current ACTUAL
                    currentMovement = currentMovement + currentActualPos;
                } else if (ACTUAL.equals(bean.getPositionType())) {
                    String key = bean.getPositionKey().replace(ACTUAL, BLOQUEO);
                    double currentBloqueo = getAggregatedMovement(currentDate, key);
                    currentMovement = currentMovement - currentBloqueo;
                }

                addAndAggregateMovement(previousDate, currentDate, bean.getPositionKey(), currentMovement);
                bean.updateQuantity(currentMovement);
                // builds the trade that will impact the position
                buildTrade(bean);

            } catch (Exception e) {
                Log.error(this, e);

            }
            previousDate = currentDate;
        }
    }

    /**
     * Adds a new Trade that will be saved later.
     *
     * @param bean
     */
    private void buildTrade(final SantPositionBean bean) {

        // if bean quantity != 0, create trade
        if (bean.getQuantity() != 0) {

            // stores the original position value
            saveOriginalPositionQuantity(bean);

            // build the trade
            final Trade newTrade = this.builder.build(bean);

            // add the trade to the map of trades that will be saved later.
            this.tradesMap.put(bean.getBeanKey(), newTrade);
        }
    }

    /**
     * @param date
     * @param key
     * @return the aggregated movement
     */
    private double getAggregatedMovement(final JDate date, final String key) {
        return this.aggregateMovementWrapper.getMovement(date, key);
    }

    /**
     * Adds and aggregated to another movement
     *
     * @param previousDate
     * @param currentDate
     * @param key
     * @param movement
     */
    private void addAndAggregateMovement(final JDate previousDate, final JDate currentDate, final String key,
                                         final double movement) {

        this.aggregateMovementWrapper.addMovement(previousDate, currentDate, key, movement);
    }

    /**
     * @param today
     * @param dayKey
     * @return true is today is previous to dayKey
     */
    private boolean isPastDay(final JDate today, final JDate dayKey) {

        return dayKey.before(today);/*
         * (dayKey.getYear() < today.getYear()) || (dayKey.getMonth() < today.getMonth()) ||
         * (dayKey.getDayOfMonth() < today.getDayOfMonth())
         */
    }

    /**
     * @param today
     * @param dayKey
     * @return true if the day, month and year are the same
     */
    private boolean isSameDay(final JDate today, final JDate dayKey) {
        return today.equals(dayKey);
        /*
         * return (today.getYear() == dayKey.getYear()) && (today.getMonth() == dayKey.getMonth()) &&
         * (today.getDayOfMonth() == dayKey.getDayOfMonth());
         */
    }

    /*
     * Logs and request resend when receiving a past position. This should not occur NEVER, however it is controlled in
     * this point
     */
    private void pastPositionDiscarded(final SantPositionBean trade, final JDate dayKey,
                                       final List<PositionLogHelper> messLogTrack) {

        criticErrorResendPosition(trade, dayKey, messLogTrack, "PAST_TRADE", "Received PAST position - Request Resend");
    }

    /*
     * Logs and request resend when receiving a past position. This should not occur NEVER, however it is controlled in
     * this point
     */
    private void dbErrorBuildingFuture(final SantPositionBean trade, final JDate dayKey,
                                       final List<PositionLogHelper> messLogTrack) {

        criticErrorResendPosition(trade, dayKey, messLogTrack, "BUILDING_PRESENT_TRADE",
                "Critic DB error while building future Trade - Request Resend");
    }

    /*
     * Logs and request resend when receiving a past position. This should not occur NEVER, however it is controlled in
     * this point
     */
    private void criticErrorResendPosition(final SantPositionBean trade, final JDate dayKey,
                                           final List<PositionLogHelper> messLogTrack, final String component, final String errorCause) {

        final PositionLogHelper rowLog = PositionLogHelper.getLog4PosBean(messLogTrack, trade);

        if (rowLog != null) {
            rowLog.addLineStatus(component, RESPONSES_CODES.ERR_DB, errorCause);
        }
    }

    /**
     * Gets the transfers of the position for that type (ACTUAL, THEORICAL or PLEDGE) for the concrete day.
     *
     * @param bean , the position
     * @return the transfers sum for the position
     */
    protected Double getSumTransferDays(SantPositionBean bean) {

        final String where = buildBOTransferQuery(bean);
        Double sum = 0.0;
        try {
            final TransferArray transferList = ds.getRemoteBO().getBOTransfers(where, null);

            if (transferList.isEmpty()) {
                return sum;
            }
            Iterator<BOTransfer> ite = transferList.iterator();

            while (ite.hasNext()) {
                final BOTransfer bot = ite.next();
                double settleAmount = bot.getRealSettlementAmount();
                // if PAY, change sign
                if (bot.getPayReceive().equals("PAY")) {
                    settleAmount = -settleAmount;
                }

                sum += settleAmount;
            }

        } catch (RemoteException e) {
            Log.error(this, "ERROR: DS severe error - " + e.getLocalizedMessage());
        }
        return sum;
    }

    /**
     * Builds the Transfer query (ACTUAL, THEORICAL or PLEDGE) for the concrete day.
     *
     * @param bean
     * @return a String with the appropiate where clause
     */
    /**
     * Builds the Transfer query (ACTUAL, THEORICAL or PLEDGE) for the concrete day.
     *
     * @param bean
     * @return a String with the appropiate where clause
     */
    private String buildBOTransferQuery(final SantPositionBean bean) {

        StringBuilder sb = new StringBuilder("(bo_transfer.product_id = ");
        sb.append(bean.getSecurity().getId()).append(") AND ");
        sb.append("(bo_transfer.transfer_type = 'SECURITY') AND ");
        sb.append("(bo_transfer.trade_date = ").append(Util.date2SQLString(bean.getPositionDate())).append(") AND ");
        sb.append("(bo_transfer.book_id = ").append(bean.getBook().getId()).append(") AND ");
        sb.append("(bo_transfer.int_agent_le_id = ").append(bean.getAgent().getId()).append(") AND ");
        sb.append("(bo_transfer.gl_account_id = ").append(bean.getAccount().getId()).append(") AND ");
        // for bloqueos, look for pledge transfers
        if (bean.getPositionType().startsWith(BLOQUEO_MAPPING)) {
            sb.append("(bo_transfer.product_family = 'Pledge') AND ");
            // for actual position, only consider SETTLED
        } else if (bean.getPositionType().startsWith(ACTUAL_MAPPING)) {
            sb.append("(bo_transfer.transfer_status = 'SETTLED') AND ");
        } else if (bean.getPositionType().startsWith(THEORETICAL)) {
            sb.append("(bo_transfer.transfer_status != 'SETTLED') AND ");
        }
        sb.append("(bo_transfer.transfer_status != 'CANCELED') AND ");
        sb.append("(bo_transfer.netted_transfer = 0 )");

        return sb.toString();
    }

    /**
     * Stores the original position, so later we can "ask" the DB to confirm that the position has change
     * <p>
     * /** Adds a new Trade that will be saved later.
     *
     * @param bean of the position
     */
    private void saveOriginalPositionQuantity(SantPositionBean bean) {

        if (bean.getQuantity() != 0) {
            Double originalPos = -1.0;

            // take the original position to be updated (so later we can compare that it has actually change)
            try {
                if (this.helper != null) {
                    originalPos = this.helper.get(bean);
                }
            } catch (Exception e) {

                Log.error(this, "Error recovering DB position" + e.getLocalizedMessage());
            }

            // store it in the bean
            bean.setOriginalPosition(originalPos);
            // build the trade
            final Trade newTrade = this.builder.build(bean);
            // add the trade to the map of trades that will be saved later.
            this.tradesMap.put(bean.getBeanKey(), newTrade);
        }
    }

    /**
     * Ensures that the trade saved has transit to verify (if not probably SDI is not correct). Afterwards checks the
     * Transfer has been created and therefore the positions have impacted properly. Gives some grace time to Calypso
     * transfer & Inventory engines to process the Transfers associated to the incoming positions.
     *
     * @param messLogTrack
     */
    public void checkSdiAndTransferSettlement(final List<PositionLogHelper> messLogTrack) {

        if ((messLogTrack == null) || messLogTrack.isEmpty() || (getTradesMap() == null) || getTradesMap().isEmpty()) {
            // no trades have been saved.
            return;
        }

        for (PositionLogHelper tradeTrack : messLogTrack) {

            // trade has been created & saved
            if (tradeTrack.getTradeId() > 0) {

                try {

                    final long tradeId = tradeTrack.getTradeId();
                    Trade trade = ds.getRemoteTrade().getTrade(tradeId);

                    if (trade != null) {

                        // check trade status are ok
                        if (verifyTradeStatus(trade)) {

                            tradeTrack.addLineStatus("ERROR_FIELDS_NUMBER", RESPONSES_CODES.WAR_SDI_NOT_CONFIGURED,
                                    RESPONSES_CODES.WAR_SDI_NOT_CONFIGURED.getResponseText() + ". Trade=" + tradeId);
                            continue;
                        }

                        // wait till Transfers are committed and confirmed, gives grace time if needed
                        final BOTransfer boTransf = getBOTransfer(ds, tradeId);

                        // KEY: try to give grace time so the transfer is VERIFIED or SETTLED, impacting the position
                        if (!transferHasBeenSettled(boTransf, tradeTrack)) {

                            Log.error(this, "Transfer id=" + boTransf.getLongId() + " for Trade id=" + trade.getLongId()
                                    + " has not been SETTLED");
                            continue;
                        }
                    } // end if

                } catch (Exception e) {
                    Log.error(this, "ERROR: DS severe error - " + e.getLocalizedMessage());
                }
            }
        } // end for
    }

    /**
     * Retrieves the transfer associated to the trade. Forces to wait until the transfer has been created by the
     * TransferEngine
     *
     * @param ds
     * @param tradeId
     * @return transfer associated to the trade.
     * @throws RemoteException
     */
    private BOTransfer getBOTransfer(final DSConnection ds, final long tradeId) throws RemoteException {

        TransferArray transferList = ds.getRemoteBO().getBOTransfers(tradeId);
        // give some time to retrieve the transfer
        Integer attemp = 1;
        final Integer maxAttemps = 10;
        do {
            transferList = ds.getRemoteBO().getBOTransfers(tradeId);
            // Transfer has impacted, OK.
            if ((transferList != null) && !transferList.isEmpty()) {
                break;
            }
            // othercase give grace time and try again
            giveGraceTime(50 * attemp);
            attemp++;

        } while (attemp <= maxAttemps);

        if ((transferList == null) || transferList.isEmpty()) {
            Log.error(this, "ERROR: Transfer for trade " + tradeId + " does NOT exist. ");
            return null;
        }

        // must be alway relation 1 to 1.
        if (transferList.size() > 1) {
            Log.error(this,
                    "ERROR: SimpleTransfer trade ID is associated with more than one GestionDisponible Transfer.");
            return null;
        }
        // transfer found
        return transferList.firstElement();
    }

    /**
     * Forces to wait until the transfer has been created by the TransferEngine and the Inventory Engine has actually
     * settle the position in database.
     *
     * @param boTransf
     * @param tradeTrack
     * @return the transfer has transitted to verify (position was settled).
     */
    private boolean transferHasBeenSettled(final BOTransfer boTransf, final PositionLogHelper tradeTrack) {

        if (boTransf == null) {
            Log.error(this, "ERROR: BO Transfer cannot be null");
            return false;
        }

        if (boTransf.getStatus().getStatus().equals(Status.CANCELED)) {
            return true;
        }

        Integer attemp = 1;
        final Integer maxAttemps = 5;

        do {
            // GSM: 22/11/13. Ensure that:
            // THEORICAL reachs verified state
            // PLEDGE AND ACTUAL reach settled state
            final Status status = boTransf.getStatus();
            final String posType = tradeTrack.getBean().getPositionType();

            if (posType.equals(THEORETICAL)) {
                if (status.equals(Status.VERIFIED)) {
                    return true;
                }

            } else if (posType.equals(ACTUAL) || posType.equals(BLOQUEO)) {
                if (status.equals(Status.SETTLED)) {
                    return true;
                }
            }

            // othercase give grace time and try again
            giveGraceTime(150 * attemp);
            attemp++;

        } while (attemp <= maxAttemps);

        // this should not happen, it will be a problem.
        return false;
    }

    /**
     * Ensures that each position read has actually change the DB and
     *
     * @param messLogTrack
     */
    public void checkPositionsHaveImpactedDB(final List<PositionLogHelper> messLogTrack) {

        if ((messLogTrack == null) || messLogTrack.isEmpty() || (getTradesMap() == null) || getTradesMap().isEmpty()) {
            // no trades have been saved.
            return;
        }

        for (PositionLogHelper tradeTrack : messLogTrack) {

            // trade has been created & saved
            if (tradeTrack.getTradeId() > 0) {

                try {
                    // recover the trade that has been saved
                    final Trade trade = ds.getRemoteTrade().getTrade(tradeTrack.getTradeId());

                    Integer attemp = 1;
                    final Integer maxAttemps = 10;

                    if ((trade != null) && (this.helper != null)) {

                        // read the Bean
                        final SantPositionBean posBean = tradeTrack.getBean();

                        do {

                            final Double originalDBPosition = posBean.getOriginalPosition();
                            // should not happen
                            if (originalDBPosition == null) {
                                Log.error(this,
                                        "ERROR: Original DB position has NOT changed in checkPositionsHaveImpactedDB(..)");
                                break;
                            }

                            // GSM: 04/11/2014. Log info to know the position recoved from DB
                            final String type = posBean.getPositionType();
                            Log.info(this, "Retrieved " + type + " position -> Value " + originalDBPosition);
                            Log.info(this, "Trade " + trade.getLongId() + " movement -> Value " + posBean.getQuantity());

                            // get updated position, considering the impact of actuales or bloqueos in the same message
                            Double updatedPosition = calculateUpdatedPosition(posBean);
                            // get the difference
                            final Double difference = updatedPosition - originalDBPosition;

                            // if position has impacted in DB, movement must be the same than the difference
                            if (difference.equals(posBean.getQuantity())) {
                                break;
                            }

                            // otherwise give grace time and try again
                            giveGraceTime(50 * attemp);
                            attemp++;

                        } while (attemp <= maxAttemps);

                    } else {
                        // this shouldn't happen
                        Log.error(this,
                                "ERROR: Trade is null or Positions is null  in checkPositionsHaveImpactedDB(..)");
                    }

                } catch (Exception e) {
                    Log.error(this,
                            "ERROR: DS severe error in checkPositionsHaveImpactedDB(..) -> " + e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Returns the update position from DB, recalculating it if in the same message we had bloqueos or actuales
     *
     * @param posBean
     * @return
     * @throws Exception
     */
    private Double calculateUpdatedPosition(SantPositionBean posBean) throws Exception {

        // position date
        final JDate currentDate = posBean.getPositionDate();

        // call to DB, retrieve position
        Double updatedPosition = this.helper.get(posBean);

        // calculate any position in the same message that should impact current
        if (THEORETICAL.equals(posBean.getPositionType())) {

            String key = posBean.getPositionKey().replace(THEORETICAL, ACTUAL);
            double currentActual = getAggregatedMovement(currentDate, key);
            key = key.replace(ACTUAL, BLOQUEO);
            double currentBloqueo = getAggregatedMovement(currentDate, key);
            updatedPosition = updatedPosition - currentActual - currentBloqueo;

        } else if (ACTUAL.equals(posBean.getPositionType())) {
            String key = posBean.getPositionKey().replace(ACTUAL, BLOQUEO);
            double currentBloqueo = getAggregatedMovement(currentDate, key);
            updatedPosition = updatedPosition - currentBloqueo;
        }

        return updatedPosition;
    }

    /**
     * @param trade
     * @return true if the trade state is VERIFIED
     */
    private boolean verifyTradeStatus(Trade trade) {

        if (trade == null) {
            Log.error(this, "Trade parameter cannot be null");
            return true;
        }

        if (trade.getStatus().getStatus().equals(Status.VERIFIED)) {
            return false;
        } else {
            // grace time
            giveGraceTime(50);
            if (trade.getStatus().getStatus().equals(Status.VERIFIED)) {
                return false;
            }
        }
        // return false, SDI not found or might be incorrect
        return true;
    }

    /*
     * Sleeps the GD positions engines
     */
    public void giveGraceTime(final long milis) {

        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            Log.error(this, "Thread error: " + e.getLocalizedMessage());
        }
    }

}
