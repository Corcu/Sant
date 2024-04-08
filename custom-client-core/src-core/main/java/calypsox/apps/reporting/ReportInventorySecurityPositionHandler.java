package calypsox.apps.reporting;

import calypsox.tk.report.BODisponibleBlockingPositionReport;
import calypsox.tk.report.BODisponibleSecurityPositionReport;
import com.calypso.apps.reporting.SelectionContext;
import com.calypso.tk.report.BOPositionReportTemplate;
import com.calypso.tk.report.InventoryFilterAdapter;
import com.calypso.tk.report.ReportRow;

public class ReportInventorySecurityPositionHandler extends com.calypso.apps.reporting.ReportInventorySecurityPositionHandler {

    @Override
    public void showTransferReport(SelectionContext<ReportRow> selectionContext) {
        if(BODisponibleSecurityPositionReport.class.getSimpleName().replace("Report","").equalsIgnoreCase(getReportPanel().getReportType())){
            showDisponibleTransferReport(selectionContext,"BOPosition Not Settled Movements");
        } else if (BODisponibleBlockingPositionReport.class.getSimpleName().replace("Report","").equalsIgnoreCase(getReportPanel().getReportType())) {
            showDisponibleTransferReport(selectionContext,"BODisponibleBlockingTransferReport");
        } else {
            super.showTransferReport(selectionContext);
        }
    }

    private void showDisponibleTransferReport(SelectionContext<ReportRow> selectionContext,String templateName){
        selectionContext.setProperty("TransferTemplateName",templateName);
        BOPositionReportTemplate.BOPositionReportTemplateContext templateContext = (BOPositionReportTemplate.BOPositionReportTemplateContext)selectionContext.getProperty(BOPositionReportTemplate.BOPositionReportTemplateContext.class);
        templateContext.customFilter = InventoryFilterAdapter.create(new BODisponibleTransferFilter());
        super.showTransferReport(selectionContext);
    }

}
