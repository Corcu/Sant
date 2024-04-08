package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

public class FI105_BRSReportTemplate extends TradeReportTemplate {
    public FI105_BRSReportTemplate() {
        super();
    }// 5

    public void setDefaults() {
        super.setDefaults();// 10
        this.setColumns(FI105_BRSReportStyle.ADDITIONAL_COLUMNS);// 11
    }// 12
}
