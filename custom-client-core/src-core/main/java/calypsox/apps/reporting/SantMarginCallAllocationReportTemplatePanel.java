/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantMultiPOSelectorReportTemplatePanel;

/**
 * Adaptation of the MarginCallAllocationReport to use multiple POs
 * 
 * @author Guillermo Solano
 * @version 1.0
 */
public class SantMarginCallAllocationReportTemplatePanel extends SantMultiPOSelectorReportTemplatePanel {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -3886160446992911362L;

	/**
	 * Default constructr
	 */
	public SantMarginCallAllocationReportTemplatePanel() {
		super();
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("SantMarginCallAllocationReport");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	// public static void main(final String... argsss) throws Exception {
	//
	// String args[] = { "-env", "dev5-local", "-user", "nav_it_sup_tec", "-password", "calypso", "-nologingui" };
	// final DSConnection ds = ConnectionUtil.connect(args, "MainEntry");
	// DSConnection.setDefault(ds);
	// final JFrame frame = new JFrame();
	// frame.setContentPane(new SantMarginCallAllocationReportTemplatePanel());
	// frame.setVisible(true);
	// frame.setSize(new Dimension(1173, 307));
	// }

}
