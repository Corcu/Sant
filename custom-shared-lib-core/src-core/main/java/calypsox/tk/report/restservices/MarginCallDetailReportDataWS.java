package calypsox.tk.report.restservices;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.calypso.infra.util.Util;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.SortColumn;

public class MarginCallDetailReportDataWS extends AbstractReportDataWS {
	private static Semaphore sem = new Semaphore(
			Integer.parseInt(DomainValues.comment("WS.MaxConcurrentReq", "MarginCallDetail")), true);

	public MarginCallDetailReportDataWS(String reporType, String reportTemplate) {
		super(reporType, reportTemplate, "MarginCallDetail");
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
			SortColumn[] sortCols = new SortColumn[sortColumns.length];
			boolean[] sortDir = new boolean[sortColumns.length];
			for (int i=0; i<sortColumns.length;i++) {
				if(sortColumns[i].contains("+")) {
					SortColumn sc = new SortColumn(sortColumns[i].replaceAll("\\+", ""));
					sc.setAscending(true);
					sortCols[i] = sc;
				}else if(sortColumns[i].contains("-")) {
					SortColumn sc = new SortColumn(sortColumns[i].replaceAll("-", ""));
					sc.setAscending(false);
					sortCols[i] = sc;
				}
			}
			report.getReportTemplate().setSortColumns(sortColumns);
			report.getReportTemplate().setSortAscDesc(sortDir);
		}
		return "";
	}

}
