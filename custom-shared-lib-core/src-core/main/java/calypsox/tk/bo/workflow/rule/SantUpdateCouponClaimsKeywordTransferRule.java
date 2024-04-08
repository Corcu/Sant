/**
 *
 */
package calypsox.tk.bo.workflow.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
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
import com.calypso.tk.product.CA;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.WorkflowUtil;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

/**
 * Propagate CA Coupon Claims attributes from transfer to trade
 * 
 * @author x957355
 *
 */
public class SantUpdateCouponClaimsKeywordTransferRule implements WfTransferRule {

	private final String CA_CLAIM_TRANSFER_ID = "CAClaimTransferId";
	private final String ACCOUNT_ID = "CaClaimAccountId";
	private final String ACCOUNT_NAME = "CaClaimAccountName";
	private final List<String> CA_SUBTYPES = new ArrayList<String>(
			Arrays.asList("INTEREST", "DIVIDEND", "AMORTIZATION", "REDEMPTION"));

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

				// Only apply to CAClaimReason informed Trades
				if (clonedTrade != null && clonedTrade.getKeywordValue("CAClaimReason") != null
						&& !clonedTrade.getKeywordValue("CAClaimReason").isEmpty()) {
					if (clonedTrade.getProduct() instanceof CA) {
						CA ca = (CA) clonedTrade.getProduct();

						if (CA_SUBTYPES.contains(ca.getSubType())) { // Only for some CA subTypr trades

							transfer.setAttribute("isCAClaim", "Y"); // Set the XferAtt to identify CAClaim
							Long id = 0l; // settlements
							if (k.getNettedTransferLongId() == 0) {
								id = k.getLongId();
								if (id <= 0) {
									id = k.getAllocatedLongSeed();
								}
								
							} else {
								id = k.getNettedTransferLongId();

								// Set the XferAtt on the underlying transfers
								try {
									k=(BOTransfer)k.clone();
									k.setAttribute("isCAClaim", "Y");
								} catch (CloneNotSupportedException e) {
									Log.error(this, "Can not set the XferAttribute", e);
								}
							}
							if (clonedTrade.getKeywordAsLongId(this.CA_CLAIM_TRANSFER_ID) != id) {
								clonedTrade.addKeywordAsLong(this.CA_CLAIM_TRANSFER_ID, id);
								tradeModified = true;
							}
							tradeModified |= setAccountKeywords(clonedTrade, k, dsCon);
						}
					}

					Map<String, String> map = WorkflowUtil.getAttributeToValueMap(wc, "SantUpdateCouponClaimsKeyword");
					if (!Util.isEmpty(map)) {
						Iterator i = map.keySet().iterator();
						while (i.hasNext()) {
							String attribute = (String) i.next();
							k.setAttribute(attribute, (String) map.get(attribute));
							String prevValue = clonedTrade.getKeywordValue(attribute);
							if (prevValue == null || !prevValue.equals((String) map.get(attribute))) {
								clonedTrade.addKeyword(attribute, (String) map.get(attribute));
								tradeModified = true;
							}

						}
					}

				}
				clonedTrade.setAction(Action.UPDATE);
				mapTransferIdTrade.replace(k, clonedTrade);
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
	private Map<BOTransfer, Trade> getTradesByTransfer(final BOTransfer transfer, final DSConnection dsCon,
			final Trade trade) {
		Map<BOTransfer, Trade> tradesMap = new HashMap<>();
		if (transfer.getNettedTransfer()) {
			TransferArray ta;
			try {
				ta = DSConnection.getDefault().getRemoteBO().getNettedTransfers(transfer.getLongId());
				ta.forEach(t -> tradesMap.put(t, getTrade(dsCon, t.getTradeLongId())));

			} catch (CalypsoServiceException e) {
				Log.error(this, "Error getting netted transfers.", e);
			}
		} else {
			tradesMap.put(transfer, getTrade(dsCon, trade.getLongId()));
		}
		return tradesMap;
	}

	/**
	 * Get Trade by id
	 * 
	 * @param dsCon   DSConnection object
	 * @param tradeId trade id
	 * @return
	 */
	private Trade getTrade(final DSConnection dsCon, final long tradeId) {
		try {
			return dsCon.getRemoteTrade().getTrade(tradeId);
		} catch (CalypsoServiceException e) {
			Log.error(this, "Error getting trade.", e);
		}
		return null;
	}

	/**
	 * Set the account info in to trade Keywords
	 * 
	 * @param trade Trade
	 * @param xfer  BoTransfer
	 * @param dsCon DSConnection object
	 */
	private boolean setAccountKeywords(Trade trade, BOTransfer xfer, final DSConnection dsCon) {
		boolean ret=false;
		int id = xfer.getGLAccountNumber();

		try {
			if (id != 0) {
				Account acc = BOCache.getAccount(dsCon, id);
				if (acc != null) {
					if(id!=trade.getKeywordAsInt(ACCOUNT_ID) && !acc.getName().equals(trade.getKeywordValue(ACCOUNT_NAME))){
						trade.addKeyword(ACCOUNT_ID, id);
						trade.addKeyword(ACCOUNT_NAME, acc.getName());
						ret=true;
					}
				}
			}
		} catch (Exception e) {
			Log.error(this, "Error obtaining the Account", e);
		}
		return ret;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.calypso.tk.bo.workflow.WfTransferRule#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Updates CAClaims Trade Keyword.";
	}


}

