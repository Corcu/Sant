package calypsox.util;

import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * @author acd
 */
public class TradeSaverManager {
    private ExecutorService executorService;
    private ConcurrentLinkedQueue<Future<Long>> futures;
    private ConcurrentLinkedQueue<String> errorQueue;

    public TradeSaverManager(int threadNum) {
        executorService = Executors.newFixedThreadPool(threadNum);
        futures = new ConcurrentLinkedQueue<>();
        errorQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * @param trades List of trades to save
     * @param action Action to apply on trades
     */
    public void saveTrades(List<Trade> trades, Action action) {
        Optional.ofNullable(trades).orElse(new ArrayList<>()).parallelStream().forEach(trade -> {
            saveTrade(trade,action);
        });
    }

    /**
     * @param trade Trade to save
     * @param action Action to apply on trade
     */
    public void saveTrade(Trade trade, Action action) {
        Optional.ofNullable(trade).ifPresent(tr-> {
            TradeSaverThread saverThread = new TradeSaverThread(tr, action, errorQueue);
            futures.add(executorService.submit(saverThread));
        });
    }

    /**
     * Wait for procces all trades
     */
    public void waitForCompletion() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Log.error(this.getClass().getSimpleName(),"Error saving trades: " + e.getMessage());
        }
    }

    /**
     * @return List of saved trade ids
     */
    public List<Long> getSavedTradesIds() {
        List<Long> ids = new ArrayList<>();
        for (Future<Long> future : futures) {
            try {
                long id = future.get();
                ids.add(id);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return ids;
    }

    public List<String> getErrors() {
        return new ArrayList<>(errorQueue);
    }

}
