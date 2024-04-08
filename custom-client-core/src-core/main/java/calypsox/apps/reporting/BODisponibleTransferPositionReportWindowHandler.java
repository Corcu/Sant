package calypsox.apps.reporting;

import calypsox.tk.report.BODisponibleTransferPositionBean;
import calypsox.tk.report.BODisponibleTransferPositionReport;
import calypsox.tk.report.BODisponibleTransferTradeReport;
import com.calypso.apps.reporting.ReportObjectHandler;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.TableModelUtil;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.report.*;
import com.calypso.tk.report.gui.ReportWindowDefinition;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import com.calypso.ui.image.ImageUtilities;
import com.jidesoft.swing.JideButton;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static calypsox.tk.report.BODisponibleTransferPositionReportStyle.FROM_TO;

/**
 * @author acd
 */
public class BODisponibleTransferPositionReportWindowHandler extends BODisponibleSecurityPositionReportWindowHandler implements ActionListener {

    private static final String MULTI_TRANSFER_AGENT_ACTION = "multipleTransferAgents";

    private static final String SELECTED_OPTION = "SelectedOption";
    JideButton toolGenerateButton;
    JideButton toolPreviewGenerateButton;
    JideButton toolClearAll;
    JideButton toolShowPendingTransferAgents;
    @Override
    public ReportWindowDefinition defaultReportWindowDefinition(String reportType) {
        return new ReportWindowDefinition(reportType);
    }
    @Override
    public void callAfterDisplay(ReportPanel panel) {
        super.callAfterDisplay(panel);
        initFromToCellEditor();
    }

    /**
     * Remove TrasnferReport panel and add again just as an extra frame
     *
     * @param reportWindow
     */
    @Override
    public void customizeReportWindow(ReportWindow reportWindow) {
        super.customizeReportWindow(reportWindow);
        JToolBar toolBar = reportWindow.getToolBar();
        initButtons();
        toolBar.addSeparator();
        toolBar.add(toolGenerateButton);
        toolBar.addSeparator();
        toolBar.add(toolPreviewGenerateButton);
        toolBar.addSeparator();
        toolBar.add(toolShowPendingTransferAgents);
        toolBar.addSeparator();
        toolBar.add(toolClearAll);
        toolBar.addSeparator();
    }

    private void initButtons(){
        generateButton();
        generatePreviewButton();
        shoPendingButton();
        clearAllButton();
    }
    private void generateButton(){
        if(toolGenerateButton == null){
            toolGenerateButton = new JideButton();
            Dimension size = new Dimension(190, 24);
            toolGenerateButton.setPreferredSize(size);
            toolGenerateButton.setMinimumSize(size);
            toolGenerateButton.setMaximumSize(size);
            toolGenerateButton.setFocusPainted(false);
            toolGenerateButton.setText("Generate TransferAgents");
            toolGenerateButton.setIcon(ImageUtilities.getIcon("com/calypso/icons/action-start.png"));
            toolGenerateButton.setToolTipText("Generate new TransferAgents");
            toolGenerateButton.setActionCommand("generateTransferAgents");
            toolGenerateButton.addActionListener(this);
        }
    }

    private void generatePreviewButton(){
        if(toolPreviewGenerateButton == null){
            toolPreviewGenerateButton = new JideButton();
            Dimension size = new Dimension(90, 24);
            toolPreviewGenerateButton.setPreferredSize(size);
            toolPreviewGenerateButton.setMinimumSize(size);
            toolPreviewGenerateButton.setMaximumSize(size);
            toolPreviewGenerateButton.setFocusPainted(false);
            toolPreviewGenerateButton.setText("Preview");
            toolPreviewGenerateButton.setIcon(ImageUtilities.getIcon("com/calypso/icons/etl_execution_log.gif"));
            toolPreviewGenerateButton.setToolTipText("Generate Preview TransferAgents");
            toolPreviewGenerateButton.setActionCommand("generatePreviewTransferAgents");
            toolPreviewGenerateButton.addActionListener(this);
        }
    }

    private void clearAllButton(){
        if(toolClearAll == null){
            toolClearAll = new JideButton();
            Dimension size = new Dimension(120, 24);
            toolClearAll.setPreferredSize(size);
            toolClearAll.setMinimumSize(size);
            toolClearAll.setMaximumSize(size);
            toolClearAll.setFocusPainted(false);
            toolClearAll.setText("Clear Selection");
            toolClearAll.setIcon(ImageUtilities.getIcon("com/calypso/icons/cancel_toolbar.gif"));
            toolClearAll.setToolTipText("Clear all selections");
            toolClearAll.setActionCommand("ClearAllSelection");
            toolClearAll.addActionListener(this);
        }
    }

