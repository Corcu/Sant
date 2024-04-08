/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantListOfTradesWithNoMtmVariationReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = -1996092872124155398L;

	@Override
	public void setDefaults() {
		setColumns(SantListOfTradesWithNoMtmVariationReportStyle.DEFAULT_COLUMNS);
	}

}
