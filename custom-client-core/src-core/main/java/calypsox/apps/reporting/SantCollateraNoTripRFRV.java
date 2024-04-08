/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.CollateralCacheUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.ProductConst;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

@SuppressWarnings("deprecation")
public class SantCollateraNoTripRFRV implements BOPositionFilter {

    private static final String ACCOUNTING_SECURITY = "ACCOUNTING_SECURITY";
    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {

        InventorySecurityPositionArray susiPositions = new InventorySecurityPositionArray();

        for (int i = 0; i < positions.size(); i++) {
            InventorySecurityPosition pos = positions.get(i);
            if (isValidPosition(pos)) {
                susiPositions.add(pos);
            }
        }
        return susiPositions;
    }

    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        return positions;
    }

    private boolean isValidPosition(InventorySecurityPosition pos) {
        MarginCallConfig contract =  CollateralCacheUtil.getMarginCallConfig(DSConnection.getDefault(), pos.getMarginCallConfigId());
        if (contract == null){
            return false;
        }

        // release-COL.6.9.0 - BONDCOLLAT Bilaterales
        if (!contract.isTriParty() && !"CSD".equalsIgnoreCase(contract.getContractType())) {
            return "true".equalsIgnoreCase(contract.getAdditionalField(ACCOUNTING_SECURITY));
        }
        return false;

    }

}