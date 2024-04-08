/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.product;

import calypsox.tk.product.BondCustomData;
import com.calypso.apps.product.CustomDataWindow;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.service.DSConnection;
import com.calypso.ui.component.table.propertytable.PropertyTableUtilities;
import com.calypso.ui.factory.PropertyFactory;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyTable;
import com.jidesoft.grid.PropertyTableModel;
import com.jidesoft.swing.NullPanel;
import org.jfree.util.Log;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Defines the Bond Custom Data
 *
 * @author xIS16412
 * @version 3.0, added MEFF & Eurex haircuts
 */
public class BondCustomDataWindow extends JDialog implements CustomDataWindow {

    private static final long serialVersionUID = 1L;
    private static String ISIN = "ISIN";
    private static String RATE = "Rate";
    private static String EXPIRED_DATE_TYPE = "Expired date TYPE";
    private static String EXPIRED_DATE = "Expired date";
    private static String LAST_UPDATE = "Last update";
    private static String ALWAYS = "ALWAYS";
    private static String NEVER = "NEVER";
    private static String CUSTOM = "CUSTOM";
    private static String ERROR_WINDOW_NAME = "Properties error";

    private HaircutTable haircutTable = null;
    private SLRTable SLRtable = null;

    private Product product;

    // value error types
    public enum ValueErrorTypes {

        NO_RATE_ERROR("'Rate' is not valid."),
        NO_EXP_DATE_TYPE_ERROR("'Expired date TYPE' is not valid."),
        NO_EXP_DATE_ERROR("'Expired date' is not valid.");

        protected String message;

        private ValueErrorTypes(String message) {
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }

    }

    public BondCustomDataWindow() {
        setTitle("Bond - Custom Data Window");
        setModal(true);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocation(200, 200);
        this.haircutTable = new HaircutTable();
        this.SLRtable = new SLRTable();
    }

    @Override
    public void init() {
        getContentPane().add(new NullPanel());
        getContentPane().add(this.haircutTable.getComponent(), BorderLayout.WEST);
        getContentPane().add(this.SLRtable.getComponent(), BorderLayout.EAST);
        getContentPane().add(getToolBar(), BorderLayout.SOUTH);
        setResizable(false);
        pack();

    }

    @Override
    public void showCustomData(Product p) {
        BondCustomData customData = (BondCustomData) p.getCustomData();
        if (customData != null) {
            this.haircutTable.display(customData);
            this.SLRtable.display(customData);
        }
    }

    // @Override
    // public void buildCustomData(Product p) {
    // if (this.product == null) {
    // this.product = p;
    // return;
    // }
    //
    // BondCustomData customData = new BondCustomData();
    // p.setCustomData(customData);
    // if (customData != null) {
    // customData.setProductId(p.getId());
    // customData.setVersion(p.getVersion());
    // this.haircutTable.buildCustomData(customData);
    // }
    // }

    // This method is launched when click on "CustomData button"
    @Override
    public void buildCustomData(Product p) {

        // link product
        setProduct(p);
        // initialize table properties
        this.haircutTable.clear();
        this.SLRtable.clear();
        this.SLRtable.setPropertiesDefaultEdition();
        this.SLRtable.isin.setValue(this.product.getSecCode(ISIN));

    }

    public void setProduct(Product p) {
        this.product = p;
    }

    public void updateHaircutCustomData(Product p) {

        if (p.getCustomData() != null) {
            this.haircutTable.buildCustomData((BondCustomData) p.getCustomData());
        } else {
            BondCustomData customData = new BondCustomData();
            customData.setProductId(p.getId());
            customData.setVersion(p.getVersion());
            this.haircutTable.buildCustomData(customData);
            p.setCustomData(customData);
        }

    }

