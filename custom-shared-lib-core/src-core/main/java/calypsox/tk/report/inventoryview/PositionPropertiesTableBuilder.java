/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import calypsox.tk.report.SantInventoryViewReportTemplate;
import calypsox.tk.report.SantThirdPartyInventoryViewReportTemplate;
import calypsox.tk.report.inventoryview.property.SantFactoryProperty;
import calypsox.tk.report.thirdpartyinventoryview.property.Value;
import calypsox.tk.report.thirdpartyinventoryview.property.ValueHelper;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.BOCashPositionReportTemplate;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOPositionReportTemplate;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.ui.component.table.propertytable.PropertyTableUtilities;
import com.calypso.ui.factory.PropertyFactory;
import com.calypso.ui.property.EnumProperty;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyTable;
import com.jidesoft.grid.PropertyTableModel;

public class PositionPropertiesTableBuilder {

	public static final String BALANCE = "Balance";
	public static final String AGG_ALL = "Book/Agent/Account";
	public static final String AGG_AGENT_ACCOUNT = "Agent/Account";
	public static final String AGG_AGENT = "Agent";
	public static final String AGG_GLOBAL = "Global";
	public static final String AGG_BOOK = "Book";
	public static final String AGG_BOOK_AGENT = "Book/Agent";

	private PropertyTable positionTable = null;

	private EnumProperty<String> positionDateProp;
	private EnumProperty<String> positionClassProp;
	private Property positionTypeProp;
	private EnumProperty<String> positionValueProp;
	private EnumProperty<String> positionAggregationProp;
	private Property positionMovementTypeProp;
	private EnumProperty<String> positionCustomFilterProp;
	private EnumProperty<String> excludeZeroPositionsProp;
	private Property currencyProp;
	private Property valueProp;

	public PositionPropertiesTableBuilder() {
		initProps();
		display();
	}

	private void initProps() {
		// MIGRATION V14.4 18/01/2015
		this.positionDateProp = PropertyFactory.makeEnumProperty(
				"Position Date", "Position Date", null, getMapFromList(getPositionDates()));
		this.positionClassProp = PropertyFactory.makeEnumProperty(
				"Position Class", "Position Class", null, getMapFromList(getPositionClasses()));
		this.positionTypeProp = makePositiontTypeProperty();
		this.positionValueProp = PropertyFactory.makeEnumProperty(
				"Position Value", "Position Value", null, getMapFromList(getPositionValues()));
		this.positionAggregationProp = PropertyFactory.makeEnumProperty(
				"Aggregation", "Aggregation", null, getMapFromList(getAggregations()));
		this.positionMovementTypeProp = makePositionMovementType();
		this.positionCustomFilterProp = PropertyFactory.makeEnumProperty(
				"CustomFilter", "CustomFilter", null,
				getMapFromList(getPositionCustomFilters()));
		this.excludeZeroPositionsProp = PropertyFactory.makeEnumProperty(
				"Filter Zero Balance", "Filter Zero Balance", null,
				getMapFromList(getexcludeZeroPositionsVector()));

		this.currencyProp = makeCurrencyProperty();
		this.valueProp = makeValueProperty("Position Check");
	}

	private Map<String, String> getMapFromList(Vector<String> list) {
		Map<String, String> values = new HashMap<String, String>();
		for (String elt : list) {
			values.put(elt, elt);
		}
		return values;
	}
	
	private Property makeValueProperty(String name) {
		ValueHelper helper = new ValueHelper(name);
		return helper.getValueProperty();
	}

	private Property makeCurrencyProperty() {
		Vector<String> currencies = LocalCache.getCurrencies();
		Collections.sort(currencies);

		return SantFactoryProperty.makeChooserListPorperty("Currencies",
				"Currencies", null, null, currencies);

	}

	private Vector<String> getPositionCustomFilters() {
		Vector<String> v = LocalCache.getDomainValues(
				DSConnection.getDefault(), "BOPositionFilter");
		v.add(0, "");
		return v;
	}

	private Vector<String> getexcludeZeroPositionsVector() {
		Vector<String> v = new Vector<String>();
		v.add(0, "false");
		v.add(0, "true");
		return v;
	}

	@SuppressWarnings("deprecation")
	private Property makePositionMovementType() {
		// Vector<String> v = new Vector<String>();
		// v.add(0, InventorySecurityPosition.BALANCE);
		// v.add(1, InventorySecurityPosition.MOVEMENTS);
		// v.add(2, InventorySecurityPosition.BALANCE_PLEDGEDOUT);

		return SantFactoryProperty.makeChooserListPorperty("Movement",
				"Movement", null, null,
				InventorySecurityPosition.getMovementTypes());

	}

	private Property makePositiontTypeProperty() {
		return SantFactoryProperty.makeChooserListPorperty("Position Type",
				"Position Type", null, null, getPositionTypes());
	}

	private Vector<String> getPositionTypes() {
		Vector<String> v = new Vector<String>();
		v.add(0, BOPositionReport.THEORETICAL);
		v.add(1, BOPositionReport.ACTUAL);
		return v;
	}

