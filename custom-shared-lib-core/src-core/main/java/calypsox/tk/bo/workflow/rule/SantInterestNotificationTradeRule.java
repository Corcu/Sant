/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.InterestBearingEntry;
import com.calypso.tk.service.DSConnection;

/**
 * 
 * @author Cedric this workflow rule recalculates the principal amount on InterestBearing trade in case adjusted amounts
 *         are keyed in manually by users. so that the principal amount is always taking them into account.
 */
public class SantInterestNotificationTradeRule implements WfTradeRule {

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		return true;
	}

	@Override
	public String getDescription() {
		return "Update Interest Bearing Principal according position type INTEREST taking into account Amount + Adjustment";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		if (!trade.getProductType().equals("InterestBearing")) {
			return true;
		}
		InterestBearing ib = (InterestBearing) trade.getProduct();

		Vector<InterestBearingEntry> entries = ib.getEntries();
		if (Util.isEmpty(entries)) {
			return true;
		}

		double principal = 0;
		//
		for (InterestBearingEntry entry : entries) {
			if (!"INTEREST".equals(entry.getEntryType())) {
				continue;
			}
			principal += (entry.getAmount() + entry.getAdjustment());
		}

		// Entries Interest represent Accruals. The Principal amount on the InterestBearing trade must be of the
		// opposite sign. = interest amount.
		ib.setPrincipal(principal * -1);

		return true;
	}
}
