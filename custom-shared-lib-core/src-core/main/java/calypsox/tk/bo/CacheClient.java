package calypsox.tk.bo;

import java.rmi.RemoteException;

import com.calypso.tk.refdata.WithholdingTaxConfig;
import com.calypso.tk.service.DSConnection;

public class CacheClient extends com.calypso.tk.bo.CacheClient {

	public WithholdingTaxConfig getWithholdingTaxConfig(DSConnection ds, int configId) {

		if (!__initialized) {
			initCaches();
		}
		WithholdingTaxConfig wtc = null;
		if (!_isCaching) {
			try {
				wtc = ds.getRemoteReferenceData().getWithholdingTaxConfig(configId);
			} catch (RemoteException var5) {
				throw new RuntimeException(var5);
			}
		} else {
			if (_withholdingTaxConfigs.isEmpty()) {
				this.getWithholdingTaxConfigs(ds);
			}
			wtc = (WithholdingTaxConfig) _withholdingTaxConfigs.get((long) configId);
		}
		return wtc;
	}

}