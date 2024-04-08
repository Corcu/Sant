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
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.report.ReportTemplate;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.apps.reporting.util.control.SantTextFieldPanel;

public class SantPortfolioBreakdownReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = -8078762688218801875L;

	protected SantProcessDatePanel processDatePanel;
	public static final String FX_ENV = "FX_ENV";
	private SantTextFieldPanel pricingEnvFx;
	
	@Override
	public void setValDatetime(JDatetime valDatetime) {
		this.processDatePanel.setValDatetime(valDatetime);
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
		this.processDatePanel = new SantProcessDatePanel("Value Date");
		this.processDatePanel.setPreferredSize(new Dimension(70, 24), new Dimension(215, 24));
		this.processDatePanel.removeDateLabel();
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Portfolio Breakdown");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	public ReportTemplate getTemplate() {
		ReportTemplate template = super.getTemplate();
		this.processDatePanel.read(this.reportTemplate);
		template.put(FX_ENV, this.pricingEnvFx.getValue());
		return template;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		super.setTemplate(template);
		this.processDatePanel.setTemplate(template);
		this.processDatePanel.write(template);
		String fxenv = template.get(FX_ENV);
		if(fxenv != null) {
			this.pricingEnvFx.setValue(fxenv);
		}
	}

	@Override
	protected JPanel getNorthPanel() {
		return this.processDatePanel;
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(5, 1));
		column1Panel.add(new JLabel());
		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.poDealPanel);
		column1Panel.add(this.tradeStatusPanel);
		column1Panel.add(this.instrumentTypePanel);

		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(5, 1));
		column2Panel.add(new JLabel());
		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);
		column2Panel.add(this.valuationPanel);
		column2Panel.add(this.valuationPanel);
		this.pricingEnvFx = new SantTextFieldPanel("FX PricingEnv");
		column2Panel.add(this.pricingEnvFx);
		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {

		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(5, 1));
		column3Panel.add(new JLabel());
		this.checkBoxPanel = new JPanel();
		final Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(this.matureDealsPanel);
		this.matureDealsPanel.setVisible(false);
		box.add(Box.createRigidArea(new Dimension(5, 0)));
		box.add(this.mtmZeroPanel);
		this.checkBoxPanel.add(box);
		column3Panel.add(this.checkBoxPanel);
	
		this.checkBoxPanel.setPreferredSize(this.economicSectorPanel.getPreferredSize());
		return column3Panel;
	}
}
