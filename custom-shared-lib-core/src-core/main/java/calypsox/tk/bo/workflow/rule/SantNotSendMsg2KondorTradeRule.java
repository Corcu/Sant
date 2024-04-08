package calypsox.tk.bo.workflow.rule;

import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

/*
 * Check the trade keyword IS_MIGRATION. If the value for the keyword is TRUE, we return FALSE so we don't send
 * the message to Kondor+, else, we return TRUE to send the message to Kondor+. we are creating Margin Call Trades,
 * the same for Kondor+ when we send messages to this system. We have to avoid that. 
 */
public class SantNotSendMsg2KondorTradeRule implements WfTradeRule {
	private static final String TRUE = "TRUE";
	private static final String IS_MIGRATION = "IS_MIGRATION";

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
			final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

		// We check the keyword.
		if (TRUE.equals(trade.getKeywordValue(IS_MIGRATION))) {
			return false;
		}

		return true;
	}

	@Override
	public String getDescription() {
		final String desc = "Check the trade keyword IS_MIGRATION. If the value for the keyword is TRUE, we return FALSE so we don't send"
				+ "the message to Kondor+, else, we return TRUE to send the message to Kondor+. we are creating Margin Call Trades,"
				+ "the same for Kondor+ when we send messages to this system. We have to avoid that.";

		return desc;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
			final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

		return true;
	}
}