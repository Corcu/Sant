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
import calypsox.tk.report.SantHaircutByIssuerReportTemplate;
import calypsox.tk.report.loader.IssuerLoader;

public class SantHaircutByIssuerReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 123L;

	protected SantProcessDatePanel processDatePanel;
	protected SantChooseButtonPanel issuerPanel;
	protected Map<Integer, String> issuerIdsMap;

	@Override
	public void setValDatetime(JDatetime valDatetime) {

		this.processDatePanel.setValDatetime(valDatetime);

	}

	@Override
	protected void loadStaticData() {

		super.loadStaticData();
		this.issuerIdsMap = new IssuerLoader().load();

	}

	@Override
	protected void buildControlsPanel() {

		super.buildControlsPanel();
		ValueComparator bvc = null;
		Map<Integer, String> sortedMap = null;

		// value date
		this.processDatePanel = new SantProcessDatePanel("Value Date");
		this.processDatePanel.setPreferredSize(new Dimension(70, 24), new Dimension(215, 24));
		this.processDatePanel.removeDateLabel();

		// issuer selector
		bvc = new ValueComparator(this.issuerIdsMap);
		sortedMap = new TreeMap<Integer, String>(bvc);
		sortedMap.putAll(this.issuerIdsMap);
		this.issuerPanel = new SantChooseButtonPanel("Issuer", sortedMap.values());

	}

	@Override
	protected Border getMasterPanelBorder() {

		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Haircut By Issuer");
		titledBorder.setTitleColor(Color.BLUE);

		return titledBorder;

	}

	@Override
	public ReportTemplate getTemplate() {

		ReportTemplate template = super.getTemplate();
		this.processDatePanel.read(this.reportTemplate);
		// issuer value/s
		String value = this.issuerPanel.getValue();
		this.reportTemplate.put(SantHaircutByIssuerReportTemplate.ISSUER_ID, value);

		return template;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		super.setTemplate(template);
		this.processDatePanel.setTemplate(template);
		this.processDatePanel.write(template);
		// issuer le value/s
		this.issuerPanel.setValue(this.reportTemplate, SantHaircutByIssuerReportTemplate.ISSUER_ID);
	}

	@Override
	protected JPanel getNorthPanel() {

		return this.processDatePanel;

	}

	@Override
	protected JPanel getColumn1Panel() {

		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(3, 1));
		column1Panel.add(new JLabel());
		// add agreement selector
		column1Panel.add(this.poAgrPanel);

		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {

		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(3, 1));
		column2Panel.add(new JLabel());
		// add agreement selector
		column2Panel.add(this.agreementNamePanel);

		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {

		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(3, 1));
		column3Panel.add(new JLabel());
		// add issuer selector
		column3Panel.add(this.issuerPanel);

		return column3Panel;

	}

}
