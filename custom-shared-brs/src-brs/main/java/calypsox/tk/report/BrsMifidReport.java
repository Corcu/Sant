package calypsox.tk.report;


import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.mo.sql.TradeFilterSQL;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Vector;


/**
 * The Class TradeFXAuditReport.
 */
public class BrsMifidReport extends TradeReport {

	
    private static final long serialVersionUID = 2137425131330538355L;

    
    @Override
    public ReportOutput load(final Vector errorMsgs) {

        //initDates();
        final DefaultReportOutput output = new DefaultReportOutput(this);
        final DSConnection ds = getDSConnection();
        final StringBuilder from = new StringBuilder();
        final StringBuilder where = new StringBuilder();

        getTradesSQL(from, where, "BrsAuditReport", errorMsgs, false);
        TradeArray trades = null;
        try {
            trades = DSConnection.getDefault().getRemoteTrade().getTrades(from.toString(), where.toString(),"trade.TRADE_ID", true, true);
        } catch (final RemoteException re) {
        	Log.error(this, "BrsMifidReport" + ": An Exception ocurred getting the trades:\n" + re);
            trades = new TradeArray();
        }

        if (trades == null) {
            return output;
        }

        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        
        for (int i = 0; i < trades.size(); i++) {
        	Trade trade = trades.get(i);
        	String po = trade.getBook().getLegalEntity().getCode();
        	String cpty = trade.getCounterParty().getCode();
            if (!po.equalsIgnoreCase(cpty)) {
                final ReportRow row = new ReportRow(trades.get(i));
                row.setProperty(ReportRow.VALUATION_DATETIME, getValuationDatetime());
                row.setProperty(ReportRow.PRICING_ENV, getPricingEnv());
                reportRows.add(row);
            }
        }
        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
        return output;
    }

    
    /**
     * Get the Where to retrieve the trades.
     *
     * @param from
     *            from clause
     * @param where
     *            where clause
     * @param report
     *            report Type
     * @param errorMsgs
     *            vector of errors
     * @param b
     *            boolean to get the trades
     */
    protected void getTradesSQL(final StringBuilder from, final StringBuilder where, final String report,
  		  					  final Vector<String> errorMsgs, final boolean b) {
      final DSConnection ds = DSConnection.getDefault();
      final TradeFilter filter = getTradeFilter(errorMsgs, TradeReportTemplate.TRADE_FILTER, ds);
      final JDate valDate = getValDate();
      String filterFrom = null;
      String filterWhere = null;

      try {	
      	filterFrom = TradeFilterSQL.generateFromClause(filter);
      	filterWhere = TradeFilterSQL.generateWhereClause(filter);
      	if (!Util.isEmpty(filterFrom)) {
      		from.append(filterFrom);
      	}
      	if (!Util.isEmpty(filterWhere)) {
      		where.append(filterWhere);
      	}
      	
      } catch (final PersistenceException e) {
        errorMsgs.add(report + ": Can't generate the sql query from the trade filter:\n" + e);
      }
    }
    
    
    /**
     * Gets the trade filter.
     * 
     * @param errorMsgs
     *            the error msgs
     * @param filterTemplateName
     *            the filter template name
     * @param ds
     *            the ds
     * @return the trade filter
     */
    protected TradeFilter getTradeFilter(final Vector<String> errorMsgs,
            final String filterTemplateName, final DSConnection ds) {
        final ReportTemplate template = getReportTemplate();
        if (template == null) {
            errorMsgs.add("The report hasn't a report template.");
            return null;
        }
        final Attributes attr = template.getAttributes();
        if (attr == null) {
            errorMsgs.add("The report can't get the attributes from the template:" + template.getId());
            return null;
        }
        final String filterName = (String) attr.get(filterTemplateName);

        if (filterName == null) {
            errorMsgs.add("The trade filter for the template:" + template.getId() + " is null.");
            return null;
        }
        TradeFilter filter = BOCache.getTradeFilter(ds, filterName);
        if (filter == null) {
            errorMsgs.add("The trade filter:" + filterName + " is null.");
            return null;
        }
        try {
            filter = (TradeFilter) filter.cloneIfImmutable();
        } catch (final CloneNotSupportedException e) {
            Log.error(this, e);
        }
        filter.setValDate(getValuationDatetime());
        return filter;
    }
          
    
}
