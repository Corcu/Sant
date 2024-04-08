package calypsox.apps.reporting;

import calypsox.apps.reporting.util.control.SantCheckBoxPanel;
import calypsox.apps.reporting.util.control.SantTextFieldPanel;
import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.util.EquityTypeIdentifier;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
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


public class AnacreditEQPositionOldReportTemplatePanel extends ReportTemplatePanel {

   //private static final String ISIN = "ISIN";

    private static final String ACTION_SELECT_EXTRACTION_TYPE = "ACTION_SELECT_EXTRACTION_TYPE";

    public static final String DV_ANACREDIT_EXTRACTION_TYPES = "Anacredit.extractionTypes";

    public static final String SELECTED_ISIN = "SELECTED_ISIN";

    private ReportTemplate template;

    private SantTextFieldPanel txtISIN;

    private SantCheckBoxPanel chkUseIdsFromPanel;

    private SantTextFieldPanel txtExtractionType;

    private JButton positionTypeButton;

    private DefaultActionListener defaultActionListener;

    private JLabel copyTypeLabel = new JLabel();

    private CalypsoComboBox copyTypeChoice = new CalypsoComboBox();

    public AnacreditEQPositionOldReportTemplatePanel() {
        super();
        setSize(new Dimension(1140, 50));
        buildControlsPanel();
    }

    @Override
    public ReportTemplate getTemplate() {

        String s = this.txtExtractionType.getValue();
        this.template.put(AnacreditConstants.ANACREDIT_EXTRACTION_TYPE, s);
        this.template.put(AnacreditConstants.ROW_DATA_TYPE, this.copyTypeChoice.getSelectedItem());
        this.template.put(SELECTED_ISIN, this.txtISIN.getValue());
        //final boolean isFlagSelected = this.flag.isSelected();
        return this.template;
    }

    @Override
    public void setTemplate(final ReportTemplate arg0) {
        this.template = arg0;
        String s = this.template.get(AnacreditConstants.ANACREDIT_EXTRACTION_TYPE);
        if (!Util.isEmpty(s)) {
            this.txtExtractionType.setValue(s);
        }

        String s2 = (String)this.template.get(AnacreditConstants.ROW_DATA_TYPE);
        if (s2 != null) {
            this.copyTypeChoice.setSelectedItem(s2);
        } else {
            this.copyTypeChoice.setSelectedIndex(0);
        }
        this.txtISIN.setValue(template.get(SELECTED_ISIN));
    }

    /**
     * Builds the custom panels: Agreement & Process. Agreement: filters by
     * owner & contract name. Process: Process date y Security Isin
     */
    private void buildControlsPanel() {


        add(this.getTxtIsin(), BorderLayout.WEST);
        // process panel
        final JPanel processPanel = new JPanel();
        processPanel.setBorder(new TitledBorder(new EtchedBorder(1, null, null), "Anacredit Selection", 4, 2, null, null));
        processPanel.setBounds(7, 225, 650, 60);

        this.copyTypeLabel.setBounds(new Rectangle(867, 12, 73, 24));
        this.copyTypeLabel.setText("Select COPY file to Generate");
        this.copyTypeLabel.setHorizontalAlignment(4);
        this.copyTypeChoice.setBounds(new Rectangle(952, 12, 288, 24));
        this.copyTypeChoice.setPreferredSize(new Dimension(185,25));
        this.copyTypeChoice.setMaximumSize(new Dimension(185,25));
        this.copyTypeChoice.setActionCommand(AnacreditConstants.ROW_DATA_TYPE);

        AppUtil.set(this.copyTypeChoice, Arrays.asList(AnacreditConstants.COPY_3,
                AnacreditConstants.COPY_4, AnacreditConstants.COPY_4A, AnacreditConstants.COPY_11, AnacreditConstants.COPY_13));

        copyTypeChoice.addActionListener(getDefaultActionListener());


        this.txtExtractionType = new SantTextFieldPanel("Type");
        this.txtExtractionType.setSize(new Dimension(240,25));
        this.txtExtractionType.setPreferredSize(new Dimension(240,25));
        this.txtExtractionType.setMaximumSize(new Dimension(240,25));
        this.txtExtractionType.setBounds(3, 5, 240, 25);

        this.chkUseIdsFromPanel = new SantCheckBoxPanel("IDs Panel",110 );
        this.chkUseIdsFromPanel.setToolTipText("Use Contract Ids from Template Panel");
        this.chkUseIdsFromPanel.setSize(new Dimension(85,25));
        this.chkUseIdsFromPanel.setPreferredSize(new Dimension(85,25));
        this.chkUseIdsFromPanel.setMaximumSize(new Dimension(85,25));

        this.chkUseIdsFromPanel.setBounds(3, 30, 85, 25);

        processPanel.add(this.copyTypeLabel, BorderLayout.WEST);
        processPanel.add(this.copyTypeChoice, BorderLayout.WEST);
        processPanel.add(this.txtExtractionType, BorderLayout.CENTER);
        processPanel.add(getPositionTypeButton(), BorderLayout.EAST);

        super.add(processPanel);
    }


    private SantTextFieldPanel getTxtIsin() {
        if (this.txtISIN == null) {
            this.txtISIN = new SantTextFieldPanel(SELECTED_ISIN);
            this.txtISIN.setToolTipText("Fill with security ISIN code");
            this.txtISIN.setName(SELECTED_ISIN);
        }
        return this.txtISIN;
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
            if (this.template!= null){
                this.template.put(AnacreditConstants.ANACREDIT_EXTRACTION_TYPE, this.txtExtractionType.getValue());
            }
        }
        return sels;
    }

    /**
     * Define different types of Extraction
     */
    protected  List<String> getExtractionTypesDomain() {
            List<String> exTypes = LocalCache.getDomainValues(DSConnection.getDefault(), DV_ANACREDIT_EXTRACTION_TYPES);
            if (Util.isEmpty(exTypes)) {
                exTypes.add("");
                exTypes = EquityTypeIdentifier.getEquityPositionBasedTypes();
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
            else if (action.equals(AnacreditConstants.ROW_DATA_TYPE)) {
                updateCopyType();
            }
            else if (action.equals(SELECTED_ISIN)) {
                updateISIN();
            }
        }
    }

    private void updateISIN() {
        this.template.put(SELECTED_ISIN, String.valueOf(this.txtISIN.getValue().trim()));
    }

    private void updateCopyType() {
        this.template.put(AnacreditConstants.ROW_DATA_TYPE, String.valueOf(this.copyTypeChoice.getSelectedItem()));
    }

}
