package calypsox.apps.reporting;

import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.SACCRCMPositionReport;
import calypsox.tk.report.SACCRCMPositionReportTemplate;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;


public class SACCRInventoryPositionReportTemplatePanel extends SantCollateralBOPositionReportTemplatePanel {

    /*
     * Inventory postion type constants
     */
    public static final String ACTUAL = "ACTUAL";
    public static final String THEORETICAL = "THEORETICAL";

    /**
     * Variables
     */
    private SantComboBoxPanel<String, String> cashSecurityChoice;
    private SantProcessDatePanel processDatePanel;
    private SantComboBoxPanel<String, String> inventoryTypes;
    private SantComboBoxPanel<String, String> maturityOffSet;
    private SantComboBoxPanel<String, String> sdfFilterChoice;
    private SantComboBoxPanel<String, String> filterGroupChoice;

    @Override
    protected Border getMasterPanelBorder() {
        final TitledBorder titledBorder = BorderFactory.createTitledBorder("SA-CCR Inventory Position Balance");
        titledBorder.setTitleColor(Color.BLUE);
        return titledBorder;
    }

    @Override
    protected Dimension getPanelSize() {
        return new Dimension(0, 120);
    }

    /**
     * Process date of the process
     */
    @Override
    protected Component getNorthPanel() {
        this.processDatePanel = new SantProcessDatePanel("");
        this.processDatePanel.setPreferredSize(new Dimension(120, 30), new Dimension(240, 30));
        this.processDatePanel.setPanelLabelName("Process Date");
        return this.processDatePanel;
    }

    // First column, they contain: MoventType, Inventory filter, SD filter,
    // inventory type, inventory date.
    @Override
    protected JPanel getColumn1Panel() {
        final JPanel column1Panel = new JPanel();
        column1Panel.removeAll();

        GridLayout gl = new GridLayout(6, 2);
        column1Panel.setLayout(gl);

        column1Panel.add(this.cashSecurityChoice);
        column1Panel.add(this.inventoryTypes);
        column1Panel.add(this.sdfFilterChoice);
        column1Panel.add(this.filterGroupChoice);
        return column1Panel;
    }

    /**
     * Second column, contains: agreementType, agreementName, POs, position &
     * Cash/Security.
     *
     * @see calypsox.apps.reporting.util.SantGenericReportTemplatePanel#getColumn2Panel()
     */
    @Override
    protected JPanel getColumn2Panel() {
        final JPanel column2Panel = new JPanel();
        column2Panel.removeAll();

        GridLayout gl = new GridLayout(6, 2);
        column2Panel.setLayout(gl);

        column2Panel.add(poAgrPanel);
        column2Panel.add(cptyPanel);
        column2Panel.add(agreementNamePanel);
        column2Panel.add(super.agreementTypePanel);
        column2Panel.add(this.maturityOffSet);

        return column2Panel;
    }

    @Override
    protected void buildControlsPanel() {
        super.buildControlsPanel();

        super.poAgrPanel.setPreferredSize(new Dimension(240, 24));
        super.poAgrPanel.setBounds(10, 50, 280, 24);

        super.cptyPanel.setPreferredSize(new Dimension(240, 24));
        super.cptyPanel.setBounds(10, 50, 280, 24);

        super.agreementNamePanel.setPreferredSize(new Dimension(240, 24));
        super.agreementNamePanel.setBounds(10, 50, 280, 24);

        super.agreementTypePanel.setPreferredSize(new Dimension(240, 24));
        super.agreementTypePanel.setBounds(10, 50, 280, 24);

        this.maturityOffSet = new SantComboBoxPanel<String, String>("Maturity Off Set	", getmaturityOffSet());
        this.maturityOffSet.setEditable(false);
        this.maturityOffSet.setBounds(10, 50, 280, 24);
        this.maturityOffSet.setPreferredSize(new Dimension(240, 24));

        this.cashSecurityChoice = new SantComboBoxPanel<String, String>("Cash/Sec", getcashSecurity());
        this.cashSecurityChoice.setEditable(false);
        this.cashSecurityChoice.setBounds(10, 50, 280, 24);
        this.cashSecurityChoice.setPreferredSize(new Dimension(240, 24));

        this.inventoryTypes = new SantComboBoxPanel<String, String>("Inventory Type", getPositionTypes());
        this.inventoryTypes.setBounds(10, 50, 280, 24);
        this.inventoryTypes.setPreferredSize(new Dimension(240, 24));
        this.inventoryTypes.setEditable(false);

        this.sdfFilterChoice = new SantComboBoxPanel<String, String>("SDF Filter", getEmpty());
        this.sdfFilterChoice.setBounds(10, 50, 280, 24);
        this.sdfFilterChoice.setPreferredSize(new Dimension(240, 24));
        this.sdfFilterChoice.setEditable(false);

        this.filterGroupChoice = new SantComboBoxPanel<String, String>("Contracts Filter", getContractsGropusFilters());
        this.filterGroupChoice.setBounds(10, 50, 280, 24);
        this.filterGroupChoice.setPreferredSize(new Dimension(240, 24));
        this.filterGroupChoice.setEditable(false);
    }

