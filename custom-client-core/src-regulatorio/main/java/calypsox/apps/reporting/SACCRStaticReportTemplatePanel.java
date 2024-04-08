package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.tk.report.SACCRStaticReport;
import calypsox.tk.report.SACCRStaticReport.REPORT_TYPES;
import calypsox.tk.report.SACCRStaticReportTemplate;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

/**
 *
 * SA CCR Static Panel - Panel for 3 CA CCR Report for Collateral Config Data information for POs, CTPYs and products.
 * Changes default columns based on enum REPORT_TYPES type (this panel is used for 3 reports in fact).
 * 
 * @author Damian Mascarella
 * @version 1.1
 * @Date 03/01/2017
 */

public class SACCRStaticReportTemplatePanel extends SantGenericReportTemplatePanel {
	
	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -1946300955579710851L;
	
	/**
	 * Specific agreement panel - multiple selections
	 */
	private SantChooseButtonPanel agreementTypePanel;
	
	/**
	 * Specific agreement type combo - multiple selections
	 */
	private SantComboBoxPanel<String, String> types;

	/**
	 * Default constructor
	 */
	public SACCRStaticReportTemplatePanel() {
		setPanelVisibility();
	}

	protected Dimension getPanelSize() {
		setSize(30, 150);
		return new Dimension(30, 80);
	}


	protected void init() {

		buildControlsPanel();
		setSize(getPanelSize());
		final JPanel masterPanel = new JPanel();
		masterPanel.setLayout(new BorderLayout());
		masterPanel.setBorder(getMasterPanelBorder());
		add(masterPanel);

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(1, 1));

		masterPanel.add(mainPanel, BorderLayout.CENTER);

		final JPanel column2Panel = getColumn2Panel();

