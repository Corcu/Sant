package calypsox.apps.reporting;

import calypsox.apps.reporting.util.control.SantTextFieldPanel;
import com.calypso.apps.reporting.TradeReportTemplatePanel;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class MICBarridoReportTemplatePanel extends TradeReportTemplatePanel {

    public static final String PLMARK_TYPE = "PLMarkType";
    private ReportTemplate template;
    private SantTextFieldPanel txtExtractionType;

    public MICBarridoReportTemplatePanel() {
        super();
        setSize(new Dimension(1140, 50));
        buildControlsPanel();
    }
    private void buildControlsPanel() {

        // process panel
        final JPanel processPanel = new JPanel();
        processPanel.setBorder(new TitledBorder(new EtchedBorder(1, null, null), "PLMark Selection", 4, 2, null, null));
        processPanel.setBounds(7, 225, 650, 60);


        this.txtExtractionType = new SantTextFieldPanel("PLMark Type");
        this.txtExtractionType.setSize(new Dimension(240,25));
        this.txtExtractionType.setPreferredSize(new Dimension(240,25));
        this.txtExtractionType.setMaximumSize(new Dimension(240,25));
        this.txtExtractionType.setBounds(3, 5, 240, 25);

        processPanel.add(this.txtExtractionType, BorderLayout.CENTER);

        super.add(processPanel);
    }


    @Override
    public ReportTemplate getTemplate() {
        String s = this.txtExtractionType.getValue();
        this.template.put(PLMARK_TYPE, s);
        this.setTemplate(super.getTemplate());
        return this.template;
    }

    @Override
    public void setTemplate(final ReportTemplate arg0) {
        this.template = arg0;
        String s = this.template.get(PLMARK_TYPE);
        if (!Util.isEmpty(s)) {
            this.txtExtractionType.setValue(s);
        }
        super.setTemplate(arg0);
    }
}
