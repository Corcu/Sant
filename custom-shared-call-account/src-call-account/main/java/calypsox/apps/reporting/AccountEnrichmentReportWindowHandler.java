package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportWindow;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Vector;

public class AccountEnrichmentReportWindowHandler extends com.calypso.apps.reporting.AccountEnrichmentReportWindowHandler implements ActionListener {


    @Override
    public JMenu getCustomMenu(ReportWindow window) {
        final JMenu customMenu = super.getCustomMenu(window);
        JMenuItem ibMenuItem = new JMenuItem("Resend");
        ibMenuItem.setActionCommand("Resend Cres");
        ibMenuItem.addActionListener(this);

        customMenu.add(ibMenuItem);

        return customMenu;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (this._reportWindow == null) {
            return;
        }

        if (this._reportWindow.getReportPanel().getRowCount() <= 0) {
            return;
        }
        Vector objectVector = this._reportWindow.getReportPanel().getSelectedObjects(
                "BOCre");
        Arrays.stream(objectVector.toArray()).parallel().forEach(cre ->{
            if(cre instanceof BOCre){
                BOCre creToSend = (BOCre) cre;
                try {
                    DSConnection.getDefault().getRemoteBO().save(((BOCre) cre),0L,null);
                } catch (CalypsoServiceException exe) {
                    Log.error(this, "Error sending Cres: " + exe.getCause().getMessage());
                }
            }
        });
    }
}
