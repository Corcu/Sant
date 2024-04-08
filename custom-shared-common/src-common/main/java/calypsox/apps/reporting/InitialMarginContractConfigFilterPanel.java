package calypsox.apps.reporting;

import calypsox.apps.reporting.util.loader.MarginCallConfigLightLoader;
import calypsox.tk.event.PSEventSantInitialMarginExport;
import calypsox.tk.report.InitialMarginContractConfigConstants;
import calypsox.tk.report.InitialMarginContractConfigReportStyle;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.tk.collateral.MarginCallEntryFacade;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallEntryBaseReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Qef partial panel to include on the margin call entry report
 *
 * @author xIS15793
 */
public class InitialMarginContractConfigFilterPanel extends JPanel implements InitialMarginContractConfigConstants {

    /**
     * serial version id
     */
    private static final long serialVersionUID = 7024823440214405396L;

    /**
     * qef panel title
     */
    private static final String QEF_PANEL_TITLE = "Qef";

    /**
     * send button text
     */
    private static final String QEF_CONTRACTS = "Send to QEF Engine";

    /**
     * name of action for select all check box
     */
    private static final String ACTION_SELECT_ALL = "ACTION_SELECT_ALL";

    /**
     * name of action for unselect all check box
     */
    private static final String ACTION_UNSELECT_ALL = "ACTION_UNSELECT_ALL";

    /**
     * action for send to qef engine button
     */
    private static final String ACTION_SEND_CONTRACTS = "ACTION_SEND_CONTRACTS";
    /**
     * initial msg to log
     */
    private static final String SENDING_ENTRIES_TO_QEF = "Sending contracts to Qef...";

    // panels
    private JPanel infoPanel = new JPanel();
    private JPanel selectPanel = new JPanel();
    private JPanel qefContractsPanel = new JPanel();

    // Info fields
    private JTextArea infoTextArea = new JTextArea();

    // Select fields
    private CalypsoCheckBox selectAllCheckBox = new CalypsoCheckBox();
    private JLabel selectAllLabel = new JLabel();
    private CalypsoCheckBox unSelectAllCheckBox = new CalypsoCheckBox();
    private JLabel unSelectAllLabel = new JLabel();

    // Send to Qef button
    private JButton sendContractsButton = new JButton(QEF_CONTRACTS);

    private transient DefaultActionListener defaultActionListener = null;
    private ReportTemplatePanel reportTemplatePanel = null;
    private Map<Integer, String> contractMap;

    /**
     * Constructor
     */
    public InitialMarginContractConfigFilterPanel() {
        setBorder(new TitledBorder(new EtchedBorder(1, null, null), QEF_PANEL_TITLE, 4, 2, null, null));
        setLayout(new GridBagLayout());

        initInfoPanel();
        initSelectPanel();
        initQefContractPanel();

        loadContracts();
    }

    public InitialMarginContractConfigFilterPanel(ReportTemplatePanel reportTemplatePanel) {
        super();
    }

    /**
     * info panel to include useful log for the users
     */
    private void initInfoPanel() {
        GridLayout gridLayoutInfo = new GridLayout(1, 1, 5, 5);
        this.infoPanel.setLayout(gridLayoutInfo);

        /* begin first row */
        BoxLayout blInfoPanel = new BoxLayout(this.infoPanel, BoxLayout.X_AXIS);
        this.infoPanel.setLayout(blInfoPanel);

        // start infoLabel properties
        this.infoTextArea.setText("");
        this.infoTextArea.setEditable(false);
        this.infoTextArea.setEnabled(true);
        JScrollPane sp = new JScrollPane(this.infoTextArea);
        // end infoLabel properties
        this.infoPanel.add(sp);
        this.infoPanel.setBorder(BorderFactory.createEtchedBorder());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0.7;
        c.gridx = 0;
        c.gridy = 0;

        /* end first row */
        this.add(this.infoPanel, c);
    }

