package calypsox.tk.report.restservices;

import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

/**
 * 
 * @author x957355
 *
 */
public class CAClaimsDigitalSettlementsReportDataWS extends AbstractReportDataWS{

	private static Semaphore sem = new Semaphore(
			Integer.parseInt(DomainValues.comment("WS.MaxConcurrentReq", "CAClaimsDigitalSettlements")), true);
	
	public CAClaimsDigitalSettlementsReportDataWS(String reportType, String reportTemplate) {
		super(reportType, reportTemplate, "BOTransfer");
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
