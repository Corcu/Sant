package calypsox.tk.report;

import com.calypso.tk.core.*;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

public class FI105_BRSReport extends TradeReport {
    private static final long serialVersionUID = -1L;

    public FI105_BRSReport() {
    }// 18

    @Override
    public ReportOutput load(Vector errors) {
        DefaultReportOutput output = new StandardReportOutput(this);// 24
        ArrayList<ReportRow> rows = new ArrayList();// 25
        JDatetime valDatetime = this.getValuationDatetime();// 26
        TradeArray trades = this.getTrades(valDatetime);// 27

        if (!Util.isEmpty(trades)) {// 29
            Trade currentTrade = null;// 30
            ReportRow currentRow = null;// 31

            for (int i = 0; i < trades.size(); ++i) {// 34
                currentTrade = trades.get(i);// 35
                currentRow = new ReportRow(currentTrade);// 39
                rows.add(currentRow);// 41
                if (!Util.isEmpty(errors)) {// 42
                    Log.debug(this, errors.toString());// 43
                }
            }
        }

        output.setRows(rows.toArray(new ReportRow[rows.size()]));// 48
        return output;// 49
    }

    protected TradeArray getTrades(JDatetime valueDate) {
        TradeArray trades = new TradeArray();// 61
        trades.addAll(this.getTradesBrs(valueDate));// 63
        return trades;// 64
    }

    private TradeArray getTradesBrs(JDatetime valueDate) {
        StringBuffer where = new StringBuffer();// 69
        where.append(this.getBrs(valueDate));// 70
        return this.getTrades(this.getFrom(), where.toString());// 71
    }

    private String getBrs(JDatetime valueDate) {
        StringBuffer where = new StringBuffer(" (  ");// 76
        where.append(" (");// 77
        where.append(" (");// 78
        where.append(" trade.product_id = product_desc.product_id");// 79
        where.append(" )");// 80
        where.append(" )");// 81
        where.append(" AND ");// 82
        where.append(" ( ");// 83
        where.append(" ( ");// 84
        where.append(" ( ");// 85
        where.append(" product_desc.product_type = 'PerformanceSwap'");// 86
        where.append(" ) ");// 87
        where.append(" AND ");// 88
        where.append(" ( ");// 94
        where.append(" trade.trade_date_time <= ");// 95
        where.append(Util.datetime2SQLString(valueDate));// 96
        where.append(" ) ");// 97
        where.append(" AND ");// 98
        where.append(" ( ");// 99
        where.append(" trade.trade_status = 'VERIFIED' ");// 100
        where.append(" ) ");// 101
        where.append(" ) ");// 102
        where.append(" ) ");// 103
        where.append(" ) ");// 104
        return where.toString();// 105
    }

    protected String getFrom() {
        return " trade trade, product_desc product_desc";// 117
    }

    private TradeArray getTrades(String from, String where) {
        TradeArray rst = null;// 129

        try {
            rst = Optional.ofNullable(DSConnection.getDefault().getRemoteTrade().getTrades(from, where, (String) null, (List) null)).orElse(TradeArray.EMPTY_TRADE_ARRAY);// 131
        } catch (RemoteException var5) {// 132
            Log.error(this, var5);// 133
        }

        return rst;// 135
    }

}
