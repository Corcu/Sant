package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

@SuppressWarnings("serial")
public class ELBEAssocReposSecLendCollatTemplate extends TradeReportTemplate {
    @Override
    public void setDefaults() {
	super.setDefaults();
	setColumns(ELBEAssocReposSecLendCollatReportStyle.DEFAULTS_COLUMNS);
    }
}
