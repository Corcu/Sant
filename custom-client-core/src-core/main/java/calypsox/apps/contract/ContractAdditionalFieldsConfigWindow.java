package calypsox.apps.contract;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.ExoticGUIUtils;
import com.calypso.apps.util.TableModelUtil;
import com.calypso.apps.util.TableUtil;
import com.calypso.tk.core.Log;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;

public class ContractAdditionalFieldsConfigWindow extends JFrame {

	private static final long serialVersionUID = 123L;

	private static final String WINDOW_TITLE = "Margin Call Additional Fields config window";
	private static final String MCC_ADD_FIELD_DV = "mccAdditionalField";
	private static final String MANDATORY = "|MANDATORY|";

	// panels
	private JPanel buttonsPanel = null;
	private JideScrollPane mccAddFieldTablePanel = null;

	// buttons
	private JButton saveButton = null;
	private JButton closeButton = null;

	// panes & tables & tableModels
	private SortableTable mccAddFieldTable = null;
	private mccAddFieldTableModel mccAddFieldTableModel = null;

	// additionalFields data
	private final HashMap<String, String> data = new HashMap<String, String>();

	// additionalFields to be changed
	private final List<String> addFieldsToBeChanged = new ArrayList<String>();

	/**
	 * Main process
	 */
	public ContractAdditionalFieldsConfigWindow() {

		setTitle(WINDOW_TITLE);
		setSize(500, 500);
		setResizable(false);
		init();
		loadData();

	}

	// *** Panels stuff *** //

	/**
	 * Build window panels, panes and tables
	 */
	protected void init() {

		initMccAddFieldTablePanel();
		initBottomPanel();

	}

	/**
	 * Build additionalFields table panel
	 */
	protected void initMccAddFieldTablePanel() {

		// create table and tableModel
		this.mccAddFieldTable = new SortableTable();
		this.mccAddFieldTableModel = new mccAddFieldTableModel(0);
		this.mccAddFieldTableModel.setTo(this.mccAddFieldTable);

		// create tablePanel, link it to table
		this.mccAddFieldTablePanel = new JideScrollPane();
		this.mccAddFieldTablePanel.setViewportView(this.mccAddFieldTable);
		this.mccAddFieldTablePanel.setAutoscrolls(true);

		// add tablePanel to main frame
		getContentPane().add(this.mccAddFieldTablePanel, BorderLayout.CENTER);

	}

