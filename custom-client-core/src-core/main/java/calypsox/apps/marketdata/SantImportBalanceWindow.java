package calypsox.apps.marketdata;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.ExoticGUIUtils;
import com.calypso.apps.util.TableUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.grid.TableModelWrapperUtils;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;

import calypsox.balances.ProcessBalances;
import calypsox.balances.SantImportBalancesTableModel;
import calypsox.tk.util.SantImportBalanceUtil;

/**
 * @author epalaobe
 *
 */
public class SantImportBalanceWindow extends JFrame implements ActionListener{

	private static final long serialVersionUID = -9038019117675310536L;

	private static final String WINDOW_TITLE = "Balance Import";
	private static final String EXCEL_EXTENSION = "xls";
	private static final int WIDGET_HEIGHT = 20;

	private JButton fileChooserButton = new JButton();
	private JButton processButton = new JButton();
	private JButton importButton = new JButton();
	private JButton cancelButton = new JButton();
	private JButton insertRowButton = new JButton();
	private JButton refreshButton = new JButton();
	private JButton deleteButton = new JButton();

	private JPanel bottomPanel = new JPanel();
	private JPanel topPanel = new JPanel();

	private JLabel balancesLabel = new JLabel();
	private JLabel fileNameLabel = new JLabel();

	private JideScrollPane balancesScrollPane = new JideScrollPane();

	private SortableTable balancesTable = new SortableTable();

	private SantImportBalancesTableModel balancesTableModel;

	private JTextField fileNameText = new JTextField();

	private JFileChooser _fileChooser = null;

	@SuppressWarnings("deprecation")
	public static void main(final String... args) throws ConnectException {

		ConnectionUtil.connect(args, "Balance");
		final SantImportBalanceWindow importWindow = new SantImportBalanceWindow();
		importWindow.show(true);

	}

	public SantImportBalanceWindow() {
		setTitle(WINDOW_TITLE);
		setSize(1000, 495);
		setResizable(true);
		initPanels();

	}

	private void initPanels() {
		getContentPane().setLayout(new BorderLayout(0, 0));
		getContentPane().setBackground(
				AppUtil.makeDarkerColor(getContentPane().getBackground(), ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));
		initBottomPanel();
		initBalancesTable();
		initTopPanel();

		this.fileChooserButton.addActionListener(this);
		this.processButton.addActionListener(this);
		this.importButton.addActionListener(this);
		this.insertRowButton.addActionListener(this);
		this.refreshButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.deleteButton.addActionListener(this);

	}

