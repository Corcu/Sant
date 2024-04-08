package calypsox.apps.reporting;

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

public class SantTradesPotentialMtmErrorReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 1L;
	public static final String TEMPLATE_PROPERTY_PRODUCT_TYPE = "PRODUCT_TYPE";
	public static final String PRODUCT_TYPE_ALL = "ALL";

	protected SantComboBoxPanel<Integer, String> productTypePanel;

	private SantProcessDatePanel processDatePanel;

	public SantTradesPotentialMtmErrorReportTemplatePanel() {
		setPanelVisibility();
	}

	@Override
	protected Component getNorthPanel() {
		this.processDatePanel = new SantProcessDatePanel("Process Date");
		this.processDatePanel.setPreferredSize(new Dimension(80, 24), new Dimension(215, 24));
		this.processDatePanel.removeDateLabel();

		return this.processDatePanel;
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Trades With Potential MTM Errors");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1panel = new JPanel();
		column1panel.removeAll();

		column1panel.setLayout(new GridLayout(2, 1));
		column1panel.add(this.poAgrPanel);
		column1panel.add(this.cptyPanel);
		return column1panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2panel = new JPanel();
		column2panel.removeAll();
		column2panel.setLayout(new GridLayout(2, 1));

		Vector<String> productTypeValues = new Vector<String>();
		productTypeValues.add("ALL");
		productTypeValues.add(Product.REPO);
		productTypeValues.add(Product.SEC_LENDING);
		productTypeValues.add(CollateralExposure.PRODUCT_TYPE);

		this.productTypePanel = new SantComboBoxPanel<Integer, String>("Product Type", productTypeValues);
		this.productTypePanel.setValue("ALL");

		column2panel.add(this.productTypePanel);
		return column2panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3panel = new JPanel();
		column3panel.removeAll();
		column3panel.setLayout(new GridLayout(2, 1));

		column3panel.add(this.agreementNamePanel);
		column3panel.add(this.agreementTypePanel);

		return column3panel;
	}

	@Override
	public ReportTemplate getTemplate() {
		super.getTemplate();
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

	private void setPanelVisibility() {
		hideAllPanels();
		this.processStartEndDatePanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.cptyPanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
	}

}