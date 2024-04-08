/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import com.calypso.apps.reporting.BOPositionFilter;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.dto.MarginCallPositionDTO;
import com.calypso.tk.core.CollateralCacheUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.ProductConst;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.DateFormattingUtil;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.text.SimpleDateFormat;
import java.time.chrono.JapaneseDate;
import java.util.*;

@SuppressWarnings("deprecation")
public class SusiDisponibleRV implements BOPositionFilter {

	private static final String POSITION_EXPORT_EQUITY_DATE = "POSITION_EXPORT_EQUITY_DATE";
	private static final String ATTR_CONTRACT_TYPE = "SusiDisponibleRV.contractTypes" ;

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

			// Triparty y Bilaterales
			if (!(pos.getProduct() instanceof Equity))  {
				return false;
			}

			if (!isValidContractType(contract))  {
				return false;
			}

			if ("CSD".equalsIgnoreCase(contract.getContractType())) {
				if ((pos.getTotal() >= 0)) {
					return false;
				}
			}

		String strMigDate = contract.getAdditionalField(POSITION_EXPORT_EQUITY_DATE);
		if (!Util.isEmpty(strMigDate))  {
			if ( JDate.valueOf(strMigDate).lte(pos.getPositionDate())) {
				return true;
			}
		}

		return false;
	}

	protected  boolean isValidContractType(MarginCallConfig config) {

		if (!config.isTriParty()) {
			if ("CSA".equalsIgnoreCase(config.getContractType())) {
				return true;
			}
		}
		return getContractTypes().contains(config.getContractType());
	}

	private static List<String> getContractTypes(){

		List<String> result = LocalCache.getDomainValues(DSConnection.getDefault(), ATTR_CONTRACT_TYPE);
		return !Util.isEmpty(result) ? result : Arrays.asList("OSLA","CSD","ISMA");
	}


}
