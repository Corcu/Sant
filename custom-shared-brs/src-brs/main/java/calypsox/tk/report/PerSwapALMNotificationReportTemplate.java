package calypsox.tk.report;

import com.calypso.tk.report.CashFlowReportTemplate;

public class PerSwapALMNotificationReportTemplate extends CashFlowReportTemplate {

    @Override
    public void setDefaults() {
        super.setDefaults();
        setColumns(PerSwapALMNotificationReportStyle.ADDITIONAL_COLUMNS);
    }

}
