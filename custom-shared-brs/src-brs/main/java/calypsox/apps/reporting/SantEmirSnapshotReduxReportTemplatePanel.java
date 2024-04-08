/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import calypsox.tk.report.SantEmirSnapshotReduxReportTemplate;
import calypsox.tk.util.emir.EmirSnapshotReportType;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TransferReportTemplate;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

//CAL_EMIR_007
public class SantEmirSnapshotReduxReportTemplatePanel extends
        ReportTemplatePanel {

    private JLabel reportTypesLabel = new JLabel();
    private JLabel tradeIdLabel = new JLabel();
    private JTextField tradeIdText = new JTextField();
    private CalypsoComboBox reporTypesComboBox = new CalypsoComboBox();
    private final JLabel jLabel2 = new JLabel();
    @SuppressWarnings("rawtypes")
    private JComboBox processingOrgChoice = new JComboBox();

    private ReportTemplate template;

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SantEmirSnapshotReduxReportTemplatePanel() {

        try {
            jInit();
        } catch (final Exception e) {
            Log.error(this, e);
        }
        initDomainsReportTypes();
        initDomains();
    }

    public JLabel getJNameLabel() {
        return this.reportTypesLabel;
    }

    @SuppressWarnings("rawtypes")
    public JComboBox getFilterNameComboBox() {
        return this.reporTypesComboBox;
    }

    public JLabel getJLabel2() {
        return this.jLabel2;
    }

    @SuppressWarnings("rawtypes")
    public JComboBox getProcessingOrgChoice() {
        return this.processingOrgChoice;
    }

    private void jInit() throws Exception {

        setLayout(null);
        setSize(new Dimension(500, 200));

        this.reportTypesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.reportTypesLabel.setText("Report Type");
        this.reportTypesLabel.setBounds(new Rectangle(133, 50, 93, 24));
        this.reporTypesComboBox.setBounds(new Rectangle(233, 50, 151, 24));

        this.tradeIdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.tradeIdLabel.setText("Trade Id");
        this.tradeIdLabel.setBounds(new Rectangle(400, 50, 93, 24));
        this.tradeIdText.setBounds(new Rectangle(500, 50, 151, 24));

        this.jLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
        this.jLabel2.setText("Processing Org");
        this.jLabel2.setBounds(new Rectangle(133, 100, 93, 24));
        this.processingOrgChoice.setBounds(new Rectangle(233, 100, 151, 24));

        this.add(this.jLabel2, null);
        this.add(this.processingOrgChoice, null);

        this.add(this.reportTypesLabel, null);
        this.add(this.tradeIdLabel, null);
        this.add(this.tradeIdText, null);
        this.add(this.reporTypesComboBox, null);
    }

    @SuppressWarnings("rawtypes")
    private void initDomains() {
        final Vector pos = AccessUtil.getAccessiblePONames(false, false);
        AppUtil.set(this.processingOrgChoice, pos);
    }

    private void initDomainsReportTypes() {
        final EmirSnapshotReportType[] reportTypeValues = EmirSnapshotReportType.values();
        List<String> reportTypesList = new ArrayList<String>();
        
        for (EmirSnapshotReportType reportType : reportTypeValues) {
            reportTypesList.add(reportType.name());
        }
        
        AppUtil.set(this.reporTypesComboBox, reportTypesList);
    }

    @Override
    public ReportTemplate getTemplate() {
        final String tradeFilterName = (String) this.reporTypesComboBox
                .getSelectedItem();
        this.template.put(
                SantEmirSnapshotReduxReportTemplate.REPORT_TYPES,
                tradeFilterName);

        final String tradeIds = this.tradeIdText.getText();
        this.template.put(
                SantEmirSnapshotReduxReportTemplate.TRADE_ID,
                tradeIds);

        final String poName = (String) this.processingOrgChoice
                .getSelectedItem();
        this.template.put(TransferReportTemplate.PO_NAME, poName);

        return this.template;
    }

    @Override
    public void setTemplate(final ReportTemplate template) {
        this.template = template;
        final String reportTypeFilter = (String) this.template
                .get(SantEmirSnapshotReduxReportTemplate.REPORT_TYPES);
        if (Util.isEmpty(reportTypeFilter)) {
            this.reporTypesComboBox.setSelectedItem("");
        } else {
            this.reporTypesComboBox.setSelectedItem(reportTypeFilter);
        }

        final String s = (String) template.get(TransferReportTemplate.PO_NAME);
        if (s != null) {
            this.processingOrgChoice.setSelectedItem(s);
        } else {
            this.processingOrgChoice.setSelectedItem("");
        }
    }
}
