/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import calypsox.apps.reporting.util.ValueComparator;
import calypsox.apps.reporting.util.control.SantCheckBoxPanel;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.core.MarginCallConfigLight;
import calypsox.tk.report.SantInterestPaymentRunnerEntry;
import calypsox.tk.report.SantInterestPaymentRunnerReportTemplate;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.loader.CallAccountLoader;
import calypsox.tk.report.loader.IndexLoader;
import calypsox.apps.reporting.util.loader.MarginCallConfigLightLoader;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.FontChooser;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.BOInterestUtil;
import com.calypso.tk.util.TradeArray;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

public class SantInterestPaymentRunnerOldReportTemplatePanel extends ReportTemplatePanel implements ActionListener {

    private static final long serialVersionUID = 3923867147610534891L;

    protected Map<RateIndex, String> rateIndexMap;

    protected Map<Integer, String> marginCallContractIdsMap;

    protected Map<Integer, Account> callAccountsMap;

    protected Map<Integer, MarginCallConfigLight> mccLightsMap;

    protected Map<Integer, String> callAccountNamesMap;

    protected SantChooseButtonPanel agreementTypePanel;

    protected SantProcessDatePanel processDatePanel;

    protected SantChooseButtonPanel rateIndexPanel;

    protected SantChooseButtonPanel agreementNamePanel;

    protected SantChooseButtonPanel callAccountPanel;

    protected PaymentButtonPanel processPaymentBtn;

    protected SantChooseButtonPanel poAgrPanel;

    protected SantChooseButtonPanel ccyPanel;

    private ReportTemplate reportTemplate;

    private SantCheckBoxPanel selectAllCheckBox;

    public SantInterestPaymentRunnerOldReportTemplatePanel() {
        loadstaticData();
        init();
    }

    private void loadstaticData() {

        this.rateIndexMap = new IndexLoader().load();

        final MarginCallConfigLightLoader mccLightLoader = new MarginCallConfigLightLoader();
        this.marginCallContractIdsMap = mccLightLoader.load();

        final CallAccountLoader accountLoader = new CallAccountLoader();
        this.callAccountNamesMap = accountLoader.load();

        this.callAccountsMap = accountLoader.get();
        this.mccLightsMap = mccLightLoader.get();
    }

    protected void init() {
        buildControlsPanel();

        setSize(0, 150);
        final JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BorderLayout());
        masterPanel.setBorder(getMasterPanelBorder());
        add(masterPanel, BorderLayout.CENTER);

