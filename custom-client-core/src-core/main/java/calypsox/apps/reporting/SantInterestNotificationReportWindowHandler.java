/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import calypsox.tk.report.SantInterestNotificationReportTemplate;
import calypsox.tk.report.generic.loader.interestnotif.SantInterestNotificationEntry;
import com.calypso.apps.refdata.AccountFrame;
import com.calypso.apps.refdata.BOMarginCallConfigWindow;
import com.calypso.apps.reporting.ReportObjectHandler;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.reporting.ReportWindowHandlerAdapter;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class SantInterestNotificationReportWindowHandler extends ReportWindowHandlerAdapter implements ActionListener {

    @Override
    public JMenu getCustomMenu(ReportWindow window) {
        setReportWindow(window);
        JMenu showMenu = new JMenu("Show");

        JMenuItem tradeMenuItem = new JMenuItem("Trade");
        tradeMenuItem.setActionCommand("Trade");
        tradeMenuItem.addActionListener(this);

        JMenuItem accountMenuItem = new JMenuItem("Account");
        accountMenuItem.setActionCommand("Account");
        accountMenuItem.addActionListener(this);

        JMenuItem accountActivityMenuItem = new JMenuItem("Account Activity");
        accountActivityMenuItem.setActionCommand("Account Activity");
        accountActivityMenuItem.addActionListener(this);

        JMenuItem contractMenuItem = new JMenuItem("MarginCall Contract");
        contractMenuItem.setActionCommand("MarginCall Contract");
        contractMenuItem.addActionListener(this);

        showMenu.add(tradeMenuItem);
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

        Vector<SantInterestNotificationEntry> objectVector = this._reportWindow.getReportPanel().getSelectedObjects(
                SantInterestNotificationReportTemplate.ROW_DATA);
        if (objectVector.size() > 1) {
            AppUtil.displayWarning("Select at most one row", this._reportWindow);
        }
        if (objectVector.size() == 0) {
            AppUtil.displayWarning("Select at least one row", this._reportWindow);
        }

        final SantInterestNotificationEntry entry = objectVector.get(0);
        if (e.getActionCommand().equals("Trade")) {
            ReportObjectHandler.showTrade(entry.getTradeId());
        } else if (e.getActionCommand().equals("Account")) {
            AccountFrame win = new AccountFrame();
            Account account = BOCache.getAccount(DSConnection.getDefault(), entry.getCallAccountId());
            win.show(account);
            win.setVisible(true);
        } else if (e.getActionCommand().equals("Account Activity")) {
            Account account = BOCache.getAccount(DSConnection.getDefault(), entry.getCallAccountId());
            AccountFrame.showAccountActivityReport(account, this._reportWindow);
        } else if (e.getActionCommand().equals("MarginCall Contract")) {
            BOMarginCallConfigWindow win = new BOMarginCallConfigWindow();
            win.showMarginCallConfig(entry.getContractId());
            win.setVisible(true);
        }

    }

}
