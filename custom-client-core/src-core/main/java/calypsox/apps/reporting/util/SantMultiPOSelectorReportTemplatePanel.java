/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.FontChooser;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

import calypsox.apps.reporting.util.control.SantLegalEntityPanel;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;

public class SantMultiPOSelectorReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = 1L;

	protected SantLegalEntityPanel poAgrPanel;
	// protected SantChooseButtonPanel poAgrPanel;

	protected ReportTemplate reportTemplate;

	public SantMultiPOSelectorReportTemplatePanel() {
		super();
		init();
	}

	@Override
	public ReportTemplate getTemplate() {

		if (!Util.isEmpty(this.poAgrPanel.getLE())) {
			this.reportTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS, this.poAgrPanel.getLEIdsStr());
			this.reportTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES, this.poAgrPanel.getLE());
		} else {
			this.reportTemplate.remove(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
			this.reportTemplate.remove(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);
		}
		// if (!Util.isEmpty(this.poAgrPanel.getValue())) {
		// this.reportTemplate.put(BOSecurityPositionReportTemplate.PROCESSING_ORG,
		// this.poAgrPanel.getValue());
		// }
		return this.reportTemplate;
	}

	@Override
	public void setTemplate(ReportTemplate template) {

		if (template == null) {
			return;
		}

		this.reportTemplate = template;

		this.poAgrPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);

		// this.poAgrPanel.setValue(this.reportTemplate,
		// SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);

	}

	protected void init() {
		buildControlsPanel();

		setSize(0, 150);
		final JPanel masterPanel = new JPanel();
		masterPanel.setLayout(new BorderLayout());
		masterPanel.setBorder(getMasterPanelBorder());
		add(masterPanel, BorderLayout.EAST);

		masterPanel.add(this.poAgrPanel);
	}

	protected Border getMasterPanelBorder() {
		// GSM 01/03/16 v14 - Not working title font in java 6, modified to java
		// 7
		TitledBorder border = new TitledBorder(BorderFactory.createEtchedBorder(), "");
		border.setTitleFont(FontChooser.F_B);
		border.setTitleColor(Color.BLACK);
		return border;
		// final TitledBorder titledBorder =
		// BorderFactory.createTitledBorder("");
		// titledBorder.setTitleFont(new
		// Font(titledBorder.getTitleFont().getName(), Font.BOLD, titledBorder
		// .getTitleFont().getSize()));
		// titledBorder.setTitleColor(Color.BLACK);
		// return titledBorder;
	}

	protected void buildControlsPanel() {
		this.poAgrPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, "Owner (Agr)", false, true, true, true);
		// final SortedSet<String> sortedLegalEntiti es = new
		// TreeSet<String>(BOCache.getLegalEntitieNamesForRole( 
		// DSConnection.getDefault(), LegalEntity.PROCESSINGORG));
		// this.poAgrPanel = new SantChooseButtonPanel("Owner Agr",
		// sortedLegalEntities);
	}

	protected Object getMultipleKey(final String value, final Map<Integer, String> map) {
		final Vector<String> agreementNames = Util.string2Vector(value);
		final Vector<Integer> agreementIds = new Vector<Integer>();
		for (final String agreementName : agreementNames) {
			agreementIds.add((Integer) getKey(agreementName, map));
		}
		return Util.collectionToString(agreementIds);
	}

	private Object getKey(final String value, final Map<Integer, String> map) {
		for (final Entry<Integer, String> entry : map.entrySet()) {
			if (entry.getValue() == null) {
				return null;
			}
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}

}
