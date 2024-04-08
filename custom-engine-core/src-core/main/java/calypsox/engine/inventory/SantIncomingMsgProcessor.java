/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.inventory;

import calypsox.engine.inventory.SantPositionConstants.RESPONSES_CODES;
import calypsox.engine.inventory.util.PositionLogHelper;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.ExternalArray;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ThreadPool;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import static calypsox.engine.inventory.SantPositionConstants.RESPONSE_LINE_SEPARATOR;

/**
 * Process the message with the new positions lines. Is the core class, as recover the old positions, calculates the new
 * positions and saves the trades to impact the positions.
 *
 * @author Patrice Guerrido & Guillermo Solano
 * @version 2.0
 * @date 04/11/2013, added sdi and transfer checks
 */
public class SantIncomingMsgProcessor {

    /*
     * Constant that defines if trades are saved it using multithreading (true) or using sequential saving (false). For
     * the sequential, trades ids created are gathered and show in the Log
     */
    private final static Boolean MULTI_THREADING = false;

    /**
     * Positions parser from the message
     */
    private final SantPositionParser parser;

    /**
     * Message reference use for internally build message
     **/
    public String messageReference = null;

    public void setMessageReference(String messageReference) {
        this.messageReference = messageReference;
    }

    public String getMessageReference() {
        return this.messageReference;
    }

    /**
     * Unique Constructor
     *
     * @param true if using the testing mode
     */
    public SantIncomingMsgProcessor(final boolean testing) {

        this.parser = new SantPositionParser(testing);
    }

    /**
     * @param message to handle
     * @return true in the positions received have been processed correctly
     */
    public List<PositionLogHelper> handleIncomingMessage(final String message) {

        // parser incoming message (logs the status process)
        final List<PositionLogHelper> messLogTrack = this.parser.parseFile(message);

        // process the positions beans that are fine
        if (this.parser.hasNewPositionsRows()) {
            // this method has the "magic" to calculate each type of positions to be updated
            processPositions(messLogTrack);
        }

        // process result returned
        return messLogTrack;
    }

    /**
     * Process all the positions: bloqueos, actuales & teoricos in two ways: Present positions: impact the total (has to
     * match the message) Future positions: impacts
     *
     * @param messLogTrack
     * @return true
     */
    private boolean processPositions(final List<PositionLogHelper> messLogTrack) {

        final ComputeNewMovementsBuilder builder = new ComputeNewMovementsBuilder();

        if (!Util.isEmpty(getMessageReference())) {
            builder.setMessageReference(getMessageReference());
        }

        /* the following order is CRITICAL: 1? bloqueos, 2? actuals & 3? theoretical */
        // 1- process bloqueo (pledged)
        builder.build(this.parser.getBloqueoMap(), this.parser, messLogTrack);
        // 2- process actual (settled)
        builder.build(this.parser.getActualMap(), this.parser, messLogTrack);
        // 3- process theoretical (settled + pending)
        builder.build(this.parser.getTheorMap(), this.parser, messLogTrack);

        // 4-saves the trades
        savesTrades(builder, messLogTrack);

        // 5-check SDI instruction and Transfer settlement, gives grace time otherwise
        builder.checkSdiAndTransferSettlement(messLogTrack);

        // 6-Ensure that each position that has created a trade != 0 has change the DB value
        builder.checkPositionsHaveImpactedDB(messLogTrack);

        return true;
    }

    /**
     * Saves the trades into calypso
     *
     * @param builder
     * @param messLogTrack
     */
    private void savesTrades(final ComputeNewMovementsBuilder builder, final List<PositionLogHelper> messLogTrack) {

        if (MULTI_THREADING) {
            // saves using paralelization, no trade id logged
            saveTradesThreadPool(builder.getTrades());

        } else {
            // saves the trade linearly and attachs trade id to each pos in the log
            saveTradesLineal(builder.getTradesMap(), messLogTrack);
        }
    }

