/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;

public class SantAllDealsReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 123L;

	private SantChooseButtonPanel agreementStatusPanel;
	private SantProcessDatePanel processDatePanel;

	@Override
	protected Dimension getPanelSize() {
		return new Dimension(0, 160);
	}

	@Override
	public void setValDatetime(JDatetime valDatetime) {
		this.processDatePanel.setValDatetime(valDatetime);
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
		this.agreementStatusPanel = new SantChooseButtonPanel("Agr Status", "CollateralStatus");
		this.processDatePanel = new SantProcessDatePanel("Process");
		this.processDatePanel.customInitDomains(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);
		this.processDatePanel.removeDateLabel();
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("All Deals");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected JPanel getNorthPanel() {
		return this.processDatePanel;
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(4, 1));
		column1Panel.add(this.poAgrPanel);
		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(4, 1));
		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementStatusPanel);
		column2Panel.add(this.agreementTypePanel);
		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(4, 1));
		column3Panel.add(this.baseCcyPanel);
		return column3Panel;
	}

	@Override
	public ReportTemplate getTemplate() {
		super.getTemplate();

		this.reportTemplate.put(SantGenericTradeReportTemplate.AGREEMENT_STATUS, this.agreementStatusPanel.getValue());
		this.processDatePanel.read(this.reportTemplate);
		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);
		this.processDatePanel.setTemplate(template);
		this.processDatePanel.write(template);
		this.agreementStatusPanel.setValue(template, SantGenericTradeReportTemplate.AGREEMENT_STATUS);
	}
}
