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
import calypsox.apps.reporting.util.loader.CallAccountLoader;
import calypsox.apps.reporting.util.loader.IndexLoader;
import calypsox.apps.reporting.util.loader.MarginCallConfigLightLoader;
import calypsox.tk.core.MarginCallConfigLight;
import calypsox.tk.report.SantInterestPaymentRunnerEntry;
import calypsox.tk.report.SantInterestPaymentRunnerReportTemplate;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.FontChooser;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.BOInterestUtil;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TradeArray;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class SantInterestPaymentRunnerReportTemplatePanel extends ReportTemplatePanel implements ActionListener {

    private static final long serialVersionUID = 3923867147610534891L;
    private MouseAdapter mouseAdapterClick;

    private static final String GENERATE_BTN = "Generate Trade";
    private static final String SEND_STATEMENT_BTN = "Send Statement";
    private static final String SEND_PAYMENTS_BTN = "Send Payments";

    public static final String  PAYMENTMSG = "PAYMENTMSG";
    public static final String  MC_INTEREST = "MC_INTEREST";

    protected Map<RateIndex, String> rateIndexMap = new HashMap<>();

    protected Map<Integer, String> marginCallContractIdsMap = new HashMap<>();

    protected Map<Integer, Account> callAccountsMap;

    protected Map<Integer, MarginCallConfigLight> mccLightsMap;

    protected Map<Integer, String> callAccountNamesMap = new HashMap<>();

    protected SantChooseButtonPanel agreementTypePanel;

    protected SantProcessDatePanel processDatePanel;

    protected SantChooseButtonPanel rateIndexPanel;

    protected SantChooseButtonPanel agreementNamePanel;

    protected SantChooseButtonPanel callAccountPanel;

    protected PaymentButtonPanel processPaymentBtn;
    protected PaymentButtonPanel generateTradeBtn;
    protected PaymentButtonPanel sendStatementBtn;
    protected PaymentButtonPanel sendPaymentBtn;


    protected SantChooseButtonPanel poAgrPanel;

    protected SantChooseButtonPanel ccyPanel;

    private ReportTemplate reportTemplate;

    private SantCheckBoxPanel selectAllCheckBox;

    private SantInterestPaymentactionPerformed interestPaymentActionListener = new SantInterestPaymentactionPerformed();

    public SantInterestPaymentRunnerReportTemplatePanel() {
        loadstaticData();
        init();
//        final JTable table = getReportWindow().getReportPanel().getTableModelWithFocus().getTable();
//        System.out.println("TEST");
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
        add(masterPanel, BorderLayout.WEST);

        masterPanel.add(this.processDatePanel, BorderLayout.NORTH);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 2));
        masterPanel.add(mainPanel, BorderLayout.WEST);

        final JPanel column1Panel = getColumn1Panel();
        final JPanel column2Panel = getColumn2Panel();

        mainPanel.add(column1Panel);
        mainPanel.add(column2Panel);

        final JPanel eastPanel = new JPanel();
        masterPanel.add(eastPanel, BorderLayout.EAST);

        eastPanel.add(getColumn1Panel2());
        /*eastPanel.add(getColumn2Panel2());*/ //Remove buttons send statement && send payment

       // add(getEastPanel(), BorderLayout.EAST);

    }