    private void shoPendingButton(){
        if(toolShowPendingTransferAgents == null){
            toolShowPendingTransferAgents = new JideButton();
            Dimension size = new Dimension(120, 24);
            toolShowPendingTransferAgents.setPreferredSize(size);
            toolShowPendingTransferAgents.setMinimumSize(size);
            toolShowPendingTransferAgents.setMaximumSize(size);
            toolShowPendingTransferAgents.setFocusPainted(false);
            toolShowPendingTransferAgents.setText("Show Pending");
            toolShowPendingTransferAgents.setIcon(ImageUtilities.getIcon("com/calypso/icons/pending_auth_16.png"));
            toolShowPendingTransferAgents.setToolTipText("Show Pending TransferAgents");
            toolShowPendingTransferAgents.setActionCommand("showPending");
            toolShowPendingTransferAgents.addActionListener(this);
        }
    }

    private void initFromToCellEditor() {
        try {
            TableModelUtil tableModelUtil = (TableModelUtil) getReportPanel().getReportViewer();
            TableColumn fromTo = tableModelUtil.getTable().getColumnModel().getColumn(0);
            Object headerValue = fromTo.getHeaderValue();

            if (headerValue instanceof String && FROM_TO.equalsIgnoreCase(headerValue.toString())) {
                tableModelUtil.setColumnEditable(0, true);

                String[] items = {"", "From", "To"};
                JComboBox<String> fromToCellComboBox = new JComboBox<>(items);
                BODisponibleTransferPositionReportCellEditorListener listener = new BODisponibleTransferPositionReportCellEditorListener(getReportPanel());
                DefaultCellEditor fromToCellEditor = new DefaultCellEditor(fromToCellComboBox);
                fromToCellEditor.setClickCountToStart(2);
                fromToCellEditor.addCellEditorListener(listener);
                fromTo.setCellEditor(fromToCellEditor);
                fromTo.setResizable(true);

            }
        } catch (Exception e) {
            Log.error(this, "Error initializing From-To cell editor: " + e.getMessage());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (this.getReportWindow() == null) {
            return;
        }

        if("showPending".equalsIgnoreCase(e.getActionCommand())){
            showGeneratedTrades(new TradeArray(),false);
        }

        if (this.getReportWindow().getReportPanel().getRowCount() <= 0) {
            return;
        }

        if(MULTI_TRANSFER_AGENT_ACTION.equalsIgnoreCase(e.getActionCommand())
                || "generateTransferAgents".equalsIgnoreCase(e.getActionCommand())){
            generateCustomTransferAgents(false);
        }else if("generatePreviewTransferAgents".equalsIgnoreCase(e.getActionCommand())){
            generateCustomTransferAgents(true);
        }else if("ClearAllSelection".equalsIgnoreCase(e.getActionCommand())){
            clearAllSelections();
        }
        super.actionPerformed(e);
    }


    /**
     * 1. Generate TransferAgents from the selected positions
     * @param preview
     */
    private void generateCustomTransferAgents(boolean preview) {
        Report report = this._reportPanel.getReport();

        if(report instanceof BODisponibleTransferPositionReport){
            JDate selectedDate = getSelectedDate();
            HashMap<String, BODisponibleTransferPositionBean> groupByKey = ((BODisponibleTransferPositionReport) report).getGroupByKey();

            ConcurrentLinkedQueue<BODisponibleTransferPositionBean> selectedPositions = new ConcurrentLinkedQueue<>();

            groupByKey.values().parallelStream().forEach(bean -> {
                if(null!=bean && !Util.isEmpty(bean.getPositiveList())){
                    bean.cleanFromToList();
                    List<ReportRow> allBeanPositions = Stream.of(bean.getPositiveList(), bean.getNegativeList())
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());
                    findSetSelectedPositions(bean, allBeanPositions,selectedDate);
                    if(!bean.getInvToList().isEmpty() && !bean.getInvFromList().isEmpty()){
                        selectedPositions.add(bean);
                    }
                }
            });

            if(!selectedPositions.isEmpty() && !checkAndDisplayWarnings(selectedPositions)){
                //Run generating process on a new Thread
                Thread saverThread = new Thread(() -> generationProcess(selectedPositions,preview));
                saverThread.setName("TransferAgentSaverThread");
                saverThread.start();
            }
        }
    }


    /**
     * Find selected From and To positions
     * @param bean
     * @param rows
     * @param selectedDate
     */
    private void findSetSelectedPositions(BODisponibleTransferPositionBean bean, List<ReportRow> rows, JDate selectedDate){
        if(!Util.isEmpty(rows)){
            rows.forEach(row -> {
                String option = String.valueOf(Optional.ofNullable(row.getProperty(SELECTED_OPTION)).orElse(""));
                Inventory inventory = getInventory(row, selectedDate);
                if(inventory!=null){
                    if ("To".equalsIgnoreCase(option)) {
                        bean.addInvToList(inventory);
                    } else if ("From".equalsIgnoreCase(option)) {
                        bean.addInvFromList(inventory);
                    }
                }
            });
        }
    }

