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

public class SantDailyTaskReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = -8078762688218801875L;

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Daily Task");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected Dimension getPanelSize() {
		return new Dimension(0, 185);
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(5, 1));
		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.cptyPanel);
		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(5, 1));
		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);
		column2Panel.add(this.agreementStatusPanel);
		column2Panel.add(this.valuationPanel);
		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(5, 1));
		column3Panel.add(this.headCloneIndicatorPanel);
		return column3Panel;
	}
}
