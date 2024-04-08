package calypsox.apps.reporting;

/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.apps.reporting.util.control.SantTextFieldPanel;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;

public class SantDelinquentMarginCallReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = -5846753312511311974L;

	private SantProcessDatePanel processDatePanel;

	private SantTextFieldPanel delinquentThreshold;

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Delinquent Margin Call");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected Dimension getPanelSize() {
		return new Dimension(0, 135);
	}

	@Override
	protected JPanel getNorthPanel() {
		return this.processDatePanel;
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
		this.delinquentThreshold = new SantTextFieldPanel("Threshold");
		this.processDatePanel = new SantProcessDatePanel("Process");
		this.processDatePanel.customInitDomains(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);
		this.processDatePanel.removeDateLabel();
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(3, 1));
		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.delinquentThreshold);
		AppUtil.addNumberListener(this.delinquentThreshold.getJTextField(), 0);

		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(3, 1));

		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);

		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(3, 1));
		column3Panel.add(this.economicSectorPanel);

		return column3Panel;
	}

	@Override
	public ReportTemplate getTemplate() {
		ReportTemplate template = super.getTemplate();
		template.put(SantGenericTradeReportTemplate.DELINQUENT_THRESHOLD, this.delinquentThreshold.getDoubleValue());
		this.processDatePanel.read(this.reportTemplate);
		return template;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		super.setTemplate(template);
		this.delinquentThreshold.setValue(this.reportTemplate, SantGenericTradeReportTemplate.DELINQUENT_THRESHOLD);
		this.processDatePanel.setTemplate(template);
		this.processDatePanel.write(template);
	}
}
