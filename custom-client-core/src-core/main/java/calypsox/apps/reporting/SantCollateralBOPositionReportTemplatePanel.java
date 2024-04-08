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
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

/**
 * Collateral Positions report, including cash and securities (bonds and equities). This report uses BOPosition for
 * MARGIN_CALL positions.
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 30/04/2015
 * 
 */
public class SantCollateralBOPositionReportTemplatePanel extends SantGenericReportTemplatePanel {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -810323607030298837L;

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
		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.removeAll();

		column2Panel.setLayout(new GridLayout(3, 1));
		column2Panel.add(this.agreementNamePanel);
		// GSM 20/07/15. SBNA Multi-PO filter
		column2Panel.add(this.poAgrPanel);

		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = new JPanel();
		return column3Panel;
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
	}

	@Override
	public void hideAllPanels() {
		super.hideAllPanels();
	}

	@Override
	public ReportTemplate getTemplate() {
		super.getTemplate();
		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);
	}

	@Override
	public void callBeforeLoad(ReportPanel panel) {
		ReportTemplate template = panel.getTemplate();
		template.callBeforeLoad();
	}

	public static void main(final String... pepe) throws ConnectException {

		final String args[] = { "-env", "dev4-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
		ConnectionUtil.connect(args, "SantCollateralBOPositionReportTemplatePanel");
		final JFrame frame = new JFrame();
		frame.setTitle("SantCollateralBOPositionReportTemplatePanel");
		frame.setContentPane(new SantCollateralBOPositionReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1273, 307));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
