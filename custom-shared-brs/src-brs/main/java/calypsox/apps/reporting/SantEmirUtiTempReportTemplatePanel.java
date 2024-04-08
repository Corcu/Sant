package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.report.ReportTemplate;

public class SantEmirUtiTempReportTemplatePanel extends ReportTemplatePanel {
    
    private static final long serialVersionUID = 1L;

    private ReportTemplate template;

    @Override
    public ReportTemplate getTemplate() {
        return this.template;
    }

    @Override
    public void setTemplate(final ReportTemplate t) {
        this.template = t;
    }

}
