package calypsox.tk.bo.workflow.rule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.rule.DisallowChangesTradeRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;


public class SLDisallowChangesTradeRule
extends DisallowChangesTradeRule {

	public static final String REPROCESS_KW = "Reprocess";
	private static final String LOG_CATEGORY = "SLDisallowChangesTradeRule";
	private static final String KW_PARTENON_REQUEST = "PartenonRequest";
	private static final String KW_PARTENON_REQUESTDATE = "PartenonRequestDate";
	//private static final String SDFILTER_RULE_PARAM = "SDFilterForceOK";
	private static final String S_TRUE = "true";
	private static final String SECLENDING_FEE = "SECLENDING_FEE";
	private static final String ERROR_CURRENCY_CHANGE = "The currency cannot be changed because there "
			+ "is at least one xfer of type SECLENDING_FEE in SETTLED status";
	
	protected String getLogCategory() {
		return LOG_CATEGORY;
	}
	protected String getRuleName() {
		return "SLDisallowChanges";
	}

	@Override
	public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon,
			Vector excps, Task task, Object dbCon, Vector events) {
		if (trade == null) {
			return true;
		}
		
		logRuleParams(wc, getRuleName());

		// If it is a reprocess, accept always
		String reprocessKW = trade.getKeywordValue(REPROCESS_KW);
		if (!Util.isEmpty(reprocessKW) && reprocessKW.equalsIgnoreCase(S_TRUE)) {
			Log.info(this, getRuleName() + " " + REPROCESS_KW + " KW is set to TRUE, ignoring all changes in Trade " + trade.getLongId());
			return true;
		}
		
		if (!atLeastOneXFerIsSettled(trade, dsCon)) {
			Log.info(this, getRuleName() + " " + " None of the XFers is Settled, ignoring all changes in Trade " + trade.getLongId());
			return true;
		}

//		StaticDataFilter sdFilter = null;
//		String sdFilterName = getSDFilterName(wc, getRuleName());
//		try {
//			if (!Util.isEmpty(sdFilterName)) {
//				Log.info(this, getRuleName() + " SD Filter for Rule bypass is set : " + sdFilterName);
//				sdFilter = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter(sdFilterName);
//				// If SD Filter accepted, accept always
//				if (sdFilter != null && sdFilter.accept(trade)) {
//					Log.info(this, getRuleName() + " SD Filter for Rule bypass has accepted Trade " + trade.getLongId());
//					return true;
//				}
//				Log.info(this, getRuleName() + " SD Filter for Rule bypass has NOT accepted Trade " + trade.getLongId());
//			}
//		} catch (CalypsoServiceException e) {
//			Log.error(this, getRuleName() + " Error retrieving SD Filter " + sdFilterName + ": " + e.toString());
//		}

		boolean res = super.check(wc, trade, oldTrade, messages, dsCon, excps, task, dbCon, events);
		if (res) {
			final String currency = trade.getProduct().getCurrency();
			final String oldCurrency = oldTrade.getProduct().getCurrency();

			if (currency != null && oldCurrency != null && !currency.equals(oldCurrency)) {
				res = checkIfHasSecLendingFeeOnSettled(trade);
			}
		}

		if (res) {
			Log.info(this, getRuleName() + " Rule has accepted Trade " + trade.getLongId());
		}
		else {
			Log.info(this, getRuleName() + " Rule has NOT accepted Trade " + trade.getLongId());
		}
		return res;
	}

	private boolean atLeastOneXFerIsSettled(Trade trade, DSConnection dsCon) {
		try {
			TransferArray xFers = dsCon.getRemoteBO().getBOTransfers(trade.getLongId());
			for (int i = 0; i < xFers.size(); i++) {
				BOTransfer xfer = xFers.get(i);
				if (xfer.getStatus().equals(Status.S_SETTLED)) {
					return true;
				}
			}
		} catch (CalypsoServiceException e) {
			Log.error(this, getRuleName() + " Cannot get XFers of Trade " + trade.getLongId());
		}
		return false;
	}
	
	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events)
	{
		String reprocessKW = trade.getKeywordValue(REPROCESS_KW);
		if (!Util.isEmpty(reprocessKW)) {
			trade.removeKeyword(REPROCESS_KW);
			Log.info(this, getRuleName() + " Removing " + REPROCESS_KW + " KW in  Trade " + trade.getLongId());
		}

		String PartenonRequestValue = "false";
		if (trade.getBookId() != oldTrade.getBookId() ||
				trade.getCounterParty().getId() != oldTrade.getCounterParty().getId()) {
			PartenonRequestValue = S_TRUE;
			Log.info(this, getRuleName() + " Book or Counterparty has changed, setting " + KW_PARTENON_REQUEST + " to TRUE in Trade " + trade.getLongId());
		}
		SecLending newSL = (SecLending)trade.getProduct();
		SecLending oldSL = (SecLending)oldTrade.getProduct();
		if (!newSL.getDirection().equals(oldSL.getDirection())) {
			PartenonRequestValue = S_TRUE;
			Log.info(this, getRuleName() + " Direction has changed, setting " + KW_PARTENON_REQUEST + " to TRUE in Trade " + trade.getLongId());
		}
		trade.addKeyword(KW_PARTENON_REQUEST, PartenonRequestValue);
		
		if (PartenonRequestValue.equals(S_TRUE)) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
			final String date = dateFormat.format(new Date());
			trade.addKeyword(KW_PARTENON_REQUESTDATE, date);
			Log.info(this, getRuleName() + " Adding " + KW_PARTENON_REQUESTDATE + " KW in Trade " + trade.getLongId());
		}
		else {
			trade.removeKeyword(KW_PARTENON_REQUESTDATE);
			Log.info(this, getRuleName() + " Removing " + KW_PARTENON_REQUESTDATE + " KW in Trade " + trade.getLongId());
		}

		return true;
	}


