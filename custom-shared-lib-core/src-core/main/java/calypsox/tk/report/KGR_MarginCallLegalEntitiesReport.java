/**
 * 
 */
package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jfree.util.Log;

import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

/**
 * @author aalonsop
 *
 *         Loads template-filtered Margin Calls and shows its po's,its counter
 *         parties or its products and exposure types.
 */
public class KGR_MarginCallLegalEntitiesReport extends CollateralConfigReport {

	private static final long serialVersionUID = -8575972192302652850L;
	private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput load(Vector errorMsgs) {
		DefaultReportOutput output = new DefaultReportOutput(this);
		// Important call
		((KGR_MarginCallLegalEntitiesReportTemplate) getReportTemplate()).createTemplateType();
		setReportTemplateColumns();
		List<CollateralConfig> contracts = super.loadMarginCallConfigs(this.getReportTemplate());
		final List<ReportRow> rows = new ArrayList<ReportRow>();
 		ExecutorService exec = Executors.newFixedThreadPool(NUM_CORES);
		try {
			// Multithread execution
			for (final CollateralConfig marginCall : contracts) {
				exec.submit(new Runnable() {
					@Override
					public void run() {
						try {
							rows.addAll(getReportRows(marginCall));
						} catch (CloneNotSupportedException e) {
							Log.error("Exception while setting ReportRow", e);
						}
					}
				});
			}
		} finally {
			exec.shutdown();
		}
		output.setRows(rows.toArray(new ReportRow[0]));
		return output;
	}

	/*
	 * Initialize template columns
	 */
	private void setReportTemplateColumns() {
		Vector<String> columns = new Vector<String>();
		((KGR_MarginCallLegalEntitiesReportTemplate) getReportTemplate()).templateType.setColumns(columns);
		getReportTemplate().setColumns((String[]) (String[]) columns.toArray(new String[columns.size()]));
	}

	/**
	 * Retrieves all rows Calls template types to retrieve the required
	 * information in each case
	 * 
	 * @return
	 * @throws CloneNotSupportedException
	 */
	private List<ReportRow> getReportRows(CollateralConfig marginCall) throws CloneNotSupportedException {
		List<ReportRow> rows = new ArrayList<ReportRow>();
		// rows.add(new ReportRow(marginCall,
		// KGR_MarginCallLegalEntitiesReportTemplate.CONTRACT));
		// Gets the required data and fills rows List
		((KGR_MarginCallLegalEntitiesReportTemplate) getReportTemplate()).templateType.getReportRows(marginCall, rows);
		return rows;

	}
}
