/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportTemplate;

import java.util.List;
import java.util.Map;

public class SantEnableThreadLoader<K, V> extends SantEnableThread<K, V> {

    private static final int SLEEP_TIME = 5000;

    private final List<Long> tradeIdsList;

    public SantEnableThreadLoader(ReportTemplate template, boolean enableThreading, List<Long> tradeIdsList) {
        super(template, enableThreading);
        this.tradeIdsList = tradeIdsList;
    }

    @Override
    protected void loadData() {
    }

    @Override
    protected Map<K, V> getDataMapFromDataList() {
        return super.getDataAsMap();
    }

    public List<Long> getTradeIdsList() {
        return this.tradeIdsList;
    }

    public static void waitUntilFinished(List<SantEnableThreadLoader<?, ?>> threadLoaders) {
        // wait until all threads have finished
        for (SantEnableThreadLoader<?, ?> threadLoader : threadLoaders) {
            if (!threadLoader.isAlive()) {
                continue;
            }
            while (threadLoader.isAlive()) {
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    Log.error(SantEnableThreadLoader.class.getName(), e);
                }
            }
        }
    }
}
