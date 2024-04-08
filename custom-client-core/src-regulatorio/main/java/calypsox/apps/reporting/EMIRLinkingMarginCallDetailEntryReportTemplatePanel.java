package calypsox.apps.reporting;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.calypso.tk.report.ReportTemplate;

import calypsox.apps.reporting.util.EmirTemplatePanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.tk.report.EMIRLinkingMarginCallDetailEntryReportTemplate;

public class EMIRLinkingMarginCallDetailEntryReportTemplatePanel extends EmirTemplatePanel {

	protected SantComboBoxPanel<Integer, String> reportTypePanel;

	/** Serial Version ID */
	private static final long serialVersionUID = 1L;

	private static final String FULL = "Full";
	private static final String DELTA = "Delta";

	public EMIRLinkingMarginCallDetailEntryReportTemplatePanel() {
		// super();
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
		this.reportTypePanel = new SantComboBoxPanel<Integer, String>("Linking Report Type", getReportType());
	}

	@Override
	protected JPanel getColumn1Panel() {
		JPanel column = super.getColumn1Panel();
//		column.remove(new JPanel());
		column.add(this.reportTypePanel);
		return column;
	}

	@Override
	public ReportTemplate getTemplate() {
		this.reportTemplate = super.getTemplate();
		this.reportTemplate.put(EMIRLinkingMarginCallDetailEntryReportTemplate.REPORT_TYPES,
				this.reportTypePanel.getValue());
		return this.reportTemplate;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		super.setTemplate(template);
		this.reportTypePanel.setValue(this.reportTemplate, EMIRLinkingMarginCallDetailEntryReportTemplate.REPORT_TYPES);
	}

	/**
	 * 
	 * @return list with different report types
	 */
	private List<String> getReportType() {
		List<String> reportTypes = new ArrayList<String>();
		reportTypes.add(FULL);
		reportTypes.add(DELTA);
		return reportTypes;
	}

}