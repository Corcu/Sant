package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

public class PerSwapMISNotificationReportTemplate extends TradeReportTemplate {
    @Override
    public void setDefaults() {
        super.setDefaults();
        setColumns(PerSwapMISNotificationReportStyle.ADDITIONAL_COLUMNS);
    }
}
