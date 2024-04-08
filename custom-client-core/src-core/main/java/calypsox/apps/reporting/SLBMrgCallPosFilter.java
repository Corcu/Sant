package calypsox.apps.reporting;

import calypsox.tk.report.extracontable.filter.MrgCallContractPOFilter;
import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

/**
 * @author aalonsop
 */
public class SLBMrgCallPosFilter implements BOPositionFilter {

    @Override
    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
        InventoryCashPositionArray result = new InventoryCashPositionArray();
        if ((positions != null) && (positions.size() > 0)) {
            for (int i = 0; i < positions.size(); i++) {
                InventoryCashPosition position = positions.get(i);
                if ("MARGIN_CALL".equals(position.getInternalExternal())) {
                    MrgCallContractPOFilter filter=new MrgCallContractPOFilter();
                    CollateralConfig config = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), position.getMarginCallConfigId());
                    if (config != null && isAccountingSecurityEnabled(config) && isEnableTriparty(config)
                            && filter.isPOEnabledForTripartyExtracontable(config)){
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
                    MrgCallContractPOFilter filter=new MrgCallContractPOFilter();
                    CollateralConfig config = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), position.getMarginCallConfigId());
                    if (config != null &&
                        isAccountingSecurityEnabled(config) && isEnableTriparty(config)
                            && filter.isPOEnabledForTripartyExtracontable(config)){
                            result.add(position);
                    }
                }
            }
        }
        return result;
    }

    private boolean isAccountingSecurityEnabled(CollateralConfig config){
        return Boolean.parseBoolean(config.getAdditionalField("ACCOUNTING_SECURITY"));
    }

    private boolean isEnableTriparty(CollateralConfig config){
        return config.isTriParty();
    }

}
