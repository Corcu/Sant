/*
*
* Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
* All rights reserved.
* 
*/
/**
 * 
 */
package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.MarginCallEntryReport;
import com.calypso.tk.report.ReportOutput;

/**
 * 
 * 
 * @author aela
 *
 */
public class SantMarginCallEntryReport extends MarginCallEntryReport {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput load(Vector errors) {
		ReportOutput ro = super.load(errors);
		// load the contracts for each entry

		return ro;
	}

}
