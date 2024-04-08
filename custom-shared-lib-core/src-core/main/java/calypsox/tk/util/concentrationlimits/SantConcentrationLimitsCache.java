package calypsox.tk.util.concentrationlimits;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

public class SantConcentrationLimitsCache {
	private Map<String, Boolean> staticDataFilterNamesMap = new HashMap<String, Boolean>();
	private Map<Integer, String> legalEntityNamesMap = new HashMap<Integer, String>();

	public void initSDFCache() {
		try {
			Vector<?> rawSDFNames = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilterNames();
			for (Object object : rawSDFNames) {
				if (object instanceof String) {
					String name = (String) object;
					setSDFPresent(name);
				}
			}
		} catch (CalypsoServiceException e) {
			Log.error(this, "Cannot retrieve Static Data Filter names from database", e);
		}
	}

	public void setSDFPresent(String name) {
		staticDataFilterNamesMap.put(name, true);
	}

	public boolean isSDFPresent(String name) {
		Boolean present = staticDataFilterNamesMap.get(name);

		return present != null && present.booleanValue();
	}

	public void addLegalEntityName(Integer entityId, String name) {
		legalEntityNamesMap.put(entityId, name);
	}

	public String getLegalEntityName(Integer entityId) {
		return legalEntityNamesMap.get(entityId);
	}
}
