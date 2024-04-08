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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.ui.component.table.propertytable.PropertyTableUtilities;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyTable;
import com.jidesoft.grid.PropertyTableModel;

import calypsox.tk.report.SantInventoryViewReportTemplate;
import calypsox.tk.report.inventoryview.property.HaircutData;
import calypsox.tk.report.inventoryview.property.HaircutHelper;
import calypsox.tk.report.inventoryview.property.HaircutMethod;

public class HaircutPropertiesTableBuilder {

	private PropertyTable haircutTable = null;

	private Property ecbHaircutProp;
	private Property swissHaircutProp;
	private Property fedHaircutProp;
	private Property boeHaircutProp;

	public HaircutPropertiesTableBuilder() {
		initProps();
	}

	private void initProps() {
		this.ecbHaircutProp = makeHaircutProperty("ECB");
		this.swissHaircutProp = makeHaircutProperty("Swiss");
		this.fedHaircutProp = makeHaircutProperty("FED");
		this.boeHaircutProp = makeHaircutProperty("BOE");
	}

	private Property makeHaircutProperty(String haircutType) {
		HaircutHelper helper = new HaircutHelper(haircutType);
		return helper.getHaircutProperty();
	}

	public JComponent getComponent() {
		ArrayList<Property> properties = new ArrayList<Property>();

		properties.add(this.ecbHaircutProp);
		properties.add(this.swissHaircutProp);
		properties.add(this.fedHaircutProp);
		properties.add(this.boeHaircutProp);

		PropertyTableModel<Property> haircutTableModel = new PropertyTableModel<Property>(properties) {

			private static final long serialVersionUID = -8168681714595333701L;

			@Override
			public String getColumnName(int i) {
				if (i == 0) {
					return "<html><b>Haircut Properties</b></html>";
				}
				return "";
			}
		};
		haircutTableModel.setOrder(PropertyTableModel.UNSORTED);
		this.haircutTable = new PropertyTable(haircutTableModel);
		this.haircutTable.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
		this.haircutTable.expandAll();
		PropertyTableUtilities.setupPropertyTableKeyActions(this.haircutTable);

		JScrollPane positionPanel = new JScrollPane();
		positionPanel.getViewport().add(this.haircutTable);
		positionPanel.getViewport().setBackground(this.haircutTable.getBackground());
		positionPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		this.haircutTable.getTableHeader().setBackground(Color.black);
		this.haircutTable.getTableHeader().setForeground(Color.white);

		return positionPanel;
	}

	public JComponent getComponent(Dimension dim) {
		JComponent comp = getComponent();
		comp.setMinimumSize(dim);
		comp.setPreferredSize(dim);
		comp.setMaximumSize(dim);
		return comp;
	}

	public ReportTemplate getTemplate(ReportTemplate template) {

		// following is done to avoid losing information if editing is not stopped for table by and keypress like tab,
		// enter etc.
		if ((this.haircutTable != null) && this.haircutTable.isEditing()) {
			this.haircutTable.getCellEditor().stopCellEditing();
			try {
				int i = 0;
				while (this.haircutTable.isEditing() && (i < 5)) {
					// wait until the editing thread has a chance
					// to stop the cell editing.
					wait(++i * 10);
				}
			} catch (Exception e2) {
				Log.debug(Log.GUI, "exception:" + e2);
			}
		}

		template.remove(SantInventoryViewReportTemplate.HAIRCUTS);

		List<HaircutData> haircutDataList = new ArrayList<HaircutData>();

		HaircutMethod method = (HaircutMethod) this.ecbHaircutProp.getValue();
		if (method != null) {
			if (!Util.isEmpty(method.toString())) {
				HaircutData data = new HaircutData("ECB");
				data.setMin(method.getMinAmount());
				data.setMax(method.getMaxAmount());
				haircutDataList.add(data);
			}
		}

		method = (HaircutMethod) this.swissHaircutProp.getValue();
		if (method != null) {
			if (!Util.isEmpty(method.toString())) {
				HaircutData data = new HaircutData("Swiss");
				data.setMin(method.getMinAmount());
				data.setMax(method.getMaxAmount());
				haircutDataList.add(data);
			}
		}

		method = (HaircutMethod) this.fedHaircutProp.getValue();
		if (method != null) {
			if (!Util.isEmpty(method.toString())) {
				HaircutData data = new HaircutData("FED");
				data.setMin(method.getMinAmount());
				data.setMax(method.getMaxAmount());
				haircutDataList.add(data);
			}
		}

		method = (HaircutMethod) this.boeHaircutProp.getValue();
		if (method != null) {
			if (!Util.isEmpty(method.toString())) {
				HaircutData data = new HaircutData("BOE");
				data.setMin(method.getMinAmount());
				data.setMax(method.getMaxAmount());
				haircutDataList.add(data);
			}
		}

		template.put(SantInventoryViewReportTemplate.HAIRCUTS, haircutDataList);
		return template;
	}

	@SuppressWarnings("unchecked")
	public void setTemplate(ReportTemplate template) {

		List<HaircutData> haircutDataList = (List<HaircutData>) template.get(SantInventoryViewReportTemplate.HAIRCUTS);
		if (Util.isEmpty(haircutDataList)) {
			return;
		}
		for (HaircutData data : haircutDataList) {
			if ("ECB".equals(data.getEntity())) {
				buildProperty(this.ecbHaircutProp, data);
			} else if ("Swiss".equals(data.getEntity())) {
				buildProperty(this.swissHaircutProp, data);
			} else if ("FED".equals(data.getEntity())) {
				buildProperty(this.fedHaircutProp, data);
			} else if ("BOE".equals(data.getEntity())) {
				buildProperty(this.boeHaircutProp, data);
			}
		}

	}

	private void buildProperty(Property property, HaircutData data) {
		HaircutMethod method = new HaircutMethod();
		method.setMinAmount(data.getMin());
		method.setMaxAmount(data.getMax());
		property.setValue(method);
	}
}
