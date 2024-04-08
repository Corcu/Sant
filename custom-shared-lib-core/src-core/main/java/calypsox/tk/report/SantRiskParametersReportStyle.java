package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.riskparameters.SantRiskParameterWrapper;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantRiskParametersReportStyle extends ReportStyle {

	private static final long serialVersionUID = -8879122526968902743L;

	public static final String AGREEMENT_ID = "Agreement Id";
	public static final String COLLATERAL_AGREEMENT = "Collateral Agreement";
	public static final String COUNTERPARTY_OWNER = "Counterparty / Owner";
	public static final String CURRENCY_AGREEMENT = "Currency Agreement";

	public static final String CURRENT_THRESHOLD_RISK_LEVEL_1 = "Current Threshold Risk Level Moody's";
	public static final String CURRENT_THRESHOLD_RISK_LEVEL_2 = "Current Threshold Risk Level S&P";
	public static final String CURRENT_THRESHOLD_RISK_LEVEL_3 = "Current Threshold Risk Level Fitch";
	public static final String CURRENT_THRESHOLD_TYPE = "Current Threshold Type";
	public static final String CURRENT_THRESHOLD = "Current Threshold";
	public static final String CURRENT_THRESHOLD_CCY = "Current Threshold Currency";

	public static final String CURRENT_MTA_RISK_LEVEL_1 = "Current MTA Risk Level Moody's";
	public static final String CURRENT_MTA_RISK_LEVEL_2 = "Current MTA Risk Level S&P";
	public static final String CURRENT_MTA_RISK_LEVEL_3 = "Current MTA Risk Level Fitch";
	public static final String CURRENT_MTA_TYPE = "Current MTA Type";
	public static final String CURRENT_MTA = "Current MTA";
	public static final String CURRENT_MTA_CCY = "Current MTA Currency";

	public static final String CURRENT_ROUNDING = "Current Rounding";

	public static final String PREVIOUS_THRESHOLD_RISK_LEVEL_1 = "Previous Threshold Risk Level Moody's";
	public static final String PREVIOUS_THRESHOLD_RISK_LEVEL_2 = "Previous Threshold Risk Level S&P";
	public static final String PREVIOUS_THRESHOLD_RISK_LEVEL_3 = "Previous Threshold Risk Level Fitch";
	public static final String PREVIOUS_THRESHOLD_TYPE = "Previous Threshold Type";
	public static final String PREVIOUS_THRESHOLD = "Previous Threshold";
	public static final String PREVIOUS_THRESHOLD_CCY = "Previous Threshold Currency";

	public static final String PREVIOUS_MTA_RISK_LEVEL_1 = "Previous MTA Risk Level Moody's";
	public static final String PREVIOUS_MTA_RISK_LEVEL_2 = "Previous MTA Risk Level S&P";
	public static final String PREVIOUS_MTA_RISK_LEVEL_3 = "Previous MTA Risk Level Fitch";
	public static final String PREVIOUS_MTA_TYPE = "Previous MTA Type";
	public static final String PREVIOUS_MTA = "Previous MTA";
	public static final String PREVIOUS_MTA_CCY = "Previous MTA Currency";

	public static final String PREVIOUS_ROUNDING = "Previous Rounding";

	public static final String REPORT_DATE = "Report Date";
	public static final String PREVIOUS_DATE = "Previous Date";
	public static final String CURRENT_DATE = "Current Date";

	public static final String[] DEFAULTS_COLUMNS = { COLLATERAL_AGREEMENT, COUNTERPARTY_OWNER, CURRENCY_AGREEMENT,
			CURRENT_THRESHOLD_RISK_LEVEL_1, CURRENT_THRESHOLD_RISK_LEVEL_2, CURRENT_THRESHOLD_RISK_LEVEL_3,
			CURRENT_THRESHOLD_TYPE, CURRENT_THRESHOLD, CURRENT_THRESHOLD_CCY, CURRENT_MTA_RISK_LEVEL_1,
			CURRENT_MTA_RISK_LEVEL_2, CURRENT_MTA_RISK_LEVEL_3, CURRENT_MTA_TYPE, CURRENT_MTA, CURRENT_MTA_CCY,
			CURRENT_ROUNDING, PREVIOUS_THRESHOLD_RISK_LEVEL_1, PREVIOUS_THRESHOLD_RISK_LEVEL_2,
			PREVIOUS_THRESHOLD_RISK_LEVEL_3, PREVIOUS_THRESHOLD_TYPE, PREVIOUS_THRESHOLD, PREVIOUS_THRESHOLD_CCY,
			PREVIOUS_MTA_RISK_LEVEL_1, PREVIOUS_MTA_RISK_LEVEL_2, PREVIOUS_MTA_RISK_LEVEL_3, PREVIOUS_MTA_TYPE,
			PREVIOUS_MTA, PREVIOUS_MTA_CCY, PREVIOUS_ROUNDING, REPORT_DATE, PREVIOUS_DATE, CURRENT_DATE };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		SantRiskParameterWrapper rpw = (SantRiskParameterWrapper) row.getProperty("SantRiskParameterWrapper");

		if (AGREEMENT_ID.equals(columnName)) {
			return rpw.getContractId();
		} else if (COLLATERAL_AGREEMENT.equals(columnName)) {
			return rpw.getContractName();
		} else if (COUNTERPARTY_OWNER.equals(columnName)) {
			return rpw.getCpOwner();
		} else if (CURRENCY_AGREEMENT.equals(columnName)) {
			return rpw.getContractCcy();
		} else if (CURRENT_THRESHOLD_RISK_LEVEL_1.equals(columnName)) {
			return rpw.getCurrentThresholdRiskLevel1();
		} else if (CURRENT_THRESHOLD_RISK_LEVEL_2.equals(columnName)) {
			return rpw.getCurrentThresholdRiskLevel2();
		} else if (CURRENT_THRESHOLD_RISK_LEVEL_3.equals(columnName)) {
			return rpw.getCurrentThresholdRiskLevel3();
		} else if (CURRENT_THRESHOLD_TYPE.equals(columnName)) {
			return rpw.getCurrentThresholdType();
		} else if (CURRENT_THRESHOLD.equals(columnName)) {
			return rpw.getCurrentThreshold();
		} else if (CURRENT_THRESHOLD_CCY.equals(columnName)) {
			return rpw.getCurrentThresholdCurrency();
		} else if (CURRENT_MTA_RISK_LEVEL_1.equals(columnName)) {
			return rpw.getCurrentMTARiskLevel1();
		} else if (CURRENT_MTA_RISK_LEVEL_2.equals(columnName)) {
			return rpw.getCurrentMTARiskLevel2();
		} else if (CURRENT_MTA_RISK_LEVEL_3.equals(columnName)) {
			return rpw.getCurrentMTARiskLevel3();
		} else if (CURRENT_MTA_TYPE.equals(columnName)) {
			return rpw.getCurrentMTAType();
		} else if (CURRENT_MTA.equals(columnName)) {
			return rpw.getCurrentMTA();
		} else if (CURRENT_MTA_CCY.equals(columnName)) {
			return rpw.getCurrentMTACurrency();
		} else if (CURRENT_ROUNDING.equals(columnName)) {
			return rpw.getCurrentRounding();
		} else if (PREVIOUS_THRESHOLD_RISK_LEVEL_1.equals(columnName)) {
			return rpw.getPreviousThresholdRiskLevel1();
		} else if (PREVIOUS_THRESHOLD_RISK_LEVEL_2.equals(columnName)) {
			return rpw.getPreviousThresholdRiskLevel2();
		} else if (PREVIOUS_THRESHOLD_RISK_LEVEL_3.equals(columnName)) {
			return rpw.getPreviousThresholdRiskLevel3();
		} else if (PREVIOUS_THRESHOLD_TYPE.equals(columnName)) {
			return rpw.getPreviousThresholdType();
		} else if (PREVIOUS_THRESHOLD.equals(columnName)) {
			return rpw.getPreviousThreshold();
		} else if (PREVIOUS_THRESHOLD_CCY.equals(columnName)) {
			return rpw.getPreviousThresholdCurrency();
		} else if (PREVIOUS_MTA_RISK_LEVEL_1.equals(columnName)) {
			return rpw.getPreviousMTARiskLevel1();
		} else if (PREVIOUS_MTA_RISK_LEVEL_2.equals(columnName)) {
			return rpw.getPreviousMTARiskLevel2();
		} else if (PREVIOUS_MTA_RISK_LEVEL_3.equals(columnName)) {
			return rpw.getPreviousMTARiskLevel3();
		} else if (PREVIOUS_MTA_TYPE.equals(columnName)) {
			return rpw.getPreviousMTAType();
		} else if (PREVIOUS_MTA.equals(columnName)) {
			return rpw.getPreviousMTA();
		} else if (PREVIOUS_MTA_CCY.equals(columnName)) {
			return rpw.getPreviousMTACurrency();
		} else if (PREVIOUS_ROUNDING.equals(columnName)) {
			return rpw.getPreviousRounding();
		} else if (REPORT_DATE.equals(columnName)) {
			return rpw.getReportDate();
		} else if (PREVIOUS_DATE.equals(columnName)) {
			return rpw.getPreviousDate();
		} else if (CURRENT_DATE.equals(columnName)) {
			return rpw.getCurrentDate();
		}
		return null;
	}
}
