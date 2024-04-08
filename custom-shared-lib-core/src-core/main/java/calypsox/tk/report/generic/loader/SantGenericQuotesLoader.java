/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.loader;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;

public class SantGenericQuotesLoader extends SantEnableThread<Integer, QuoteValue> {

	private final List<String> whereList;

	public SantGenericQuotesLoader(boolean enableThreading, List<String> whereList) {
		super(enableThreading);
		this.whereList = whereList;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void loadData() {
		try {
			for (String where : this.whereList) {
				this.dataList.addAll(DSConnection.getDefault().getRemoteMarketData().getQuoteValues(where));
			}
		} catch (final RemoteException e) {
			Log.error(this, "Cannot load quotes", e);
		}
	}

	@Override
	protected Map<Integer, QuoteValue> getDataMapFromDataList() {
		for (QuoteValue qv : this.dataList) {
			this.dataMap.put(qv.getId(), qv);
		}
		return this.dataMap;
	}

	public QuoteValue fetchQuoteValue(final String quoteName) {
		for (final QuoteValue qv : this.dataList) {
			if (qv.getName().equals(quoteName)) {
				return qv;
			}
		}
		return null;
	}

	public QuoteValue fetchFXQuoteValue(String ccy1, String ccy2) {
		String fxQuoteName = "FX." + ccy1 + "." + ccy2;

		QuoteValue qv = fetchQuoteValue(fxQuoteName);
		if (qv == null) {
			fxQuoteName = "FX." + ccy2 + "." + ccy1;
			qv = fetchQuoteValue(fxQuoteName);
		}
		return qv;

	}
}
