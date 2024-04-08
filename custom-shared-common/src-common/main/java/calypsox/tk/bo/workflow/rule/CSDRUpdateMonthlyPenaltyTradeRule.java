package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.service.DSConnection;;

public class CSDRUpdateMonthlyPenaltyTradeRule implements WfTradeRule {


	private static final String PENALTY = "PENALTY";
	private static final String MONTHLY_CONFIRMED_AMOUNT = "MonthlyConfirmedAmount";
	private static final String PENALTY_AMOUNT = "Penalty_Amount";

	@Override
	public String getDescription() {
		return "Updates principal trade amount with monthly confirmed amount.";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade paramTrade2,
			Vector paramVector1, DSConnection paramDSConnection, Vector paramVector2, Task paramTask,
			Object paramObject, Vector paramVector3) {
		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade newTrade,
			Vector paramVector1, DSConnection paramDSConnection, Vector paramVector2, Task paramTask,
			Object paramObject, Vector paramVector3) {
		Product p = newTrade.getProduct();
		if (p instanceof SimpleTransfer && p.getSubType() != null && p.getSubType().equals(PENALTY)) {
			double mconfAmt = trade.getKeywordAsDouble(MONTHLY_CONFIRMED_AMOUNT);
			p.setPrincipal(mconfAmt);
			double quantity = Math.abs(trade.getQuantity());
			quantity = Math.copySign(quantity, mconfAmt);
			trade.setQuantity(quantity);
			trade.setProduct(p);
			trade.addKeyword(PENALTY_AMOUNT, Double.toString(mconfAmt));
		}
		return true;
	}

}
