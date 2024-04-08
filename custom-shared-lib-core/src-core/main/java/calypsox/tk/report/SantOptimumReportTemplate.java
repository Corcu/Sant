/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class SantOptimumReportTemplate extends ReportTemplate {

	private static final long serialVersionUID = 1L;

	public static final String OPTIMIZATION_REPORT = "Optimization Report";

	public static final String OPTIMIZATION_CONFIGURATION = "Optimization";
	public static final String CONTRACT_TYPE = "ContractType";
	public static final String REASSIGN_CATEGORIES = "ReassignCategories";
	public static final String USE_CACHE = "UseCache";

	public static final String[] DEFAULTS_COLUMNS = { SantOptimumReportStyle.MC_CONTRACT_NAME,
			SantOptimumReportStyle.CATEGORY_NAME, SantOptimumReportStyle.ELIGIBLE_CATEGORY,
			SantOptimumReportStyle.CATEGORY_TYPE, SantOptimumReportStyle.SUM_OF_CONTRACT_VALUES,
			SantOptimumReportStyle.MC_CONTRACT_BASE_CURRENCY };

	@Override
	public void setDefaults() {
		setColumns(DEFAULTS_COLUMNS);
	}

}
