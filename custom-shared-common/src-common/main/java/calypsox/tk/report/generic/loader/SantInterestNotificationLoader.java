/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import calypsox.tk.report.SantInterestNotificationReportTemplate;
import calypsox.tk.report.generic.loader.interestnotif.SantInterestNotificationEntry;
import calypsox.tk.report.generic.loader.interestnotif.SantInterestNotificationEntryBuilder;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

public class SantInterestNotificationLoader {

    public List<SantInterestNotificationEntry> load(final ReportTemplate template, final JDate processStartDate, final JDate valDate,
                                                    final JDate processEndDate) throws Exception {

        final TradeArray trades = loadTrades(template, processStartDate, processEndDate);

        final SantInterestNotificationEntryBuilder builder = new SantInterestNotificationEntryBuilder();
        builder.build(trades, processStartDate, processEndDate, valDate, template);
        return builder.getEntries();
    }

    protected TradeArray loadTrades(final ReportTemplate template, final JDate processStartDate,
                                  final JDate processEndDate) throws Exception {

        final StringBuilder sqlFrom = new StringBuilder();
        final StringBuilder sqlWhere = new StringBuilder();

        buildSQLQuery(sqlFrom, sqlWhere, template, processStartDate, processEndDate);

        try {
            return DSConnection.getDefault().getRemoteTrade()
                    .getTrades(sqlFrom.toString(), sqlWhere.toString(), "", true, null);

        } catch (final RemoteException e) {
            Log.error(this, "Cannot load trades", e);
            throw e;
        }
    }

    protected void buildSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
                                 final ReportTemplate template, final JDate processStartDate, final JDate processEndDate) throws Exception {

        // Default
        sqlFrom.append(" product_int_bearing, acc_account ");

        sqlWhere.append(" trade.product_id = product_int_bearing.product_id ");
        sqlWhere.append(" AND acc_account.acc_account_id = product_int_bearing.account_id ");
        sqlWhere.append(" AND trade.trade_status = 'VERIFIED' ");

        // Start managing date
        sqlWhere.append(" AND (");
        // Process Start date
        sqlWhere.append("(").append(Util.date2SQLString(processStartDate))
                .append(">= TRUNC(product_int_bearing.start_date)").append(" AND ")
                .append(Util.date2SQLString(processEndDate)).append(" <= TRUNC(product_int_bearing.end_date)")
                .append(")");
        sqlWhere.append(" OR ");
        // Process End date
        sqlWhere.append("(").append(Util.date2SQLString(processStartDate))
                .append("<= TRUNC(product_int_bearing.end_date)").append(" AND ")
                .append(Util.date2SQLString(processEndDate)).append(" >= TRUNC(product_int_bearing.start_date)")
                .append(")");

        sqlWhere.append(" ) ");
        // End managing date

        // Call Account Ids
        final String callAccountIds = (String) template.get(SantInterestNotificationReportTemplate.CALL_ACCOUNT_ID);
        if (!Util.isEmpty(callAccountIds)) {
            final Vector<String> accIds = Util.string2Vector(callAccountIds);
            sqlWhere.append(" AND acc_account.acc_account_id in ");
            sqlWhere.append(Util.collectionToSQLString(accIds));
        }

        // Agreement Ids
        final String agreementIds = (String) template.get(SantInterestNotificationReportTemplate.AGREEMENT_ID);
        if (!Util.isEmpty(agreementIds)) {
            final Vector<String> agrIds = Util.string2Vector(agreementIds);

            sqlFrom.append(" ,account_trans ");
            sqlWhere.append(" AND acc_account.acc_account_id = account_trans.account_id ");
            sqlWhere.append(" AND (");
            sqlWhere.append(" account_trans.attribute = 'MARGIN_CALL_CONTRACT' ");
            sqlWhere.append(" AND account_trans.value in ");
            sqlWhere.append(Util.collectionToSQLString(agrIds));
            sqlWhere.append(" )");
        }

    }
}
