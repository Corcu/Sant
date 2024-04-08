package calypsox.tk.report.restservices;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.calypso.tk.refdata.DomainValues;

public class MarginCallDigitalSettlementsReportDataWS extends AbstractReportDataWS {

	private static Semaphore sem = new Semaphore(
			Integer.parseInt(DomainValues.comment("WS.MaxConcurrentReq", "MarginCallDigitalSettlements")), true);

	public MarginCallDigitalSettlementsReportDataWS(String reportType, String reportTemplate) {
		super(reportType, reportTemplate, "BOTransfer");
	}

	@Override
	boolean adquireSemaphore() throws InterruptedException {
		if (max_conc_req == 0) {
			return true;
		}
		return sem.tryAcquire(timeout, TimeUnit.SECONDS);
	}

	@Override
	void releaseSemaphore() {
		if (max_conc_req != 0) {
			sem.release();
		}

	}
}
