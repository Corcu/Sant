package calypsox.tk.bo;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;

public interface ReporRowCacheAdapterInterface {

	public ReportRow[] getReportRows();

	public void preloadCache(DefaultReportOutput defaultReportOutput) throws CalypsoServiceException;
	public boolean newEvent(PSEvent event,String engineName) throws CalypsoServiceException;

	public void clear();
}
