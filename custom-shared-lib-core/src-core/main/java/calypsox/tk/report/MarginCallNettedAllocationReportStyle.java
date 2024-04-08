/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

public class MarginCallNettedAllocationReportStyle extends com.calypso.tk.report.MarginCallNettedAllocationReportStyle {
	private static final long serialVersionUID = 1L;

	public static final String CONTRACT_ID = "Contract ID";
	public static final String CONTRACT_NAME = "Contract Name";
	public static final String CONTRACT_TYPE = "Contract Type";

	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {
		if ((row == null) || (columnName == null)) {
			return null;
		}

		MarginCallAllocation allocation = (MarginCallAllocation) row.getProperty("Default");

		CollateralConfig collateralConfig = allocation.getCollateralConfig();
		if (CONTRACT_ID.equals(columnName)) {
			return collateralConfig.getId();
		} else if (CONTRACT_NAME.equals(columnName)) {
			return collateralConfig.getName();
		} else if (CONTRACT_TYPE.equals(columnName)) {
			return collateralConfig.getContractType();
		}

		return super.getColumnValue(row, columnName, errors);

	}

}
