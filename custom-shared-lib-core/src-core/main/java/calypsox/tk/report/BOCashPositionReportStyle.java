/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.bo.Inventory;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

public class BOCashPositionReportStyle extends com.calypso.tk.report.BOCashPositionReportStyle {

	private static final long serialVersionUID = -7182936405056020890L;

	public static final String AGENT_NAME = "Agent Name";
	public static final String MCC_CONTRACT_NAME = "MCC Contract Name";
	public static final String MCC_CONTRACT_TYPE = "MCC Contract Type";
	public static final String MCC_REHYPOTHECABLE_COLLATERAL = "MCC Rehypothecable Collateral";
	public static final String MCC_HAIRCUT_TYPE = "MCC Haircut Type";
	public static final String CPTY_NAME = "Counterparty Full Name";
	public static final String CPTY_SHORT_NAME = "Counterparty Short Name";
	public static final String BALANCE = "Balance";
	

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

		Inventory inventory = (Inventory) row.getProperty(ReportRow.INVENTORY);
		
		CollateralConfig mcConfig = null;
		
		if (inventory == null) {
			throw new InvalidParameterException("Invalid row " + row + ". Cannot locate Inventory object");
		}

		try {
			//GSM v14 fix - now the MC id is not in getConfigId()
			mcConfig = (inventory.getMarginCallConfigId() == 0) ? null : CacheCollateralClient.getCollateralConfig(
					DSConnection.getDefault(), inventory.getMarginCallConfigId());
		} catch (Exception e) {
			Log.error(this, e);
		}

		if (AGENT_NAME.equals(columnId)) {
			if (inventory.getAgent() == null) {
				return "NONE";
			}
			return inventory.getAgent().getName();
			
		} else if (MCC_CONTRACT_NAME.equals(columnId)) {
			return (mcConfig == null) ? null : mcConfig.getName();
			
		} else if (MCC_CONTRACT_TYPE.equals(columnId)) {
			return (mcConfig == null) ? null : mcConfig.getContractType();
			
		} else if (MCC_REHYPOTHECABLE_COLLATERAL.equals(columnId)) {
			return (mcConfig == null) ? null : mcConfig.isRehypothecable();
			
		} else if (MCC_HAIRCUT_TYPE.equals(columnId)) {
			return (mcConfig == null) ? null : mcConfig.getHaircutType();
		} else if (CPTY_NAME.equals(columnId)) {
			return (mcConfig == null) ? null : mcConfig.getLegalEntity().getName();
		} else if (CPTY_SHORT_NAME.equals(columnId)) {
			return (mcConfig == null) ? null : mcConfig.getLegalEntity().getAuthName();
		}else if(BALANCE.equals(columnId)) {
			
		}

		return super.getColumnValue(row, columnId, errors);
	}

}
