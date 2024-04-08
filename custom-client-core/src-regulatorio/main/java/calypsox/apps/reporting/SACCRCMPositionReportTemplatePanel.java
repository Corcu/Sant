
package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import com.calypso.tk.core.JDatetime;
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
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.SACCRCMPositionReport;
import calypsox.tk.report.SACCRCMPositionReportTemplate;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;

/**
 *
 * SA CCR Balances Panel (new report for IRIS balances files). Provides same
 * template attributes for BOPositions reports plus some custom panels.
 * 
 * NOTE: due to time loading SD Filters in Panel startup, it is loaded in second
 * thread.
 * 
 * @author Damian Mascarella
 * @version 1.2
 * @Date 04/01/2017
 */

public class SACCRCMPositionReportTemplatePanel extends SantGenericReportTemplatePanel {

	/**
	 * Generated Serial UID
	 */
	private static final long serialVersionUID = -8844966712832138984L;

	/*
	 * Inventory postion type constants
	 */
	public static final String ACTUAL = "ACTUAL";
	public static final String THEORETICAL = "THEORETICAL";

	/**
	 * Variables
	 */
	private SantComboBoxPanel<String, String> cashSecurityChoice;
	private SantProcessDatePanel processDatePanel;
	private SantComboBoxPanel<String, String> inventoryTypes;
	private SantComboBoxPanel<String, String> maturityOffSet;
	private SantChooseButtonPanel agreementTypePanel;
	private SantComboBoxPanel<String, String> sdfFilterChoice;
	private SantComboBoxPanel<String, String> filterGroupChoice;

	/**
	 * Default constructor
	 */
	public SACCRCMPositionReportTemplatePanel() {
		setPanelVisibility();
	}

	@Override
	protected Dimension getPanelSize() {
		return new Dimension(0, 120);
	}

	/**
	 * Process date of the process
	 */
	@Override
	protected Component getNorthPanel() {
		this.processDatePanel = new SantProcessDatePanel("");
		this.processDatePanel.setPreferredSize(new Dimension(120, 30), new Dimension(240, 30));
		this.processDatePanel.setPanelLabelName("Process Date");
		return this.processDatePanel;
	}

	/*
	 * Report title
	 * 
	 * @see calypsox.apps.reporting.util.SantGenericReportTemplatePanel#
	 * getMasterPanelBorder()
	 */
	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("SA-CCR Collateral Manager Balance");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	// First column, they contain: MoventType, Inventory filter, SD filter,
	// inventory type, inventory date.
	@Override
	protected JPanel getColumn1Panel() {

		/*
		 * Load SDF in background
		 */
		Worker worker = new Worker();
		worker.execute();

		final JPanel column1Panel = new JPanel();
		column1Panel.removeAll();

		GridLayout gl = new GridLayout(6, 3);
		column1Panel.setLayout(gl);
		gl.setHgap(9);
		gl.setVgap(9);

		this.cashSecurityChoice = new SantComboBoxPanel<String, String>("Cash/Sec", getcashSecurity());
		this.cashSecurityChoice.setEditable(false);
		column1Panel.add(this.cashSecurityChoice);
		this.cashSecurityChoice.setBounds(10, 50, 280, 24);
		this.cashSecurityChoice.setPreferredSize(new Dimension(240, 24));

		this.inventoryTypes = new SantComboBoxPanel<String, String>("Inventory Type", getPositionTypes());
		this.inventoryTypes.setBounds(10, 50, 280, 24);
		this.inventoryTypes.setPreferredSize(new Dimension(240, 24));
		this.inventoryTypes.setEditable(false);
		column1Panel.add(this.inventoryTypes);

		this.sdfFilterChoice = new SantComboBoxPanel<String, String>("SDF Filter", getEmpty());
		this.sdfFilterChoice.setBounds(10, 50, 280, 24);
		this.sdfFilterChoice.setPreferredSize(new Dimension(240, 24));
		this.sdfFilterChoice.setEditable(false);
		column1Panel.add(this.sdfFilterChoice);

		this.filterGroupChoice = new SantComboBoxPanel<String, String>("Contracts Filter", getContractsGropusFilters());
		this.filterGroupChoice.setBounds(10, 50, 280, 24);
		this.filterGroupChoice.setPreferredSize(new Dimension(240, 24));
		this.filterGroupChoice.setEditable(false);
		column1Panel.add(this.filterGroupChoice);

		return column1Panel;
	}

