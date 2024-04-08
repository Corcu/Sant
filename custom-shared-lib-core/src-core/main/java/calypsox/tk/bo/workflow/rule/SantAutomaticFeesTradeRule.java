/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.bo.workflow.rule.AutomaticFeesTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

public class SantAutomaticFeesTradeRule implements WfTradeRule {

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		return true;
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("Apply sequentially AutomaticFees trade rule and then SantSaveTradeIndAmountFeeASMark trade rule.\n");
		sb.append("1- AutomaticFees rule attaches fees to trades according FeeGrid definition.\n");
		sb.append("2- SantSaveTradeIndAmountFeeASMark rule creates PLMark Independent Amount when trades got fee INDEP_AMOUNT or INDEP_AMOOUNT_PO.\n");
		return sb.toString();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {

		AutomaticFeesTradeRule autoFeeRule = new AutomaticFeesTradeRule();
		SantSaveTradeIndAmountFeeASMarkTradeRule feeAsMarkTradeRule = new SantSaveTradeIndAmountFeeASMarkTradeRule();

		boolean ret = autoFeeRule.update(wc, trade, oldTrade, messages, dsCon, excps, task, dbCon, events);
		ret = ret && feeAsMarkTradeRule.update(wc, trade, oldTrade, messages, dsCon, excps, task, dbCon, events);

		return ret;
	}

}
