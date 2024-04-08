
package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.calypso.apps.reporting.BOSecurityPositionReportTemplatePanel;
import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.User;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantCheckBoxPanel;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.SACCRBalancesReport;
import calypsox.tk.report.SACCRBalancesReportTemplate;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;

/**
 *
 * SA CCR Balances Panel (new report for IRIS balances files). Provides same template attributes for BOPositions reports plus
 * some custom panels.
 * 
 * NOTE: due to time loading SD Filters in Panel startup, it is loaded in second thread.
 * 
 * @author Damian Mascarella
 * @version 1.2
 * @Date 04/01/2017
 */

public class SACCRBalancesReportTemplatePanel extends SantGenericReportTemplatePanel {

	/**
	 * Generated Serial UID
	 */
	private static final long serialVersionUID = 4528232294293955567L;

	/*
	 * Balance types constants
	 */
	public final static String BALANCE = "Balance";
	public final static String MOVEMENTS = "Movements";

	/*
	 *  Inventory postion type constants
	 */
	public final static String ACTUAL = "Actual";;
	public final static String NOT_SETTLED = "Not settled";
	public final static String THEORETICAL = "Theoretical";

	/*
	 *  Inventory date used constants
	 */
	public final static String BOOKING = "Booking";
	public final static String SETTLE = "Settle";
	public final static String TRADE = "Trade";
	public final static String VALUE = "Value";

	/**
	 * Variables
	 */
	private SantComboBoxPanel<String, String> positionValueChoice;
	private SantComboBoxPanel<String, String> cashSecurityChoice;
	private SantComboBoxPanel<String, String> filterChoice;
	private CalypsoCheckBox filterZeroCheck;
	private CalypsoCheckBox filterMaturedCheck;
	private SantProcessDatePanel processDatePanel;
	private SantComboBoxPanel<String, String> movementType;
	private SantComboBoxPanel<String, String> inventoryTypes;
	private SantComboBoxPanel<String, String> inventoryDate;
	private SantComboBoxPanel<String, String> maturityOffSet;
	private SantChooseButtonPanel agreementTypePanel;
	private SantCheckBoxPanel matureDealsPanel;
	private SantCheckBoxPanel mtmZeroPanel;
	private SantComboBoxPanel<String, String> securitiesFilterChoice;

	/**
	 * Default constructor
	 */
	public SACCRBalancesReportTemplatePanel() {
		setPanelVisibility();
	}

	protected Dimension getPanelSize() {
		return new Dimension(0, 180);
	}

	/**
	 * Process date of the process
	 */
	protected Component getNorthPanel() {

		this.processDatePanel = new SantProcessDatePanel("Process Date");
		this.processDatePanel.setPreferredSize(new Dimension(80, 24), new Dimension(215, 24));
		this.processDatePanel.removeDateLabel();

		return this.processDatePanel;
	}

