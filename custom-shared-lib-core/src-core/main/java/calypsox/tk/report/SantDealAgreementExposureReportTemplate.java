/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

@SuppressWarnings("serial")
public class SantDealAgreementExposureReportTemplate extends MarginCallReportTemplate {

	@Override
	public void setDefaults() {
		super.setDefaults();

		setColumns(SantDealAgreementExposureReportStyle.DEFAULTS_COLUMNS);
	}
}