    /**
     * Generate and save all new TransferAgent trades on background
     *
     * @param selectedPositions
     * @param preview
     */
    private void generationProcess(ConcurrentLinkedQueue<BODisponibleTransferPositionBean> selectedPositions,boolean preview){
        activateButtons(false);

        ConcurrentLinkedQueue<Trade> generatedTransfer = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> errors = new ConcurrentLinkedQueue<>();

        getReportWindow().getReportController().showStatus("Generating TransferAgents", ReportController.MessageType.InProgess);
        //Generate all TransferAgents
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            selectedPositions.forEach(bean -> executorService.submit(new BODisponibleTransferAgentGen(bean,generatedTransfer, errors)));
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            Log.error(this.getClass().getSimpleName(),"Error Generating TransferAgents: " + e.getMessage());
        }

        //Get all generated TransferAgents
        ArrayList<Trade> trades = new ArrayList<>(generatedTransfer);

        //Check and show Errors.
        if(!Util.isEmpty(errors)){
            logErrors(new ArrayList<>(errors));
            AppUtil.displayWarnings(new ArrayList<>(errors), getReportWindow());
        }

        //Save and load all generated TransferAgents
        if(!Util.isEmpty(trades)){
            if(!preview){
                try {
                    getReportWindow().getReportController().showStatus("Saving generated TransferAgents", ReportController.MessageType.InProgess);
                    saveAllGeneratedTrades(trades);
                    showGeneratedTrades(new TradeArray(),false);
                } catch (InterruptedException e) {
                    Log.error(this,"Error saving Trades: " + e.getMessage());
                }
            }else {
                showGeneratedTrades(new TradeArray(trades), true);
            }
        }

        getReportWindow().getReportController().showStatus("TransferAgent generated", ReportController.MessageType.Ok);