	/**
	 * Second column, contains: agreementType, agreementName, POs, position &
	 * Cash/Security.
	 * 
	 * @see calypsox.apps.reporting.util.SantGenericReportTemplatePanel#getColumn2Panel()
	 */
	@Override
	protected JPanel getColumn2Panel() {

		BOSecurityPositionReportTemplatePanel panel = new BOSecurityPositionReportTemplatePanel();
		this.agreementTypePanel = new SantChooseButtonPanel("Agr Type", "legalAgreementType");

		final JPanel column2Panel = new JPanel();
		column2Panel.removeAll();

		GridLayout gl = new GridLayout(6, 3);
		column2Panel.setLayout(gl);
		gl.setHgap(9);
		gl.setVgap(9);

		column2Panel.add(panel.add(poAgrPanel));
		this.poAgrPanel.setPreferredSize(new Dimension(240, 24));
		this.poAgrPanel.setBounds(10, 50, 280, 24);

		column2Panel.add(panel.add(cptyPanel));
		this.cptyPanel.setPreferredSize(new Dimension(240, 24));
		this.cptyPanel.setBounds(10, 50, 280, 24);

		column2Panel.add(panel.add(agreementNamePanel));
		this.agreementNamePanel.setPreferredSize(new Dimension(240, 24));
		this.agreementNamePanel.setBounds(10, 50, 280, 24);

		column2Panel.add(panel.add(agreementTypePanel));
		this.agreementTypePanel.setPreferredSize(new Dimension(240, 24));
		this.agreementTypePanel.setBounds(10, 50, 280, 24);

		this.maturityOffSet = new SantComboBoxPanel<String, String>("Maturity Off Set	", getmaturityOffSet());
		this.maturityOffSet.setEditable(false);
		this.maturityOffSet.setBounds(10, 50, 280, 24);
		this.maturityOffSet.setPreferredSize(new Dimension(240, 24));
		column2Panel.add(this.maturityOffSet);

		return column2Panel;
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
	
	
	
	@Override
	public void setValDatetime(JDatetime valDatetime) {
		this.processDatePanel.setValDatetime(valDatetime);
	}

	/**
	 * Recovers the values of the panel into the template
	 * 
	 */
	@Override
	public ReportTemplate getTemplate() {

		ReportTemplate template = super.getTemplate();

		this.processDatePanel.read(this.reportTemplate);

		template.put(BOSecurityPositionReportTemplate.POSITION_TYPE, inventoryTypes.getValue());
		template.put(BOSecurityPositionReportTemplate.CASH_SECURITY, cashSecurityChoice.getValue());
		template.put(BOSecurityPositionReportTemplate.FILTER_ZERO, mtmZeroPanel.getValue() ? true : false);
		template.put(BOSecurityPositionReportTemplate.FILTER_MATURED, matureDealsPanel.getValue() ? true : false);
		template.put(BOSecurityPositionReportTemplate.SEC_FILTER, this.sdfFilterChoice.getValue());
		template.put(BOSecurityPositionReportTemplate.CUSTOM_FILTER, filterGroupChoice.getValue());

		final String ownersNames = (String) template.put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS,
				poAgrPanel.getTextField());
		if (!Util.isEmpty(ownersNames)) {
			template.put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS, ownersNames);
		}

		template.put(SACCRCMPositionReportTemplate.MATURITY_OFFSET, this.maturityOffSet.getValue());
		template.put(SantGenericTradeReportTemplate.AGREEMENT_TYPE, this.agreementTypePanel.getValue());

