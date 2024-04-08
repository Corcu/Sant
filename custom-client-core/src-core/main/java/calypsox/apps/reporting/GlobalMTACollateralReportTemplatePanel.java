package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.report.ReportTemplate;

import java.awt.*;
import java.util.Map;

/**
 * @author aalonsop
 */
public class GlobalMTACollateralReportTemplatePanel extends ReportTemplatePanel {

    private static final long serialVersionUID = 1L;
    private CollateralConfigFilterPanel ccFilterPanel = null;
    protected ReportTemplate template;

    public GlobalMTACollateralReportTemplatePanel() {
        this.setLayout(null);
        this.add(this.getMarginCallEntryFilterPanel());
        this.setPreferredSize(new Dimension(640, 280));
        this.setSize(new Dimension(640, 280));
    }// 36

    private CollateralConfigFilterPanel getMarginCallEntryFilterPanel() {
        if (this.ccFilterPanel == null) {
            this.ccFilterPanel = new CollateralConfigFilterPanel();
            this.ccFilterPanel.setBounds(5, 5, 1010, 320);
        }

        return this.ccFilterPanel;
    }

    @Override
    public void setTemplate(ReportTemplate template) {
        this.template = template;
        this.getMarginCallEntryFilterPanel().setTemplate(template);
    }

    @Override
    public ReportTemplate getTemplate() {
        this.getMarginCallEntryFilterPanel().getTemplate(this.template);
        return this.template;
    }

    @Override
    public boolean isValidLoad(ReportPanel panel) {
        Map potentialSizesByTypeOfObject = panel.getReport().getPotentialSize();
        return this.displayLargeListWarningMessage(this, potentialSizesByTypeOfObject);
    }


}