	/* Report title
	 * @see calypsox.apps.reporting.util.SantGenericReportTemplatePanel#getMasterPanelBorder()
	 */
	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("SA-CCR Balance");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	// First column, they contain: MoventType, Inventory filter, SD filter,
	// inventory type, inventory date.
	@Override
	protected JPanel getColumn1Panel() {

		Worker worker = new Worker();
		worker.execute();

		final JPanel column1Panel = new JPanel();
		column1Panel.removeAll();

		GridLayout gl = new GridLayout(6, 4);
		column1Panel.setLayout(gl);
		gl.setHgap(9);
		gl.setVgap(9);

		this.movementType = new SantComboBoxPanel<String, String>("Movement Type", getMovementTypes());
		this.movementType.setBounds(10, 50, 280, 24);
		this.movementType.setPreferredSize(new Dimension(240, 24));
		movementType.setEditable(false);
		column1Panel.add(this.movementType);
		
		this.inventoryTypes = new SantComboBoxPanel<String, String>("Inventory Type", getInventoryTypes());
		this.inventoryTypes.setBounds(10, 50, 280, 24);
		this.inventoryTypes.setPreferredSize(new Dimension(240, 24));
		this.inventoryTypes.setEditable(false);
		column1Panel.add(this.inventoryTypes);

		this.inventoryDate = new SantComboBoxPanel<String, String>("Inventory Date", getInventoryDate());
		this.inventoryDate.setBounds(10, 50, 280, 24);
		this.inventoryDate.setPreferredSize(new Dimension(240, 24));
		this.inventoryDate.setEditable(false);
		column1Panel.add(this.inventoryDate);

		this.filterChoice = new SantComboBoxPanel<String, String>("Inventory Filter", getInventoryFilters());
		this.filterChoice.setBounds(10, 50, 280, 24);
		this.filterChoice.setPreferredSize(new Dimension(240, 24));
		this.filterChoice.setEditable(false);
		column1Panel.add(this.filterChoice);

		this.securitiesFilterChoice = new SantComboBoxPanel<String, String>("SD Filter", getEmpty());
		this.securitiesFilterChoice.setBounds(10, 50, 280, 24);
		this.securitiesFilterChoice.setPreferredSize(new Dimension(240, 24));
		this.securitiesFilterChoice.setEditable(false);
		column1Panel.add(this.securitiesFilterChoice);

		return column1Panel;
	}

	/**
	 *  Second column, contains: agreementType, agreementName, POs, position & Cash/Security.
	 * @see calypsox.apps.reporting.util.SantGenericReportTemplatePanel#getColumn2Panel()
	 */
	@Override
	protected JPanel getColumn2Panel() {

		BOSecurityPositionReportTemplatePanel panel = new BOSecurityPositionReportTemplatePanel();
		this.agreementTypePanel = new SantChooseButtonPanel("Agr Type", "legalAgreementType");

		final JPanel column2Panel = new JPanel();
		column2Panel.removeAll();

		GridLayout gl = new GridLayout(6, 4);
		column2Panel.setLayout(gl);
		gl.setHgap(9);
		gl.setVgap(9);

		column2Panel.add(panel.add(agreementNamePanel));
		this.agreementNamePanel.setPreferredSize(new Dimension(240, 24));
		this.agreementNamePanel.setBounds(10, 50, 280, 24);
		column2Panel.add(panel.add(poAgrPanel));
		this.poAgrPanel.setPreferredSize(new Dimension(240, 24));
		this.poAgrPanel.setBounds(10, 50, 280, 24);
		column2Panel.add(panel.add(agreementTypePanel));
		this.agreementTypePanel.setPreferredSize(new Dimension(240, 24));
		this.agreementTypePanel.setBounds(10, 50, 280, 24);

		this.positionValueChoice = new SantComboBoxPanel<String, String>("Position Value", getpositionValueType());
		this.positionValueChoice.setBounds(10, 50, 280, 24);
		this.positionValueChoice.setPreferredSize(new Dimension(240, 24));
		this.positionValueChoice.setEditable(false);
		column2Panel.add(this.positionValueChoice);

		this.cashSecurityChoice = new SantComboBoxPanel<String, String>("Cash/Sec", getcashSecurity());
		this.cashSecurityChoice.setEditable(false);
		column2Panel.add(this.cashSecurityChoice);
		this.cashSecurityChoice.setBounds(10, 50, 280, 24);
		this.cashSecurityChoice.setPreferredSize(new Dimension(240, 24));

		return column2Panel;
	}

	/**
	 *  Third column, they contains: Filter zero balance, matured zero check & maturity off set.
	 */
	@Override
	protected JPanel getColumn3Panel() {

		this.matureDealsPanel = new SantCheckBoxPanel("Mature Deals");
		this.mtmZeroPanel = new SantCheckBoxPanel("MTM Zero");

		final JPanel column3Panel = new JPanel();
		column3Panel.removeAll();

		GridLayout gl = new GridLayout(6, 4);
		column3Panel.setLayout(gl);
		gl.setHgap(9);
		gl.setVgap(9);

		column3Panel.add(this.mtmZeroPanel);
		mtmZeroPanel.setValue(true);

		column3Panel.add(this.matureDealsPanel);
		matureDealsPanel.setValue(true);

		this.maturityOffSet = new SantComboBoxPanel<String, String>("Maturity Off Set	", getmaturityOffSet());
		column3Panel.add(this.maturityOffSet);
		this.maturityOffSet.setEditable(false);
		this.maturityOffSet.setBounds(10, 50, 280, 24);
		this.maturityOffSet.setPreferredSize(new Dimension(240, 24));

		return column3Panel;
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();

	}

