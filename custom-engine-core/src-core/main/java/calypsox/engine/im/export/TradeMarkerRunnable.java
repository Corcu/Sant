package calypsox.engine.im.export;

import calypsox.util.SantCalypsoUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author adc
 */
public class TradeMarkerRunnable extends Thread {

    private static final String QEF_KEYWORD = "QEF_CORRELATION_ID";
    private static final Integer DEFAULT_THREADS = 10;

    protected boolean terminateMarker;
    protected String date;
    protected DSConnection con;
    protected ExecutorService exec;
    protected String correlationID;
    protected Stack<TradeArray> tradesA;
    protected TradeArray trades;


    public TradeMarkerRunnable() {
        this.con = DSConnection.getDefault();
        this.tradesA = new Stack<>();
        this.terminateMarker = false;
        this.exec = Executors.newFixedThreadPool(DEFAULT_THREADS);
    }

    @Override
    public void run() {
        Log.system(TradeMarkerRunnable.class.getName(), "Marker started - ThreadID= " + currentThread().getId());
        while (!terminateMarker) {
            if (!Util.isEmpty(this.tradesA) && !Util.isEmpty(this.tradesA.get(0))) {
                exec.submit(new Marker(this.tradesA.pop(), this.con, this.correlationID));
                this.tradesA.clear();
            }
        }
        Log.system(TradeMarkerRunnable.class.getName(), "Closing marker - ThreadID= " + currentThread().getId());
        shutdownAndAwaitTermination(exec);
    }

    /**
     * @param trades
     */
    public void addTrades(TradeArray trades) {
        this.tradesA.push(trades);
    }

    /**
     * @param date
     */
    public void setDate(String date) {
        if (!Util.isEmpty(date)) {
            correlationID = date;
        } else {
            correlationID = "CorrelationID Empty";
        }
    }

    /**
     * @param pool
     */
    public void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(2, TimeUnit.MINUTES)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(2, TimeUnit.MINUTES))
                    Log.error(this, "Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param terminate
     */
    public void setTerminateMarker(boolean terminate) {
        this.terminateMarker = terminate;
    }

    private class Marker implements Runnable {
        protected TradeArray tradestoMark;
        protected long[] tradesIds;
        protected DSConnection con;
        protected String date;

        public Marker(TradeArray trades, DSConnection con, String date) {
            try {
                this.tradestoMark = (TradeArray) trades.clone();
            } catch (CloneNotSupportedException e) {
                Log.error(this, "Cannot Clone " + e);
            }
            this.con = con;
            this.date = date;
        }

        @Override
        public void run() {
            if (!Util.isEmpty(this.tradestoMark)) {
                this.tradesIds = new long[this.tradestoMark.size()];
                addQefKeyword();
                try {
                    Log.system(TradeMarkerRunnable.class.getName(), "Saving marked trades (" + this.tradesIds.length + ") - ThreadID= " + currentThread().getId());
                    this.con.getRemoteTrade().saveTrades(new ExternalArray(this.tradestoMark.asList()));
                    Log.system(TradeMarkerRunnable.class.getName(), "SAVE COMPLETED (" + this.tradesIds.length + ") - ThreadID= " + currentThread().getId());
                } catch (Exception e) {
                    Log.error(this, "Error occurred while saving the trades " + e);
                    Log.system(TradeMarkerRunnable.class.getName(), "Trying to save again.. (" + this.tradesIds.length + ") - ThreadID= " + currentThread().getId());
                    reloadAndSaveTrades();
                }
            }
        }

        private TradeArray reloadAndSaveTrades() {

            try {
                Log.system(TradeMarkerRunnable.class.getName(), "Loading " + this.tradesIds.length + " trades...");
                this.tradestoMark = SantCalypsoUtilities.getInstance().getTradesWithTradeFilter(this.tradesIds);
                Log.system(TradeMarkerRunnable.class.getName(), "End load");
                if (!Util.isEmpty(this.tradestoMark)) {
                    addQefKeyword();
                    Log.system(TradeMarkerRunnable.class.getName(), "Start second save (last chance)");
                    this.con.getRemoteTrade().saveTrades(new ExternalArray(trades.asList()));
                    Log.system(TradeMarkerRunnable.class.getName(), "SECOND SAVE COMPLETED (" + this.tradesIds.length + ") - ThreadID= " + currentThread().getId());
                }
            } catch (CalypsoServiceException | InvalidClassException e) {
                Log.error(this, "Cannot reload or save trades in the second time (we are f...) - " + e);
            }
            return trades;
        }

        private void addQefKeyword() {
            for (int i = 0; i < this.tradestoMark.size(); i++) {
                this.tradesIds[i] = this.tradestoMark.get(i).getLongId();
                this.tradestoMark.get(i).addKeyword(QEF_KEYWORD, this.date);
                this.tradestoMark.get(i).setAction(Action.AMEND);
            }
        }
    }
}
