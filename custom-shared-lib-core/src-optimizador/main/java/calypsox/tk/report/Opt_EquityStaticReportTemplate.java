/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.core.Product;
import com.calypso.tk.product.EquityBase;
import com.calypso.tk.report.EquityReportTemplate;

public class Opt_EquityStaticReportTemplate<P extends Product & EquityBase> extends EquityReportTemplate<P> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(Opt_EquityStaticReportStyle.DEFAULTS_COLUMNS);
	}

}
