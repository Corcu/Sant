package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

/**
 * @author aalonsop
 * A file containing FI open trades is generated to be sent to TCyCC system.
 */
public class MarginCallTradeFIFlowReportTemplate extends TradeReportTemplate {

    private static final String TF_NAME = "Carteras";
    private static final long serialVersionUID = -1655127533046540816L;

    @Override
    public void setDefaults() {
        super.setDefaults();
        setTradeFilterCriteria();
    }

    /**
     * Next enhancement will be adding a customPanel to allow TradeFilter selection
     */
    private void setTradeFilterCriteria() {
        this.put(TradeReportTemplate.TRADE_FILTER, TF_NAME);
    }
}