//	private String getSDFilterName(TaskWorkflowConfig wc, String ruleName) {
//		Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, ruleName);
//
//		String domainName = null;
//		if (!Util.isEmpty(map)) {
//			for (String key : map.keySet()) {
//				if (key.equalsIgnoreCase(SDFILTER_RULE_PARAM))        {
//					domainName = (String)map.get(key);
//					break;
//				}
//			}
//		}
//		return domainName;
//	}
	
	private void logRuleParams(TaskWorkflowConfig wc, String ruleName) {
		StringBuilder sb = new StringBuilder();
		sb.append(getRuleName());
		sb.append(" in ");
		sb.append(wc.getCurrentWorkflow());
		sb.append(" Workflow");
		sb.append(" from ");
		sb.append(wc.getStatus());
		sb.append(" to ");
		sb.append(wc.getResultingStatus());
		sb.append(" applying ");
		sb.append(wc.getPossibleAction());
		Log.info(this, sb.toString());

		Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, ruleName);
		if (!Util.isEmpty(map)) {
			for (String key : map.keySet()) {
				Log.info(this, getRuleName() + " Rule param : " + key + "=" + (String)map.get(key));
			}
		}
		else {
			Log.info(this, getRuleName() + " Rule has no parameters set.");
		}
	}
	

	/**
	 * It return false if the trade has at least one SECLENDING_FEE xfer on SETTLED
	 * status
	 *
	 * @param trade
	 * @return
	 */
	private boolean checkIfHasSecLendingFeeOnSettled(Trade trade) {
		final boolean result = true;

		final TransferArray transfers = getTransfers(trade.getLongId());
		if (transfers != null) {
			for (final BOTransfer xfer : transfers) {
				if (SECLENDING_FEE.equalsIgnoreCase(xfer.getTransferType())
						&& Status.SETTLED.equals(xfer.getStatus().getStatus())) {
					Log.error(this, ERROR_CURRENCY_CHANGE);
					return false;
				}
			}
		}

		return result;
	}

	/**
	 * Gets the xfers associated with a tradeId
	 *
	 * @param tradeId
	 * @return
	 */
	private TransferArray getTransfers(long tradeId) {
		TransferArray xfers = null;

		try {
			xfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(tradeId);
		} catch (final CalypsoServiceException e) {
			Log.error("Error getting the xfers of the trade: " + tradeId, e);
		}

		return xfers;
	}
}
