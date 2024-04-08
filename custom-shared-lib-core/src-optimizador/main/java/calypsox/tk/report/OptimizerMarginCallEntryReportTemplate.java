package calypsox.tk.report;

import java.text.SimpleDateFormat;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.report.MarginCallEntryReportTemplate;

public class OptimizerMarginCallEntryReportTemplate extends
		MarginCallEntryReportTemplate {

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	/**
	 * 
	 */
	private static final long serialVersionUID = -7997296564678705048L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		JDatetime currentDate = new JDatetime();
		String currentDateSt = sdf.format(currentDate);
		put(PROCESS_START_DATE, currentDateSt);
		put(PROCESS_END_DATE, currentDateSt);
	}
}
