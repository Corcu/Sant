/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import calypsox.tk.report.SantTradeBrowserItem;
import com.calypso.tk.core.JDate;
import com.calypso.tk.marketdata.PLMark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SantGenericPLMarksLoader extends SantEnableThread<Integer, PLMark> {

    private static final int NUM_THREAD = 5;

    private final Set<JDate> valDateSet;
    private final Map<Long, SantTradeBrowserItem> tradeItemsMap;
    private final String pricingEnvName;

    public SantGenericPLMarksLoader(boolean enableThreading, Map<Long, SantTradeBrowserItem> tradeItemsMap,
                                    Set<JDate> valDateSet, String pricingEnvName) {
        super(enableThreading);
        this.tradeItemsMap = tradeItemsMap;
        this.valDateSet = valDateSet;
        this.pricingEnvName = pricingEnvName;
    }

    @Override
    public void loadData() {

        if (this.tradeItemsMap.size() == 0) {
            return;
        }

        List<SantEnableThreadLoader<?, ?>> plMarkLoaderThreads = new ArrayList<>();
        final List<Long> tradeIdsList = new ArrayList<>(this.tradeItemsMap.keySet());

        int nbTradesPerThread = tradeIdsList.size() / NUM_THREAD;
        int idx = 0;
        for (int i = 0; i < NUM_THREAD; i++) {
            SantGenericPLMarksThreadLoader plMarkLoaderThread = new SantGenericPLMarksThreadLoader(true, this.template,
                    this.valDateSet, (i == (NUM_THREAD - 1)) ? tradeIdsList.subList(idx, tradeIdsList.size())
                    : tradeIdsList.subList(idx, (idx + nbTradesPerThread)), this.pricingEnvName);
            idx = idx + nbTradesPerThread;
            plMarkLoaderThreads.add(plMarkLoaderThread);

            plMarkLoaderThread.load();
        }

        SantEnableThreadLoader.waitUntilFinished(plMarkLoaderThreads);

        // get loaded data from threads
        for (SantEnableThreadLoader<?, ?> plMarkLoaderThread : plMarkLoaderThreads) {
            buildMap(plMarkLoaderThread.getDataAsList());
        }
    }

    @SuppressWarnings("rawtypes")
    private void buildMap(List plMarks) {
        for (int i = 0; i < plMarks.size(); i++) {
            PLMark plMark = (PLMark) plMarks.get(i);
            // Set plMarks to SantTradeBrowserItem
            if (this.tradeItemsMap.get(plMark.getTradeLongId()) != null) {
                final SantTradeBrowserItem santTradeBrowserItem = this.tradeItemsMap.get(plMark.getTradeLongId());
                santTradeBrowserItem.addPLMark(plMark.getValDate(), plMark);
            }
        }
    }

    @Override
    protected Map<Integer, PLMark> getDataMapFromDataList() {
        // Not applicable
        return null;
    }

    @Override
    public List<PLMark> getDataAsList() {
        // Not applicable
        return null;
    }

}
