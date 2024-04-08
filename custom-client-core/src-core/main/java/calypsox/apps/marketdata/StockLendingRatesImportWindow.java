package calypsox.apps.marketdata;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.ExoticGUIUtils;
import com.calypso.apps.util.TableModelUtil;
import com.calypso.apps.util.TableUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.ui.component.table.propertytable.PropertyTableUtilities;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyTable;
import com.jidesoft.grid.PropertyTableModel;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;

import calypsox.tk.product.BondCustomData;
import calypsox.tk.product.EquityCustomData;
import calypsox.tk.report.inventoryview.property.SantFactoryProperty;
import calypsox.util.StockLendingRatesUtilities;

public class StockLendingRatesImportWindow extends JFrame {

	private static final long serialVersionUID = 123L;

	private static final String WINDOW_TITLE = "Import StockLending rates";
	final protected int WIDGET_HEIGHT = 20;
	final protected static String EXCEL_EXTENSION = "xls";
	private static final String ISIN = "ISIN";

	protected boolean ratesFromFile = false;
	protected JFileChooser _fileChooser = null;
	protected Vector<String> validExpiredDateTypes = new Vector<String>();
	protected ProductsCache productsCache = new ProductsCache(); // cache for products

	// panels
	protected JPanel topPanel = new JPanel();
	protected JPanel bottomPanel = new JPanel();

	// buttons
	protected JButton importButton = new JButton();
	protected JButton cancelButton = new JButton();
	protected JButton loadButton = new JButton();
	protected JButton fileChooserButton = new JButton();

	// labels & text fields
	protected JLabel dateLabel = new JLabel();
	protected JTextField dateText = new JTextField();
	protected JLabel fileNameLabel = new JLabel();
	protected JTextField fileNameText = new JTextField();

	// panes & tables
	protected JideScrollPane ratesScrollPane = new JideScrollPane();
	protected SortableTable ratesTable = new SortableTable();
	protected ImportRatesTableModel ratesTableModel;
	protected ProductPropertiesTable productTable = new ProductPropertiesTable();

	@SuppressWarnings("deprecation")
	public static void main(final String... args) throws ConnectException {

		ConnectionUtil.connect(args, "StockLending rates");
		final StockLendingRatesImportWindow importWindow = new StockLendingRatesImportWindow();
		importWindow.show(true);

	}

	public StockLendingRatesImportWindow() {
		this(null, null);
	}

	public StockLendingRatesImportWindow(final JDate adjustDate, final String subId) {
		setTitle(WINDOW_TITLE);
		setSize(780, 300);
		setResizable(true);
		initPanels();
	}

	// *** Panels & tables management *** //

