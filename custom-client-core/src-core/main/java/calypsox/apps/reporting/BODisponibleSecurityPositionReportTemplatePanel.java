package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.inventory.InventoryTypeModel;
import com.calypso.apps.reporting.inventory.InventoryTypePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.inventoryposition.InventoryTypeSelection;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author acd
 */
public class BODisponibleSecurityPositionReportTemplatePanel extends BOSecurityPositionReportTemplatePanel implements ActionListener{
    ReportTemplate template;
    CalypsoCheckBox calculateRealPositionCheck;
    CalypsoCheckBox includeBloqAccounts;
    CalypsoCheckBox excludeBloqAccounts;
    CalypsoCheckBox mergeByCentro;
    public static final String CALCULATE_REAL_POSITION = "CalculateRealPosition";
    public static final String ONLY_BLOQ_ACCOUNTS = "OnlyBloqAccounts";
    public static final String EXCLUDE_BLOQ_ACCOUNTS = "ExcludeBloqAccounts";
    public static final String MERGE_BY_CENTER = "MergeByCenter";
    public BODisponibleSecurityPositionReportTemplatePanel() {

        this.calculateRealPositionCheck = new CalypsoCheckBox("Calculate Real Position");
        this.add(this.calculateRealPositionCheck);
        this.calculateRealPositionCheck.setBounds(1020, 194, 168, 24);
        this.calculateRealPositionCheck.setToolTipText("Calculate Real Position");
        this.calculateRealPositionCheck.addActionListener(this);

        this.includeBloqAccounts = new CalypsoCheckBox("Only Bloq. Accounts");
        this.add(this.includeBloqAccounts);
        this.includeBloqAccounts.setBounds(1210, 194, 168, 24);
        this.includeBloqAccounts.setToolTipText("Load Only Accounts with Bloqueo/Pignoracion true");
        this.includeBloqAccounts.addActionListener(this);

        this.excludeBloqAccounts = new CalypsoCheckBox("Exclude Bloq. Accounts");
        this.add(this.excludeBloqAccounts);
        this.excludeBloqAccounts.setBounds(1210, 170, 171, 24);
        this.excludeBloqAccounts.setToolTipText("Exclude Accounts with Bloqueo/Pignoracion true");
        this.excludeBloqAccounts.addActionListener(this);

        this.mergeByCentro = new CalypsoCheckBox("Merge By Centro");
        this.add(this.mergeByCentro);
        this.mergeByCentro.setBounds(1070, 170, 171, 24);
        this.mergeByCentro.setToolTipText("Merge By Centro Contable");
        this.mergeByCentro.addActionListener(this);
    }

    @Override
    public void setTemplate(ReportTemplate arg0) {
        super.setTemplate(arg0);
        this.template = arg0;
        if(null!=this.template){
            String s = (String) this.template.get(CALCULATE_REAL_POSITION);
            if (Util.isTrue(s, false)) {
                this.calculateRealPositionCheck.setSelected(true);
            }
            s = (String) this.template.get(ONLY_BLOQ_ACCOUNTS);
            if (Util.isTrue(s, false)) {
                this.includeBloqAccounts.setSelected(true);
                this.excludeBloqAccounts.setSelected(false);
            }
            s = (String) this.template.get(EXCLUDE_BLOQ_ACCOUNTS);
            if (Util.isTrue(s, false)) {
                this.excludeBloqAccounts.setSelected(true);
                this.includeBloqAccounts.setSelected(false);
            }
            s = (String) this.template.get(MERGE_BY_CENTER);
            if (Util.isTrue(s, false)) {
               this.mergeByCentro.setSelected(true);
            }
        }
    }

    @Override
    public ReportTemplate getTemplate() {
        this.template = super.getTemplate();
        if(null!=this.template){
            this.template.put(CALCULATE_REAL_POSITION, this.calculateRealPositionCheck.isSelected() && isValidTemplateConfig() ? "true" : "false");
            this.template.put(ONLY_BLOQ_ACCOUNTS, this.includeBloqAccounts.isSelected() ? "true" : "false");
            this.template.put(EXCLUDE_BLOQ_ACCOUNTS, this.excludeBloqAccounts.isSelected() ? "true" : "false");
            this.template.put(MERGE_BY_CENTER, this.mergeByCentro.isSelected() ? "true" : "false");
        }
        return this.template;
    }

