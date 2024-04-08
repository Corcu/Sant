/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import calypsox.tk.report.SantInterestPaymentRunnerEntry;
import calypsox.tk.report.SantInterestPaymentRunnerReportTemplate;
import com.calypso.apps.refdata.AccountFrame;
import com.calypso.apps.refdata.BOMarginCallConfigWindow;
import com.calypso.apps.reporting.ReportObjectHandler;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.reporting.ReportWindowHandlerAdapter;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class SantInterestPaymentRunnerOldReportWindowHandler extends ReportWindowHandlerAdapter implements ActionListener {

	@Override
	public JMenu getCustomMenu(ReportWindow window) {
		setReportWindow(window);
		JMenu showMenu = new JMenu("Show");

		JMenuItem ibMenuItem = new JMenuItem("Interest Bearing");
		ibMenuItem.setActionCommand("Interest Bearing");
		ibMenuItem.addActionListener(this);

		JMenuItem simpleXferMenuItem = new JMenuItem("Notification");
		simpleXferMenuItem.setActionCommand("Notification");
		simpleXferMenuItem.addActionListener(this);

		JMenuItem accountMenuItem = new JMenuItem("Account");
		accountMenuItem.setActionCommand("Account");
		accountMenuItem.addActionListener(this);

		JMenuItem accountActivityMenuItem = new JMenuItem("Account Activity");
		accountActivityMenuItem.setActionCommand("Account Activity");
		accountActivityMenuItem.addActionListener(this);

		JMenuItem contractMenuItem = new JMenuItem("MarginCall Contract");
		contractMenuItem.setActionCommand("MarginCall Contract");
		contractMenuItem.addActionListener(this);

		showMenu.add(ibMenuItem);
		showMenu.add(simpleXferMenuItem);
		showMenu.add(accountMenuItem);
		showMenu.add(accountActivityMenuItem);
		showMenu.add(contractMenuItem);

		return showMenu;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(final ActionEvent e) {
		if (this._reportWindow == null) {
			return;
		}

		if (this._reportWindow.getReportPanel().getRowCount() <= 0) {
			return;
		}

		Vector<SantInterestPaymentRunnerEntry> objectVector = this._reportWindow.getReportPanel().getSelectedObjects(
				SantInterestPaymentRunnerReportTemplate.ROW_DATA);
		if (objectVector.size() > 1) {
			AppUtil.displayWarning("Select at most one row", this._reportWindow);
		}
		if (objectVector.size() == 0) {
			AppUtil.displayWarning("Select at least one row", this._reportWindow);
		}

		final SantInterestPaymentRunnerEntry entry = objectVector.get(0);
		if (e.getActionCommand().equals("Interest Bearing")) {
			if (entry.getIbTrade() != null) {
				ReportObjectHandler.showTrade(entry.getIbTrade());
			}
		} else if (e.getActionCommand().equals("Notification")) {
			if (entry.getSimpleXferTrade() != null) {
				ReportObjectHandler.showTrade(entry.getSimpleXferTrade());
			}
		} else if (e.getActionCommand().equals("Account")) {
			AccountFrame win = new AccountFrame();
			Account account = BOCache.getAccount(DSConnection.getDefault(), entry.getAccount().getId());
			win.show(account);
			win.setVisible(true);
		} else if (e.getActionCommand().equals("Account Activity")) {
			Account account = BOCache.getAccount(DSConnection.getDefault(), entry.getAccount().getId());
			AccountFrame.showAccountActivityReport(account, this._reportWindow);
		} else if (e.getActionCommand().equals("MarginCall Contract")) {
			BOMarginCallConfigWindow win = new BOMarginCallConfigWindow();
			Integer contractId = getContractId(entry.getAccount());
			if (contractId != null) {
				win.showMarginCallConfig(contractId);
			}
			win.setVisible(true);
		}

	}

	private Integer getContractId(Account account) {
		String mccIdStr = account.getAccountProperty("MARGIN_CALL_CONTRACT");

		if (Util.isEmpty(mccIdStr)) {
			return null;
		}

		try {
			return Integer.parseInt(mccIdStr);
		} catch (Exception e) {
			Log.error(this, e); //sonar
		}
		return null;
	}

}
