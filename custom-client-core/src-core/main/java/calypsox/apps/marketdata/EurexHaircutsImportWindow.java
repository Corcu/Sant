package calypsox.apps.marketdata;

import java.awt.BorderLayout;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.PropertiesUtils;

import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.ExoticGUIUtils;
import com.calypso.apps.util.TableModelUtil;
import com.calypso.apps.util.TableUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.factory.QuoteTypeEnum;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.ThreadPool;
import com.calypso.tk.util.ThreadPoolListener;
import com.enterprisedt.util.debug.Logger;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;

public class EurexHaircutsImportWindow extends JFrame {

	private static final long serialVersionUID = 123L;

	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger(EurexHaircutsImportWindow.class);

	private static ThreadPool threadPoolReInit;

	private static ThreadPool threadPoolSaving;

	private static final String WINDOW_TITLE = "Import Eurex Haircuts";
	private final static String PROPERTIES_FILE_NAME = "configuration.properties.eurexHaircuts";
	private final static String PATH = "path";
	private final static String FILENAMEREAD = "fileNameRead";
	private final static String FILENAMEIMPORT = "fileNameImport";
	/**
	 * Mandatory fields in the properties file
	 */
	private final static String MANDATORY_FIELDS[] = { PATH, FILENAMEREAD, FILENAMEIMPORT };
	/**
	 * Default values in case there is an error reading the properties file
	 */
	private final static String DEFAULT_VALUES[] = { "C:/calypso_interfaces/tmp", "eurex_haircuts_read_log",
			"eurex_haircuts_import_log" };
	final protected int WIDGET_HEIGHT = 20;
	final protected static String EXCEL_EXTENSION = "xls";
	protected boolean haircutsFromFile = false;
	protected JFileChooser _fileChooser = null;

	// panels
	protected JPanel topPanel = new JPanel();
	protected JPanel bottomPanel = new JPanel();

	// buttons
	protected JButton importButton = new JButton();
	protected JButton cancelButton = new JButton();
	protected JButton loadButton = new JButton();
	protected JButton fileChooserButton = new JButton();

	// labels & text fields
	protected JLabel fileNameLabel = new JLabel();
	protected JTextField fileNameText = new JTextField();

	// panes & tables
	protected JideScrollPane haircutsScrollPane = new JideScrollPane();
	protected SortableTable haircutsTable = new SortableTable();
	protected ImportHaircutsTableModel haircutsTableModel;

	private PropertiesUtils properties;

	@SuppressWarnings("deprecation")
	public static void main(final String... args) throws ConnectException {

		ConnectionUtil.connect(args, "Eurex Haircuts");
		final EurexHaircutsImportWindow importWindow = new EurexHaircutsImportWindow();
		importWindow.show(true);

	}

	public EurexHaircutsImportWindow() {
		this(null, null);
	}

	public EurexHaircutsImportWindow(final JDate adjustDate, final String subId) {
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
		initHaircutsTable();
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

		// add panel elements
		this.topPanel.add(this.fileNameLabel);
		this.topPanel.add(this.fileNameText);
		this.topPanel.add(this.fileChooserButton);
		this.topPanel.add(new JLabel(" ")); // space
		this.topPanel.add(new JLabel(" ")); // space
		this.topPanel.add(new JLabel(" ")); // space
		this.topPanel.add(new JLabel(" ")); // space
		this.topPanel.add(new JLabel(" ")); // space

		getContentPane().add(this.topPanel, BorderLayout.NORTH);

	}

