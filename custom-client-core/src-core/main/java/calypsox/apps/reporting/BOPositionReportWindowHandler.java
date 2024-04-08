/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import calypsox.tk.report.SantAgreementListByISINReportTemplate;

import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;

public class BOPositionReportWindowHandler extends com.calypso.apps.reporting.BOSecurityPositionReportWindowHandler
		implements ActionListener {

	@Override
	public JMenu getCustomMenu(ReportWindow window) {
		// setReportWindow(window);
		JMenu customMenu = super.getCustomMenu(true);

		JMenuItem agreementListByISIN = new JMenuItem("SantAgreementList By ISIN Report");
		agreementListByISIN.setActionCommand("SantAgreementListByISINReport");
		agreementListByISIN.addActionListener(this);

		customMenu.add(agreementListByISIN);

		return customMenu;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(final ActionEvent e) {
		if (this.getReportWindow() == null) {
			return;
		}

		if (this.getReportWindow().getReportPanel().getRowCount() <= 0) {
			return;
		}

		if (e.getActionCommand().equals("SantAgreementListByISINReport")) {
			Vector<Inventory> objectVector = this.getReportWindow().getReportPanel()
					.getSelectedObjects(ReportRow.INVENTORY);

			if (objectVector.size() > 1) {
				AppUtil.displayWarning("Select at most one row", this.getReportWindow());
			}
			if (objectVector.size() == 0) {
				AppUtil.displayWarning("Select at least one row", this.getReportWindow());
			}

			final Inventory entry = objectVector.get(0);
			ReportWindow reportWin = new ReportWindow("SantAgreementListByISIN");
			ReportTemplate template = reportWin.getTemplate();
			Bond bond = (Bond) entry.getProduct();
			String isin = bond.getSecCode("ISIN");
			template.put(SantAgreementListByISINReportTemplate.ISIN, isin);

			reportWin.setVisible(true);
			reportWin.load();

		}

	}

}
