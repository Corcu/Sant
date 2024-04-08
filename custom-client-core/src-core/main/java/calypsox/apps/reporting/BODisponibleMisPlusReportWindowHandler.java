package calypsox.apps.reporting;

import com.calypso.tk.report.gui.ReportWindowDefinition;

public class BODisponibleMisPlusReportWindowHandler extends BODisponibleSecurityPositionReportWindowHandler{

    @Override
    public ReportWindowDefinition defaultReportWindowDefinition(String reportType) {
        return new ReportWindowDefinition(reportType);
    }
}
