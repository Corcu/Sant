package calypsox.tk.report;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;


public class SantEmirUtiTempReport extends Report {

	
  private static final long serialVersionUID = 1L;
  private static final String KEYWORD_UTI_TRADE_ID = "UTI_REFERENCE";
  private static final String KEYWORD_TEMP_UTI_TRADE_ID = "TempUTITradeId";
  
  
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public ReportOutput load(Vector error) {
    final JDatetime jdatetime = getValuationDatetime();
    final TradeArray tradeArray = getTrades(jdatetime);
    final ReportOutput reportOutput = getReport(tradeArray, jdatetime);
    return reportOutput;
  }


  /**
   * Get report
   *
   * @param tradeArray
   * @return
   */
  private ReportOutput getReport(final TradeArray tradeArray, final JDatetime jdatetime) {

    final StandardReportOutput standardReportOutput = new StandardReportOutput(this);
    SantEmirUtiTempReportLogic logic = null;
    final List<ReportRow> list = new ArrayList<ReportRow>();

    if(tradeArray!=null && tradeArray.size()>0) {
	    for (int i = 0; i < tradeArray.size(); i++) {
	      final Trade trade = tradeArray.elementAt(i);
	      ReportRow row = null;
	      logic = new SantEmirUtiTempReportLogic(trade, jdatetime);
	      final SantEmirUtiTempReportItem item = new SantEmirUtiTempReportItem();
	      logic.fillItem(item);
	      row = new ReportRow(item);
	      row.setProperty(SantEmirUtiTempReportItem.SANT_EMIR_UTI_TEMP_ITEM, item);
	      row.setProperty(ReportRow.TRADE, trade);
	      list.add(row);
	    }
	    final ReportRow[] reportRowsArray = list.toArray(new ReportRow[list.size()]);
	    standardReportOutput.setRows(reportRowsArray);
  	}
    return standardReportOutput;
  }


  /**
   * Get trades
   *
   * @param jdatetime
   * @return
   */
  protected TradeArray getTrades(final JDatetime jdatetime) {

      final Date date = jdatetime.getJDate(TimeZone.getDefault()).getDate(TimeZone.getDefault());
      final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      final String sDate = sdf.format(date);
    	
      String from = "trade_keyword, product_desc";
      StringBuilder where = new StringBuilder();
      where.append("trade.trade_id = trade_keyword.trade_id AND ");
      where.append("trade_keyword.keyword_name = '" + KEYWORD_TEMP_UTI_TRADE_ID + "' AND ");
     // where.append("TO_CHAR(trade.entered_date, 'YYYY-MM-DD') = '" + sDate + "' AND ");
      where.append("trade.product_id = product_desc.product_id AND ");
      where.append("product_desc.product_type IN ('PerformanceSwap')");
      TradeArray tradeArray = null;
      try {
    	tradeArray = DSConnection.getDefault().getRemoteTrade().getTrades(from, where.toString(), null, null);
      } catch (CalypsoServiceException e) {
    	Log.info(this, e);
      }

      if(tradeArray!=null && tradeArray.size()>0) {
	      TradeArray tradeArrayEndList = new TradeArray();
	      for (int i = 0; i < tradeArray.size(); i++) {
	    	  Trade trade = tradeArray.get(i);
	    	  String uti = trade.getKeywordValue(KEYWORD_UTI_TRADE_ID);
	    	  if (Util.isEmpty(uti)) {
	              tradeArrayEndList.add(trade);
	          }
	      }
	      return tradeArrayEndList;
      }
      else {
    	  return tradeArray;
      }
  }


}