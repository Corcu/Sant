package calypsox.tk.report;

import com.calypso.tk.report.BalanceReportTemplate;

public class BalancesPostingReportTemplate extends BalanceReportTemplate {

    public BalancesPostingReportTemplate(){
        super();
    }
    public void setDefailts() {
        super.setDefaults();
        this.setColumns(BalancesPostingReportStyle.ADDITIONAL_COLUMNS);
    }
}
