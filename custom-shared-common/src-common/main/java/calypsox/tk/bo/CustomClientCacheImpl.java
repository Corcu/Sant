package calypsox.tk.bo;

import java.util.HashMap;
import java.util.Vector;

import com.calypso.tk.bo.CustomClientCache;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.event.PSEventQuote;
import com.calypso.tk.event.PSEventQuoteRemoved;
import com.calypso.tk.refdata.GlobalRatingConfiguration;
import com.calypso.tk.refdata.Haircut;
import com.calypso.tk.service.DSConnection;

public class CustomClientCacheImpl implements CustomClientCache {

	private static String LOG_CAT = "CustomClientCacheImpl";
	private static HashMap<String, Vector<Haircut>> _haircutCache = null;

	static {
		Log.system(LOG_CAT, "### - CustomClientCacheImpl instantiated successfully.");
	}

	private static GlobalRatingConfiguration globalRatingconfiguration = null;

	public static GlobalRatingConfiguration getGlobalRatingConfiguration()  throws Exception { 
		if (globalRatingconfiguration == null) {
			globalRatingconfiguration = ServiceRegistry.getDefault().getCollateralDataServer()
					.loadDefaultGlobalRatingConfiguration();	
		}
		return globalRatingconfiguration;
	}

	private static HashMap<String, Vector<Haircut>> getHaircutCache() { 
		if (_haircutCache == null) {
			_haircutCache  = new HashMap<String, Vector<Haircut>>();
		}
		return _haircutCache;
	}

	@Override
	public void clear() {
		Log.system(LOG_CAT, "Clear command received. Cache will be cleaned.");
		synchronized (this) {
			_haircutCache  = null;
			globalRatingconfiguration = null;
		}
	}

	@Override
	public void newEvent(DSConnection paramDSConnection, PSEventDomainChange evt) {
		switch (evt.getType())  { 
		case PSEventDomainChange.HAIRCUT :
			processHaitcutEvent(evt);	
			return;
		case PSEventDomainChange.RATING_VALUE :
			processRatingValueEvent(evt);
			return;
		}
		
	}
	
	private void processRatingValueEvent(PSEventDomainChange evt) {
		synchronized (this) {
			globalRatingconfiguration = null;
		}
	}

	private void processHaitcutEvent(PSEventDomainChange evt) {
		if (!getHaircutCache().containsKey(evt.getValue())) {
			return;
		}
		synchronized (this) {
			_haircutCache.remove(evt.getValue());
		}
	}

	public Vector<Haircut> getHaircuts(String ruleName) {
		Vector<Haircut> v = null;
		if (getHaircutCache().containsKey(ruleName)) {
			v  = getHaircutCache().get(ruleName);
		} else {
			v = getHaircutFromDB(ruleName);
			synchronized(this) {
				_haircutCache.put(ruleName, v);
			}
		} 
		return  v;
	}

	@SuppressWarnings({ "unchecked" })
	private Vector<Haircut> getHaircutFromDB(String haircutRuleName) {
		try {
			return DSConnection.getDefault().getRemoteReferenceData()
					.getHaircuts(haircutRuleName);
		} catch (CalypsoServiceException e) {
			Log.error(LOG_CAT, "Error retrieving Haircut for rule:" + haircutRuleName , e);
		}
		return null;
	}

	@Override
	public void newEvent(DSConnection paramDSConnection, PSEventQuote evt) {
		// Do nothing
	}

	@Override
	public void newEvent(DSConnection paramDSConnection, PSEventQuoteRemoved evt) {
		// Do nothing
	}

}