    public void updateSLRCustomData(Product p) {

        if (p.getCustomData() != null) {
            this.SLRtable.buildCustomData((BondCustomData) p.getCustomData());
        } else {
            BondCustomData customData = new BondCustomData();
            customData.setProductId(p.getId());
            customData.setVersion(p.getVersion());
            this.SLRtable.buildCustomData(customData);
            p.setCustomData(customData);
        }

    }

    private Component getToolBar() {
        JToolBar toolbar = new JToolBar();

        ToolBarAction action = new ToolBarAction();

        JButton applyButton = createButton(ToolBarAction.APPLY, action);
        JButton loadButton = createButton(ToolBarAction.LOAD, action);
        JButton closeButton = createButton(ToolBarAction.CLOSE, action);
        JButton clearButton = createButton(ToolBarAction.CLEAR, action);

        toolbar.add(closeButton);
        toolbar.add(applyButton);
        toolbar.add(clearButton);
        toolbar.add(loadButton);
        toolbar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        toolbar.setFloatable(false);
        return toolbar;
    }

    private JButton createButton(String name, ToolBarAction action) {
        JButton btn = new JButton(name);
        btn.setActionCommand(name);
        btn.addActionListener(action);
        btn.setMinimumSize(new Dimension(70, 25));
        btn.setMaximumSize(new Dimension(70, 25));
        btn.setPreferredSize(new Dimension(70, 25));
        btn.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        return btn;
    }

    protected class ToolBarAction implements ActionListener {

        public static final String APPLY = "Apply";
        public static final String LOAD = "Load";
        public static final String CLOSE = "Close";
        public static final String CLEAR = "Clear";

        @Override
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            if (APPLY.equals(command)) {
                // apply haricut
                updateHaircutCustomData(BondCustomDataWindow.this.product);
                // apply rates
                ValueErrorTypes error = BondCustomDataWindow.this.SLRtable.checkValues();
                if (error == null) {
                    updateSLRCustomData(BondCustomDataWindow.this.product);
                    dispose();
                } else {
                    showValuesErrorMessage(error);
                }

            } else if (LOAD.equals(command)) {
                load();
            } else if (CLOSE.equals(command)) {
                dispose();
            } else if (CLEAR.equals(command)) {
                BondCustomDataWindow.this.haircutTable.clear();
                BondCustomDataWindow.this.SLRtable.clear();
            }

        }

