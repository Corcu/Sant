package calypsox.tk.event;

import java.util.ArrayList;
import java.util.Vector;

import com.calypso.helper.CoreAPI;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.MarginCallEventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import calypsox.tk.report.ECMSPledgeTradesAndPositionReport;

public class ExcludeNoECMSPledgedPositionsEventFilter extends MarginCallEventFilter {

	private final String ECMS_PLEDGE_ACCOUNTS = "ECMS_PLEDGE_ACCOUNTS";
	private final String DV_SEPARATOR = ";";

	@Override
	public boolean accept(PSEvent event) {
		boolean result = super.accept(event);
		Account accountName;
		if (!(event instanceof PSEventTransfer)) {
			Log.debug(this, "ACCEPT: " + event.getClass().getSimpleName());
			return true;
		} else {
			PSEventTransfer et = (PSEventTransfer) event;
			BOTransfer transfer = et.getBoTransfer();

			if (transfer == null) {
				Log.debug(this, "REJECT: No transfer found in PSEvent.");
				return false;
			} else {
				Log.debug(this, "INFO: Transfer " + CoreAPI.getId(transfer));
				String mc_id = transfer.getAttribute("MarginCall");
				if (!Util.isEmpty(mc_id) && et.getTrade() != null) {
					Trade tr = et.getTrade();
					Product p = tr.getProduct();
					if (p instanceof MarginCall) {
						if ("ECMS".equals(((MarginCall) p).getMarginCallConfig().getContractType())
								&& ECMSPledgeTradesAndPositionReport.Y_VALUE.equalsIgnoreCase(
										tr.getKeywordValue(ECMSPledgeTradesAndPositionReport.IS_ECMS_PLEDGE))) {

							ArrayList<String> pledgeAccounts = getEcmsAccounts();

							accountName = BOCache.getAccount(DSConnection.getDefault(), transfer.getGLAccountNumber());

							if (!pledgeAccounts.contains(accountName.getName()))
								return false;
						}
					}
				}
				return result;
			}
		}
	}

	private ArrayList<String> getEcmsAccounts() {
		ArrayList<String> accountsFinal = new ArrayList<>();
		Vector<String> pledgeAccounts = LocalCache.getDomainValues(DSConnection.getDefault(), ECMS_PLEDGE_ACCOUNTS);

		for (String account : pledgeAccounts) {

			accountsFinal.add(account.split(DV_SEPARATOR)[1]);
		}

		return accountsFinal;
	}
}
