package calypsox.tk.report.portbreakdown;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;

/**
 * @author aalonsop
 */
public class MxPortBreakdownWrapper extends PortfolioBreakdownMTMWrapper{


    public MxPortBreakdownWrapper(PLMark plMark, Trade trade, JDatetime valDate) {
        super(plMark, trade, valDate);
    }

    @Override
    protected String buildBoReference(Trade trade){
        return String.valueOf(trade.getLongId());
    }

    @Override
    protected String buildBOSystem(Trade trade) {
        return "CALYPSO_STC";
    }

}
