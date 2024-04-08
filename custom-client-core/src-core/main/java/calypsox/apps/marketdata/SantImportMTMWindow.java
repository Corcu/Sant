package calypsox.apps.marketdata;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.util.SantImportMTMUtil;
import calypsox.tk.util.SantTradeKeywordUtil;
import calypsox.tk.util.ScheduledTaskUnPriceTrades;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantPLMarkBuilder;
import calypsox.util.TradeInterfaceUtils;
import com.calypso.apps.util.*;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.TradeArray;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.grid.TableModelWrapperUtils;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;
import org.apache.poi.ss.usermodel.*;

import javax.swing.Action;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.rmi.RemoteException;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static calypsox.tk.core.CollateralStaticAttributes.TRADE_NOT_FOUND;
import static calypsox.tk.core.SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE;
import static calypsox.tk.core.SantPricerMeasure.S_NPV_BASE;
import static com.calypso.tk.core.PricerMeasure.S_MARGIN_CALL;
import static com.calypso.tk.core.PricerMeasure.S_NPV;

public class SantImportMTMWindow extends JFrame {

    protected JPanel topPanel = new JPanel();
    protected JLabel fileNameLabel = new JLabel();
    protected JTextField fileNameText = new JTextField();
    protected JButton fileChooserButton = new JButton();
    protected JFileChooser _fileChooser = null;

    protected JLabel dateLabel = new JLabel();
    protected JTextField dateText = new JTextField();
    protected JLabel peLabel = new JLabel();
    protected CalypsoComboBox peChoice = new CalypsoComboBox();

    protected JPanel bottomPanel = new JPanel();
    protected JButton processButton = new JButton();
    protected JCheckBox calculateContractCheckBox = new JCheckBox();

    protected JButton importButton = new JButton();
    protected JButton cancelButton = new JButton();

    protected static final String WINDOW_TITLE = "MTM Import";
    protected static final String EXCEL_EXTENSION = "xls";
    protected static final int WIDGET_HEIGHT = 20;
    protected JLabel marksLabel = new JLabel();

    protected JButton insertRowButton = new JButton();
    protected JButton refreshButton = new JButton();

    protected JScrollPane markAdjScrollPane = new JideScrollPane();
    protected JideScrollPane marksScrollPane = new JideScrollPane();
    protected SortableTable marksTable = new SortableTable();
    protected ImportMarksTableModel marksTableModel;

    private final String[] multiCcyValues = {"No", "Si"};

    protected Map<String, List<Trade>> tradeMap = null;
    protected static SantMultiTradeChooser _tradeChooser = null;

    protected Vector<String> validMarkNames = new Vector<>();

    private final Action deleteMarksAction = new AbstractAction("Delete Selected") {
        private static final long serialVersionUID = 2345L;

        @Override
        public void actionPerformed(final ActionEvent e) {
            deleteSelectedMarks();
        }
    };
    private JButton deleteButton = new JButton(this.deleteMarksAction);

    private static final long serialVersionUID = 123L;
    private PricingEnv pricingEnv = null;
    private JDate processDate = null;

    public SantImportMTMWindow() {
        this(null, null);
    }

