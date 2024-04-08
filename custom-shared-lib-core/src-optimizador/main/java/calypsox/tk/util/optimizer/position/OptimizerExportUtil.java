package calypsox.tk.util.optimizer.position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.util.gdisponible.GDisponibleUtil;
import calypsox.tk.util.gdisponible.SantGDBookInvSecPosKey;
import calypsox.tk.util.optimizer.position.OptimizerPositions.OptimizerPositionsKey;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;

public class OptimizerExportUtil {

	public static List<OptimizerExportPosition> buildExportPositions(
			List<Integer> securityIds) {
		return buildExportPositions(securityIds, null);
	}
	
	public static List<OptimizerExportPosition> buildExportPositions(
			List<Integer> securityIds, List<Integer> bookIds) {
		List<OptimizerPositions> listOptimizerPositions = get10BusinessDaysPositions(securityIds, bookIds);
		List<OptimizerExportPosition> optimizerExportPositions = new ArrayList<OptimizerExportPosition>();
		for (OptimizerPositions optimPos : listOptimizerPositions) {
			optimizerExportPositions.addAll(OptimizerPositionsFormatter
					.format(optimPos));
		}
		return optimizerExportPositions;
	}

	public static List<OptimizerPositions> get10BusinessDaysPositions(
			List<Integer> securityIds, List<Integer> bookIds) {
		List<OptimizerPositions> listPositions = new ArrayList<OptimizerPositions>();
		JDate date = JDate.getNow();
		Holiday hol = Holiday.getCurrent();
		int nbDaysToLoad = (int) JDate.diff(
				date,
				date.addBusinessDays(9,
						Util.string2Vector(GDisponibleUtil.SYSTEM_CAL))) + 1;

		HashMap<JDate, HashMap<SantGDBookInvSecPosKey, InventorySecurityPosition>> datesInvSecPositions = GDisponibleUtil
				.buildSecurityBookPositionsNbDays(securityIds, bookIds, null, Arrays.asList(
								InventorySecurityPosition.ACTUAL_TYPE,
								InventorySecurityPosition.THEORETICAL_TYPE),
						date.getJDatetime(TimeZone.getDefault()), nbDaysToLoad);

		if (datesInvSecPositions != null) {
			@SuppressWarnings("unchecked")
			Vector<JDate> sortedListKeys = Util.sort(datesInvSecPositions
					.keySet());
			HashMap<OptimizerPositionsKey, OptimizerPositions> positions = new HashMap<OptimizerPositionsKey, OptimizerPositions>();
			for (JDate keyDate : sortedListKeys) {
				if (!hol.isBusinessDay(keyDate,
						Util.string2Vector(GDisponibleUtil.SYSTEM_CAL))) {
					continue;
				}
				for (SantGDBookInvSecPosKey keyInv : datesInvSecPositions.get(
						keyDate).keySet()) {
					InventorySecurityPosition invSecPos = datesInvSecPositions
							.get(keyDate).get(keyInv);
					OptimizerPositionsKey optPosKey = new OptimizerPositionsKey(
							keyInv.getSecurityId(), keyInv.getBookId());
					OptimizerPositions optPositions = positions.get(optPosKey);
					if (optPositions == null) {
						optPositions = new OptimizerPositions(date,
								GDisponibleUtil.BSTE_PO, optPosKey);
						positions.put(optPosKey, optPositions);
					}
					 try {
                         optPositions.addPosition(invSecPos, keyDate);
                  }
                  catch(ArrayIndexOutOfBoundsException e) {
                         Log.system(OptimizerExportUtil.class.getName(), "invSecPos: "+invSecPos.toString() + e.toString());
                         throw e;
                  }
				}
			}

			for (OptimizerPositions optPositions : positions.values()) {
				optPositions.completePositions();
				if (optPositions.hasPositions()) {
					listPositions.add(optPositions);
				}
			}
		}
		return listPositions;
	}

}
