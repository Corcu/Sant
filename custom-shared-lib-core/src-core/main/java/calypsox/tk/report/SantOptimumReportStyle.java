/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Amount;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantOptimumReportStyle extends ReportStyle {

	private static final long serialVersionUID = 1L;

	// report columns definition
	public static final String MC_CONTRACT_NAME = "Margin Call Contract Name"; // id name
	public static final String CATEGORY_NAME = "Category Name";
	public static final String ELIGIBLE_CATEGORY = "Eligibility Category";

	public static final String CATEGORY_TYPE = "Category Type";
	public static final String SUM_OF_CONTRACT_VALUES = "Sum of Contracts Values";
	public static final String MC_CONTRACT_BASE_CURRENCY = "Margin Call Contract Base Currency"; // EUR, USD

	public static final String OPTIMIZATION_PRICE = "Optimization Price";
	public static final String CALCULATED_OPTIMIZATION_PRICE = "Calculated Optimization Price";

	// report columns definition
	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {

		// no data
		if ((row == null) || (row.getProperty(SantOptimumReportTemplate.OPTIMIZATION_REPORT) == null)) {
			return null;
		}

		// retrive row data
		final SantOptimumReportItemLight entry = (SantOptimumReportItemLight) row
				.getProperty(SantOptimumReportTemplate.OPTIMIZATION_REPORT);

		// process rows of the report
		if (MC_CONTRACT_NAME.equals(columnName)) {

			return entry.getContractName();

		} else if (CATEGORY_NAME.equals(columnName)) {

			return entry.getOptCategoryName();

		} else if (ELIGIBLE_CATEGORY.equals(columnName)) {

			return entry.isEligible() ? "Y" : "N";

		} else if (CATEGORY_TYPE.equals(columnName)) {
			if (!Util.isEmpty(entry.getCategoryType())) {
				return entry.getCategoryType();
			} else {
				return (entry.getProductId() == 0) ? "Cash" : "Security";
			}

		} else if (SUM_OF_CONTRACT_VALUES.equals(columnName)) {

			return new Amount(entry.getCategoryTotal(), 2);

		} else if (MC_CONTRACT_BASE_CURRENCY.equals(columnName)) {

			return entry.getContractBaseCcy();
		} else if (OPTIMIZATION_PRICE.equals(columnName)) {
			return entry.getOptimizationPrice();
		} else if (CALCULATED_OPTIMIZATION_PRICE.equals(columnName)) {
			return new Amount(entry.calcOptimizationPrice(), 2);
		}

		// column not found
		return null;
	}

}
