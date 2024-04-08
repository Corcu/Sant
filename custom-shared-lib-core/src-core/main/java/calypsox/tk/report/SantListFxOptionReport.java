/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteTrade;
import com.calypso.tk.util.TradeArray;
import org.jfree.util.Log;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Vector;

public class SantListFxOptionReport extends SantReport {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 286417350045844234L;

    private RemoteTrade remoteTrade = null;

    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ReportOutput loadReport(final Vector errorMsgs) {

        final DefaultReportOutput output = new DefaultReportOutput(this);

        final String pricingEnv = getPricingEnv().getName();

        // process start date
        final JDate processDate = getProcessStartDate();

        // Counterparty
        final String counterparty = (String) getReportTemplate().get(SantListFxOptionReportStyle.COUNTERPARTY);

        // Selling currency
        final String curr = (String) getReportTemplate().get(SantListFxOptionReportStyle.PRINCIPAL_CCY);

        // GSM 20/07/15. SBNA Multi-PO filter
        // Agreement owners
        // GSM 03/08/15. SBNA Multi-PO filter. Adaptation to ST filter
        final String ownersIds = CollateralUtilities.filterPoIdsByTemplate(this._reportTemplate);
        // (String) getReportTemplate().get(SantListFxOptionReportTemplatePanel.OWNER_AGR);

        // Necesito comprobar todas las operaciones del sistema, cuyo tipo sean
        // Exposure Trades y product FX Option que est?n mature pero no settled
        // y que hayan generado FX Spot cuyo MtM debe ser incluido para el
        // calculo de la exposici?n.

        this.remoteTrade = DSConnection.getDefault().getRemoteTrade();

        TradeArray tradeArray = null;
        Trade[] listTrade = null;
        String from = " product_collateral_exposure, trade_keyword ";
        StringBuilder sqlwhere = new StringBuilder();
        sqlwhere.append(" trade.product_id = product_collateral_exposure.product_id ");
        sqlwhere.append(" AND trade.trade_id=trade_keyword.trade_id ");
        sqlwhere.append(" AND product_collateral_exposure.underlying_type='FX_OPTION' ");
        sqlwhere.append(" AND trade.trade_status !='CANCELED' ");
        sqlwhere.append(" and keyword_name ='" + TradeInterfaceUtils.TRADE_KWD_IMPORT_REASON + "'");
        sqlwhere.append(" and keyword_value ='FXSPOT'");
        sqlwhere.append(" AND TRUNC(product_collateral_exposure.end_date) <= ")
                .append(Util.date2SQLString(processDate));

        // GSM 20/07/15. SBNA Multi-PO filter
        if (!Util.isEmpty(ownersIds)) {
            // BAU 24/02/2016 adrian
            final Vector<String> posAsList = Util.string2Vector(ownersIds);
            // sqlwhere.append("AND trade_keyword.keyword_name = ").append(
            // Util.string2SQLString(CollateralStaticAttributes.MC_CONTRACT_NUMBER));
            // sqlwhere.append(" AND to_number(trade_keyword.keyword_value) = mrgcall_config.mrg_call_def");
            sqlwhere.append(" AND mrgcall_config.mrg_call_def = (SELECT KEYWORD_VALUE " + "from  trade_keyword "
                    + "where TRADE_ID = trade.trade_id AND KEYWORD_NAME = 'MC_CONTRACT_NUMBER')");
            sqlwhere.append(" AND mrgcall_config.process_org_id=legal_entity.legal_entity_id");
            sqlwhere.append(" AND legal_entity.legal_entity_id IN ").append(Util.collectionToSQLString(posAsList));
            from = from + ", mrgcall_config, legal_entity ";
        }

        if (!Util.isEmpty(counterparty)) {
            sqlwhere.append(" and trade.cpty_id = '").append(counterparty).append("' ");
        }

        try {
            // GSM 20/07/15. SBNA Multi-PO filter
            tradeArray = this.remoteTrade.getTrades(from, sqlwhere.toString(), null, null);
            // tradeArray = this.remoteTrade.getTrades(
            // " product_collateral_exposure, trade_keyword, mrgcall_config, legal_entity ", sqlwhere.toString(),
            // null);

            listTrade = tradeArray.getTrades();

            final ArrayList<ReportRow> reportRows = getRows(listTrade, pricingEnv, processDate, curr);

            output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

            return output;

        } catch (final RemoteException re) {
            String error = "Error retrieving trades\n";
            Log.error(this, re);
            errorMsgs.add(error + re.getMessage());
        }

        return null;
    }