		return template;

	}

	/**
	 * Stores panel filter selections into template
	 */
	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);

		this.inventoryTypes.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.POSITION_TYPE);
		this.cashSecurityChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.CASH_SECURITY);
		this.maturityOffSet.setValue(this.reportTemplate, SACCRCMPositionReportTemplate.MATURITY_OFFSET);
		this.agreementTypePanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.AGREEMENT_TYPE);
		this.sdfFilterChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.SEC_FILTER);
		this.filterGroupChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.CUSTOM_FILTER);
	
		this.processDatePanel.setTemplate(template);
		this.processDatePanel.write(template);
	
	}

	/*
	 * Sets type position to be included
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Collection<String> getcashSecurity() {
		Vector v = new Vector();
		v.addElement("Both");
		v.addElement(BOPositionReport.CASH);
		v.addElement(BOPositionReport.SECURITY);
		return v;
	}

	/*
	 * List maturity dates. Defaults ones plus optional configurations through
	 * DV
	 */
	private Collection<String> getmaturityOffSet() {

		Vector<String> v = new Vector<String>(Arrays.asList(new String[] { "1", "7", "15", "30" }));
		String domainName = SACCRCMPositionReport.class.getSimpleName() + "."
				+ (SACCRCMPositionReport.CONFIGURATIONS.MATURITY_RANGE.getName());
		Map<String, String> decimalMap = CollateralUtilities.initDomainValueComments(domainName);
		if (decimalMap.containsKey(domainName)) {
			v.addAll(Util.string2Vector(decimalMap.get(domainName)));
		}

		return v;
	}

	/*
	 * Inventory Types
	 */
	private Collection<String> getPositionTypes() {
		ArrayList<String> InventoryType = new ArrayList<String>();
		InventoryType.add(0, "");
		InventoryType.add(ACTUAL);
		InventoryType.add(THEORETICAL);
		return InventoryType;
	}

	/*
	 * List of filter groups from DV
	 */
	private Collection<String> getContractsGropusFilters() {

		Vector<String> v = LocalCache.getDomainValues(DSConnection.getDefault(), "Collateral.Config.Group");
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

	// The panels to be shown by default
	private void setPanelVisibility() {
		hideAllPanels();
		this.cashSecurityChoice.setVisible(true);
		this.sdfFilterChoice.setVisible(true);
		// this.mtmZeroPanel.setVisible(true);
		// this.matureDealsPanel.setVisible(true);
		this.processDatePanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.cptyPanel.setVisible(true);
		this.filterGroupChoice.setVisible(true);
	}

	@Override
	protected void init() {
		buildControlsPanel();

		setSize(getPanelSize());

		final JPanel masterPanel = new JPanel();
		masterPanel.setLayout(new BorderLayout());
		masterPanel.setBorder(getMasterPanelBorder());
		add(masterPanel);

		masterPanel.add(getNorthPanel(), BorderLayout.NORTH);

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(1, 3));

		masterPanel.add(mainPanel, BorderLayout.CENTER);

		final JPanel column1Panel = getColumn1Panel();
		final JPanel column2Panel = getColumn2Panel();

		mainPanel.add(column1Panel);
		mainPanel.add(column2Panel);
	}

	/*
	 * Load in the background Static Data Filter AFTER loading the template
	 */
	public class Worker extends SwingWorker<Collection<String>, Void> {

		protected void done() {
			try {
				SACCRCMPositionReportTemplatePanel.this.sdfFilterChoice.getChoice().setTheWholeItems(get().toArray());
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

	// public static void main(final String... pepe) throws ConnectException {
	//
	// final String args[] = { "-env", "dev3", "-user", "calypso_user",
	// "-password", "calypso" }; ConnectionUtil.connect(args,
	// "SACCRCMBalancesReportTemplatePanel"); final JFrame frame = new JFrame();
	// frame.setTitle("SACCRCMBalancesReportTemplatePanel");
	// frame.setContentPane(new SACCRCMBalancesReportTemplatePanel());
	// frame.setVisible(true); frame.setSize(new Dimension(1273, 307));
	// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); }

}