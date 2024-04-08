package calypsox.apps.reporting;

import calypsox.apps.reporting.util.control.SantCheckBoxPanel;
import calypsox.apps.reporting.util.control.SantTextFieldPanel;
import calypsox.tk.report.AnacreditAbstractReport;
import calypsox.tk.report.AnacreditOperacionesReportTemplate;
import com.calypso.apps.reporting.MarginCallPositionEntryReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.jidesoft.swing.JideSwingUtilities;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static calypsox.tk.report.AnacreditOperacionesReportTemplate.ANACREDIT_USE_IDS_FROM_PANEL;

public class AnacreditOperacionesReportTemplatePanel extends MarginCallPositionEntryReportTemplatePanel {
    private static final String ACTION_SELECT_EXTRACTION_TYPE = "ACTION_SELECT_EXTRACTION_TYPE";
    private ReportTemplate template;

    private SantCheckBoxPanel chkUseIdsFromPanel;

    private SantTextFieldPanel txtExtractionType;

    private JButton positionTypeButton;

    private DefaultActionListener defaultActionListener;

    public AnacreditOperacionesReportTemplatePanel() {
        super();
        setSize(new Dimension(1140, 50));
        buildControlsPanel();
    }

    @Override
    public final ReportTemplate getTemplate() {

        ReportTemplate reportTemplate = super.getTemplate();
        reportTemplate.put(AnacreditOperacionesReportTemplate.ANACREDIT_EXTRACTION_TYPE, this.txtExtractionType.getValue());
        reportTemplate.put(ANACREDIT_USE_IDS_FROM_PANEL, this.chkUseIdsFromPanel.getValue());
        return reportTemplate;

    }

    @Override
    public void setTemplate(final ReportTemplate template) {
        super.setTemplate(template);
        this.txtExtractionType.setValue(template.get(AnacreditOperacionesReportTemplate.ANACREDIT_EXTRACTION_TYPE));
        boolean b = false;
        if (null != template.get(ANACREDIT_USE_IDS_FROM_PANEL)) {
            b = template.get(ANACREDIT_USE_IDS_FROM_PANEL);
        }
        this.chkUseIdsFromPanel.setValue(b);

    }

    /**
     * Builds the custom panels: Agreement & Process. Agreement: filters by
     * owner & contract name. Process: Process date y Security Isin
     */
    private void buildControlsPanel() {

        // process panel
        final JPanel processPanel = new JPanel();
        processPanel.setBorder(new TitledBorder(new EtchedBorder(1, null, null), "Anacredit Selection", 4, 2, null, null));
        processPanel.setBounds(612, 225, 297, 60);

        this.txtExtractionType = new SantTextFieldPanel("Type");
        this.txtExtractionType.setSize(new Dimension(140,25));
        this.txtExtractionType.setPreferredSize(new Dimension(140,25));
        this.txtExtractionType.setMaximumSize(new Dimension(140,25));
        this.txtExtractionType.setBounds(3, 5, 140, 25);


        this.chkUseIdsFromPanel = new SantCheckBoxPanel("IDs Panel",110 );
        this.chkUseIdsFromPanel.setToolTipText("Use Contract Ids from Template Panel");
        this.chkUseIdsFromPanel.setSize(new Dimension(85,25));
        this.chkUseIdsFromPanel.setPreferredSize(new Dimension(85,25));
        this.chkUseIdsFromPanel.setMaximumSize(new Dimension(85,25));

        this.chkUseIdsFromPanel.setBounds(3, 30, 85, 25);

        //processPanel.add(this.chkUseIdsFromPanel, BorderLayout.WEST);
        processPanel.add(this.txtExtractionType, BorderLayout.CENTER);
        processPanel.add(getPositionTypeButton(), BorderLayout.EAST);

        super.add(processPanel);
    }

    private JButton getPositionTypeButton() {
        if (this.positionTypeButton == null) {
            this.positionTypeButton = new JButton();
            this.positionTypeButton.setText("...");
            this.positionTypeButton.setBounds(241, 50, 32, 24);
            this.positionTypeButton.setActionCommand(ACTION_SELECT_EXTRACTION_TYPE);
            this.positionTypeButton.setToolTipText("Choose type of extraction for Anacredit.");
            this.positionTypeButton.addActionListener(this.getDefaultActionListener());
        }

        return this.positionTypeButton;
    }

    private List<String> selectPositionType() {

        List<String> exTypes = getExtractionTypesDomain();
        Vector<String> sels = Util.string2Vector(this.txtExtractionType.getValue());
        sels = AppUtil.chooseList(JideSwingUtilities.getFrame(this), new Vector(exTypes), sels, "Select type of extraction");
        if (sels != null) {
            this.txtExtractionType.setValue(Util.collectionToString(sels));
        }
        return sels;
    }

    public static  List<String> getExtractionTypesDomain() {
        List<String> exTypes = LocalCache.getDomainValues(DSConnection.getDefault(), AnacreditOperacionesReportTemplate.DV_ANACREDIT_EXTRACTION_TYPES);
        if (Util.isEmpty(exTypes)) {
            exTypes = Arrays.asList("ALL", "Cash");
        }
        return exTypes;
    }

    private DefaultActionListener getDefaultActionListener() {
        if (this.defaultActionListener == null) {
            this.defaultActionListener = new DefaultActionListener();
        }
        return this.defaultActionListener;
    }

    private class DefaultActionListener implements ActionListener {
        private DefaultActionListener() {
        }
        public void actionPerformed(ActionEvent e) {
            String action = e.getActionCommand();
            if (ACTION_SELECT_EXTRACTION_TYPE.equals(action)) {
                selectPositionType();
            }
        }
    }

}
