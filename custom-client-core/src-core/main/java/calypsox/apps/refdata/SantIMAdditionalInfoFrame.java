/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.refdata;

import calypsox.tk.core.MarginCallConfigLight;
import calypsox.util.collateral.SantCollateralConfigUtil;
import calypsox.util.SantReportingUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoDialogInterface;
import com.calypso.apps.util.DomainWindowListener;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.CollateralCacheUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.ui.factory.PropertyTableFactory;
import com.calypso.ui.property.DynamicContext;
import com.calypso.ui.property.MarginCallConfigProperty;
import com.calypso.ui.property.MultipleSelectionListProperty;
import com.jidesoft.grid.DefaultProperty;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyPane;
import com.jidesoft.grid.PropertyTable;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.util.List;
import java.util.*;

public class SantIMAdditionalInfoFrame extends JDialog implements DomainWindowListener, CalypsoDialogInterface {
    private static final String IM_SUB_CONTRACTS = "IM_SUB_CONTRACTS";

    private static final String IM_CALCULATION_METHOD = "IM_CALCULATION_METHOD";

    private static final String IM_GLOBAL_ID = "IM_GLOBAL_ID";

    private static final String MTA_THRESHOLD_FIXING = "MTA_THRESHOLD_FIXING";
    private static final String MTA_EUR = "MTA_EUR";
    private static final String MTA_USD = "MTA_USD";
    private static final String THRESHOLD_EUR = "THRESHOLD_EUR";
    private static final String THRESLHOD_USD = "THRESLHOD_USD";

    private static final String TRUE = "TRUE";

    /**
     * serial version id
     */
    private static final long serialVersionUID = 1L;


    private static final String OK_BUTTON = "Ok";
    private static final String CANCEL_BUTTON = "Cancel";

    private PropertyPane detailsPropertyPane;
    private PropertyTable detailsPropertyTable;
    private DetailsPropertyChangeListener propertyChangeListener;

    private Map<String, String> values = new HashMap<String, String>();

    protected boolean fComponentsAdjusted;
    protected JButton okButton;
    protected JButton cancelButton;
    protected Frame frame;

    protected String action;
    protected CollateralConfig collateralConfig;
    protected CollateralConfig parentContractSelected = null;

    protected String buttonSelected;

    public SantIMAdditionalInfoFrame(final Frame frame, final CollateralConfig margincallconfig,
                                     final Map<String, String> imCalcMethodFields, final Vector<String> readOnlyKw) {
        super(frame);
        final StringBuffer title = new StringBuffer("Save IM Additional Info - ");
        title.append(margincallconfig);
        setTitle(title.toString());

        collateralConfig = margincallconfig;

        this.buttonSelected = null;

        this.frame = frame;

        this.fComponentsAdjusted = false;
        this.okButton = new JButton();

        this.cancelButton = new JButton();
        if (checkMtaThresholdFixing(margincallconfig)) {
            setSize(500, 380);
        } else {
            setSize(500, 280);
        }
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        try {
            init(imCalcMethodFields);
        } catch (final Exception e) {
            Log.error(this, e);
        }
    }

    private void init(final Map<String, String> imCalcMethodFields) throws Exception {
        setModal(true);
        getContentPane().setLayout(null);

        setVisible(false);

        this.okButton.setText(OK_BUTTON);
        this.okButton.setActionCommand(OK_BUTTON);
        this.okButton.setAlignmentY(0.5F);
        getContentPane().add(this.okButton);

        this.cancelButton.setText(CANCEL_BUTTON);
        this.cancelButton.setActionCommand(CANCEL_BUTTON);
        this.cancelButton.setAlignmentY(0.5F);
        getContentPane().add(this.cancelButton);

        add(getDetailsPropertyPane(imCalcMethodFields));

        if (checkMtaThresholdFixing(this.collateralConfig)) {
            this.okButton.setBounds(5, 310, 115, 25);
            this.cancelButton.setBounds(352, 310, 115, 25);
        } else {
            this.okButton.setBounds(5, 210, 115, 25);
            this.cancelButton.setBounds(352, 210, 115, 25);
        }


        final MenuAction menuAction = new MenuAction();
        this.okButton.addActionListener(menuAction);
        this.cancelButton.addActionListener(menuAction);
    }

