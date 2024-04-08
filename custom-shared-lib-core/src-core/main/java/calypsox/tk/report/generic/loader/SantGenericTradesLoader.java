/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.Vector;

public class SantGenericTradesLoader extends SantAbstractLoader {

    public TradeArray loadTrades(final ReportTemplate template,
                                 final String agreementIds, final JDate valDate)
            throws RemoteException {

        final StringBuilder sqlFrom = new StringBuilder();
        final StringBuilder sqlWhere = new StringBuilder();

        buildMandatorySQLQuery(sqlFrom, sqlWhere, template, agreementIds,
                valDate);

        buildOptionalCptySQLQuery(sqlFrom, sqlWhere, template);

        buildOptionalFundSQLQuery(sqlFrom, sqlWhere, template);

        buildOptionalDealOwnerSQLQuery(sqlFrom, sqlWhere, template);

        buildOptionalTradeStatusSQLQuery(sqlFrom, sqlWhere, template);

        buildOptionalEconomicSectorSQLQuery(sqlFrom, sqlWhere, template);

        return DSConnection.getDefault().getRemoteTrade()
                .getTrades(sqlFrom.toString(), sqlWhere.toString(), null, null);
    }

    protected void buildMandatorySQLQuery(final StringBuilder sqlFrom,
                                          final StringBuilder sqlWhere, final ReportTemplate template,
                                          final String agreementIds, final JDate valDate) {

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
        sqlWhere.append("AND TRUNC(trade.trade_date_time) <= ");
        sqlWhere.append(Util.date2SQLString(valDate));

        // Mandatory Process date on maturity date
        sqlWhere.append(" AND( ");
        sqlWhere.append(" TRUNC(product_desc.maturity_date) >= ");
        sqlWhere.append(Util.date2SQLString(valDate));
        sqlWhere.append(" OR product_desc.maturity_date is null ");
        sqlWhere.append(" ) ");
    }

}
