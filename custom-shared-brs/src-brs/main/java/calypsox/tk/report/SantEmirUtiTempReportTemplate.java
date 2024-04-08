package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

public class SantEmirUtiTempReportTemplate extends TradeReportTemplate {

	
    private static final long serialVersionUID = 1L;


    @Override
    public void setDefaults() {
        super.setDefaults();
        setColumns(SantEmirUtiTempReportStyle.DEFAULTS_COLUMNS);
    }
    
    
}
