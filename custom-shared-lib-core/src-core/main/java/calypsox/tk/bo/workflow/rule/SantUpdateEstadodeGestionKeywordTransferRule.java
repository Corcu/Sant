package calypsox.tk.bo.workflow.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.ExternalArray;
import com.calypso.tk.core.InvalidClassException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

/**
 * Propagate CA Coupon Claims EstadodeGestion Attribute  from transfer to trade
 * @author x957355
 *
 */
public class SantUpdateEstadodeGestionKeywordTransferRule implements WfTransferRule {

	@SuppressWarnings("rawtypes")
	@Override
	public boolean check(final TaskWorkflowConfig wc, final BOTransfer transfer, final BOTransfer oldTransfer,
			final Trade trade, final Vector messages, final DSConnection dsCon, final Vector excps, final Task task,
			final Object dbCon, final Vector events) {
		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean update(final TaskWorkflowConfig wc, final BOTransfer transfer, final BOTransfer oldTransfer,
			final Trade trade, final Vector messages, final DSConnection dsCon, final Vector excps, final Task task,
			final Object dbCon, final Vector events) {
		boolean tradeModified = false; // Used to save or not the Trade
		if (transfer.getProductType().equals("CA")) {

			Map<BOTransfer, Trade> mapTransferIdTrade = getTradesByTransfer(transfer, dsCon, trade);

			for (Map.Entry<BOTransfer, Trade> entry : mapTransferIdTrade.entrySet()) {

				BOTransfer k = entry.getKey();
				Trade v = entry.getValue();
				Trade clonedTrade = v.clone();
				String claimReason = v.getKeywordValue("CAClaimReason");
				if (claimReason != null && !v.getKeywordValue("CAClaimReason").isEmpty()) {
					Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc,
							"SantUpdateEstadodeGestionKeyword");
					if (!Util.isEmpty(map)) {
						Iterator i = map.keySet().iterator();
						while (i.hasNext()) {
							// Only apply to CAClaimReason informed Trades

							String attribute = (String) i.next();
							clonedTrade.addKeyword(attribute, (String) map.get(attribute)); // Set the Keyword
							tradeModified = true;
							if (k.getNettedTransferLongId() != 0) {
								try {
									k = (BOTransfer) k.clone();
									// Propagate attribute to underlying xfers
									k.setAttribute(attribute, (String) map.get(attribute));
								} catch (CloneNotSupportedException e) {
									Log.error(this, "Can not set the XferAttribute", e);
								}
							}
						}
						clonedTrade.setAction(Action.UPDATE);
						mapTransferIdTrade.replace(k, clonedTrade);

					}
				}

			}

			if (tradeModified) {
				try {
					ExternalArray externalArray = new ExternalArray(new ArrayList<>(mapTransferIdTrade.values()));
					dsCon.getRemoteTrade().saveTrades(externalArray);
				} catch (InvalidClassException e) {
					Log.error(this, e);
					return false;
				} catch (CalypsoServiceException e) {
					Log.error(this, e);
					return false;
				}
			}

		}

		return true;
	}

	/**
	 * Get trades from the transfer
	 * 
	 * @param transfer BOtransfer
	 * @param dsCon    DSConnection object
	 * @param trade    trade
	 * @return
	 */
	private Map<BOTransfer, Trade> getTradesByTransfer(final BOTransfer transfer, final DSConnection dsCon, final Trade trade) {
		Map<BOTransfer, Trade> tradesMap = new HashMap<>();
		if (transfer.getNettedTransfer()) {
			TransferArray ta = transfer.getUnderlyingTransfers();
			ta.forEach(t -> tradesMap.put(t, getTrade(dsCon, t.getTradeLongId())));
		} else {
			tradesMap.put(transfer, getTrade(dsCon, trade.getLongId()));
		}
		return tradesMap;
	}

	/**
	 * Get Trade by id
	 * @param dsCon DSConnection object
	 * @param tradeId trade id
	 * @return
	 */
	private Trade getTrade(final DSConnection dsCon, final long tradeId) {
		try {
			return dsCon.getRemoteTrade().getTrade(tradeId);
		} catch (CalypsoServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see com.calypso.tk.bo.workflow.WfTransferRule#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Updates 'EstadodeGestion' Trade Keyword.";
	}

}

