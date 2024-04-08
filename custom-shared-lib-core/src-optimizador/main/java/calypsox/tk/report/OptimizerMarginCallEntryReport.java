package calypsox.tk.report;

import java.util.Arrays;
import java.util.Vector;

import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallEntryReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.report.ReportTemplate;

import calypsox.tk.util.OptimizerMarginCallEntryConstants;

public class OptimizerMarginCallEntryReport extends MarginCallEntryReport implements OptimizerMarginCallEntryConstants {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8400164165915183868L;

	private DefaultReportOutput reportOutput = null;

	public DefaultReportOutput getReportOutput() {
		return reportOutput;
	}

	public void setReportOutput(DefaultReportOutput reportOutput) {
		this.reportOutput = reportOutput;
	}

	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") Vector errorMsgs) {
		DefaultReportOutput tmpReportOutput = (DefaultReportOutput) super
				.load(errorMsgs);
		if (tmpReportOutput == null || tmpReportOutput.getRows() == null) {
			return tmpReportOutput;
		}
		setReportOutput(tmpReportOutput);
		return tmpReportOutput;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ReportTemplate getReportTemplate() {
		if (reportOutput != null) {
			ReportStyle reportStyle = reportOutput.getStyle();
			if (reportStyle != null) {
				reportStyle
						.setProperty(
								OPTIMIZE_UNSELECT_ALL,
								_reportTemplate
										.get(OPTIMIZE_UNSELECT_ALL));
				reportStyle
						.setProperty(
								OPTIMIZE_SELECT_ALL,
								_reportTemplate
										.get(OPTIMIZE_SELECT_ALL));
			}
		}
		ReportTemplate reportTemplate = super.getReportTemplate();		
		if (reportTemplate != null && reportTemplate.getColumns() != null) {
			if (!Arrays.asList(reportTemplate.getColumns()).contains(ID_MARGIN_CALL)) {
				Vector<String> columns = new Vector<String>();
				columns.addAll(Arrays.asList(reportTemplate.getColumns()));
				columns.addElement(ID_MARGIN_CALL);
				reportTemplate.setColumns((String[])(String[])columns.toArray(new String[columns.size()]));
			}
		}
		return reportTemplate;
	}
}