    private void initSelectPanel() {
        GridLayout gridLayoutSelect = new GridLayout(1, 1, 1, 1);

        this.selectPanel.setBounds(new Rectangle(1, 4, 150, 24));
        this.selectPanel.setLayout(gridLayoutSelect);

        /* begin first row */
        BoxLayout blSelectPanel = new BoxLayout(this.selectPanel, BoxLayout.X_AXIS);
        this.selectPanel.setLayout(blSelectPanel);
        this.selectPanel.add(Box.createHorizontalStrut(20));

        // start selectAllCheckBox properties
        this.selectAllCheckBox.setText("");
        this.selectAllCheckBox.setPreferredSize(new Dimension(20, 24));
        this.selectAllCheckBox.setMaximumSize(new Dimension(20, 24));
        this.selectAllCheckBox.setMinimumSize(new Dimension(10, 24));
        this.selectAllCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        this.selectAllCheckBox.setActionCommand(ACTION_SELECT_ALL);
        this.selectAllCheckBox.addActionListener(getDefaultActionListener());
        // end selectAllCheckBox properties
        this.selectPanel.add(this.selectAllCheckBox);

        // start selectAllLabel properties
        this.selectAllLabel.setText("Select All");
        this.selectAllLabel.setPreferredSize(new Dimension(150, 24));
        this.selectAllLabel.setMaximumSize(new Dimension(150, 24));
        this.selectAllLabel.setMinimumSize(new Dimension(120, 24));
        this.selectAllLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        // end selectAllLabel properties
        this.selectPanel.add(this.selectAllLabel);

        // start unSelectAllCheckBox properties
        this.unSelectAllCheckBox.setText("");
        this.unSelectAllCheckBox.setPreferredSize(new Dimension(20, 24));
        this.unSelectAllCheckBox.setMaximumSize(new Dimension(20, 24));
        this.unSelectAllCheckBox.setMinimumSize(new Dimension(10, 24));
        this.unSelectAllCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        this.unSelectAllCheckBox.setActionCommand(ACTION_UNSELECT_ALL);
        this.unSelectAllCheckBox.addActionListener(getDefaultActionListener());
        // end unSelectAllCheckBox properties
        this.selectPanel.add(this.unSelectAllCheckBox);

        // start unSelectAllLabel properties
        this.unSelectAllLabel.setText("Unselect All");
        this.unSelectAllLabel.setPreferredSize(new Dimension(150, 24));
        this.unSelectAllLabel.setMaximumSize(new Dimension(150, 24));
        this.unSelectAllLabel.setMinimumSize(new Dimension(120, 24));
        this.unSelectAllLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        // end unSelectAllLabel properties
        this.selectPanel.add(this.unSelectAllLabel);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0.15;
        c.gridx = 0;
        c.gridy = 1;

        /* end first row */
        this.add(this.selectPanel, c);
    }

    private void initQefContractPanel() {
        GridLayout gridLayoutOptimizeContracts = new GridLayout(1, 1, 1, 1);

        this.qefContractsPanel.setBounds(new Rectangle(1, 1, 150, 24));
        this.qefContractsPanel.setLayout(gridLayoutOptimizeContracts);

        /* begin first row */
        BoxLayout blOptimizeContractsPanel = new BoxLayout(this.qefContractsPanel, BoxLayout.X_AXIS);
        this.qefContractsPanel.setLayout(blOptimizeContractsPanel);
        this.qefContractsPanel.add(Box.createHorizontalStrut(20));

        // start sendContractsButton properties
        this.sendContractsButton.setText(QEF_CONTRACTS);
        this.sendContractsButton.setPreferredSize(new Dimension(150, 24));
        this.sendContractsButton.setMaximumSize(new Dimension(150, 24));
        this.sendContractsButton.setMinimumSize(new Dimension(120, 24));
        this.sendContractsButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        // end optimizeContractsButton properties
        this.sendContractsButton.setActionCommand(ACTION_SEND_CONTRACTS);
        this.sendContractsButton.addActionListener(getDefaultActionListener());
        this.qefContractsPanel.add(this.sendContractsButton);
        this.qefContractsPanel.add(Box.createHorizontalStrut(10));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0.15;
        c.gridx = 0;
        c.gridy = 2;

        /* end first row */
        this.add(this.qefContractsPanel, c);

    }

    private void loadContracts() {
        this.contractMap = new MarginCallConfigLightLoader().load();
    }

    public Map<Integer, String> getContractMap() {
        return this.contractMap;
    }

    public void setContractMap(Map<Integer, String> contractsMap) {
        this.contractMap = contractsMap;
    }

    public ReportTemplatePanel getReportTemplatePanel() {
        return this.reportTemplatePanel;
    }

    public void setReportTemplatePanel(ReportTemplatePanel reportTemplatePanel) {
        this.reportTemplatePanel = reportTemplatePanel;
    }

    private void addMessageInfo(String message) {
        this.infoTextArea.append(message + "\n");

        this.infoTextArea.update(this.infoTextArea.getGraphics());
    }

    private void clearMessageInfo() {
        this.infoTextArea.setText("");

        this.infoTextArea.update(this.infoTextArea.getGraphics());
    }

    private DefaultActionListener getDefaultActionListener() {
        if (this.defaultActionListener == null) {
            this.defaultActionListener = new DefaultActionListener();
        }
        return this.defaultActionListener;
    }

    /**
     * Listener for select/unselect checkBox & send contracts to Qef Engine
     *
     * @author xIS15793
     */
    private class DefaultActionListener implements ActionListener {


        private DefaultActionListener() {
            super();
        }

