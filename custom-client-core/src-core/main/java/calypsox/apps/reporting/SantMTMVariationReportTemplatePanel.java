/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import javax.swing.JPanel;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantCheckBoxPanel;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

import com.calypso.tk.report.ReportTemplate;

public class SantMTMVariationReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 123L;

	private SantCheckBoxPanel modifiedManuallyPanel;

	public SantMTMVariationReportTemplatePanel() {
		// this.modifiedManuallyPanel = new
		// SantCheckBoxPanel("Modified Manually",
		// 120);
		setPanelVisibility();
	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.tradeIdPanel.setLabelName("Ext Ref");
		this.tradeIdPanel.setVisible(true);
		this.poDealPanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.cptyPanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		// this.processStartEndDatePanel.setDateLabelName("Val Date");
		this.processStartEndDatePanel.setVisible(true);
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3 = super.getColumn3Panel();
		column3.removeAll();
		// this.modifiedManuallyPanel.setBorder(new LineBorder(Color.red));
		// this.modifiedManuallyPanel.setPreferredSize(new Dimension(300, 25));
		this.modifiedManuallyPanel = new SantCheckBoxPanel("Modified Manually", 120);
		column3.add(this.modifiedManuallyPanel);

		return column3;
	}

	@Override
	public ReportTemplate getTemplate() {
		final ReportTemplate template = super.getTemplate();
		template.put(SantGenericTradeReportTemplate.MANUALLY_MODIFIED, this.modifiedManuallyPanel.getValue());
		return template;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);
		this.modifiedManuallyPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.MANUALLY_MODIFIED);
	}

	// public static void main(final String... args) throws ConnectException {
	// final DSConnection ds = ConnectionUtil.connect(args,
	// "SantMTMVariationReportTemplatePanel");
	// final JFrame frame = new JFrame();
	// frame.setContentPane(new SantMTMVariationReportTemplatePanel());
	// frame.setVisible(true);
	// frame.setSize(new Dimension(1173, 307));
	// }

}
