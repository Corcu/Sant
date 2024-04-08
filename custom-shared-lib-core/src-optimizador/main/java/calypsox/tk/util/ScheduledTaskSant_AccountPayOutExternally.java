/**
 * 
 */
package calypsox.tk.util;

import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import calypsox.tk.report.loader.CallAccountLoader;

/**
 * @author aalonsop
 * @description Gets all the Call Accounts which handle interests and checks or
 *              unchecks the PayOutExternally attribute
 */
public class ScheduledTaskSant_AccountPayOutExternally extends ScheduledTask {

	@Override
	public boolean process(DSConnection ds, PSConnection ps) {
		_executeB=false;
		Map<Integer, Account> callAccountsMap;
		final CallAccountLoader accountLoader = new CallAccountLoader();
		accountLoader.load();
		callAccountsMap = accountLoader.get();
		Vector<Account> accounts = new Vector<>();
		int i = 0;
		for (Account acc : callAccountsMap.values()) {
			if (acc.getDestinationAccountId() == 0 && !acc.getPayOutExternallyB()) {
				acc.setPayOutExternallyB(true);
				accounts.addElement(acc);
				i++;
			}
		}
		try {
			DSConnection.getDefault().getRemoteAccounting().saveAccounts(accounts);
		} catch (CalypsoServiceException e) {
			Log.error(this, "Couldn't update CallAccounts \n" + e); //sonar
		}
		Log.info(this,"Processed Accounts -----> " + i);
		_executeB=true;
		return _executeB;
	}
	@Override
	public String getTaskInformation() {
		return "Sets PayOutExternally to true in every system CallAccount";
	}
}
