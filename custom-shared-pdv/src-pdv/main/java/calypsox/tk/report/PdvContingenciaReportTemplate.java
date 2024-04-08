package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

public class PdvContingenciaReportTemplate extends TradeReportTemplate {
    public void setDefaults() {
        super.setDefaults();// 10
        this.setColumns(PdvContingenciaReportStyle.ADDITIONAL_COLUMNS);// 11
    }// 12

}
