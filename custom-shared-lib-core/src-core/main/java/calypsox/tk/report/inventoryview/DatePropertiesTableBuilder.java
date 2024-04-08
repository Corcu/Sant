/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Tenor;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.ui.component.table.propertytable.PropertyTableUtilities;
import com.calypso.ui.factory.PropertyFactory;
import com.calypso.ui.property.DateProperty;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyTable;
import com.jidesoft.grid.PropertyTableModel;

public class DatePropertiesTableBuilder {

	private PropertyTable dateTable = null;

	private DateProperty startDateProp;
	private DateProperty endDateProp;

	private Property endTenor;

	public DatePropertiesTableBuilder() {
		initProps();
		display();
	}

	private void initProps() {
		this.startDateProp = PropertyFactory.makeDateProperty("Start Date",
				"Start Date", null, null);
		this.endDateProp = PropertyFactory.makeDateProperty("End Date",
				"End Date", null, null);

		// MIGRATION V14.4 18/01/2015
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for (Tenor tenor : Tenor.values()) {
			ret.put(tenor.getName(), tenor.getName());
		}
		this.endTenor = PropertyFactory.makeEnumProperty("+", "+", null, ret);

		this.endTenor.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String tenor = (String) DatePropertiesTableBuilder.this.endTenor
						.getValue();
				if (tenor == null) {
					return;
				}
				Tenor t = new Tenor(tenor);
				JDate endDate = JDate.getNow().addDays(t.getCode());
				DatePropertiesTableBuilder.this.endDateProp.setValue(endDate
						.getDate(null));
			}
		});
	}

	public JComponent getComponent() {
		ArrayList<Property> properties = new ArrayList<Property>(2);

		properties.add(this.startDateProp);
		properties.add(this.endDateProp);

		PropertyTableModel<Property> dateTableModel = new PropertyTableModel<Property>(
				properties) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getColumnName(int i) {
				if (i == 0) {
					return "<html><b>Date Properties</b></html>";
				}
				return "";
			}

		};

		dateTableModel.setOrder(PropertyTableModel.UNSORTED);
		this.dateTable = new PropertyTable(dateTableModel);

		this.dateTable.expandAll();
		PropertyTableUtilities.setupPropertyTableKeyActions(this.dateTable);

		JScrollPane panel = new JScrollPane();
		panel.getViewport().add(this.dateTable);
		panel.getViewport().setBackground(this.dateTable.getBackground());
		panel.setBorder(BorderFactory.createLineBorder(Color.black));
		this.dateTable.getTableHeader().setBackground(Color.black);
		this.dateTable.getTableHeader().setForeground(Color.white);

		return panel;
	}

	public JComponent getComponent(Dimension dim) {
		JComponent comp = getComponent();
		comp.setMinimumSize(dim);
		comp.setPreferredSize(dim);
		comp.setMaximumSize(dim);
		comp.setSize(dim);
		return comp;
	}

	private void display() {
		JDate now = JDate.getNow();
		this.startDateProp.setValue(now.addDays(-5));
		this.endDateProp.setValue(now.addDays(10));
	}

	public ReportTemplate getTemplate(ReportTemplate template) {

		// following is done to avoid losing information if editing is not
		// stopped for table by and keypress like tab,
		// enter etc.
		if ((this.dateTable != null) && this.dateTable.isEditing()) {
			this.dateTable.getCellEditor().stopCellEditing();
			try {
				int i = 0;
				while (this.dateTable.isEditing() && (i < 5)) {
					// wait until the editing thread has a chance
					// to stop the cell editing.
					wait(++i * 10);
				}
			} catch (Exception e2) {
				Log.debug(Log.GUI, "exception:" + e2);
			}
		}

		template.remove(ReportTemplate.START_DATE);
		template.remove(ReportTemplate.END_DATE);

		JDate date = this.startDateProp.getValueAsJDate();
		template.put(ReportTemplate.START_DATE, date != null ? date.toString()
				: null);
		date = this.endDateProp.getValueAsJDate();
		template.put(ReportTemplate.END_DATE, date != null ? date.toString()
				: null);

		return template;
	}

	public void setTemplate(ReportTemplate template) {
		String value = null;

		value = (String) template.get(ReportTemplate.START_DATE);
		if (!Util.isEmpty(value)) {
			this.startDateProp.setValue(JDate.valueOf(value));
		} else {
			this.startDateProp.setValue(null);
		}

		value = (String) template.get(ReportTemplate.END_DATE);
		if (!Util.isEmpty(value)) {
			this.endDateProp.setValue(JDate.valueOf(value));
		} else {
			this.endDateProp.setValue(null);
		}
	}

}
