package calypsox.tk.report;

import com.calypso.apps.util.AppUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallDetailEntryDTOReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author aela
 */
@SuppressWarnings("rawtypes")
public class SantMCPortfolioReport extends MarginCallDetailEntryDTOReport {

    public static final String TYPE = "SantMCPortfolio";
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public ReportOutput load(Vector errorMsgs) {

        SantMCPortfolioReportTemplate template = (SantMCPortfolioReportTemplate) getReportTemplate();
        List<MarginCallDetailEntryDTO> entries = template.getDetailEntries();
        DefaultReportOutput output = new DefaultReportOutput(this);
        if (Util.isEmpty(entries)) {
            // build the from and where parts for the report query
            String where = buildQuery(_reportTemplate);
            List<String> from = buildFrom(where);

            try {
                // get the entires using the from and where clauses
                entries = ServiceRegistry.getDefault().getDashBoardServer().loadMarginCallDetailEntries(where, from);
            } catch (RemoteException e) {
                Log.error(this, e);
            }
        }

        if (!Util.isEmpty(entries)) {
            MarginCallConfig config = null;

            // ReportRow[] rows = new ReportRow[entries.size()];
            ArrayList<ReportRow> filteredRows = new ArrayList<ReportRow>();
            PricingEnv pe = null;
            for (int i = 0; i < entries.size(); i++) {
                MarginCallDetailEntryDTO entry = entries.get(i);
                ReportRow row = new ReportRow(entry, ReportRow.DEFAULT);
                if (config == null) {
                    try {
                        config = DSConnection.getDefault().getRemoteReferenceData()
                                .getMarginCallConfig(entry.getMarginCallConfigId());
                    } catch (RemoteException e) {
                        Log.error(this, e);
                        config = null;
                    }

                }

                if (config != null && pe == null) {
                    pe = AppUtil.loadPE(config.getPricingEnvName(), getValuationDatetime());
                    setPricingEnv(pe);
                }

                row.setProperty("MARGIN_CALL_CONFIG", config);
                row.setProperty("PRICING_ENV", getPricingEnv());
                Trade trade = null;
                // get the trade related to this entry
                try {
                    trade = DSConnection.getDefault().getRemoteTrade().getTrade(entry.getTradeId());
                    row.setProperty("TRADE", trade);
                } catch (RemoteException e) {
                    Log.error(this, e);
                }
                if (trade != null && "DISPUTE_ADJUSTMENT".equals(trade.getProductSubType())) {
                    continue;
                }
                filteredRows.add(row);
                // rows[i] = row;
            }

            ReportRow[] finalRows = new ReportRow[filteredRows.size()];
            finalRows = filteredRows.toArray(finalRows);
            output.setRows(finalRows);
            return output;

        }
        return null;

        // // set more information on each row (such as margin call contract
        // // info...)
        // if (output != null) {
        // ReportRow[] rows = output.getRows();
        // if (rows != null && rows.length > 0) {
        // for (int i = 0; i < rows.length; i++) {
        // ReportRow row = rows[i];
        // MarginCallDetailEntryDTO entry = (MarginCallDetailEntryDTO)
        // row.getProperty(ReportRow.DEFAULT);
        // // since all the entries belongs to the same contract, so we
        // // will look for it once.
        // if (config == null) {
        // try {
        // config = DSConnection.getDefault().getRemoteReferenceData()
        // .getMarginCallConfig(entry.getMarginCallConfigId());
        // } catch (RemoteException e) {
        // Log.error(this, e);
        // config = null;
        // }
        //
        // }
        // row.setProperty("MARGIN_CALL_CONFIG", config);
        // // get the trade related to this entry
        // try {
        // Trade trade =
        // DSConnection.getDefault().getRemoteTrade().getTrade(entry.getTradeLongId());
        // row.setProperty("TRADE", trade);
        // } catch (RemoteException e) {
        // Log.error(this, e);
        // }
        // }
        // }
        // }
        // return output;
    }
}