	private Vector<String> getPositionClasses() {
		Vector<String> v = new Vector<String>();
		v.add(0, BOPositionReport.INTERNAL);
		v.add(1, BOPositionReport.MARGIN_CALL);
		return v;
	}

	private Vector<String> getPositionDates() {
		Vector<String> v = new Vector<String>();
		v.add(0, BOPositionReport.TRADE);
		v.add(1, BOPositionReport.SETTLE);
		return v;
	}

	private Vector<String> getPositionValues() {
		Vector<String> v = new Vector<String>();
		v.add(0, BOPositionReport.QUANTITY);
		v.add(1, BOPositionReport.NOMINAL);
		v.add(2, BOPositionReport.NOMINAL_UNFACTORED);
		return v;
	}

	private Vector<String> getAggregations() {
		Vector<String> v = new Vector<String>();
		v.add(0, AGG_ALL);
		v.add(1, AGG_AGENT_ACCOUNT);
		v.add(2, AGG_AGENT);
		v.add(3, AGG_GLOBAL);
		v.add(4, AGG_BOOK);
		v.add(5, AGG_BOOK_AGENT);
		Collections.sort(v);
		return v;
	}

	public JComponent getComponent() {
		ArrayList<Property> properties = new ArrayList<Property>();

		properties.add(this.positionDateProp);
		properties.add(this.positionClassProp);
		properties.add(this.positionTypeProp);
		properties.add(this.positionValueProp);
		properties.add(this.positionAggregationProp);
		properties.add(this.positionMovementTypeProp);
		properties.add(this.positionCustomFilterProp);
		properties.add(this.excludeZeroPositionsProp);
		properties.add(this.currencyProp);
		properties.add(this.valueProp);

		PropertyTableModel<Property> positionTableModel = new PropertyTableModel<Property>(
				properties) {

			private static final long serialVersionUID = -8168681714595333701L;

			@Override
			public String getColumnName(int i) {
				if (i == 0) {
					return "<html><b>Position Properties</b></html>";
				}
				return "";
			}
		};

		positionTableModel.setOrder(PropertyTableModel.UNSORTED);
		this.positionTable = new PropertyTable(positionTableModel);
		this.positionTable.expandAll();
		PropertyTableUtilities.setupPropertyTableKeyActions(this.positionTable);

		JScrollPane panel = new JScrollPane();
		panel.getViewport().add(this.positionTable);
		panel.getViewport().setBackground(this.positionTable.getBackground());
		panel.setBorder(BorderFactory.createLineBorder(Color.black));
		this.positionTable.getTableHeader().setBackground(Color.black);
		this.positionTable.getTableHeader().setForeground(Color.white);

		return panel;
	}

	public JComponent getComponent(Dimension dim) {
		JComponent comp = getComponent();
		comp.setMinimumSize(dim);
		comp.setPreferredSize(dim);
		comp.setMaximumSize(dim);
		return comp;
	}

	private void display() {
		this.positionDateProp.setValue(BOPositionReport.SETTLE);
		this.positionClassProp.setValue(BOPositionReport.INTERNAL);
		this.positionTypeProp.setValue(Util
				.string2Vector(BOPositionReport.THEORETICAL));
		this.positionValueProp.setValue(BOPositionReport.QUANTITY);
		this.positionAggregationProp.setValue(AGG_ALL);
		this.positionMovementTypeProp.setValue(Util
				.string2Vector(InventorySecurityPosition.BALANCE_DEFAULT));
		this.positionCustomFilterProp.setValue(null);
		this.excludeZeroPositionsProp.setValue("false");
	}