	/**
	 * Build buttons panel
	 */
	protected void initBottomPanel() {

		// define panel
		this.buttonsPanel = new JPanel();
		this.buttonsPanel.setBorder(new PartialEtchedBorder(PartialSide.HORIZONTAL));
		this.buttonsPanel.setBackground(AppUtil.makeDarkerColor(this.buttonsPanel.getBackground(),
				ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		// create buttons
		initButtons();

		// add buttons to panel
		// this.buttonsPanel.add(this.addButton);
		this.buttonsPanel.add(this.saveButton);
		this.buttonsPanel.add(this.closeButton);

		// add panel to main frame
		getContentPane().add(this.buttonsPanel, BorderLayout.SOUTH);

	}

	/**
	 * Create buttons and add them action listeners
	 */
	protected void initButtons() {

		// this.addButton = new JButton("Add");
		// this.addButton.addActionListener(new buttonListener());
		this.saveButton = new JButton("Save");
		this.saveButton.addActionListener(new buttonListener());
		this.closeButton = new JButton("Close");
		this.closeButton.addActionListener(new buttonListener());

	}

	// *** Actions stuff *** //

	/**
	 * buttonListener class: for each button event execute one action
	 */
	protected class buttonListener implements java.awt.event.ActionListener {
		@Override
		public void actionPerformed(final java.awt.event.ActionEvent event) {
			final Object object = event.getSource();
			final Cursor origCursor = getCursor();
			try {
				// Add
				// if (object == ContractAdditionalFieldsConfigWindow.this.addButton) {
				// ContractAdditionalFieldsAddWindow addWindow = new ContractAdditionalFieldsAddWindow();
				// addWindow.setModal(true);
				// addWindow.setVisible(true);
				// // refresh changes
				// addButton_actionPerformed(event);
				// }
				// Save
				if (object == ContractAdditionalFieldsConfigWindow.this.saveButton) {
					saveButton_actionPerformed(event);
				}
				// Close
				if (object == ContractAdditionalFieldsConfigWindow.this.closeButton) {
					closeButton_actionPerformed(event);
				}
			} finally {
				setCursor(origCursor);
			}
		}
	}

	/**
	 * Execute save action. Get data from table and save it to domainValues
	 * 
	 * @param event
	 */
	void saveButton_actionPerformed(final java.awt.event.ActionEvent event) {

		getDataFromTable();
		try {
			saveData();
			AppUtil.displayMessage("DomainValues updated.", this);
		} catch (RemoteException e) {
			Log.error(this, "Error updating DomainValues.\n", e);
			AppUtil.displayError(this, "Error updating DomainValues.");
		}
	}

	/**
	 * Execute close action. Close window.
	 * 
	 * @param event
	 */
	void closeButton_actionPerformed(final java.awt.event.ActionEvent event) {

		setVisible(false);
		dispose();

	}

	/**
	 * Execute add action. Refresh data and window.
	 * 
	 * @param event
	 */
	// void addButton_actionPerformed(final java.awt.event.ActionEvent event) {
	//
	// this.data.clear();
	// this.mccAddFieldTableModel.clean();
	// loadData();
	//
	// }

	// *** Data stuff *** //

	/**
	 * Load data from domainValues to data HashMap
	 */
	protected void getDataFromDB() {

		Vector<String> mccAddFields = CollateralUtilities.getDomainValues(MCC_ADD_FIELD_DV);

		for (String mccAddField : mccAddFields) {
			String comment = CollateralUtilities.getDomainValueComment(MCC_ADD_FIELD_DV, mccAddField);
			this.data.put(mccAddField, comment);
		}

	}

	/**
	 * Get data from DB to data table
	 */
	protected void loadData() {

		// load data in data HashMap
		getDataFromDB();

		int rowsNumber = 0;

		// insert into data table
		for (String mccAddField : this.data.keySet()) {
			this.mccAddFieldTableModel.insertRowAt(rowsNumber);
			this.mccAddFieldTableModel.setValueAt(rowsNumber, 0, mccAddField);
			this.mccAddFieldTableModel.setValueAt(rowsNumber, 1, isMandatory(this.data.get(mccAddField)));
			rowsNumber++;
		}

		this.mccAddFieldTableModel.refresh();
		TableUtil.adjust(this.mccAddFieldTable);

	}

	/**
	 * Save data from data HashMap to domainValues
	 */
	protected void saveData() throws RemoteException {

		for (String mccAddField : this.addFieldsToBeChanged) {
			CollateralUtilities.updateDomainValue(MCC_ADD_FIELD_DV, mccAddField, this.data.get(mccAddField));
		}

		// reset
		this.addFieldsToBeChanged.clear();

	}

	/**
	 * Get data from additionalFields table panel and save it to data HashMap
	 */
	protected void getDataFromTable() {

		for (int i = 0; i < this.mccAddFieldTable.getRowCount(); i++) {

			String mccAddField = (String) this.mccAddFieldTable.getValueAt(i,
					this.mccAddFieldTableModel.MCC_ADD_FIELD_COL);

			Boolean isMandatory = (Boolean) this.mccAddFieldTable.getValueAt(i,
					this.mccAddFieldTableModel.IS_MANDATORY_COL);

			if (isMandatory) {
				// if previously was not mandatory, set to
				if (!this.data.get(mccAddField).contains(MANDATORY)) {
					this.data.put(mccAddField, this.data.get(mccAddField) + MANDATORY);
					// mark field in order to save it after
					this.addFieldsToBeChanged.add(mccAddField);
				}
			} else {
				// if previously was mandatory, set to not
				if (this.data.get(mccAddField).contains(MANDATORY)) {
					this.data.put(mccAddField, this.data.get(mccAddField).replace(MANDATORY, ""));
					// mark field in order to save it after
					this.addFieldsToBeChanged.add(mccAddField);
				}
			}

		}

	}

	/**
	 * Returns TRUE if field is mandatory or FALSE if not. Field is mandatory if value contains word |MANDATORY|
	 * 
	 * @param value
	 * @return
	 */
	private boolean isMandatory(String value) {

		return value.contains(MANDATORY);

	}

	// *** mccAddtionalField TableModel class *** //

	/**
	 * mccAddFieldTableModel: TableModel linked to mccAdditionalField table to manage it
	 */
	protected class mccAddFieldTableModel extends TableModelUtil {

		private static final long serialVersionUID = 123L;

		protected final int MCC_ADD_FIELD_COL = 0;
		protected final int IS_MANDATORY_COL = 1;
		protected final String MCC_ADD_FIELD_COL_NAME = "Additional Field";
		protected final String IS_MANDATORY_COL_NAME = "Is Mandatory";

		protected mccAddFieldTableModel(final int rows) {

			super(2, rows);
			setColumnName(this.MCC_ADD_FIELD_COL, this.MCC_ADD_FIELD_COL_NAME);
			setColumnName(this.IS_MANDATORY_COL, this.IS_MANDATORY_COL_NAME);
			setColumnClass(this.MCC_ADD_FIELD_COL, String.class);
			setColumnClass(this.IS_MANDATORY_COL, Boolean.class);
			setColumnEditable(this.MCC_ADD_FIELD_COL, false);
			setColumnEditable(this.IS_MANDATORY_COL, true);

		}

	}

} // End of the Main Class

