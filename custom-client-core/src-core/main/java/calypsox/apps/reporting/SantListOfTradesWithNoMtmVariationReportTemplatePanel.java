/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;

import com.calypso.tk.core.Product;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.report.ReportTemplate;

public class SantListOfTradesWithNoMtmVariationReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 5867668276588396582L;

	public static final String TEMPLATE_PROPERTY_PRODUCT_TYPE = "PRODUCT_TYPE";

	protected SantComboBoxPanel<Integer, String> productTypePanel;

	private SantProcessDatePanel processDatePanel;

	@Override
	protected void init() {
		buildControlsPanel();

		setSize(0, 115);
		final JPanel masterPanel = new JPanel();
		masterPanel.setLayout(new BorderLayout());
		masterPanel.setBorder(getMasterPanelBorder());
		add(masterPanel);

		masterPanel.add(getNorthPanel(), BorderLayout.NORTH);

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(1, 2));

		masterPanel.add(mainPanel, BorderLayout.CENTER);

		final JPanel column1Panel = getColumn1Panel();
		final JPanel column2Panel = getColumn2Panel();
		// GSM 24/08/15. SBNA Multi-PO filter.
		final JPanel column3Panel = getColumn3Panel();

		mainPanel.add(column1Panel);
		mainPanel.add(column2Panel);
		mainPanel.add(column3Panel);

	}

	@Override
	protected Component getNorthPanel() {
		this.processDatePanel = new SantProcessDatePanel("Value Date");
		this.processDatePanel.setPreferredSize(new Dimension(80, 24), new Dimension(215, 24));
		this.processDatePanel.removeDateLabel();

		return this.processDatePanel;
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Trades No MTM Variation");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	// GSM 24/08/15. SBNA Multi-PO filter.
	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(2, 1));

		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.poDealPanel);

		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(2, 1));

		Vector<String> productTypeValues = new Vector<String>();
		productTypeValues.add("");
		productTypeValues.add(Product.REPO);
		productTypeValues.add(Product.SEC_LENDING);
		productTypeValues.add(CollateralExposure.PRODUCT_TYPE);

		this.productTypePanel = new SantComboBoxPanel<Integer, String>("Product Type", productTypeValues);
		this.productTypePanel.setValue("");

		// column1Panel.add(this.poDealPanel);
		column2Panel.add(this.productTypePanel);

		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(2, 1));

		column3Panel.add(this.agreementNamePanel);
		column3Panel.add(this.agreementTypePanel);
		return column3Panel;
	}

	@Override
	public ReportTemplate getTemplate() {
		this.reportTemplate = super.getTemplate();
		this.processDatePanel.read(this.reportTemplate);
		this.reportTemplate.put(TEMPLATE_PROPERTY_PRODUCT_TYPE, this.productTypePanel.getValue());

		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);
		this.processDatePanel.setTemplate(template);
		this.processDatePanel.write(template);
		this.productTypePanel.setValue(this.reportTemplate, TEMPLATE_PROPERTY_PRODUCT_TYPE);
	}

	// test
	// public static void main(final String... argsss) throws ConnectException {
	// final String args[] = { "-env", "dev5-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
	// ConnectionUtil.connect(args, "SantListOfTradesWithNoMtmVariationReportTemplatePanel");
	// final JFrame frame = new JFrame();
	// frame.setTitle("SantListOfTradesWithNoMtmVariationReportTemplatePanel");
	// frame.setContentPane(new SantListOfTradesWithNoMtmVariationReportTemplatePanel());
	// frame.setVisible(true);
	// frame.setSize(new Dimension(1273, 307));
	// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// }

}
