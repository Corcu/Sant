/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.BOPositionReportTemplate;

public class SantInventoryMissingSecurityQuotesReportTemplate extends BOPositionReportTemplate {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String INVENTORY_MISSING_SECURITY = "INVENTORY_MISSING_SECURITY";
	public static final String PRICING_ENV = "Pricing Env";

	@Override
	public void callBeforeLoad() {
		super.callBeforeLoad();
	}

	//V14 Migration AAP
	public void setBOContext(BOPositionReportTemplateContext ctx){
		this.context = ctx;
	}
}