	// Hides all panels
	@Override
	public void hideAllPanels() {
		super.hideAllPanels();
	}

	/** 
	 * Recovers the values of the panel into the template
	 * 
	 */
	@Override
	public ReportTemplate getTemplate() {

		ReportTemplate template = super.getTemplate();

		this.processDatePanel.read(this.reportTemplate);
		template.put(BOSecurityPositionReportTemplate.MOVE, this.movementType.getValue());
		template.put(BOSecurityPositionReportTemplate.CUSTOM_FILTER, filterChoice.getValue());
		template.put(BOSecurityPositionReportTemplate.SEC_FILTER, this.securitiesFilterChoice.getValue());
		template.put(BOSecurityPositionReportTemplate.POSITION_VALUE, positionValueChoice.getValue());
		template.put(BOSecurityPositionReportTemplate.POSITION_TYPE, inventoryTypes.getValue());
		template.put(BOSecurityPositionReportTemplate.POSITION_DATE, inventoryDate.getValue());
		template.put(BOSecurityPositionReportTemplate.CASH_SECURITY, cashSecurityChoice.getValue());
		template.put(BOSecurityPositionReportTemplate.FILTER_ZERO, mtmZeroPanel.getValue() ? true : false);
		template.put(BOSecurityPositionReportTemplate.FILTER_MATURED, matureDealsPanel.getValue() ? true : false);
		final String ownersNames = (String) template.put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS,
				poAgrPanel.getTextField());
		if (!Util.isEmpty(ownersNames)) {
			template.put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS, ownersNames);
		}
		template.put(SACCRBalancesReportTemplate.MATURITY_OFFSET, this.maturityOffSet.getValue());
		template.put(SantGenericTradeReportTemplate.AGREEMENT_TYPE, this.agreementTypePanel.getValue());

		return template;

	}

	/**
	 * Stores panel filter selections into template
	 */
	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);
		this.movementType.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.MOVE);
		this.filterChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.CUSTOM_FILTER);
		this.securitiesFilterChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.SEC_FILTER);
		this.positionValueChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.POSITION_VALUE);
		this.inventoryTypes.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.POSITION_TYPE);
		this.inventoryDate.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.POSITION_DATE);
		this.cashSecurityChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.CASH_SECURITY);
		this.maturityOffSet.setValue(this.reportTemplate, SACCRBalancesReportTemplate.MATURITY_OFFSET);
		this.agreementTypePanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.AGREEMENT_TYPE);
		this.mtmZeroPanel.getValue();
		this.matureDealsPanel.getValue();
	}

	
	void filterZeroCheck_actionPerformed(ActionEvent event) {
		if (this.reportTemplate == null)
			return;
		this.reportTemplate.put("INV_FILTER_ZERO", (this.filterZeroCheck.isSelected()) ? "true" : "false");
	}

	void filterMaturedCheck_actionPerformed(ActionEvent event) {
		if (this.reportTemplate == null)
			return;
		this.reportTemplate.put("INV_FILTER_MATURED", (this.filterMaturedCheck.isSelected()) ? "true" : "false");
	}

	/*
	 * Sets positions types
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<String> getpositionValueType() {
		Vector v = new Vector();
		v.addElement("Quantity");
		v.addElement("Nominal");
		v.addElement("Nominal (Unfactored)");
		return v;
	}

	/*
	 * Sets type position to be included 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<String> getcashSecurity() {
		Vector v = new Vector();
		v.addElement("Both");
		v.addElement(BOPositionReport.CASH);
		v.addElement(BOPositionReport.SECURITY);
		return v;
	}

	/*
	 *  List maturity dates. Defaults ones plus optional configurations through DV 
	 */
	private Collection<String> getmaturityOffSet() {

		Vector<String> v = new Vector<String>(Arrays.asList(new String[] { "1", "7", "15", "30" }));
		String domainName = SACCRBalancesReport.class.getSimpleName() + "."
				+ (SACCRBalancesReport.CONFIGURATIONS.MATURITY_RANGE.getName());
		Map<String, String> decimalMap = CollateralUtilities.initDomainValueComments(domainName);
		if (decimalMap.containsKey(domainName)) {
			v.addAll(Util.string2Vector(decimalMap.get(domainName)));
		}

		return v;
	}

	/*
	 *  List of filter for BOPosition
	 */
	private Collection<String> getInventoryFilters() {

		Vector<String> v = LocalCache.getDomainValues(DSConnection.getDefault(), "BOPositionFilter");
		v.add(0, "");
		return v;
	}

	/*
	 * Empty String Collection
	 */
	private Collection<String> getEmpty() {
		ArrayList<String> Empity = new ArrayList<String>();
		Empity.add("");
		return Empity;
	}

	/*
	 * Movement types
	 */
	private Collection<String> getMovementTypes() {
		ArrayList<String> movements = new ArrayList<String>();
		movements.add(BALANCE);
		movements.add(MOVEMENTS);
		return movements;
	}

	/*
	 * Inventory Types
	 */
	private Collection<String> getInventoryTypes() {
		ArrayList<String> InventoryType = new ArrayList<String>();
		InventoryType.add(ACTUAL);
		InventoryType.add(NOT_SETTLED);
		InventoryType.add(THEORETICAL);
		return InventoryType;
	}

	/*
	 * Inventory date selection
	 */
	private Collection<String> getInventoryDate() {
		ArrayList<String> InventoryType = new ArrayList<String>();
		InventoryType.add(BOOKING);
		InventoryType.add(SETTLE);
		InventoryType.add(TRADE);
		InventoryType.add(VALUE);
		return InventoryType;
	}

	// The panels to be shown by default
	private void setPanelVisibility() {
		hideAllPanels();
		this.positionValueChoice.setVisible(true);
		this.cashSecurityChoice.setVisible(true);
		this.filterChoice.setVisible(true);
		this.securitiesFilterChoice.setVisible(true);
		this.mtmZeroPanel.setVisible(true);
		this.matureDealsPanel.setVisible(true);
		this.processDatePanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.movementType.setVisible(true);
	}
	
	/*
	 *  Load in the background Static Data Filter AFTER loading the template
	 */
	public class Worker extends SwingWorker<Collection<String>, Void> {

		protected void done() {
			try {
				SACCRBalancesReportTemplatePanel.this.securitiesFilterChoice.getChoice()
						.setTheWholeItems(get().toArray());
			} catch (InterruptedException | ExecutionException e) {
				Log.error(this, e); //sonar
			}
		}

		@SuppressWarnings("unchecked")
		protected Collection<String> doInBackground() throws Exception {
			Vector<String> v = AccessUtil.getAllNames(User.STATIC_DATA_FILTER);
			v.add(1, "");
			ArrayList<String> filters = new ArrayList<String>(v);
			return filters;
		}
	}

	/*
	 * public static void main(final String... pepe) throws ConnectException {
	 * 
	 * final String args[] = { "-env", "dev3", "-user", "calypso_user",
	 * "-password", "calypso" }; ConnectionUtil.connect(args,
	 * "SACCRBalancesReportTemplatePanel"); final JFrame frame = new JFrame();
	 * frame.setTitle("SACCRBalancesReportTemplatePanel");
	 * frame.setContentPane(new SACCRBalancesReportTemplatePanel());
	 * frame.setVisible(true); frame.setSize(new Dimension(1273, 307));
	 * frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); }
	 */

}