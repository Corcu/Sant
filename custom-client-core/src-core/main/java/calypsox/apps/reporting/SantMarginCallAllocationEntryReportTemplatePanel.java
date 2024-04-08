/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Dimension;

import javax.swing.JFrame;

import com.calypso.apps.reporting.MarginCallAllocationEntryReportTemplatePanel;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;

/**
 * Adaptation of the MarginCallAllocationEntryReport to use multiple POs from STs. However you have a core panel that
 * can be used.
 * 
 * @author Guillermo Solano
 * @version 1.0
 */
public class SantMarginCallAllocationEntryReportTemplatePanel extends MarginCallAllocationEntryReportTemplatePanel {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -8953141002040646697L;

	/**
	 * Default constructr
	 */
	public SantMarginCallAllocationEntryReportTemplatePanel() {
		super();
	}

	public static void main(final String... argsss) throws Exception {

		String args[] = { "-env", "dev5-local", "-user", "nav_it_sup_tec", "-password", "calypso", "-nologingui" };
		final DSConnection ds = ConnectionUtil.connect(args, "MainEntry");
		DSConnection.setDefault(ds);
		final JFrame frame = new JFrame();
		frame.setContentPane(new SantMarginCallAllocationEntryReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}

}
