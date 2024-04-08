/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting.util.loader;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;

public class CallAccountLoader extends SantLoader<Integer, String> {

	private final Map<Integer, Account> callAccountsMap = new HashMap<Integer, Account>();

	@SuppressWarnings("unchecked")
	public Map<Integer, String> load() {

		final Map<Integer, String> callAcountNamesMap = new HashMap<Integer, String>();
		try {
			final Vector<Account> accounts = DSConnection.getDefault().getRemoteAccounting().getAccounts(false);
			for (final Account acc : accounts) {
				if (acc.getCallAccountB()) {
					this.callAccountsMap.put(acc.getId(), acc);
					callAcountNamesMap
							.put(acc.getId(), acc.getId() + " - " + acc.getName() + " - " + acc.getCurrency());
				}
			}

		} catch (final RemoteException e) {
			Log.error(this, "Cannot load accounts", e);
		}
		return callAcountNamesMap;
	}

	public Map<Integer, Account> get() {
		return this.callAccountsMap;
	}

}
