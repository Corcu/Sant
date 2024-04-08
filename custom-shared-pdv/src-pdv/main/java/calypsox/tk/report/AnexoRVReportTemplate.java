package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

public class AnexoRVReportTemplate extends TradeReportTemplate {
    public void setDefaults() {
        super.setDefaults();// 10
        this.setColumns(AnexoRVReportStyle.ADDITIONAL_COLUMNS);// 11
    }// 12
}
