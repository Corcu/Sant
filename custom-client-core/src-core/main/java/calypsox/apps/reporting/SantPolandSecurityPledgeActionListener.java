package calypsox.apps.reporting;

import calypsox.tk.report.SantPolandSecurityPledgeReport;
import calypsox.tk.report.SantPolandSecurityPledgeReportStyle;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.*;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * This ActionListener listens to events from the "Select All" and
 * "Unselect All" checkboxes and the "Reverse Trades" button.
 *
 * @author Carlos Cejudo
 */
public class SantPolandSecurityPledgeActionListener implements ActionListener {

    public static final String ACTION_SELECT_ALL = "ACTION_SELECT_ALL";
    public static final String ACTION_UNSELECT_ALL = "ACTION_UNSELECT_ALL";
    public static final String ACTION_REVERSE_TRADES = "ACTION_REVERSE_TRADES";

    private static final String LABEL_BEGIN = "<html><div align=\"center\">";
    private static final String LABEL_END = "</div></html>";

    private static final String LABEL_INFO = "The following trades will be reversed:";
    private static final String LABEL_QUESTION = "Do you wish to continue?";
    private static final String LABEL_ERROR = "The following trades could not be reversed:";
    private static final String LABEL_SUCCESS = "All trades reversed succesfully";

    protected ReportTemplatePanel templatePanel;
    protected JCheckBox selectAllCheckBox;
    protected JCheckBox unselectAllCheckBox;

    /**
     * Constructs the ActionListener.
     *
     * @param templatePanel       The TemplatePanel that contains the checkboxes and the button.
     * @param selectAllCheckBox   The "Select All" checkbox.
     * @param unselectAllCheckBox The "Unselect All" checkbox.
     */
    public SantPolandSecurityPledgeActionListener(
            ReportTemplatePanel templatePanel, JCheckBox selectAllCheckBox,
            JCheckBox unselectAllCheckBox) {
        this.templatePanel = templatePanel;
        this.selectAllCheckBox = selectAllCheckBox;
        this.unselectAllCheckBox = unselectAllCheckBox;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String action = event.getActionCommand();

        if (ACTION_SELECT_ALL.equals(action)) {
            selectAll();
        } else if (ACTION_UNSELECT_ALL.equals(action)) {
            unselectAll();
        } else if (ACTION_REVERSE_TRADES.equals(action)) {
            reverseTrades();
        }
    }

    /**
     * Mark all trades in the table as selected.
     */
    private void selectAll() {
        if (selectAllCheckBox.isSelected()) {
            setReverseMarkToAllRows(Boolean.TRUE);
            unselectAllCheckBox.setSelected(false);
        }
    }

    /**
     * Mark all trades in the table as not selected.
     */
    private void unselectAll() {
        if (unselectAllCheckBox.isSelected()) {
            setReverseMarkToAllRows(Boolean.FALSE);
            selectAllCheckBox.setSelected(false);
        }
    }

    /**
     * Sets the reverse mark of all rows in the table to the same given value.
     *
     * @param value A Boolean with the value to be set.
     */
    private void setReverseMarkToAllRows(Boolean value) {
        ReportRow[] reportRows = getReportRows();

        int reverseMarkColumn = SantPolandSecurityPledgeUtil.findColumnIndex(
                this.templatePanel.getReportWindow(),
                SantPolandSecurityPledgeReportStyle.COLUMN_NAME_REVERSE);
        JTable table = this.templatePanel.getReportWindow().getReportPanel()
                .getTableModelWithFocus().getTable();

        // We set the properties for each row and update the mark in the table
        // separately, because the logical position of the report row does not
        // necessarily match the position of that row in the table drawn on
        // screen.
        for (int iRow = 0; iRow < reportRows.length; iRow++) {
            // Set the ReportRow property
            reportRows[iRow].setProperty(
                    SantPolandSecurityPledgeReport.PROPERTY_REVERSE_MARK,
                    value);

        }
        for (int iRow = 0; iRow < reportRows.length; iRow++) {
            // Update mark in the table
            table.setValueAt(value, iRow, reverseMarkColumn);
        }

        this.templatePanel.getReportWindow().getReportPanel()
                .getTableModelWithFocus().fireTableDataChanged();
    }

    /**
     * Returns an array with all ReportRows in the table.
     *
     * @return An array of ReportRows.
     */
    private ReportRow[] getReportRows() {
        return this.templatePanel.getReportWindow().getReportPanel().getOutput()
                .getRows();
    }