	@SuppressWarnings("unchecked")
	public ReportTemplate getTemplate(ReportTemplate template) {

		Value valueObject = (Value) this.valueProp.getValue();

		// following is done to avoid losing information if editing is not
		// stopped for table by and keypress like tab,
		// enter etc.
		if ((this.positionTable != null) && this.positionTable.isEditing()) {
			this.positionTable.getCellEditor().stopCellEditing();
			try {
				int i = 0;
				while (this.positionTable.isEditing() && (i < 5)) {
					// wait until the editing thread has a chance
					// to stop the cell editing.
					wait(++i * 10);
				}
			} catch (Exception e2) {
				Log.debug(Log.GUI, "exception:" + e2);
			}
		}

		template.remove(BOSecurityPositionReportTemplate.POSITION_DATE);
		template.remove(BOSecurityPositionReportTemplate.POSITION_CLASS);
		template.remove(BOSecurityPositionReportTemplate.POSITION_TYPE);
		template.remove(BOSecurityPositionReportTemplate.POSITION_VALUE);
		template.remove(BOCashPositionReportTemplate.AGGREGATION_TYPE);
		template.remove(BOSecurityPositionReportTemplate.MOVE);
		template.remove(BOSecurityPositionReportTemplate.CUSTOM_FILTER);
		template.remove(SantInventoryViewReportTemplate.CURRENCIES);

		// This one is hard-coded as we don't add the control on the screen to
		// exclude zero balance pos
		template.remove(BOSecurityPositionReportTemplate.FILTER_ZERO);
		template.put(BOSecurityPositionReportTemplate.FILTER_ZERO, "true");

		String value = null;
		Vector<String> valueList = null;

		value = this.positionDateProp.getValue();
		if (!Util.isEmpty(value)) {
			template.put(BOSecurityPositionReportTemplate.POSITION_DATE, value);
		}

		value = this.positionClassProp.getValue();
		if (!Util.isEmpty(value)) {
			template.put(BOSecurityPositionReportTemplate.POSITION_CLASS, value);
		}

		valueList = (Vector<String>) this.positionTypeProp.getValue();
		if (!Util.isEmpty(valueList)) {
			template.put(BOSecurityPositionReportTemplate.POSITION_TYPE,
					Util.collectionToString(valueList));
		}

		value = this.positionValueProp.getValue();
		if (!Util.isEmpty(value)) {
			template.put(BOSecurityPositionReportTemplate.POSITION_VALUE, value);
		}

		value = this.positionAggregationProp.getValue();
		if (!Util.isEmpty(value)) {
			template.put(BOSecurityPositionReportTemplate.AGGREGATION, value);
		}

		valueList = (Vector<String>) this.positionMovementTypeProp.getValue();
		if (!Util.isEmpty(valueList)) {
			template.put(BOSecurityPositionReportTemplate.MOVE,
					Util.collectionToString(valueList));
		}

		value = this.positionCustomFilterProp.getValue();
		if (!Util.isEmpty(value)) {
			template.put(BOSecurityPositionReportTemplate.CUSTOM_FILTER, value);
		}

		value = this.excludeZeroPositionsProp.getValue();
		if (!Util.isEmpty(value)) {
			template.put(BOSecurityPositionReportTemplate.FILTER_ZERO, value);
		}

		valueList = (Vector<String>) this.currencyProp.getValue();
		if (!Util.isEmpty(valueList)) {
			template.put(SantInventoryViewReportTemplate.CURRENCIES,
					Util.collectionToString(valueList));
		}

		template.remove(SantThirdPartyInventoryViewReportTemplate.VALUE);
		template.put(SantThirdPartyInventoryViewReportTemplate.VALUE,
				valueObject);

		if (valueObject != null) {
			JDate valueDate = JDate.getNow();
			valueDate = valueDate.addBusinessDays(valueObject.getDays(),
					LocalCache.getCurrentHoliday().getHolidayCodeList());

			template.remove(BOPositionReportTemplate.START_DATE);
			template.remove(BOPositionReportTemplate.END_DATE);

			template.put(BOPositionReportTemplate.START_DATE,
					Util.dateToString(JDate.getNow()));
			template.put(BOPositionReportTemplate.END_DATE,
					Util.dateToString(valueDate));
		}

		return template;
	}

	public void setTemplate(ReportTemplate template) {
		String value = null;
		value = (String) template
				.get(BOSecurityPositionReportTemplate.POSITION_DATE);
		if (!Util.isEmpty(value)) {
			this.positionDateProp.setValue(value);
		} else {
			this.positionDateProp.setValue(null);
		}
		value = (String) template
				.get(BOSecurityPositionReportTemplate.POSITION_CLASS);
		if (!Util.isEmpty(value)) {
			this.positionClassProp.setValue(value);
		} else {
			this.positionClassProp.setValue(null);
		}
		value = (String) template
				.get(BOSecurityPositionReportTemplate.POSITION_TYPE);
		if (!Util.isEmpty(value)) {
			this.positionTypeProp.setValue(Util.string2Vector(value));
		} else {
			this.positionTypeProp.setValue(null);
		}
		value = (String) template
				.get(BOSecurityPositionReportTemplate.POSITION_VALUE);
		if (!Util.isEmpty(value)) {
			this.positionValueProp.setValue(value);
		} else {
			this.positionValueProp.setValue(null);
		}
		value = (String) template
				.get(BOSecurityPositionReportTemplate.AGGREGATION);
		if (!Util.isEmpty(value)) {
			this.positionAggregationProp.setValue(value);
		} else {
			this.positionAggregationProp.setValue(null);
		}
		value = (String) template.get(BOSecurityPositionReportTemplate.MOVE);
		if (!Util.isEmpty(value)) {
			this.positionMovementTypeProp.setValue(Util.string2Vector(value));
		} else {
			this.positionMovementTypeProp.setValue(null);
		}
		value = (String) template
				.get(BOSecurityPositionReportTemplate.CUSTOM_FILTER);
		if (!Util.isEmpty(value)) {
			this.positionCustomFilterProp.setValue(value);
		} else {
			this.positionCustomFilterProp.setValue(null);
		}

		value = (String) template
				.get(BOSecurityPositionReportTemplate.FILTER_ZERO);
		if (!Util.isEmpty(value)) {
			this.excludeZeroPositionsProp.setValue(value);
		} else {
			this.excludeZeroPositionsProp.setValue("false");
		}

		value = (String) template
				.get(SantInventoryViewReportTemplate.CURRENCIES);
		if (!Util.isEmpty(value)) {
			this.currencyProp.setValue(Util.string2Vector(value));
		} else {
			this.currencyProp.setValue(null);
		}

	}
}
