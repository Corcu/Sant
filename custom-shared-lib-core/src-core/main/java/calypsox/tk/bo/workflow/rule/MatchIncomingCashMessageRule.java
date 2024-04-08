package calypsox.tk.bo.workflow.rule;

import java.sql.Connection;
import java.util.Vector;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.CurrencyUtil;

public class MatchIncomingCashMessageRule extends
		com.calypso.tk.bo.workflow.rule.MatchIncomingCashMessageRule {

	protected boolean saveTransfer(BOTransfer transfer, BOMessage message,
			Vector events, Connection con) {
		double amountToMatch = Math.abs(getAmountToMatch(message));
		double xferAmount = CurrencyUtil.roundAmount(
				Math.abs(transfer.getSettlementAmount()),
				transfer.getSettlementCurrency());
		if (amountToMatch != xferAmount) {
			double diff = amountToMatch - xferAmount;
			transfer.setAttribute("MoneyDiff", Util.inumberToString(diff));
			if (transfer.getSettlementAmount() < 0.0D)
				amountToMatch = -amountToMatch;
			transfer.setRealSettlementAmount(amountToMatch);
		}
		try {
			if (!settleTransfer(transfer, message, events, con))
				return false;
		} catch (Exception e) {
			Log.error(this, e);
			return false;
		}
		return true;
	}

	public static double getAmountToMatch(BOMessage message) {
		String amountS = message.getAttribute("Money Amount");
		if (Util.isEmpty(amountS)) {
			amountS = message.getAttribute("Amount");
		}
		if (Util.isEmpty(amountS)) {
			return 0.0D;
		} else {
			return Util.stringToNumber(amountS);
		}
	}
}
