package calypsox.apps.reporting;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.position.impl.CollateralPositionLoaderUtil;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

public class CSDCptyPosFilter implements BOPositionFilter {

    private static final String CONTRACT_TYPE_CSD = "CSD";
    private static final String IM_CSD_TYPE = "IM_CSD_TYPE";
    private static final String CPTY = "CPTY";

    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        InventoryCashPositionArray result = new InventoryCashPositionArray();
        if ((positions != null) && (positions.size() > 0)) {
            for (int i = 0; i < positions.size(); i++) {
                InventoryCashPosition position = positions.get(i);
                if ("MARGIN_CALL".equals(position.getInternalExternal())) {
                    CollateralConfig config = getMarginCallConfig(position);
                    if (config != null &&
                            null != config.getContractType() && CONTRACT_TYPE_CSD.equals(config.getContractType()) &&
                            null != config.getAdditionalField(IM_CSD_TYPE) && CPTY.equals(config.getAdditionalField(IM_CSD_TYPE))) {
                        result.add(position);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public InventorySecurityPositionArray filterSecurity(InventorySecurityPositionArray positions) {
        InventorySecurityPositionArray result = new InventorySecurityPositionArray();
        if ((positions != null) && (positions.size() > 0)) {
            for (int i = 0; i < positions.size(); i++) {
                InventorySecurityPosition position = positions.get(i);
                if ("MARGIN_CALL".equals(position.getInternalExternal())) {
                    CollateralConfig config = getMarginCallConfig(position);
                    if (config != null &&
                            null != config.getContractType() && CONTRACT_TYPE_CSD.equals(config.getContractType()) &&
                            null != config.getAdditionalField(IM_CSD_TYPE) && CPTY.equals(config.getAdditionalField(IM_CSD_TYPE))) {
                        result.add(position);
                    }
                }
            }
        }
        return result;
    }

    private CollateralConfig getMarginCallConfig(Inventory position) {
        CollateralConfig result = null;
        result = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), CollateralPositionLoaderUtil.getCollateralConfigId(position));
        return result;
    }

}
