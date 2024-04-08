/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportTemplate;

@SuppressWarnings("serial")
public class SantGenericAgreementExposureReportTemplate extends MarginCallReportTemplate {

	@Override
	public void setDefaults() {
		super.setDefaults();

		setColumns(SantGenericAgreementExposureReportStyle.DEFAULTS_COLUMNS);
	}
}
