package calypsox.apps.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

import org.apache.camel.component.file.GenericFileOperationFailedException;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import calypsox.apps.marketdata.SantMultiTradeChooser;

import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.ExoticGUIUtils;
import com.calypso.apps.util.TableModelUtil;
import com.calypso.apps.util.TableUtil;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.grid.TableModelWrapperUtils;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;

/**
 * Class used to import collateral?s additional field .
 * 
 * @author pgallegj
 *
 */
public class AdditionalFieldImportWindow extends JFrame {

	protected JPanel topPanel = new JPanel();
	protected JLabel fileNameLabel = new JLabel();
	protected JTextField fileNameText = new JTextField();
	protected JButton fileChooserButton = new JButton();
	protected JFileChooser fileChooser = null;
	public static final String DOMAIN_ADD_INFO="mccAdditionalField";
	public static final String ADDITIONAL_FIELD_IMPORT_LIMIT="AdditionalFieldImportLimit";


	protected JPanel bottomPanel = new JPanel();
	protected JButton importButton = new JButton();
	protected JButton cancelButton = new JButton();

	protected static final String WINDOW_TITLE = "Collateral Additional Field Import";
	protected static final String EXCEL_EXTENSION = "xls";
	protected static final int WIDGET_HEIGHT = 20;

	protected JButton insertRowButton = new JButton();
	protected JButton refreshButton = new JButton();
	protected JButton processButton = new JButton();
	protected JButton deleteButton = new JButton();

	protected JScrollPane markAdjScrollPane = new JideScrollPane();
	protected JideScrollPane scrollPane = new JideScrollPane();
	protected SortableTable table = new SortableTable();
	protected Vector<String> fieldAddInfo;
	protected AdditionalFieldImportUtil tableModel;

	protected static SantMultiTradeChooser tradeChooser = null;
	public final Vector<String> rowData = new Vector<>();
	protected Vector<String> validMarkNames = new Vector<>();
	private TreeMap<Integer, Exception> filasError = new TreeMap<>();

	private static final long serialVersionUID = 123L;
	protected static final int MAX_ATTR_FIX = 15;
	protected int maxAttrib = 15;
	protected int totalColumns = maxAttrib * 2 + 1;
	private String formatString="dd/MM/yyyy";
	
	/**
	 * Constructor method.
	 */
	public AdditionalFieldImportWindow() {
		this(null);
	}

	/**
	 * Constructor method.
	 * 
	 * @param subId
	 */
	public AdditionalFieldImportWindow(final String subId) {
		this.initAdditionalFieldImportWindow();				// to avoid sonar warning: Constructors should only call non-overridable methods
	}
	 
	final void initAdditionalFieldImportWindow() {
		setTitle(WINDOW_TITLE);
		setSize(1000, 495);
		setResizable(true);
		initPanels();
	}

