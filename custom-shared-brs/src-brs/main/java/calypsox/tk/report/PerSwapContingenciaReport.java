package calypsox.tk.report;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.FlowGenerationException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

public class PerSwapContingenciaReport extends TradeReport {
	
	public static final String LOG_CATEGORY = "PerSwapContingenciaReport";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public ReportOutput load(Vector errorMsgs) {
		
		DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);
		if(output!=null) {
			ReportRow[] rows = output.getRows();
			HashMap<Trade, CashFlowSet> calculatedFlows = new HashMap<Trade, CashFlowSet>();
			for(int i=0; i<rows.length; i++) {
				Trade trade = (Trade)rows[i].getProperty(ReportRow.TRADE);
				if(!calculatedFlows.containsKey(trade)) {
					try {
						if(!trade.getProduct().getCustomFlowsB()) {
							trade.getProduct().getFlows(trade, getValDate(), false, -1, true);
						}
					} catch (FlowGenerationException e) {
						Log.error(LOG_CATEGORY, e);
					}
					calculatedFlows.put(trade,trade.getProduct().getFlows());
				}
				else
				{
					trade.getProduct().setFlows(calculatedFlows.get(trade));
				}
			}
		}
		return output;
		
	}
	
}
