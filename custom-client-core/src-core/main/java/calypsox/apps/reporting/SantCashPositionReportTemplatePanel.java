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

import com.calypso.tk.report.ReportTemplate;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;

public class SantCashPositionReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 6870788484899868652L;

	@Override
	protected Dimension getPanelSize() {
		return new Dimension(0, 135);
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Cash Position");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(3, 1));
		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.cptyPanel);
		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		// column2Panel.setLayout(new GridLayout(3, 1));
		// column2Panel.add(this.callAccountPanel);
		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(3, 1));

		column3Panel.add(this.portfolioPanel);
		column3Panel.add(this.baseCcyPanel);
		return column3Panel;
	}

	@Override
	public ReportTemplate getTemplate() {
		super.getTemplate();

		// This code is from SantFiPositionReportTemplatePanel.java
		// BEGIN
//		if (!Util.isEmpty(this.poAgrPanel.getLE())) {
//			final String shortName = this.poDealPanel.getTextField().getText();
//			this.reportTemplate.put("ProcessingOrg", shortName);
//		} else {
//			this.reportTemplate.remove("ProcessingOrg");
//		}

//		final String value = this.portfolioPanel.getValue();
//		this.reportTemplate.put("INV_POS_BOOKLIST", value);

		this.reportTemplate.put("POSITION_DATE", "Trade");
		this.reportTemplate.put("POSITION_CLASS", "Margin_Call");
		this.reportTemplate.put("POSITION_TYPE", "Actual,Not settled");
		this.reportTemplate.put("AGGREGATION", "Book/Agent/Account");
		this.reportTemplate.put("CASH_SECURITY", "Cash");
		this.reportTemplate.put("INV_POS_MOVE", "Balance");
		// END

		// Select currency
		//this.reportTemplate.put("INV_POS_CURLIST", this.baseCcyPanel.getValue());

		// Do not filter zero values
		this.reportTemplate.put("INV_FILTER_ZERO", "false");

		return this.reportTemplate;
	}

	// @Override
	// public void setTemplate(final ReportTemplate template) {
	// super.setTemplate(template);
	// this.callAccountPanel.setValue(this.reportTemplate, CALL_ACCOUNT);
	// }

}
