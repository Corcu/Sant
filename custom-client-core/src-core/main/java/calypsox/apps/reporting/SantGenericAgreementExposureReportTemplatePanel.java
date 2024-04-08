/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantLegalEntityPanel;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.ConnectException;

public class SantGenericAgreementExposureReportTemplatePanel extends SantGenericReportTemplatePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SantGenericAgreementExposureReportTemplatePanel() {
		setPanelVisibility();
	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.processStartEndDatePanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.valuationPanel.setVisible(true);
		this.economicSectorPanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
		this.poDealPanel.setVisible(true);
		this.matureDealsPanel.setVisible(true);
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Agreement Exposure");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected SantLegalEntityPanel getCounterPartyPanel() {
		return new SantLegalEntityPanel(LegalEntity.FUND, "Fund", false, true, true, true);
	}

	@Override
	public ReportTemplate getTemplate() {
		return super.getTemplate();
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);
	}

	public static void main(final String... args) throws ConnectException {
		final JFrame frame = new JFrame();
		frame.setContentPane(new SantRiskParametersReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}
}
