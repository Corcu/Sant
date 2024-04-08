package calypsox.apps.reporting;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.CollateralCacheUtil;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

public class SantAnacreditEligible implements BOPositionFilter {

    private static final String ANACREDIT = "ANACREDIT";
    private static final String ACCOUNTING_SECURITY = "ACCOUNTING_SECURITY";

    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {

        InventorySecurityPositionArray filteredPositions = new InventorySecurityPositionArray();

        for (int i = 0; i < positions.size(); i++) {
            InventorySecurityPosition pos = positions.get(i);
            if (isValidPosition(pos)) {
                filteredPositions.add(pos);
            }
        }
        return filteredPositions;
    }

    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {

        InventoryCashPositionArray filteredPositions = new InventoryCashPositionArray();

        for (int i = 0; i < positions.size(); i++) {
            InventoryCashPosition pos = positions.get(i);
            MarginCallConfig contract =  CollateralCacheUtil.getMarginCallConfig(DSConnection.getDefault(), pos.getMarginCallConfigId());
            if (isAnacreditContract(contract)) {
                filteredPositions.add(pos);
            }
        }
        return filteredPositions;
    }

    private boolean isValidPosition(InventorySecurityPosition pos) {
        MarginCallConfig contract =  CollateralCacheUtil.getMarginCallConfig(DSConnection.getDefault(), pos.getMarginCallConfigId());
        if (contract == null){
            return false;
        }
        return isAnacreditContract(contract);
    }


    private boolean isAnacreditContract(MarginCallConfig mcc) {
        if (mcc == null){
            return false;
        }
        return "True".equalsIgnoreCase(mcc.getAdditionalField(ANACREDIT))
                &&   "true".equalsIgnoreCase(mcc.getAdditionalField(ACCOUNTING_SECURITY));
    }
}


