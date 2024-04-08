package calypsox.tk.util.emir;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class LegalEntitiesCache {
    private static LegalEntitiesCache instance = null;
    private Map<String, LegalEntity> leByCode = null;

    private LegalEntitiesCache() {
        leByCode = new HashMap<String, LegalEntity>();
    }

    public static LegalEntitiesCache getInstance() {
        if (instance == null) {
            instance = new LegalEntitiesCache();
        }

        return instance;
    }

    public LegalEntity getLegalEntity(String code) {
        LegalEntity le = leByCode.get(code);

        if (le == null) {
            try {
                le = DSConnection.getDefault().getRemoteReferenceData()
                        .getLegalEntity(code);
                if (le != null) {
                    leByCode.put(code, le);
                }
            } catch (RemoteException e) {
                Log.error(this,
                        "Could not retrieve Legal Entity with code " + code, e);
            }
        }

        return le;
    }
}
