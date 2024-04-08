/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import com.calypso.tk.core.JDate;

import java.util.*;

public class SantExcludedTradeLoader extends SantEnableThread<Long, Boolean> {

    private static final int NUM_THREAD = 10;

    private final Set<Long> tradeIds;
    private final JDate valDate;

    public SantExcludedTradeLoader(boolean enableThreading, Set<Long> tradeIds, JDate valDate) {
        super(enableThreading);
        this.tradeIds = tradeIds;
        this.valDate = valDate;

    }

    @Override
    protected void loadData() {
        if (this.tradeIds.size() == 0) {
            return;
        }

        final List<Long> tradeIdsList = new ArrayList<>(this.tradeIds);
        List<SantEnableThreadLoader<?, ?>> excludedTradeLoaderThreads = new ArrayList<>();

        int nbTradesPerThread = tradeIdsList.size() / NUM_THREAD;
        int idx = 0;
        for (int i = 0; i < NUM_THREAD; i++) {
            SantExcludedTradeThreadLoader excludedTradeLoaderThread = new SantExcludedTradeThreadLoader(true,
                    this.template, this.valDate, (i == (NUM_THREAD - 1)) ? tradeIdsList.subList(idx,
                    tradeIdsList.size()) : tradeIdsList.subList(idx, ((idx + nbTradesPerThread))));
            idx = idx + nbTradesPerThread;
            excludedTradeLoaderThreads.add(excludedTradeLoaderThread);

            excludedTradeLoaderThread.load();
        }

        SantEnableThreadLoader.waitUntilFinished(excludedTradeLoaderThreads);

        // get loaded data from threads
        for (SantEnableThreadLoader<?, ?> excludedTradeLoaderThread : excludedTradeLoaderThreads) {
            buildMap(excludedTradeLoaderThread.getDataAsList());
        }
    }

    @SuppressWarnings("rawtypes")
    private void buildMap(List results) {
        for (int i = 0; i < results.size(); i++) {
            Vector row = (Vector) results.get(i);
            long tradeId = Integer.valueOf((String) row.get(0));
            int excluded = Integer.valueOf((String) row.get(1));
            boolean isExcluded = excluded == 1 ? true : false;
            this.dataMap.put(tradeId, isExcluded);
        }
    }

    @Override
    protected Map<Long, Boolean> getDataMapFromDataList() {
        return null;
    }
}
