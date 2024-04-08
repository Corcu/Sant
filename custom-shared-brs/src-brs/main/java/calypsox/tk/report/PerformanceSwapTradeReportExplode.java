package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.core.Trade;
import com.calypso.tk.report.TradeReportExplode;

public class PerformanceSwapTradeReportExplode implements TradeReportExplode {
	
	public static String REPORTING_LEG = "REPORTING_LEG";
	public static String REPORTING_LEG_PRIMARY = "PRIMARY";
	public static String REPORTING_LEG_SECONDARY = "SECONDARY";

	@Override
	public Vector<Trade> explode(Trade trade) {
		Vector<Trade> result = new Vector<Trade>();
		Trade tradeClone = trade.clone();
		trade.addKeyword(REPORTING_LEG, REPORTING_LEG_PRIMARY);
		tradeClone.addKeyword(REPORTING_LEG, REPORTING_LEG_SECONDARY);
		result.add(trade);
		result.add(tradeClone);
		return result;
	}

}
