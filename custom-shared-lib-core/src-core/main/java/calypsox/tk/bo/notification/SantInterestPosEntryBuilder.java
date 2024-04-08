/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.calypso.tk.core.JDate;
import com.calypso.tk.product.InterestBearingEntry;

public class SantInterestPosEntryBuilder {

	public Map<JDate, InterestPosEntry> mergeEntries(List<InterestBearingEntry> entries) {
		final Map<JDate, InterestPosEntry> m = new HashMap<JDate, InterestPosEntry>();
		if (entries == null) {
			return m;
		}

		for (final InterestBearingEntry entry : entries) {
			if (InterestBearingEntry.INTEREST.equals(entry.getEntryType())) {
				InterestPosEntry intPos = m.get(entry.getEntryDate());
				if (intPos == null) {
					intPos = new InterestPosEntry();
					m.put(entry.getEntryDate(), intPos);
				}
				intPos.setInterest(entry);
			} else if (InterestBearingEntry.POSITION.equals(entry.getEntryType())) {
				InterestPosEntry intPos = m.get(entry.getEntryDate());
				if (intPos == null) {
					intPos = new InterestPosEntry();
					m.put(entry.getEntryDate(), intPos);
				}
				intPos.setPos(entry);
			} else if (InterestBearingEntry.ADJUSTMENT.equals(entry.getEntryType())) {
				InterestPosEntry intPos = m.get(entry.getEntryDate());
				if (intPos == null) {
					intPos = new InterestPosEntry();
					m.put(entry.getEntryDate(), intPos);
				}
				intPos.setAdjustment(entry);
			} else if (InterestBearingEntry.PARTIAL_SETTLE.equals(entry.getEntryType())) {
				InterestPosEntry intPos = m.get(entry.getEntryDate());
				if (intPos == null) {
					intPos = new InterestPosEntry();
					m.put(entry.getEntryDate(), intPos);
				}
				intPos.setPartialSettle(entry);
			}

		}

		return m;
	}
}
