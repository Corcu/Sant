/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import calypsox.util.SantReportingUtil;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SantExcludedTradeThreadLoader extends SantEnableThreadLoader<Integer, Vector<?>> {

    private final JDate valDate;

    public SantExcludedTradeThreadLoader(boolean enableThreading, ReportTemplate template, JDate valDate,
                                         List<Long> tradeIdsList) {
        super(template, enableThreading, tradeIdsList);
        this.valDate = valDate;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected void loadData() {
        final int SQL_IN_ITEM_COUNT = 999;
        int start = 0;

        List<Long> tradeIdsList = getTradeIdsList();
        for (int i = 0; i <= (getTradeIdsList().size() / SQL_IN_ITEM_COUNT); i++) {
            int end = (i + 1) * SQL_IN_ITEM_COUNT;
            if (end > tradeIdsList.size()) {
                end = tradeIdsList.size();
            }
            final List<Long> subList = tradeIdsList.subList(start, end);
            start = end;
            StringBuilder sqlQuery = new StringBuilder();

            JDateFormat format = new JDateFormat("yyyy-MM-dd");
            String date = format.format(this.valDate);

            // MARGIN_CALL_ENTRIES.TRADE_DATETIME corresponds to the VALUE DATE on the collateral manager
            sqlQuery.append(" select MARGIN_CALL_DETAIL_ENTRIES.TRADE_ID, MARGIN_CALL_DETAIL_ENTRIES.IS_EXCLUDED from MARGIN_CALL_DETAIL_ENTRIES, MARGIN_CALL_ENTRIES ");
            sqlQuery.append("  where MARGIN_CALL_ENTRIES.ID = MARGIN_CALL_DETAIL_ENTRIES.MC_ENTRY_ID ");
            sqlQuery.append("  and  MARGIN_CALL_DETAIL_ENTRIES.IS_EXCLUDED = 1");
            sqlQuery.append("  and MARGIN_CALL_ENTRIES.TRADE_DATETIME >= TO_DATE ('" + date + " 00:00:00', 'YYYY-MM-DD HH24:MI:SS')");
            sqlQuery.append("  and MARGIN_CALL_ENTRIES.TRADE_DATETIME <= TO_DATE ('" + date + " 23:59:59', 'YYYY-MM-DD HH24:MI:SS')");

            // JRL Migration 14.4
            if (!subList.isEmpty()) {
                sqlQuery.append("  and MARGIN_CALL_DETAIL_ENTRIES.trade_id in ( ");
                sqlQuery.append(Util.collectionToString(subList));
                sqlQuery.append("  ) ");
            }

            try {
                Vector results = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).executeSelectSQL(
                        sqlQuery.toString());
                if (results.size() > 2) {
                    this.dataList.addAll(results.subList(2, results.size()));
                }
            } catch (Exception e) {
                Log.error(this, "Error loading details", e);
            }
        }
    }

    @Override
    protected Map<Integer, Vector<?>> getDataMapFromDataList() {
        return super.getDataMapFromDataList();
    }
}
