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
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.DateFormattingUtil;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class SusiDisponibleRF implements BOPositionFilter {

    private static final String POSITION_EXPORT_BOND_DATE = "POSITION_EXPORT_BOND_DATE";

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
        MarginCallConfig contract =  CollateralCacheUtil.getMarginCallConfig(DSConnection.getDefault(), pos.getMarginCallConfigId());
        if (contract == null){
            return false;
        }

        if (!contract.isTriParty()) {
            return false;
        }

        if (!(pos.getProduct() instanceof Bond))  {
            return false;
        }

        String strMigDate = contract.getAdditionalField(POSITION_EXPORT_BOND_DATE);
        if (!Util.isEmpty(strMigDate))  {
            if ( JDate.valueOf(strMigDate).lte(pos.getPositionDate())) {
                return true;
            }
        }

        return false;
    }
}