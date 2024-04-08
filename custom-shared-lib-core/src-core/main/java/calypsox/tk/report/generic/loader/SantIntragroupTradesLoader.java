/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import calypsox.tk.report.SantIntragroupPortfolioBreakdownReportTemplate;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

public class SantIntragroupTradesLoader extends SantEnableThread<Long, Trade> {
    public static final String INTRAGROUP_ATTRIBUTE_NAME = "INTRAGROUP";
    public static final String INTRAGROUP_ATTRIBUTE_YES_VALUE = "YES";

    private final String agreementIds;
    private final JDate valDate;

    public SantIntragroupTradesLoader(boolean enableThreading, ReportTemplate template, String agreementIds,
                                      JDate valDate) {
        super(template, enableThreading);
        this.agreementIds = agreementIds;
        this.valDate = valDate;

    }

    @SuppressWarnings("unchecked")
    @Override
    protected void loadData() {

        final StringBuilder sqlFrom = new StringBuilder();
        final StringBuilder sqlWhere = new StringBuilder();

        final Set<Integer> tradeIds = (Set<Integer>) this.template
                .get(SantIntragroupPortfolioBreakdownReportTemplate.TRADE_IDS_FOR_EMAIL_GATEWAY_NOTIF);
        if (!Util.isEmpty(tradeIds)) {
            buildSQLQueryForNotification(tradeIds, sqlFrom, sqlWhere);
            return;
        }

        final String intraLeStr = (String) this.template
                .get(SantIntragroupPortfolioBreakdownReportTemplate.INTRAGROUP_LE_ID);

        buildMandatorySQLQuery(sqlFrom, sqlWhere, this.template, this.agreementIds, this.valDate);

        // filter trades only getting those have intragroup cpty (or its cpty is on the cpty list set)
        buildIntragroupLeQuery(sqlFrom, sqlWhere, intraLeStr);

        SantSQLQueryUtil.buildOptionalDealOwnerSQLQuery(sqlFrom, sqlWhere, this.template);

        SantSQLQueryUtil.buildOptionalTradeStatusSQLQuery(sqlFrom, sqlWhere, this.template);

        String agrType = (String) this.template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        if (!Util.isEmpty(agrType)) {
            sqlFrom.append(" , trade_keyword tk_agr_type ");
            sqlWhere.append(" AND tk_agr_type.trade_id = trade.trade_id");
            sqlWhere.append(" AND tk_agr_type.keyword_name = 'CONTRACT_TYPE'");
            sqlWhere.append(" AND tk_agr_type.keyword_value = ");
            sqlWhere.append(Util.string2SQLString(agrType));
        }

        String instrument = (String) this.template.get(SantGenericTradeReportTemplate.INSTRUMENT_TYPE);
        if (!Util.isEmpty(instrument)) {
            sqlWhere.append(" AND (product_desc.product_sub_type=");
            sqlWhere.append(Util.string2SQLString(instrument));
            sqlWhere.append(" OR product_desc.product_type=");
            sqlWhere.append(Util.string2SQLString(instrument));
            sqlWhere.append(" )");
        }

        try {
            this.dataList.addAll(DSConnection.getDefault().getRemoteTrade()
                    .getTrades(sqlFrom.toString(), sqlWhere.toString(), null, null));
        } catch (RemoteException e) {
            Log.error(this, "Cannot Load Trades", e);
        }

    }

    @SuppressWarnings("unchecked")
    private void buildSQLQueryForNotification(Set<Integer> tradeIds, StringBuilder sqlFrom, StringBuilder sqlWhere) {

        final ArrayList<Integer> tradeIdsList = new ArrayList<>(tradeIds);
        final int SQL_IN_ITEM_COUNT = 999;
        int start = 0;
        try {
            for (int i = 0; i <= (tradeIdsList.size() / SQL_IN_ITEM_COUNT); i++) {
                StringBuilder localWhere = new StringBuilder(sqlWhere);
                int end = (i + 1) * SQL_IN_ITEM_COUNT;
                if (end > tradeIdsList.size()) {
                    end = tradeIdsList.size();
                }
                final List<Integer> subList = tradeIdsList.subList(start, end);
                start = end;
                localWhere.append(" trade.trade_id IN (");
                localWhere.append(Util.collectionToString(subList));
                localWhere.append(" ) ");

                this.dataList.addAll(DSConnection.getDefault().getRemoteTrade()
                        .getTrades(sqlFrom.toString(), localWhere.toString(), null, null));
            }
        } catch (RemoteException e) {
            Log.error(this, "Cannot Load Trades", e);
        }

    }

    private void buildMandatorySQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
                                        final ReportTemplate template, final String agreementIds, final JDate valDate) {

        sqlFrom.append(" trade, product_desc ");

        // Basic join
        sqlWhere.append(" trade.product_id=product_desc.product_id ");

        // Contracts join
        if (Util.isEmpty(agreementIds)) {
            sqlWhere.append("AND trade.internal_reference is not null ");
        } else {
            final Vector<Integer> v = Util.string2IntVector(agreementIds);
            if (v.size() <= ioSQL.MAX_ITEMS_IN_QUERY) {
                sqlWhere.append("AND trade.internal_reference in  ");
                sqlWhere.append(Util.collectionToSQLString(v));
            } else {
                sqlWhere.append("AND trade.internal_reference is not null ");
            }
        }

        // Process Date on trade date
        sqlWhere.append(" AND TRUNC(trade.trade_date_time) <= ");
        sqlWhere.append(Util.date2SQLString(valDate));

        // Mandatory Process date on maturity date
        sqlWhere.append(" AND( ");
        sqlWhere.append(" TRUNC(product_desc.maturity_date) >= ");
        sqlWhere.append(Util.date2SQLString(valDate));
        sqlWhere.append(" OR product_desc.maturity_date is null ");
        sqlWhere.append(" ) ");
    }

    private void buildIntragroupLeQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
                                        String intragroupLeIds) {

        Vector<String> intraLeIds = null;
        if (!Util.isEmpty(intragroupLeIds)) {
            intraLeIds = Util.string2Vector(intragroupLeIds);
            sqlFrom.append(", legal_entity");
            sqlWhere.append(" AND trade.cpty_id = legal_entity.legal_entity_id");
            sqlWhere.append(" AND legal_entity.legal_entity_id IN ");
            sqlWhere.append(Util.collectionToSQLString(intraLeIds));
        } else {
            sqlFrom.append(", legal_entity, le_attribute");
            sqlWhere.append(" AND trade.cpty_id = legal_entity.legal_entity_id");
            sqlWhere.append(" AND legal_entity.legal_entity_id = le_attribute.legal_entity_id");
            sqlWhere.append(" AND le_attribute.attribute_type = ");
            sqlWhere.append(Util.string2SQLString(INTRAGROUP_ATTRIBUTE_NAME));
            sqlWhere.append(" AND le_attribute.attribute_value = ");
            sqlWhere.append(Util.string2SQLString(INTRAGROUP_ATTRIBUTE_YES_VALUE));
        }

    }

    @Override
    protected Map<Long, Trade> getDataMapFromDataList() {
        for (Trade trade : this.dataList) {
            this.dataMap.put(trade.getLongId(), trade);
        }
        return this.dataMap;
    }

}
