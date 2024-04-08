package calypsox.apps.reporting;

import calypsox.util.TradeSaverManager;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.TableModelUtil;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportController;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.ui.image.ImageUtilities;
import com.jidesoft.swing.JideButton;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BODisponibleTransferTradeReportWindowHandler extends TradeReportWindowHandler implements ActionListener {
    JideButton toolAcceptAll;
    JideButton toolCancelAll;
    private static final String LOG_CATEGORY = "BODisponibleTransferAgent";
    @Override
    public void callAfterDisplay(ReportPanel panel) {
        super.callAfterDisplay(panel);
        initCellEditors();
    }
    @Override
    public void customizeReportWindow(ReportWindow reportWindow) {
        super.customizeReportWindow(reportWindow);
        cleanUnnecessaryComponents(reportWindow);
        initButtons();
        JToolBar toolBar = reportWindow.getToolBar();
        toolBar.add(toolAcceptAll);
        toolBar.addSeparator();
        toolBar.add(toolCancelAll);
        toolBar.addSeparator();

    }

    private void cleanUnnecessaryComponents(ReportWindow reportWindow){
        reportWindow.hideClearButton();
        reportWindow.hidePrintButton();
        reportWindow.hideCloseButton();
        reportWindow.hideCountButton();
    }
    private void initButtons(){
        generateAcceptAllButton();
        generateCancelAllButton();
    }

    private void generateAcceptAllButton(){
        if(toolAcceptAll == null){
            toolAcceptAll = new JideButton();
            Dimension size = new Dimension(100, 24);
            toolAcceptAll.setPreferredSize(size);
            toolAcceptAll.setMinimumSize(size);
            toolAcceptAll.setMaximumSize(size);
            toolAcceptAll.setFocusPainted(false);
            toolAcceptAll.setText("Accept All");
            toolAcceptAll.setIcon(ImageUtilities.getIcon("com/calypso/icons/authorize_toolbar.gif"));
            toolAcceptAll.setToolTipText("Authorize all TransferAgents");
            toolAcceptAll.setActionCommand("acceptAll");
            toolAcceptAll.addActionListener(this);
        }
    }

    private void generateCancelAllButton(){
        if(toolCancelAll == null){
            toolCancelAll = new JideButton();
            Dimension size = new Dimension(100, 24);
            toolCancelAll.setPreferredSize(size);
            toolCancelAll.setMinimumSize(size);
            toolCancelAll.setMaximumSize(size);
            toolCancelAll.setFocusPainted(false);
            toolCancelAll.setText("Cancel All");
            toolCancelAll.setIcon(ImageUtilities.getIcon("com/calypso/icons/cancel_toolbar.gif"));
            toolCancelAll.setToolTipText("Cancel all TransferAgents.");
            toolCancelAll.setActionCommand("cancelAll");
            toolCancelAll.addActionListener(this);
        }
    }

    private void initCellEditors() {
        BODisponibleTransferTradeReportCellEditorListener listener = new BODisponibleTransferTradeReportCellEditorListener(this.getReportPanel());
        setTableColumnEditable("Quantity",listener);
        setTableColumnEditable("TRADE_KEYWORD.NIFBeneficiario",listener);
        setTableColumnEditable("TRADE_KEYWORD.DescripcionBeneficiario",listener);
    }

    private void setTableColumnEditable(String columnName, BODisponibleTransferTradeReportCellEditorListener listener){
        try {
            ReportPanel reportPanel = getReportPanel();
            if(Arrays.stream(getReportPanel().getTemplate().getColumns()).anyMatch(columnName::equalsIgnoreCase)){
                TableModelUtil tableModelUtil = (TableModelUtil) reportPanel.getReportViewer();
                AtomicReference<String> columnNameIdentifier = new AtomicReference<>("");
                Hashtable columnNamesHash = reportPanel.getTemplate().getColumnNamesHash();
                if(!Util.isEmpty(columnNamesHash)){
                    columnNamesHash.forEach((k,v) -> {
                        if(columnName.equalsIgnoreCase(k.toString())){
                            columnNameIdentifier.set(v.toString());
                        }
                    });
                }
                TableColumn tableColumnQuantity = tableModelUtil.getTable().getColumn(columnNameIdentifier.get());
                Optional.ofNullable(tableColumnQuantity).ifPresent(tableColumn -> {
                    tableModelUtil.setColumnEditable( tableColumn.getModelIndex(),true);
                    tableModelUtil.setColumnListener( tableColumn.getModelIndex(), listener);
                });
            }
        } catch (Exception e) {
            Log.error(this, "Error initializing cell editor for: " + columnName +" : " + e.getMessage());
        }
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (this.getReportWindow() == null) {
            return;
        }
        if (this.getReportWindow().getReportPanel().getRowCount() <= 0) {
            return;
        }

        List<Trade> allTrades = getAllTrades();
        List<Trade> blockingTrades = getBlockingTrades(allTrades);

        if("acceptAll".equalsIgnoreCase(e.getActionCommand())){
            if(!Util.isEmpty(blockingTrades)){
                applyActionOnTrades(blockingTrades,Action.valueOf("MANUAL AUTHORIZE_BLOQUEO"));
                allTrades = getNoBlockingTrades(allTrades);
            }
            applyActionOnTrades(allTrades,Action.valueOf("MANUAL AUTHORIZE"));
        } else if ("cancelAll".equalsIgnoreCase(e.getActionCommand())) {
            applyActionOnTrades(allTrades,Action.CANCEL);
        }

    }

    private List<Trade> getAllTrades(){
        DefaultReportOutput output = getReportWindow().getReportPanel().getOutput();
        return Arrays.stream(Optional.ofNullable(output)
                        .map(DefaultReportOutput::getRows).orElse(new ReportRow[0]))
                        .map(row -> row.getProperty("Trade")).map(Trade.class::cast)
                        .collect(Collectors.toList());
    }

    private List<Trade> getBlockingTrades(List<Trade> allTrades){
        return allTrades.stream().filter(t -> "true".equalsIgnoreCase(t.getKeywordValue("isBloqueo"))).collect(Collectors.toList());
    }

    private List<Trade> getNoBlockingTrades(List<Trade> allTrades){
        return allTrades.stream().filter(t -> !"true".equalsIgnoreCase(t.getKeywordValue("isBloqueo"))).collect(Collectors.toList());
    }

    /**
     * Apply action on trades in background
     * @param tradeListToAccept
     * @param action
     */
    private void applyActionOnTrades(List<Trade> tradeListToAccept,Action action){
        if(!Util.isEmpty(tradeListToAccept)){

            Thread applyActionThread = new Thread(() -> {
                activateButtons(false);
                getReportWindow().getReportController().showStatus("Apply action "  +action + " on trades.", ReportController.MessageType.InProgess);
                TradeSaverManager saverManager = new TradeSaverManager(5);
                saverManager.saveTrades(tradeListToAccept, action);
                saverManager.waitForCompletion();

                List<String> errorList = saverManager.getErrors();
                if(!errorList.isEmpty()){
                    AppUtil.displayMessage(new ArrayList<>(errorList), this.getReportWindow());
                    getReportWindow().getReportController().showStatus("Some actions can not be applied.", ReportController.MessageType.Warning);
                    StringBuilder errorsList = new StringBuilder();
                    errorList.forEach(error -> {
                        errorsList.append(error).append("\n");
                    });
                    Log.error(this.getClass().getSimpleName(),errorsList.toString());
                }else {
                    AppUtil.displayMessage("All TransferAgents have been updated." , this.getReportWindow());
                    getReportWindow().getReportController().showStatus("All TransferAgents have been updated.", ReportController.MessageType.Ok);
                }
                reloadAllTrades();
                activateButtons(true);
            });

            applyActionThread.start();
        }
    }

    /**
     * Set enable or disable buttons
     * @param status
     */
    private void activateButtons(boolean status){
        toolCancelAll.setEnabled(status);
        toolAcceptAll.setEnabled(status);
    }

    /**
     * Reload report
     */
    private void reloadAllTrades(){
        if(!isPreviewMode()){
            getReportWindow().getReportPanel().load();
        }
    }
    private boolean isPreviewMode(){
        return Optional.ofNullable(getReportWindow().getReport().getReportTemplate())
                .map(temp -> ((ReportTemplate) temp).get("TRADE_ID"))
                .map(String::valueOf).filter("-1"::equalsIgnoreCase).isPresent();
    }
}
