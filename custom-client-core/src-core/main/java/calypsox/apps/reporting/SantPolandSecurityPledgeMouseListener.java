package calypsox.apps.reporting;

import calypsox.tk.report.SantPolandSecurityPledgeReport;
import calypsox.tk.report.SantPolandSecurityPledgeReportStyle;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.ReportRow;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * This MouseAdapter listens to the events produced when the user selects or
 * excludes a trade by clicking on its mark in the table.
 *
 * @author Carlos Cejudo
 */
public class SantPolandSecurityPledgeMouseListener extends MouseAdapter {

    private ReportTemplatePanel templatePanel;
    protected JCheckBox selectAllCheckBox;
    protected JCheckBox unselectAllCheckBox;

    /**
     * Constructor for this MouseListener.
     *
     * @param templatePanel       The ReportTemplatePanel of the current report.
     * @param selectAllCheckBox   "Select All" checkbox.
     * @param unselectAllCheckBox "Unselect All" checkbox.
     */
    public SantPolandSecurityPledgeMouseListener(
            ReportTemplatePanel templatePanel, JCheckBox selectAllCheckBox,
            JCheckBox unselectAllCheckBox) {
        this.templatePanel = templatePanel;
        this.selectAllCheckBox = selectAllCheckBox;
        this.unselectAllCheckBox = unselectAllCheckBox;
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 1) {
            JTable table = this.templatePanel.getReportWindow().getReportPanel()
                    .getTableModelWithFocus().getTable();
            int reverseMarkColumn = SantPolandSecurityPledgeUtil
                    .findColumnIndex(this.templatePanel.getReportWindow(),
                            SantPolandSecurityPledgeReportStyle.COLUMN_NAME_REVERSE);

            int selectedCol = table.getSelectedColumn();
            if (reverseMarkColumn == selectedCol) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    long tradeId = getTradeIdInRow(table, selectedRow);
                    ReportRow row = getReportRowWithTradeId(
                            this.templatePanel.getReportWindow()
                                    .getReportPanel().getOutput().getRows(),
                            tradeId);
                    if (row != null) {
                        // Toggle reverse mark on selected row
                        row.setProperty(
                                SantPolandSecurityPledgeReport.PROPERTY_REVERSE_MARK,
                                !SantPolandSecurityPledgeUtil
                                        .getReverseMark(row));
                        SantPolandSecurityPledgeUtil.updateReverseMark(row,
                                table, selectedCol, selectedRow);

                        // Uncheck "Select All" and "Unselect All" checkboxes
                        this.selectAllCheckBox.setSelected(false);
                        this.unselectAllCheckBox.setSelected(false);

                        // Update table view
                        this.templatePanel.getReportWindow().getReportPanel()
                                .getTableModelWithFocus()
                                .fireTableDataChanged();
                    }
                }
            }
        }
    }

    /**
     * Retrieves the trade id of the trade represented in the given row of the
     * table.
     *
     * @param table Graphical representation of the table.
     * @param row   Position of the row in the graphical representation.
     * @return The id of the trade from the given row.
     */
    private Long getTradeIdInRow(JTable table, int row) {
        int tradeIdColumn = SantPolandSecurityPledgeUtil.findColumnIndex(
                this.templatePanel.getReportWindow(),
                SantPolandSecurityPledgeReportStyle.TRADE_ID);
        Object rawTradeId = table.getValueAt(row, tradeIdColumn);

        long tradeId = 0;
        if (rawTradeId instanceof Long) {
            tradeId = (Long) rawTradeId;
        }

        return tradeId;
    }

    /**
     * Gets the ReportRow that contains the trade with the given id.
     *
     * @param reportRows Array of all ReportRows in the report.
     * @param tradeId    Id of the trade we are looking for.
     * @return The ReportRow containing the given trade.
     */
    private ReportRow getReportRowWithTradeId(ReportRow[] reportRows,
                                              long tradeId) {
        ReportRow rowToReturn = null;
        boolean rowFound = false;
        for (int iRow = 0; !rowFound && iRow < reportRows.length; iRow++) {
            ReportRow row = reportRows[iRow];
            Object rawTrade = row.getProperty(ReportRow.DEFAULT);
            if (rawTrade instanceof Trade) {
                Trade trade = (Trade) rawTrade;
                if (tradeId == trade.getLongId()) {
                    rowFound = true;
                    rowToReturn = row;
                }
            }
        }

        return rowToReturn;
    }

}
