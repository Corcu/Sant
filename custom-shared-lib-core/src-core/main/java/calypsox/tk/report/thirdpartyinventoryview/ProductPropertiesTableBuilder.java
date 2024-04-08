/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.thirdpartyinventoryview;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.ui.component.table.propertytable.PropertyTableUtilities;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyTable;
import com.jidesoft.grid.PropertyTableModel;

import calypsox.tk.report.SantThirdPartyInventoryViewReportTemplate;
import calypsox.tk.report.thirdpartyinventoryview.property.ProductHelper;
import calypsox.tk.report.thirdpartyinventoryview.property.Rate;

public class ProductPropertiesTableBuilder {

	private PropertyTable productTable = null;

	private Property stockLendingProp;

	private final Map<String, ProductHelper> productHelperMap = new HashMap<String, ProductHelper>();

	public ProductPropertiesTableBuilder() {
		initProps();
	}

	private void initProps() {
		this.stockLendingProp = makeProperty("Stock Lending Rate");
	}

	private Property makeProperty(String name) {
		ProductHelper helper = new ProductHelper(name);
		this.productHelperMap.put(name, helper);
		return helper.getProductProperty();
	}

	public JComponent getComponent() {
		ArrayList<Property> properties = new ArrayList<Property>();

		properties.add(this.stockLendingProp);

		PropertyTableModel<Property> productTableModel = new PropertyTableModel<Property>(properties) {

			private static final long serialVersionUID = -8168681714595333701L;

			@Override
			public String getColumnName(int i) {
				if (i == 0) {
					return "<html><b>Product Properties</b></html>";
				}
				return "";
			}
		};
		productTableModel.setOrder(PropertyTableModel.UNSORTED);
		this.productTable = new PropertyTable(productTableModel);
		this.productTable.expandAll();
		PropertyTableUtilities.setupPropertyTableKeyActions(this.productTable);

		JScrollPane panel = new JScrollPane();
		panel.getViewport().add(this.productTable);
		panel.getViewport().setBackground(this.productTable.getBackground());
		panel.setBorder(BorderFactory.createLineBorder(Color.black));
		this.productTable.getTableHeader().setBackground(Color.black);
		this.productTable.getTableHeader().setForeground(Color.white);

		return panel;
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
		if ((this.productTable != null) && this.productTable.isEditing()) {
			this.productTable.getCellEditor().stopCellEditing();
			try {
				int i = 0;
				while (this.productTable.isEditing() && (i < 5)) {
					// wait until the editing thread has a chance
					// to stop the cell editing.
					wait(++i * 10);
				}
			} catch (Exception e2) {
				Log.debug(Log.GUI, "exception:" + e2);
			}
		}

		template.remove(SantThirdPartyInventoryViewReportTemplate.RATE);

		Rate rate = (Rate) this.stockLendingProp.getValue();

		template.put(SantThirdPartyInventoryViewReportTemplate.RATE, rate);

		return template;
	}

	public void setTemplate(ReportTemplate template) {
		Rate stockLendingRate;

		stockLendingRate = (Rate) template.get(SantThirdPartyInventoryViewReportTemplate.RATE);
		this.stockLendingProp.setValue(stockLendingRate);
	}
}
