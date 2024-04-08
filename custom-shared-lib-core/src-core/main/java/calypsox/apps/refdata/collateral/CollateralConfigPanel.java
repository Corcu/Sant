package calypsox.apps.refdata.collateral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

public class CollateralConfigPanel extends com.calypso.apps.refdata.collateral.CollateralConfigPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String GROUP_IM = "IM";
	private static final String IM_GLOBAL_ID = "IM_GLOBAL_ID";
	private static final String IM_SUB_CONTRACTS = "IM_SUB_CONTRACTS";
	
	
	@Override
	protected void saveMargin(boolean isSaveAsNew, int iaParentId) {

		super.saveMargin(isSaveAsNew, iaParentId);

		if(GROUP_IM.equalsIgnoreCase(this._mcc.getContractGroup()) 
		   && !CollateralConfig.SUBTYPE_FACADE.equalsIgnoreCase(this._mcc.getSubtype())){
			String fatherid = this._mcc.getAdditionalField(IM_GLOBAL_ID);
			if(!fatherid.equals("") && this._mcc != null){
				setSubContract(this._mcc.getId(),Integer.valueOf(fatherid));
			}
		}
	}
	
	public void setSubContract (int idsub, int idfather){

		CollateralConfig father = new CollateralConfig();
		try {
			father = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(idfather);
		} catch (Exception e) {
			Log.error(this, e); //sonar purpose
			Log.error(this, "Couldn't get the contract: " + e.getMessage());
		}
		if(null!=father){
			String existingChildren = father.getAdditionalField(IM_SUB_CONTRACTS);
			
			Map<String, String> newValue = new HashMap<String, String>();
			String childId = String.valueOf(idsub);

			// if children is not empty
			if (!Util.isEmpty(existingChildren)) {
				// if childId not in existingChildren, we add it
				if (!childId.equals("0") && !existingChildren.contains(childId)) {
					StringBuilder newChildren = new StringBuilder(existingChildren);
					newChildren.append(',').append(childId);
					newValue.put(IM_SUB_CONTRACTS, newChildren.toString());
				}
				// if already exists, do nothing
			} else {
				// if empty, we add the new child id
				newValue.put(IM_SUB_CONTRACTS, String.valueOf(idsub));
			}

			// add additional field with new value and save the contract
			(father.getAdditionalFields()).putAll(newValue);
			saveCollateralConfig(father);
		}
	}
	
	private void saveCollateralConfig(CollateralConfig contractToSave) {
		if(null!=contractToSave){
			try {
				contractToSave = (CollateralConfig) DSConnection.getDefault().getRemoteReferenceData()
						.applyNoSave(contractToSave);
				if (contractToSave.isValid(new ArrayList<String>())) {
					ServiceRegistry.getDefault().getCollateralDataServer().save(contractToSave);
				}
				
			} catch (CalypsoServiceException e) {
				Log.error(this, e); //sonar purpose
				Log.error(this, "Couldn't apply action Update on old Parent Contract: " + e.getMessage());
			} catch (CollateralServiceException e) {
				Log.error(this, e); //sonar purpose
				Log.error(this, "Couldn't save old Parent Contract: " + e.getMessage());
			}
		}
		
	}
	
	
	
	

}
