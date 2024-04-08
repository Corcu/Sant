/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview;

import static calypsox.tk.core.CollateralStaticAttributes.FITCH;
import static calypsox.tk.core.CollateralStaticAttributes.MOODY;
import static calypsox.tk.core.CollateralStaticAttributes.SC;
import static calypsox.tk.core.CollateralStaticAttributes.SNP;

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

import calypsox.tk.report.SantInventoryViewReportTemplate;
import calypsox.tk.report.inventoryview.property.CreditRatingHelper;
import calypsox.tk.report.inventoryview.property.CreditRatingMethod;

public class CreditRatingPropertiesTableBuilder {

	private PropertyTable creditRatingTable = null;

	private Property spEQRatingProp;
	private Property moodyEQRatingProp;
	private Property fitchEQRatingProp;

	private Property strictlySPRatingProp;
	private Property strictlyMoodyRatingProp;
	private Property strictlyFitchRatingProp;
	private Property strictlySCRatingProp;

	private final Map<String, CreditRatingHelper> ratingHelperMap = new HashMap<String, CreditRatingHelper>();

	public CreditRatingPropertiesTableBuilder() {
		initProps();
		// display();
	}

	private void initProps() {
		this.spEQRatingProp = makeProperty("Equivalent S&P", SNP, false);
		this.moodyEQRatingProp = makeProperty("Equivalent Moody", MOODY, false);
		this.fitchEQRatingProp = makeProperty("Equivalent Fitch", FITCH, false);

		this.strictlySPRatingProp = makeProperty("Strictly S&P", SNP, true);
		this.strictlyMoodyRatingProp = makeProperty("Strictly Moody", MOODY, true);
		this.strictlyFitchRatingProp = makeProperty("Strictly Fitch", FITCH, true);
		this.strictlySCRatingProp = makeProperty("Strictly SC", SC, true);

	}

	private Property makeProperty(String name, String ratingAgencyName, boolean isStrict) {
		return makeProperty(name, ratingAgencyName, isStrict, false);
	}

	private Property makeProperty(String name, String ratingAgencyName, boolean isStrict, boolean isInternal) {
		CreditRatingHelper helper = new CreditRatingHelper(name, ratingAgencyName, isStrict, isInternal);
		this.ratingHelperMap.put(name, helper);
		return helper.getCreditRatingProperty();
	}

	public JComponent getComponent() {
		ArrayList<Property> properties = new ArrayList<Property>();

		properties.add(this.spEQRatingProp);
		properties.add(this.moodyEQRatingProp);
		properties.add(this.fitchEQRatingProp);

		properties.add(this.strictlySPRatingProp);
		properties.add(this.strictlyMoodyRatingProp);
		properties.add(this.strictlyFitchRatingProp);
		properties.add(this.strictlySCRatingProp);

		PropertyTableModel<Property> creditRatingTableModel = new PropertyTableModel<Property>(properties) {

			private static final long serialVersionUID = -8168681714595333701L;

			@Override
			public String getColumnName(int i) {
				if (i == 0) {
					return "<html><b>Rating Properties</b></html>";
				}
				return "";
			}
		};
		creditRatingTableModel.setOrder(PropertyTableModel.UNSORTED);
		this.creditRatingTable = new PropertyTable(creditRatingTableModel);
		this.creditRatingTable.expandAll();
		PropertyTableUtilities.setupPropertyTableKeyActions(this.creditRatingTable);

		JScrollPane panel = new JScrollPane();
		panel.getViewport().add(this.creditRatingTable);
		panel.getViewport().setBackground(this.creditRatingTable.getBackground());
		panel.setBorder(BorderFactory.createLineBorder(Color.black));
		this.creditRatingTable.getTableHeader().setBackground(Color.black);
		this.creditRatingTable.getTableHeader().setForeground(Color.white);

		return panel;
	}

	public JComponent getComponent(Dimension dim) {
		JComponent comp = getComponent();
		comp.setMinimumSize(dim);
		comp.setPreferredSize(dim);
		comp.setMaximumSize(dim);
		return comp;
	}

	// private void display() {
	// }

