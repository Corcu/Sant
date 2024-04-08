package calypsox.util;

import java.util.Vector;

import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

/**
 * Several utilities for Calypso Collateral in Banco Santander
 * 
 * @author several authors
 * @version 4.0
 * 
 */
public class CollateralUtilitiesMadrid {



	/**
	 * Get domain values for a domain name
	 * 
	 * @param domainName
	 * @return
	 */
	public static Vector<String> getDomainValues(String domainName) {
		return LocalCache.getDomainValues(DSConnection.getDefault(), domainName);
	}

	
}