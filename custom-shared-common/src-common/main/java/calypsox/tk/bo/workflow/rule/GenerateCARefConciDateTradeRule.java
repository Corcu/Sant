package calypsox.tk.bo.workflow.rule;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.FlowGenerationException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CA;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.bo.util.CAUtil;

public class GenerateCARefConciDateTradeRule implements WfTradeRule {


	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		return true;
	}

	@Override
	public String getDescription() {
		return "Add Swift_Event_Code + CAReference + EventDate on keyword CARefConciDate";
	}

	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		Product product = trade.getProduct();
		if (null != trade && product instanceof CA) {
			String swiftEventCodeName = CAUtil.getCASwiftEventCode(dsCon, product);
			JDate eventDate = null;
			CA ca = (CA) product;
			if (ca.getUnderlyingProduct() instanceof Bond) {
				JDatetime tradeDate = trade.getTradeDate();
				Bond underlyingProductBond = (Bond) ca.getUnderlyingProduct();
				CashFlowSet cashFlows = null;
				try {
					cashFlows = underlyingProductBond.generateFlows(tradeDate.getJDate(TimeZone.getDefault()));
				} catch (FlowGenerationException e) {
					Log.error(this.getClass().getSimpleName(),
							"Could not generate CashFlows for product Bond with id: " + product.getId(), e);
				}
				if (cashFlows != null && cashFlows.size() > 0) {
					CashFlow cashFlow = getEnclosingCashFlow(cashFlows,
							trade.getTradeDate().getJDate(TimeZone.getDefault()));
					if (cashFlow != null) {
						eventDate = cashFlow.getEndDate();
					}
				}
			}
			String eventDateString = "";
			if (eventDate != null) {
				final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
				eventDateString = sdf.format(eventDate.getDate());
			}
			if (!Util.isEmpty(swiftEventCodeName)) {
				trade.addKeyword("CARefConciDate", eventDateString + swiftEventCodeName);
			}
		}
		return true;
	}

	private CashFlow getEnclosingCashFlow(CashFlowSet cashFlowSet, JDate tradeDate) {
		CashFlow cashFlow = (CashFlow) cashFlowSet.findEnclosingCashFlow(tradeDate, CashFlow.INTEREST);
		if (cashFlow == null) {
			cashFlow = (CashFlow) cashFlowSet.findEnclosingCashFlow(tradeDate, CashFlow.PRINCIPAL);
		}
		return cashFlow;
	}

}
