package calypsox.tk.report.restservices;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;

public class CAClaimTradeReportDataWS extends AbstractReportDataWS {
	private static  Semaphore sem = new Semaphore(Integer.parseInt(DomainValues.comment("WS.MaxConcurrentReq","CAClaimTrade")), true);

	public CAClaimTradeReportDataWS(String reporType, String reportTemplate) {
		super(reporType, reportTemplate, "Trade");
	}

	@Override
	boolean adquireSemaphore() throws InterruptedException {
		if(max_conc_req == 0) {
			return true;
		}
		return sem.tryAcquire(timeout, TimeUnit.SECONDS);
	}

	@Override
	void releaseSemaphore() {
		if(max_conc_req != 0) {
			sem.release();
		}
			
	}
	@Override
	public String performPagination(int page,int pageSize, Report report, List<String> orderByOptions, List<String> groupOptions, Vector<String> errorMsgs) {

		TradeFilter tf = getTradeFilter();
		String paginationQuery = super.performPagination(page, pageSize, report, orderByOptions, groupOptions,
				errorMsgs);
		String tfQuery = "";
		if(tf != null) {
			if(tf.getSQLWhereClause() != null)
				tfQuery = " AND " + tf.getSQLWhereClause();
		}
		String str = (String) report.getReportTemplate().get("ProductType"); 
		if(!Util.isEmpty(str)) {
			tfQuery = tfQuery.concat(" AND product_desc.product_id = trade.product_id "); //Needed for pagination when a Product Type filter is set
		}
		return tfQuery.concat(paginationQuery);
		
	}
	
	private TradeFilter getTradeFilter() {
		//The TradeFilter name will allways be the template name
		TradeFilter tfActual = BOCache.getTradeFilter(DSConnection.getDefault(), this.getReportTemplate());
		return tfActual;
		                
	}
	@Override
	public void modifyTemplate(Report report) {
		TradeReportTemplate template = report.getReportTemplate();
		
		template.put("TRADE_FILTER", "");
		report.setReportTemplate(template);
	}
	@Override
	public boolean checkBookFilter(Report report) {
		boolean exists = true;
		Vector books;
		int booknum = 0;
		ReportTemplate  t = report.getReportTemplate();
		books = Util.string2Vector(t.get("Book"));;
		for(booknum = 0; booknum < books.size(); ++booknum) {
			Book b = BOCache.getBook(DSConnection.getDefault(), (String)books.get(booknum));
			if(b != null) {
				return true;
			} else {
				exists = false;
			}
		}
		return exists;
		
	}

}
