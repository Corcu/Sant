package calypsox.apps.reporting;

import calypsox.apps.reporting.util.control.SantTextFieldPanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.ProductConst;
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

public class BalanzaDePagosReportTemplatePanel extends ReportTemplatePanel {

    private static final String ISIN = "ISIN";
    public static final String MARGIN_CALL_PDV = "Margin Call & PDV";
    public static final String PDV = "PDV";
    public static final String MARGIN_CALL = "Margin Call";

    private ReportTemplate _template;

    private static final long serialVersionUID = 6271658179046890608L;

    public  static final String SELECTION_PRODUCT_TYPE = "SELECTION_PRODUCT_TYPE";

    private SantTextFieldPanel txtProductType;

    private SantTextFieldPanel txtISIN;

    private JButton positionTypeButton;

    private DefaultActionListener defaultActionListener;

    public BalanzaDePagosReportTemplatePanel() {
        super();
        setSize(new Dimension(1140, 50));
        buildControlsPanel();
    }

    @Override
    public final ReportTemplate getTemplate() {
        this._template.put(SELECTION_PRODUCT_TYPE, this.txtProductType.getValue());
        this._template.put(ISIN, this.txtISIN.getValue());
        return this._template;

    }

    @Override
    public void setTemplate(final ReportTemplate template) {
        this._template = template;
        this.txtProductType.setValue(template.get(SELECTION_PRODUCT_TYPE));
        this.txtISIN.setValue(template.get(ISIN));
    }

    /**
     * Builds the custom panels: Agreement & Process. Agreement: filters by
     * owner & contract name. Process: Process date y Security Isin
     */
    private void buildControlsPanel() {

        // process panel
        final JPanel processPanel = new JPanel();
        processPanel.setBorder(new TitledBorder(new EtchedBorder(1, null, null), "Select Product Type", 4, 2, null, null));
        processPanel.setBounds(15, 225, 350, 60);

        this.txtProductType = new SantTextFieldPanel("Product Type");
        this.txtProductType.setSize(new Dimension(340,25));
        this.txtProductType.setPreferredSize(new Dimension(340,25));
        this.txtProductType.setMaximumSize(new Dimension(340,25));
        this.txtProductType.setBounds(3, 5, 340, 25);

        processPanel.add(this.txtProductType, BorderLayout.EAST);
        processPanel.add(getPositionTypeButton(), BorderLayout.CENTER);
        processPanel.add(this.getTxtIsin(), BorderLayout.WEST);


        super.add(processPanel);
    }

    private JButton getPositionTypeButton() {
        if (this.positionTypeButton == null) {
            this.positionTypeButton = new JButton();
            this.positionTypeButton.setText("...");
            this.positionTypeButton.setBounds(241, 50, 32, 24);
            this.positionTypeButton.setActionCommand(SELECTION_PRODUCT_TYPE);
            this.positionTypeButton.setToolTipText("Choose product Type of extraction");
            this.positionTypeButton.addActionListener(this.getDefaultActionListener());
        }
        return this.positionTypeButton;
    }

    private SantTextFieldPanel getTxtIsin() {
        if (this.txtISIN == null) {
            this.txtISIN = new SantTextFieldPanel(ISIN);
            this.txtISIN.setToolTipText("Fill with security ISIN code");
        }
        return this.txtISIN;
    }

    private java.util.List<String> selectPositionType() {

        java.util.List<String> productTypeOptions = getProductTypeOptions();
        Vector<String> sels = Util.string2Vector(this.txtProductType.getValue());
        sels = AppUtil.chooseList(JideSwingUtilities.getFrame(this), new Vector(productTypeOptions), sels, "Select type of extraction");
        if (sels != null) {
            this.txtProductType.setValue(Util.collectionToString(sels));
        }
        return sels;
    }

    public static Vector<String> getProductTypeOptions() {
        List<String> exTypes = LocalCache.getDomainValues(DSConnection.getDefault(), "BalanzaDePagosInventoryProductTypes");
        if (Util.isEmpty(exTypes)) {
            exTypes = Arrays.asList(MARGIN_CALL_PDV, ProductConst.REPO, ProductConst.EQUITY);
        }
        return new Vector(exTypes);
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
            if (SELECTION_PRODUCT_TYPE.equals(action)) {
                selectPositionType();
            }
        }
    }
}