	public ReportTemplate getTemplate(ReportTemplate template) {

		// following is done to avoid losing information if editing is not stopped for table by and keypress like tab,
		// enter etc.
		if ((this.creditRatingTable != null) && this.creditRatingTable.isEditing()) {
			this.creditRatingTable.getCellEditor().stopCellEditing();
			try {
				int i = 0;
				while (this.creditRatingTable.isEditing() && (i < 5)) {
					// wait until the editing thread has a chance
					// to stop the cell editing.
					wait(++i * 10);
				}
			} catch (Exception e2) {
				Log.debug(Log.GUI, "exception:" + e2);
			}
		}

		template.remove(SantInventoryViewReportTemplate.RATING_METHOD_SNP_EQV);
		CreditRatingMethod ratingMethod = (CreditRatingMethod) this.spEQRatingProp.getValue();
		if ((ratingMethod != null) && !ratingMethod.isEmpty()) {
			template.put(SantInventoryViewReportTemplate.RATING_METHOD_SNP_EQV, ratingMethod.toTemplateString());
		}

		template.remove(SantInventoryViewReportTemplate.RATING_METHOD_MOODY_EQV);
		ratingMethod = (CreditRatingMethod) this.moodyEQRatingProp.getValue();
		if ((ratingMethod != null) && !ratingMethod.isEmpty()) {
			template.put(SantInventoryViewReportTemplate.RATING_METHOD_MOODY_EQV, ratingMethod.toTemplateString());
		}

		template.remove(SantInventoryViewReportTemplate.RATING_METHOD_FITCH_EQV);
		ratingMethod = (CreditRatingMethod) this.fitchEQRatingProp.getValue();
		if ((ratingMethod != null) && !ratingMethod.isEmpty()) {
			template.put(SantInventoryViewReportTemplate.RATING_METHOD_FITCH_EQV, ratingMethod.toTemplateString());
		}

		template.remove(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_SNP);
		ratingMethod = (CreditRatingMethod) this.strictlySPRatingProp.getValue();
		if ((ratingMethod != null) && !ratingMethod.isEmpty()) {
			template.put(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_SNP, ratingMethod.toTemplateString());
		}

		template.remove(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_MOODY);
		ratingMethod = (CreditRatingMethod) this.strictlyMoodyRatingProp.getValue();
		if ((ratingMethod != null) && !ratingMethod.isEmpty()) {
			template.put(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_MOODY, ratingMethod.toTemplateString());
		}

		template.remove(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_FITCH);
		ratingMethod = (CreditRatingMethod) this.strictlyFitchRatingProp.getValue();
		if ((ratingMethod != null) && !ratingMethod.isEmpty()) {
			template.put(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_FITCH, ratingMethod.toTemplateString());
		}

		template.remove(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_SC);
		ratingMethod = (CreditRatingMethod) this.strictlySCRatingProp.getValue();
		if ((ratingMethod != null) && !ratingMethod.isEmpty()) {
			template.put(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_SC, ratingMethod.toTemplateString());
		}

		return template;

	}

	public void setTemplate(ReportTemplate template) {
		String ratingMethodStr = "";

		ratingMethodStr = (String) template.get(SantInventoryViewReportTemplate.RATING_METHOD_SNP_EQV);
		this.spEQRatingProp.setValue(CreditRatingMethod.valueOf(ratingMethodStr, SNP, false));

		ratingMethodStr = (String) template.get(SantInventoryViewReportTemplate.RATING_METHOD_MOODY_EQV);
		this.moodyEQRatingProp.setValue(CreditRatingMethod.valueOf(ratingMethodStr, MOODY, false));

		ratingMethodStr = (String) template.get(SantInventoryViewReportTemplate.RATING_METHOD_FITCH_EQV);
		this.fitchEQRatingProp.setValue(CreditRatingMethod.valueOf(ratingMethodStr, FITCH, false));

		ratingMethodStr = (String) template.get(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_SNP);
		this.strictlySPRatingProp.setValue(CreditRatingMethod.valueOf(ratingMethodStr, SNP, true));

		ratingMethodStr = (String) template.get(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_MOODY);
		this.strictlyMoodyRatingProp.setValue(CreditRatingMethod.valueOf(ratingMethodStr, MOODY, true));

		ratingMethodStr = (String) template.get(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_FITCH);
		this.strictlyFitchRatingProp.setValue(CreditRatingMethod.valueOf(ratingMethodStr, FITCH, true));

		ratingMethodStr = (String) template.get(SantInventoryViewReportTemplate.RATING_METHOD_STRICTLY_SC);
		this.strictlySCRatingProp.setValue(CreditRatingMethod.valueOf(ratingMethodStr, SC, true));

	}
}
