package calypsox.tk.report;


import com.calypso.tk.core.*;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;


public class EquityMisPlusDividendosReport extends TradeReport {


    private static final long serialVersionUID = 1L;


    @SuppressWarnings({ "rawtypes", "deprecation", "unused" })
	@Override
    public ReportOutput load(final Vector errorMsgs) {

        final SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
        JDate valuationDate = getValDate();
        JDate startDate = JDate.valueOf("01/01/" + valuationDate.getYear());
        JDate endDate = JDate.valueOf("31/12/"+valuationDate.getYear());

        final DefaultReportOutput output = new DefaultReportOutput(this);

        final StringBuilder from = new StringBuilder();
        from.append("trade trade, product_ca ca, product_desc underlying");
        final StringBuilder where = new StringBuilder();
        where.append("trade.trade_status = 'VERIFIED' AND ");
        where.append("trade.product_id = ca.product_id AND ");
        where.append("ca.underlying_id = underlying.product_id AND ");
        where.append("underlying.product_type = 'Equity' AND ");
        where.append("ca.record_date >= TO_DATE('" + sdf.format(new JDatetime(startDate, 0, 0, 0, TimeZone.getDefault())) + "','DDMMYYHH24MI') AND ");
        where.append("ca.record_date <= TO_DATE('" + sdf.format(new JDatetime(endDate, 23, 59, 59, TimeZone.getDefault())) + "','DDMMYYHH24MI')");

        TradeArray trades = null;
        try {
            trades = DSConnection.getDefault().getRemoteTrade().getTrades(from.toString(), where.toString(),"trade.TRADE_ID", true, true);
        } catch (final RemoteException re) {
            Log.error(this, "EquityMisPlusDividendosReport" + ": An Exception ocurred getting the trades:\n" + re);
            trades = new TradeArray();
        }
        if (trades == null) {
            return output;
        }

        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        for (int i = 0; i < trades.size(); i++) {
            Trade trade = trades.get(i);
            final ReportRow row = new ReportRow(trades.get(i));
            row.setProperty(ReportRow.VALUATION_DATETIME, getValuationDatetime());
            row.setProperty(ReportRow.PRICING_ENV, getPricingEnv());
            reportRows.add(row);
        }
        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
        return output;
    }


}