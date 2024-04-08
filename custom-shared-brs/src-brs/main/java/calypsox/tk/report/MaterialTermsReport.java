package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.FXSwap;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

public class MaterialTermsReport extends TradeReport{


	private static final long serialVersionUID = -7250167569558646097L;
	
	
	public MaterialTermsReport(){
		super();
	}

	 
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public ReportOutput load(final Vector errors) {

        final DefaultReportOutput rst = new StandardReportOutput(this);
        final ArrayList<ReportRow> rows = new ArrayList<ReportRow>();
        final JDatetime valDatetime = getValuationDatetime();
        final TradeArray trades = getTrades(valDatetime);

        if (!Util.isEmpty(trades)) {
            Trade currentTrade = null;
            ReportRow currentRow = null;
            MaterialTermsReportLogic logic = null;
            MaterialTermsReportItem item = null;
            for (int i = 0; i < trades.size(); i++) {
                currentTrade = trades.get(i);
                logic = new MaterialTermsReportLogic(currentTrade, valDatetime);
                item = new MaterialTermsReportItem();
                logic.fillItem(item, errors);
                currentRow = new ReportRow(item);
                currentRow.setProperty(MaterialTermsReportItem.MATERIAL_TERMS_REPORT_ITEM, item);
                rows.add(currentRow);
                if (!Util.isEmpty(errors)) {
                    Log.debug(this, errors.toString());
                }
            }
        }
        
        rst.setRows(rows.toArray(new ReportRow[rows.size()]));
        return rst;
    }


    /**
     * Get all trades without exploding the FXSwaps.
     * 
     * @param relevantStatus
     * 
     * @param valueDate
     *            Valuation date time.
     * @return TradeArray containing all the trades.
     */
    protected TradeArray getTrades(final JDatetime valueDate) {
        final TradeArray trades = new TradeArray();
        final TradeArray finalTrades = new TradeArray();
        
        trades.addAll(getTradesBrs(valueDate));
        for (int i = 0; i < trades.size(); i++) {
            if (!isInternalDeal(trades.get(i))) {
                finalTrades.add(trades.get(i));
            }
        }
        return finalTrades;
    }


    private TradeArray getTradesBrs(final JDatetime valueDate) {
        final StringBuffer where = new StringBuffer();
        where.append(getBrs(valueDate));
        return getTrades(getFrom(), where.toString());
    }


    private String getBrs(final JDatetime valueDate) {
        final StringBuffer str = new StringBuffer(" (  ");
        str.append(" (");
        str.append(" (");
        str.append(" trade.product_id = product_desc.product_id");
        str.append(" )");
        str.append(" )");
        str.append(" AND ");
        str.append(" ( ");
        str.append(" ( ");
        str.append(" ( ");
        str.append(" product_desc.product_type = 'PerformanceSwap'");
        str.append(" ) ");
        str.append(" AND ");
        str.append(" ( ");
        str.append(" product_desc.maturity_date >= ");
        str.append(Util.datetime2SQLString(valueDate));
        str.append(" ) ");
        str.append(" AND ");
        str.append(" ( ");
        str.append(" trade.trade_date_time <= ");
        str.append(Util.datetime2SQLString(valueDate));
        str.append(" ) ");
        str.append(" AND ");
        str.append(" ( ");
        str.append(" trade.trade_status IN ('PENDING', 'PARTENON', 'VERIFIED')");
        str.append(" ");
        str.append(" ) ");
        str.append(" ) ");
        str.append(" ) ");
        str.append(" ) ");
        return str.toString();
    }


    /**
     * getFrom
     * 
     * @return String
     */
    protected String getFrom() {
        final StringBuffer str = new StringBuffer(" trade trade, ");

        str.append(" product_desc product_desc");

        return str.toString();
    }

 
    /**
     * get trades from DataBase
     * 
     * @param from
     *            from clause
     * @param where
     *            where clause
     * @return array of trades
     */
    private TradeArray getTrades(final String from, final String where) {
        TradeArray rst = null;
        try {
            rst = DSConnection.getDefault().getRemoteTrade().getTrades(from, where, null, null);
        } catch (final RemoteException e) {
            Log.error(this, e);
        }
        return rst;
    }


    /**
     * Check if the trade is internal
     * 
     * @param trade
     *            trade
     * @return if its internal
     */
    boolean isInternalDeal(final Trade trade) {
        boolean resp = false;

        final Book book = trade.getBook();
        final String bookLegalEntity = book.getLegalEntity().getName();
        final String cpty = trade.getCounterParty().getName();

        if (cpty.equalsIgnoreCase(bookLegalEntity)) {
            resp = true;
        }

        return resp;
    }


}
