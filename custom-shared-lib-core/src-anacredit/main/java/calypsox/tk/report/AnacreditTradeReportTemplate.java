package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

public class AnacreditTradeReportTemplate extends TradeReportTemplate {

    @Override
    public void setDefaults() {
        setColumns(AnacreditInventoryOperReportStyle.DEFAULT_COLUMNS);
    }


}