        masterPanel.add(this.processDatePanel, BorderLayout.NORTH);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 2));
        masterPanel.add(mainPanel, BorderLayout.CENTER);

        final JPanel column1Panel = getColumn1Panel();
        final JPanel column2Panel = getColumn2Panel();

        mainPanel.add(column1Panel);
        mainPanel.add(column2Panel);

        add(getEastPanel(), BorderLayout.EAST);

    }

    private JPanel getEastPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(this.processPaymentBtn, BorderLayout.NORTH);
        panel.add(this.selectAllCheckBox, BorderLayout.SOUTH);
        return panel;
    }

    protected void buildControlsPanel() {
        this.processDatePanel = new SantProcessDatePanel("Process");
        this.processDatePanel.removeDateLabel();
        this.rateIndexPanel = new SantChooseButtonPanel("Rate Index", this.rateIndexMap.values());
        this.agreementTypePanel = new SantChooseButtonPanel("Agr Type", "legalAgreementType");

        final ValueComparator bvc = new ValueComparator(this.marginCallContractIdsMap);
        final Map<Integer, String> sortedMap = new TreeMap<Integer, String>(bvc);
        sortedMap.putAll(this.marginCallContractIdsMap);
        this.agreementNamePanel = new SantChooseButtonPanel("Agr Name", sortedMap.values());

        this.callAccountPanel = new SantChooseButtonPanel("Call Account", this.callAccountNamesMap.values());

        this.ccyPanel = new SantChooseButtonPanel("Currency", LocalCache.getCurrencies());

        final SortedSet<String> sortedLegalEntities = new TreeSet<String>(
                BOCache.getLegalEntitieNamesForRole(DSConnection.getDefault(), LegalEntity.PROCESSINGORG));
        this.poAgrPanel = new SantChooseButtonPanel("Owner Agr", sortedLegalEntities);

        this.processPaymentBtn = new PaymentButtonPanel("Run Payments", this);

        this.selectAllCheckBox = new SantCheckBoxPanel("Select All");
        this.selectAllCheckBox.setValue(true);
        this.selectAllCheckBox.setListener("Select All", this);

    }

    protected Border getMasterPanelBorder() {

        // GSM 01/03/16 - Not working title font in java 6, modified to java 7
        TitledBorder border = BorderFactory.createTitledBorder("Interest Payment Runner");// new
        // TitledBorder(BorderFactory.createEtchedBorder(),
        // "");
        border.setTitleFont(FontChooser.F_B);
        border.setTitleColor(Color.BLUE);
        return border;
    }

    private JPanel getColumn1Panel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));
        panel.add(this.agreementNamePanel);
        panel.add(this.agreementTypePanel);
        panel.add(this.poAgrPanel);
        return panel;
    }

    private JPanel getColumn2Panel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));
        panel.add(this.callAccountPanel);
        panel.add(this.rateIndexPanel);
        panel.add(this.ccyPanel);
        return panel;
    }

    @Override
    public void setTemplate(final ReportTemplate template) {

        // GSM 24/02/2016 - check maps have been loaded
        if ((this.rateIndexMap == null) || (this.marginCallContractIdsMap == null) || (this.callAccountNamesMap == null)
                || (this.mccLightsMap == null)) {
            loadstaticData();
        }

        this.reportTemplate = template;
        this.processDatePanel.setTemplate(template);
        this.processDatePanel.write(template);

        // 03/08/15. SBNA Multi-PO filter
        this.poAgrPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);
        // this.poAgrPanel.setValue(this.reportTemplate,
        // SantInterestPaymentRunnerReportTemplate.OWNER_AGR);

        this.agreementNamePanel.setValue(this.reportTemplate, SantInterestPaymentRunnerReportTemplate.AGREEMENT_ID,
                this.marginCallContractIdsMap);

        this.agreementTypePanel.setValue(this.reportTemplate, SantInterestPaymentRunnerReportTemplate.AGREEMENT_TYPE);

        this.rateIndexPanel.setValue(this.reportTemplate, SantInterestPaymentRunnerReportTemplate.RATE_INDEX);

        this.callAccountPanel.setValue(this.reportTemplate, SantInterestPaymentRunnerReportTemplate.CALL_ACCOUNT_ID,
                this.callAccountNamesMap);

        this.ccyPanel.setValue(this.reportTemplate, SantInterestPaymentRunnerReportTemplate.CURRENCY);
    }

    @Override
    public ReportTemplate getTemplate() {
        this.processDatePanel.read(this.reportTemplate);

        String value = this.agreementNamePanel.getValue();
        this.reportTemplate.put(SantInterestPaymentRunnerReportTemplate.AGREEMENT_ID,
                getMultipleKey(value, this.marginCallContractIdsMap));

        value = this.callAccountPanel.getValue();
        this.reportTemplate.put(SantInterestPaymentRunnerReportTemplate.CALL_ACCOUNT_ID,
                getMultipleKey(value, this.callAccountNamesMap));

        this.reportTemplate.put(SantInterestPaymentRunnerReportTemplate.AGREEMENT_TYPE,
                this.agreementTypePanel.getValue());

        // 03/08/15. SBNA Multi-PO filter
        this.reportTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES, this.poAgrPanel.getValue());
        // this.reportTemplate.put(SantInterestPaymentRunnerReportTemplate.OWNER_AGR,
        // this.poAgrPanel.getValue());

        this.reportTemplate.put(SantInterestPaymentRunnerReportTemplate.RATE_INDEX, this.rateIndexPanel.getValue());

        this.reportTemplate.put(SantInterestPaymentRunnerReportTemplate.CURRENCY, this.ccyPanel.getValue());

        this.reportTemplate.put(SantInterestPaymentRunnerReportTemplate.ACCOUNT_MAP, this.callAccountsMap);

        this.reportTemplate.put(SantInterestPaymentRunnerReportTemplate.CONTRACT_MAP, this.mccLightsMap);

        return this.reportTemplate;
    }

    protected Object getMultipleKey(final String value, final Map<Integer, String> map) {
        final Vector<String> agreementNames = Util.string2Vector(value);
        final Vector<Integer> agreementIds = new Vector<Integer>();
        for (String agreementName : agreementNames) {
            agreementIds.add((Integer) getKey(agreementName, map));
        }
        return Util.collectionToString(agreementIds);
    }

    private Object getKey(final String value, final Map<Integer, String> map) {
        for (final Entry<Integer, String> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                return null;
            }
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Run Payments")) {
            runPayments();
        } else if (e.getActionCommand().equals("Select All")) {
            selectAll();
        }
    }

    private void selectAll() {
        Boolean selectAll = this.selectAllCheckBox.getValue();
        if (this._reportWindow.getReportPanel().getRowCount() <= 0) {
            return;
        }

        DefaultReportOutput reportOutput = this._reportWindow.getReportPanel().getOutput();
        ReportRow[] rows = reportOutput.getRows();
        for (ReportRow row : rows) {
            row.setProperty(SantInterestPaymentRunnerReportTemplate.SELECT_ALL_ROW_DATA, selectAll);
        }
        this._reportWindow.getReportPanel().refresh();
    }

    // AAP MIG 14.4 Added InterestBearing processing to generate a
    // SimpleTransfer insteadof a CustomerTransfer
    private synchronized void runPayments() {
        DefaultReportOutput reportOutput = this._reportWindow.getReportPanel().getOutput();
        if (reportOutput.getNumberOfDisplayedRows() <= 0) {
            JOptionPane.showMessageDialog(this._reportWindow, "No row to process", "", JOptionPane.PLAIN_MESSAGE);
            return;
        }

        List<Integer> filteredRowIds = new ArrayList<Integer>();
        for (int i = 0; i < reportOutput.getNumberOfDisplayedRows(); i++) {
            filteredRowIds.add(reportOutput.getRowConsideringRowFiltering(i));
        }

        ReportRow[] rows = reportOutput.getRows();

        JDate valDate = ((SantInterestPaymentRunnerEntry) rows[0]
                .getProperty(SantInterestPaymentRunnerReportTemplate.ROW_DATA)).getProcessDate();

        if (!AppUtil.displayQuestion(
                "Are you sure you want to perform notification of the selected rows on " + valDate.toString() + " ?",
                this._reportWindow)) {
            return;
        }

        // set time to 23:59 - ok for now might be changed one day to reflect
        // the real time creation
        JDatetime valDatetime = new JDatetime(valDate, TimeZone.getDefault());
        DSConnection ds = DSConnection.getDefault();
        String leRole = "CounterParty";
        LegalEntity le = null;
        BOInterestUtil util = new BOInterestUtil();
        StringBuilder processMsg = new StringBuilder();
        // AAP Creates AccountInterestUtil
        // MIG 16.1 commented out
        //AccountInterestLiquidation acl = new AccountInterestLiquidation("", valDatetime, TimeZone.getDefault());
        for (Integer filteredRowId : filteredRowIds) {
            ReportRow row = rows[filteredRowId];
            Boolean isSelected = (Boolean) row.getProperty(SantInterestPaymentRunnerReportTemplate.SELECT_ALL_ROW_DATA);
            if (!isSelected) {
                continue;
            }

            SantInterestPaymentRunnerEntry entry = (SantInterestPaymentRunnerEntry) row
                    .getProperty(SantInterestPaymentRunnerReportTemplate.ROW_DATA);

            // AD HOC are processed manually
            String adHoc = entry.getAccount().getAccountProperty("PayInterestAdHoc");
            if ("true".equalsIgnoreCase(adHoc)) {
                continue;
            }

            try {
                // AAP Unchecks isClient keyword
                // MIG 16.1 commented out
                //acl.setAccountId(String.valueOf(entry.getAccount().getId()));
                //acl.processGeneratedInterestBearing(ds, false);
                TradeArray trades = util.generateTradeTransferInterets(valDate, valDatetime, leRole, le,
                        entry.getAccount(), null, null, ds);
                if (!Util.isEmpty(trades)) {
                    long[] ids = ds.getRemoteTrade().saveTrades(new ExternalArray(trades.toVector()));
                    processMsg.append("Account Id [ ").append(entry.getAccount().getId())
                            .append(" ] - Cash notification transfers [ ");
                    for (Long id : ids) {
                        processMsg.append(" ").append(id);
                    }
                    processMsg.append(" ]\n");
                }
            } catch (Exception ex) {
                Log.error(this, ex);
            }
            // MIG 16.1 commented out
            /* finally {
                // AAP Checks isClient again
                try {
                    acl.processGeneratedInterestBearing(ds, true);
                    acl.setAccountId(null);
                    acl.flushAccountList();
                } catch (CalypsoServiceException e) {
                    Log.error(this, e.getMessage());
                    Log.error(this, e); //sonar
                }

            }
            */
        }
        if (processMsg.length() == 0) {
            processMsg.append("No Cash notification transfer generated for the selected accounts. \n");
        }
        JTextArea textArea = new JTextArea(processMsg.toString());
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setPreferredSize(new Dimension(400, 300));
        scroll.getHorizontalScrollBar().setVisible(true);
        scroll.getVerticalScrollBar().setVisible(true);
        textArea.setEditable(false);
        textArea.setBackground(scroll.getBackground());
        JOptionPane.showMessageDialog(this._reportWindow, scroll, "", JOptionPane.PLAIN_MESSAGE);

    }

    class PaymentButtonPanel extends JButton {

        private static final long serialVersionUID = 6498512575607484715L;

        protected JButton button = new JButton();

        public PaymentButtonPanel(String name, ActionListener l) {
            this.button.addActionListener(l);
            init(name);

        }

        private void init(String name) {
            this.button.setText(name);
            this.button.setActionCommand(name);
            this.button.setFont(new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
            setBackground(Color.BLACK);
            this.button.setForeground(Color.BLACK);
            Dimension dim = new Dimension(140, 50);
            this.button.setMinimumSize(dim);
            this.button.setPreferredSize(dim);
            this.button.setMaximumSize(dim);
            setMargin(new Insets(0, 0, 0, 0));
            add(this.button);
        }
    }

}