    private ArrayList<ReportRow> getRows(final Trade[] listTrade, final String pricingEnv, final JDate today,
                                         final String curr) throws RemoteException {
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        for (int i = 0; i < listTrade.length; i++) {
            final Trade trade = listTrade[i];

            final CollateralExposure colExpoProduct = (CollateralExposure) trade.getProduct();
            final Product product = trade.getProduct();
            final PLMark pl = CollateralUtilities.retrievePLMark(trade, getDSConnection(), pricingEnv, today);

            if (!Util.isEmpty(curr)) {
                if (curr.equals(product.getCurrency())) {
                    reportRows.add(createReportRow(trade, product, pl, colExpoProduct));
                }
            } else {
                reportRows.add(createReportRow(trade, product, pl, colExpoProduct));
            }
        }
        return reportRows;
    }

    private ReportRow createReportRow(final Trade trade, final Product product, final PLMark pl,
                                      final CollateralExposure colExpoProduct) {
        final ReportRow row = new ReportRow(trade);

        row.setProperty(SantListFxOptionReportStyle.BOOK, trade.getBook().toString());

        row.setProperty(SantListFxOptionReportStyle.COUNTERPARTY, trade.getCounterParty().getAuthName());

        row.setProperty(SantListFxOptionReportStyle.EXTERNAL_REF, trade.getKeywordValue("NUM_FRONT_ID"));

        if (product.getBuySell(trade) == 1) {
            row.setProperty(SantListFxOptionReportStyle.GIVE_RECEIVE, "BUY");
        } else if (product.getBuySell(trade) == -1) {
            row.setProperty(SantListFxOptionReportStyle.GIVE_RECEIVE, "SELL");
        }

        row.setProperty(SantListFxOptionReportStyle.PRODUCT_TYPE, product.getType());

        row.setProperty(SantListFxOptionReportStyle.UNDERLYING, colExpoProduct.getAttribute("UNDERLYING"));

        row.setProperty(SantListFxOptionReportStyle.VALUE_DATE, getValDate().toString());
        row.setProperty(SantListFxOptionReportStyle.PO, colExpoProduct.getAttribute("OWNER"));

        row.setProperty(SantListFxOptionReportStyle.PRINCIPAL, product.getPrincipal());

        row.setProperty(SantListFxOptionReportStyle.PRINCIPAL_CCY, product.getCurrency());

        row.setProperty(SantListFxOptionReportStyle.MATURITY_DATE, trade.getProduct().getMaturityDate());

        row.setProperty(SantListFxOptionReportStyle.TRADE_DATE, trade.getTradeDate().getJDate(TimeZone.getDefault()));

        row.setProperty(SantListFxOptionReportStyle.SOURCE_SYSTEM, trade.getKeywordValue("FO_SYSTEM"));

        row.setProperty(SantListFxOptionReportStyle.EXTERNAL_REF_2, trade.getKeywordValue("BO_REFERENCE"));

        row.setProperty(SantListFxOptionReportStyle.SOURCE_SYSTEM_2, trade.getKeywordValue("BO_SYSTEM"));

        row.setProperty(SantListFxOptionReportStyle.UNDERLYING_TYPE, colExpoProduct.getAttribute("UNDERLYING_TYPE"));

        row.setProperty(SantListFxOptionReportStyle.CALL_PUT, colExpoProduct.getAttribute("CALL_PUT"));

        if (pl != null) {
            Amount mtm = null;
            // MIGRATION V14.4
            PLMarkValue markValue = pl.getPLMarkValueFromList("NPV_BASE");
            if (markValue != null) {
                mtm = new Amount(markValue.getMarkValue());
            }

            row.setProperty(SantListFxOptionReportStyle.MTM_TRADE, mtm);
        }

        return row;

    }

}