	private void initBottomPanel() {
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

	private void initBalancesTable() {
		final JPanel balancesTablePanel = new JPanel(new BorderLayout());
		balancesTablePanel.setBackground(AppUtil.makeDarkerColor(balancesTablePanel.getBackground(), ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		this.balancesLabel.setText("Balance Values");
		balancesTablePanel.add(this.balancesLabel, BorderLayout.NORTH);

		this.balancesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.balancesTable.setRowSelectionAllowed(false);
		this.balancesTable.setCellSelectionEnabled(true);

		this.balancesTableModel = new SantImportBalancesTableModel(1);
		this.balancesTableModel.setTo(this.balancesTable);
		this.balancesScrollPane.setViewportView(this.balancesTable);
		this.balancesScrollPane.setAutoscrolls(true);
		balancesTablePanel.add(this.balancesScrollPane, BorderLayout.CENTER);

		this.balancesTable.setCellRendererManagerEnabled(false);

		getContentPane().add(balancesTablePanel, BorderLayout.CENTER);
	}

	private void initTopPanel() {
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

		this.topPanel.add(this.fileNameLabel);
		this.topPanel.add(this.fileNameText);
		this.topPanel.add(this.fileChooserButton);

		getContentPane().add(this.topPanel, BorderLayout.NORTH);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final Object object = e.getSource();
		if (object == this.processButton) {
			processButton_actionPerformed();
		}
		else if (object == this.importButton) {
			importButton_actionPerformed();
		}
		else if (object == this.insertRowButton) {
			insertRowButton_actionPerformed();
		}
		else if (object == this.refreshButton) {
			refreshButton_actionPerformed();
		}
		else if (object == this.cancelButton) {
			cancelButton_actionPerformed();
		}
		else if (object == this.fileChooserButton) {
			fileChooserButton_ActionPerformed();
		}
		else if (object == this.deleteButton) {
			deleteSelectedMarks();
		}
	}

	private void insertRowButton_actionPerformed() {

		this.balancesTableModel.insertRowAt(this.balancesTable.getRowCount());
		this.balancesTableModel.refresh();
	}

	private void refreshButton_actionPerformed() {

		this.balancesTableModel.notifyOnNewValue(false);

		final int rowCount = this.balancesTableModel.getRowCount();
		for (int i = 0; i < rowCount; i++) {
			String contractName = (String) this.balancesTableModel.getValueAt(i, SantImportBalancesTableModel.CONTRACT_NAME_COL_NUM);
			String procesingOrg = (String) this.balancesTableModel.getValueAt(i, SantImportBalancesTableModel.PO_OWNER_COL_NUM);
			String counterParty = (String) this.balancesTableModel.getValueAt(i, SantImportBalancesTableModel.COUNTERPARTY_COL_NUM);

			if (!Util.isEmpty(contractName) && (Util.isEmpty(procesingOrg) && Util.isEmpty(counterParty))) {
				if(SantImportBalanceUtil.isValidContract(contractName)){
					this.balancesTableModel.setCollateralConfigInfoToCells(i, contractName);
				}else {
					AppUtil.displayError(this, "Contract Name: " + contractName + " not exist.");
				}
			}
		}

		this.balancesTableModel.notifyOnNewValue(true);

		TableUtil.adjust(this.balancesTable);
	}

	private void cancelButton_actionPerformed() {
		setVisible(false);
		dispose();
	}

	private void fileChooserButton_ActionPerformed() {
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
	}

	private void importButton_actionPerformed() {
		if (this.fileNameText.getText() == null) {
			AppUtil.displayError(this, "Please select a file to Import");
			return;
		}

		try {
			loadFile();
		} catch (final Exception e) {
			removeAllRows();
			Log.error(SantImportBalanceWindow.class, e);
			AppUtil.displayError(this, e.getMessage());
			return;
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void deleteSelectedMarks() {
		final int[] selRows = this.balancesTable.getSelectedRows();
		if ((selRows == null) || (selRows.length == 0)) {
			// Nothing to delete
			return;
		}
		final HashSet rowsToRemove = new HashSet();
		for (int i = 0; i < selRows.length; i++) {
			final int realRowIndex = TableModelWrapperUtils.getActualRowAt(this.balancesTable.getModel(), selRows[i]);
			if (realRowIndex >= 0) {
				rowsToRemove.add(new Integer(realRowIndex));
			}
		}

		if (!rowsToRemove.isEmpty()) {
			this.balancesTableModel.removeRows(rowsToRemove);
			this.balancesTableModel.refresh();
		}
	}

	protected void loadFile() throws Exception {
		final String fileName = this.fileNameText.getText();

		if (Util.isEmpty(fileName)) {
			AppUtil.displayError(this, "Please select a file to import");
			return;
		}

		// 1. read the contents of the file in to a vector<PLMarkValue>
		@SuppressWarnings("rawtypes")
		Vector<Vector> fileData = null;
		try {
			fileData = readFile(new File(fileName));
		} catch (final MarketDataException exc) {
			Log.error(this, exc); //sonar
			AppUtil.displayError(this, exc.getMessage());
			return;
		}

		// 2. reinit the table model to the no of rows
		this.balancesTableModel.reinitRows(fileData.size() + 5);

		this.balancesTableModel.notifyOnNewValue(false);

		loadDataTable(fileData);

		this.balancesTableModel.notifyOnNewValue(true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Vector<Vector> readFile(final File file) throws Exception {

		final InputStream inp = new FileInputStream(file);
		final Workbook wb = WorkbookFactory.create(inp);
		// create a workbook out of the input stream
		final Sheet sheet = wb.getSheetAt(0);

		final Vector<Vector> fileData = new Vector<Vector>();
		final Iterator rowIterator = sheet.rowIterator();

		int rowCount = 0;
		String errores = null;
		while (rowIterator.hasNext()) {
			Row row = (Row) rowIterator.next();
			// exclude the first row which is header
			if (rowCount++ == 0) {
				continue;
			}

			String procesingOrg = null;
			String counterParty = null;
			String contractName = null;
			String isin = null;
			Double nominal = null;
			String fatherId = null;
			JDate valueDate = null;
			String sendMovement = null;
			String type = null;
			String currency = null;

			if (row.getCell(SantImportBalancesTableModel.PO_OWNER_COL_NUM) != null) {
				if (SantImportBalanceUtil.isValidLE(row.getCell(SantImportBalancesTableModel.PO_OWNER_COL_NUM).getStringCellValue())) {
					procesingOrg = row.getCell(SantImportBalancesTableModel.PO_OWNER_COL_NUM).getStringCellValue();
				}
			}

			if (row.getCell(SantImportBalancesTableModel.COUNTERPARTY_COL_NUM) != null) {
				if (SantImportBalanceUtil.isValidLE(row.getCell(SantImportBalancesTableModel.COUNTERPARTY_COL_NUM).getStringCellValue())) {
					counterParty = row.getCell(SantImportBalancesTableModel.COUNTERPARTY_COL_NUM).getStringCellValue();
				}
			}

			if (row.getCell(SantImportBalancesTableModel.CONTRACT_NAME_COL_NUM) != null) {
				if (SantImportBalanceUtil.isValidContract(row.getCell(SantImportBalancesTableModel.CONTRACT_NAME_COL_NUM).getStringCellValue())) {
					contractName = row.getCell(SantImportBalancesTableModel.CONTRACT_NAME_COL_NUM).getStringCellValue();
					if(Util.isEmpty(procesingOrg) && Util.isEmpty(counterParty)){
						this.balancesTableModel.setCollateralConfigInfoToCells(rowCount, contractName);
					}
				}else{
					errores = addError(errores,  "Row: " + rowCount + " - Contract Name don't Exist!");
				}
			}else{
				errores = addError(errores,  "Row: " + rowCount + " - Contract Name is null but is mandatory!");
			}

			if (row.getCell(SantImportBalancesTableModel.ISIN_COL_NUM) != null) {
				isin = row.getCell(SantImportBalancesTableModel.ISIN_COL_NUM).getStringCellValue();
			}

			if (row.getCell(SantImportBalancesTableModel.NOMINAL_COL_NUM) != null) {
				nominal = row.getCell(SantImportBalancesTableModel.NOMINAL_COL_NUM).getNumericCellValue();
			}else{
				errores = addError(errores,  "Row: " + rowCount + " - Nominal is null but is mandatory!");
			}

			if (row.getCell(SantImportBalancesTableModel.FATHER_ID_COL_NUM) != null) {
				fatherId = ""; //row.getCell(ImportBalancesTableModel.FATHER_ID_COL_NUM).getStringCellValue();
			}

			if (row.getCell(SantImportBalancesTableModel.VALUE_DATE_COL_NUM) != null) {
				if(row.getCell(SantImportBalancesTableModel.VALUE_DATE_COL_NUM).getDateCellValue()!=null){
					valueDate = JDate.valueOf(row.getCell(SantImportBalancesTableModel.VALUE_DATE_COL_NUM).getDateCellValue());
				}else{
					errores = addError(errores,  "Row: " + rowCount + " - Value Date is not in correct format!");
				}
			}else{
				errores = addError(errores,  "Row: " + rowCount + " - Value Date is null but is mandatory!");
			}

			if (row.getCell(SantImportBalancesTableModel.SEND_MOVEMENT_COL_NUM) != null) {
				sendMovement = row.getCell(SantImportBalancesTableModel.SEND_MOVEMENT_COL_NUM).getStringCellValue();
			}

			if (row.getCell(SantImportBalancesTableModel.TYPE_COL_NUM) != null) {
				if (SantImportBalanceUtil.isValidType(row.getCell(SantImportBalancesTableModel.TYPE_COL_NUM).getStringCellValue())) {
					type = row.getCell(SantImportBalancesTableModel.TYPE_COL_NUM).getStringCellValue();
				}else{
					errores = addError(errores,  "Row: " + rowCount + " - This type is not valid!");
				}
			}else{
				errores = addError(errores,  "Row: " + rowCount + " - Type is null but is mandatory!");
			}

			if (row.getCell(SantImportBalancesTableModel.CURRENCY_COL_NUM) != null) {
				currency = row.getCell(SantImportBalancesTableModel.CURRENCY_COL_NUM).getStringCellValue();
			}else{
				errores = addError(errores,  "Row: " + rowCount + " - Currency is null but is mandatory!");
			}

			if(!SantImportBalanceUtil.isValidIsinForBond(isin, type)){
				errores = addError(errores,  "Row: " + rowCount + " - ISIN don't exist or is null, but is mandatory for type Security!");
			}

			if (!Util.isEmpty(contractName) && valueDate!=null && !Util.isEmpty(currency) && !Util.isEmpty(type) && SantImportBalanceUtil.isValidIsinForBond(isin, type)) {
				Vector rowData = new Vector();
				rowData.add(procesingOrg);
				rowData.add(counterParty);
				rowData.add(contractName);
				rowData.add(isin);
				rowData.add(nominal);
				rowData.add(fatherId);
				rowData.add(valueDate);
				rowData.add(sendMovement);
				rowData.add(type);
				rowData.add(currency);

				fileData.add(rowData);
			}
		}

		
		if(!Util.isEmpty(errores)){
			AppUtil.displayError(this, errores);
		}
		
		return fileData;
	}
	
	private String addError(String errores, String error){
		if(errores == null){
			errores = error;
		} else {
			errores = errores + "\n" + error;
		}
		return errores;
	}

	@SuppressWarnings("rawtypes")
	protected void loadDataTable(final Vector<Vector> contentVec) {
		try {
			this.balancesTableModel.reinitRows(contentVec.size());
			for (int i = 0; i < contentVec.size(); i++) {
				final Vector rowData = contentVec.elementAt(i);
				for (int j = 0; j < rowData.size(); j++) {
					final Object value = rowData.elementAt(j);
					this.balancesTableModel.setValueAt(i, j, value);
				}
			}
		} catch (final Exception ex) {
			Log.error(SantImportBalanceWindow.class, ex);
		}
		this.balancesTableModel.refresh();
		TableUtil.adjust(this.balancesTable);
	}

	private void processButton_actionPerformed() {
		if (this.balancesTableModel.getRowCount() == 0) {
			AppUtil.displayError(this, "There are no rows to process.");
			return;
		}else{
			refreshButton_actionPerformed();
			ProcessBalances process = new ProcessBalances(balancesTableModel);
			this.balancesTableModel =  process.start();
			if(this.balancesTableModel.getRowCount()>0){
				AppUtil.displayMessage("Inserted "+this.balancesTableModel.getRowCount()+" row in the system", this);
				this.balancesTable.setModel(this.balancesTableModel);
			}
		}
		this.balancesTable.editingStopped(null);
	}


	private void removeAllRows(){
		int rowCount = this.balancesTableModel.getRowCount();
		//Remove rows one by one from the end of the table
		for (int i = rowCount - 1; i >= 0; i--) {
			this.balancesTableModel.removeRow(i);
		}
		this.balancesTableModel.refresh();
	}
} // End of the Main Class

class SantImportBalanceThreadPolFactory implements ThreadFactory {

	private final AtomicInteger threadNumber = new AtomicInteger(0);
	protected String poolName;
	protected ArrayList<String> errorMessages;

	SantImportBalanceThreadPolFactory(String poolName, ArrayList<String> errorMessages) {
		this.poolName = poolName;
		this.errorMessages = errorMessages;
	}

	@Override
	public Thread newThread(Runnable runnable) {
		Thread t = new Thread(runnable, this.poolName + "_" + this.threadNumber.incrementAndGet());

		t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable throwable) {
				SantImportBalanceThreadPolFactory.this.errorMessages.add("Error in thread " + thread.getName() + "; Message=" + throwable.getMessage());
				Log.error("SantImportBalanceWindow", "Error in thread " + thread.getName(), throwable);
			}
		});

		return t;
	}
}
