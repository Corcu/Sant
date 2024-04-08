/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.inventoryview;

import calypsox.apps.reporting.util.loader.MarginCallConfigLightLoader;
import calypsox.tk.report.SantInventoryViewReportTemplate;
import calypsox.tk.report.inventoryview.property.SantFactoryProperty;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.ui.component.table.propertytable.PropertyTableUtilities;
import com.calypso.ui.factory.PropertyFactory;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyTable;
import com.jidesoft.grid.PropertyTableModel;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

public class LegalPropertiesTableBuilder {

    private PropertyTable legalTable = null;

    private Property agentProp;
    private Property bookProp;
    private Property accBookProp;
    private Property contractProp;
    private Property securityIdsProp;

    private Property poProp;

    private Map<Integer, String> contractMap;

    public LegalPropertiesTableBuilder() {
        initProps();
        display();
    }

    private void display() {
        this.agentProp.setValue(null);
        this.bookProp.setValue(null);
        this.accBookProp.setValue(null);
        this.contractProp.setValue(null);
        this.securityIdsProp.setValue(null);
    }

    private void initProps() {
        this.agentProp = PropertyFactory.makeLegalEntityProperty("Agent", null);
        this.bookProp = makeBookProperty();
        this.accBookProp = makeAccBookProperty();
        this.contractProp = makeContractProperty();
        this.securityIdsProp = makeSecuritiesProperty();
        this.poProp = PropertyFactory.makeLegalEntityProperty(LegalEntity.PROCESSINGORG, null);
    }

    private Property makeSecuritiesProperty() {
        Vector<String> v = new Vector<String>();
        v.add(0, Product.BOND);
        v.add(0, Product.EQUITY);
        return SantFactoryProperty.makeProductChooserListPorperty("Securities", null, null, null, v);
    }

    public JComponent getComponent() {
        ArrayList<Property> properties = new ArrayList<Property>();

        properties.add(this.agentProp);
        properties.add(this.bookProp);
        properties.add(this.accBookProp);
        properties.add(this.contractProp);
        properties.add(this.securityIdsProp);
        properties.add(this.poProp);

        PropertyTableModel<Property> tableModel = new PropertyTableModel<Property>(properties) {

            private static final long serialVersionUID = -8168681714595333701L;

            @Override
            public String getColumnName(int i) {
                if (i == 0) {
                    return "<html><b>Legal Properties</b></html>";
                }
                return "";
            }
        };
        tableModel.setOrder(PropertyTableModel.UNSORTED);
        this.legalTable = new PropertyTable(tableModel);
        this.legalTable.expandAll();
        PropertyTableUtilities.setupPropertyTableKeyActions(this.legalTable);

        JScrollPane panel = new JScrollPane();
        panel.getViewport().add(this.legalTable);
        panel.getViewport().setBackground(this.legalTable.getBackground());
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.legalTable.getTableHeader().setBackground(Color.black);
        this.legalTable.getTableHeader().setForeground(Color.white);

        return panel;
    }

    public JComponent getComponent(Dimension dim) {
        JComponent comp = getComponent();
        comp.setMinimumSize(dim);
        comp.setPreferredSize(dim);
        comp.setMaximumSize(dim);
        return comp;
    }

