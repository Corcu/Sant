package calypsox.apps.reporting;

/**
 * @author x957355
 */
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;


@SuppressWarnings("deprecation")
public class ECMSMrgCallExtracontableFilter implements BOPositionFilter {
	
	private static final String MARGIN_CALL         = "MARGIN_CALL";
	private static final String ECMS                = "ECMS";
    private static final String ACCOUNTING_SECURITY = "ACCOUNTING_SECURITY";
	
	 @Override
	    public InventoryCashPositionArray filterCash(InventoryCashPositionArray positions) {
	        InventoryCashPositionArray result = new InventoryCashPositionArray();
	        if ((positions != null) && (positions.size() > 0)) {
	            for (int i = 0; i < positions.size(); i++) {
	                InventoryCashPosition position = positions.get(i);
	                if (MARGIN_CALL.equals(position.getInternalExternal())) {
	                    CollateralConfig cc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), position.getMarginCallConfigId());
	                    if (cc != null && isAccountingSecurityEnabled(cc) && isECMSContractType(cc)){
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
	                if (MARGIN_CALL.equals(position.getInternalExternal())) {
	                    CollateralConfig cc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), position.getMarginCallConfigId());
	                    if (cc != null && isAccountingSecurityEnabled(cc) && isECMSContractType(cc)){
	                        result.add(position);
	                    }
	                }
	            }
	        }
	        return result;
	    }

	    
	    private boolean isAccountingSecurityEnabled(CollateralConfig config){
	        return Boolean.parseBoolean(config.getAdditionalField(ACCOUNTING_SECURITY));
	    }
	    private boolean isECMSContractType(CollateralConfig config) {
	    	return config.getContractType() != null && config.getContractType().equals(ECMS);
	    	
	    }


}


