/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import java.util.Vector;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

@SuppressWarnings("deprecation")
public class SusiPositionView implements BOPositionFilter {

    private static final String INV_CALMARGINCALL_AGENT = "INV_CALMARGINCALL_AGENT";

    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {

        InventorySecurityPositionArray susiPositions = new InventorySecurityPositionArray();

        for (int i = 0; i < positions.size(); i++) {
            InventorySecurityPosition pos = positions.get(i);
            if (isSusiPosition(pos)) {
                susiPositions.add(pos);
            }
        }
        return susiPositions;
    }

    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        return positions;
    }

    private boolean isSusiPosition(InventorySecurityPosition pos) {
        Vector<String> calMarginCallAgents = LocalCache.getDomainValues(DSConnection.getDefault(),
                INV_CALMARGINCALL_AGENT);

        if (!Util.isEmpty(calMarginCallAgents) && (pos.getAgent() != null)
                && !calMarginCallAgents.contains(pos.getAgent().getCode())) {
            return true;
        }
        return false;
    }

}
