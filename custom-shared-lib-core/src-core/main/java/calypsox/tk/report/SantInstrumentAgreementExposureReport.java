/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import calypsox.tk.report.agrexposure.SantMCDetailEntryLight;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallDetailEntry;

@SuppressWarnings("serial")
public class SantInstrumentAgreementExposureReport extends SantCounterpartyAgreementExposureReport {
	public static final String TYPE = "SantInstrumentAgreementExposure";

	@Override
	public String getKey(SantMarginCallDetailEntry entry) {
		String key = "";
		if (entry == null) {
			return null;
		}

		key = entry.getMarginCallConfig().getName() + "-" + getInstrument(entry.getTrade());
		return key;
	}

	@Override
	public String getKey(SantMCDetailEntryLight entry) {
		String key = "";
		if (entry == null) {
			return null;
		}

		key = entry.getAgreementName() + "-" + entry.getInstrument();
		return key;
	}

}