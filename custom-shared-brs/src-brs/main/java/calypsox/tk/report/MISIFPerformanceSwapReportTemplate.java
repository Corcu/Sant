package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

public class MISIFPerformanceSwapReportTemplate extends TradeReportTemplate {
    @Override
    public void setDefaults() {
        super.setDefaults();
        setColumns(MISIFPerformanceSwapReportStyle.ADDITIONAL_COLUMNS);
    }
}
