/*
 *
 * Copyright (c) 2011 Kaupthing Bank
 * Borgart?n 19, IS-105 Reykjavik, Iceland
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import calypsox.apps.reporting.util.ValueComparator;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.tk.report.OptimSecurityFiltersReportTemplate;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.OptimizationConfiguration;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

public class OptimSecurityFiltersReportTemplatePanel extends ReportTemplatePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static String LOG_CATEGORY = "OptimSecurityFiltersReportTemplatePanel";

	protected ReportTemplate reportTemplate;

	protected SantChooseButtonPanel agreementNamePanel;
	protected Map<Integer, String> optimConfigIdsMap;

	@SuppressWarnings("rawtypes")
	protected Vector selectedProds;
	@SuppressWarnings("rawtypes")
	protected Vector selectedTradeStatuses;

	public OptimSecurityFiltersReportTemplatePanel() {

		this.optimConfigIdsMap = loadOptimConfigs();
		init();
		this.reportTemplate = null;
	}

	private Map<Integer, String> loadOptimConfigs() {
		Map<Integer, String> optimConfigMap = new HashMap<Integer, String>();
		try {
			List<OptimizationConfiguration> configList = ServiceRegistry.getDefault(DSConnection.getDefault())
					.getCollateralDataServer().loadAllOptimizationConfiguration();
			for (OptimizationConfiguration config : configList) {
				optimConfigMap.put(config.getId(), config.getName());
			}
		} catch (final RemoteException e) {
			Log.error(this, "Cannot load Optimization configurations", e);
		}

		return optimConfigMap;
	}

	private void init() {

		setLayout(null);
		setSize(new Dimension(1173, 307));

		final ValueComparator bvc = new ValueComparator(this.optimConfigIdsMap);
		final Map<Integer, String> sortedMap = new TreeMap<Integer, String>(bvc);
		sortedMap.putAll(this.optimConfigIdsMap);
		this.agreementNamePanel = new SantChooseButtonPanel("Optim Config", sortedMap.values(), 900);
		this.agreementNamePanel.setBounds(100, 100, 350, 34);
		add(this.agreementNamePanel);

	}

	@Override
	public ReportTemplate getTemplate() {

		// Set contract id in the template
		final String value = this.agreementNamePanel.getValue();
		if (!Util.isEmpty(value)) {
			this.reportTemplate.put(OptimSecurityFiltersReportTemplate.OPTIM_CONFIG_IDS,
					getMultipleKey(value, this.optimConfigIdsMap));
		} else {
			this.reportTemplate.remove(OptimSecurityFiltersReportTemplate.OPTIM_CONFIG_IDS);
		}

		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		this.reportTemplate = template;

		this.agreementNamePanel.setValue(this.reportTemplate, OptimSecurityFiltersReportTemplate.OPTIM_CONFIG_IDS,
				this.optimConfigIdsMap);

	}

	protected Object getMultipleKey(final String value, final Map<Integer, String> map) {
		final Vector<String> agreementNames = Util.string2Vector(value);
		final Vector<Integer> agreementIds = new Vector<Integer>();
		for (String agreementName : agreementNames) {
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
				final int key = entry.getKey();
				return key;
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean isValidLoad(ReportPanel panel) {
		Map potentialSizesByTypeOfObject = panel.getReport().getPotentialSize();
		return displayLargeListWarningMessage(this, potentialSizesByTypeOfObject);
	}

}
