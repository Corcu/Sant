package calypsox.apps.refdata;

import calypsox.tk.refdata.CustomLeveragePercentage;
import calypsox.tk.refdata.service.RemoteCustomLeverageService;
import com.calypso.apps.util.AppUtil;
import com.calypso.executesql.support.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.ui.component.table.celleditor.ProductCellEditor;
import com.calypso.ui.image.ImageUtilities;
import com.jidesoft.converter.ConverterContext;
import com.jidesoft.grid.*;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.swing.JideButton;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.jidesoft.grid.EditorContext.DEFAULT_CONTEXT;

/**
 * calypsox.apps.refdata.CustomIALeverageWindow
 *
 * @author x865229
 * date 02/01/2023
 */
public class CustomIALeverageWindow extends JFrame {
    private static final long serialVersionUID = 5544678307624439062L;

    public static final String ACCESS_FUNCTION = "AddModifyCustomIALeverage";

    private JPanel leverageActionPanel = null;
    private JideButton addLeverageButton = null;
    private JideButton removeLeverageButton = null;

    JPanel gridPanel;
    JPanel buttonPanel;

    TreeTable treeTable;
    JButton saveButton;
    JButton closeButton;

    LeverageModel model = new LeverageModel();


    public static final EditorContext productEditorContext = new EditorContext("ProductEditorContext");

    public CustomIALeverageWindow() throws HeadlessException {
        initUI();
        initData();
    }

    private void initData() {
        CellEditorManager.registerEditor(Equity.class, () -> {
            ProductCellEditor pce = new ProductCellEditor(this, Collections.singletonList("Equity"));
            pce.getTextField().setEditable(false);
            return pce;
        }, productEditorContext);

        loadLeverage();
    }

    private void initUI() {
        setTitle("Custom IA Leverage");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createLeverageActionPanel(), BorderLayout.NORTH);

        gridPanel = new JPanel();
        gridPanel.setLayout(new BorderLayout());
        add(gridPanel, BorderLayout.CENTER);

        treeTable = new TreeTable();
        treeTable.setShowTreeLines(false);
        treeTable.setShowLeafNodeTreeLines(false);
        treeTable.setModel(model);

