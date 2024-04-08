package calypsox.apps.reporting;

import calypsox.tk.collateral.allocation.importer.AllocationsImporterWindow;
import calypsox.tk.collateral.allocation.importer.TripartyAgreedAmountWindow;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.report.ReportRow;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.net.URL;

/**
 * @author aela
 */
public class MarginCallReportWindowHandler extends com.calypso.apps.reporting.MarginCallReportWindowHandler {

    private static final String ALLOC_ICON_PATH = "/calypsox/apps/icons/allocImport.png";

    @Override
    public JMenu getCustomMenu(ReportWindow window) {

        ActionListener actionListenerAlloc = new CustomImportAllocActionListener();
        ActionListener actionListenerTriparty = new CustomImportTripartyAgreedAmountListener();
        JMenu menu = super.getCustomMenu(window);
        if (menu == null) {
            menu = new JMenu();
        }
        menu.add(createNewMenuItem("Import allocations", "importAllocs", actionListenerAlloc));
        menu.add(createNewMenuItem("Import Triparty Agreed Amount", "importTriparty", actionListenerTriparty));

        return menu;

    }

    /**
     * @param menuName
     * @param resourcePath
     * @param actionCommand
     * @param listener
     * @return
     */
    private JMenuItem createNewMenuItem(String menuName, String actionCommand, ActionListener listener) {
        URL iconurl = this.getClass().getResource(ALLOC_ICON_PATH);
        JMenuItem menuItem;
        if (iconurl != null) {
            menuItem = new JMenuItem(menuName, new ImageIcon(iconurl));
        } else {
            menuItem = new JMenuItem(menuName);
        }
        menuItem.setActionCommand(actionCommand);
        menuItem.addActionListener(listener);
        return menuItem;

    }

    /**
     *
     */
    protected class CustomImportAllocActionListener implements ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent event) {
            ReportPanel panel = getReportPanel();
            ReportRow[] selectedRows = panel.getSelectedReportRows();

            if ((selectedRows != null)) {
                if (selectedRows.length > 1) {
                    AppUtil.displayWarning(panel, "Only one margin call can be selected.");
                    return;
                } else if (selectedRows.length == 0) {
                    AppUtil.displayWarning(panel, "Please select a margin call.");
                    return;
                }
                for (ReportRow row : selectedRows) {
                    Object entryObject = row.getProperty("MarginCallEntry");
                    if (entryObject instanceof MarginCallEntry) {
                        MarginCallEntry entry = (MarginCallEntry) entryObject;
                        AllocationsImporterWindow window = new AllocationsImporterWindow(entry);
                        window.setVisible(true);
                    }
                }
            } else {
                AppUtil.displayWarning(panel, "Please select a margin call.");
                return;
            }
        }
    }

    /**
     *
     */
    protected class CustomImportTripartyAgreedAmountListener implements ActionListener {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent event) {
            ReportPanel panel = getReportPanel();
            ReportRow[] selectedRows = panel.getSelectedReportRows();

            if ((selectedRows != null)) {
                if (selectedRows.length > 1) {
                    AppUtil.displayWarning(panel, "Only one margin call can be selected.");
                    return;
                } else if (selectedRows.length == 0) {
                    AppUtil.displayWarning(panel, "Please select a margin call.");
                    return;
                }
                for (ReportRow row : selectedRows) {
                    Object entryObject = row.getProperty("MarginCallEntry");
                    if (entryObject instanceof MarginCallEntry) {
                        MarginCallEntry entry = (MarginCallEntry) entryObject;
                        TripartyAgreedAmountWindow window = new TripartyAgreedAmountWindow(entry);
                        window.setVisible(true);
                    }
                }
            } else {
                AppUtil.displayWarning(panel, "Please select a margin call.");
                return;
            }
        }
    }

}
