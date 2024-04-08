/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

@SuppressWarnings("serial")
public class SantInstrumentAgreementExposureReportTemplate extends MarginCallReportTemplate {

	@Override
	public void setDefaults() {
		super.setDefaults();

		setColumns(SantInstrumentAgreementExposureReportStyle.DEFAULTS_COLUMNS);
	}
}
