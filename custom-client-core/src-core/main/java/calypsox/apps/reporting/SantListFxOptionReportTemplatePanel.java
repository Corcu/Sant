/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.apps.reporting.util.control.SantLegalEntityPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.LocalCache;

public class SantListFxOptionReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = 1L;

	public static final String COUNTERPARTY = "Counterparty";
	public static final String PRINCIPAL_CCY = "Principal Currency";
	// GSM 22/07/15. SBNA Multi-PO filter
	public static final String OWNER_AGR = SantGenericTradeReportTemplate.PROCESSING_ORG_IDS;
	// "Owner agreement";

	protected ReportTemplate reportTemplate;

	protected SantProcessDatePanel processDate;

	// GSM 21/07/15. SBNA Multi-PO filter
	protected SantLegalEntityPanel poAgrPanel;

	protected SantLegalEntityPanel cptyPanel;

	protected SantComboBoxPanel<Integer, String> ccyPanel;

	public SantListFxOptionReportTemplatePanel() {
		init();
	}

	private void init() {

		this.processDate = new SantProcessDatePanel("Process");

		this.cptyPanel = new SantLegalEntityPanel(LegalEntity.COUNTERPARTY, "CounterParty", false, true, true, true);

		this.ccyPanel = new SantComboBoxPanel<Integer, String>("Currency", LocalCache.getCurrencies());

		// GSM 21/07/15. SBNA Multi-PO filter
		this.poAgrPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, "Owner (Agr)", false, true, true, true);

		final JPanel masterPanel = new JPanel();
		masterPanel.setLayout(new BorderLayout());
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("List FX Spot");
		titledBorder.setTitleColor(Color.BLUE);
		masterPanel.setBorder(titledBorder);

		final JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new GridLayout(1, 2));
		// GSM 20/07/15. SBNA Multi-PO filter
		centerPanel.add(this.poAgrPanel);
		centerPanel.add(this.cptyPanel);
		centerPanel.add(this.ccyPanel);

		masterPanel.add(this.processDate, BorderLayout.NORTH);
		masterPanel.add(centerPanel, BorderLayout.CENTER);

		add(masterPanel);
		setSize(0, 150);

	}

	@Override
	public ReportTemplate getTemplate() {

		this.processDate.read(this.reportTemplate);

		this.reportTemplate.put(COUNTERPARTY, this.cptyPanel.getLEIdsStr());

		this.reportTemplate.put(PRINCIPAL_CCY, this.ccyPanel.getValue());
		// GSM 20/07/15. SBNA Multi-PO filter
		if (!Util.isEmpty(this.poAgrPanel.getLE())) {
			this.reportTemplate.put(OWNER_AGR, this.poAgrPanel.getLEIdsStr());

		} else {
			this.reportTemplate.remove(OWNER_AGR);

		}

		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		this.reportTemplate = template;

		this.processDate.setTemplate(template);
		this.processDate.write(template);

		this.cptyPanel.setValue(this.reportTemplate, COUNTERPARTY);

		this.ccyPanel.setValue(this.reportTemplate, PRINCIPAL_CCY);
		// GSM 20/07/15. SBNA Multi-PO filter
		this.poAgrPanel.setValue(this.reportTemplate, OWNER_AGR);

	}

	// public static void main(final String... argsss) throws ConnectException {
	// final String args[] = { "-env", "dev4-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
	// ConnectionUtil.connect(args, "SantListFxOptionReportTemplatePanel");
	// final JFrame frame = new JFrame();
	// frame.setTitle("SantListFxOptionReportTemplatePanel");
	// frame.setContentPane(new SantListFxOptionReportTemplatePanel());
	// frame.setVisible(true);
	// frame.setSize(new Dimension(1273, 307));
	// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// }
}
