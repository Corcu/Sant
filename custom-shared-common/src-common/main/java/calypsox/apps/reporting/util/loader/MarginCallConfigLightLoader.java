/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting.util.loader;

import calypsox.tk.core.MarginCallConfigLight;
import calypsox.tk.report.loader.SantLoader;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MarginCallConfigLightLoader extends SantLoader<Integer, String> {

    private final Map<Integer, MarginCallConfigLight> mccLightMap = new HashMap<Integer, MarginCallConfigLight>();

    public Map<Integer, String> load() {

        final Map<Integer, String> mccNamesMap = new HashMap<Integer, String>();
        try {
            final Vector<MarginCallConfigLight> mccLights = SantReportingUtil
                    .getSantReportingService(DSConnection.getDefault())
                    .getMarginCallConfigsLight();
            for (final MarginCallConfigLight mccLight : mccLights) {
                this.mccLightMap.put(mccLight.getId(), mccLight);
                mccNamesMap.put(mccLight.getId(), mccLight.getDescription() + " [" + mccLight.getId() + "]");
            }

        } catch (final RemoteException e) {
            Log.error(this, "Cannot load margin call contract light object", e);
        }
        return mccNamesMap;
    }

    public Map<Integer, MarginCallConfigLight> get() {
        return this.mccLightMap;
    }
}