        @SuppressWarnings({"deprecation"})
        @Override
        public void actionPerformed(ActionEvent e) {
            String action = e.getActionCommand();

            if (ACTION_SELECT_ALL.equals(action)) {

                clearSendFlag();

                if (InitialMarginContractConfigFilterPanel.this.selectAllCheckBox.getSelectedObjects() != null) {
                    InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getTemplate().put(QEF_SELECT_ALL,
                            SELECT_TRUE);
                    InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getTemplate().put(QEF_UNSELECT_ALL,
                            "");
                    InitialMarginContractConfigFilterPanel.this.unSelectAllCheckBox.setSelected(false);
                } else {
                    InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getTemplate().put(QEF_SELECT_ALL,
                            "");
                }

                // call ReportStyle getColumnValue
                InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getReportWindow().getReportPanel()
                        .refresh();
            } else if (ACTION_UNSELECT_ALL.equals(action)) {

                clearSendFlag();

                if (InitialMarginContractConfigFilterPanel.this.unSelectAllCheckBox.getSelectedObjects() != null) {
                    InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getTemplate().put(QEF_UNSELECT_ALL,
                            SELECT_TRUE);
                    InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getTemplate().put(QEF_SELECT_ALL,
                            "");
                    InitialMarginContractConfigFilterPanel.this.selectAllCheckBox.setSelected(false);
                } else {
                    InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getTemplate().put(QEF_UNSELECT_ALL,
                            "");
                }

                // call ReportStyle getColumnValue
                InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getReportWindow().getReportPanel()
                        .refresh();
            } else if (ACTION_SEND_CONTRACTS.equals(action)) {
                clearMessageInfo();
                addMessageInfo(SENDING_ENTRIES_TO_QEF);

                // get output
                DefaultReportOutput reportOutput = InitialMarginContractConfigFilterPanel.this.reportTemplatePanel
                        .getReportWindow().getReportPanel().getOutput();

                if ((reportOutput != null) && (reportOutput.getRows() != null)) {
                    JTable table = InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getReportWindow()
                            .getReportPanel().getTableModel().getTable();

                    ReportRow[] rows = reportOutput.getRows();

                    List<Integer> entriesIds = new ArrayList<>();

                    // check every row send flag
                    for (int i = 0; i < InitialMarginContractConfigFilterPanel.this.reportTemplatePanel
                            .getReportWindow().getReportPanel().getTableModel().getTable().getRowCount(); i++) {

                        JDate processDate = null;
                        boolean sendFlag = (Boolean) table
                                .getValueAt(i,
                                        InitialMarginContractConfigReportTemplatePanel.findColumnIndex(
                                                InitialMarginContractConfigFilterPanel.this.reportTemplatePanel
                                                        .getReportWindow(),
                                                InitialMarginContractConfigReportStyle.SEND_FIELD));

                        if (sendFlag) {
                            entriesIds.add((Integer) table.getValueAt(i, InitialMarginContractConfigReportTemplatePanel.findColumnIndex(
                                    InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getReportWindow(),
                                    MarginCallEntryBaseReportStyle.ID)));

                            // check for the column
                            int contractIdColIndex = InitialMarginContractConfigReportTemplatePanel.findColumnIndex(
                                    InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getReportWindow(),
                                    "MarginCallConfig." + MarginCallEntryBaseReportStyle.CONTRACT_ID);

                            CollateralConfig marginCallConfig = null;
                            MarginCallEntryFacade entry = rows[i].getProperty("Default");

                            if (entry.getProcessDate() != null) {
                                processDate = entry.getProcessDate();
                            } else {
                                processDate = JDate.getNow();
                            }

                            // if column doesn't exist, load contract from the entry
                            if (contractIdColIndex == -1) {

                                if (entry != null) {
                                    marginCallConfig = CacheCollateralClient.getCollateralConfig(
                                            DSConnection.getDefault(), entry.getCollateralConfigId());
                                }
                            } else {
                                // if exists, get it from the row
                                marginCallConfig = rows[i].getProperty("MarginCallConfig");
                            }

                            if (marginCallConfig != null) {

                                // publish all events
                                try {
                                    // Create custom PSEvent
                                    PSEventSantInitialMarginExport newEvent = new PSEventSantInitialMarginExport(marginCallConfig.getId(), processDate, entry.getId());
                                    addMessageInfo("Event created for Qef Engine - Margin Call Config Id: " + marginCallConfig.getId() + ".");

                                    DSConnection.getDefault().getRemoteTrade().saveAndPublish(newEvent);
                                }  catch (RemoteException ex) {
                                    Log.error(this, "Couldn't publish the MarginCallQef Events: " + ex.getMessage());
                                    addMessageInfo("Problems ocurred when publishing the Margin Call Qef events. Please, Try again...");
                                }
                            }
                        }
                    }

                    //Reload for see the Attributte Sent marked
                    InitialMarginContractConfigFilterPanel.this.reportTemplatePanel.getReportWindow().load();

                } else {
                    addMessageInfo("No contract selected - doing nothing.");
                }
            }
        }
    }

    private void clearSendFlag() {
        DefaultReportOutput reportOutput = getReportTemplatePanel().getReportWindow().getReportPanel().getOutput();
        if (reportOutput == null) {
            return;
        }
        ReportRow[] reportRow = reportOutput.getRows();
        if (reportRow == null) {
            return;
        }
        for (int i = 0; i < reportRow.length; i++) {
            if (reportRow[i] == null) {
                continue;
            }
            reportRow[i].setProperty(QEF_PROPERTY, "");
        }
    }
}