    private Property makeBookProperty() {
        Map<String, Book> bookHashTable = BOCache.getBooks(DSConnection.getDefault());
        List<String> sortedBookList = new ArrayList<String>(bookHashTable.keySet());
        Collections.sort(sortedBookList);
        return SantFactoryProperty.makeChooserListPorperty("Books", "Books", null, null, new Vector<String>(
                sortedBookList));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Property makeAccBookProperty() {
        List<String> sortedBookList = new ArrayList<String>();
        try {
            Hashtable accountingBooks = DSConnection.getDefault().getRemoteAccounting().getAccountingBooks();
            sortedBookList = new ArrayList<String>(accountingBooks.keySet());
            Collections.sort(sortedBookList);
        } catch (RemoteException e) {
            Log.debug(Log.ERROR, "Error getting accounting books :", e);
        }
        return SantFactoryProperty.makeChooserListPorperty("Acc Books", "Acc Books", null, null, new Vector<String>(
                sortedBookList));

    }

    private Property makeContractProperty() {
        this.contractMap = new MarginCallConfigLightLoader().load();
        return SantFactoryProperty.makeChooserListPorperty("Contracts", "Contracts", null, null, new Vector<String>(
                this.contractMap.values()));
    }

    @SuppressWarnings("unchecked")
    public ReportTemplate getTemplate(ReportTemplate template) {

        // following is done to avoid losing information if editing is not stopped for table by and keypress like tab,
        // enter etc.
        if ((this.legalTable != null) && this.legalTable.isEditing()) {
            this.legalTable.getCellEditor().stopCellEditing();
            try {
                int i = 0;
                while (this.legalTable.isEditing() && (i < 5)) {
                    // wait until the editing thread has a chance
                    // to stop the cell editing.
                    wait(++i * 10);
                }
            } catch (Exception e2) {
                Log.debug(Log.GUI, "exception:" + e2);
            }
        }

        template.remove(BOSecurityPositionReportTemplate.AGENT_ID);
        template.remove(BOSecurityPositionReportTemplate.BOOK_LIST);
        template.remove(SantInventoryViewReportTemplate.CONTRACTS);
        template.remove(SantInventoryViewReportTemplate.SEC_LIST);
        template.remove(SantInventoryViewReportTemplate.PROCESSING_ORG);

        if (this.agentProp.getValue() != null) {
            template.put(BOSecurityPositionReportTemplate.AGENT_ID, ((LegalEntity) this.agentProp.getValue()).getCode());
        }
        if (this.bookProp.getValue() != null) {
            template.put(BOSecurityPositionReportTemplate.BOOK_LIST,
                    Util.collectionToString((List<String>) this.bookProp.getValue()));
        }
        if (this.accBookProp.getValue() != null) {
            template.put(SantInventoryViewReportTemplate.ACC_BOOK_LIST,
                    Util.collectionToString((List<String>) this.accBookProp.getValue()));
        }

        if (this.contractProp.getValue() != null) {
            template.put(SantInventoryViewReportTemplate.CONTRACTS,
                    getContractIds((Vector<String>) this.contractProp.getValue()));
        }

        if (this.securityIdsProp.getValue() != null) {
            template.put(SantInventoryViewReportTemplate.SEC_LIST,
                    Util.collectionToString((List<Integer>) this.securityIdsProp.getValue()));
        }

        if (this.poProp.getValue() != null) {
            template.put(BOSecurityPositionReportTemplate.PROCESSING_ORG,
                    ((LegalEntity) this.poProp.getValue()).getCode());
        }

        return template;
    }

    @SuppressWarnings("unchecked")
    public void setTemplate(ReportTemplate template) {
        String value = null;
        value = (String) template.get(BOSecurityPositionReportTemplate.AGENT_ID);
        if (!Util.isEmpty(value)) {
            LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), value);
            this.agentProp.setValue(le);
        } else {
            this.agentProp.setValue(null);
        }
        value = (String) template.get(BOSecurityPositionReportTemplate.BOOK_LIST);
        if (!Util.isEmpty(value)) {
            this.bookProp.setValue(Util.string2Vector(value));
        } else {
            this.bookProp.setValue(null);
        }

        value = (String) template.get(SantInventoryViewReportTemplate.ACC_BOOK_LIST);
        if (!Util.isEmpty(value)) {
            this.accBookProp.setValue(Util.string2Vector(value));
        } else {
            this.accBookProp.setValue(null);
        }

        Vector<Integer> contractIds = (Vector<Integer>) template.get(SantInventoryViewReportTemplate.CONTRACTS);
        if (!Util.isEmpty(contractIds)) {
            this.contractProp.setValue(getContractNames(contractIds));
        } else {
            this.contractProp.setValue(null);
        }

        String securityIds = (String) template.get(SantInventoryViewReportTemplate.SEC_LIST);
        if (!Util.isEmpty(securityIds)) {
            this.securityIdsProp.setValue(Util.string2IntVector(securityIds));
        } else {
            this.securityIdsProp.setValue(null);
        }

        value = (String) template.get(BOSecurityPositionReportTemplate.PROCESSING_ORG);
        if (!Util.isEmpty(value)) {
            LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), value);
            this.poProp.setValue(le);
        } else {
            this.poProp.setValue(null);
        }

    }

    private Vector<String> getContractNames(List<Integer> contractIds) {
        Vector<String> contractNames = new Vector<String>();
        for (Integer contractId : contractIds) {
            contractNames.add(this.contractMap.get(contractId));
        }
        return contractNames;
    }

    private Vector<Integer> getContractIds(List<String> contractNames) {
        Vector<Integer> contractIds = new Vector<Integer>();
        for (Entry<Integer, String> entry : this.contractMap.entrySet()) {
            if (contractNames.contains(entry.getValue())) {
                contractIds.add(entry.getKey());
            }
        }
        return contractIds;
    }
}