        private void load() {
            try {
                Product p = DSConnection.getDefault().getRemoteProduct()
                        .getProduct(BondCustomDataWindow.this.product.getId());
                if ((p == null) || (p.getCustomData() == null)) {
                    return;
                }
                BondCustomDataWindow.this.haircutTable.display((BondCustomData) p.getCustomData());
                BondCustomDataWindow.this.SLRtable.display((BondCustomData) p.getCustomData());
            } catch (RemoteException ex) {
                Log.error(this, ex); //sonar
            }
        }

    }

    protected class PropertyChangeAction implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent propertychangeevent) {
            Property prop = (Property) propertychangeevent.getSource();
            if (prop.getCategory().equals(EXPIRED_DATE_TYPE)) {
                Object newValue = propertychangeevent.getNewValue();
                if (newValue != null) {
                    if (newValue.equals(ALWAYS)) {
                        BondCustomDataWindow.this.SLRtable.expiredDate.setValue(JDate.getNow().getNow(
                                TimeZone.getDefault()));
                        BondCustomDataWindow.this.SLRtable.expiredDate.setEditable(false);
                    } else if (newValue.equals(CUSTOM)) {
                        BondCustomDataWindow.this.SLRtable.expiredDate.setValue(null);
                        BondCustomDataWindow.this.SLRtable.expiredDate.setEditable(true);
                    } else if (newValue.equals(NEVER)) {
                        BondCustomDataWindow.this.SLRtable.expiredDate.setValue(null);
                        BondCustomDataWindow.this.SLRtable.expiredDate.setEditable(false);
                    }
                }
            }
        }
    }

    protected class HaircutTable {

        private PropertyTable table = null;

        private Property ecbHaircutProp;
        private Property swissHaircutProp;
        private Property fedHaircutProp;
        private Property boeHaircutProp;
        // OA START 18/11/2013 Eurex Haircuts Import
        private Property eurexHaircutProp;
        // GSM 14/07/2014. Added MEFF Haircut Import
        private Property meffHaircutProp;

        public HaircutTable() {
            initProps();
        }

        public void clear() {
            this.ecbHaircutProp.setValue(null);
            this.boeHaircutProp.setValue(null);
            this.swissHaircutProp.setValue(null);
            this.fedHaircutProp.setValue(null);
            // OA START 18/11/2013 Eurex Haircuts Import
            this.eurexHaircutProp.setValue(null);
            // OA END 18/11/2013 Eurex Haircuts Import
            // GSM 14/07/2014. Added MEFF Haircut Import
            this.meffHaircutProp.setValue(null);

        }

        public void display(BondCustomData customData) {
            this.ecbHaircutProp.setValue(customData.getHaircut_ecb());
            this.boeHaircutProp.setValue(customData.getHaircut_boe());
            this.swissHaircutProp.setValue(customData.getHaircut_swiss());
            this.fedHaircutProp.setValue(customData.getHaircut_fed());
            // OA START 18/11/2013 Eurex Haircuts Import
            this.eurexHaircutProp.setValue(customData.getHaircut_eurex());
            // OA END 18/11/2013 Eurex Haircuts Import
            // GSM 14/07/2014. Added MEFF Haircut Import
            this.meffHaircutProp.setValue(customData.getHaircut_meff());
        }

        public void buildCustomData(BondCustomData customData) {
            customData.setHaircut_ecb((Double) this.ecbHaircutProp.getValue());
            customData.setHaircut_boe((Double) this.boeHaircutProp.getValue());
            customData.setHaircut_swiss((Double) this.swissHaircutProp.getValue());
            customData.setHaircut_fed((Double) this.fedHaircutProp.getValue());
            // OA START 18/11/2013 Eurex Haircuts Import
            customData.setHaircut_eurex((Double) this.eurexHaircutProp.getValue());
            // OA END 18/11/2013 Eurex Haircuts Import
            // GSM 14/07/2014. Added MEFF Haircut Import
            customData.setHaircut_meff((Double) this.meffHaircutProp.getValue());

        }

        private void initProps() {
            this.ecbHaircutProp = makeHaircutProperty("ECB");
            this.swissHaircutProp = makeHaircutProperty("Swiss");
            this.fedHaircutProp = makeHaircutProperty("FED");
            this.boeHaircutProp = makeHaircutProperty("BOE");
            // OA START 18/11/2013 Eurex Haircuts Import
            this.eurexHaircutProp = makeHaircutProperty("Eurex");
            // OA END 18/11/2013 Eurex Haircuts Import
            // GSM 14/07/2014. Added MEFF Haircut Import
            this.meffHaircutProp = makeHaircutProperty("MEFF");
        }

        private Property makeHaircutProperty(String haircutType) {
            return PropertyFactory.makeNumberProperty(haircutType, haircutType, haircutType, 2);
        }

        public JComponent getComponent() {
            ArrayList<Property> properties = new ArrayList<Property>();

            properties.add(this.ecbHaircutProp);
            properties.add(this.swissHaircutProp);
            properties.add(this.boeHaircutProp);
            properties.add(this.fedHaircutProp);
            // OA START 18/11/2013 Eurex Haircuts Import
            properties.add(this.eurexHaircutProp);
            // OA END 18/11/2013 Eurex Haircuts Import
            // GSM 14/07/2014. Added MEFF Haircut Import
            properties.add(this.meffHaircutProp);

            PropertyTableModel<Property> haircutTableModel = new PropertyTableModel<Property>(properties) {

                private static final long serialVersionUID = 1L;

                @Override
                public String getColumnName(int i) {
                    if (i == 0) {
                        return "<html><b>Haircut Properties</b></html>";
                    }
                    return "";
                }
            };
            haircutTableModel.setOrder(PropertyTableModel.UNSORTED);
            this.table = new PropertyTable(haircutTableModel);
            this.table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
            this.table.expandAll();
            PropertyTableUtilities.setupPropertyTableKeyActions(this.table);

            JScrollPane positionPanel = new JScrollPane();
            positionPanel.getViewport().add(this.table);
            positionPanel.getViewport().setBackground(this.table.getBackground());
            positionPanel.setBorder(BorderFactory.createLineBorder(Color.black));
            this.table.getTableHeader().setBackground(Color.black);
            this.table.getTableHeader().setForeground(Color.white);

            positionPanel.setPreferredSize(new Dimension(280, 120));
            positionPanel.setMaximumSize(new Dimension(280, 120));
            positionPanel.setMinimumSize(new Dimension(280, 120));

            return positionPanel;
        }

    }

    protected class SLRTable {

        private PropertyTable table = null;

        private Property isin;
        private Property rate;
        private Property expiredDateType;
        private Property expiredDate;
        private Property lastUpdate;

        public SLRTable() {
            initProps();
        }

        public void clear() {
            this.rate.setValue(null);
            this.expiredDateType.setValue(null);
            this.expiredDate.setValue(null);
            this.lastUpdate.setValue(null);
        }

        public void display(BondCustomData customData) {

            // rate
            this.rate.setValue(customData.getFee());
            // expired date type
            if (customData.getExpired_date_type() != null) {
                if (customData.getExpired_date_type().equals(ALWAYS)) {
                    this.expiredDateType.setValue(ALWAYS);
                } else if (customData.getExpired_date_type().equals(NEVER)) {
                    this.expiredDateType.setValue(NEVER);
                } else if (customData.getExpired_date_type().equals(CUSTOM)) {
                    this.expiredDateType.setValue(CUSTOM);
                }
            }
            this.expiredDate.setValue(customData.getExpired_date());
            this.lastUpdate.setValue(customData.getLast_update());

        }

        public void buildCustomData(BondCustomData customData) {

            // update rate (and its last update) if it changes
            if ((Double) this.rate.getValue() != customData.getFee()) {
                customData.setFee((Double) this.rate.getValue());
                customData.setLast_update(JDate.getNow());
            }
            // update expired date type and expired date
            String expiredDateType = (String) this.expiredDateType.getValue();
            if (expiredDateType != null) {
                customData.setExpired_date_type(expiredDateType);
                if (expiredDateType.equals(CUSTOM)) {
                    Date expiredDate = (Date) this.expiredDate.getValue();
                    customData.setExpired_date(JDate.valueOf(expiredDate));
                } else if (expiredDateType.equals(NEVER)) {
                    customData.setExpired_date(null);
                } else if (expiredDateType.equals(ALWAYS)) {
                    customData.setExpired_date(JDate.getNow());
                }
            }

            return;
        }

        private void initProps() {

            createProperties();
            setPropertiesDefaultEdition();

        }

        public JComponent getComponent() {
            ArrayList<Property> properties = new ArrayList<Property>();

            properties.add(this.isin);
            properties.add(this.rate);
            properties.add(this.expiredDateType);
            properties.add(this.expiredDate);
            properties.add(this.lastUpdate);

            PropertyTableModel<Property> SLRTableModel = new PropertyTableModel<Property>(properties) {

                private static final long serialVersionUID = 1L;

                @Override
                public String getColumnName(int i) {
                    if (i == 0) {
                        return "<html><b>StockLendingRates Properties</b></html>";
                    }
                    return "";
                }
            };
            SLRTableModel.setOrder(PropertyTableModel.UNSORTED);
            this.table = new PropertyTable(SLRTableModel);
            this.table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
            this.table.expandAll();
            PropertyTableUtilities.setupPropertyTableKeyActions(this.table);

            JScrollPane positionPanel = new JScrollPane();
            positionPanel.getViewport().add(this.table);
            positionPanel.getViewport().setBackground(this.table.getBackground());
            positionPanel.setBorder(BorderFactory.createLineBorder(Color.black));
            this.table.getTableHeader().setBackground(Color.black);
            this.table.getTableHeader().setForeground(Color.white);

            positionPanel.setPreferredSize(new Dimension(380, 120));
            positionPanel.setMaximumSize(new Dimension(280, 120));
            positionPanel.setMinimumSize(new Dimension(280, 120));

            return positionPanel;
        }

        public ValueErrorTypes checkValues() {

            ValueErrorTypes errorType = null;

            if (!isNoValues()) {
                if (!isValidRateValue()) {
                    errorType = ValueErrorTypes.NO_RATE_ERROR;
                } else if (!isValidExpiredDateTypeValue()) {
                    errorType = ValueErrorTypes.NO_EXP_DATE_TYPE_ERROR;
                } else if (!isValidExpiredDateValue()) {
                    errorType = ValueErrorTypes.NO_EXP_DATE_ERROR;
                }
            }

            return errorType;
        }

        public boolean isValidRateValue() {
            return this.rate.getValue() != null;
        }

        public boolean isValidExpiredDateTypeValue() {
            return this.expiredDateType.getValue() != null;
        }

        public boolean isValidExpiredDateValue() {
            return (this.expiredDateType.getValue().equals(NEVER) && (this.expiredDate.getValue() == null))
                    || (this.expiredDateType.getValue().equals(CUSTOM) && (this.expiredDate.getValue() != null) && (JDate
                    .diff(JDate.getNow(), JDate.valueOf((Date) this.expiredDate.getValue())) >= 0))
                    || (this.expiredDateType.getValue().equals(ALWAYS) && (this.expiredDate.getValue() != null));
        }

        public boolean isNoValues() {
            return (this.rate.getValue() == null) && (this.expiredDateType.getValue() == null)
                    && (this.expiredDate.getValue() == null);
        }

        @SuppressWarnings("deprecation")
        public void createProperties() {
            // isin
            this.isin = PropertyFactory.makeStringProperty(ISIN, ISIN, ISIN);
            // rate
            this.rate = PropertyFactory.makeNumberProperty(RATE, RATE, RATE, 2);
            // expiredDateType
            List<String> expiredDateTypeList = new ArrayList<String>();
            expiredDateTypeList.add(ALWAYS);
            expiredDateTypeList.add(NEVER);
            expiredDateTypeList.add(CUSTOM);
            this.expiredDateType = PropertyFactory.makeEnumProperty(EXPIRED_DATE_TYPE, EXPIRED_DATE_TYPE,
                    EXPIRED_DATE_TYPE, expiredDateTypeList);
            this.expiredDateType.addPropertyChangeListener(new PropertyChangeAction()); // publisher
            // expiredDate
            this.expiredDate = PropertyFactory.makeDateProperty(EXPIRED_DATE, EXPIRED_DATE, EXPIRED_DATE, null);
            // lastUpdate
            this.lastUpdate = PropertyFactory.makeDateProperty(LAST_UPDATE, LAST_UPDATE, LAST_UPDATE, null);
        }

        public void setPropertiesDefaultEdition() {

            this.isin.setEditable(false);
            this.rate.setEditable(true);
            this.expiredDateType.setEditable(true);
            this.expiredDate.setEditable(true);
            this.lastUpdate.setEditable(false);
        }

    }

    public void showValuesErrorMessage(ValueErrorTypes error) {
        JOptionPane.showMessageDialog(null, error.getMessage(), ERROR_WINDOW_NAME, JOptionPane.ERROR_MESSAGE);

    }

}