    private PropertyPane getDetailsPropertyPane(Map<String, String> imCalcMethodFields) {
        if (this.detailsPropertyPane == null) {
            this.detailsPropertyPane = new PropertyPane(getDetailsPropertyTable(imCalcMethodFields));
            this.detailsPropertyPane.getToolBar().remove(2);
            this.detailsPropertyPane.getToolBar().remove(1);
            this.detailsPropertyPane.getToolBar().remove(0);
        }
        if (checkMtaThresholdFixing(this.collateralConfig)) {
            this.detailsPropertyPane.setSize(495, 300);
        } else {
            this.detailsPropertyPane.setSize(495, 200);
        }

        return this.detailsPropertyPane;
    }

    private PropertyTable getDetailsPropertyTable(Map<String, String> imCalcMethodFields) {
        if (this.detailsPropertyTable == null) {
            this.detailsPropertyTable = PropertyTableFactory
                    .makeCategorizedPropertyTable(getProperties(imCalcMethodFields));

            this.detailsPropertyTable.expandFirstLevel();
        }

        return this.detailsPropertyTable;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Property> getProperties(Map<String, String> imCalcMethodFields) {
        List properties = new ArrayList();
        Property property = null;

        for (Map.Entry<String, String> field : imCalcMethodFields.entrySet()) {
            String key = field.getKey();

            if (IM_GLOBAL_ID.equalsIgnoreCase(key)) {
                property = getGlobalIMContractListProperty(key, field.getValue());
                property.addPropertyChangeListener(getPropertyChangeListener());
            } else if (key.contains(IM_CALCULATION_METHOD)) {
                property = getProductListProperty(key, field.getValue());
                property.addPropertyChangeListener(getPropertyChangeListener());
            }

            //new MTA_THRESHOLD_FIXING
            if (MTA_EUR.equalsIgnoreCase(key)) {
                property = getFxFields(key, field.getValue());
                property.addPropertyChangeListener(getPropertyChangeListener());
            }
            if (MTA_USD.equalsIgnoreCase(key)) {
                property = getFxFields(key, field.getValue());
                property.addPropertyChangeListener(getPropertyChangeListener());
            }
            if (THRESHOLD_EUR.equalsIgnoreCase(key)) {
                property = getFxFields(key, field.getValue());
                property.addPropertyChangeListener(getPropertyChangeListener());
            }
            if (THRESLHOD_USD.equalsIgnoreCase(key)) {
                property = getFxFields(key, field.getValue());
                property.addPropertyChangeListener(getPropertyChangeListener());
            }

            if (property != null) {
                properties.add(property);
                property = null;
            }


        }

        return properties;
    }

    @SuppressWarnings("unchecked")
    private Property getProductListProperty(String fieldKey, String value) {
        Vector<String> values = null;
        if (!Util.isEmpty(value)) {
            String[] valuesSelected = value.split("; ");
            values = new Vector<String>(Arrays.asList(valuesSelected));
        }

        Vector<String> possibleUPIValues = new Vector<String>();
        Vector<String> finalUPIList = new Vector<String>();
        try {
            possibleUPIValues = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilterNames();

            if (!Util.isEmpty(possibleUPIValues)) {
                for (String filter : possibleUPIValues) {
                    if (filter.startsWith(IM_CALCULATION_METHOD)) {
                        finalUPIList.add(filter);
                    }
                }
            }

        } catch (CalypsoServiceException e) {
            Log.error(this, e);
        }


        MultipleSelectionListProperty<String> productListProperty = new MultipleSelectionListProperty<String>(fieldKey,
                finalUPIList);

        StringBuilder description = new StringBuilder("Specify the UPIs for calculation method");
        productListProperty.setDescription(description.toString());
        productListProperty.setCategory("Calculation Methods");
        productListProperty.setValue(values);

        productListProperty.setPossibleValues(finalUPIList);
        return productListProperty;
    }

    private MarginCallConfigProperty getGlobalIMContractListProperty(String fieldKey, String value) {
        StringBuilder description = new StringBuilder("Select the Facade Contract");

        MarginCallContractValueProvider contractProvider = new MarginCallContractValueProvider();
        MarginCallConfigProperty globalImContracts = new MarginCallConfigProperty(fieldKey, fieldKey,
                description.toString(), contractProvider, this.frame);
        globalImContracts.setCategory("Parent Contract");

        if (!Util.isEmpty(value)) {
            Object contractSelected = contractProvider.getSelectedValue(Integer.valueOf(value));

            if (contractSelected != null) {
                globalImContracts.setValue(contractSelected);
            }
        }

        return globalImContracts;
    }

    private DefaultProperty getFxFields(String fieldKey, String value) {
        StringBuilder description = new StringBuilder("Set Mta Threshold Fixing Values");
        Double decimalValue = getDouble(value);
        DefaultProperty property = new DefaultProperty();
        property.setName(fieldKey);
        property.setValue(value);
        property.setCategory("Mta Threshold Fixing");
        property.setDescription(description.toString());
//        property.setType(Double.class);
        return property;
    }

    private Double getDouble(String value) {
        Double decimalValue = null;
        try {
            if (!Util.isEmpty(value) && !value.equalsIgnoreCase("null")) {
                if (value.contains(",")) {
                    value = value.replace(",", ".");
                }
                decimalValue = Double.valueOf(value);
            }
        } catch (Exception e) {
            Log.error(this, "Cannot Cast value: " + value + " " + e);
        }
        return decimalValue;
    }

    @SuppressWarnings("deprecation")
    protected class MarginCallContractValueProvider extends DynamicContext.ValueProvider {
        private static final String CSA_FACADE_TYPE = "CSA_FACADE";
        private Object[] mccArray = null;

        private MarginCallContractValueProvider() {
            List<CollateralConfig> allFacadeContracts = new ArrayList<CollateralConfig>();
            try {
                Vector<MarginCallConfigLight> mccLights = SantReportingUtil
                        .getSantReportingService(DSConnection.getDefault()).getMarginCallConfigsLight();
                for (MarginCallConfigLight marginCallConfig : mccLights) {
                    String contractType = marginCallConfig.getContractType();

                    if (CSA_FACADE_TYPE.equalsIgnoreCase(contractType)) {
                        CollateralConfig contract = new CollateralConfig();
                        contract.setId(marginCallConfig.getId());
                        contract.setContractType(contractType);
                        contract.setName(marginCallConfig.getDescription());
                        allFacadeContracts.add(contract);
                    }
                }
            } catch (RemoteException e) {
                Log.error(this, "Couldn't get Margin Call Contracts: " + e.getMessage());
                Log.error(this, e); //sonar
            }

            this.mccArray = allFacadeContracts.toArray();
        }

        public Object[] getPossibleValues() {
            return this.mccArray;
        }

        public CollateralConfig getSelectedValue(int contractId) {
            List<Object> contracts = Arrays.asList(mccArray);

            for (Object contract : contracts) {
                CollateralConfig contractConfig = (CollateralConfig) contract;

                if (contractConfig.getId() == contractId) {
                    return contractConfig;
                }
            }

            return null;
        }

    }

    protected class MenuAction implements ActionListener {

        /**
         * Call specific method given the action applied
         */
        @Override
        public void actionPerformed(final ActionEvent actionevent) {
            final Object obj = actionevent.getSource();
            if (obj == SantIMAdditionalInfoFrame.this.okButton) {
                okButton_ActionPerformed();
            }
            if (obj == SantIMAdditionalInfoFrame.this.cancelButton) {
                cancelButton_ActionPerformed();
            }
        }

        protected final SantIMAdditionalInfoFrame this$0;

        MenuAction() {
            super();
            this.this$0 = SantIMAdditionalInfoFrame.this;
        }

    }

    /**
     * cancel button
     */
    void cancelButton_ActionPerformed() {
        this.buttonSelected = CANCEL_BUTTON;

        setVisible(false);
        dispose();
    }

    /**
     * ok button
     */
    void okButton_ActionPerformed() {
        this.buttonSelected = OK_BUTTON;

        // set child additional fields info
		/* OLD MIG V16
		(collateralConfig.getAdditionalFields()).putAll(values);
		*/
        SantCollateralConfigUtil.getAllAdditionalFields(collateralConfig).putAll(values);

        // set old and new parent additional info field
        modifyOldParentContract();
//		modifyNewParentContract();

        setVisible(false);
        dispose();
    }

    private void modifyOldParentContract() {
        // get CollateralConfig from DB to get old previous parent_id.
        // the new one is not saved yet
        MarginCallConfig mcc = CollateralCacheUtil.getMarginCallConfig(DSConnection.getDefault(),
                collateralConfig.getId());

        String oldParentId = "";

        if (mcc != null) {
            oldParentId = mcc.getAdditionalField(IM_GLOBAL_ID);
        }

        CollateralConfig oldParentContract = null;
        if (!Util.isEmpty(oldParentId)) {
            try {
                oldParentContract = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(Integer.valueOf(oldParentId));
            } catch (Exception e) {
                Log.error(this, "Couldn't get the contract: " + e.getMessage());
                Log.error(this, e); //sonar
            }
//			oldParentContract = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
//					Integer.valueOf(oldParentId));
        }

        if (oldParentContract != null) {
            String existingChildren = oldParentContract.getAdditionalField(IM_SUB_CONTRACTS);
            Map<String, String> newValue = new HashMap<>();
            String childId = String.valueOf(collateralConfig.getId());

            // if children not empty
            if (!Util.isEmpty(existingChildren)) {
                // if childId in existingChildren, we remove it
                if (existingChildren.contains(childId)) {
                    final String[] children = existingChildren.trim().split(",");
                    List<String> newChildren = new ArrayList<>();

                    for (final String child : children) {
                        if (!child.equalsIgnoreCase(childId)) {
                            newChildren.add(child);
                        }
                    }

                    newValue.put(IM_SUB_CONTRACTS, StringUtils.join(newChildren, ','));

                    // set additional field with new value and save the entry
                    (oldParentContract.getAdditionalFields()).putAll(newValue);
                    saveCollateralConfig(oldParentContract);
                }
                // if doesn't exist, do nothing
            }
        } else {
            // if there is no old parent, means we don't need to delete anything
        }
    }

//	private void modifyNewParentContract() {
//		if (parentContractSelected != null) {
//			String existingChildren = parentContractSelected.getAdditionalField(IM_SUB_CONTRACTS);
//			Map<String, String> newValue = new HashMap<String, String>();
//			String childId = String.valueOf(collateralConfig.getId());
//
//			// if children is not empty
//			if (!Util.isEmpty(existingChildren)) {
//				// if childId not in existingChildren, we add it
//				if (!childId.equals("0") && !existingChildren.contains(childId)) {
//					StringBuilder newChildren = new StringBuilder(existingChildren);
//					newChildren.append(',').append(childId);
//					newValue.put(IM_SUB_CONTRACTS, newChildren.toString());
//				}
//
//				// if already exists, do nothing
//			} else {
//				// if empty, we add the new child id
//				newValue.put(IM_SUB_CONTRACTS, String.valueOf(collateralConfig.getId()));
//			}
//
//			// add additional field with new value and save the contract
//			(parentContractSelected.getAdditionalFields()).putAll(newValue);
//			saveCollateralConfig(parentContractSelected);
//		} else {
//			// if there is no parent, means we don't need to do anything
//		}
//	}

    private void saveCollateralConfig(CollateralConfig contractToSave) {
        try {
            contractToSave = (CollateralConfig) DSConnection.getDefault().getRemoteReferenceData()
                    .applyNoSave(contractToSave);
            if (contractToSave.isValid(new ArrayList<String>())) {
                ServiceRegistry.getDefault().getCollateralDataServer().save(contractToSave);
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Couldn't apply action Update on old Parent Contract: " + e.getMessage());
            Log.error(this, e); //sonar
            AppUtil.displayError(this.frame, "Couldn't apply action Update on old Parent Contract", Log.ERROR);
        } catch (CollateralServiceException e) {
            Log.error(this, "Couldn't save old Parent Contract: " + e.getMessage());
            Log.error(this, e); //sonar
            AppUtil.displayError(this.frame, "Couldn't save old Parent Contract", Log.ERROR);
        }
    }

    private DetailsPropertyChangeListener getPropertyChangeListener() {
        if (propertyChangeListener == null) {
            propertyChangeListener = new DetailsPropertyChangeListener(this);
        }
        return propertyChangeListener;
    }

    private class DetailsPropertyChangeListener implements PropertyChangeListener {
        protected Component owner;

        private DetailsPropertyChangeListener(Component owner) {
            this.owner = owner;
        }

        @SuppressWarnings("unchecked")
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = ((Property) evt.getSource()).getName();

            if (IM_GLOBAL_ID.equalsIgnoreCase(propertyName)) {
                CollateralConfig newParent = (CollateralConfig) ((Property) evt.getSource()).getValue();

                if (newParent != null) {
                    parentContractSelected = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                            newParent.getId());

                    // insert new Global id on child
                    values.put(propertyName, String.valueOf(newParent.getId()));
                }
            } else if (propertyName.contains(IM_CALCULATION_METHOD)) {
                ArrayList<String> list = new ArrayList<String>((List<String>) ((Property) evt.getSource()).getValue());
                String propertyValue = StringUtils.join(list, "; ");

                if (propertyValue.length() > 255) {
                    AppUtil.displayError(this.owner, "Error",
                            new Throwable("The length should not exceed 255 characters."), "AppUtil");
                } else {
                    values.put(propertyName, propertyValue);
                }
            } else if (propertyName.contains(MTA_EUR) || propertyName.contains(MTA_USD)
                    || propertyName.contains(THRESHOLD_EUR)
                    || propertyName.contains(THRESLHOD_USD)) {
                String value = "";
                try {
                    value = String.valueOf(((Property) evt.getSource()).getValue());
                } catch (Exception e) {
                    Log.error(this, "Cannot cast to string: " + ((Property) evt.getSource()).getValue() + " " + e);
                }
                if (value.equalsIgnoreCase("null")) {
                    value = "";
                } else {
                    if (propertyName.toLowerCase().contains("eur")) {
                        value = CurrencyUtil.checkAmount(value, "EUR");
                    } else {
                        value = CurrencyUtil.checkAmount(value, "USD");
                    }
                }
                if (value.equalsIgnoreCase("0,00")
                        || value.equalsIgnoreCase("0.00")) {
                    value = "";
                }

                ((Property) evt.getSource()).setValue(value);
                values.put(propertyName, value);

            } else {
                String value = "";
                value = (String) ((Property) evt.getSource()).getValue();
                values.put(propertyName, value);
            }
        }
    }


    private boolean checkMtaThresholdFixing(CollateralConfig margincallconfig) {
        return (TRUE.equalsIgnoreCase(margincallconfig.getAdditionalField(MTA_THRESHOLD_FIXING)));
    }


    @Override
    public void domainSaved(String paramString) {
        // do nothing
    }

}