	@SuppressWarnings("deprecation")
	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "MTM");
		final AdditionalFieldImportWindow importWindow = new AdditionalFieldImportWindow();
		importWindow.setVisible(true);
	}

	/**
	 * Method used to init Windows.
	 */
	protected void initPanels() {

		fieldAddInfo = LocalCache.getDomainValues(DSConnection.getDefault(), DOMAIN_ADD_INFO);
		if(LocalCache.getDomainValues(DSConnection.getDefault(), ADDITIONAL_FIELD_IMPORT_LIMIT)!= null || LocalCache.getDomainValues(DSConnection.getDefault(), ADDITIONAL_FIELD_IMPORT_LIMIT).get(0)!=null){
			try{
				maxAttrib = Integer.parseInt(LocalCache.getDomainValues(DSConnection.getDefault(), ADDITIONAL_FIELD_IMPORT_LIMIT).get(0));
			} catch (Exception e){
				Log.info(this, "Can't get maxAttrib. Using default MAX_ATTR_FIX=" + MAX_ATTR_FIX + ". Error: " + e, e);
				maxAttrib=MAX_ATTR_FIX;
			}
		} else {
			maxAttrib=MAX_ATTR_FIX;
		}
		totalColumns = maxAttrib * 2 + 1;
		getContentPane().setLayout(new BorderLayout(0, 0));
		getContentPane().setBackground(AppUtil.makeDarkerColor(getContentPane().getBackground(), ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));
		initBottomPanel();
		initTable();
		initTopPanel();
		
		//Set the Listener button
		final SymAction lSymAction = new SymAction();
		this.fileChooserButton.addActionListener(lSymAction);
		this.importButton.addActionListener(lSymAction);
		this.insertRowButton.addActionListener(lSymAction);
		this.processButton.addActionListener(lSymAction);
		this.refreshButton.addActionListener(lSymAction);
		this.cancelButton.addActionListener(lSymAction);
		this.deleteButton.addActionListener(lSymAction);
	}

	/**
	 * Method used to init the top panel.
	 */
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

		this.topPanel.add(this.fileNameLabel);
		this.topPanel.add(this.fileNameText);
		this.topPanel.add(this.fileChooserButton);

		getContentPane().add(this.topPanel, BorderLayout.NORTH);
	}

	/**
	 * Method used to init the bottom panel.
	 */
	protected void initBottomPanel() {
		this.bottomPanel.setPreferredSize(new Dimension(700, 40));
		this.bottomPanel.setBorder(new PartialEtchedBorder(PartialSide.HORIZONTAL));
		this.bottomPanel.setBackground(AppUtil.makeDarkerColor(this.bottomPanel.getBackground(), ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		this.importButton.setText("Import");
		this.bottomPanel.add(this.importButton);

		// Add Space
		this.bottomPanel.add(new JLabel(" "));
		// Add Space
		this.processButton.setText("Process");
		this.bottomPanel.add(this.processButton);
		this.bottomPanel.add(new JLabel(" "));

		this.deleteButton.setText("Delete Selected");
		this.bottomPanel.add(this.deleteButton);
		this.bottomPanel.add(new JLabel(" "));

		this.insertRowButton.setText("Insert Empty Row");
		this.bottomPanel.add(this.insertRowButton);
		this.bottomPanel.add(new JLabel(" "));

		this.cancelButton.setText("Close");
		this.bottomPanel.add(this.cancelButton);

		getContentPane().add(this.bottomPanel, BorderLayout.SOUTH);
	}

	/**
	 * Method used to init the Table.
	 */
	protected void initTable() {
		final JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.setBackground(AppUtil.makeDarkerColor(tablePanel.getBackground(), ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		this.table.setRowSelectionAllowed(false);
		this.table.setCellSelectionEnabled(true);

		this.tableModel = new AdditionalFieldImportUtil(1,totalColumns,maxAttrib);
		this.tableModel.setTo(this.table);
		this.scrollPane.setViewportView(this.table);
		this.scrollPane.setAutoscrolls(true);
		tablePanel.add(this.scrollPane, BorderLayout.CENTER);

		this.table.setCellRendererManagerEnabled(false);
		createGrid();
		getContentPane().add(tablePanel, BorderLayout.CENTER);
	}

	/**
	 * Method used to create the Grid.
	 */
	private void createGrid() {
		fieldAddInfo.insertElementAt("", 0);
		for (int i = 0; i < maxAttrib; i++) {
			createCellEditor(i * 2 + 1, fieldAddInfo.toArray());
		}
	}

	/**
	 * Method used to create the selector columns.
	 * 
	 * @param column to add the selector
	 * @param values to select
	 */
	public void createCellEditor(final int column, Object[] values) {
		final JComboBox<Object> comboBox = new JComboBox<>(values);
		comboBox.setSelectedIndex(0);
		final TableCellEditor cellEditor = new DefaultCellEditor(comboBox);
		final TableColumn tableColumn = this.table.getColumnModel().getColumn(column);
		tableColumn.setCellEditor(cellEditor);
	}

	/**
	 * Class used to check which button is pressed.
	 * 
	 * @author pgallegj
	 *
	 */
	class SymAction implements java.awt.event.ActionListener {
		@Override
		public void actionPerformed(final java.awt.event.ActionEvent event) {
			final Object object = event.getSource();
			final Cursor origCursor = getCursor();
			try {
				if (object.equals(AdditionalFieldImportWindow.this.importButton)) {			// use "equals" instead of "==" to remove sonar warning "Objects should be compared with "equals()""
					importButtonActionPerformed();
				}

				if (object.equals(AdditionalFieldImportWindow.this.insertRowButton)) {
					insertRowButtonActionPerformed();
				}

				if (object.equals(AdditionalFieldImportWindow.this.processButton)) {
					processButtonActionPerformed();
				}

				if (object.equals(AdditionalFieldImportWindow.this.cancelButton)) {
					cancelButtonActionPerformed();
				}
				if (object.equals(AdditionalFieldImportWindow.this.fileChooserButton)) {
					fileChooserButtonActionPerformed();
				}
				
				if (object.equals(AdditionalFieldImportWindow.this.deleteButton)) {
					deleteSelected();
				}

			} finally {
				setCursor(origCursor);
			}
		}
	}

	/**
	 * Method used to process and import in Calypso the information in the table.
	 */
	public void processButtonActionPerformed() {
		filasError.clear();
		if (this.tableModel.getRowCount() == 0 || (this.tableModel.getRowCount() == 1) && this.table.getValueAt(0, 0) == null) {
			AppUtil.displayError(this, "There are no rows to process.");
			return;
		}
		this.table.editingStopped(null);

		for (int i = 0; i < this.table.getRowCount(); i++) {
			try {
				Double mccId;
				if (this.table.getValueAt(i, 0) instanceof String) {
					String aux = (String) this.table.getValueAt(i, 0);
					try {
						mccId = Double.parseDouble(aux);
					} catch (Exception e) {
						throw new IllegalArgumentException("First column must be a number", e);
					}
				} else {
					mccId = (double) this.table.getValueAt(i, 0);
				}
				HashMap<String, String> addInfo = new HashMap<>();

				CollateralConfig collatConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId.intValue());
				if (collatConfig == null) {
					filasError.put(i+1 , new IllegalArgumentException("ID must exists in Calypso"));
				}
				else {
					for (int j = 1; j < this.table.getColumnCount(); j = j + 2) {
						if (this.table.getValueAt(i, j) == null || this.table.getValueAt(i, j).equals("")) {
							//if key is null or "" , we skip the rest of the line
							break;
						} else {
							try {
								String key = (String) this.table.getValueAt(i, j);
								String value = (String) this.table.getValueAt(i, j + 1);
								Vector<String> reasonValuesTemp = LocalCache.getDomainValues(DSConnection.getDefault(), DOMAIN_ADD_INFO + "." + key);
								//if this attribute has a domain value , the value must be in the domain
								if (!reasonValuesTemp.isEmpty()) {
									if (reasonValuesTemp.contains(value)) {
										addInfo.put(key, value);
									} else {
										throw new IllegalArgumentException(value + " must be " + "in Domain Value " + DOMAIN_ADD_INFO + "." + key);			// split "must be in Domain Value" to avoid Sonar warning
									}
								} else {
									addInfo.put(key, value);
								}
							} catch (IllegalArgumentException e) {
								filasError.put(i+1, e);
							}
						}
					}
						try {
							//Save the new additional fields

							collatConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId.intValue());

							for(Entry<String, String> pair : addInfo.entrySet()){
								collatConfig.setAdditionalField(pair.getKey(), pair.getValue());
							}
							ServiceRegistry.getDefault(DSConnection.getDefault()).getCollateralDataServer().save(collatConfig);
						} catch (CollateralServiceException e) {
							if(e.toString().contains("has been changed already")){
								throw new CalypsoException("Information has been changed alredy.", e);
							} else {
								throw new CalypsoException("Calypso could not process, check the conection.", e);
							}
						}
					}
			} catch (IllegalArgumentException e) {
				filasError.put(i + 1, e);
			}
			catch (Exception e) {
			filasError.put(i+1, e);
			continue;
			}

		}
		if (!filasError.isEmpty()) {
			AppUtil.displayError(this, "Error in rows:" + StringUtils.join(filasError.keySet(), ", ") + "\n" + messageError(filasError));
		}
		if (filasError.size()!=this.table.getRowCount()){
			AppUtil.displayMessage((this.table.getRowCount()-filasError.size())+" rows process successfully.", this,WINDOW_TITLE);
		}
		filasError.clear();
	}
	
	/**
	 * Method used to add a new blank line.
	 */
	void insertRowButtonActionPerformed() {
		this.tableModel.insertRowAt(this.table.getRowCount());
		this.tableModel.refresh();
	}

	/**
	 * Method used to close the windows.
	 */
	void cancelButtonActionPerformed() {
		setVisible(false);
		dispose();
	}

	/**
	 * Method used to select the file to import.
	 */
	protected void fileChooserButtonActionPerformed() {
		final String cwd = System.getProperty("user.dir");
		if (this.fileChooser == null) {
			this.fileChooser = AppUtil.getFileChooser(EXCEL_EXTENSION, "");
			try {
				final File f = new File(cwd);
				this.fileChooser.setCurrentDirectory(f);
			} catch (final Exception e) {
				Log.error(this, e);
			}
		}
		final int returnVal = this.fileChooser.showOpenDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}
		final File dataFile = this.fileChooser.getSelectedFile();

		this.fileNameText.setText(dataFile.getAbsolutePath());
	}

	/**
	 * Method used to check if a file is select and call loadFile.
	 */
	void importButtonActionPerformed() {
		if (this.fileNameText.getText() == null) {
			AppUtil.displayError(this, "Please select a file to Import");
			return;
		}
		try {
			loadFile();
		} catch (final Exception e) {
			Log.error(AdditionalFieldImportWindow.class, e);
			AppUtil.displayError(this, e.getMessage());
			return;
		}

	}
	/**
	 * Method used to loadFile and call readFile.
	 * @throws Exception
	 */
	protected void loadFile() {
		final String fileName = this.fileNameText.getText();

		if (Util.isEmpty(fileName)) {
			AppUtil.displayError(this, "Please select a file to import");
			return;
		}

		// 1. read the contents of the file in to a vector<PLMarkValue>
		Vector<Vector<String>> fileData = null;
		try {
			fileData = readFile(new File(fileName));
			if (Util.isEmpty(fileData)) {
				return;
			}
		} catch (final GenericFileOperationFailedException exc) {
			Log.error(this, exc); // sonar purpose
			AppUtil.displayError(this, exc.getMessage());
			return;
		}

		// 2. reinit the table model to the no of rows
		this.tableModel.reinitRows(fileData.size());

		this.tableModel.notifyOnNewValue(false);
		loadDataTable(fileData);
		this.tableModel.notifyOnNewValue(true);
	}
	
	/**
	 * Method used to read the file and check if something is wrong.
	 * @param file to read
	 * @return
	 * @throws InvalidFormatException 
	 * @throws IOException 
	 */
	public Vector<Vector<String>> readFile(final File file) throws GenericFileOperationFailedException {

		Workbook wb;
		try {
			final InputStream inp = new FileInputStream(file);
			wb = WorkbookFactory.create(inp);
		} catch (InvalidFormatException | IOException e) {
			throw new GenericFileOperationFailedException("Can't open file '" + file + "' or format is invalid: " + e.getMessage(), e);
		}
		// create a workbook out of the input stream
		final Sheet sheet = wb.getSheetAt(0);

		final Vector<Vector<String>> fileData = new Vector<>();
		final Iterator<Row> rowIterator = sheet.rowIterator();
		boolean blank = false;
		int rowCount = 0;
		int importLine=0;
		filasError.clear();

		while (rowIterator.hasNext()) {
			 blank = false;
			 importLine++;

			final Row row = rowIterator.next();
			// exclude the first row which is header
			rowCount++;
			if (rowCount == 1) {
				importLine--;
				continue;
			}
			final Vector<String> dataRow = new Vector<>();

			int i = -1;
			while (true) {
				i++;
				if (i >= totalColumns)
					break;
				if (row.getCell(i) != null) {
					try {
						if (i == 0) {
							try {
								Double aux=row.getCell(i).getNumericCellValue();
								dataRow.add(aux.intValue()+"");
							} catch (Exception e) {
								throw new IllegalArgumentException("First row must be a number", e);
							}
						} else if (i % 2 == 1) {
							//The columns Atributtes has to be in Domain Value
							if (fieldAddInfo.contains(row.getCell(i).getStringCellValue())) {
								if(row.getCell(i).getStringCellValue().equals("")){
									i++;			// forces 'while' instead of 'for' (to avoid sonar warning on modifying 'for' variable)
									continue;
								} else {
									dataRow.add(row.getCell(i).getStringCellValue());
								}
							} else {
								throw new IllegalArgumentException(row.getCell(i).getStringCellValue() + " must be in Domain Value " + DOMAIN_ADD_INFO);
							}
						} else {
							String value="";
							if(row.getCell(i).getCellType()==0){
								if(DateUtil.isCellDateFormatted(row.getCell(i))){
									SimpleDateFormat sdf = new SimpleDateFormat(formatString);
									Date dateAux=row.getCell(i).getDateCellValue();
									value=sdf.format(dateAux);
								}else {
									value=row.getCell(i).getNumericCellValue()+"";
								}
							} else {
								value=row.getCell(i).getStringCellValue();
							}
							String att = row.getCell(i - 1).getStringCellValue();
							Vector<String> reasonValuesTemp = LocalCache.getDomainValues(DSConnection.getDefault(), DOMAIN_ADD_INFO + "." + att);
							if (!reasonValuesTemp.isEmpty()) {
								//if this attribute has a domain value , the value must be in the domain
								if (reasonValuesTemp.contains(value)) {
									dataRow.add(value);
								} else {
									throw new IllegalArgumentException(value + " must be in Domain Value " + DOMAIN_ADD_INFO + "." + att);
								}
							} else {
								dataRow.add(value);
							}
						}
					} catch (IllegalArgumentException e) {
						filasError.put(rowCount - 1, e);
						blank = true;
						importLine--;
						continue;
					}
				} else if (i == 0) {
					rowCount--;
					importLine--;
					blank = true;
					break;
				}
			}
			if (!blank) {
				fileData.add(dataRow);
			}
		}

		if (!filasError.isEmpty()) {
			AppUtil.displayError(this, "Error in rows: " + StringUtils.join(filasError.keySet(), ", ") + "\n" + messageError(filasError));
		}
		
		if (filasError.size()!=rowCount){
			AppUtil.displayMessage(importLine+" rows import successfully.", this,WINDOW_TITLE);
		}
		filasError.clear();

		return fileData;
	}

	/**
	 * Method used to create the message that will be show to the user if something was wrong.
	 * @param filasError2 
	 * @return
	 */
	protected String messageError(TreeMap<Integer, Exception> filasError2) {
		StringBuilder sb = new StringBuilder("");
		SortedSet<Integer> keys = new TreeSet<>(filasError2.keySet());
		for (int key : keys) { 
		   sb.append("Row " + key + ": " + filasError2.get(key).getMessage() + "\n");
		   // do something
		}
		return sb.toString();

	}

	
	/**
	 * Method used to charge the information.
	 * @param fileData
	 */
	protected void loadDataTable(final Vector<Vector<String>> fileData) {
		this.tableModel.reinitRows(fileData.size());
		for (int i = 0; i < fileData.size(); i++) {
			final Vector<String> dataRow = fileData.elementAt(i);
			for (int j = 0; j < dataRow.size(); j++) {
				final Object value = dataRow.elementAt(j);
				this.tableModel.setValueAt(i, j, value);
			}
		}

		this.tableModel.refresh();
		TableUtil.adjust(this.table);
	}

	/**
	 * Method used to delete the rows selected.
	 */
	private void deleteSelected() {
		final int[] selRows = this.table.getSelectedRows();
		if ((selRows == null) || (selRows.length == 0)) {
			// Nothing to delete
			return;
		}
		final HashSet<Integer> rowsToRemove = new HashSet<>();
		for (int i = 0; i < selRows.length; i++) {
			final int realRowIndex = TableModelWrapperUtils.getActualRowAt(this.table.getModel(), selRows[i]);
			if (realRowIndex >= 0) {
				rowsToRemove.add(realRowIndex);
			}
		}

		if (!rowsToRemove.isEmpty()) {
			this.tableModel.removeRows(rowsToRemove);
			this.tableModel.refresh();
		}
	}

}

/**
 * Class used to manage the table.
 * @author pgallegj
 *
 */
class AdditionalFieldImportUtil extends TableModelUtil {

	private static final long serialVersionUID = -690294491690071547L;

	protected static final String FIRST_COLUMN = "ID";
	protected static final String ATTRIBUTE = "Attribute ";
	protected static final String VALUE = "Value ";

	/**
	 * Constructor method.
	 * @param rows
	 * @param totalColumns
	 * @param maxAttrib
	 */
	protected AdditionalFieldImportUtil(final int rows,final int totalColumns, final int maxAttrib) {
		super(totalColumns, rows);
		initAdditionalFieldImportUtil(maxAttrib);		// to avoid sonar error: Constructors should only call non-overridable methods 
	}
	
	final protected void initAdditionalFieldImportUtil(final int maxAttrib) {
		setColumnEditable(0, true);
		setColumnName(0, FIRST_COLUMN);
		for (int i = 0; i < maxAttrib; i++) {
			setColumnEditable(i * 2 + 1, true);
			setColumnName(i * 2 + 1, ATTRIBUTE + i);
			setColumnEditable(i * 2 + 2, true);
			setColumnName(i * 2 + 2, VALUE + i);
		}
	}

}
	

