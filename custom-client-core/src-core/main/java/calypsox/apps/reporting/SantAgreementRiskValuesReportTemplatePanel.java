package calypsox.apps.reporting;

import javax.swing.JPanel;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;

import com.calypso.tk.report.ReportTemplate;

/*
 * Sets the Process Date in the GUI 
 */
public class SantAgreementRiskValuesReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 123L;

	protected SantProcessDatePanel processDatePanel;

	public SantAgreementRiskValuesReportTemplatePanel() {
		hideAllPanels();
		this.poAgrPanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3 = super.getColumn3Panel();
		column3.removeAll();

		this.processDatePanel = new SantProcessDatePanel("Process");
		column3.add(this.processDatePanel);

		return column3;
	}

	@Override
	public ReportTemplate getTemplate() {
		ReportTemplate template = super.getTemplate();
		this.processDatePanel.read(this.reportTemplate);
		return template;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		super.setTemplate(template);
		this.processDatePanel.setTemplate(template);
		this.processDatePanel.write(template);
	}

}
