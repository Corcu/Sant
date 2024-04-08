/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.report.ReportTemplate;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.ValueComparator;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.SantIntragroupPortfolioBreakdownReportTemplate;
import calypsox.tk.report.loader.IntragroupLegalEntityLoader;

public class SantIntragroupPortfolioBreakdownReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 123L;
	public static final String INTRAGROUP_ATTRIBUTE_NAME = "INTRAGROUP"; // intragroup le attribute
	public static final String INTRAGROUP_ATTRIBUTE_YES_VALUE = "YES";

	protected SantProcessDatePanel processDatePanel;
	protected SantChooseButtonPanel intragroupLePanel;
	protected Map<Integer, String> intragroupLeIdsMap;

	@Override
	public void setValDatetime(JDatetime valDatetime) {
		this.processDatePanel.setValDatetime(valDatetime);
	}

	@Override
	protected void loadStaticData() {
		super.loadStaticData();

		// get intragroup le map id-code from db
		this.intragroupLeIdsMap = new IntragroupLegalEntityLoader().load();
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
		this.processDatePanel = new SantProcessDatePanel("Value Date");
		this.processDatePanel.setPreferredSize(new Dimension(70, 24), new Dimension(215, 24));
		this.processDatePanel.removeDateLabel();

		// create intragoup le selector
		ValueComparator bvc = new ValueComparator(this.intragroupLeIdsMap);
		Map<Integer, String> sortedMap = new TreeMap<Integer, String>(bvc);
		sortedMap.putAll(this.intragroupLeIdsMap);
		this.intragroupLePanel = new SantChooseButtonPanel("Intra Cpty", sortedMap.values());
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Intragroup Portfolio Breakdown");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	public ReportTemplate getTemplate() {
		ReportTemplate template = super.getTemplate();
		this.processDatePanel.read(this.reportTemplate);

		// intragroup le value/s
		String value = this.intragroupLePanel.getValue();
		this.reportTemplate.put(SantIntragroupPortfolioBreakdownReportTemplate.INTRAGROUP_LE_ID,
				getMultipleKey(value, this.intragroupLeIdsMap));

		return template;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		super.setTemplate(template);
		this.processDatePanel.setTemplate(template);
		this.processDatePanel.write(template);

		// intragroup le value/s
		this.intragroupLePanel.setValue(this.reportTemplate,
				SantIntragroupPortfolioBreakdownReportTemplate.INTRAGROUP_LE_ID, this.intragroupLeIdsMap);
	}

	@Override
	protected JPanel getNorthPanel() {
		return this.processDatePanel;
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(6, 1));
		column1Panel.add(new JLabel());
		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.poDealPanel);

		// add intragroup le selector
		column1Panel.add(this.intragroupLePanel);

		column1Panel.add(this.tradeStatusPanel);
		column1Panel.add(this.instrumentTypePanel);

		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(6, 1));
		column2Panel.add(new JLabel());
		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);
		column2Panel.add(this.valuationPanel);
		// column2Panel.add(this.valuationPanel);

		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {

		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(6, 1));
		column3Panel.add(new JLabel());
		this.checkBoxPanel = new JPanel();
		final Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(this.matureDealsPanel);
		this.matureDealsPanel.setVisible(false);
		box.add(Box.createRigidArea(new Dimension(5, 0)));
		box.add(this.mtmZeroPanel);
		this.checkBoxPanel.add(box);
		column3Panel.add(this.checkBoxPanel);
		this.checkBoxPanel.setPreferredSize(this.economicSectorPanel.getPreferredSize());
		return column3Panel;
	}

}
