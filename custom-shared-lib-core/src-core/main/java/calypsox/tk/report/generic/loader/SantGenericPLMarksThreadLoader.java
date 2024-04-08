/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.report.ReportTemplate;

import java.util.*;

public class SantGenericPLMarksThreadLoader extends SantEnableThreadLoader<Long, PLMark> {

    private final Set<JDate> valDateSet;
    private final String pricingEnvName;

    public SantGenericPLMarksThreadLoader(boolean enableThreading, ReportTemplate template, Set<JDate> valDateSet,
                                          List<Long> tradeIdsList, String pricingEnvName) {
        super(template, enableThreading, tradeIdsList);
        this.valDateSet = valDateSet;
        this.pricingEnvName = pricingEnvName;
    }

    @Override
    protected void loadData() {
        final int SQL_IN_ITEM_COUNT = 999;
        int start = 0;

        List<Long> tradeIdsList = getTradeIdsList();
        for (int i = 0; i <= (tradeIdsList.size() / SQL_IN_ITEM_COUNT); i++) {
            int end = (i + 1) * SQL_IN_ITEM_COUNT;
            if (end > tradeIdsList.size()) {
                end = tradeIdsList.size();
            }
            final List<Long> subList = tradeIdsList.subList(start, end);
            start = end;

            // To retrieve PLMarks we need a map with TradeIds as keys as per
            // the API. So create one.
            Collection<Long> tradeIdsMap = new ArrayList<>();
            for (final Long trade_id : subList) {
                tradeIdsMap.add(trade_id);

            }

            for (JDate date : this.valDateSet) {
                try {
                    //JRL MIG 14.4
                    Collection<PLMark> plMarks = CollateralUtilities.retrievePLMarkBothTypes(tradeIdsMap, this.pricingEnvName, date);
                    this.dataList.addAll(plMarks);
                } catch (Exception e) {
                    Log.error(this, "Error loading PL Marks", e);
                }
            }
        }
    }

    @Override
    protected Map<Long, PLMark> getDataMapFromDataList() {
        return getDataAsMap();
    }
}
