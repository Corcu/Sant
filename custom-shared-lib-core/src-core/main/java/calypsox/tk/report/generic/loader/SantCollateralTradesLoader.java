/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Vector;

public class SantCollateralTradesLoader extends SantGenericTradesLoader {

    public static final String TEMPLATE_PROPERTY_PRODUCT_TYPE = "PRODUCT_TYPE";


    public long[] getTradeIds(final ReportTemplate template, final String agreementIds, final JDate valDate)
            throws RemoteException {
        StringBuilder from = new StringBuilder();
        StringBuilder where = new StringBuilder();
        buildFromAndWhere(from, where, template, agreementIds, valDate);

        // If additional tables are needed in the from clause, this will start with a comma. It is assumed that there
        // are tables trade and product_desc already in the clause.
        String fromClause = from.toString();
        if ((fromClause.length() > 0) && (fromClause.charAt(0) == ',')) {
            fromClause = fromClause.substring(1);
        }

        return DSConnection.getDefault().getRemoteTrade().getTradeIds(fromClause, where.toString(), 0, 0, null, null);
    }

    protected void buildFromAndWhere(StringBuilder sqlFrom, StringBuilder sqlWhere, final ReportTemplate template,
                                     final String agreementIds, final JDate valDate) {
        buildMandatorySQLQuery(sqlFrom, sqlWhere, template, agreementIds, valDate);

        buildOptionalCptySQLQuery(sqlFrom, sqlWhere, template);

        buildOptionalFundSQLQuery(sqlFrom, sqlWhere, template);

        buildOptionalDealOwnerSQLQuery(sqlFrom, sqlWhere, template);

        buildOptionalTradeStatusSQLQuery(sqlFrom, sqlWhere, template);

        buildOptionalEconomicSectorSQLQuery(sqlFrom, sqlWhere, template);

    }

    @Override
    protected void buildMandatorySQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
                                          final ReportTemplate template, final String agreementIds, final JDate valDate) {
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

        // Product Type
        sqlWhere.append(" AND product_desc.product_type ");

        String productType = (String) template
                .get(TEMPLATE_PROPERTY_PRODUCT_TYPE);
        if (Util.isEmpty(productType) || "ALL".equals(productType)) {
            sqlWhere.append("IN ('");
            sqlWhere.append(CollateralExposure.PRODUCT_TYPE);
            sqlWhere.append("', '");
            sqlWhere.append(Product.REPO);
            sqlWhere.append("', '");
            sqlWhere.append(Product.SEC_LENDING);
            sqlWhere.append("') ");
        } else {
            sqlWhere.append("= '");
            sqlWhere.append(productType);
            sqlWhere.append("' ");
        }
    }

}