    /**
     * Reverses every marked trade in the table.
     */
    private void reverseTrades() {
        List<Long> tradesToReverse = getTradesToReverse();

        if (!Util.isEmpty(tradesToReverse)) {
            // Asks the user for confirmation, showing a list of all trades that
            // will be reversed.
            int userResponse = JOptionPane.showConfirmDialog(
                    this.templatePanel.getReportWindow(),
                    getDialogContent(tradesToReverse), "Reverse trades",
                    JOptionPane.YES_NO_OPTION);
            if (userResponse == JOptionPane.YES_OPTION) {
                List<Long> tradesFailed = new ArrayList<>();

                // Uses the report's valuation date as the processing date for
                // the reverse trades.
                JDate processDate = this.templatePanel.getReportWindow()
                        .getValDatetime().getJDate(TimeZone.getDefault());
                for (Long tradeId : tradesToReverse) {
                    boolean tradeReversedOK = reverseTrade(tradeId,
                            processDate);
                    if (!tradeReversedOK) {
                        tradesFailed.add(tradeId);
                    }
                }
                // Unselect all trades
                unselectAll();

                if (!Util.isEmpty(tradesFailed)) {
                    // Shows dialog in case of error.
                    JOptionPane.showMessageDialog(
                            this.templatePanel.getReportWindow(),
                            getErrorDialogContent(tradesFailed), "Error",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    // Shows message when all selected trades have been reversed
                    // successfully.
                    JOptionPane.showMessageDialog(
                            this.templatePanel.getReportWindow(),
                            getSuccessDialogContent(), "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                }

                this.templatePanel.getReportWindow().getReportPanel()
                        .refresh(true);
            }
        } else {
            // Show message if no trades where selected.
            JOptionPane.showMessageDialog(this.templatePanel.getReportWindow(),
                    "No trades selected", "No trades selected",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Gets every trade that has been marked to be reversed.
     *
     * @return A List containing the trade ids of every trade selected.
     */
    private List<Long> getTradesToReverse() {
        List<Long> tradeIds = new ArrayList<>();

        ReportRow[] reportRows = getReportRows();
        for (int iRow = 0; iRow < reportRows.length; iRow++) {
            ReportRow row = reportRows[iRow];

            if (SantPolandSecurityPledgeUtil.getReverseMark(row)) {
                Object rawTrade = row.getProperty(ReportRow.DEFAULT);
                if (rawTrade instanceof Trade) {
                    Trade trade = (Trade) rawTrade;
                    tradeIds.add(trade.getLongId());
                }
            }
        }

        return tradeIds;
    }

    /**
     * Reverses the given trade with the given process date.
     *
     * @param tradeId     Id of the trade to be reversed.
     * @param processDate Process Date of the reverse. This date will be used to set
     *                    both the trade date and the settlement date of the new reverse
     *                    trade.
     * @return <code>true</code> if the trade was reversed correctly and
     * <code>false</code> otherwise.
     */
    private boolean reverseTrade(long tradeId, JDate processDate) {
        boolean tradeReversedOK = false;
        DSConnection ds = DSConnection.getDefault();

        try {
            Trade trade = ds.getRemoteTrade().getTrade(tradeId);

            if (SantPolandSecurityPledgeUtil.canPerformAction(trade,
                    Action.AMEND)
                    && Util.isEmpty(trade.getKeywordValue(
                    SantPolandSecurityPledgeUtil.TRADE_KEYWORD_REVERSE_TO))) {
                Trade revTrade = SantPolandSecurityPledgeUtil
                        .getReverseTrade(trade, processDate);
                long revTradeId = ds.getRemoteTrade().save(revTrade);

                trade = SantPolandSecurityPledgeUtil.setReversedTo(trade,
                        revTradeId);
                ds.getRemoteTrade().save(trade);

                tradeReversedOK = true;
            } else {
                Log.error(this,
                        String.format("Cannot perform action %s on trade %d",
                                Action.S_AMEND, tradeId));
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, String
                    .format("Error while trying to reverse trade %d", tradeId));
            Log.error(this, e); //sonar
        }

        return tradeReversedOK;
    }

    /**
     * Builds the content of the dialog that informs the user of the trades that
     * will be reversed.
     *
     * @param tradesToReverse List of trades to be reversed.
     * @return The content of the dialog.
     */
    private Component getDialogContent(List<Long> tradesToReverse) {
        JPanel panel = new JPanel();

        JLabel infoLabel = new JLabel(LABEL_BEGIN + LABEL_INFO + LABEL_END);

        StringBuilder listOfTrades = new StringBuilder();
        for (int iTrade = 0; iTrade < tradesToReverse.size(); iTrade++) {
            if (iTrade > 0) {
                listOfTrades.append('\n');
            }
            listOfTrades.append(tradesToReverse.get(iTrade));
        }
        JTextPane listOfTradesPane = new JTextPane();
        listOfTradesPane.setText(listOfTrades.toString());
        listOfTradesPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(listOfTradesPane);

        JLabel questionLabel = new JLabel(
                LABEL_BEGIN + LABEL_QUESTION + LABEL_END);

        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        panel.add(infoLabel);
        panel.add(scrollPane);
        panel.add(questionLabel);

        panel.setPreferredSize(new Dimension(200, 400));

        return panel;
    }

    /**
     * Builds the content of the dialog that informs the user about the trades
     * that could not be reversed.
     *
     * @param tradesFailed A list of the trades that could not be reversed.
     * @return The content of the dialog.
     */
    private Component getErrorDialogContent(List<Long> tradesFailed) {
        JPanel panel = new JPanel();

        JLabel infoLabel = new JLabel(LABEL_BEGIN + LABEL_ERROR + LABEL_END);

        StringBuilder listOfTrades = new StringBuilder();
        for (int iTrade = 0; iTrade < tradesFailed.size(); iTrade++) {
            if (iTrade > 0) {
                listOfTrades.append('\n');
            }
            listOfTrades.append(tradesFailed.get(iTrade));
        }
        JTextPane listOfTradesPane = new JTextPane();
        listOfTradesPane.setText(listOfTrades.toString());
        listOfTradesPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(listOfTradesPane);

        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        panel.add(infoLabel);
        panel.add(scrollPane);

        panel.setPreferredSize(new Dimension(200, 400));

        return panel;
    }

    /**
     * Builds the content of the dialog that informs the user that every
     * selected trade has been reversed successfully.
     *
     * @return The content of the dialog.
     */
    private Component getSuccessDialogContent() {
        JPanel panel = new JPanel();
        JLabel infoLabel = new JLabel(LABEL_BEGIN + LABEL_SUCCESS + LABEL_END);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(infoLabel);

        return panel;
    }

}
