package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantAllDealsReportStyle extends ReportStyle {

	private static final long serialVersionUID = 123L;

	public static final String MCC_NAME = "MCC_NAME";
	public static final String BSTE_EXPOSURE = "BSTE_EXPOSURE";
	public static final String BSNY_EXPOSURE = "BSNY_EXPOSURE";
	public static final String BDSD_EXPOSURE = "BDSD_EXPOSURE";
	public static final String BSHK_EXPOSURE = "BSHK_EXPOSURE";
	public static final String TOTAL_EXPOSURE = "TOTAL_EXPOSURE";
	public static final String EXPOSURE_BASE_CCY = "EXPOSURE_BASE_CCY";
	public static final String COLLAT_1_ID = "COLLAT_1_ID";
	public static final String COLLAT_1_VALUE = "COLLAT_1_VALUE";
	public static final String COLLAT_1_CCY = "COLLAT_1_CCY";
	public static final String COLLAT_1_MCCVALUE = "COLLAT_1_MCCVALUE";
	public static final String COLLAT_2_ID = "COLLAT_2_ID";
	public static final String COLLAT_2_VALUE = "COLLAT_2_VALUE";
	public static final String COLLAT_2_CCY = "COLLAT_2_CCY";
	public static final String COLLAT_2_MCCVALUE = "COLLAT_2_MCCVALUE";
	public static final String COLLAT_3_ID = "COLLAT_3_ID";
	public static final String COLLAT_3_VALUE = "COLLAT_3_VALUE";
	public static final String COLLAT_3_CCY = "COLLAT_3_CCY";
	public static final String COLLAT_3_MCCVALUE = "COLLAT_3_MCCVALUE";
	public static final String COLLAT_4_ID = "COLLAT_4_ID";
	public static final String COLLAT_4_VALUE = "COLLAT_4_VALUE";
	public static final String COLLAT_4_CCY = "COLLAT_4_CCY";
	public static final String COLLAT_4_MCCVALUE = "COLLAT_4_MCCVALUE";
	public static final String COLLAT_5_ID = "COLLAT_5_ID";
	public static final String COLLAT_5_VALUE = "COLLAT_5_VALUE";
	public static final String COLLAT_5_CCY = "COLLAT_5_CCY";
	public static final String COLLAT_5_MCCVALUE = "COLLAT_5_MCCVALUE";
	public static final String COLLAT_6_ID = "COLLAT_6_ID";
	public static final String COLLAT_6_VALUE = "COLLAT_6_VALUE";
	public static final String COLLAT_6_CCY = "COLLAT_6_CCY";
	public static final String COLLAT_6_MCCVALUE = "COLLAT_6_MCCVALUE";
	public static final String COLLAT_7_ID = "COLLAT_7_ID";
	public static final String COLLAT_7_VALUE = "COLLAT_7_VALUE";
	public static final String COLLAT_7_CCY = "COLLAT_7_CCY";
	public static final String COLLAT_7_MCCVALUE = "COLLAT_7_MCCVALUE";
	public static final String COLLAT_8_ID = "COLLAT_8_ID";
	public static final String COLLAT_8_VALUE = "COLLAT_8_VALUE";
	public static final String COLLAT_8_CCY = "COLLAT_8_CCY";
	public static final String COLLAT_8_MCCVALUE = "COLLAT_8_MCCVALUE";
	public static final String COLLAT_9_ID = "COLLAT_9_ID";
	public static final String COLLAT_9_VALUE = "COLLAT_9_VALUE";
	public static final String COLLAT_9_CCY = "COLLAT_9_CCY";
	public static final String COLLAT_9_MCCVALUE = "COLLAT_9_MCCVALUE";
	public static final String COLLAT_10_ID = "COLLAT_10_ID";
	public static final String COLLAT_10_VALUE = "COLLAT_10_VALUE";
	public static final String COLLAT_10_CCY = "COLLAT_10_CCY";
	public static final String COLLAT_10_MCCVALUE = "COLLAT_10_MCCVALUE";

	public static final int N_COLLAT_POS_IN_COLUMN_NAME = 7;
	public static final int COLLAT_NAME_POS = 0;
	public static final int COLLAT_VALUE_POS = 1;
	public static final int COLLAT_VALUE_CCY_POS = 2;
	public static final int COLLAT_MCCVALUE_POS = 3;

	public static final String[] DEFAULT_COLUMNS = { BSTE_EXPOSURE, BSNY_EXPOSURE, BDSD_EXPOSURE, BSHK_EXPOSURE,
			TOTAL_EXPOSURE, EXPOSURE_BASE_CCY, COLLAT_1_ID, COLLAT_1_VALUE, COLLAT_1_CCY, COLLAT_1_MCCVALUE,
			COLLAT_2_ID, COLLAT_2_VALUE, COLLAT_2_CCY, COLLAT_2_MCCVALUE, COLLAT_3_ID, COLLAT_3_VALUE, COLLAT_3_CCY,
			COLLAT_3_MCCVALUE, COLLAT_4_ID, COLLAT_4_VALUE, COLLAT_4_CCY, COLLAT_4_MCCVALUE, COLLAT_5_ID,
			COLLAT_5_VALUE, COLLAT_5_CCY, COLLAT_5_MCCVALUE, COLLAT_6_ID, COLLAT_6_VALUE, COLLAT_6_CCY,
			COLLAT_6_MCCVALUE, COLLAT_7_ID, COLLAT_7_VALUE, COLLAT_7_CCY, COLLAT_7_MCCVALUE, COLLAT_8_ID,
			COLLAT_8_VALUE, COLLAT_8_CCY, COLLAT_8_MCCVALUE, COLLAT_9_ID, COLLAT_9_VALUE, COLLAT_9_CCY,
			COLLAT_9_MCCVALUE, COLLAT_10_ID, COLLAT_10_VALUE, COLLAT_10_CCY, COLLAT_10_MCCVALUE };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow reportRow, final String columnName, final Vector errors)
			throws InvalidParameterException {

		SantAllDealsItem dealItem = (SantAllDealsItem) reportRow.getProperty("SantAllDealsItem");

		if (columnName.startsWith("COLLAT")) {
			String collatNumberChar = columnName.substring(columnName.indexOf("_") + 1, columnName.lastIndexOf("_"));
			int collatNumberPos = Integer.parseInt(collatNumberChar) - 1;
			if (columnName.endsWith("MCCVALUE")) {
				return getCollateralInfo(dealItem, collatNumberPos, COLLAT_MCCVALUE_POS);
			} else if (columnName.endsWith("VALUE")) {
				return getCollateralInfo(dealItem, collatNumberPos, COLLAT_VALUE_POS);
			} else if (columnName.endsWith("CCY")) {
				return getCollateralInfo(dealItem, collatNumberPos, COLLAT_VALUE_CCY_POS);
			} else {
				return getCollateralInfo(dealItem, collatNumberPos, COLLAT_NAME_POS);
			}
		} else {
			return dealItem.getColumnValue(columnName);
		}

	}

	/**
	 * Get data for collateral n related to deal **
	 * 
	 * @param SantAllDealsItem
	 *            dealItem
	 * @param int n_collat_pos
	 * @param int collat_value_pos
	 * @return Object collateral data
	 */
	private Object getCollateralInfo(SantAllDealsItem dealItem, int n_collat_pos, int collat_value_pos) {

		if (dealItem.getCollateralsInfo().get(n_collat_pos) != null) {
			return dealItem.getCollateralsInfo().get(n_collat_pos).get(collat_value_pos);
		} else {
			return null;
		}
	}
}
