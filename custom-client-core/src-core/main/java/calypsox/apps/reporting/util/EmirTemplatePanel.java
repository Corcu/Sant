package calypsox.apps.reporting.util;

import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.apps.reporting.util.control.SantLegalEntityPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.EMIRValuationMCReportTemplate;
import calypsox.util.SantDomainValuesUtil;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EmirTemplatePanel extends SantGenericReportTemplatePanel {

    protected SantProcessDatePanel processDatePanel;
    protected SantComboBoxPanel<Integer, String> submitterReportPanel;
    protected SantLegalEntityPanel replaceOwnerPanel;
    protected SantChooseButtonPanel contractTypePanel;
    protected SantLegalEntityPanel groupingReportPanel;

    /**
     * Serial Version ID
     */
    private static final long serialVersionUID = 1L;

    private static final String PROCESS_DATE = "Process Date";
    private static final String PROCESSING_ORG_OWNER = "Processing Org (Owner)";
    private static final String GROUPING_REPORT = "Grouping Submitter Report";
    private static final String MARGIN_CALL_CONTRACTS = "Margin Call Contracts";
    private static final String CONTRACT_TYPE = "Contract Type";
    private static final String SUBMITTER_REPORT = "Submitter Report";
    private static final String REPLACE_OWNER = "Replace Owner";
    private static final String DTCC = "DTCC";
    private static final String LEI = "LEI";

    /**
     * Constructor
     */
    public EmirTemplatePanel() {
    }

    @Override
    protected void init() {
        buildControlsPanel();

        setSize(getPanelSize());

        final JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BorderLayout());
        masterPanel.setBorder(getMasterPanelBorder());
        add(masterPanel);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(1, 1));

        masterPanel.add(mainPanel, BorderLayout.CENTER);

        final JPanel column1Panel = getColumn1Panel();
        final JPanel column2Panel = getColumn2Panel();

        mainPanel.add(column1Panel);
        mainPanel.add(column2Panel);
    }

    @Override
    protected void buildControlsPanel() {
        super.buildControlsPanel();
        this.processDatePanel = new SantProcessDatePanel(PROCESS_DATE);
        this.poAgrPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, PROCESSING_ORG_OWNER, false, true, true,
                true);
        this.agreementNamePanel = new SantChooseButtonPanel(MARGIN_CALL_CONTRACTS,
                getSortedMap(this.marginCallContractIdsMap).values());
        this.contractTypePanel = new SantChooseButtonPanel(CONTRACT_TYPE, SantDomainValuesUtil.LEGAL_AGGR_TYPE);
        this.submitterReportPanel = new SantComboBoxPanel<Integer, String>(SUBMITTER_REPORT, getSubmitterOptions());
        this.replaceOwnerPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, REPLACE_OWNER, false, true,
                false, true);
        this.groupingReportPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, GROUPING_REPORT, false, true,
                true, true);
    }

    @Override
    public void setValDatetime(JDatetime valDatetime) {
        this.processDatePanel.setValDatetime(valDatetime);
    }

    protected JPanel getColumn1Panel() {
        final JPanel column = new JPanel();
        column.setLayout(new GridLayout(4, 1, 5, 5));
        column.add(this.processDatePanel);
        column.add(this.poAgrPanel);
        column.add(this.groupingReportPanel);
        return column;
    }

    @Override
    protected JPanel getColumn2Panel() {
        final JPanel column = new JPanel();
        column.setLayout(new GridLayout(4, 1, 5, 5));
        column.add(this.agreementNamePanel);
        column.add(this.contractTypePanel);
        column.add(this.replaceOwnerPanel);
        column.add(this.submitterReportPanel);
        return column;
    }

    @Override
    public ReportTemplate getTemplate() {
        this.reportTemplate = super.getTemplate();
        this.processDatePanel.read(this.reportTemplate);
        this.reportTemplate.put(EMIRValuationMCReportTemplate.CONTRACT_TYPE, this.contractTypePanel.getValue());
        this.reportTemplate.put(EMIRValuationMCReportTemplate.SUBMITTER_REPORT, this.submitterReportPanel.getValue());
        if (!Util.isEmpty(this.groupingReportPanel.getLE())) {
            this.reportTemplate.put(EMIRValuationMCReportTemplate.GROUPING_REPORT_IDS, this.groupingReportPanel.getLEIdsStr());
            this.reportTemplate.put(EMIRValuationMCReportTemplate.GROUPING_REPORT_NAMES, this.groupingReportPanel.getLE());
        } else {
            this.reportTemplate.remove(EMIRValuationMCReportTemplate.GROUPING_REPORT_IDS);
            this.reportTemplate.remove(EMIRValuationMCReportTemplate.GROUPING_REPORT_NAMES);
        }
        if (!Util.isEmpty(this.replaceOwnerPanel.getLE())) {
            this.reportTemplate.put(EMIRValuationMCReportTemplate.REPLACE_OWNER_ID, this.replaceOwnerPanel.getLEIdsStr());
            this.reportTemplate.put(EMIRValuationMCReportTemplate.REPLACE_OWNER_NAME, this.replaceOwnerPanel.getLE());
        } else {
            this.reportTemplate.remove(EMIRValuationMCReportTemplate.REPLACE_OWNER_ID);
            this.reportTemplate.remove(EMIRValuationMCReportTemplate.REPLACE_OWNER_NAME);
        }
        return this.reportTemplate;
    }

    @Override
    public void setTemplate(ReportTemplate template) {
        super.setTemplate(template);
        this.processDatePanel.setTemplate(template);
        this.processDatePanel.write(template);
        this.contractTypePanel.setValue(this.reportTemplate, EMIRValuationMCReportTemplate.CONTRACT_TYPE);
        this.submitterReportPanel.setValue(this.reportTemplate, EMIRValuationMCReportTemplate.SUBMITTER_REPORT);
        this.groupingReportPanel.setValue(this.reportTemplate, EMIRValuationMCReportTemplate.GROUPING_REPORT_IDS);
        this.replaceOwnerPanel.setValue(this.reportTemplate, EMIRValuationMCReportTemplate.REPLACE_OWNER_ID);
    }

    private List<String> getSubmitterOptions() {
        List<String> submitterOptions = new ArrayList<String>();
        submitterOptions.add(DTCC);
        submitterOptions.add(LEI);
        return submitterOptions;
    }

    @Override
    protected Border getMasterPanelBorder() {
        return BorderFactory.createLineBorder(Color.GRAY);
    }

}