        JScrollPane r = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        r.setViewportView(treeTable);
        r.getViewport().setBackground(treeTable.getBackground());
        r.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIDefaultsLookup.getColor("controlShadow")),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        gridPanel.add(r, BorderLayout.CENTER);

        add(createTheButtonPanel(), BorderLayout.SOUTH);

        setSize(500, 500);
        setLocation(((int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth()) - getWidth()) / 2,
                ((int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getHeight()) - getHeight()) / 2);
    }


    private JPanel createLeverageActionPanel() {
        if (leverageActionPanel == null) {
            leverageActionPanel = new JPanel(new BorderLayout());
            leverageActionPanel.add(new JPanel(), BorderLayout.CENTER);
            JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            westPanel.add(createAddLeverageButton());
            westPanel.add(createRemoveLeverageButton());
            leverageActionPanel.add(westPanel, BorderLayout.WEST);
        }
        return leverageActionPanel;
    }

    private JideButton createAddLeverageButton() {
        if (addLeverageButton == null) {
            addLeverageButton = new JideButton();
            addLeverageButton.setIcon(ImageUtilities.getIcon("com/calypso/icons/collateral/add_16.png"));
            addLeverageButton.setFocusable(false);
            addLeverageButton.setToolTipText("Add Product/Legal Entity % Leverage");
            addLeverageButton.setButtonStyle(2);
            addLeverageButton.addActionListener((e) -> addLeverage());
        }
        return addLeverageButton;
    }


    private JideButton createRemoveLeverageButton() {
        if (removeLeverageButton == null) {
            removeLeverageButton = new JideButton();
            removeLeverageButton.setIcon(ImageUtilities.getIcon("com/calypso/icons/collateral/delete_16.png"));
            removeLeverageButton.setFocusable(false);
            removeLeverageButton.setToolTipText("Remove Leverage");
            removeLeverageButton.setButtonStyle(2);
            removeLeverageButton.addActionListener((e) -> removeLeverage());
        }
        return removeLeverageButton;
    }

    private JPanel createTheButtonPanel() {
        if (buttonPanel == null) {
            buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

            saveButton = new JButton("Save");
            saveButton.addActionListener((e) -> saveLeverage());
            buttonPanel.add(saveButton);

            closeButton = new JButton("Close");
            closeButton.addActionListener((e) -> closeWindow());
            buttonPanel.add(closeButton);
        }
        return buttonPanel;
    }


    private void addLeverage() {
        model.addRow(new LeverageDataRow());
    }

    private void removeLeverage() {
        int[] selected = treeTable.getSelectedRows();
        if (selected != null) {
            for (int i : selected) {
                model.removeRow(i);
            }
        }
    }

    private void loadLeverage() {
        RemoteCustomLeverageService service = DSConnection.getDefault().getService(RemoteCustomLeverageService.class);
        try {
            CustomLeveragePercentage leveragePercentage = service.loadAll();
            showLeverage(leveragePercentage);
        } catch (RemoteException e) {
            Log.error(this, e);
            AppUtil.displayError(this, e.getLocalizedMessage());
        }
    }


    private void saveLeverage() {
        if (!AccessUtil.isAuthorized(ACCESS_FUNCTION)) {
            AppUtil.displayError(this, "User is not authorized to Add/Modify Custom IA Leverage.");
            return;
        }

        if (!validateLeverage()) {
            AppUtil.displayError(this, "Rows with empty values. Check the errors and save again.");
            return;
        }

        if (!checkLegalEntities()) {
            AppUtil.displayError(this, "Settings not saved. Check the errors and save again.");
            return;
        }
        CustomLeveragePercentage built = buildLeverage();

        RemoteCustomLeverageService service = DSConnection.getDefault().getService(RemoteCustomLeverageService.class);
        try {
            service.save(built);
        } catch (RemoteException e) {
            Log.error(this, e);
            AppUtil.displayError(this, "Error saving Custom Leverage.\n" + e.getLocalizedMessage());
        }
    }

    private boolean checkLegalEntities() {
        StringBuilder err = new StringBuilder();
        for (LeverageDataRow r : model.getRows()) {
            if (Util.isEmpty(r.getLegalEntityCode())) {
                err.append("Empty legal entity code for the product ").append(r.getProduct())
                        .append(" and leverage ").append(r.getPercentage()).append("\n");
            } else {
                if (!CustomLeveragePercentage.CustomLeveragePercentageItem.ALL_CODE.equals(r.getLegalEntityCode())) {
                    LegalEntity e = BOCache.getLegalEntity(DSConnection.getDefault(), r.getLegalEntityCode());
                    if (e == null) {
                        err.append("The Legal Entity with code ").append(r.getLegalEntityCode())
                                .append(" does not exist in the system. Product ")
                                .append(r.getProduct()).append(" leverage ")
                                .append(r.getPercentage()).append("\n");
                    }
                }
            }
        }
        if (!Util.isEmpty(err.toString())) {
            AppUtil.displayError(this, err.toString());
        }
        return Util.isEmpty(err.toString());
    }

    private void closeWindow() {
        setVisible(false);
    }

    private void showLeverage(CustomLeveragePercentage leveragePercentage) {
        for (int i = 0; i < model.getRowCount(); i++) {
            model.removeRow(i);
        }

        if (leveragePercentage != null && leveragePercentage.getItems() != null) {
            leveragePercentage.getItems().stream()
                    .sorted(Comparator.comparingInt(CustomLeveragePercentage.CustomLeveragePercentageItem::getProductId))
                    .forEach(item -> {
                        LeverageDataRow row = new LeverageDataRow(
                                BOCache.getExchangedTradedProduct(DSConnection.getDefault(), item.getProductId()),
                                getLegalEntityCode(item.getLegalEntityId()),
                                item.getPercentage());
                        model.addRow(row);
                    });
        }
    }

    private String getLegalEntityCode(int id) {
        if (id == CustomLeveragePercentage.CustomLeveragePercentageItem.ALL_ID) {
            return CustomLeveragePercentage.CustomLeveragePercentageItem.ALL_CODE;
        }
        LegalEntity e = BOCache.getLegalEntity(DSConnection.getDefault(), id);
        return e != null ? e.getCode() : "";
    }


    private boolean validateLeverage() {
        return model.getRows().stream()
                .noneMatch(r -> r.getProduct() == null || Util.isEmpty(r.getLegalEntityCode()) || r.getPercentage() == null);
    }

    private CustomLeveragePercentage buildLeverage() {
        CustomLeveragePercentage result = new CustomLeveragePercentage();
        model.getRows().stream()
                .sorted(Comparator.comparingInt(r -> r.getProduct().getId()))
                .forEach(r -> result.addPercentage(new CustomLeveragePercentage.CustomLeveragePercentageItem(
                        r.getProduct().getId(),
                        getLegalEntityId(r.getLegalEntityCode()),
                        r.getPercentage()
                )));
        return result;
    }

    private int getLegalEntityId(String code){
        if(!Util.isEmpty(code)){
            if(CustomLeveragePercentage.CustomLeveragePercentageItem.ALL_CODE.equals(code)){
                return CustomLeveragePercentage.CustomLeveragePercentageItem.ALL_ID;
            }else{
                LegalEntity e = BOCache.getLegalEntity(DSConnection.getDefault(), code);
                return e != null ? e.getId() : CustomLeveragePercentage.CustomLeveragePercentageItem.ERROR_ID;
            }
        }
        return CustomLeveragePercentage.CustomLeveragePercentageItem.ERROR_ID;
    }

    static class LeverageModel extends TreeTableModel<LeverageDataRow> {

        private static final long serialVersionUID = 8179172565438912582L;

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Product";
                case 1:
                    return "Legal Entity";
                case 2:
                    return "% Leverage";
            }
            return null;
        }
    }

    protected static class LeverageDataRow extends AbstractExpandableRow {

        com.calypso.tk.core.Product product;
        String legalEntityCode;
        Double percentage;

        public LeverageDataRow() {
        }

        public LeverageDataRow(Product product, String legalEntityCode, Double percentage) {
            this.product = product;
            this.legalEntityCode = legalEntityCode;
            this.percentage = percentage;
        }

        @Override
        public boolean isCellEditable(int i) {
            return true;
        }

        @Override
        public Object getValueAt(int i) {
            switch (i) {
                case 0:
                    if (product != null)
                        return product;
                case 1:
                    if (legalEntityCode != null)
                        return legalEntityCode;
                case 2:
                    return percentage;

            }
            return null;
        }

        @Override
        public void setValueAt(Object o, int i) {
            switch (i) {
                case 0:
                    product = o instanceof com.calypso.tk.core.Product ? (com.calypso.tk.core.Product) o : null;
                    break;
                case 1:
                    legalEntityCode = (String) o;
                    break;
                case 2:
                    percentage = (Double) o;
            }
        }

        @Override
        public ConverterContext getConverterContextAt(int i) {
            return ConverterContext.DEFAULT_CONTEXT;
        }

        @Override
        public EditorContext getEditorContextAt(int i) {
            if (i == 0) {
                return CustomIALeverageWindow.productEditorContext;
            }
            return DEFAULT_CONTEXT;
        }

        @Override
        public Class<?> getCellClassAt(int i) {
            switch (i) {
                case 0:
                    return Equity.class;
                case 1:
                    return String.class;
                case 2:
                    return Double.class;
            }
            return null;
        }

        @Override
        public List<?> getChildren() {
            return Collections.EMPTY_LIST;
        }

        @Override
        public void setChildren(List<?> list) {

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LeverageDataRow that = (LeverageDataRow) o;
            return Objects.equals(product, that.product) && Objects.equals(legalEntityCode, that.legalEntityCode) && Objects.equals(percentage, that.percentage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(product, legalEntityCode, percentage);
        }

        public Product getProduct() {
            return product;
        }

        public String getLegalEntityCode() {
            return legalEntityCode;
        }

        public Double getPercentage() {
            return percentage;
        }
    }

}
