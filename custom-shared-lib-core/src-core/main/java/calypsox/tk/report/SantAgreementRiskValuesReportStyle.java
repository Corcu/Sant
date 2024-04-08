package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.riskvalues.RiskValuesItem;

import com.calypso.tk.core.Amount;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantAgreementRiskValuesReportStyle extends ReportStyle {

	private static final long serialVersionUID = -8879122526968902743L;

	public static final String AGREEMENT_ID = "Agreement Id";
	public static final String AGREEMENT_NAME = "Agreement Name";
	public static final String COUNTERPARTY_FULLNAME = "Cpty FullName";

	public static final String MOODY_RISK_RATE = "Moody Risk Rate";
	public static final String SNP_RISK_RATE = "S&P Risk Rate";
	public static final String FITCH_RISK_RATE = "Fitch Risk Rate";

	public static final String DELIVERY_ROUNDING_PO = "Delivery Rounding";
	public static final String RETURN_ROUNDING_LE = "Return Rounding";
	public static final String DELIVERY_MC_RATING_MTA = "Delivery MC Rating MTA";
	public static final String RETURN_MC_RATING_MTA = "Return MC Rating MTA";
	public static final String MCRATING_THRESHOLD = "Rating Threshold";
	public static final String INDAMOUNT_PO = "INDAMOUNT_PO";
	public static final String INDAMOUNT_CPTY = "INDAMOUNT_CPTY";
	public static final String ACTIVO = "ACTIVO";
	public static final String ACTIVO_MTA = "ACTIVO(MTA)";
	public static final String RATING_CCY = "Rating Ccy";

	public static final String[] DEFAULTS_COLUMNS = { AGREEMENT_NAME, COUNTERPARTY_FULLNAME, MOODY_RISK_RATE,
			SNP_RISK_RATE, FITCH_RISK_RATE, DELIVERY_ROUNDING_PO, RETURN_ROUNDING_LE, DELIVERY_MC_RATING_MTA,
			RETURN_MC_RATING_MTA, MCRATING_THRESHOLD, INDAMOUNT_PO, INDAMOUNT_CPTY };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		RiskValuesItem item = (RiskValuesItem) row.getProperty(SantAgreementRiskValuesReport.SANT_RISK_VALUES_ITEM);

		if (AGREEMENT_ID.equals(columnName)) {
			return item.getContractId();
		} else if (AGREEMENT_NAME.equals(columnName)) {
			return item.getContractName();
		} else if (COUNTERPARTY_FULLNAME.equals(columnName)) {
			return item.getLEFullName();

		} else if (MOODY_RISK_RATE.equals(columnName)) {
			return item.getMoodyMCrating();
		} else if (SNP_RISK_RATE.equals(columnName)) {
			return item.getSnpMCrating();
		} else if (FITCH_RISK_RATE.equals(columnName)) {
			return item.getFitchMCrating();
		} else if (DELIVERY_ROUNDING_PO.equals(columnName)) {
			return format(item.getDeliveryRoundingPO());
		} else if (RETURN_ROUNDING_LE.equals(columnName)) {
			return format(item.getReturnRoundingLE());
		} else if (DELIVERY_MC_RATING_MTA.equals(columnName)) {
			return item.getDeliveryMCRatingMTA();
		} else if (RETURN_MC_RATING_MTA.equals(columnName)) {
			return item.getReturnMCRatingMTA();
		} else if (MCRATING_THRESHOLD.equals(columnName)) {
			return item.getMCRatingThreshold();
		} else if (RATING_CCY.equals(columnName)) {
			return item.getMCRatingCcy();
		} else if (INDAMOUNT_PO.equals(columnName)) {
			return item.getIndependentAmountPO();
		} else if (INDAMOUNT_CPTY.equals(columnName)) {
			return item.getIndependentAmountCPTY();
		} else if (ACTIVO.equals(columnName)) {
			if (item.isActive()) {
				return "Y";
			} else {
				return "N";
			}
		} else if (ACTIVO_MTA.equals(columnName)) {
			if (item.isActiveMTA()) {
				return "Y";
			} else {
				return "N";
			}
		}

		return null;
	}

	private Amount format(double d) {
		return new Amount(d, 2);
	}
}