    private boolean isValidTemplateConfig() {
        ReportPanel reportPanel = getReportWindow().getReportPanel();
        if(reportPanel != null) {
            ReportTemplate reportTemplate = Optional.ofNullable(reportPanel.getTemplate()).orElse(new ReportTemplate());
            String aggregation = reportTemplate.get("AGGREGATION").toString();
            InventoryTypeSelection inventoryTypeSelection = getInvSelectionType();

            boolean containsActualAndTheoretical = inventoryTypeSelection.getInventoryType().stream().anyMatch(inventoryType -> inventoryType.getPositionType().equalsIgnoreCase("THEORETICAL")) &&
                    inventoryTypeSelection.getInventoryType().stream().anyMatch(inventoryType -> inventoryType.getPositionType().equalsIgnoreCase("ACTUAL"));

            if(containsActualAndTheoretical && !Util.isEmpty(aggregation)
                    && aggregation.contains("Agent") && aggregation.contains("Account")) {
                return true;
            }

            AppUtil.displayError(reportPanel, " Aggregation must include at least Agent and Account\n " +
                    "Inventory Type Selection must include Actual and Theoretical types.");
            this.calculateRealPositionCheck.setSelected(false);
        }
        return false;
    }

    private boolean isValidAggregation() {
        ReportPanel reportPanel = getReportWindow().getReportPanel();
        if(reportPanel != null) {
            ReportTemplate reportTemplate = Optional.ofNullable(reportPanel.getTemplate()).orElse(new ReportTemplate());
            String aggregation = reportTemplate.get("AGGREGATION").toString();
            if(aggregation.contains("Book")){
                return true;
            }
            AppUtil.displayError(reportPanel, " Aggregation must include Book\n");
            this.mergeByCentro.setSelected(false);
        }
        return false;
    }

    private InventoryTypeSelection getInvSelectionType() {
        ReportPanel reportPanel = getReportWindow().getReportPanel();

        if(reportPanel == null) {
            return new InventoryTypeSelection();
        }

        Component[] components = reportPanel.getTemplatePanel().getComponents();

        return Arrays.stream(components)
                .filter(InventoryTypePanel.class::isInstance)
                .map(InventoryTypePanel.class::cast)
                .map(InventoryTypePanel::getModel)
                .map(InventoryTypeModel::getConfiguration)
                .findFirst()
                .orElse(new InventoryTypeSelection());
    }

    /**
     * Invoked when an action occurs.
     *
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        Object object = event.getSource();
        if (!(object instanceof CalypsoCheckBox)) {
            return;
        }

        CalypsoCheckBox checkBox = (CalypsoCheckBox) object;

        if(checkBox == this.includeBloqAccounts && this.template != null) {
            if(checkBox.isSelected()){
                this.template.put(ONLY_BLOQ_ACCOUNTS, "true");
                this.template.put(EXCLUDE_BLOQ_ACCOUNTS, "false");
                this.excludeBloqAccounts.setSelected(false);
            }
            if(!checkBox.isSelected()){
                this.template.put(ONLY_BLOQ_ACCOUNTS, "false");
                this.includeBloqAccounts.setSelected(false);
            }
        }

        if(checkBox == this.excludeBloqAccounts && this.template != null) {
            if(checkBox.isSelected() ){
                this.template.put(EXCLUDE_BLOQ_ACCOUNTS, "true");
                this.template.put(ONLY_BLOQ_ACCOUNTS, "false");
                this.includeBloqAccounts.setSelected(false);
            }
            if(!checkBox.isSelected()){
                this.template.put(EXCLUDE_BLOQ_ACCOUNTS, "false");
                this.excludeBloqAccounts.setSelected(false);
            }
        }

        if(checkBox == this.mergeByCentro && this.template != null) {
            if(checkBox.isSelected() && isValidAggregation()){
                this.template.put(MERGE_BY_CENTER, "true");
            }
            if(!checkBox.isSelected()){
                this.template.put(MERGE_BY_CENTER, "false");
                this.mergeByCentro.setSelected(false);
            }
        }

        if (checkBox == this.calculateRealPositionCheck) {
            if (!checkBox.isSelected() || !isValidTemplateConfig()) {
                checkBox.setSelected(false);
            }
            if (this.template != null) {
                this.template.put(CALCULATE_REAL_POSITION, checkBox.isSelected() ? "true" : "false");
            }
        }
    }

}
