/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.ui.component.table.propertytable.PropertyTableUtilities;
import com.calypso.ui.factory.PropertyFactory;
import com.calypso.ui.property.DateProperty;
import com.calypso.ui.property.EnumProperty;
import com.calypso.ui.property.ExtendedEditionProperty;
import com.calypso.ui.property.LegalEntityProperty;
import com.calypso.ui.property.StringProperty;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyTable;
import com.jidesoft.grid.PropertyTableModel;

import calypsox.tk.report.SantAuditReportTemplate;

public class SantAuditReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = -2216340651361984998L;

	protected ReportTemplate template = null;

	public static final String ALL = "ALL";

	private static final String BOTH = "BOTH";

	private static final String CASH = "CASH";

	private static final String SECURITY = "SECURITY";
	
	// Right Panel
	private StringProperty dateStringTitleProp;
	private StringProperty additionalFieldsTitleProp;

	private DateProperty fromDateProp;
	private DateProperty toDateProp;

	// Left Panel
	private StringProperty poPartiesTitleProp;
	private StringProperty cptyPartiesTitleProp;
	private EnumProperty<String> poCollateralTypeProp;
	private EnumProperty<String> cptyCollateralTypeProp;

	private StringProperty contractSetupTitleProp;

	private EnumProperty<String> baseCcyProp;
	private ExtendedEditionProperty<LegalEntity> poOwnerProp;
	private ExtendedEditionProperty<LegalEntity> counterPartyProp;
	private EnumProperty<String> hedgeFundProp;
	private EnumProperty<String> contractTypeProp;
	private EnumProperty<String> headCloneProp;
	private EnumProperty<String> instrumentTypeProp;;

	private PropertyTable leftTable = null;
	private PropertyTable rightTable = null;

	public SantAuditReportTemplatePanel() {
		init();
		display();
	}

	private void init() {
		initProps();
		JScrollPane leftScrollPane = new JScrollPane();
		JScrollPane rightScrollPane = new JScrollPane();
		JTable leftTable = getLeftTable();
		JTable rightTable = getRightTable();
		leftScrollPane.getViewport().add(leftTable);
		rightScrollPane.getViewport().add(rightTable);

		leftScrollPane.getViewport().setBackground(leftTable.getBackground());
		rightScrollPane.getViewport().setBackground(rightTable.getBackground());

		setLayout(new GridLayout(1, 2));
		this.add(leftScrollPane);
		this.add(rightScrollPane);
		this.setSize(new Dimension(900, 220));
	}

	private JTable getRightTable() {
		ArrayList<Property> properties = new ArrayList<Property>();

		this.dateStringTitleProp.addChild(this.fromDateProp);
		this.dateStringTitleProp.addChild(this.toDateProp);

		this.additionalFieldsTitleProp.addChild(this.hedgeFundProp);
		this.additionalFieldsTitleProp.addChild(this.headCloneProp);

		properties.add(this.dateStringTitleProp);
		properties.add(getEmptyRow());
		properties.add(this.additionalFieldsTitleProp);

		PropertyTableModel<Property> rightTableModel = new PropertyTableModel<Property>(properties);
		rightTableModel.setOrder(PropertyTableModel.UNSORTED);
		this.rightTable = new PropertyTable(rightTableModel);
		this.rightTable.expandAll();
		PropertyTableUtilities.setupPropertyTableKeyActions(this.rightTable);
		return this.rightTable;
	}

	private JTable getLeftTable() {
		ArrayList<Property> properties = new ArrayList<Property>();

		this.poPartiesTitleProp.addChild(this.poOwnerProp);
		this.poPartiesTitleProp.addChild(this.poCollateralTypeProp);

		this.cptyPartiesTitleProp.addChild(this.counterPartyProp);
		this.cptyPartiesTitleProp.addChild(this.cptyCollateralTypeProp);

		this.contractSetupTitleProp.addChild(this.baseCcyProp);
		this.contractSetupTitleProp.addChild(this.contractTypeProp);
		this.contractSetupTitleProp.addChild(this.instrumentTypeProp);

		properties.add(this.poPartiesTitleProp);
		properties.add(getEmptyRow());
		properties.add(this.cptyPartiesTitleProp);
		properties.add(getEmptyRow());
		properties.add(this.contractSetupTitleProp);

		PropertyTableModel<Property> leftTableModel = new PropertyTableModel<Property>(properties);
		leftTableModel.setOrder(PropertyTableModel.UNSORTED);
		this.leftTable = new PropertyTable(leftTableModel);
		this.leftTable.expandAll();
		PropertyTableUtilities.setupPropertyTableKeyActions(this.leftTable);
		return this.leftTable;
	}

	@SuppressWarnings("deprecation")
	private void initProps() {

		// Right Panel
		this.dateStringTitleProp = PropertyFactory.makeStringProperty("Date Properties", "Date Properties", null);
		this.dateStringTitleProp.setEditable(false);

		this.fromDateProp = PropertyFactory.makeDateProperty("From Date", "From Date", null, null);

		this.toDateProp = PropertyFactory.makeDateProperty("To Date", "To Date", null, null);

		this.additionalFieldsTitleProp = PropertyFactory.makeStringProperty("Additional Fields", "Additional Fields",
				null);
		this.additionalFieldsTitleProp.setEditable(false);
		this.headCloneProp = PropertyFactory.makeEnumProperty("Head Clone", "Head Clone", null, getHeadClones());
		this.hedgeFundProp = PropertyFactory.makeEnumProperty("Hedge Fund", "Hedge Fund", null, getHedgeFunds());

		// Left Panel
		this.poPartiesTitleProp = PropertyFactory.makeStringProperty("Processing Org", "Processing Org", null);
		this.poPartiesTitleProp.setEditable(false);

		this.cptyPartiesTitleProp = PropertyFactory.makeStringProperty("CounterParty", "CounterParty", null);
		this.cptyPartiesTitleProp.setEditable(false);

		this.poOwnerProp = new LegalEntityProperty(LegalEntity.PROCESSINGORG, 1, true);
		this.counterPartyProp = new LegalEntityProperty(LegalEntity.COUNTERPARTY, 1, true);
		this.poCollateralTypeProp = PropertyFactory.makeEnumProperty("Collateral Type", "Collateral Type", null, getCollateralTypes());
		this.cptyCollateralTypeProp = PropertyFactory.makeEnumProperty("Collateral Type", "Collateral Type", null,
				getCollateralTypes());

		this.contractSetupTitleProp = PropertyFactory.makeStringProperty("Contract Setup", "Contract Setup", null);
		this.contractSetupTitleProp.setEditable(false);
		this.baseCcyProp = PropertyFactory.makeEnumProperty("Base Currency", "Base Currency", null, getCurrencies());
		this.contractTypeProp = PropertyFactory.makeEnumProperty("Contract Type", "Contract Type", null, getContractTypes());
		this.instrumentTypeProp = PropertyFactory.makeEnumProperty("Instrument Type", "Instrument Type", null, getInstrumentTypes());

	}

	private StringProperty getEmptyRow() {
		StringProperty emptyRow = PropertyFactory.makeStringProperty("", "", null);
		emptyRow.setEditable(false);
		return emptyRow;
	}

	private Vector<String> getCollateralTypes() {
		Vector<String> v = new Vector<String>();
		v.add(0, ALL);
		v.add(1, BOTH);
		v.add(2, CASH);
		v.add(3, SECURITY);
		return v;
	}

	// TODO
	private Vector<String> getInstrumentTypes() {
		Vector<String> v = LocalCache.getDomainValues(DSConnection.getDefault(), "productType");
		Collections.sort(v);
		v.add(0, ALL);
		return v;
	}

	private Vector<String> getHeadClones() {
		Vector<String> v = LocalCache.getDomainValues(DSConnection.getDefault(), "mccAdditionalField.HEAD_CLONE");
		Collections.sort(v);
		v.add(0, ALL);
		return v;
	}

	private Vector<String> getContractTypes() {
		Vector<String> v = LocalCache.getDomainValues(DSConnection.getDefault(), "legalAgreementType");
		Collections.sort(v);
		v.add(0, ALL);
		return v;
	}

	private Vector<String> getCurrencies() {
		Vector<String> v = LocalCache.getCurrencies();
		Collections.sort(v);
		v.add(0, ALL);
		return v;
	}

	private Vector<String> getHedgeFunds() {
		Vector<String> v = LocalCache.getDomainValues(DSConnection.getDefault(),
				"mccAdditionalField.HEDGE_FUNDS_REPORT");
		Collections.sort(v);
		v.add(0, ALL);
		return v;
	}

	public void display() {
		this.baseCcyProp.setValue(ALL);
		this.hedgeFundProp.setValue(ALL);
		this.contractTypeProp.setValue(ALL);
		this.headCloneProp.setValue(ALL);
		this.instrumentTypeProp.setValue(ALL);
		this.poCollateralTypeProp.setValue(ALL);
		this.cptyCollateralTypeProp.setValue(ALL);
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		if (template == null) {
			return;
		}
		this.template = template;

		String s = (String) this.template.get(SantAuditReportTemplate.PO);
		if (s != null) {
			this.poOwnerProp.setValue(s);
		} else {
			this.poOwnerProp.setValue(ALL);
		}

		s = (String) this.template.get(SantAuditReportTemplate.CPTY);
		if (s != null) {
			this.counterPartyProp.setValue(s);
		} else {
			this.counterPartyProp.setValue(ALL);
		}

		s = (String) this.template.get(SantAuditReportTemplate.BASE_CCY);
		if (s != null) {
			this.baseCcyProp.setValue(s);
		} else {
			this.baseCcyProp.setValue(ALL);
		}

		s = (String) this.template.get(SantAuditReportTemplate.CONTRACT_TYPE);
		if (s != null) {
			this.contractTypeProp.setValue(s);
		} else {
			this.contractTypeProp.setValue(ALL);
		}

		s = (String) this.template.get(SantAuditReportTemplate.PO_COLLAT_TYPE);
		if (s != null) {
			this.poCollateralTypeProp.setValue(s);
		} else {
			this.poCollateralTypeProp.setValue(ALL);
		}

		s = (String) this.template.get(SantAuditReportTemplate.CPTY_COLLAT_TYPE);
		if (s != null) {
			this.cptyCollateralTypeProp.setValue(s);
		} else {
			this.cptyCollateralTypeProp.setValue(ALL);
		}

		s = (String) this.template.get(SantAuditReportTemplate.HEAD_CLONE);
		if (s != null) {
			this.headCloneProp.setValue(s);
		} else {
			this.headCloneProp.setValue(ALL);
		}

		s = (String) this.template.get(SantAuditReportTemplate.HEDGE_FUND);
		if (s != null) {
			this.hedgeFundProp.setValue(s);
		} else {
			this.hedgeFundProp.setValue(ALL);
		}

		s = (String) this.template.get(SantAuditReportTemplate.INSTRUMENT_TYPE);
		if (s != null) {
			this.instrumentTypeProp.setValue(s);
		} else {
			this.instrumentTypeProp.setValue(ALL);
		}

		JDate fromDate = (JDate) this.template.get(SantAuditReportTemplate.FROM_DATE);
		if (fromDate != null) {
			this.fromDateProp.setValue(fromDate);
		} else {
			this.fromDateProp.setValue(null);
		}

		JDate toDate = (JDate) this.template.get(SantAuditReportTemplate.TO_DATE);
		if (toDate != null) {
			this.toDateProp.setValue(toDate);
		} else {
			this.toDateProp.setValue(null);
		}
	}

	@Override
	public ReportTemplate getTemplate() {

		// following is done to avoid losing information if editing is not stopped for table by and keypress like tab,
		// enter etc.
		if ((this.rightTable != null) && this.rightTable.isEditing()) {
			this.rightTable.getCellEditor().stopCellEditing();
			try {
				int i = 0;
				while (this.rightTable.isEditing() && (i < 5)) {
					// wait until the editing thread has a chance
					// to stop the cell editing.
					wait(++i * 10);
				}
			} catch (Exception e2) {
				Log.debug(Log.GUI, "exception:" + e2);
			}
		}

		// doing the same for left table
		if ((this.leftTable != null) && this.leftTable.isEditing()) {
			this.leftTable.getCellEditor().stopCellEditing();
			try {
				int i = 0;
				while (this.leftTable.isEditing() && (i < 5)) {
					// wait until the editing thread has a chance
					// to stop the cell editing.
					wait(++i * 10);
				}
			} catch (Exception e2) {
				Log.debug(Log.GUI, "exception:" + e2);
			}
		}

		this.template.remove(SantAuditReportTemplate.BASE_CCY);
		this.template.remove(SantAuditReportTemplate.CONTRACT_TYPE);
		this.template.remove(SantAuditReportTemplate.CPTY);
		this.template.remove(SantAuditReportTemplate.PO_COLLAT_TYPE);
		this.template.remove(SantAuditReportTemplate.CPTY_COLLAT_TYPE);
		this.template.remove(SantAuditReportTemplate.FROM_DATE);
		this.template.remove(SantAuditReportTemplate.HEAD_CLONE);
		this.template.remove(SantAuditReportTemplate.HEDGE_FUND);
		this.template.remove(SantAuditReportTemplate.INSTRUMENT_TYPE);
		this.template.remove(SantAuditReportTemplate.PO);
		this.template.remove(SantAuditReportTemplate.TO_DATE);

		this.template.put(SantAuditReportTemplate.BASE_CCY, this.baseCcyProp.getValue());
		this.template.put(SantAuditReportTemplate.CONTRACT_TYPE, this.contractTypeProp.getValue());

		if (this.counterPartyProp.getValue() instanceof LegalEntity) {
			this.template.put(SantAuditReportTemplate.CPTY, this.counterPartyProp.getValue().getCode());
		} else {
			this.template.put(SantAuditReportTemplate.CPTY, this.counterPartyProp.getValue());
		}
		if (this.poOwnerProp.getValue() instanceof LegalEntity) {
			this.template.put(SantAuditReportTemplate.PO, this.poOwnerProp.getValue().getCode());
		} else {
			this.template.put(SantAuditReportTemplate.PO, this.poOwnerProp.getValue());
		}

		this.template.put(SantAuditReportTemplate.PO_COLLAT_TYPE, this.poCollateralTypeProp.getValue());
		this.template.put(SantAuditReportTemplate.CPTY_COLLAT_TYPE, this.cptyCollateralTypeProp.getValue());
		this.template.put(SantAuditReportTemplate.FROM_DATE, this.fromDateProp.getValueAsJDate());
		this.template.put(SantAuditReportTemplate.HEAD_CLONE, this.headCloneProp.getValue());
		this.template.put(SantAuditReportTemplate.HEDGE_FUND, this.hedgeFundProp.getValue());
		this.template.put(SantAuditReportTemplate.INSTRUMENT_TYPE, this.instrumentTypeProp.getValue());

		this.template.put(SantAuditReportTemplate.TO_DATE, this.toDateProp.getValueAsJDate());

		return this.template;
	}

	@Override
	public boolean isValidLoad(ReportPanel panel) {
		ReportTemplate template = panel.getReport().getReportTemplate();

		JDate fromDate = (JDate) template.get(SantAuditReportTemplate.FROM_DATE);
		JDate toDate = (JDate) template.get(SantAuditReportTemplate.TO_DATE);

		StringBuilder errorMsgs = new StringBuilder();
		if (fromDate == null) {
			errorMsgs.append("Please enter a valid From Date.\n");
		}
		if (toDate == null) {
			errorMsgs.append("Please enter a valid To Date.\n");
		}
		if ((fromDate != null) && (toDate != null)) {
			if (fromDate.gte(toDate)) {
				errorMsgs.append("From Date has to be before To Date.\n");
			}
		}
		if (!errorMsgs.toString().isEmpty()) {
			AppUtil.displayError(panel, errorMsgs.toString());
			return false;
		}

		return true;
	}
}
