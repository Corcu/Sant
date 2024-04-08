/**
 * 
 */
package calypsox.tk.bo;

import java.util.Vector;

import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

/**
 * Custom SDISelector for margin call trades
 * 
 * @author aela
 * 
 */
@SuppressWarnings("rawtypes")
public class InterestBearingSDISelector extends com.calypso.tk.bo.InterestBearingSDISelector {

	@Override
	public Vector getValidSDIList(Trade trade, TradeTransferRule transfer, JDate settleDate, String legalEntity,
			String legalEntityRole, Vector exceptions, boolean includeNotPreferred, DSConnection dsCon) {

		Vector validSDI = super.getValidSDIList(trade, transfer, settleDate, legalEntity, legalEntityRole, exceptions,
				includeNotPreferred, dsCon);
		//
		Product product = trade.getProduct();

		if (validSDI != null && validSDI.size() > 0 && product instanceof InterestBearing) {
			InterestBearing ib = (InterestBearing) product;
			int ibAccountId = ib.getAccountId();
			for (int i = 0; i < validSDI.size(); i++) {
				SettleDeliveryInstruction sdi = (SettleDeliveryInstruction) validSDI.get(i);

				if (sdi != null && sdi.getDirectRelationship() && transfer.getCounterPartyRole().equals("Client")) {

					if (ibAccountId != sdi.getGeneralLedgerAccount()) {
						// Account sdiAccount = BOCache.getAccount(dsCon,
						// sdi.getGeneralLedgerAccount());
						// Account ibAccount = BOCache.getAccount(dsCon,
						// ibAccountId);
						// if (sdiAccount != null && ibAccount != null) {
						// int sdiMCCId = getMCCIdFromAccount(sdiAccount);
						// int ibMCCId = getMCCIdFromAccount(ibAccount);
						// if (sdiMCCId > 0 && ibMCCId > 0 && sdiMCCId !=
						// ibMCCId) {
						validSDI.remove(sdi);
						// }
						// }
					}
				}
			}
		}
		return validSDI;
	}

	/**
	 * @param account
	 * @return the margin call config id from the given account
	 */
	int getMCCIdFromAccount(Account account) {
		int mccId = -1;
		String mccIdValue = account.getAccountProperty("MARGIN_CALL_CONTRACT");
		try {
			mccId = Integer.parseInt(mccIdValue);

		} catch (NumberFormatException e) {
			mccId = -1;
			Log.error(this, e);
		}

		return mccId;
	}
}