	protected void initPanels() {

		getContentPane().setLayout(new BorderLayout(0, 0));
		getContentPane().setBackground(
				AppUtil.makeDarkerColor(getContentPane().getBackground(), ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		initBottomPanel();
		initRatesTable();
		initTopPanel();

		final SymAction lSymAction = new SymAction();
		this.loadButton.addActionListener(lSymAction);
		this.cancelButton.addActionListener(lSymAction);
		this.fileChooserButton.addActionListener(lSymAction);
		this.importButton.addActionListener(lSymAction);

	}

	protected void initTopPanel() {

		// define panel
		final FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		this.topPanel.setLayout(flowLayout);
		this.topPanel.setBorder(new LineBorder(Color.GRAY, 1, true));

		// define panel elements
		this.fileNameLabel.setHorizontalAlignment(SwingConstants.LEFT);
		this.fileNameLabel.setText("File Name:");
		this.fileNameLabel.setPreferredSize(new Dimension(60, this.WIDGET_HEIGHT));
		this.fileNameText.setText("");
		this.fileNameText.setPreferredSize(new Dimension(300, this.WIDGET_HEIGHT));
		this.fileChooserButton.setText("...");
		this.fileChooserButton.setPreferredSize(new Dimension(22, this.WIDGET_HEIGHT));
		JComponent productTable = this.productTable.getComponent(new Dimension(300, 50));

		// add panel elements
		this.topPanel.add(this.fileNameLabel);
		this.topPanel.add(this.fileNameText);
		this.topPanel.add(this.fileChooserButton);
		this.topPanel.add(new JLabel(" ")); // space
		this.topPanel.add(new JLabel(" ")); // space
		this.topPanel.add(new JLabel(" ")); // space
		this.topPanel.add(new JLabel(" ")); // space
		this.topPanel.add(new JLabel(" ")); // space
		this.topPanel.add(productTable);

		getContentPane().add(this.topPanel, BorderLayout.NORTH);

	}

	protected void initBottomPanel() {

		// define panel
		this.bottomPanel.setPreferredSize(new Dimension(700, 40));
		this.bottomPanel.setBorder(new PartialEtchedBorder(PartialSide.HORIZONTAL));
		this.bottomPanel.setBackground(AppUtil.makeDarkerColor(this.bottomPanel.getBackground(),
				ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		this.loadButton.setText("Load rates");
		this.bottomPanel.add(this.loadButton);
		this.importButton.setText("Import rates");
		this.bottomPanel.add(this.importButton);
		this.cancelButton.setText("Close");
		this.bottomPanel.add(this.cancelButton);

		getContentPane().add(this.bottomPanel, BorderLayout.SOUTH);

	}

	protected void initRatesTable() {

		final JPanel ratesTablePanel = new JPanel(new BorderLayout());
		ratesTablePanel.setBackground(AppUtil.makeDarkerColor(ratesTablePanel.getBackground(),
				ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		this.ratesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.ratesTable.setRowSelectionAllowed(false);
		this.ratesTable.setCellSelectionEnabled(true);

		this.ratesTableModel = new ImportRatesTableModel(0);
		this.ratesTableModel.setTo(this.ratesTable);
		this.ratesScrollPane.setViewportView(this.ratesTable);
		this.ratesScrollPane.setAutoscrolls(true);
		ratesTablePanel.add(this.ratesScrollPane, BorderLayout.CENTER);

		this.ratesTable.setCellRendererManagerEnabled(false);

		// Set table Cell Editors
		setupRatesTableCellEditors();

		getContentPane().add(ratesTablePanel, BorderLayout.CENTER);

	}

	// *** Listeners *** //

	protected class SymAction implements java.awt.event.ActionListener {
		@Override
		public void actionPerformed(final java.awt.event.ActionEvent event) {
			final Object object = event.getSource();
			final Cursor origCursor = getCursor();
			try {
				if (object == StockLendingRatesImportWindow.this.importButton) {
					importButton_actionPerformed(event);
				}
				if (object == StockLendingRatesImportWindow.this.cancelButton) {
					cancelButton_actionPerformed(event);
				}
				if (object == StockLendingRatesImportWindow.this.fileChooserButton) {
					fileChooserButton_ActionPerformed(event);
				}
				if (object == StockLendingRatesImportWindow.this.loadButton) {
					loadButton_ActionPerformed(event);
				}

			} finally {
				setCursor(origCursor);
			}
		}
	}

	// *** Actions *** //

	// load securities on table
	protected void loadButton_ActionPerformed(final java.awt.event.ActionEvent event) {

		List<Integer> securitiesList = new ArrayList<Integer>();
		securitiesList = this.productTable.getSecurityIds();

		// if table contains data, remove all before load new one
		if (this.ratesTable.getRowCount() > 0) {
			this.ratesTableModel.deleteRows();
		}
		// load new data
		if (!Util.isEmpty(securitiesList)) { // from securities selected
			loadRatesFromProductTable(securitiesList);
		} else if (this.ratesFromFile) { // from file selected
			loadRatesFromFile();
		} else {
			AppUtil.displayWarning("You must select securities or file to import.", this);
		}

	}

	// select file to import
	protected void fileChooserButton_ActionPerformed(final java.awt.event.ActionEvent event) {

		final String cwd = System.getProperty("user.dir");
		if (this._fileChooser == null) {
			this._fileChooser = AppUtil.getFileChooser(EXCEL_EXTENSION, "");
			try {
				final File f = new File(cwd);
				this._fileChooser.setCurrentDirectory(f);
			} catch (final Exception e) {
				Log.error(this, e); //sonar
			}
		}
		final int returnVal = this._fileChooser.showOpenDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}
		final File dataFile = this._fileChooser.getSelectedFile();

		this.fileNameText.setText(dataFile.getAbsolutePath());
		this.ratesFromFile = true;

	}

	// import rates from table
	void importButton_actionPerformed(final java.awt.event.ActionEvent event) {

		ArrayList<String> errorMessages = new ArrayList<String>();

		// check input values
		if (!checkIsValidInput()) {
			return;
		}

		// import rates
		for (int i = 0; i < this.ratesTable.getRowCount(); i++) {
			String isin = (String) this.ratesTable.getValueAt(i, ImportRatesTableModel.ISIN_COL_NUM);
			Product product = this.productsCache.getIsinProduct(isin);
			if (product != null) {
				// isin coming from product selector, is in cache list
				saveRateOnProduct(product, i, errorMessages);
			} else {
				// isin coming from file, is not in cache list
				saveRateOnSeveralProducts(isin, i, errorMessages);
			}

		}

		if (errorMessages.size() > 0) {
			AppUtil.displayError(this, "Error Messages", errorMessages);
		} else {
			AppUtil.displayMessage("Rates have been successfully imported.", this);
		}

	}

	// close window
	void cancelButton_actionPerformed(final java.awt.event.ActionEvent event) {
		setVisible(false);
		dispose();
	}

	// *** Auxiliar methods *** //

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void setupRatesTableCellEditors() {

		// assign combo to expired date type column
		final JComboBox<?> expiredDateTypeCombo = new JComboBox(getValidExpiredDateTypes().toArray());
		final TableCellEditor expiredDateTypeEditor = new DefaultCellEditor(expiredDateTypeCombo);
		final TableColumn expiredDateType = this.ratesTable.getColumnModel().getColumn(
				ImportRatesTableModel.EXPIRED_DATE_TYPE_COL_NUM);
		expiredDateType.setCellEditor(expiredDateTypeEditor);

	}

	public Vector<String> getValidExpiredDateTypes() {

		if (this.validExpiredDateTypes.size() == 0) {
			this.validExpiredDateTypes.add("CUSTOM");
			// this.validExpiredDateTypes.add("ALWAYS");
			this.validExpiredDateTypes.add("NEVER");
		}

		return this.validExpiredDateTypes;

	}

	@SuppressWarnings("unchecked")
	protected void loadRatesFromProductTable(List<Integer> securitiesList) {
		// get products
		Map<Integer, Product> products = (Map<Integer, Product>) StockLendingRatesUtilities
				.getSecuritiesFromList(securitiesList);

		if (!Util.isEmpty(products)) {
			for (Product product : products.values()) {
				this.productsCache.setIsinProductMap(product); // save product-ISIN on cache
				this.ratesTableModel.insertRowAt(this.ratesTable.getRowCount());
				this.ratesTableModel.setRateInfoToCells(this.ratesTable.getRowCount() - 1, product.getSecCode("ISIN"),
						product);
			}
		}
	}

	protected void loadRatesFromFile() {

		try {
			loadFile();
		} catch (final Exception e) {
			Log.error(SantImportMTMWindow.class, e);
			AppUtil.displayError(this, e.getMessage());
			return;
		}

	}

	@SuppressWarnings("rawtypes")
	protected void loadFile() throws Exception {

		final String fileName = this.fileNameText.getText();

		if (Util.isEmpty(fileName)) {
			AppUtil.displayError(this, "Please select a file to import.");
			return;
		}

		// 1. read the contents of the file in to a vector
		Vector<Vector> fileData = null;
		try {
			fileData = readFile(new File(fileName));
			if (Util.isEmpty(fileData)) {
				AppUtil.displayError(this, "Could not Parse the Data File");
				return;
			}
		} catch (final MarketDataException exc) {
			Log.error(this, exc); //sonar
			AppUtil.displayError(this, exc.getMessage());
			return;
		}

		// 2. reinit the table model to the no of rows
		this.ratesTableModel.reinitRows(fileData.size());
		this.ratesTableModel.notifyOnNewValue(false);
		loadDataTable(fileData);
		this.ratesTableModel.notifyOnNewValue(true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Vector<Vector> readFile(final File file) throws Exception {

		final InputStream inp = new FileInputStream(file);
		final Workbook wb = WorkbookFactory.create(inp);
		final Sheet sheet = wb.getSheetAt(0);

		final Vector<Vector> fileData = new Vector<Vector>();
		final Iterator rowIterator = sheet.rowIterator();
		int rowCount = 0;

		while (rowIterator.hasNext()) {
			final Row row = (Row) rowIterator.next();
			// exclude the first row which is header
			if (rowCount++ == 0) {
				continue;
			}

			String isin = null;
			Double rate = null;
			String expDateType = null;
			JDate expDate = null;

			// get isin
			if (row.getCell(ImportRatesTableModel.ISIN_COL_NUM) != null) {
				isin = row.getCell(ImportRatesTableModel.ISIN_COL_NUM).getStringCellValue();
			}

			// get rate
			if (row.getCell(ImportRatesTableModel.RATE_COL_NUM) != null) {
				rate = row.getCell(ImportRatesTableModel.RATE_COL_NUM).getNumericCellValue();
			}

			// get expired date type
			if (row.getCell(ImportRatesTableModel.EXPIRED_DATE_TYPE_COL_NUM) != null) {
				if (getValidExpiredDateTypes().contains(
						row.getCell(ImportRatesTableModel.EXPIRED_DATE_TYPE_COL_NUM).getStringCellValue())) {
					expDateType = row.getCell(ImportRatesTableModel.EXPIRED_DATE_TYPE_COL_NUM).getStringCellValue();
				}
			}

			// get expired date
			if (row.getCell(ImportRatesTableModel.EXPIRED_DATE_COL_NUM) != null) {
				if (row.getCell(ImportRatesTableModel.EXPIRED_DATE_COL_NUM).getDateCellValue() != null) {
					expDate = JDate.valueOf(row.getCell(ImportRatesTableModel.EXPIRED_DATE_COL_NUM).getDateCellValue());
				}
			}

			if (!Util.isEmpty(isin) && !Util.isEmpty(expDateType)) { // quizas se necesitan comprobaciones expDateType y
																	 // expDate
				final Vector rowData = new Vector();
				rowData.add(isin);
				rowData.add(rate);
				rowData.add(expDateType);
				rowData.add(expDate);
				fileData.add(rowData);
			}
		}

		return fileData;
	}

	@SuppressWarnings("rawtypes")
	protected void loadDataTable(final Vector<Vector> contentVec) {
		// int cols = marksTableModel.getColumnCount();
		try {

			this.ratesTableModel.reinitRows(contentVec.size());
			for (int i = 0; i < contentVec.size(); i++) {
				final Vector rowData = contentVec.elementAt(i);
				for (int j = 0; j < rowData.size(); j++) {
					final Object value = rowData.elementAt(j);
					this.ratesTableModel.setValueAt(i, j, value);
				}
			}
		} catch (final Exception ex) {
			Log.error(StockLendingRatesImportWindow.class, ex);
		}
		this.ratesTableModel.refresh();
		TableUtil.adjust(this.ratesTable);
	}

	private boolean checkIsValidInput() {

		for (int i = 0; i < this.ratesTable.getRowCount(); i++) {

			// Check rate
			if (this.ratesTable.getValueAt(i, ImportRatesTableModel.RATE_COL_NUM) == null) {

				return displayError(i, ImportRatesTableModel.RATE_COL_NUM, ImportRatesTableModel.RATE_COL
						+ " in the row " + i + " is not valid.");
			}

			// Check expiredDateType
			String expDateType = (String) this.ratesTable
					.getValueAt(i, ImportRatesTableModel.EXPIRED_DATE_TYPE_COL_NUM);
			if (expDateType == null) {
				return displayError(i, ImportRatesTableModel.EXPIRED_DATE_TYPE_COL_NUM,
						ImportRatesTableModel.EXPIRED_DATE_TYPE_COL + " in the row " + i + " is not valid.");
			}

			// Check expiredDate
			JDate expDate = (JDate) this.ratesTable.getValueAt(i, ImportRatesTableModel.EXPIRED_DATE_COL_NUM);
			if ((expDateType.equals("CUSTOM") && (expDate == null))) {
				return displayError(i, ImportRatesTableModel.EXPIRED_DATE_COL_NUM,
						ImportRatesTableModel.EXPIRED_DATE_COL + " in the row " + i + " is not valid.");
			}

		}

		return true;
	}

	private boolean displayError(final int row, final int column, final String error) {
		AppUtil.displayError(this, error);
		this.ratesTable.changeSelection(row, column, false, false);
		return false;
	}

	public void saveRateOnProduct(Product product, int row, ArrayList<String> errorMessages) {

		boolean isUpdated = true;

		if (product == null) {
			errorMessages.add("Cannot get product on row " + row);
			return;
		}

		// get values
		Double rate = (Double) this.ratesTable.getValueAt(row, ImportRatesTableModel.RATE_COL_NUM);
		String expDateType = (String) this.ratesTable.getValueAt(row, ImportRatesTableModel.EXPIRED_DATE_TYPE_COL_NUM);
		JDate expDate = (JDate) this.ratesTable.getValueAt(row, ImportRatesTableModel.EXPIRED_DATE_COL_NUM);

		// bond
		if (product instanceof Bond) {
			BondCustomData bcd = (BondCustomData) product.getCustomData();
			if (bcd == null) {
				bcd = new BondCustomData();
				product.setCustomData(bcd);
			}
			// check updates
			if ((bcd.getFee() == rate) && bcd.getExpired_date_type().equals(expDateType)
					&& (JDate.diff(bcd.getExpired_date(), expDate) == 0)) {
				isUpdated = false;
			}
			// update
			if (isUpdated) {
				bcd.setFee(rate);
				bcd.setExpired_date_type(expDateType);
				bcd.setExpired_date(expDate);
				bcd.setLast_update(JDate.getNow());
				this.ratesTable.setValueAt(JDate.getNow(), row, ImportRatesTableModel.LAST_UPDATE_COL_NUM);
			}
		}

		// equity
		if (product instanceof Equity) {
			EquityCustomData ecd = (EquityCustomData) product.getCustomData();
			if (ecd == null) {
				ecd = new EquityCustomData();
				product.setCustomData(ecd);
			}
			// check updates
			if ((ecd.getFee() == rate) && ecd.getExpired_date_type().equals(expDateType)
					&& (JDate.diff(ecd.getExpired_date(), expDate) == 0)) {
				isUpdated = false;
			}
			// update
			if (isUpdated) {
				ecd.setFee(rate);
				ecd.setExpired_date_type(expDateType);
				ecd.setExpired_date(expDate);
				ecd.setLast_update(JDate.getNow());
				this.ratesTable.setValueAt(JDate.getNow(), row, ImportRatesTableModel.LAST_UPDATE_COL_NUM);
			}
		}

		// save
		try {
			DSConnection.getDefault().getRemoteProduct().saveProduct(product);
			this.ratesTableModel.refresh();
		} catch (RemoteException e) {
			Log.error(this, e);
			errorMessages.add("Cannot save product on row " + (row + 1) + "\nReason is: " + e.getMessage());
		}

	}

	// For ISINs picked from file, in case of duplicates update all ones, like batch import
	@SuppressWarnings("unchecked")
	public void saveRateOnSeveralProducts(String isin, int row, ArrayList<String> errorMessages) {

		try {
			final Vector<Product> matchingProducts = DSConnection.getDefault().getRemoteProduct()
					.getProductsByCode(ISIN, isin);
			if (!Util.isEmpty(matchingProducts)) {
				for (Product matchingProduct : matchingProducts) {
					if (isValidProduct(matchingProduct)) {
						saveRateOnProduct(matchingProduct, row, errorMessages);
					}
				}
			}
		} catch (RemoteException e) {
			Log.error(this, "Error getting products matching with ISIN = " + isin, e);
		}

	}

	// *** Tables classes *** //

	// Data table
	protected class ImportRatesTableModel extends TableModelUtil {

		// private static final long serialVersionUID = -690294491690071547L;
		private static final long serialVersionUID = 123L;

		protected static final int ISIN_COL_NUM = 0;
		protected static final int RATE_COL_NUM = 1;
		protected static final int EXPIRED_DATE_TYPE_COL_NUM = 2;
		protected static final int EXPIRED_DATE_COL_NUM = 3;
		protected static final int LAST_UPDATE_COL_NUM = 4;

		protected static final String ISIN_COL = "ISIN";
		protected static final String RATE_COL = "Rate";
		protected static final String EXPIRED_DATE_TYPE_COL = "Expired date TYPE";
		protected static final String EXPIRED_DATE_COL = "Expired date VALUE";
		protected static final String LAST_UPDATE_COL = "Last update";

		protected static final int TOTAL_COLUMNS = 5;

		protected ImportRatesTableModel() {
			this(0);
		}

		protected ImportRatesTableModel(final int rows) {
			super(TOTAL_COLUMNS, rows);
			setColumnName(ISIN_COL_NUM, ISIN_COL);
			setColumnName(RATE_COL_NUM, RATE_COL);
			setColumnName(EXPIRED_DATE_TYPE_COL_NUM, EXPIRED_DATE_TYPE_COL);
			setColumnName(EXPIRED_DATE_COL_NUM, EXPIRED_DATE_COL);
			setColumnName(LAST_UPDATE_COL_NUM, LAST_UPDATE_COL);

			setColumnClass(ISIN_COL_NUM, String.class);
			setColumnClass(RATE_COL_NUM, Double.class);
			setColumnClass(EXPIRED_DATE_TYPE_COL_NUM, String.class);
			setColumnClass(EXPIRED_DATE_COL_NUM, JDate.class);
			setColumnClass(LAST_UPDATE_COL_NUM, JDate.class);

			setColumnEditable(ISIN_COL_NUM, false);
			setColumnEditable(RATE_COL_NUM, true);
			setColumnEditable(EXPIRED_DATE_TYPE_COL_NUM, true);
			setColumnEditable(EXPIRED_DATE_COL_NUM, true);
			setColumnEditable(LAST_UPDATE_COL_NUM, false);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			String expDateType = (String) getValueAt(row, EXPIRED_DATE_TYPE_COL_NUM);
			if ((column == ISIN_COL_NUM) || (column == LAST_UPDATE_COL_NUM)) {
				return false;
			}
			if ((column == EXPIRED_DATE_COL_NUM) && ("NEVER".equals(expDateType))) {
				return false;
			}
			return true;

		}

		@Override
		public void newValueAt(final int row, final int column, final Object value) {

			if (column == EXPIRED_DATE_TYPE_COL_NUM) {
				String stringValue = (String) value;
				if (stringValue.equals("CUSTOM")) {
					setValueNoCheck(row, column + 1, null);
					// changeCellEnablement(row, column + 1, true);
				}
				if (stringValue.equals("NEVER")) {
					setValueNoCheck(row, column + 1, null);
					// changeCellEnablement(row, column + 1, false);
				}
				// if (stringValue.equals("ALWAYS")) {
				// setValueNoCheck(row, column + 1, JDate.getNow());
				// // changeCellEnablement(row, column + 1, false);
				// }
			}

			if (column == RATE_COL_NUM) {
				String expDateType = (String) getValueAt(row, EXPIRED_DATE_TYPE_COL_NUM);
				if (expDateType.equals("ALWAYS")) {
					setValueNoCheck(row, column + 1, "CUSTOM");
				}
			}

			refresh();

		}

		public void setRateInfoToCells(final int row, String isin, Product product) {

			if (product != null) {
				setValueNoCheck(row, ISIN_COL_NUM, isin);
				if (product instanceof Bond) {
					BondCustomData bcd = (BondCustomData) product.getCustomData();
					if (bcd != null) {
						setValueNoCheck(row, RATE_COL_NUM, bcd.getFee());
						setValueNoCheck(row, EXPIRED_DATE_TYPE_COL_NUM, bcd.getExpired_date_type());
						setValueNoCheck(row, EXPIRED_DATE_COL_NUM, bcd.getExpired_date());
						setValueNoCheck(row, LAST_UPDATE_COL_NUM, bcd.getLast_update());
					}
				}
				if (product instanceof Equity) {
					EquityCustomData ecd = (EquityCustomData) product.getCustomData();
					if (ecd != null) {
						setValueNoCheck(row, RATE_COL_NUM, ecd.getFee());
						setValueNoCheck(row, EXPIRED_DATE_TYPE_COL_NUM, ecd.getExpired_date_type());
						setValueNoCheck(row, EXPIRED_DATE_COL_NUM, ecd.getExpired_date());
						setValueNoCheck(row, LAST_UPDATE_COL_NUM, ecd.getLast_update());
					}
				}
				refresh();
			}

		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		protected void deleteRows() {

			final HashSet rowsToRemove = new HashSet();

			for (int i = 0; i < getRowCount(); i++) {
				rowsToRemove.add(i);
			}
			if (!rowsToRemove.isEmpty()) {
				removeRows(rowsToRemove);
				refresh();
			}

		}

	}

	// Product selector table
	protected class ProductPropertiesTable {

		private PropertyTable productTable = null;

		private Property securityIdsProp;

		private ProductPropertiesTable() {
			initProps();
			display();
		}

		private void display() {
			this.securityIdsProp.setValue(null);
		}

		private void initProps() {
			this.securityIdsProp = makeSecuritiesProperty();
		}

		private Property makeSecuritiesProperty() {
			Vector<String> v = new Vector<String>();
			v.add(0, Product.BOND);
			v.add(0, Product.EQUITY);
			return SantFactoryProperty.makeProductChooserListPorperty("Securities", null, null, null, v);
		}

		public JComponent getComponent() {
			ArrayList<Property> properties = new ArrayList<Property>();

			properties.add(this.securityIdsProp);

			PropertyTableModel<Property> tableModel = new PropertyTableModel<Property>(properties) {

				private static final long serialVersionUID = 123L;

				@Override
				public String getColumnName(int i) {
					if (i == 0) {
						return "<html><b>Product Properties</b></html>";
					}
					return "";
				}
			};
			tableModel.setOrder(PropertyTableModel.UNSORTED);
			this.productTable = new PropertyTable(tableModel);
			this.productTable.expandAll();
			PropertyTableUtilities.setupPropertyTableKeyActions(this.productTable);

			JScrollPane panel = new JScrollPane();
			panel.getViewport().add(this.productTable);
			panel.getViewport().setBackground(this.productTable.getBackground());
			panel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
			this.productTable.getTableHeader().setBackground(Color.black);
			this.productTable.getTableHeader().setForeground(Color.white);

			return panel;
		}

		public JComponent getComponent(Dimension dim) {
			JComponent comp = getComponent();
			comp.setMinimumSize(dim);
			comp.setPreferredSize(dim);
			comp.setMaximumSize(dim);
			return comp;
		}

		@SuppressWarnings("unchecked")
		private List<Integer> getSecurityIds() {
			if (this.securityIdsProp.getValue() != null) {
				return (List<Integer>) this.securityIdsProp.getValue();
			}
			return null;
		}

	}

	// Cache for products
	protected class ProductsCache {

		private final Map<String, Product> isinProductMap;

		private ProductsCache() {
			this.isinProductMap = new HashMap<String, Product>();
		}

		private void setIsinProductMap(Product product) {
			this.isinProductMap.put(product.getSecCode("ISIN"), product);
		}

		private Product getIsinProduct(String isin) {
			return this.isinProductMap.get(isin);
		}

	}

	public boolean isValidProduct(Product product) {
		if ((product != null) && ((product instanceof Bond) || (product instanceof Equity))) {
			return true;
		}
		return false;
	}

} // End of the Main Class
