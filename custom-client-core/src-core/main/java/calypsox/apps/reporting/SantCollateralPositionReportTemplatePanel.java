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
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.tk.report.SantCollateralPositionReport;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.tk.report.ReportTemplate;

public class SantCollateralPositionReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = -810323607030298837L;

	public final static String ALLOCATION_STATUS = "ALLOCATION_STATUS";

	private SantComboBoxPanel<Integer, String> allocationStatusPanel;

	private static Vector<String> allocationStatusValues = new Vector<String>();
	static {
		allocationStatusValues.add("");
		allocationStatusValues.add(SantCollateralPositionReport.ALLOCATION_STATUS_HELD);
		allocationStatusValues.add(SantCollateralPositionReport.ALLOCATION_STATUS_IN_TRANSIT);
	}

	@Override
	protected Dimension getPanelSize() {
		return new Dimension(0, 160);
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Collateral Position");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(4, 1));
		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.allocationStatusPanel);
		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(4, 1));
		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);
		column2Panel.add(this.valuationPanel);
		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(4, 1));
		column3Panel.add(this.baseCcyPanel);
		return column3Panel;
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
		this.allocationStatusPanel = new SantComboBoxPanel<Integer, String>("Status", allocationStatusValues);
		this.allocationStatusPanel.setValue("");
	}

	@Override
	public void hideAllPanels() {
		super.hideAllPanels();

		this.allocationStatusPanel.setVisible(false);
	}

	@Override
	public ReportTemplate getTemplate() {
		super.getTemplate();
		this.reportTemplate.put(ALLOCATION_STATUS, this.allocationStatusPanel.getValue());
		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);
		this.allocationStatusPanel.setValue(this.reportTemplate, ALLOCATION_STATUS);
	}

	@Override
	public void callBeforeLoad(ReportPanel panel) {
		ReportTemplate template = panel.getTemplate();
		template.callBeforeLoad();
	}

}
