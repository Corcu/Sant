/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import calypsox.tk.report.SantPortfolioBreakdownReportTemplate;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

public class SantPortfolioTradesLoader extends SantEnableThread<Long, Trade> {

    private final String agreementIds;
    private final JDate valDate;
    private static final String EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME = "ExposureInterfaceTradeKeywordAfterMaturity";
    private static String KEYWORD_CM_AFTER_MATURITY = null;
    @SuppressWarnings("unused")
    private static String VALUE_CM_AFTER_MATURITY = null;
    private static boolean retrievePastMaturityTrades = true;

    public SantPortfolioTradesLoader(boolean enableThreading, ReportTemplate template, String agreementIds,
                                     JDate valDate) {
        super(template, enableThreading);
        this.agreementIds = agreementIds;
        this.valDate = valDate;
        setCMAfterMaturityKeywordFromDV();

    }

    /**
     * @return maturity keyword from DV + value
     */
    private void setCMAfterMaturityKeywordFromDV() {

        if (KEYWORD_CM_AFTER_MATURITY != null)
            return;
        final Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(),
                EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME);
        if (domainValues.isEmpty()) {
            Log.error(this, "In DomainName " + EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME
                    + "DomainValue not found. Please check configuration.");
            retrievePastMaturityTrades = false;
            return;
        }
        if (domainValues.size() > 1) {
            Log.error(this, "In DomainName " + EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME
                    + "More than one DomainValue found. Please check configuration.");
            retrievePastMaturityTrades = false;
            return;

        }
        final String comment = CollateralUtilities.getDomainValueComment(EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME,
                domainValues.get(0));

        if (Util.isEmpty(comment)) {
            Log.error(this, "DomainValue " + EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME
                    + "has no comment -keyword value-. Please check configuration.");
            retrievePastMaturityTrades = false;
            return;
        }
        KEYWORD_CM_AFTER_MATURITY = domainValues.get(0);
        VALUE_CM_AFTER_MATURITY = comment;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void loadData() {

        StringBuilder sqlFrom = new StringBuilder();
        StringBuilder sqlWhere = new StringBuilder();

        final Set<Integer> tradeIds = (Set<Integer>) this.template
                .get(SantPortfolioBreakdownReportTemplate.TRADE_IDS_FOR_EMAIL_GATEWAY_NOTIF);
        if (!Util.isEmpty(tradeIds)) {
            buildSQLQueryForNotification(tradeIds, sqlFrom, sqlWhere);
            return;
        }
        buildMandatorySQLQuery(sqlFrom, sqlWhere, this.template, this.agreementIds, this.valDate);


        try {
            this.dataList.addAll(DSConnection.getDefault().getRemoteTrade().getTrades(sqlFrom.toString(),
                    sqlWhere.toString(), null, null));
        } catch (RemoteException e) {
            Log.error(this, "Cannot Load Trades", e);
        }
        // AAP
        // Part 2, now loads the past MaturityDate trades looking for it
        // throught a Keyword
        if (retrievePastMaturityTrades) {
            sqlFrom = new StringBuilder();
            sqlWhere = new StringBuilder();
            buildPastMaturityTradesSQLQuery(sqlFrom, sqlWhere, this.template, this.agreementIds, this.valDate);
            try {
                TradeArray nonFilteredTrades = DSConnection.getDefault().getRemoteTrade().getTrades(sqlFrom.toString(),
                        sqlWhere.toString(), null, null);
                this.dataList.addAll(nonFilteredTrades);//filterByExposureAfterMaturityKeyword(nonFilteredTrades));
            } catch (RemoteException e) {
                Log.error(this, "Cannot Load Trades", e);
            }
        }

    }

