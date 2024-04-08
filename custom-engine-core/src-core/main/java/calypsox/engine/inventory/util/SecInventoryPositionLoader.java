/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.engine.inventory.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.TimeZone;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;

import calypsox.engine.inventory.SantPositionBean;
import calypsox.engine.inventory.SantPositionConstants;
import calypsox.tk.util.gdisponible.GDisponibleUtilTemp;
import calypsox.tk.util.gdisponible.SantGDInvSecPosKey;

/**
 * Fetchs internal positions: actual, theoretical, pledge & unavailable
 * 
 * @author Patrice Guerrido & Guillermo Solano
 * @version 2.0
 * @date 23/01/2014
 * 
 */
public class SecInventoryPositionLoader {

	/**
	 * 
	 * @param santUpdatePos
	 * @param isBOPosition
	 * @param isBloqueo
	 * @return
	 * @throws Exception
	 */
	public static double fecthInternalPosition(final SantPositionBean santUpdatePos, final boolean isBloqueo)
			throws Exception {

		HashMap<JDate, HashMap<SantGDInvSecPosKey, InventorySecurityPosition>> posDays = GDisponibleUtilTemp
				.buildSecurityPositionsNbDays(
						-1,
						Arrays.asList(santUpdatePos.getSecurity().getId()),
						Arrays.asList(santUpdatePos.getBook().getId()),
						Arrays.asList(santUpdatePos.getAgent().getId()),
						Arrays.asList(santUpdatePos.getAccount().getId()),
						null,
						Arrays.asList(SantPositionConstants.BLOQUEO.equals(santUpdatePos.getPositionType()) ? SantPositionConstants.ACTUAL
								: santUpdatePos.getPositionType()),
						santUpdatePos.getPositionDate().getJDatetime(TimeZone.getDefault()), 1);

		if ((posDays.get(santUpdatePos.getPositionDate()) == null)
				|| Util.isEmpty(posDays.get(santUpdatePos.getPositionDate()).keySet())) {
			return 0;
		}

		if (posDays.get(santUpdatePos.getPositionDate()).keySet().size() > 1) {
			throw new Exception("More than one position requested. The positions table is incoherent.");
		}

		HashMap<SantGDInvSecPosKey, InventorySecurityPosition> posDay = posDays.get(santUpdatePos.getPositionDate());

		// only one position
		for (SantGDInvSecPosKey key : posDay.keySet()) {
			if (posDay.get(key) == null) {
				throw new Exception("Position requested is null, doing nothing.");
			} else {
				if (isBloqueo) {
					return posDay.get(key).getTotalPledgedOut();
				} else {
					return posDay.get(key).getTotalSecurity();
				}
			}
		}
		throw new Exception("Position requested is null, doing nothing.");
	}
}
