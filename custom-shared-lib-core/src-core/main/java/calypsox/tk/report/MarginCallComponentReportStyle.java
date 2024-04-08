/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.SecurityAllocation;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Rate;
import com.calypso.tk.report.ReportRow;

public class MarginCallComponentReportStyle extends com.calypso.tk.report.MarginCallComponentReportStyle {

	private static final long serialVersionUID = 1L;

	public static final String SANT_DIRTY_PRICE = "Sant Dirty Price";

	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {
		if (SANT_DIRTY_PRICE.equals(columnName)) {
			MarginCallAllocation allocation = (MarginCallAllocation) row.getProperty("Default");
			if (allocation instanceof SecurityAllocation) {

				Amount allInValue = (Amount) super.getColumnValue(row, ALL_IN_VALUE, errors);
				Amount nominal = (Amount) super.getColumnValue(row, NOMINAL, errors);
				Rate haircut = (Rate) super.getColumnValue(row, HAIRCUT, errors);

				return CollateralUtilities.getSantDirtyPrice(allInValue, nominal, haircut);
			}
			return "";
		}
		return super.getColumnValue(row, columnName, errors);

	}

}
