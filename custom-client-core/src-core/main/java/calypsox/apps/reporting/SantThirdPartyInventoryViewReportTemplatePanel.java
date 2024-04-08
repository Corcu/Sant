/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.report.ReportTemplate;

import calypsox.tk.report.inventoryview.CreditRatingPropertiesTableBuilder;
import calypsox.tk.report.inventoryview.HaircutPropertiesTableBuilder;
import calypsox.tk.report.inventoryview.LegalPropertiesTableBuilder;
import calypsox.tk.report.thirdpartyinventoryview.ProductPropertiesTableBuilder;
import calypsox.tk.report.thirdpartyinventoryview.ThirdPartyPositionPropertiesTableBuilder;

public class SantThirdPartyInventoryViewReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = -2216340651361984998L;

	protected ReportTemplate template = null;

	private final ThirdPartyPositionPropertiesTableBuilder positionBuilder = new ThirdPartyPositionPropertiesTableBuilder();
	private final HaircutPropertiesTableBuilder haircutBuilder = new HaircutPropertiesTableBuilder();
	private final LegalPropertiesTableBuilder legalBuilder = new LegalPropertiesTableBuilder();
	// private final DatePropertiesTableBuilder dateBuilder = new DatePropertiesTableBuilder();
	private final CreditRatingPropertiesTableBuilder crBuilder = new CreditRatingPropertiesTableBuilder();
	private final ProductPropertiesTableBuilder productBuilder = new ProductPropertiesTableBuilder();

	public SantThirdPartyInventoryViewReportTemplatePanel() {
		init();
	}

	private void init() {

		// JComponent dateComponent = this.dateBuilder.getComponent(new Dimension(250, 55));
		JComponent legalComponent = this.legalBuilder.getComponent(new Dimension(250, 100));

		JComponent positionComponent = this.positionBuilder.getComponent(new Dimension(250, 100));
		JComponent haircutComponent = this.haircutBuilder.getComponent(new Dimension(250, 92));
		JComponent crComponent = this.crBuilder.getComponent(new Dimension(350, 100));

		JComponent productComponent = this.productBuilder.getComponent(new Dimension(250, 55));

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.VERTICAL;

		// constraints.gridx = 0;
		// constraints.gridy = 0;
		// constraints.gridheight = 1;
		// constraints.gridwidth = 2;
		// mainPanel.add(dateComponent, constraints);

		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 3;
		mainPanel.add(legalComponent, constraints);

		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridheight = 3;
		mainPanel.add(positionComponent, constraints);

		constraints.gridx = 2;
		constraints.gridy = 0;
		constraints.gridheight = 3;
		// constraints.gridwidth = 3;
		mainPanel.add(crComponent, constraints);

		constraints.gridx = 3;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		// constraints.gridwidth = 2;
		mainPanel.add(haircutComponent, constraints);

		constraints.gridx = 3;
		constraints.gridy = 1;
		constraints.gridheight = 1;
		mainPanel.add(productComponent, constraints);

		JPanel masterPanel = new JPanel();
		masterPanel.setLayout(new FlowLayout());
		masterPanel.add(mainPanel, constraints);

		setLayout(new BorderLayout());
		add(masterPanel, BorderLayout.CENTER);

		this.setSize(new Dimension(900, 180));
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		if (template == null) {
			return;
		}
		this.template = template;

		this.positionBuilder.setTemplate(template);
		this.haircutBuilder.setTemplate(template);
		this.legalBuilder.setTemplate(template);
		// this.dateBuilder.setTemplate(template);
		this.crBuilder.setTemplate(template);
		this.productBuilder.setTemplate(template);

	}

	@Override
	public ReportTemplate getTemplate() {

		this.positionBuilder.getTemplate(this.template);
		this.haircutBuilder.getTemplate(this.template);
		this.legalBuilder.getTemplate(this.template);
		// this.dateBuilder.getTemplate(this.template);
		this.crBuilder.getTemplate(this.template);
		this.productBuilder.getTemplate(this.template);

		return this.template;
	}

	@Override
	public void callBeforeLoad(ReportPanel panel) {
		panel.getTemplate().callBeforeLoad();

	}
}