    // Hides all panels
    @Override
    public void hideAllPanels() {
        final boolean visibility = false;
        this.agreementStatusPanel.setVisible(visibility);
        this.cptyPanel.setVisible(visibility);
        this.economicSectorPanel.setVisible(visibility);
        this.isFundPanel.setVisible(visibility);
        this.headCloneIndicatorPanel.setVisible(visibility);
        this.instrumentTypePanel.setVisible(visibility);
        this.tradeIdPanel.setVisible(visibility);
        this.matureDealsPanel.setVisible(visibility);
        this.mtmZeroPanel.setVisible(visibility);
        this.poDealPanel.setVisible(visibility);
        this.tradeStatusPanel.setVisible(visibility);
        this.valuationPanel.setVisible(visibility);
        this.portfolioPanel.setVisible(visibility);
        this.baseCcyPanel.setVisible(visibility);
        this.lastAllocationCurrencyCheckBox.setVisible(visibility);
    }

    @Override
    public void setValDatetime(JDatetime valDatetime) {
        this.processDatePanel.setValDatetime(valDatetime);
    }

    /**
     * Recovers the values of the panel into the template
     */
    @Override
    public ReportTemplate getTemplate() {
        this.reportTemplate = super.getTemplate();
        this.processDatePanel.read(this.reportTemplate);

        this.reportTemplate.put(BOSecurityPositionReportTemplate.POSITION_TYPE, inventoryTypes.getValue());
        this.reportTemplate.put(BOSecurityPositionReportTemplate.CASH_SECURITY, cashSecurityChoice.getValue());
        this.reportTemplate.put(BOSecurityPositionReportTemplate.FILTER_ZERO, mtmZeroPanel.getValue());
        this.reportTemplate.put(BOSecurityPositionReportTemplate.FILTER_MATURED, matureDealsPanel.getValue());
        this.reportTemplate.put(BOSecurityPositionReportTemplate.SEC_FILTER, this.sdfFilterChoice.getValue());
        this.reportTemplate.put(BOSecurityPositionReportTemplate.CUSTOM_FILTER, filterGroupChoice.getValue());

        final String ownersNames = (String) this.reportTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS,
                poAgrPanel.getLE());
        if (!Util.isEmpty(ownersNames)) {
            this.reportTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS, ownersNames);
        }

        this.reportTemplate.put(SACCRCMPositionReportTemplate.MATURITY_OFFSET, this.maturityOffSet.getValue());
        this.reportTemplate.put(SantGenericTradeReportTemplate.AGREEMENT_TYPE, this.agreementTypePanel.getValue());

        return this.reportTemplate;
    }

    /**
     * Stores panel filter selections into template
     */
    @Override
    public void setTemplate(final ReportTemplate template) {
        super.setTemplate(template);

        this.inventoryTypes.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.POSITION_TYPE);
        this.cashSecurityChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.CASH_SECURITY);
        this.maturityOffSet.setValue(this.reportTemplate, SACCRCMPositionReportTemplate.MATURITY_OFFSET);
        this.agreementTypePanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        this.sdfFilterChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.SEC_FILTER);
        this.filterGroupChoice.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.CUSTOM_FILTER);

        this.processDatePanel.setTemplate(template);
        this.processDatePanel.write(template);
    }

    /*
     * Sets type position to be included
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Collection<String> getcashSecurity() {
        Vector v = new Vector();
        v.addElement("Both");
        v.addElement(BOPositionReport.CASH);
        v.addElement(BOPositionReport.SECURITY);
        return v;
    }

    /*
     * List maturity dates. Defaults ones plus optional configurations through
     * DV
     */
    private Collection<String> getmaturityOffSet() {
        Vector<String> v = new Vector<String>(Arrays.asList(new String[]{"1", "7", "15", "30"}));
        String domainName = SACCRCMPositionReport.class.getSimpleName() + "."
                + (SACCRCMPositionReport.CONFIGURATIONS.MATURITY_RANGE.getName());
        Map<String, String> decimalMap = CollateralUtilities.initDomainValueComments(domainName);
        if (decimalMap.containsKey(domainName)) {
            v.addAll(Util.string2Vector(decimalMap.get(domainName)));
        }
        return v;
    }

    /*
     * Inventory Types
     */
    private Collection<String> getPositionTypes() {
        ArrayList<String> InventoryType = new ArrayList<String>();
        InventoryType.add(0, "");
        InventoryType.add(ACTUAL);
        InventoryType.add(THEORETICAL);
        return InventoryType;
    }

    /*
     * List of filter groups from DV
     */
    private Collection<String> getContractsGropusFilters() {
        Vector<String> v = LocalCache.getDomainValues(DSConnection.getDefault(), "Collateral.Config.Group");
        v.add(0, "");
        return v;
    }

    /*
     * Empty String Collection
     */
    private Collection<String> getEmpty() {
        ArrayList<String> Empity = new ArrayList<String>();
        Empity.add("");
        return Empity;
    }

    // The panels to be shown by default
    private void setPanelVisibility() {
        hideAllPanels();
        this.cashSecurityChoice.setVisible(true);
        this.sdfFilterChoice.setVisible(true);
        // this.mtmZeroPanel.setVisible(true);
        // this.matureDealsPanel.setVisible(true);
        this.processDatePanel.setVisible(true);
        this.agreementNamePanel.setVisible(true);
        this.poAgrPanel.setVisible(true);
        this.cptyPanel.setVisible(true);
        this.filterGroupChoice.setVisible(true);
    }

    @Override
    protected void init() {
        buildControlsPanel();

        setSize(getPanelSize());

        final JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BorderLayout());
        masterPanel.setBorder(getMasterPanelBorder());
        add(masterPanel);

        masterPanel.add(getNorthPanel(), BorderLayout.NORTH);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 3));

        masterPanel.add(mainPanel, BorderLayout.CENTER);

        final JPanel column1Panel = getColumn1Panel();
        final JPanel column2Panel = getColumn2Panel();

        mainPanel.add(column1Panel);
        mainPanel.add(column2Panel);
    }
}
