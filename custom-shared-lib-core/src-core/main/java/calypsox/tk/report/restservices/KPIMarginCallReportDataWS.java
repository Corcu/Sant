package calypsox.tk.report.restservices;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.calypso.infra.util.Util;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.report.Report;

public class KPIMarginCallReportDataWS extends AbstractReportDataWS {
	private static Semaphore sem = new Semaphore(
			Integer.parseInt(DomainValues.comment("WS.MaxConcurrentReq", "KPIMarginCall")), true);

	public KPIMarginCallReportDataWS(String reporType, String reportTemplate) {
		super(reporType, reportTemplate, "KPIMarginCall");
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
	public String performPagination(int page, int pageSize, Report report, List<String> orderByOptions,
			List<String> groupOptions, Vector<String> errorMsgs) {
		if (!Util.isEmpty(orderByOptions)) {
			String[] sortColumns = orderByOptions.toArray(new String[orderByOptions.size()]);
			report.getReportTemplate().setSortColumns(sortColumns);
		}
		return "";
	}

}