		mainPanel.add(column2Panel);

	}

	/*
	 * Panel Name (non-Javadoc)
	 * @see calypsox.apps.reporting.util.SantGenericReportTemplatePanel#getMasterPanelBorder()
	 */
	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("SA-CCR Static Collateral");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	/*
	 * Column 2 (non-Javadoc)
	 * @see calypsox.apps.reporting.util.SantGenericReportTemplatePanel#getColumn2Panel()
	 */
	@Override
	protected JPanel getColumn2Panel() {

		this.agreementTypePanel = new SantChooseButtonPanel("Agr Type", "legalAgreementType");

		final JPanel column2Panel = new JPanel();
		column2Panel.removeAll();
		GridLayout gl = new GridLayout(3, 2);
		column2Panel.setSize(0, 80);
		column2Panel.setLayout(gl);
		gl.setHgap(10);
		gl.setVgap(7);

		column2Panel.add(this.poAgrPanel);
		column2Panel.add(this.cptyPanel);
		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);

		this.types = new SantComboBoxPanel<String, String>("REPORT Type", getTypes());
		this.types.setBounds(10, 50, 280, 24);
		this.types.setPreferredSize(new Dimension(240, 24));
		this.types.setEditable(false);
		column2Panel.add(this.types);
		SymItem lSymItem = new SymItem();
		this.types.getChoice().addItemListener(lSymItem);

		return column2Panel;
	}

	/*
	 * REmove super class panels (non-Javadoc)
	 * @see calypsox.apps.reporting.util.SantGenericReportTemplatePanel#hideAllPanels()
	 */
	public void hideAllPanels() {
		super.hideAllPanels();
	}

	// The panel fields are placed in the template
	@Override
	public ReportTemplate getTemplate() {
		ReportTemplate template = super.getTemplate();
		if (!Util.isEmpty(this.poAgrPanel.getLE())) {
			template.put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS, this.poAgrPanel.getLEIdsStr());
			template.put(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES, this.poAgrPanel.getLE());
		} else {
			template.remove(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
			template.remove(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);
		}
		if (!Util.isEmpty(this.cptyPanel.getLE())) {
			template.put(SantGenericTradeReportTemplate.COUNTERPARTY, this.cptyPanel.getLEIdsStr());
		} else {
			template.remove(SantGenericTradeReportTemplate.COUNTERPARTY);
		}
		String value = this.agreementNamePanel.getValue();
		template.put(SantGenericTradeReportTemplate.AGREEMENT_ID, getMultipleKey(value, this.marginCallContractIdsMap));

		template.put(SantGenericTradeReportTemplate.AGREEMENT_TYPE, this.agreementTypePanel.getValue());
		if (!Util.isEmpty(this.cptyPanel.getLE())) {
			template.put(SantGenericTradeReportTemplate.COUNTERPARTY, this.cptyPanel.getLEIdsStr());
		} else {
			template.remove(SantGenericTradeReportTemplate.COUNTERPARTY);
		}
		template.put(SantGenericTradeReportTemplate.AGREEMENT_TYPE, this.agreementTypePanel.getValue());
		template.put(SACCRStaticReportTemplate.REPORT_TYPE, this.types.getValue());

		return this.reportTemplate;
	}

	/**
	 * Definition of the template
	 */
	@Override
	public void setTemplate(final ReportTemplate template) {
		
		super.setTemplate(template);
		this.poAgrPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
		this.cptyPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.COUNTERPARTY);
		this.agreementNamePanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.AGREEMENT_ID,
				this.marginCallContractIdsMap);
		this.agreementTypePanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.AGREEMENT_TYPE);
		this.types.setValue(this.reportTemplate, SACCRStaticReportTemplate.REPORT_TYPE);

	}

	/**
	 * Loads template before loading report
	 */
	@Override
	public void callBeforeLoad(ReportPanel panel) {
		
		ReportTemplate template = panel.getTemplate();
		template.callBeforeLoad();
	}

	// Fields are displayed on the panel
	private void setPanelVisibility() {
		hideAllPanels();
		this.poAgrPanel.setVisible(true);
		this.cptyPanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
		this.types.setVisible(true);
	}

	// For report types
	private Collection<String> getTypes() {
		List<String> list = SACCRStaticReport.REPORT_TYPES.getReportNamesAsList();
		Vector<String> v = new Vector<String>(list);
		ArrayList<String> filters = new ArrayList<String>(v);
		return filters;
	}

	// Event for report Types
	/**
	 * Listener to change default columns based on the type of report
	 *
	 */
	class SymItem implements ItemListener {
		
		public void itemStateChanged(ItemEvent event) {
			Object object = event.getSource();
			if (object == SACCRStaticReportTemplatePanel.this.types.getChoice())
				SACCRStaticReportTemplatePanel.this.reportTypes_itemStateChanged(event);
		}
	}

	void reportTypes_itemStateChanged(ItemEvent event) {
		String showCashSecurity = (String) this.types.getValue();
		
		if (showCashSecurity == null) {
			return;
		}
		if ((showCashSecurity.equals(REPORT_TYPES.COL_BRANCH.getReportName()) || showCashSecurity.equals(REPORT_TYPES.COL_CPTY.getReportName()))) {
			
			setTemplateColumnsColLe();
		} else if ((showCashSecurity.equals(REPORT_TYPES.COL_INSTRUMENT.getReportName()))) {
			setTemplateColumnsColProduct();
		}
	}

	private void setTemplateColumnsColProduct() {
		ReportTemplate template = super.getTemplate();
		String[] columns = SACCRStaticReportTemplate.DEFAULT_COLUMNS_COL_PRODUCT;
		template.setColumns(columns);
	}

	private void setTemplateColumnsColLe() {
		ReportTemplate template = super.getTemplate();
		String[] columns = SACCRStaticReportTemplate.DEFAULT_COLUMNS_COL_LE;
		template.setColumns(columns);
	}

	/*
	 * Panel Testing
	 */
/*	public static void main(final String... pepe) throws ConnectException {

		final String args[] = { "-env", "dev3", "-user", "calypso_user", "-password", "calypso" };
		ConnectionUtil.connect(args, "SACCRStaticReportTemplatePanel");
		final JFrame frame = new JFrame();
		frame.setTitle("SACCRStaticReportTemplatePanel");
		frame.setContentPane(new SACCRStaticReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1273, 307));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	} */

}