        activateButtons(true);
    }
    private void logErrors(List<String> errors){
        StringBuilder errorsList = new StringBuilder();
        errors.forEach(error -> {
            errorsList.append(error).append("\n");
        });
        Log.error(this.getClass().getSimpleName(),errorsList.toString());
    }

    /**
     * Set enable or disable buttons
     * @param status
     */
    private void activateButtons(boolean status){
        toolPreviewGenerateButton.setEnabled(status);
        toolGenerateButton.setEnabled(status);
        toolShowPendingTransferAgents.setEnabled(status);
    }


    /**
     * @param row selected row
     * @param valDate valDate to load positions
     * @return @{@link Inventory}
     */
    private Inventory getInventory(ReportRow row, JDate valDate){
        Inventory inventory = null;
        HashMap<JDate, Vector<Inventory>> inventoryMap = row.getProperty("POSITIONS");
        List<Inventory> subSelection = inventoryMap.get(valDate);
        Inventory subPos;
        if (!Util.isEmpty(subSelection)) {
            for(Iterator<Inventory> var8 = subSelection.iterator(); var8.hasNext();
                inventory = this.addToTotal(inventory, subPos)) {
                subPos = var8.next();
            }
            return inventory;
        }
        return null;
    }


    /**
     * @param tradesToSave List of trades to save
     * @return Generated TradesIds
     * @throws InterruptedException
     */
    private List<Long> saveAllGeneratedTrades(List<Trade> tradesToSave) throws InterruptedException {
        ConcurrentLinkedQueue<Long> tradeIds = new ConcurrentLinkedQueue<>();

        if (!Util.isEmpty(tradesToSave)) {
            ExecutorService exec = Executors.newSingleThreadExecutor();
            try {
                int size = 5;
                for (int start = 0; start < tradesToSave.size(); start += size) {
                    int end = Math.min(start + size, tradesToSave.size());
                    final List<Trade> trades = new ArrayList<>(tradesToSave.subList(start, end));
                    exec.execute(
                            new Runnable() {
                                public void run() {
                                    try {
                                        long[] longs = DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(trades));
                                        tradeIds.addAll(Arrays.stream(longs).boxed().collect(Collectors.toList()));
                                    } catch (CalypsoServiceException e) {
                                        Log.error(this, "Cannot save trades: " + e);
                                    } catch (InvalidClassException e) {
                                        Log.error(this, "Cannot convert to ExternalArray: " + e);
                                    }
                                }
                            });
                }
            } finally {
                exec.shutdown();
                exec.awaitTermination(20, TimeUnit.MINUTES);
            }
        }
        return new ArrayList<>(tradeIds);
    }

    /**
     * @param selectedPositions {@link BODisponibleTransferPositionBean} whit the aggregation by ISIN and Agent of the selected positions.
     * @return errors
     */
    private boolean checkAndDisplayWarnings(ConcurrentLinkedQueue<BODisponibleTransferPositionBean> selectedPositions) {
        return selectedPositions.stream()
                .anyMatch(bean -> {
                    String key = bean.getKey();
                    List<Inventory> invFromList = bean.getInvFromList();
                    List<Inventory> invToList = bean.getInvToList();

                    if (invToList.isEmpty()) {
                        AppUtil.displayWarning("No positions selected: " + key, this._reportWindow);
                        return true;
                    }

                    if (invToList.size() > 1) {
                        AppUtil.displayWarning("Select only one 'To' position: " + key, this._reportWindow);
                        return true;
                    }

                    Inventory invTo = invToList.get(0);
                    if (invTo.getTotal() > 0) {
                        AppUtil.displayWarning("'To' position selection is not valid: " + key, this._reportWindow);
                        return true;
                    }

                    return invFromList.stream()
                            .anyMatch(invFrom -> {
                                if (invFrom.getTotal() < 0) {
                                    AppUtil.displayWarning("'From' position selection is not valid: " + key, this._reportWindow);
                                    return true;
                                }
                                return false;
                            });
                });
    }

    /**
     * Load and Show TradeReport with the generated trades (no Criteria Panel)
     * @param trades List of trades on waiting accepted status by the User
     * @param preview true when only need to generate a preview of the TransferAgents
     */
    private void showGeneratedTrades(TradeArray trades,boolean preview){
        String reportType = BODisponibleTransferTradeReport.class.getSimpleName().replace("Report", "");
        ReportPanel reportPanel = ReportObjectHandler.showReport(reportType, false);

        TradeReportTemplate reportTemplate = loadTemplate(reportType,reportPanel);

        if(preview){
            reportTemplate.setTrades(trades);
            reportTemplate.put("TRADE_ID", "-1");
        }
        reportPanel.getReportTemplatePanel().setTemplate(reportTemplate);
        ReportWindow reportWindow = reportPanel.getReportWindow();
        if(null!=reportWindow){
            reportWindow.requestFocus();
            reportWindow.getDockingManager().removeFrame("Criteria",true);
        }
        reportPanel.load(reportTemplate);
    }

    /**
     * Lad default Template BODisponibleTransferTrade
     * @param reportType
     * @return
     */
    private TradeReportTemplate loadTemplate(String reportType,ReportPanel reportPanel){
        try {
            ReportTemplate reportTemplate = DSConnection.getDefault().getRemoteReferenceData().getReportTemplate(reportType, "BODisponibleTransferTrade");
            return null!=reportTemplate ? (TradeReportTemplate)reportTemplate : (TradeReportTemplate)reportPanel.getTemplate();
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading report template: " + e.getMessage());
        }
        return (TradeReportTemplate)reportPanel.getTemplate();
    }

    protected Inventory addToTotal(Inventory aggPosition, Inventory subPos) {
        if (aggPosition == null) {
            return this.clonePosition(subPos);
        } else {
            try {
                if (subPos instanceof InventoryCashPosition) {
                    ((InventoryCashPosition)aggPosition).addToTotal((InventoryCashPosition)subPos);
                } else {
                    ((InventorySecurityPosition)aggPosition).addToTotal((InventorySecurityPosition)subPos);
                }
            } catch (Exception var4) {
                Log.error(this, var4);
            }

            return aggPosition;
        }
    }



    /**
     * Clear all selected rows.
     */
    private void clearAllSelections(){//FIXME Clear all position
        DefaultReportOutput output = getReportWindow().getReportPanel().getOutput();
        Arrays.stream(output.getRows()).forEach(row -> {
            row.setProperty(SELECTED_OPTION,"");
        });
        getReportWindow().getReportPanel().refresh();
    }

    protected Inventory clonePosition(Inventory subPos) {
        try {
            return (Inventory)(subPos instanceof InventoryCashPosition ? ((InventoryCashPosition)subPos).clone() : (Inventory)((InventorySecurityPosition)subPos).clone());
        } catch (Exception var3) {
            Log.error(this, var3);
            return null;
        }
    }

    /**
     * @return Selected Column JDate (by Default Current Date)
     */
    protected JDate getSelectedDate() {
        try{
            ReportPanel reportPanel = this._reportWindow.getReportPanel();
            String selectedColumn = reportPanel.getSelectedColumn();
            if(!Util.isEmpty(selectedColumn)){
                JDate jDate = Util.MStringToDate(selectedColumn, true);
                if(null!=jDate){
                    return jDate;
                }
            }
            return reportPanel.getReport().getValuationDatetime().getJDate(TimeZone.getDefault());
        }catch (Exception e){
            Log.error(this,"Error: " + e.getMessage());
        }
        return JDate.getNow();
    }


}
