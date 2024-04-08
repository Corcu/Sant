/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.kpidailytask;

import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Amount;

public class DailyTaskUtil {

	public static String getEvent() {
		return "Margin Call";
	}

	public static Amount getIndependent(SantMarginCallEntry santEntry) {
		return new Amount(santEntry.getSumIndepAmountBase(), 2);
	}

	public static Amount getThreshold(MarginCallEntryDTO entryDTO) {
		return new Amount(Math.abs(entryDTO.getThresholdAmount()), 2);
	}

	public static String getMarginCallSituation(MarginCallEntryDTO entryDTO) {
		return "PRICING".equals(entryDTO.getStatus()) ? "Pending" : "Valid";
	}
}