	protected void initBottomPanel() {

		// define panel
		this.bottomPanel.setPreferredSize(new Dimension(700, 40));
		this.bottomPanel.setBorder(new PartialEtchedBorder(PartialSide.HORIZONTAL));
		this.bottomPanel.setBackground(AppUtil.makeDarkerColor(this.bottomPanel.getBackground(),
				ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		this.loadButton.setText("Load haircuts");
		this.bottomPanel.add(this.loadButton);
		this.importButton.setText("Import haircuts");
		this.bottomPanel.add(this.importButton);
		this.cancelButton.setText("Close");
		this.bottomPanel.add(this.cancelButton);

		getContentPane().add(this.bottomPanel, BorderLayout.SOUTH);

	}

	protected void initHaircutsTable() {

		final JPanel haircutsTablePanel = new JPanel(new BorderLayout());
		haircutsTablePanel.setBackground(AppUtil.makeDarkerColor(haircutsTablePanel.getBackground(),
				ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		this.haircutsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.haircutsTable.setRowSelectionAllowed(false);
		this.haircutsTable.setCellSelectionEnabled(true);

		this.haircutsTableModel = new ImportHaircutsTableModel(0);
		this.haircutsTableModel.setTo(this.haircutsTable);
		this.haircutsScrollPane.setViewportView(this.haircutsTable);
		this.haircutsScrollPane.setAutoscrolls(true);
		haircutsTablePanel.add(this.haircutsScrollPane, BorderLayout.CENTER);

		this.haircutsTable.setCellRendererManagerEnabled(false);

		getContentPane().add(haircutsTablePanel, BorderLayout.CENTER);

	}

	// *** Listeners *** //

	protected class SymAction implements java.awt.event.ActionListener {
		@Override
		public void actionPerformed(final java.awt.event.ActionEvent event) {
			final Object object = event.getSource();
			final Cursor origCursor = getCursor();
			try {
				if (object == EurexHaircutsImportWindow.this.importButton) {
					importButton_actionPerformed(event);
				}
				if (object == EurexHaircutsImportWindow.this.cancelButton) {
					cancelButton_actionPerformed(event);
				}
				if (object == EurexHaircutsImportWindow.this.fileChooserButton) {
					fileChooserButton_ActionPerformed(event);
				}
				if (object == EurexHaircutsImportWindow.this.loadButton) {
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

		// if table contains data, remove all before load new one
		if (this.haircutsTable.getRowCount() > 0) {
			this.haircutsTableModel.deleteRows();
		}
		// load new data
		if (this.haircutsFromFile) { // from file selected
			loadHaircutsFromFile();
		} else {
			AppUtil.displayWarning("You must select a file to import.", this);
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
		this.haircutsFromFile = true;

	}

	private class EurexHaircutsThreadPoolListener implements ThreadPoolListener {

		private int numberOfFinishedJobs;

		@Override
		public void newException(ThreadPool t, Runnable job, Exception e) {
		}

		@Override
		public void jobFinished(ThreadPool t, Runnable job) {
			this.numberOfFinishedJobs++;
		}

		public int getNumberOfFinishedJobs() {
			return this.numberOfFinishedJobs;
		}

	}

	// import haircuts from table
	void importButton_actionPerformed(final java.awt.event.ActionEvent event) {

		final ArrayList<String> logMessages = new ArrayList<String>();
		final DSConnection dsCon = DSConnection.getDefault();
		final Vector<QuoteValue> quotes = new Vector<QuoteValue>();
		final RemoteSantCollateralService santCollateralService = (RemoteSantCollateralService) dsCon.getRMIService(
				"baseSantCollateralService", RemoteSantCollateralService.class);

		// clean all the Discountable_Eurex SEC CODES
		try {

			Vector<String> haircutsProductTypes = LocalCache.getDomainValues(dsCon, "EurexProductTypes");

			if (threadPoolReInit == null) {
				threadPoolReInit = new ThreadPool(haircutsProductTypes.size(), "Eurex Haircuts Reinitialization");
			}

			final Vector<Product> products = new Vector<Product>();

			for (final String type : haircutsProductTypes) {
				Runnable importer = new Runnable() {
					@Override
					public void run() {
						products.addAll(BOCache.getExchangeTradedProducts(dsCon, type, null, null, true));
					}
				};
				threadPoolReInit.addJob(importer);
			}

			EurexHaircutsThreadPoolListener tpReinitListener = new EurexHaircutsThreadPoolListener();
			threadPoolReInit.setListener(tpReinitListener);

			while (tpReinitListener.getNumberOfFinishedJobs() < haircutsProductTypes.size()) {
			}

			Vector<Product> toUpdate = new Vector<Product>();

			for (Product prod : products) {
				if (!Util.isEmpty(prod.getSecCode(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_EUREX))) {
					try {
						Product productToUpdate = (Product) prod.cloneIfImmutable();
						productToUpdate.setSecCode(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_EUREX, null);
						toUpdate.add(productToUpdate);
					} catch (CloneNotSupportedException cnse) {
						Log.error(getClass(), cnse);
						AppUtil.displayError(this, cnse.getMessage());
					}
				}
			}

			santCollateralService.clearSecCodesBatch(toUpdate,
					CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_EUREX);

		} catch (RemoteException re) {
			Log.error(getClass(), re);
			AppUtil.displayError(this, re.getMessage());
		}

		final int rowCount = this.haircutsTable.getRowCount();
		final Set<String> quotesToSave = new HashSet<String>(rowCount);
		final Vector<Product> toSave = new Vector<Product>(rowCount);

		Vector<String> threadsNumberDV = LocalCache.getDomainValues(dsCon, "EurexThreadsNumber");

		int threadsNumberfromDV = 0;
		if (Util.isEmpty(threadsNumberDV)) {
			// The default value is 10
			threadsNumberfromDV = 10;
		} else {
			try {
				threadsNumberfromDV = Integer.parseInt(threadsNumberDV.get(0));
			} catch (NumberFormatException nfe) {
				Log.error(getClass(), nfe);
				AppUtil.displayError(this, nfe.getMessage());
			}
		}

		final int threadsNumber = threadsNumberfromDV;

		// import haircuts

		// 1st case : less than 'threadsNumber' rows, no threads pool
		if (rowCount < threadsNumber) {
			for (int i = 0; i < rowCount; i++) {
				processLine(logMessages, dsCon, quotes, toSave, quotesToSave, i);
			}

			// Saving of the newly created quotes
			try {
				dsCon.getRemoteMarketData().saveQuoteValues(quotes);
				santCollateralService.updateBatch(toSave);
			} catch (RemoteException re) {
				Log.error(getClass(), re);
				AppUtil.displayError(this, re.getMessage());
			}
		}

		// More than 'threadsNumber' rows, use of several threads
		else {

			final Set<String> _quotesToSave = Collections.synchronizedSet(new HashSet<String>(rowCount));
			if (threadPoolSaving == null) {
				threadPoolSaving = new ThreadPool(threadsNumber, "Eurex Haircuts Saving");
			}

			for (int i = 0; i < threadsNumber; i++) {
				final int currentRow = i;
				Runnable importer = new Runnable() {
					@Override
					public void run() {
						final Vector<Product> toSave = new Vector<Product>(rowCount);
						final Vector<QuoteValue> quotes = new Vector<QuoteValue>();
						for (int j = 0 + (currentRow * (rowCount / threadsNumber)); j < ((rowCount / threadsNumber) * (currentRow + 1)); j++) {
							processLine(logMessages, dsCon, quotes, toSave, _quotesToSave, j);
						}
						// Saving of the newly created quotes
						try {
							dsCon.getRemoteMarketData().saveQuoteValues(quotes);
							santCollateralService.updateBatch(toSave);
						} catch (RemoteException re) {
							Log.error(getClass(), re);
						}
					}
				};
				threadPoolSaving.addJob(importer);
			}

			// If the row count is not a multiple of the threads number, handle the last rows
			if (rowCount != ((rowCount / threadsNumber) * threadsNumber)) {
				for (int i = (rowCount / threadsNumber) * threadsNumber; i < rowCount; i++) {
					processLine(logMessages, dsCon, quotes, toSave, _quotesToSave, i);
				}
				// Saving of the newly created quotes
				try {
					dsCon.getRemoteMarketData().saveQuoteValues(quotes);
					santCollateralService.updateBatch(toSave);
				} catch (RemoteException re) {
					Log.error(getClass(), re);
					AppUtil.displayError(this, re.getMessage());
				}
			}

			EurexHaircutsThreadPoolListener tpListener = new EurexHaircutsThreadPoolListener();
			threadPoolSaving.setListener(tpListener);

			while (tpListener.getNumberOfFinishedJobs() < threadsNumber) {
			}
		}

		if (logMessages.size() > 0) {

			String path = this.properties.getProperty(PATH);
			File logFile = createLogFile(path, FILENAMEIMPORT);
			try {
				FileWriter logFileWriter = createLogFileWriter(logFile);
				writeLogFile(logFileWriter, logMessages);
				closeLogFile(logFileWriter);
			} catch (IOException e) {
				Log.error(this, "Error creating log", e);
			}

			AppUtil.displayError(this, "Error Messages", logMessages);
		} else {
			AppUtil.displayMessage("Haircuts have been successfully imported.", this);
		}
	}

	@SuppressWarnings("unchecked")
	private void processLine(final ArrayList<String> logMessages, final DSConnection dsCon,
			final Vector<QuoteValue> quotes, final Vector<Product> toSave, final Set<String> quotesToSave,
			int lineNumber) {
		String isin = (String) EurexHaircutsImportWindow.this.haircutsTable.getValueAt(lineNumber,
				ImportHaircutsTableModel.ISIN_COL_NUM);
		Double evalPct = (Double) EurexHaircutsImportWindow.this.haircutsTable.getValueAt(lineNumber,
				ImportHaircutsTableModel.EVAL_PCT_COL_NUM);
		Vector<Product> products = null;
		try {
			products = dsCon.getRemoteProduct().getProductsByCode("ISIN", isin);
		} catch (RemoteException re) {
			Log.error(getClass(), re);
		}
		if (!Util.isEmpty(products)) {
			for (Product product : products) {
				boolean exists = !quotesToSave.add(product.getQuoteName());
				if (!exists) {
					try {
						product = (Product) product.cloneIfImmutable();
						QuoteValue quote = generateHaircutOnProduct(product, lineNumber, logMessages, dsCon);
						if (quote != null) {
							quotes.add(quote);
							toSave.add(product);
							Task task = new Task();
							task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
							task.setEventType("EX_EUREX_HAIRCUT_IMPORT_OK");
							task.setPriority(Task.PRIORITY_NORMAL);
							task.setDatetime(new JDatetime());
							task.setComment("The following line (Row number : " + (lineNumber + 1) + ", Isin : " + isin
									+ ", EvalPct : " + evalPct + " is imported successfully)");

							logMessages.add("The following line (Row number : " + (lineNumber + 1) + ", Isin : " + isin
									+ ", Currency : " + product.getCurrency() + ", EvalPct : " + evalPct
									+ " is imported successfully)");
							dsCon.getRemoteBO().save(task);

						} else {
							Task task = new Task();
							task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
							task.setEventType("EX_EUREX_HAIRCUT_IMPORT_KO");
							task.setPriority(Task.PRIORITY_NORMAL);
							task.setDatetime(new JDatetime());
							task.setComment("The following line (Row number : " + (lineNumber + 1) + ", Isin : " + isin
									+ ", EvalPct : " + evalPct + " is rejected because ISIN doesn't exist)");
							logMessages.add("The following line (Row number : " + (lineNumber + 1) + ", Isin : " + isin
									+ ", EvalPct : " + evalPct + " is rejected because ISIN doesn't exist)");
							dsCon.getRemoteBO().save(task);
						}
					}

					catch (CloneNotSupportedException cnse) {
						Log.error(getClass(), cnse);

					} catch (RemoteException re) {
						Log.error(getClass(), re);
					}
				}
			}
		} else {
			logMessages.add("The following line (Row number : " + (lineNumber + 1) + ", Isin : " + isin
					+ ", EvalPct : " + evalPct + " is rejected because ISIN doesn't exist)");
		}

	}

	// close window
	void cancelButton_actionPerformed(final java.awt.event.ActionEvent event) {
		setVisible(false);
		dispose();
	}

	protected void loadHaircutsFromFile() {

		try {
			loadFile();
		} catch (final Exception e) {
			Log.error(getClass(), e);
			AppUtil.displayError(this, e.getMessage());
			return;
		}

	}

	protected void loadFile() throws Exception {

		final String fileName = this.fileNameText.getText();

		if (Util.isEmpty(fileName)) {
			AppUtil.displayError(this, "Please select a file to import.");
			return;
		}

		// 1. read the contents of the file in to a vector
		Vector<Vector<Object>> fileData = null;
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
		this.haircutsTableModel.reinitRows(fileData.size());
		this.haircutsTableModel.notifyOnNewValue(false);
		loadDataTable(fileData);
		this.haircutsTableModel.notifyOnNewValue(true);
	}

	public Vector<Vector<Object>> readFile(final File file) throws Exception {

		final InputStream inp = new FileInputStream(file);
		final Workbook wb = WorkbookFactory.create(inp);
		final Sheet sheet = wb.getSheetAt(0);
		List<String> logMessage = new ArrayList<String>();

		final Vector<Vector<Object>> fileData = new Vector<Vector<Object>>();
		final Iterator<Row> rowIterator = sheet.rowIterator();
		int rowCount = 0;

		this.properties = new PropertiesUtils(PROPERTIES_FILE_NAME, MANDATORY_FIELDS, DEFAULT_VALUES);
		this.properties.process();

		while (rowIterator.hasNext()) {
			final Row row = rowIterator.next();
			// exclude the first row which is header
			if (rowCount++ == 0) {
				continue;
			}

			Date lastUpdate = null;
			String isin = null;
			String securityGroup = null;
			String securityType = null;
			String depositsAllowed = null;
			Double sharesNominal = null;
			Double collClosingPrice = null;
			Double evalPct = null;
			String usedAsCollateral = null;
			Double freeForDeposit = null;
			String marketPriceValidity = null;
			boolean allCellsEmpty = true;
			Cell cell = null;
			try {
				cell = row.getCell(ImportHaircutsTableModel.LAST_UPDATE_COL_NUM);
				if (cell != null) {
					lastUpdate = cell.getDateCellValue();
					allCellsEmpty = false;
				}
			} catch (IllegalStateException ise) {
				Log.error(this, ise); //sonar
			}
			try {
				cell = row.getCell(ImportHaircutsTableModel.ISIN_COL_NUM);
				if (cell != null) {
					isin = cell.getStringCellValue();
					allCellsEmpty = false;
				}
			} catch (IllegalStateException ise) {
				Log.error(this, ise); //sonar
			}
			cell = row.getCell(ImportHaircutsTableModel.SECURITY_GROUP_COL_NUM);
			if (cell != null) {
				securityGroup = cell.getStringCellValue();
				allCellsEmpty = false;
			}
			cell = row.getCell(ImportHaircutsTableModel.SECURITY_TYPE_COL_NUM);
			if (cell != null) {
				securityType = cell.getStringCellValue();
				allCellsEmpty = false;
			}
			cell = row.getCell(ImportHaircutsTableModel.DEPOSIT_ALLOWED_COL_NUM);
			if (cell != null) {
				depositsAllowed = cell.getStringCellValue();
				allCellsEmpty = false;
			}
			cell = row.getCell(ImportHaircutsTableModel.SHARES_NOMINAL_COL_NUM);
			if (cell != null) {
				sharesNominal = cell.getNumericCellValue();
				if (sharesNominal != 0) {
					allCellsEmpty = false;
				}
			}
			cell = row.getCell(ImportHaircutsTableModel.COLL_CLOSING_PRC_COL_NUM);
			if (cell != null) {
				collClosingPrice = cell.getNumericCellValue();
				if (collClosingPrice != 0) {
					allCellsEmpty = false;
				}
			}
			cell = row.getCell(ImportHaircutsTableModel.EVAL_PCT_COL_NUM);
			if (cell != null) {
				evalPct = cell.getNumericCellValue();
				if (evalPct != 0) {
					allCellsEmpty = false;
				}
			}
			cell = row.getCell(ImportHaircutsTableModel.USED_AS_COLLATERAL_COL_NUM);
			if (cell != null) {
				usedAsCollateral = cell.getStringCellValue();
				allCellsEmpty = false;
			}
			cell = row.getCell(ImportHaircutsTableModel.FREE_FOR_DEPOSIT_COL_NUM);
			if (cell != null) {
				freeForDeposit = cell.getNumericCellValue();
				if (freeForDeposit != 0) {
					allCellsEmpty = false;
				}
			}
			cell = row.getCell(ImportHaircutsTableModel.MARKET_PRICE_VALIDITY_COL_NUM);
			if (cell != null) {
				marketPriceValidity = cell.getStringCellValue();
				allCellsEmpty = false;
			}

			if (allCellsEmpty) {
				break;
			}

			if ((isin != null)
					&& (lastUpdate != null)
					&& (evalPct != null)
					&& ((depositsAllowed != null) && (depositsAllowed.equals("Y") && ((usedAsCollateral != null) && usedAsCollateral
							.equals("Y"))))) {
				final Vector<Object> rowData = new Vector<Object>();
				rowData.add(lastUpdate);
				rowData.add(isin);
				rowData.add(securityGroup);
				rowData.add(securityType);
				rowData.add(depositsAllowed);
				rowData.add(sharesNominal);
				rowData.add(collClosingPrice);
				rowData.add(evalPct);
				rowData.add(usedAsCollateral);
				rowData.add(freeForDeposit);
				rowData.add(marketPriceValidity);
				fileData.add(rowData);

			}

			// rejected rows
			else {
				Task task = new Task();
				DSConnection dsCon = DSConnection.getDefault();
				task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
				task.setEventType("EX_EUREX_HAIRCUT_IMPORT_KO");
				task.setDatetime(new JDatetime());
				task.setPriority(Task.PRIORITY_NORMAL);
				StringBuffer comment = new StringBuffer();

				if (lastUpdate == null) {
					comment.append("Last Update is not a valid date;");
				}

				if (Util.isEmpty(isin)) {
					comment.append(" Isin is empty;");
				}

				if (evalPct == null) {
					comment.append(" EvalPct is null;");
				}

				if ((depositsAllowed == null) || (!depositsAllowed.equals("Y"))) {
					comment.append(" Deposits Allowed is different from Y;");
				}

				if ((usedAsCollateral == null) || (!usedAsCollateral.equals("Y"))) {
					comment.append(" Used as Collateral is different from Y;");
				}

				task.setComment("The following line (Row number : " + row.getRowNum() + ", Isin : " + isin
						+ ", EvalPct : " + evalPct + " is rejected because " + comment.toString());

				logMessage.add("The following line (Row number : " + row.getRowNum() + ", Isin : " + isin
						+ ", EvalPct : " + evalPct + " is rejected because " + comment.toString());

				dsCon.getRemoteBO().save(task);

			}

		}

		if (logMessage.size() > 0) {

			String path = this.properties.getProperty(PATH);
			File logFile = createLogFile(path, FILENAMEREAD);
			try {
				FileWriter logFileWriter = createLogFileWriter(logFile);
				writeLogFile(logFileWriter, logMessage);
				closeLogFile(logFileWriter);
			} catch (IOException e) {
				Log.error(this, "Error creating log", e);
				throw new RemoteException("Error creating log");
			}

		}

		return fileData;
	}

	private File createLogFile(String logPath, String fileName) {

		// get time
		final java.util.Date d = new java.util.Date();
		String time = "";
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

		synchronized (timeFormat) {
			time = timeFormat.format(d);
		}

		// create log file
		return new File(logPath, this.properties.getProperty(fileName) + "_" + time + ".txt");
	}

	private FileWriter createLogFileWriter(File logFile) throws IOException {
		FileWriter logFileWriter = null;
		logFileWriter = new FileWriter(logFile.toString(), true);
		return logFileWriter;
	}

	private void writeLogFile(FileWriter logFileWriter, List<String> errorMessages) throws IOException {
		if (!Util.isEmpty(errorMessages)) {
			for (String errorMessage : errorMessages) {
				logFileWriter.write(errorMessage + "\n");
			}
		}
	}

	private void closeLogFile(FileWriter logFileWriter) throws IOException {
		logFileWriter.close();
	}

	@SuppressWarnings("rawtypes")
	protected void loadDataTable(final Vector<Vector<Object>> contentVec) {
		try {

			this.haircutsTableModel.reinitRows(contentVec.size());
			for (int i = 0; i < contentVec.size(); i++) {
				final Vector rowData = contentVec.elementAt(i);
				for (int j = 0; j < rowData.size(); j++) {
					final Object value = rowData.elementAt(j);
					this.haircutsTableModel.setValueAt(i, j, value);
				}
			}
		} catch (final Exception ex) {
			Log.error(EurexHaircutsImportWindow.class, ex);
		}
		this.haircutsTableModel.refresh();
		TableUtil.adjust(this.haircutsTable);
	}

	@SuppressWarnings("unused")
	private boolean displayError(final int row, final int column, final String error) {
		AppUtil.displayError(this, error);
		this.haircutsTable.changeSelection(row, column, false, false);
		return false;
	}

	public QuoteValue generateHaircutOnProduct(Product product, int row, ArrayList<String> errorMessages,
			DSConnection dsCon) {

		if (product == null) {
			errorMessages.add("Cannot get product on row " + row);
			return null;
		}

		// get the values
		Double evalPct = (Double) this.haircutsTable.getValueAt(row, ImportHaircutsTableModel.EVAL_PCT_COL_NUM);
		Date lastUpdate = (Date) this.haircutsTable.getValueAt(row, ImportHaircutsTableModel.LAST_UPDATE_COL_NUM);
		Double hairCut = 100 - evalPct;

		product.setSecCode(CollateralStaticAttributes.BOND_SEC_CODE_DISCOUNTABLE_EUREX, "true");

		QuoteValue quote = new QuoteValue("Haircuts_Eurex", product.getQuoteName(), JDate.valueOf(lastUpdate),
				QuoteTypeEnum.DISCOUNT.getName());
		quote.setClose(hairCut / 100);
		quote.setEnteredUser(dsCon.getUser());

		return quote;
	}

	// *** Tables classes *** //

	// Data table
	protected class ImportHaircutsTableModel extends TableModelUtil {

		private static final long serialVersionUID = 1L;

		protected static final int LAST_UPDATE_COL_NUM = 0;
		protected static final int ISIN_COL_NUM = 1;
		protected static final int SECURITY_GROUP_COL_NUM = 2;
		protected static final int SECURITY_TYPE_COL_NUM = 3;
		protected static final int DEPOSIT_ALLOWED_COL_NUM = 4;
		protected static final int SHARES_NOMINAL_COL_NUM = 5;
		protected static final int COLL_CLOSING_PRC_COL_NUM = 6;
		protected static final int EVAL_PCT_COL_NUM = 7;
		protected static final int USED_AS_COLLATERAL_COL_NUM = 8;
		protected static final int FREE_FOR_DEPOSIT_COL_NUM = 9;
		protected static final int MARKET_PRICE_VALIDITY_COL_NUM = 10;

		protected static final String LAST_UPDATE_COL = "#LastUpdate";
		protected static final String ISIN_COL = "ISIN";
		protected static final String SECURITY_GROUP_COL = "Security_Group";
		protected static final String SECURITY_TYPE_COL = "Security_Type";
		protected static final String DEPOSIT_ALLOWED_COL = "Deposits_Allowed";
		protected static final String SHARES_NOMINAL_COL = "Shares/Nominal";
		protected static final String COLL_CLOSING_PRC_COL = "CollClosingPrc";
		protected static final String EVAL_PCT_COL = "EvalPct";
		protected static final String USED_AS_COLLATERAL_COL = "Used_as_Collateral";
		protected static final String FREE_FOR_DEPOSIT_COL = "Free_for_deposit";
		protected static final String MARKET_PRICE_VALIDITY_COL = "Market_Price_Validity";

		protected static final int TOTAL_COLUMNS = 11;

		protected ImportHaircutsTableModel() {
			this(0);
		}

		protected ImportHaircutsTableModel(final int rows) {
			super(TOTAL_COLUMNS, rows);
			setColumnName(LAST_UPDATE_COL_NUM, LAST_UPDATE_COL);
			setColumnName(ISIN_COL_NUM, ISIN_COL);
			setColumnName(SECURITY_GROUP_COL_NUM, SECURITY_GROUP_COL);
			setColumnName(SECURITY_TYPE_COL_NUM, SECURITY_TYPE_COL);
			setColumnName(DEPOSIT_ALLOWED_COL_NUM, DEPOSIT_ALLOWED_COL);
			setColumnName(SHARES_NOMINAL_COL_NUM, SHARES_NOMINAL_COL);
			setColumnName(COLL_CLOSING_PRC_COL_NUM, COLL_CLOSING_PRC_COL);
			setColumnName(EVAL_PCT_COL_NUM, EVAL_PCT_COL);
			setColumnName(USED_AS_COLLATERAL_COL_NUM, USED_AS_COLLATERAL_COL);
			setColumnName(FREE_FOR_DEPOSIT_COL_NUM, FREE_FOR_DEPOSIT_COL);
			setColumnName(MARKET_PRICE_VALIDITY_COL_NUM, MARKET_PRICE_VALIDITY_COL);

			setColumnClass(LAST_UPDATE_COL_NUM, Date.class);
			setColumnClass(ISIN_COL_NUM, String.class);
			setColumnClass(SECURITY_GROUP_COL_NUM, String.class);
			setColumnClass(SECURITY_TYPE_COL_NUM, String.class);
			setColumnClass(DEPOSIT_ALLOWED_COL_NUM, String.class);
			setColumnClass(SHARES_NOMINAL_COL_NUM, Double.class);
			setColumnClass(COLL_CLOSING_PRC_COL_NUM, Double.class);
			setColumnClass(EVAL_PCT_COL_NUM, Double.class);
			setColumnClass(USED_AS_COLLATERAL_COL_NUM, String.class);
			setColumnClass(FREE_FOR_DEPOSIT_COL_NUM, Integer.class);
			setColumnClass(MARKET_PRICE_VALIDITY_COL_NUM, String.class);

			setColumnEditable(LAST_UPDATE_COL_NUM, false);
			setColumnEditable(ISIN_COL_NUM, false);
			setColumnEditable(SECURITY_GROUP_COL_NUM, false);
			setColumnEditable(SECURITY_TYPE_COL_NUM, false);
			setColumnEditable(DEPOSIT_ALLOWED_COL_NUM, false);
			setColumnEditable(SHARES_NOMINAL_COL_NUM, false);
			setColumnEditable(COLL_CLOSING_PRC_COL_NUM, false);
			setColumnEditable(EVAL_PCT_COL_NUM, false);
			setColumnEditable(USED_AS_COLLATERAL_COL_NUM, false);
			setColumnEditable(FREE_FOR_DEPOSIT_COL_NUM, false);
			setColumnEditable(MARKET_PRICE_VALIDITY_COL_NUM, false);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		protected void deleteRows() {

			final HashSet<Integer> rowsToRemove = new HashSet<Integer>();

			for (int i = 0; i < getRowCount(); i++) {
				rowsToRemove.add(i);
			}
			if (!rowsToRemove.isEmpty()) {
				removeRows(rowsToRemove);
				refresh();
			}

		}
	}
}