    @SuppressWarnings("unchecked")
    private void buildSQLQueryForNotification(Set<Integer> tradeIds, StringBuilder sqlFrom, StringBuilder sqlWhere) {

        final ArrayList<Integer> tradeIdsList = new ArrayList<Integer>(tradeIds);
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

                this.dataList.addAll(DSConnection.getDefault().getRemoteTrade().getTrades(sqlFrom.toString(),
                        localWhere.toString(), null, null));
            }
        } catch (RemoteException e) {
            Log.error(this, "Cannot Load Trades", e);
        }

    }

    private void buildMandatorySQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
                                        final ReportTemplate template, final String agreementIds, final JDate valDate) {
        buildTradesSQLQuery(sqlFrom, sqlWhere, template, agreementIds, valDate);
        appendMaturityGreaterThanToday(sqlWhere);
        SantSQLQueryUtil.buildOptionalDealOwnerSQLQuery(sqlFrom, sqlWhere, this.template);
        SantSQLQueryUtil.buildOptionalTradeStatusSQLQuery(sqlFrom, sqlWhere, this.template);
        appendAgreementTypeAndInstrumentFilter(sqlFrom, sqlWhere);

    }

    /**
     * @param sqlFrom
     * @param sqlWhere
     * @param template
     * @param agreementIds
     * @param valDate      Adds the collateral trades with past maturity date by looking
     *                     for a keyword which identifies them as collateralizable
     * @author aalonsop
     */
    private void buildPastMaturityTradesSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
                                                 final ReportTemplate template, final String agreementIds, final JDate valDate) {
        buildTradesSQLQuery(sqlFrom, sqlWhere, template, agreementIds, valDate);
        //AAP FIX 09/06/2016 - INCIDENCIA DE RENDIMIENTO - NO UTILIZAR
        SantSQLQueryUtil.buildOptionalTradeStatusSQLQuery(sqlFrom, sqlWhere, this.template);
        //old
        //sqlWhere.append(" AND ( trade.trade_status = '" + Status.VERIFIED + "' OR trade.trade_status = '" + Status.MATURED + "' )");
        //appendMaturityLowerThanToday(sqlWhere);
        //GSM 29/08/2016 - Fix rendimiento vencidas
        appendMaturityKeyword(sqlFrom, sqlWhere);

        SantSQLQueryUtil.buildOptionalDealOwnerSQLQuery(sqlFrom, sqlWhere, this.template);
        appendAgreementTypeAndInstrumentFilter(sqlFrom, sqlWhere);
    }

    private void buildTradesSQLQuery(final StringBuilder sqlFrom, final StringBuilder sqlWhere,
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
        // sqlWhere.append("AND TRUNC(trade.trade_date_time) <= ");
        // sqlWhere.append(Util.date2SQLString(valDate));
    }

    /**
     * Appends maturity date greater than valDate clause
     *
     * @param sqlWhere
     */
    private void appendMaturityGreaterThanToday(StringBuilder sqlWhere) {
        // Mandatory Process date on maturity date
        sqlWhere.append(" AND( ");
        sqlWhere.append(" TRUNC(product_desc.maturity_date) >= ");
        sqlWhere.append(Util.date2SQLString(valDate));
        sqlWhere.append(" OR product_desc.maturity_date is null ");
        sqlWhere.append(" ) ");
    }

//	private void appendMaturityLowerThanToday(StringBuilder sqlWhere) {
//		// Mandatory Process date on maturity date
//		sqlWhere.append(" AND ");
//		sqlWhere.append(" TRUNC(product_desc.maturity_date) < ");
//		sqlWhere.append(Util.date2SQLString(valDate) + " ");
//	}

    //GSM 29/08/2016 - Fix rendimiento vencidas
    private void appendMaturityKeyword(StringBuilder sqlFrom, StringBuilder sqlWhere) {

        sqlFrom.append(" , trade_keyword kwd1 ");
        sqlWhere.append("AND trade.trade_status<>'CANCELED' AND kwd1.trade_id = trade.trade_id  "
                + " AND trade.trade_id=kwd1.trade_id AND kwd1.keyword_name='ExposureAfterMaturity' "
                + "AND kwd1.keyword_value='true' ");

    }

    private void appendAgreementTypeAndInstrumentFilter(StringBuilder sqlFrom, StringBuilder sqlWhere) {
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
    }

//	private TradeArray filterByExposureAfterMaturityKeyword(TradeArray trades) {
//		TradeArray filteredArray = new TradeArray();
//		for (Object trade : trades) {
//			String keywordValue = ((Trade) trade).getKeywordValue(KEYWORD_CM_AFTER_MATURITY);
//			if (!Util.isEmpty(keywordValue) && keywordValue.equals(VALUE_CM_AFTER_MATURITY))
//				filteredArray.add(trade);
//		}
//		return filteredArray;
//	}

    @Override
    protected Map<Long, Trade> getDataMapFromDataList() {
        for (Trade trade : this.dataList) {
            this.dataMap.put(trade.getLongId(), trade);
        }
        return this.dataMap;
    }

}
