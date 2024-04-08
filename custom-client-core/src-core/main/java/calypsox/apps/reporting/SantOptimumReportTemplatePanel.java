/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import calypsox.apps.reporting.util.loader.MarginCallConfigLightLoader;
import calypsox.tk.report.SantOptimumReportTemplate;
import calypsox.tk.report.inventoryview.property.SantFactoryProperty;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.MarginCallEntryDTOReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.ui.component.table.propertytable.PropertyTableUtilities;
import com.calypso.ui.factory.PropertyFactory;
import com.calypso.ui.property.DateProperty;
import com.calypso.ui.property.EnumProperty;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyTable;
import com.jidesoft.grid.PropertyTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

/**
 * OptimusReport Templatepanel. Reuses MarginCallEntryDTOReportTemplate for allocations, affected in set/get template
 * method.
 * 
 * @author Guillermo Solano
 * 
 */
public class SantOptimumReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = 1L;

	private static final String PROCESS_DATE = "Process Date";
	private static final String CONTRACTS_IDS = "Contracts";
	private static final String OPTIMIZATION = "Optimization";
	private static final String CONTRACT_TYPE = "Contract Type";

	private ReportTemplate template;
	private Map<Integer, String> contractMap;

	// panel properties
	private PropertyTable table;
	private DateProperty processDateProp;
	private Property contractProp;
	private Property optimizationConfigProp;
	private Property contractType;
	private EnumProperty<String> reassignCategories;

	private EnumProperty<String> useCache;

	/**
	 * Constructor
	 */
	public SantOptimumReportTemplatePanel() {
		initProps();
		initComponent();
		display();
	}

	// initially properties
	@SuppressWarnings("deprecation")
	private void initProps() {
		this.processDateProp = PropertyFactory.makeDateProperty(PROCESS_DATE, PROCESS_DATE, null, null);
		this.contractProp = makeContractProperty();
		this.optimizationConfigProp = makeOptiomizationConfigProperty();
		this.contractType = makeContractTypeProperty();
		this.reassignCategories = new EnumProperty<String>("Re-Assign Categories", "Re-Assign Categories",
				getReassignCategories());
		this.useCache = new EnumProperty<String>("Use Cache", "Use Cache", getUseCache());

	}

	private Property makeContractTypeProperty() {
		Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(), "legalAgreementType");
		// domainValues.insertElementAt("", 0);
		return SantFactoryProperty.makeChooserListPorperty(CONTRACT_TYPE, CONTRACT_TYPE, null, null, domainValues);
	}

	private Vector<String> getReassignCategories() {
		Vector<String> v = new Vector<String>();
		v.add(0, "false");
		v.add(0, "true");
		return v;
	}

	private Vector<String> getUseCache() {
		return getReassignCategories();
	}

	// contract property
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Property makeContractProperty() {
		this.contractMap = new MarginCallConfigLightLoader().load();
		List sortedList = new ArrayList(this.contractMap.values());
		Collections.sort(sortedList);
		return SantFactoryProperty.makeChooserListPorperty(CONTRACTS_IDS, CONTRACTS_IDS, null, null,
				new Vector<String>(sortedList));
	}

	// optimization property
	private Property makeOptiomizationConfigProperty() {
		List<String> result = new ArrayList<String>();
		result.add("");
		try {
			List<String> loaded = ServiceRegistry.getDefault().getCollateralDataServer()
					.loadAllOptimizationConfigurationNames();
			if (!Util.isEmpty(loaded)) {
				result.addAll(loaded);
			}
		} catch (Exception e) {
			Log.error(this, e);
		}

		return SantFactoryProperty.makeChooserListPorperty(OPTIMIZATION, OPTIMIZATION, null, null, new Vector<String>(
				result));
	}

	/**
	 * initially Optimusreport panel conmponents
	 */
	public void initComponent() {
		ArrayList<Property> properties = new ArrayList<Property>();

		properties.add(this.processDateProp);
		properties.add(this.contractProp);
		properties.add(this.optimizationConfigProp);
		properties.add(this.contractType);
		properties.add(this.reassignCategories);
		properties.add(this.useCache);

		PropertyTableModel<Property> tableModel = new PropertyTableModel<Property>(properties) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getColumnName(int i) {
				if (i == 0) {
					return "<html><b>Properties</b></html>";
				}
				return "";
			}

		};

		tableModel.setOrder(PropertyTableModel.UNSORTED);
		this.table = new PropertyTable(tableModel);

		this.table.expandAll();
		PropertyTableUtilities.setupPropertyTableKeyActions(this.table);

		JScrollPane panel = new JScrollPane();
		panel.getViewport().add(this.table);
		panel.getViewport().setBackground(this.table.getBackground());
		panel.setBorder(BorderFactory.createLineBorder(Color.black));
		this.table.getTableHeader().setBackground(Color.black);
		this.table.getTableHeader().setForeground(Color.white);

		setLayout(new FlowLayout());
		add(panel);

		panel.setPreferredSize(new Dimension(400, 120));
		panel.setMaximumSize(new Dimension(400, 120));
		panel.setMinimumSize(new Dimension(400, 120));
		this.setSize(new Dimension(500, 150));
	}

	private void display() {
		this.processDateProp.setValue(JDate.getNow());
		this.reassignCategories.setValue("false");
		this.useCache.setValue("true");
	}

	/**
	 * getTemplate override. Retrieves Process date, MCContracts Ids (from MarginCallEntryDTOReportTemplate) and
	 * optimization configuration (from SantOptimumReportTemplate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ReportTemplate getTemplate() {

		// following is done to avoid losing information if editing is not stopped for table by and keypress like tab,
		// enter etc.
		waitEditingThreadFinishEdition();

		// remove PD in case
		this.template.remove(MarginCallEntryDTOReportTemplate.PROCESS_START_DATE);
		this.template.remove(MarginCallEntryDTOReportTemplate.PROCESS_END_DATE);

		// get and put the Process date
		JDate date = this.processDateProp.getValueAsJDate();
		this.template.put(MarginCallEntryDTOReportTemplate.PROCESS_START_DATE, date != null ? date.toString() : null);
		this.template.put(MarginCallEntryDTOReportTemplate.PROCESS_END_DATE, date != null ? date.toString() : null);

		// remove and put contract IDs
		this.template.remove(MarginCallEntryDTOReportTemplate.MARGIN_CALL_CONFIG_IDS);
		this.template.put(MarginCallEntryDTOReportTemplate.MARGIN_CALL_CONFIG_IDS,
				getContractIdsAsString((Vector<String>) this.contractProp.getValue()));

		// put configuration selection
		this.template.remove(SantOptimumReportTemplate.OPTIMIZATION_CONFIGURATION);
		this.template.put(SantOptimumReportTemplate.OPTIMIZATION_CONFIGURATION, this.optimizationConfigProp.getValue());

		this.template.remove(SantOptimumReportTemplate.CONTRACT_TYPE);
		this.template.put(SantOptimumReportTemplate.CONTRACT_TYPE, this.contractType.getValue());

		if (!Util.isEmpty(this.reassignCategories.getValue())) {
			this.template.put(SantOptimumReportTemplate.REASSIGN_CATEGORIES, this.reassignCategories.getValue());
		}

		if (!Util.isEmpty(this.useCache.getValue())) {
			this.template.put(SantOptimumReportTemplate.USE_CACHE, this.useCache.getValue());
		}

		return this.template;
	}

	/*
	 * following is done to avoid losing information if editing is not stopped for table by and keypress like tab, enter
	 * etc.
	 */
	private void waitEditingThreadFinishEdition() {
		if ((this.table != null) && this.table.isEditing()) {
			this.table.getCellEditor().stopCellEditing();
			try {
				int i = 0;
				while (this.table.isEditing() && (i < 5)) {
					// wait until the editing thread has a chance
					// to stop the cell editing.
					wait(++i * 10);
				}
			} catch (Exception e2) {
				Log.debug(Log.GUI, "exception:" + e2);
			}
		}
	}

	/**
	 * setTemplate override. Sets Process date, MCContracts Ids ( MarginCallEntryDTOReportTemplate) and optimization
	 * configuration (SantOptimumReportTemplate)
	 * 
	 * @param template
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void setTemplate(ReportTemplate template) {

		this.template = template;
		String value = null;
		String contractIds = null;
		Vector optimization = null;
		Vector contractTypeVect = null;

		value = (String) template.get(MarginCallEntryDTOReportTemplate.PROCESS_START_DATE);
		if (!Util.isEmpty(value)) {
			this.processDateProp.setValue(JDate.valueOf(value));
		} else {
			this.processDateProp.setValue(JDate.getNow());
		}

		contractIds = (String) template.get(MarginCallEntryDTOReportTemplate.MARGIN_CALL_CONFIG_IDS);
		if (!Util.isEmpty(contractIds)) {
			this.contractProp.setValue(getContractNames(contractIds));
		} else {
			this.contractProp.setValue(null);
		}

		optimization = (Vector) template.get(SantOptimumReportTemplate.OPTIMIZATION_CONFIGURATION);
		if (!Util.isEmpty(optimization)) {
			this.optimizationConfigProp.setValue(optimization);
		} else {
			this.optimizationConfigProp.setValue(null);
		}

		contractTypeVect = (Vector) template.get(SantOptimumReportTemplate.CONTRACT_TYPE);
		if (!Util.isEmpty(contractTypeVect)) {
			this.contractType.setValue(contractTypeVect);
		} else {
			this.contractType.setValue(null);
		}

		if (!Util.isEmpty((String) template.get(SantOptimumReportTemplate.REASSIGN_CATEGORIES))) {
			this.reassignCategories.setValue(template.get(SantOptimumReportTemplate.REASSIGN_CATEGORIES));
		}

		if (!Util.isEmpty((String) template.get(SantOptimumReportTemplate.USE_CACHE))) {
			this.useCache.setValue(template.get(SantOptimumReportTemplate.USE_CACHE));
		}

	}

	/**
	 * @param contractIdsAsString
	 * @return vector of contracts Names as strings
	 */
	private Vector<String> getContractNames(String contractIdsAsString) {
		Vector<Integer> contractIds = Util.string2IntVector(contractIdsAsString);
		Vector<String> contractNames = new Vector<String>();
		for (Integer contractId : contractIds) {
			contractNames.add(this.contractMap.get(contractId));
		}
		return contractNames;
	}

	/**
	 * @param contractNames
	 *            Collection of contracts Names
	 * @return String
	 */
	private String getContractIdsAsString(List<String> contractNames) {
		if (Util.isEmpty(contractNames)) {
			return null;
		}
		Vector<Integer> contractIds = new Vector<Integer>();
		for (Entry<Integer, String> entry : this.contractMap.entrySet()) {
			if (contractNames.contains(entry.getValue())) {
				contractIds.add(entry.getKey());
			}
		}
		return Util.collectionToString(contractIds);

	}
}