/*    private JPanel getEastPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(this.processPaymentBtn, BorderLayout.NORTH);
        panel.add(this.generateTradeBtn, BorderLayout.WEST);
        panel.add(this.sendStatementBtn, BorderLayout.CENTER);
        panel.add(this.sendPaymentBtn, BorderLayout.EAST);
        panel.add(this.selectAllCheckBox, BorderLayout.SOUTH);
        return panel;
    }*/

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

        this.generateTradeBtn = new PaymentButtonPanel(GENERATE_BTN, this);
        this.sendStatementBtn = new PaymentButtonPanel(SEND_STATEMENT_BTN, this);
        this.sendPaymentBtn = new PaymentButtonPanel(SEND_PAYMENTS_BTN, this);

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

    private JPanel getColumn1Panel2(){
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));
//        panel.add(this.processPaymentBtn);
        panel.add(this.generateTradeBtn);
        panel.add(this.selectAllCheckBox);
        return panel;
    }

    private JPanel getColumn2Panel2(){
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1));
        panel.add(this.sendStatementBtn);
        panel.add(this.sendPaymentBtn);
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
        } else if (e.getActionCommand().equals("Generate Trade")) {
            generateTradeCT();
        } else if (e.getActionCommand().equals("Send Statement")) {
            sendStatement();
        } else if (e.getActionCommand().equals("Send Payments")) {
            sendPayments();
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

    private ArrayList<ReportRow> getSelectedRows(ReportRow[] outputRows, DefaultReportOutput reportOutput) {

        ArrayList<ReportRow> selectedRows = new  ArrayList<ReportRow>();
        List<Integer> filteredRowIds = new ArrayList<Integer>();
        for (int i = 0; i < reportOutput.getNumberOfDisplayedRows(); i++) {
            filteredRowIds.add(reportOutput.getRowConsideringRowFiltering(i));
        }

        for (Integer filteredRowId : filteredRowIds) {
            ReportRow row = outputRows[filteredRowId];
            Boolean isSelected = (Boolean) row.getProperty(SantInterestPaymentRunnerReportTemplate.SELECT_ALL_ROW_DATA);
            if (!isSelected) {
                continue;
            }else{
                selectedRows.add(row);
            }
        }
    return selectedRows;
    }

    private void saveTrade(Trade trade){
        if(null!=trade){
            try {
                DSConnection.getDefault().getRemoteTrade().save(trade);
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error saving trades: " + e );
            }
        }
    }

    private void saveTrades(List<Trade> trades){
        if(!Util.isEmpty(trades)){
            try {
                DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(trades));
            } catch (CalypsoServiceException e) {
                Log.error(this,"Error saving trades: " + e );
            } catch (InvalidClassException e) {
                Log.error(this,"Error casting to ExternalArray: " + e );
            }
        }
    }
    private void setfieldsCT(String buttonCT, Trade trade){
        if(null!=trade){
            if (buttonCT.equalsIgnoreCase(GENERATE_BTN)){
                trade.addKeyword("InterestConfirmed","");
                trade.addKeyword("SendStatement","");
            } else if (buttonCT.equalsIgnoreCase(SEND_PAYMENTS_BTN)){
                trade.addKeyword("InterestConfirmed","True");
                trade.addKeyword("SendStatement","");
                trade.setSettleDate(getSDate(trade));
            } else if (buttonCT.equalsIgnoreCase(SEND_STATEMENT_BTN)){
                trade.addKeyword("InterestConfirmed","");
                trade.addKeyword("SendStatement","True");
            }
        }

    }

    private void modifyfieldsCT(String buttonCT, Trade trade){
        if(null!=trade) {
            if ( buttonCT.equalsIgnoreCase(GENERATE_BTN) ) {
                trade.addKeyword("InterestConfirmed", "");
                trade.addKeyword("SendStatement", "");
            } else if ( buttonCT.equalsIgnoreCase(SEND_PAYMENTS_BTN) ) {
                trade.addKeyword("InterestConfirmed", "True");
                trade.setSettleDate(getSDate(trade));
            } else if ( buttonCT.equalsIgnoreCase(SEND_STATEMENT_BTN) ) {
                trade.addKeyword("SendStatement", "True");
            }
        }
    }

    private JDate getSDate (Trade CT){
        try {
            Vector<String> holidaysCollection = DSConnection.getDefault().getRemoteReferenceData().getCurrencyDefault(CT.getTradeCurrency()).getDefaultHolidays();
            return JDate.getNow().addBusinessDays(1, holidaysCollection);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error setting settle date: " + e );
        }
    return null;

    }

    private void commonActionCT(List<ReportRow> selectedRows, String buttonCT){

        String leRole = "CounterParty";
        DSConnection ds = DSConnection.getDefault();
        BOInterestUtil boInterestUtil = new BOInterestUtil();

        if(!Util.isEmpty(selectedRows)){
            final List<SantInterestPaymentRunnerEntry> entries = selectedRows.stream()
                    .map(selectedRow -> (SantInterestPaymentRunnerEntry) selectedRow.getProperty(SantInterestPaymentRunnerReportTemplate.ROW_DATA))
                    .collect(Collectors.toList());

            for(SantInterestPaymentRunnerEntry entry : entries){
                //We generate the CT if it doesn't exists (and add it to the entry), we update the InterestBearing (when the CT is created, the IB changes)
                if(entry.getCtTrade()==null){
                    if(null!=entry.getIbTrade()){
                        final Trade ibTrade = entry.getIbTrade();
                        TradeArray trades = boInterestUtil.generateTradeTransferInterets(ibTrade.getSettleDate(),
                                ibTrade.getSettleDatetime(), leRole, ibTrade.getCounterParty(),
                                entry.getAccount(), null, null, ds);

                        if(!Util.isEmpty(trades)){
                            //Update ibTrade
                            Trade interestBearing = Arrays.stream(trades.getTrades())
                                    .filter(trade -> trade.getProduct() instanceof InterestBearing)
                                    .findFirst().orElse(null);

                            //1.Saving InterestBearing trades
                            saveTrade(interestBearing);
                            interestBearing = reloadTrade(interestBearing.getLongId());
                            entry.setIbTrade(interestBearing);

                            //Update ctTrade
                            Trade customerTransfer = Arrays.stream(trades.getTrades())
                                    .filter(trade -> trade.getProduct() instanceof CustomerTransfer)
                                    .findFirst().orElse(null);

                            saveTrade(customerTransfer);
                            customerTransfer = reloadTrade(getCtTradeID(interestBearing));

                            setfieldsCT(buttonCT, customerTransfer);
                            entry.setCtTrade(customerTransfer);
                            HashMap<String, BOMessage> latestMessages = getLatestMessages(customerTransfer);
                            entry.setPaymentMessage(latestMessages.get(PAYMENTMSG));
                            entry.setInterest(latestMessages.get(MC_INTEREST));
                        }
                    }else{
                        Log.info(this,"No InterestBearing found for line: "  );
                    }
                }else{
                    //If the CT exists (reload trades)
                    Trade ctTrade = entry.getCtTrade();
                    final Trade ibTrade = reloadTrade(entry.getIbTrade().getLongId());

                    updateCtTrade(buttonCT,ibTrade,ctTrade);

                    saveTrade(ctTrade);
                    //setting Trades
                    ctTrade = reloadTrade(entry.getCtTrade().getLongId());
                    entry.setCtTrade(ctTrade);
                    entry.setIbTrade(ibTrade);
                    HashMap<String, BOMessage> latestMessages = getLatestMessages(ctTrade);
                    entry.setPaymentMessage(latestMessages.get(PAYMENTMSG));
                    entry.setInterest(latestMessages.get(MC_INTEREST));

                }

            }


            //Refrescar el reporte y dejar las select marcadas
            this._reportWindow.getReport().getReportPanel().refresh();
        }
    }


    private void updateCtTrade(String btn,Trade ibTrade, Trade ctTrade){
        if(ctTrade!= null && ibTrade!=null && btn.equalsIgnoreCase(GENERATE_BTN)){
            final double principal = ibTrade.getProduct().getPrincipal();
            ctTrade.getProduct().setPrincipal(Math.abs(principal));
            if (principal < 0.0D) {
                ctTrade.setQuantity(1.0D);
            } else {
                ctTrade.setQuantity(-1.0D);
            }
        }
        //Default actions
        modifyfieldsCT(btn, ctTrade);
        ctTrade.setAction(Action.AMEND);
    }

    private Trade reloadTrade(long id){
        try {
            return DSConnection.getDefault().getRemoteTrade().getTrade(id);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading Customer Transfer: " + e );
        }
        return null;
    }

    /**
     * Generate CustomerTransfer trade from InterestBearing
     */
    private synchronized void generateTradeCT() {
        List<ReportRow> selectedRows = defaultGetSelectedRows();
        commonActionCT(selectedRows, GENERATE_BTN);
        Log.info(this,"Generate Trade event");
    }


    /**
     * Send Statement for the selected trades
     */
    private synchronized void sendStatement() {

        List<ReportRow> selectedRows = defaultGetSelectedRows();
        commonActionCT(selectedRows, SEND_STATEMENT_BTN);
        Log.info(this,"Send statement event");
    }

    /**
     * Send Payments for selected trade
     */
    private synchronized void sendPayments() {
        List<ReportRow> selectedRows = defaultGetSelectedRows();
        commonActionCT(selectedRows, SEND_PAYMENTS_BTN);
        Log.info(this,"Send payment event");
    }


    private List<ReportRow> defaultGetSelectedRows(){
        DefaultReportOutput reportOutput = this._reportWindow.getReportPanel().getOutput();
        if (reportOutput.getNumberOfDisplayedRows() <= 0) {
            JOptionPane.showMessageDialog(this._reportWindow, "No row to process", "", JOptionPane.PLAIN_MESSAGE);
            return null;
        }

        ReportRow[] rows = reportOutput.getRows();
        List<ReportRow> selectedRows = getSelectedRows(rows, reportOutput);

        JDate valDate = ((SantInterestPaymentRunnerEntry) rows[0]
                .getProperty(SantInterestPaymentRunnerReportTemplate.ROW_DATA)).getProcessDate();

        if (!AppUtil.displayQuestion(
                "Are you sure you want to generate Send Payments for the selected rows on " + valDate.toString() + " ?",
                this._reportWindow)) {
            return null;
        }

        return selectedRows;
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

    private long getCtTradeID(Trade trade){
        return trade!=null ? trade.getKeywordAsLongId("INTEREST_TRANSFER_TO") : 0L;
    }


    public static void main(final String... args) throws ConnectException {
        final JFrame frame = new JFrame();
        frame.setContentPane(new SantInterestPaymentRunnerReportTemplatePanel());
        frame.setVisible(true);
        frame.setSize(new Dimension(1173, 307));
    }

    static int findColumnIndex(ReportWindow reportWindow, String name) {
        int col = reportWindow.getReportPanel().getTableModel().findColumn(name);
        if (col == -1) {
            // look for filtered column
            col = reportWindow.getReportPanel().getTableModel().findColumn(name);
        }
        return col;
    }

    /**
     * Return last SWIFT-> PAYMENTMSG message and last MC_INTEREST message for the trade.
     *
     * @param trade
     * @return
     */
    public HashMap<String, BOMessage> getLatestMessages(Trade trade){
        HashMap<String,BOMessage> latestMessages = new HashMap<>();

        if(null!=trade){
            StringBuilder where = new StringBuilder();
            where.append(" TRADE_ID = " + trade.getLongId());
            where.append(" AND message_type IN ('MC_INTEREST','PAYMENTMSG')");
            try {
                MessageArray messages = DSConnection.getDefault().getRemoteBO().getMessages(where.toString(), null);

                BOMessage paymentMessage = Arrays.stream(messages.getMessages())
                        .filter(message -> PAYMENTMSG.equalsIgnoreCase(message.getMessageType()))
                        .max(Comparator.comparingLong(BOMessage::getLongId)).orElse(null);

                latestMessages.put(PAYMENTMSG,paymentMessage);

                BOMessage mcInterestMessage = Arrays.stream(messages.getMessages())
                        .filter(message -> MC_INTEREST.equalsIgnoreCase(message.getMessageType()))
                        .max(Comparator.comparingLong(BOMessage::getLongId)).orElse(null);

                latestMessages.put(MC_INTEREST,mcInterestMessage);

            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading messages: " + e );
            }

        }
        return latestMessages;
    }

}
