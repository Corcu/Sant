package calypsox.apps.reporting;

import calypsox.tk.report.InitialMarginContractConfigConstants;
import calypsox.tk.report.InitialMarginContractConfigReportStyle;
import com.calypso.apps.reporting.MarginCallEntryReportTemplatePanel;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.TableModelUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

/**
 * qef margin call entry template panel
 *
 * @author xIS15793
 */
public class InitialMarginContractConfigReportTemplatePanel extends MarginCallEntryReportTemplatePanel {
    /**
     * click mouse adapter
     */
    private MouseAdapter mouseAdapterClick;

    /**
     * serial version id
     */
    private static final long serialVersionUID = 8343573245338021777L;

    public InitialMarginContractConfigReportTemplatePanel() {
        GridLayout gridLayoutMain = new GridLayout(1, 1, 2, 2);
        setLayout(gridLayoutMain);

        // adding Qef Specific panel to the report template panel
        InitialMarginContractConfigFilterPanel qefFilterPanel = new InitialMarginContractConfigFilterPanel();
        qefFilterPanel.setReportTemplatePanel(this);
        this.add(qefFilterPanel, BorderLayout.WEST);

        // asociate clic event to the table
        initMouseAdapter();
    }

    /**
     * Change the value of the check box 'Send' when clicking on the cell
     */
    void initMouseAdapter() {
        this.mouseAdapterClick = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 1) {
                    JTable table = getReportWindow().getReportPanel().getTableModel().getTable();
                    int colSend = findColumnIndex(getReportWindow(), InitialMarginContractConfigReportStyle.SEND_FIELD);

                    int selectedCol = table.getSelectedColumn();
                    if (colSend == selectedCol) {
                        int selectedRow = table.getSelectedRow();
                        if (selectedRow >= 0) {
                            table.setValueAt(!(Boolean) table.getValueAt(selectedRow, selectedCol), selectedRow, selectedCol);
                            getReportWindow().getReportPanel().getOutput().getRows()[selectedRow].setProperty(InitialMarginContractConfigConstants.QEF_PROPERTY, (Boolean) table.getValueAt(selectedRow, selectedCol));
                            getReportWindow().getReportPanel().getTableModel().fireTableDataChanged();
                        }
                    }
                }
            }
        };
    }

    /**
     * adding the mouse publisher to the table
     *
     * @param panel panel to associate the publisher
     */
    @Override
    public void callAfterDisplay(final ReportPanel panel) {
        TableModelUtil m = panel.getTableModel();
        JTable table = m.getTable();
        if (!Arrays.asList(table.getMouseListeners()).contains(this.mouseAdapterClick)) {
            table.addMouseListener(this.mouseAdapterClick);
        }
    }

    /**
     * Function to retrieve Column index
     *
     * @param reportWindow window
     * @param name         name of the column
     * @return index index of the column
     */
    static int findColumnIndex(ReportWindow reportWindow, String name) {
        int col = reportWindow.getReportPanel().getTableModel().findColumn(name);
        if (col == -1) {
            // look for filtered column
            col = reportWindow.getReportPanel().getTableModel().findColumn(name + "(F)");
        }
        return col;
    }

}