    public SantImportMTMWindow(final JDate adjustDate, final String subId) {
        setTitle(WINDOW_TITLE);
        setSize(1000, 495);
        setResizable(true);
        initPanels();
        setDefaults();
        this.dateText.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(final FocusEvent arg0) {
                final JDate processDateTemp = Util.stringToJDate(SantImportMTMWindow.this.dateText.getText());

                if (!processDateTemp.equals(SantImportMTMWindow.this.processDate)) {
                    SantImportMTMWindow.this.processDate = processDateTemp;
                    loadPrivingEnv(processDateTemp);
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                // Auto-generated method stub
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static void main(final String... args) throws ConnectException {
        ConnectionUtil.connect(args, "MTM");
        final SantImportMTMWindow importWindow = new SantImportMTMWindow();
        importWindow.show(true);
    }

    protected void loadPrivingEnv(final JDate date) {
        try {
            SantImportMTMWindow.this.pricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(
                    this.peChoice.getSelectedItem().toString(), new JDatetime(date, TimeZone.getDefault()));
        } catch (final RemoteException e) {
            Log.error(SantImportMTMWindow.class, "Erorr loading pricing env for date " + date, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void setupMarksTableCellEditors() {
        Vector<String> reasonValuesTemp = LocalCache.getDomainValues(DSConnection.getDefault(),
                CollateralStaticAttributes.DOMAIN_SANT_MTM_CHANGE_REASON);
        Vector<String> reasonValues = new Vector<>();
        if (reasonValuesTemp != null) {
            reasonValues = (Vector) reasonValuesTemp.clone();
        }
        reasonValues.insertElementAt("", 0);
        createCellEditor(ImportMarksTableModel.REASON_COL_NUM, reasonValues.toArray());

        // Create a Combo Box CellEditor for Measure Name cell
        createCellEditor(ImportMarksTableModel.MARK_NAME_BASE_COL_NUM, getValidMarknames().toArray());

        // Create a Combo Box for Multi-Currency
        createCellEditor(ImportMarksTableModel.MULTI_CURRENCY_COL_NUM, multiCcyValues);
        this.marksTableModel.setValueNoCheck(this.marksTable.getRowCount() - 1,
                ImportMarksTableModel.MULTI_CURRENCY_COL_NUM, multiCcyValues[0]);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void createCellEditor(final int column, Object[] values) {
        final JComboBox comboBox = new JComboBox(values);
        comboBox.setSelectedIndex(0);
        final TableCellEditor cellEditor = new DefaultCellEditor(comboBox);
        final TableColumn tableColumn = this.marksTable.getColumnModel().getColumn(column);
        tableColumn.setCellEditor(cellEditor);
    }

    public Vector<String> getValidMarknames() {
        if (Util.isEmpty(this.validMarkNames)) {
            this.validMarkNames.add(S_NPV_BASE);
            this.validMarkNames.add(S_INDEPENDENT_AMOUNT_BASE);
        }
        return this.validMarkNames;
    }

    protected void setDefaults() {
        try {
            // Set previous day by default
            this.processDate = JDate.getNow().addBusinessDays(-1,
                    LocalCache.getCurrencyDefault("EUR").getDefaultHolidays());
            this.dateText.setText(this.processDate.toString());

            this.pricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(
                    DSConnection.getDefault().getDefaultPricingEnv(),
                    new JDatetime(this.processDate, TimeZone.getDefault()));
        } catch (final Exception exc) {
            Log.error(this, exc); //sonar purpose
            Log.error(this, exc.getMessage());
        }
    }

    protected void initPanels() {
        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(
                AppUtil.makeDarkerColor(getContentPane().getBackground(), ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));
        initBottomPanel();
        initMarksTable();
        initTopPanel();

        final SymAction lSymAction = new SymAction();
        this.fileChooserButton.addActionListener(lSymAction);
        this.processButton.addActionListener(lSymAction);
        this.importButton.addActionListener(lSymAction);
        this.insertRowButton.addActionListener(lSymAction);
        this.refreshButton.addActionListener(lSymAction);
        this.cancelButton.addActionListener(lSymAction);

    }

    protected void initTopPanel() {
        final FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        this.topPanel.setLayout(flowLayout);
        this.topPanel.setBorder(new LineBorder(Color.GRAY, 1, true));

        this.fileNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        this.fileNameLabel.setText("File Name:");
        this.fileNameLabel.setPreferredSize(new Dimension(60, WIDGET_HEIGHT));
        this.fileNameText.setText("");
        this.fileNameText.setPreferredSize(new Dimension(300, WIDGET_HEIGHT));
        this.fileChooserButton.setText("...");
        this.fileChooserButton.setPreferredSize(new Dimension(22, WIDGET_HEIGHT));

        this.dateLabel.setText("Date:");
        this.dateLabel.setPreferredSize(new Dimension(30, WIDGET_HEIGHT));
        this.dateText.setPreferredSize(new Dimension(80, WIDGET_HEIGHT));
        AppUtil.addDateListener(this.dateText);

        this.peLabel.setText("Pricing Env:");
        this.peLabel.setPreferredSize(new Dimension(70, WIDGET_HEIGHT));

        this.topPanel.add(this.fileNameLabel);
        this.topPanel.add(this.fileNameText);
        this.topPanel.add(this.fileChooserButton);

        this.topPanel.add(new Label(" "));
        this.topPanel.add(this.dateLabel);
        this.topPanel.add(this.dateText);
        this.topPanel.add(new Label(" "));
        this.topPanel.add(this.peLabel);

        AppUtil.setPricingEnvChoice(this.peChoice);
        this.topPanel.add(this.peChoice);

        getContentPane().add(this.topPanel, BorderLayout.NORTH);
    }

    protected void initBottomPanel() {
        this.bottomPanel.setPreferredSize(new Dimension(700, 40));
        this.bottomPanel.setBorder(new PartialEtchedBorder(PartialSide.HORIZONTAL));
        this.bottomPanel.setBackground(
                AppUtil.makeDarkerColor(this.bottomPanel.getBackground(), ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

        this.importButton.setText("Import");
        this.bottomPanel.add(this.importButton);

        // Add Space
        this.bottomPanel.add(new JLabel(" "));
        this.processButton.setText("Process");
        this.bottomPanel.add(this.processButton);
        this.calculateContractCheckBox.setText("Calculate Contracts");
        this.calculateContractCheckBox.setSelected(true);
        this.bottomPanel.add(this.calculateContractCheckBox);
        // Add Space
        this.bottomPanel.add(new JLabel(" "));

        this.deleteButton.setText("Delete Selected");
        this.bottomPanel.add(this.deleteButton);

        this.insertRowButton.setText("Insert Empty Row");
        this.bottomPanel.add(this.insertRowButton);

        this.refreshButton.setText("Refresh");
        this.bottomPanel.add(this.refreshButton);

        this.cancelButton.setText("Close");
        this.bottomPanel.add(this.cancelButton);

        getContentPane().add(this.bottomPanel, BorderLayout.SOUTH);
    }

    protected void initMarksTable() {
        final JPanel marksTablePanel = new JPanel(new BorderLayout());
        marksTablePanel.setBackground(
                AppUtil.makeDarkerColor(marksTablePanel.getBackground(), ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

        this.marksLabel.setText("MTM Values");
        marksTablePanel.add(this.marksLabel, BorderLayout.NORTH);

        this.marksTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.marksTable.setRowSelectionAllowed(false);
        this.marksTable.setCellSelectionEnabled(true);

        this.marksTableModel = new ImportMarksTableModel(1);
        this.marksTableModel.setTo(this.marksTable);
        this.marksScrollPane.setViewportView(this.marksTable);
        this.marksScrollPane.setAutoscrolls(true);
        marksTablePanel.add(this.marksScrollPane, BorderLayout.CENTER);

        this.marksTableModel.setValueNoCheck(this.marksTable.getRowCount() - 1,
                ImportMarksTableModel.MARK_NAME_BASE_COL_NUM, S_NPV_BASE);

        this.marksTable.setCellRendererManagerEnabled(false);

        // Set table Cell Editors
        setupMarksTableCellEditors();

        // Table listeners
        this.marksTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_DELETE) && SantImportMTMWindow.this.deleteMarksAction.isEnabled()) {
                    // deleteMarksAction.actionPerformed(null);
                }
            }
        });

        getContentPane().add(marksTablePanel, BorderLayout.CENTER);
    }

    protected class SymAction implements java.awt.event.ActionListener {
        @Override
        public void actionPerformed(final java.awt.event.ActionEvent event) {
            final Object object = event.getSource();
            final Cursor origCursor = getCursor();
            try {
                if (object == SantImportMTMWindow.this.processButton) {
                    processButtonActionPerformed();
                }
                if (object == SantImportMTMWindow.this.importButton) {
                    importButtonActionPerformed();
                }
                if (object == SantImportMTMWindow.this.insertRowButton) {
                    insertRowButtonActionPerformed();
                }
                if (object == SantImportMTMWindow.this.refreshButton) {
                    refreshButtonActionPerformed();
                }
                if (object == SantImportMTMWindow.this.cancelButton) {
                    cancelButtonActionPerformed();
                }
                if (object == SantImportMTMWindow.this.fileChooserButton) {
                    fileChooserButtonActionPerformed();
                }

            } finally {
                setCursor(origCursor);
            }
        }
    }

    protected void refreshButtonActionPerformed() {

        this.marksTableModel.notifyOnNewValue(false);

        // check if the date is changed
        @SuppressWarnings("unused") final JDate processDateTemp = Util.stringToJDate(this.dateText.getText());
        loadPrivingEnv(this.processDate);
        // if (!processDateTemp.equals(this.processDate)) {
        // this.processDate = processDateTemp;
        // loadPrivingEnv(this.processDate);
        // }

        final int rowCount = this.marksTableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            final String prodType = (String) this.marksTableModel.getValueAt(i,
                    ImportMarksTableModel.PRODUCTTYPE_COL_NUM);
            String externalRef = (String) this.marksTableModel.getValueAt(i, ImportMarksTableModel.EXT_REF_COL_NUM);
            String tradeIdStr = (String) this.marksTableModel.getValueAt(i, ImportMarksTableModel.TRADE_ID_COL_NUM);
            String boRef = (String) this.marksTableModel.getValueAt(i, ImportMarksTableModel.BO_REF_COL_NUM);

            if (!Util.isEmpty(externalRef)
                    && (Util.isEmpty(prodType) || Util.isEmpty(tradeIdStr) || Util.isEmpty(boRef))) {
                this.marksTableModel.setTradeInfoToCells(i, ImportMarksTableModel.PRODUCTTYPE_COL_NUM, externalRef,
                        tradeIdStr);
            }

            tradeIdStr = (String) this.marksTableModel.getValueAt(i, ImportMarksTableModel.TRADE_ID_COL_NUM);
            // If valid trade is found/selected then set Trade Base Ccy and
            // Amount
            if (!Util.isEmpty(tradeIdStr)) {
                this.marksTableModel.setTradeCcyAndValue(i, ImportMarksTableModel.EXT_REF_COL_NUM, true);
            }
        }

        this.marksTableModel.notifyOnNewValue(true);

        TableUtil.adjust(this.marksTable);
    }

    protected void fileChooserButtonActionPerformed() {
        final String cwd = System.getProperty("user.dir");
        if (this._fileChooser == null) {
            this._fileChooser = AppUtil.getFileChooser(EXCEL_EXTENSION, "");
            try {
                final File f = new File(cwd);
                this._fileChooser.setCurrentDirectory(f);
            } catch (final Exception e) {
                Log.error(this, e);
            }
        }
        final int returnVal = this._fileChooser.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final File dataFile = this._fileChooser.getSelectedFile();

        this.fileNameText.setText(dataFile.getAbsolutePath());
        // loadButton_ActionPerformed(event);
    }

    void importButtonActionPerformed() {
        if (this.fileNameText.getText() == null) {
            AppUtil.displayError(this, "Please select a file to Import");
            return;
        }

        try {
            loadFile();
        } catch (final Exception e) {
            Log.error(SantImportMTMWindow.class, e);
            AppUtil.displayError(this, e.getMessage());
            return;
        }

    }

    void insertRowButtonActionPerformed() {

        this.marksTableModel.insertRowAt(this.marksTable.getRowCount());
        this.marksTableModel.setValueNoCheck(this.marksTable.getRowCount() - 1,
                ImportMarksTableModel.MARK_NAME_BASE_COL_NUM, S_NPV_BASE);
        this.marksTableModel.setValueNoCheck(this.marksTable.getRowCount() - 1,
                ImportMarksTableModel.MULTI_CURRENCY_COL_NUM, multiCcyValues[0]);
    }

    void cancelButtonActionPerformed() {
        setVisible(false);
        dispose();
    }

    protected class SymWindow extends java.awt.event.WindowAdapter {
        @Override
        public void windowClosing(final java.awt.event.WindowEvent event) {
            final Object object = event.getSource();
            if (object == SantImportMTMWindow.this) {
                importMTMWindowClosing();
            }
        }
    }

    protected void importMTMWindowClosing() {
        setVisible(false);
        dispose();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, List<Trade>> initTradeMap(Sheet sheet) {
        Map<String, List<Trade>> localTradeMap = new HashMap<>();
        final Iterator rowIterator = sheet.rowIterator();

        // 1. get list of ExtRefs from Excel
        List<String> extRefList = new ArrayList<>();
        while (rowIterator.hasNext()) {
            String externalRef;
            final Row row = (Row) rowIterator.next();
            if (row.getCell(ImportMarksTableModel.EXT_REF_COL_NUM) != null) {
                if (row.getCell(ImportMarksTableModel.EXT_REF_COL_NUM).getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    // in case if the cell in the excel file is defined as
                    // numeric
                    externalRef = Integer
                            .toString((int) row.getCell(ImportMarksTableModel.EXT_REF_COL_NUM).getNumericCellValue());
                } else {
                    externalRef = row.getCell(ImportMarksTableModel.EXT_REF_COL_NUM).getStringCellValue();
                }

                if (!Util.isEmpty(externalRef)) {
                    extRefList.add(externalRef);
                }
            }
        }

        // 2. Load Trades Map.
        if (!Util.isEmpty(extRefList)) {
            List<List<String>> splitCollection = CollateralUtilities.splitCollection(extRefList, 999);
            for (List<String> subList : splitCollection) {
                if (Util.isEmpty(subList)) {
                    continue;
                }

                String where = " trade.trade_status<>'CANCELED' and  trade.external_reference in "
                        + Util.collectionToSQLString(subList);

                TradeArray tradeArray = null;
                try {
                    tradeArray = DSConnection.getDefault().getRemoteTrade().getTrades(null, where, null, null);
                } catch (RemoteException e) {
                    Log.error(this, e);
                }

                if (!Util.isEmpty(tradeArray)) {
                    Iterator iterator = tradeArray.iterator();
                    while (iterator.hasNext()) {
                        Trade trade = (Trade) iterator.next();
                        List<Trade> list = localTradeMap.get(trade.getExternalReference());
                        if (list == null) {
                            list = new ArrayList();
                            localTradeMap.put(trade.getExternalReference(), list);
                        }
                        list.add(trade);
                    }
                }

            }
        }

        return localTradeMap;

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Vector<Vector> readFile(final File file) throws Exception {

        final InputStream inp = new FileInputStream(file);
        final Workbook wb = WorkbookFactory.create(inp);
        // create a workbook out of the input stream
        final Sheet sheet = wb.getSheetAt(0);

        final Vector<Vector> fileData = new Vector<>();
        final Iterator rowIterator = sheet.rowIterator();

        this.tradeMap = initTradeMap(sheet);

        int rowCount = 0;
        while (rowIterator.hasNext()) {
            final Row row = (Row) rowIterator.next();
            // exclude the first row which is header
            if (rowCount++ == 0) {
                continue;
            }

            Trade trade = null;
            String markName = null;
            String markBaseCcy = null;
            Amount markBaseAmount = null;
            String externalRef = null;
            String changeReason = null;
            Double haircut = null;

            String markTradeCcy = null;
            Amount markTradeAmount = null;
            String productType = null;
            String tradeIdStr = "";
            String boRef = "";

            // New fields
            Double dirtyPrice = null;
            String principalCcy = "";
            Double principal = null;
            Double nominal = null;
            //  Double mtmHaircut = null;
            Double indexRatio = null;
            String multiCcy = "";

            if (row.getCell(ImportMarksTableModel.MARK_NAME_BASE_COL_NUM) != null) {
                if (getValidMarknames()
                        .contains(row.getCell(ImportMarksTableModel.MARK_NAME_BASE_COL_NUM).getStringCellValue())) {
                    markName = row.getCell(ImportMarksTableModel.MARK_NAME_BASE_COL_NUM).getStringCellValue();
                }
            }

            if (row.getCell(ImportMarksTableModel.CURRENCY_BASE_COL_NUM) != null) {
                markBaseCcy = row.getCell(ImportMarksTableModel.CURRENCY_BASE_COL_NUM).getStringCellValue();
            }

            if (row.getCell(ImportMarksTableModel.PRICER_MEASURE_VALUE_BASE_COL_NUM) != null) {
                Double markAmountDbl = row.getCell(ImportMarksTableModel.PRICER_MEASURE_VALUE_BASE_COL_NUM)
                        .getNumericCellValue();
                if (markAmountDbl != null) {
                    markBaseAmount = new Amount(markAmountDbl);
                }
            }

            if (row.getCell(ImportMarksTableModel.EXT_REF_COL_NUM) != null) {
                if (row.getCell(ImportMarksTableModel.EXT_REF_COL_NUM).getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    // in case if the cell in the excel file is defined as
                    // numeric
                    externalRef = Integer
                            .toString((int) row.getCell(ImportMarksTableModel.EXT_REF_COL_NUM).getNumericCellValue());
                } else {
                    externalRef = row.getCell(ImportMarksTableModel.EXT_REF_COL_NUM).getStringCellValue();
                }
                try {
                    if (!Util.isEmpty(externalRef)) {
                        trade = getTradeByExtRef(this.tradeMap, externalRef, null);

                        productType = this.marksTableModel.getProductType(trade);

                        // new code for new fields
                        if (Repo.REPO.equals(productType)) {

                            dirtyPrice = getValueFromCell(ImportMarksTableModel.DIRTY_PRICE_COL_NUM, row);
                            principal = getValueFromCell(ImportMarksTableModel.PRINCIPAL_COL_NUM, row);
                            nominal = getValueFromCell(ImportMarksTableModel.NOMINAL_COL_NUM, row);
                            //  mtmHaircut = getValueFromCell(ImportMarksTableModel.HAIRCUT_COL_NUM, row);
                            indexRatio = getValueFromCell(ImportMarksTableModel.INDEX_RATIO_COL_NUM, row);
                            if (row.getCell(ImportMarksTableModel.PRINCIPAL_CCY_COL_NUM) != null) {
                                principalCcy = row.getCell(ImportMarksTableModel.PRINCIPAL_CCY_COL_NUM)
                                        .getStringCellValue();
                            }
                            if (row.getCell(ImportMarksTableModel.MULTI_CURRENCY_COL_NUM) != null) {
                                multiCcy = row.getCell(ImportMarksTableModel.MULTI_CURRENCY_COL_NUM)
                                        .getStringCellValue();
                            }

                        }
                    }

                } catch (final Exception exc) {
                    Log.warn(SantImportMTMWindow.class, exc);
                }
            }

            // Convert mtm to Trade Ccy
            if (trade != null) {
                markTradeCcy = trade.getTradeCurrency();
                tradeIdStr = String.valueOf(trade.getLongId());
                boRef = trade.getKeywordValue(TradeInterfaceUtils.TRADE_KWD_BO_REFERENCE);
                if (markBaseCcy.equals(markTradeCcy)) {
                    markTradeAmount = markBaseAmount;
                } else {
                    markTradeAmount = convertAmountToTradeCcy(markBaseCcy, markBaseAmount, markTradeCcy);
                }
            }

            Cell oslaCell = row.getCell(ImportMarksTableModel.OSLAFACTOR_COL_NUM);
            if (oslaCell != null && oslaCell.getNumericCellValue() != 0.0d) {
                haircut = oslaCell.getNumericCellValue();
            }

            if (row.getCell(ImportMarksTableModel.REASON_COL_NUM) != null) {
                changeReason = row.getCell(ImportMarksTableModel.REASON_COL_NUM).getStringCellValue();
            }

            if (!Util.isEmpty(markName) && !Util.isEmpty(externalRef)) {
                final Vector rowData = new Vector();
                rowData.add(markName);
                rowData.add(markBaseCcy);
                rowData.add(markBaseAmount);
                rowData.add(externalRef);
                rowData.add(haircut);
                rowData.add(changeReason);

                rowData.add(markTradeCcy);
                rowData.add(markTradeAmount);
                rowData.add(productType);
                rowData.add(tradeIdStr);
                rowData.add(boRef);

                // new fields
                if (Repo.REPO.equals(productType)) {
                    rowData.add(dirtyPrice);
                    rowData.add(principalCcy);
                    rowData.add(principal);
                    rowData.add(nominal);
                    //  rowData.add(mtmHaircut);
                    rowData.add(indexRatio);
                    rowData.add(multiCcy);
                }

                fileData.add(rowData);
            }
        }

        return fileData;
    }

    private Double getValueFromCell(final int column, final Row row) {
        Cell cell = row.getCell(column);
        if (cell != null) {
            return cell.getNumericCellValue();
        }
        return null;
    }

    /*
     * Converts the markBaseAmount to markTradeCcy and returns Amount object
     */
    public Amount convertAmountToTradeCcy(final String markBaseCcy, final Amount markBaseAmount,
                                          final String markTradeCcy) throws MarketDataException {
        final double markTradeAmountDbl = CollateralUtilities.convertCurrency(markBaseCcy, markBaseAmount.get(),
                markTradeCcy, this.processDate, this.pricingEnv);
        return new Amount(markTradeAmountDbl);
    }

    @SuppressWarnings("rawtypes")
    protected void loadFile() throws Exception {
        final String fileName = this.fileNameText.getText();

        if (Util.isEmpty(fileName)) {
            AppUtil.displayError(this, "Please select a file to import");
            return;
        }

        // 1. read the contents of the file in to a vector<PLMarkValue>
        Vector<Vector> fileData = null;
        try {
            fileData = readFile(new File(fileName));
            if (Util.isEmpty(fileData)) {
                AppUtil.displayError(this, "Could not Parse the Data File");
                return;
            }
        } catch (final MarketDataException exc) {
            Log.error(this, exc); //sonar purpose
            AppUtil.displayError(this, exc.getMessage());
            return;
        }

        // 2. reinit the table model to the no of rows
        this.marksTableModel.reinitRows(fileData.size() + 5);

        this.marksTableModel.notifyOnNewValue(false);
        loadDataTable(fileData);
        this.marksTableModel.notifyOnNewValue(true);
    }

    /*
     * Config Table rows map to Data Table columns.
     */
    @SuppressWarnings("rawtypes")
    protected void loadDataTable(final Vector<Vector> contentVec) {
        try {
            this.marksTableModel.reinitRows(contentVec.size());
            for (int i = 0; i < contentVec.size(); i++) {
                final Vector rowData = contentVec.elementAt(i);
                for (int j = 0; j < rowData.size(); j++) {
                    final Object value = rowData.elementAt(j);
                    this.marksTableModel.setValueAt(i, j, value);
                }
                String productType = (String) rowData.elementAt(ImportMarksTableModel.PRODUCTTYPE_COL_NUM);
                if (!Repo.REPO.equalsIgnoreCase(productType)) {
                    this.marksTableModel.setValueNoCheck(i, ImportMarksTableModel.DIRTY_PRICE_COL_NUM, "");
                    this.marksTableModel.setValueNoCheck(i, ImportMarksTableModel.PRINCIPAL_CCY_COL_NUM, "");
                    this.marksTableModel.setValueNoCheck(i, ImportMarksTableModel.PRINCIPAL_COL_NUM, "");
                    this.marksTableModel.setValueNoCheck(i, ImportMarksTableModel.NOMINAL_COL_NUM, "");
                    //  this.marksTableModel.setValueNoCheck(i, ImportMarksTableModel.HAIRCUT_COL_NUM, "");
                    this.marksTableModel.setValueNoCheck(i, ImportMarksTableModel.INDEX_RATIO_COL_NUM, "");
                }
            }
        } catch (final Exception ex) {
            Log.error(SantImportMTMWindow.class, ex);
        }
        this.marksTableModel.refresh();
        TableUtil.adjust(this.marksTable);
    }

    /**
     * Delete selected marks
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void deleteSelectedMarks() {
        final int[] selRows = this.marksTable.getSelectedRows();
        if ((selRows == null) || (selRows.length == 0)) {
            // Nothing to delete
            return;
        }
        final HashSet rowsToRemove = new HashSet();
        for (int i = 0; i < selRows.length; i++) {
            final int realRowIndex = TableModelWrapperUtils.getActualRowAt(this.marksTable.getModel(), selRows[i]);
            if (realRowIndex >= 0) {
                rowsToRemove.add(new Integer(realRowIndex));
            }
        }

        if (!rowsToRemove.isEmpty()) {
            this.marksTableModel.removeRows(rowsToRemove);
            this.marksTableModel.refresh();
        }
    }

    void processButtonActionPerformed() {
        final JDate processDateTemp = Util.stringToJDate(this.dateText.getText());
        if (processDateTemp == null) {
            AppUtil.displayError(this, "Please input a valid Date.");
            this.dateText.requestFocus();
            return;
        }

        if (this.peChoice.getSelectedItem() == null) {
            AppUtil.displayError(this, "Please select a pricing Env.");
            return;
        }

        this.processDate = processDateTemp;

        if (this.marksTableModel.getRowCount() == 0) {
            AppUtil.displayError(this, "There are no rows to process.");
            return;
        }

        this.marksTable.editingStopped(null);
        // saveUpdatedMarks();

        final HashMap<Trade, PLMark> plMarks = new HashMap<>();
        final ArrayList<String> errorMessages = new ArrayList<>();
        final HashSet<Integer> contractSet = new HashSet<>();
        final HashSet<Trade> tradesToSave = new HashSet<>();

        Long s1 = System.currentTimeMillis();
        // check if the data input is valid
        if (!checkIsValidInput(this.processDate)) {
            return;
        }

        Long s2 = System.currentTimeMillis();
        System.out.println("********1.start: " + (s2 - s1));

        for (int i = 0; i < this.marksTable.getRowCount(); i++) {

            final String markNameBase = (String) this.marksTable.getValueAt(i,
                    ImportMarksTableModel.MARK_NAME_BASE_COL_NUM);

            final String ccy = (String) this.marksTable.getValueAt(i, ImportMarksTableModel.CURRENCY_BASE_COL_NUM);

            final Amount valueAmt = (Amount) this.marksTable.getValueAt(i,
                    ImportMarksTableModel.PRICER_MEASURE_VALUE_BASE_COL_NUM);
            final Double valueBase = valueAmt.get();

            final Double hairCut = (Double) this.marksTable.getValueAt(i, ImportMarksTableModel.OSLAFACTOR_COL_NUM);
            final String reason = (String) this.marksTable.getValueAt(i, ImportMarksTableModel.REASON_COL_NUM);
            String externalRef = (String) this.marksTableModel.getValueAt(i, ImportMarksTableModel.EXT_REF_COL_NUM);

            String tradeStr = (String) this.marksTableModel.getValueAt(i, ImportMarksTableModel.TRADE_ID_COL_NUM);

            Trade trade = getTradeByExtRef(externalRef, tradeStr);

            if (trade == null) {
                displayError(i, ImportMarksTableModel.EXT_REF_COL_NUM,
                        "Trade not found for ExternalRef in the row " + i);
                return;
            }

            String productType = trade.getProductType();
            if (Repo.REPO.equalsIgnoreCase(productType)) {
                addRepoAttributes(trade, i);
            }

            // All the checks have now been done. retreive PLMarks from DB if
            // exists,
            // set new value, calculate MARGIN_CALL if the markName is NPV
            PLMark currentPLMark = null;
            try {
                if (plMarks.get(trade) != null) {
                    currentPLMark = plMarks.get(trade);
                } else {
                    currentPLMark = CollateralUtilities.createPLMarkIfNotExists(trade, DSConnection.getDefault(),
                            this.peChoice.getSelectedItem().toString(), this.processDate);

                    // v14 GSM - Fix to clean PlMark
                    currentPLMark = cleanPlMarks(currentPLMark, trade, S_NPV_BASE);
                    plMarks.put(trade, currentPLMark);
                }
            } catch (final RemoteException e1) {
                Log.error(this.getClass().getName(),
                        "Error retreiving PLMark for trade ExternalRef=" + trade.getExternalReference(), e1);
            }

            PLMarkValue pLMarkValueBase = CollateralUtilities.buildPLMarkValue(markNameBase, ccy, valueBase, reason);

            SantImportMTMUtil.adjustAndAddPLMarkValue(plMarks, trade, pLMarkValueBase,
                    this.peChoice.getSelectedItem().toString(), this.processDate); //npv_base

            final PLMarkValue plmValueTradeCcy = SantImportMTMUtil.convertPLMarkValueToTradeCcy(pLMarkValueBase,
                    this.pricingEnv, trade.getTradeCurrency(), this.processDate, errorMessages);

            // Display Error messages after trades have been saved
            if (!Util.isEmpty(errorMessages)) {
                AppUtil.displayError(this, "Error Messages", errorMessages);
                return;
            }

            SantImportMTMUtil.adjustAndAddPLMarkValue(plMarks, trade, plmValueTradeCcy,
                    this.peChoice.getSelectedItem().toString(), this.processDate); //npv

            if (markNameBase.equals(S_NPV_BASE)) {
                // Handle unsettled Trades

                CollateralUtilities.handleUnSettledTrade(trade, valueBase, this.processDate);

                // calculate MARGIN_CALL if the markName is NPV
                if (trade.getProductType().equals(CollateralStaticAttributes.SEC_LENDING)
                        || trade.getProductType().equals(CollateralStaticAttributes.REPO)
                        || trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)
                        || trade.getProductType().equals(PerformanceSwap.PERFORMANCESWAP)
                        || trade.getProduct() instanceof Bond) {
                    try {
                        // GSM: 06/05/2014 - Adaptation to
                        // CollateralExposure.SECURITY_LENDING
                        if ("SECURITY_LENDING".equals(trade.getProductSubType()) && hairCut != null && !hairCut.equals(0.0d)) {
                            trade.addKeyword(CollateralStaticAttributes.FO_HAIRCUT, hairCut.toString());
                        }

                        // Calculate MARGIN_CALL
                        final PLMarkValue mcPLMarkValue = CollateralUtilities.calculateMARGIN_CALL(
                                DSConnection.getDefault(), trade, plmValueTradeCcy,
                                this.peChoice.getSelectedItem().toString(), this.processDate, hairCut, errorMessages);
                        // AAP Now returns errors
                        SantImportMTMUtil.adjustAndAddPLMarkValue(plMarks, trade, mcPLMarkValue,
                                this.peChoice.getSelectedItem().toString(), this.processDate);

                        // GSM: 06/05/2014 - Adaptation to
                        // CollateralExposure.SECURITY_LENDING
                        if ((hairCut != null) && (trade.getProductType().equals(CollateralStaticAttributes.SEC_LENDING)
                                || "SECURITY_LENDING".equals(trade.getProductSubType()))) {

                            final String trdHairCutStr = trade.getKeywordValue(CollateralStaticAttributes.FO_HAIRCUT);
                            Double trdHairCut = 0.0;

                            if (!Util.isEmpty(trdHairCutStr)) {
                                trdHairCut = Double.parseDouble(trdHairCutStr);
                            }
                            if (!trdHairCut.equals(hairCut)) {
                                trade.addKeyword(CollateralStaticAttributes.FO_HAIRCUT, hairCut);
                            }
                        }
                    } catch (final Exception e) {
                        Log.error(this.getClass().getName(), e);
                    }
                }
            }

            // Finally save all trades.
            tradesToSave.add(trade);

            // Add to contractSet
            if (trade.getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER) != 0) {
                contractSet.add(trade.getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER));
            }
        }

        Long s3 = System.currentTimeMillis();
        System.out.println("*********2.start: " + (s3 - s2));

        buildAndSavePLMarks(plMarks, errorMessages);
        // We also need to save trades modified and unsettled Trades.
        errorMessages.clear();

        saveTrades(tradesToSave, errorMessages);
        // Display Error messages after trades have been saved
        if (!Util.isEmpty(errorMessages)) {
            AppUtil.displayError(this, "Error Messages", errorMessages);
            return;
        }

        calculateContracts(contractSet, errorMessages);
    }

    private void addRepoAttributes(final Trade trade, final int line) {
        Repo repo = (Repo) trade.getProduct();
        trade.addKeyword(SantTradeKeywordUtil.DIRTY_PRICE, getDirtyPrice(repo, line));
        trade.addKeyword(SantTradeKeywordUtil.PRINCIPAL_CCY, getPrincipalCcy(repo, line));
        trade.addKeyword(SantTradeKeywordUtil.PRINCIPAL, getPrincipal(repo, line));
        trade.addKeyword(SantTradeKeywordUtil.NOMINAL, getNominal(trade, repo, line));
        trade.addKeyword(SantTradeKeywordUtil.INDEX_RATIO, getIndexRatio(trade, line));
        trade.addKeyword(SantTradeKeywordUtil.MULTICURRENCY, getMultiCcy(line));
    }

    private String getDirtyPrice(final Repo repo, final int line) {
        Double dirtyPriceD = (Double) this.marksTable.getValueAt(line, ImportMarksTableModel.DIRTY_PRICE_COL_NUM);
        if (dirtyPriceD == null) {
            Collateral collateral = getFirstSecCollateral(repo);
            if (collateral != null) {
                dirtyPriceD = collateral.getDirtyPrice() * 100;
            } else {
                dirtyPriceD = 0.0D;
            }
        }
        return dirtyPriceD.toString();
    }

    private Collateral getFirstSecCollateral(final SecFinance repo) {
        Collateral collateral = null;
        Vector<Collateral> secCollaterals = repo.getAllSecCollaterals();
        if (!Util.isEmpty(secCollaterals)) {
            collateral = secCollaterals.firstElement();
        }
        return collateral;
    }

    private String getPrincipalCcy(final Repo repo, final int line) {
        String principalCcy = (String) this.marksTable.getValueAt(line, ImportMarksTableModel.PRINCIPAL_CCY_COL_NUM);
        if (Util.isEmpty(principalCcy)) {
            principalCcy = repo.getCurrency();
        }
        return principalCcy;
    }

    private String getPrincipal(final Repo repo, final int line) {
        Double principal = (Double) this.marksTable.getValueAt(line, ImportMarksTableModel.PRINCIPAL_COL_NUM);
        if (principal == null) {
            principal = repo.getPrincipal();
        }
        System.out.println("principal: " + principal);
        return principal.toString();
    }

    private String getNominal(final Trade trade, Repo repo, final int line) {
        Double nominal = (Double) this.marksTable.getValueAt(line, ImportMarksTableModel.NOMINAL_COL_NUM);
        if (nominal == null) {
            nominal = repo.computeNominal(trade);
        }
        System.out.println("Nominal: " + nominal);
        return nominal.toString();
    }

    private String getIndexRatio(final Trade trade, final int line) {
        Double indexRatio = (Double) this.marksTable.getValueAt(line, ImportMarksTableModel.INDEX_RATIO_COL_NUM);
        if (indexRatio == null) {
            PLMark plMark = null;
            try {
                plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(), pricingEnv.getName(),
                        processDate);
            } catch (RemoteException e) {
                Log.error(this, e);
            }

            PLMarkValue plValue = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_CAPITAL_FACTOR);
            if (plValue != null) {
                indexRatio = plValue.getMarkValue();
            } else {
                indexRatio = 0.0D;
            }
        }
        System.out.println("Index Ratio: " + indexRatio);
        return indexRatio.toString();
    }

    private String getMultiCcy(final int line) {
        String multiCcy = (String) this.marksTable.getValueAt(line, ImportMarksTableModel.MULTI_CURRENCY_COL_NUM);
        if (Util.isEmpty(multiCcy)) {
            return "No";
        }
        return multiCcy;
    }


    private void buildAndSavePLMarks(final HashMap<Trade, PLMark> plMarks, final ArrayList<String> errorMessages) {
        // We built all plMarks. Now save all of them.
        try {
            final Iterator<PLMark> iterator = plMarks.values().iterator();

            while (iterator.hasNext()) {

                PLMark plmark = iterator.next();
                plmark = insertDummyPLMark(plmark);
                DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(Arrays.asList(plmark), true);
            }
        } catch (final Exception e) {
            errorMessages.add(e.getMessage());
            Log.error(this.getClass().getName(), e);
        }

        // Display Error messages
        if (!Util.isEmpty(errorMessages)) {
            errorMessages.add("\nAbove errors occured while saving Pricer Measures.\n");
            AppUtil.displayError(this, "Error Messages", errorMessages);
        } else {
            AppUtil.displayMessage("\nSaved Pricer Measures Successfully.", this);
        }
    }

    /**
     * Saves the trades if the action AMEND is applicable
     *
     * @param tradesToSave
     * @param errorMessages
     */
    private void saveTrades(final HashSet<Trade> tradesToSave, final ArrayList<String> errorMessages) {

        ExecutorService executor = getBoundedQueueThreadPoolExecutor(calculateThredPoolSize(tradesToSave),
                errorMessages);

        for (final Trade tradeTemp : tradesToSave) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (isTradeActionApplicable(tradeTemp, com.calypso.tk.core.Action.AMEND)) {
                            tradeTemp.setAction(com.calypso.tk.core.Action.AMEND);
                            DSConnection.getDefault().getRemoteTrade().save(tradeTemp);
                        }
                    } catch (final RemoteException e) {
                        errorMessages.add("Failed to save the Trade , externalRef=" + tradeTemp.getExternalReference()
                                + ", TradeId=" + tradeTemp.getLongId() + ". " + e.getLocalizedMessage());
                        Log.error(this.getClass().getName(), e);
                    }
                }
            });
        }

        shutdownExecutor(executor);
    }

    private void calculateContracts(final HashSet<Integer> contractSet, final ArrayList<String> errorMessages) {
        if (this.calculateContractCheckBox.isSelected() && (!Util.isEmpty(contractSet))) {
            // Now calculate valDate and processDate for contract
            JDate contractValDate = this.processDate;
            final int calculationOffSet = ServiceRegistry.getDefaultContext().getValueDateDays() * 1;
            final JDate contractProcessDate = Holiday.getCurrent().addBusinessDays(contractValDate,
                    DSConnection.getDefault().getUserDefaults().getHolidays(), calculationOffSet);

            //TODO eliminar el try catch solo para desplegar
            try {
                CollateralUtilities.calculateContracts(contractSet,
                        new JDatetime(contractProcessDate, TimeZone.getDefault()), errorMessages);
            } catch (Exception e) {

            }
        }

        // Display Error messages after messages have been calculated
        if (!Util.isEmpty(errorMessages)) {
            AppUtil.displayError(this, "Error Messages", errorMessages);
            return;
        } else {
            AppUtil.displayMessage("\nContracts have been successfully calculated.", this);
        }
    }

    /**
     * Checks if the trade action is applicable.
     *
     * @param transfer the trade
     * @return true if sucess, false otherwise
     */
    protected boolean isTradeActionApplicable(final Trade trade, final com.calypso.tk.core.Action action) {
        return TradeWorkflow.isTradeActionApplicable(trade, action, DSConnection.getDefault(), null);
    }

    /**
     * Clean duplicate NPV_BASE
     */
    private PLMark cleanPlMarks(PLMark currentPLMark, Trade trade, final String plMarkName) {
        // S_NPV_BASE

        if (currentPLMark.getPLMarkValuesByName(plMarkName) != null) {
            // GSM HOT-FIX
            if (currentPLMark.getPLMarkValuesByName(plMarkName).size() <= 1)
                return currentPLMark;
            // ok
            PLMark copy = null;

            try {
                int contractId = trade.getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
                if (contractId == 0) {
                    contractId = Integer.parseInt(trade.getInternalReference());
                }
                final CollateralConfig marginCallConfig = CacheCollateralClient
                        .getCollateralConfig(DSConnection.getDefault(), contractId);

                copy = (PLMark) currentPLMark.clone();

                final PLMark newCleanPlMark = new SantPLMarkBuilder().forTrade(trade)
                        .atDate(copy.getValDate()).withPricingEnv(copy.getPricingEnvName()).build();

                // remove all
                DSConnection.getDefault().getRemoteMark().removeMark(currentPLMark.getId());

                for (PLMarkValue value : copy.getMarkValuesAsList()) {

                    // DISCARD
                    if (value.getMarkName().equals(plMarkName)
                            && !value.getCurrency().equals(marginCallConfig.getCurrency())) {
                        continue;
                    }

                    final PLMarkValue markValue = new PLMarkValue();

                    markValue.setMarkName(value.getMarkName());
                    markValue.setCurrency(value.getCurrency());
                    // Migration V14 - 04012016
                    markValue.setOriginalCurrency(value.getOriginalCurrency());
                    markValue.setMarkValue(value.getMarkValue());
                    markValue.setAdjustmentType(value.getAdjustmentType());
                    if (!Util.isEmpty(value.getAdjustmentComment()))
                        markValue.setAdjustmentComment(value.getAdjustmentComment());

                    newCleanPlMark.addPLMarkValue(markValue);
                }

                DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(Arrays.asList(newCleanPlMark), true);
                return newCleanPlMark;

            } catch (Exception e) {
                Log.error(this, e);
            }

        }
        return currentPLMark;
    }

    private ExecutorService getBoundedQueueThreadPoolExecutor(int noOfThreads, ArrayList<String> errorMessages) {
        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(noOfThreads * 2);
        ExecutorService executor = new ThreadPoolExecutor(noOfThreads, noOfThreads, 0, TimeUnit.SECONDS, blockingQueue,
                new SantImportMTMThreadPolFactory("UnPriceTradeThreadPool", errorMessages),
                new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    private void shutdownExecutor(ExecutorService executor) {

        executor.shutdown();
        try {
            // Await until all the tasks are completed
            executor.awaitTermination(2, TimeUnit.MINUTES);
            Log.info(ScheduledTaskUnPriceTrades.class, "Has Executor been terminated=" + executor.isTerminated());
            Log.info(ScheduledTaskUnPriceTrades.class, "Has Executor been shutdown=" + executor.isShutdown());
        } catch (InterruptedException e) {
            Log.error(ScheduledTaskUnPriceTrades.class, e);
            executor.shutdownNow();
        }
    }

    private int calculateThredPoolSize(HashSet<Trade> tradesToSave) {
        int noOfThreads = 1;
        int tradeCount = tradesToSave.size();

        if (tradeCount > 10) {
            if (tradeCount < 100) {
                noOfThreads = 5;
            } else {
                noOfThreads = 10;
            }
        }

        return noOfThreads;
    }

    /**
     * It inserts a Zero value PLMarkValue for NPV, NPV_BASE and MARGIN_CALL if
     * it is the first time we are saving plmarks for the trade. Reason is that
     * users want, anything saved in the MTMBlotter, to see in MTMVariation and
     * MTMAudit reports.
     *
     * @param plmark
     * @return
     * @throws RemoteException
     * @throws PersistenceException
     */
    private PLMark insertDummyPLMark(PLMark plmark) throws PersistenceException, RemoteException {

        if ((plmark.getId() != 0) || (CollateralUtilities.retrievePLMarkValue(plmark, S_NPV_BASE) == null)) {
            return plmark;
        }
        // save a dummy Zero PLMarkValue for NPV, NPV_BASE and MARGIN_CALL so
        // when you save the actual one, becomes an
        // AMEND and an audit line is created which is used in MTMVariation and
        // MTMAudit reports.
        PLMarkValue origNpvBasePLMarkValue = CollateralUtilities.retrievePLMarkValue(plmark, S_NPV_BASE);
        PLMarkValue origNpvPLMarkValue = CollateralUtilities.retrievePLMarkValue(plmark, S_NPV);
        PLMarkValue origMarginCallPLMarkValue = CollateralUtilities.retrievePLMarkValue(plmark, S_MARGIN_CALL);

        double npvBase = origNpvBasePLMarkValue.getMarkValue();
        double npv = origNpvPLMarkValue.getMarkValue();
        double marginCall = origMarginCallPLMarkValue.getMarkValue();

        origNpvBasePLMarkValue.setMarkValue(0.0);
        origNpvPLMarkValue.setMarkValue(0.0);
        origMarginCallPLMarkValue.setMarkValue(0.0);
        // V14.4 AAP 11/02/2016
        DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(Arrays.asList(plmark), true);
        PLMark newPLMark = CollateralUtilities.retrievePLMarkBothTypes(plmark.getTradeLongId(),
                this.peChoice.getSelectedItem().toString(), processDate);
        origNpvBasePLMarkValue.setMarkValue(npvBase);
        origNpvPLMarkValue.setMarkValue(npv);
        origMarginCallPLMarkValue.setMarkValue(marginCall);

        SantImportMTMUtil.adjustPLMarkValues(newPLMark, origNpvBasePLMarkValue);
        SantImportMTMUtil.adjustPLMarkValues(newPLMark, origNpvPLMarkValue);
        SantImportMTMUtil.adjustPLMarkValues(newPLMark, origMarginCallPLMarkValue);

        return newPLMark;
    }

    /**
     * This method checks if the data entered is valid.
     *
     * @param processDate
     */
    private boolean checkIsValidInput(final JDate processDate) {

        final Vector<String> validCurrencies = LocalCache.getDomainValues(DSConnection.getDefault(),
                CollateralStaticAttributes.DOMAIN_CURRENCY);

        for (int i = 0; i < this.marksTable.getRowCount(); i++) {
            Trade trade = null;

            // Check MarkName
            final String markName = (String) this.marksTable.getValueAt(i,
                    ImportMarksTableModel.MARK_NAME_BASE_COL_NUM);
            if (Util.isEmpty(markName)) {
                return displayError(i, ImportMarksTableModel.MARK_NAME_BASE_COL_NUM,
                        "MarkName in the row " + i + " is empty.");
            }

            // Check Value
            final Amount valueAmt = (Amount) this.marksTable.getValueAt(i,
                    ImportMarksTableModel.PRICER_MEASURE_VALUE_BASE_COL_NUM);

            if (valueAmt == null) {
                return displayError(i, ImportMarksTableModel.PRICER_MEASURE_VALUE_BASE_COL_NUM,
                        "Value(Base) in the row " + i + " is empty.");
            }

            // Check externalRef
            String externalRef = (String) this.marksTableModel.getValueAt(i, ImportMarksTableModel.EXT_REF_COL_NUM);
            if (Util.isEmpty(externalRef)) {
                return displayError(i, ImportMarksTableModel.EXT_REF_COL_NUM,
                        "ExternalRef in the row " + i + " is empty.");
            } else {
                String tradeIdStr = (String) this.marksTableModel.getValueAt(i, ImportMarksTableModel.TRADE_ID_COL_NUM);

                if (this.tradeMap != null) {
                    trade = getTradeByExtRef(this.tradeMap, externalRef, tradeIdStr);
                } else {
                    trade = getTradeByExtRef(externalRef, tradeIdStr);
                }

                if ((trade == null) || (trade.getStatus().toString().equals(Status.CANCELED))
                        || (trade.getStatus().toString().equals(Status.MATURED)
                        && trade.getMaturityDateInclFees().before(processDate))) {
                    return displayError(i, ImportMarksTableModel.EXT_REF_COL_NUM,
                            "Invalid/non-live Trade in the row " + i);
                } else if (trade.getProductType().equals(Repo.REPO)
                        || trade.getProductType().equals(SecLending.SEC_LENDING)
                        || trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)
                        || trade.getProductType().equals(PerformanceSwap.PERFORMANCESWAP)
                        || trade.getProduct() instanceof Bond) {

                    if (trade.getTradeDate().getJDate(TimeZone.getDefault()).after(processDate)) {
                        return displayError(i, ImportMarksTableModel.EXT_REF_COL_NUM,
                                "Process date is earlier than Trade Date in the row " + i);
                    }
                } else {
                    return displayError(i, ImportMarksTableModel.EXT_REF_COL_NUM,
                            "Trade should be either Repo/Seclending/Exposure in the row " + i);
                }
            }

            // Check Ccy
            final String ccy = (String) this.marksTable.getValueAt(i, ImportMarksTableModel.CURRENCY_BASE_COL_NUM);
            if (Util.isEmpty(ccy)) {
                return displayError(i, ImportMarksTableModel.CURRENCY_BASE_COL_NUM,
                        "Currency in the row " + i + " is empty.");
            } else {

                if (!validCurrencies.contains(ccy)) {
                    return displayError(i, ImportMarksTableModel.CURRENCY_BASE_COL_NUM,
                            "Invalid Currency in the row " + i);
                } else {
                    final int contractId = trade.getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
                    if (contractId == 0) {
                        return displayError(i, ImportMarksTableModel.EXT_REF_COL_NUM,
                                "Trade is not indexed in the row " + i);
                    }
                    final CollateralConfig marginCallConfig = CacheCollateralClient
                            .getCollateralConfig(DSConnection.getDefault(), contractId);
                    if (marginCallConfig == null) {
                        return displayError(i, ImportMarksTableModel.EXT_REF_COL_NUM,
                                "MarginCall Contract for Trade is not found in the row " + i);
                    }

                    if (!marginCallConfig.getCurrency().equals(ccy)) {
                        return displayError(i, ImportMarksTableModel.CURRENCY_BASE_COL_NUM,
                                "Currency(Base) should be same as Contract Base Currency ("
                                        + marginCallConfig.getCurrency() + ") in the row " + i);
                    }
                }
            }

            // Check Haircut. It can be null
            // final Double hairCut = (Double) this.marksTable.getValueAt(i,
            // ImportMarksTableModel.OSLAFACTOR_COL_NUM);

            final String reason = (String) this.marksTable.getValueAt(i, ImportMarksTableModel.REASON_COL_NUM);
            if (Util.isEmpty(reason)) {
                return displayError(i, ImportMarksTableModel.REASON_COL_NUM, "Reason in the row " + i + " is empty.");
            }
        }
        return true;
    }

    private boolean displayError(final int row, final int column, final String error) {
        AppUtil.displayError(this, error);
        this.marksTable.changeSelection(row, column, false, false);
        return false;
    }

    /*
     * There is a possibility that we can have more than one trade for the same
     * ExtRef. In which case, if tradeIdStr is passed we only find one trade.
     * Otherwise, when we ask user to choose which trade he wants to use.
     */
    public static Trade getTradeByExtRef(String externalRef, String tradeIdStr) {
        String where = " trade.trade_status<>'CANCELED' ";
        Trade trade = null;
        if (Util.isEmpty(externalRef)) {
            return null;
        } else {
            where = where + " and  trade.external_reference=" + Util.string2SQLString(externalRef);
        }

        try {
            TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade().getTrades(null, where, null, null);
            if (Util.isEmpty(tradeArray)) {
                return null;
            }

            // If you find passed in trade return it
            if (!Util.isEmpty(tradeIdStr)) {
                for (int i = 0; i < tradeArray.size(); i++) {
                    if (tradeIdStr.equals(String.valueOf(tradeArray.get(i).getLongId()))) {
                        return tradeArray.get(i);
                    }
                }
            }

            if (tradeArray.size() > 1) {
                AppUtil.displayMessage("There are more than one trade in the system with External Ref " + externalRef
                        + ". Please choose one in the next screen", null);
                if (_tradeChooser == null) {
                    _tradeChooser = new SantMultiTradeChooser();
                    // _tradeRetriever
                }
                trade = _tradeChooser.pickATrade(tradeArray);
            } else {
                trade = tradeArray.get(0);
            }
        } catch (final Exception exc) {
            Log.warn(SantImportMTMWindow.class, exc);
        }
        return trade;
    }

    public static Trade getTradeByExtRef(Map<String, List<Trade>> tradeMap, String externalRef, String tradeIdStr) {

        Trade trade = null;
        if (Util.isEmpty(externalRef)) {
            return null;
        }

        try {
            List<Trade> list = tradeMap.get(externalRef);
            if (Util.isEmpty(list)) {
                return null;
            }

            // If you find passed in trade return it
            if (!Util.isEmpty(tradeIdStr)) {
                for (int i = 0; i < list.size(); i++) {
                    if (tradeIdStr.equals(String.valueOf(list.get(i).getLongId()))) {
                        return list.get(i);
                    }
                }
            }

            if (list.size() > 1) {
                AppUtil.displayMessage("There are more than one trade in the system with External Ref " + externalRef
                        + ". Please choose one in the next screen", null);
                if (_tradeChooser == null) {
                    _tradeChooser = new SantMultiTradeChooser();
                }
                trade = _tradeChooser.pickATrade(new TradeArray(list));
            } else {
                trade = list.get(0);
            }
        } catch (final Exception exc) {
            Log.warn(SantImportMTMWindow.class, exc);
        }
        return trade;
    }

    protected class ImportMarksTableModel extends TableModelUtil {

        private static final long serialVersionUID = -690294491690071547L;

        protected static final int MARK_NAME_BASE_COL_NUM = 0;
        protected static final int CURRENCY_BASE_COL_NUM = 1;
        protected static final int PRICER_MEASURE_VALUE_BASE_COL_NUM = 2;
        protected static final int EXT_REF_COL_NUM = 3;
        protected static final int OSLAFACTOR_COL_NUM = 4;
        protected static final int REASON_COL_NUM = 5;
        protected static final int CURRENCY_TRADE_COL_NUM = 6;
        protected static final int PRICER_MEASURE_VALUE_TRADE_COL_NUM = 7;
        protected static final int PRODUCTTYPE_COL_NUM = 8;
        protected static final int TRADE_ID_COL_NUM = 9;
        protected static final int BO_REF_COL_NUM = 10;

        protected static final int DIRTY_PRICE_COL_NUM = 11;
        protected static final int PRINCIPAL_CCY_COL_NUM = 12;
        protected static final int PRINCIPAL_COL_NUM = 13;
        protected static final int NOMINAL_COL_NUM = 14;
        //  protected static final int HAIRCUT_COL_NUM = 15;
        protected static final int INDEX_RATIO_COL_NUM = 15;
        protected static final int MULTI_CURRENCY_COL_NUM = 16;

        protected static final String MARK_NAME_BASE_COL = "Mark Name(Base)";
        protected static final String CURRENCY_BASE_COL = "Currency(Base)";
        protected static final String PRICER_MEASURE_VALUE_BASE_COL = "Value(Base)";
        protected static final String EXT_REF_COL = "Ext Ref";
        protected static final String OSLAFACTOR_COL = "OSLA Factor";
        protected static final String REASON_COL = "Reason";
        protected static final String CURRENCY_TRADE_COL = "Currency(Trade)";
        protected static final String PRICER_MEASURE_VALUE_TRADE_COL = "Value(Trade)";
        protected static final String PRODUCTTYPE_COL = "Product Type";
        protected static final String TRADE_ID_COL = "Trade Id";
        protected static final String BO_REF_COL = "BO Ref";

        protected static final String DIRTY_PRICE_COL = "Dirty Price";
        protected static final String PRINCIPAL_CCY_COL = "Principal CCY";
        protected static final String PRINCIPAL_COL = "Principal";
        protected static final String NOMINAL_COL = "Nominal";
        //  protected static final String HAIRCUT_COL = "Haircut";
        protected static final String INDEX_RATIO_COL = "Index Ratio";
        protected static final String MULTI_CURRENCY_COL = "Multi-currency";

        protected static final int TOTAL_COLUMNS = 17;

        protected ImportMarksTableModel() {
            this(0);
        }

        protected ImportMarksTableModel(final int rows) {
            super(TOTAL_COLUMNS, rows);
            setColumnName(MARK_NAME_BASE_COL_NUM, MARK_NAME_BASE_COL);
            setColumnName(CURRENCY_BASE_COL_NUM, CURRENCY_BASE_COL);
            setColumnName(PRICER_MEASURE_VALUE_BASE_COL_NUM, PRICER_MEASURE_VALUE_BASE_COL);
            setColumnName(EXT_REF_COL_NUM, EXT_REF_COL);
            setColumnName(OSLAFACTOR_COL_NUM, OSLAFACTOR_COL);
            setColumnName(REASON_COL_NUM, REASON_COL);
            setColumnName(CURRENCY_TRADE_COL_NUM, CURRENCY_TRADE_COL);
            setColumnName(PRICER_MEASURE_VALUE_TRADE_COL_NUM, PRICER_MEASURE_VALUE_TRADE_COL);
            setColumnName(PRODUCTTYPE_COL_NUM, PRODUCTTYPE_COL);
            setColumnName(TRADE_ID_COL_NUM, TRADE_ID_COL);
            setColumnName(BO_REF_COL_NUM, BO_REF_COL);

            setColumnName(DIRTY_PRICE_COL_NUM, DIRTY_PRICE_COL);
            setColumnName(PRINCIPAL_CCY_COL_NUM, PRINCIPAL_CCY_COL);
            setColumnName(PRINCIPAL_COL_NUM, PRINCIPAL_COL);
            setColumnName(NOMINAL_COL_NUM, NOMINAL_COL);
            //  setColumnName(HAIRCUT_COL_NUM, HAIRCUT_COL);
            setColumnName(INDEX_RATIO_COL_NUM, INDEX_RATIO_COL);
            setColumnName(MULTI_CURRENCY_COL_NUM, MULTI_CURRENCY_COL);

            setColumnClass(MARK_NAME_BASE_COL_NUM, String.class);
            setColumnClass(CURRENCY_BASE_COL_NUM, String.class);
            setColumnClass(PRICER_MEASURE_VALUE_BASE_COL_NUM, Amount.class);
            setColumnClass(EXT_REF_COL_NUM, String.class);
            setColumnClass(OSLAFACTOR_COL_NUM, Double.class);
            setColumnClass(REASON_COL_NUM, String.class);
            setColumnClass(CURRENCY_TRADE_COL_NUM, String.class);
            setColumnClass(PRICER_MEASURE_VALUE_TRADE_COL_NUM, Amount.class);
            setColumnClass(PRODUCTTYPE_COL_NUM, String.class);
            setColumnClass(TRADE_ID_COL_NUM, String.class);
            setColumnClass(BO_REF_COL_NUM, String.class);

            setColumnClass(DIRTY_PRICE_COL_NUM, Double.class);
            setColumnClass(PRINCIPAL_CCY_COL_NUM, String.class);
            setColumnClass(PRINCIPAL_COL_NUM, Double.class);
            setColumnClass(NOMINAL_COL_NUM, Double.class);
            //  setColumnClass(HAIRCUT_COL_NUM, Double.class);
            setColumnClass(INDEX_RATIO_COL_NUM, Double.class);
            setColumnClass(MULTI_CURRENCY_COL_NUM, String.class);

            setColumnEditable(MARK_NAME_BASE_COL_NUM, true);
            setColumnEditable(CURRENCY_BASE_COL_NUM, true);
            setColumnEditable(PRICER_MEASURE_VALUE_BASE_COL_NUM, true);
            setColumnEditable(EXT_REF_COL_NUM, true);
            setColumnEditable(OSLAFACTOR_COL_NUM, true);
            setColumnEditable(REASON_COL_NUM, true);
            setColumnEditable(CURRENCY_TRADE_COL_NUM, false);
            setColumnEditable(PRICER_MEASURE_VALUE_TRADE_COL_NUM, false);
            setColumnEditable(PRODUCTTYPE_COL_NUM, false);
            setColumnEditable(TRADE_ID_COL_NUM, false);
            setColumnEditable(BO_REF_COL_NUM, false);

            setColumnEditable(DIRTY_PRICE_COL_NUM, false);
            setColumnEditable(PRINCIPAL_CCY_COL_NUM, false);
            setColumnEditable(PRINCIPAL_COL_NUM, false);
            setColumnEditable(NOMINAL_COL_NUM, false);
            //  setColumnEditable(HAIRCUT_COL_NUM, false);
            setColumnEditable(INDEX_RATIO_COL_NUM, false);
            setColumnEditable(MULTI_CURRENCY_COL_NUM, false);
        }

        @Override
        public void newValueAt(final int row, final int column, final Object value) {

            boolean isConversionRequired = false;

            if (column == EXT_REF_COL_NUM) {
                if (value != null) {

                    setTradeInfoToCells(row, PRODUCTTYPE_COL_NUM, (String) getValueAt(row, EXT_REF_COL_NUM),
                            (String) getValueAt(row, TRADE_ID_COL_NUM));
                    isConversionRequired = true;
                } else {
                    setValueNoCheck(row, PRODUCTTYPE_COL_NUM, "");
                    refresh();
                }

            } else if (column == CURRENCY_BASE_COL_NUM) {
                isConversionRequired = true;

            } else if (column == PRICER_MEASURE_VALUE_BASE_COL_NUM) {
                isConversionRequired = true;
            }

            if (isConversionRequired) {
                setTradeCcyAndValue(row, column, true);
                refresh();
            }
        }

        public void setTradeCcyAndValue(final int row, final int column, final boolean displayErrMsg) {
            String baseCcy = (String) getValueAt(row, CURRENCY_BASE_COL_NUM);
            Amount baseAmount = (Amount) getValueAt(row, PRICER_MEASURE_VALUE_BASE_COL_NUM);
            String externalRef = (String) getValueAt(row, EXT_REF_COL_NUM);
            String tradeIdStr = (String) getValueAt(row, TRADE_ID_COL_NUM);
            Trade trade = SantImportMTMWindow.getTradeByExtRef(externalRef, tradeIdStr);

            if ((!Util.isEmpty(baseCcy)) && (baseAmount != null) && (trade != null)) {
                // Need to do conversion
                try {
                    final Amount amountTradeCcy = convertAmountToTradeCcy(baseCcy, baseAmount,
                            trade.getTradeCurrency());
                    setValueNoCheck(row, CURRENCY_TRADE_COL_NUM, trade.getTradeCurrency());
                    setValueNoCheck(row, PRICER_MEASURE_VALUE_TRADE_COL_NUM, amountTradeCcy);
                } catch (final MarketDataException e) {
                    Log.error(this, e); //sonar
                    if (displayErrMsg) {
                        displayError(row, column, e.getMessage());
                    }
                    return;
                }
            } else {
                setValueNoCheck(row, CURRENCY_TRADE_COL_NUM, "");
                setValueNoCheck(row, PRICER_MEASURE_VALUE_TRADE_COL_NUM, null);
            }
        }

        public void setTradeInfoToCells(final int row, final int column, String externalRef, String tradeIdStr) {
            String productType = TRADE_NOT_FOUND;
            Trade trade = null;
            try {
                trade = SantImportMTMWindow.getTradeByExtRef(externalRef, tradeIdStr);
                if (trade != null) {
                    productType = trade.getProductType();
                }
            } catch (final Exception exc) {
                Log.warn(SantImportMTMWindow.class, exc);
            }

            setValueNoCheck(row, column, productType);
            if (trade != null) {
                setValueNoCheck(row, TRADE_ID_COL_NUM, String.valueOf(trade.getLongId()));
                setValueNoCheck(row, BO_REF_COL_NUM, trade.getKeywordValue(TradeInterfaceUtils.TRADE_KWD_BO_REFERENCE));

            } else {
                setValueNoCheck(row, TRADE_ID_COL_NUM, "");
                setValueNoCheck(row, BO_REF_COL_NUM, "");
            }

        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            String comboValue = (String) getValueAt(rowIndex, PRODUCTTYPE_COL_NUM);
            String extRef = (String) getValueAt(rowIndex, EXT_REF_COL_NUM);
            List<Integer> columns = new ArrayList<>();
            columns.add(DIRTY_PRICE_COL_NUM);
            columns.add(PRINCIPAL_CCY_COL_NUM);
            columns.add(PRINCIPAL_COL_NUM);
            columns.add(NOMINAL_COL_NUM);
            //columns.add(HAIRCUT_COL_NUM);
            columns.add(INDEX_RATIO_COL_NUM);
            columns.add(MULTI_CURRENCY_COL_NUM);
            if (columns.contains(columnIndex)) {
                if (Repo.REPO.equalsIgnoreCase(comboValue)) {
                    return true;
                } else if (!Util.isEmpty(extRef) && !Repo.REPO.equalsIgnoreCase(comboValue)) {
                    return false;
                }
            }
            return super.isCellEditable(rowIndex, columnIndex);
        }

        public String getProductType(Trade trade) {
            String productType = TRADE_NOT_FOUND;
            if (trade != null) {
                productType = trade.getProductType();
            }
            return productType;
        }

    }

} // End of the Main Class

class SantImportMTMThreadPolFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(0);
    protected String poolName;
    protected ArrayList<String> errorMessages;

    SantImportMTMThreadPolFactory(String poolName, ArrayList<String> errorMessages) {
        this.poolName = poolName;
        this.errorMessages = errorMessages;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread t = new Thread(runnable, this.poolName + "_" + this.threadNumber.incrementAndGet());

        t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                SantImportMTMThreadPolFactory.this.errorMessages
                        .add("Error in thread " + thread.getName() + "; Message=" + throwable.getMessage());
                Log.error("SantImportMTMWindow", "Error in thread " + thread.getName(), throwable);
            }
        });

        return t;
    }
}