    /**
     * Saves the trades to update positions in a linear way.
     *
     * @param trades   to impact the positions
     * @param logTrack
     */
    private void saveTradesLineal(final Map<String, Trade> trades, final List<PositionLogHelper> logTrack) {

        for (Map.Entry<String, Trade> entry : trades.entrySet()) {

            String posBeanKey = entry.getKey();
            Trade trade = entry.getValue();

            try {
                final long id = DSConnection.getDefault().getRemoteTrade().save(trade);
                logTradeSavedId(logTrack, posBeanKey, id);
                Log.info(this, "SimpleTransfer GD trade saved id=" + id);

                if (id < 0) {
                    nackMessageAndLogError(logTrack, posBeanKey);
                }

            } catch (RemoteException e) {

                Log.error(this, "Error saving trade GestionDisponible Online " + "\n" + e.getMessage() + "\n");
                logTradeSavedId(logTrack, posBeanKey, -1);
                nackMessageAndLogError(logTrack, posBeanKey);
            }
        }
    }

    /**
     * Set log if there is a DB error and the Trade couldn't not be stored
     *
     * @param logTrack
     * @param posBeanKey
     */
    private void nackMessageAndLogError(List<PositionLogHelper> logTrack, String posBeanKey) {

        final PositionLogHelper rowLog = PositionLogHelper.getLog4PosBean(logTrack, posBeanKey);

        if (rowLog != null) {

            rowLog.addLineStatus("SAVING_TRADE", RESPONSES_CODES.ERR_DB, "Critic DB access - Request Resend");
        }
    }

    /**
     * Matches each SantPosition Bean with the trade id created
     *
     * @param logTrack
     * @param posBeanKey
     * @param id
     */
    private void logTradeSavedId(List<PositionLogHelper> logTrack, final String posBeanKey, long id) {

        for (PositionLogHelper logInfo : logTrack) {
            if (logInfo.getBean().getBeanKey().equals(posBeanKey)) {
                logInfo.setTradeId(id);
                return;
            }
        }
        Log.info(this, "Saved id=" + id + " not found for bean key=" + posBeanKey);
    }

    /**
     * Saves the trades to update positions, using a thread pool.
     *
     * @param trades to impact the positions
     */
    // MULTI-THREADING - NO TRADE ID CAPTURE FOR THE LOG
    private void saveTradesThreadPool(List<Trade> trades) {

        ThreadPool pool = new ThreadPool(5, this.getClass().getSimpleName());

        // Save trades by bunch of 100
        final int SQL_COUNT = 100;
        int start = 0;
        for (int i = 0; i <= (trades.size() / SQL_COUNT); i++) {
            int end = (i + 1) * SQL_COUNT;
            if (end > trades.size()) {
                end = trades.size();
            }
            final List<Trade> subList = trades.subList(start, end);
            start = end;
            pool.addJob(new Runnable() {

                @Override
                public void run() {
                    try {
                        // override thread method, paralelize the save of trades
                        DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(subList));
                    } catch (Exception e) {
                        Log.error(this, "Error saving trade GestionDisponible Online " + "\n" + e.getMessage() + "\n");

                    }
                }
            });
        }
    } // end multithreading trade saving

    /**
     * @param messLogTrack2
     * @return the message response
     */
    // EXAMPLE:
    // XS0414704451|DUMMY_BOOK|BBVD|00000000000000000000000000000000182|30/07/2013|2013-06-20-16.23.35.415046|00
    // XS0414704451|DUMMY_BOOK|BBVD|00000000000000000000000000000000182|30/07/2013|2013-06-20-18.03.25.677045|00
    // XS0414704451|DUMMY_BOOK|BBVD|00000000000000000000000000000000182|30/07/2013|2013-06-20-16.43.35.415046|00
    // XS0414704451|FO_BONOS|DUMMY_AGENT|1@DUMMY|30/07/2013|2013-06-20-18.03.26.155321|00
    // XS0414704451|FO_BONOS|DUMMY_AGENT|1@DUMMY|30/07/2013|2013-06-20-18.02.26.155321|00
    // XS0414704451|FO_BONOS|DUMMY_AGENT|1@DUMMY|30/07/2013|2013-06-20-18.02.26.155321|00
    // XS0414704451|FO_BONOS|DUMMY_AGENT|1@DUMMY|30/07/2013|2013-06-20-18.02.26.155321|00
    public String buildResponseMessage(final List<PositionLogHelper> logTrack) {

        final StringBuffer sb = new StringBuffer();

        for (PositionLogHelper line : logTrack) {

            sb.append(line.getBean().getRowResponse(line));
            sb.append(RESPONSE_LINE_SEPARATOR);
        }
        // remove the last line separator
        sb.delete(sb.length() - RESPONSE_LINE_SEPARATOR.length(), sb.length());

        return sb.toString();
    }

} // end CLASS