package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.TradeInterfaceUtils;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantNoMTMVariationReportStyle extends ReportStyle {

	private static final long serialVersionUID = -9175525544674459030L;
	public static final String BO_ID = "BO_ID";
	public static final String FRONT_ID = "FRONT_ID";
	public static final String CONTRACT_ID = "MC Agreement ID";
	public static final String MCC_NAME = "MC Contract Name";
	public static final String SETTLE_DATE = "Settle date";
	public static final String MATURITY_DATE = "Maturity date";
	public static final String PO = "PO";
	public static final String MTM_CCY = "MTM Ccy";
	public static final String MTM_VALUE = "MTM Value";
	public static final String PRODUCT_TYPE = "Product Type";
	public static final String STRUCTURE_ID = "STRUCTURE_ID";

	public static final String[] DEFAULTS_COLUMNS = { BO_ID, FRONT_ID, CONTRACT_ID, MCC_NAME, SETTLE_DATE,
			MATURITY_DATE, PO, MTM_CCY, MTM_VALUE, PRODUCT_TYPE, STRUCTURE_ID };

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {

		final SantNoMTMVariationItem noMtmVariationItem = (SantNoMTMVariationItem) row
				.getProperty(SantNoMTMVariationReport.SANT_NOMTM_VARIATION_ITEM);

		if (columnName.equals(BO_ID)) {
			return noMtmVariationItem.getTrade().getKeywordValue(TradeInterfaceUtils.TRADE_KWD_BO_REFERENCE);

		} else if (columnName.equals(FRONT_ID)) {
			return noMtmVariationItem.getTrade().getExternalReference();

		} else if (columnName.equals(CONTRACT_ID)) {
			return noMtmVariationItem.getTrade().getKeywordValue(CollateralStaticAttributes.MC_CONTRACT_NUMBER);

		} else if (columnName.equals(MCC_NAME)) {
			return noMtmVariationItem.getMarginCallname();

		} else if (columnName.equals(SETTLE_DATE)) {
			return noMtmVariationItem.getTrade().getSettleDate();

		} else if (columnName.equals(MATURITY_DATE)) {
			return noMtmVariationItem.getTrade().getMaturityDate();

		} else if (columnName.equals(PO)) {
			return noMtmVariationItem.getTrade().getBook().getLegalEntity().getCode();

		} else if (columnName.equals(MTM_CCY)) {
			return noMtmVariationItem.getMarkCcy();

		} else if (columnName.equals(MTM_VALUE)) {
			return noMtmVariationItem.getMarkValue();
		} else if (columnName.equals(PRODUCT_TYPE)) {
			return noMtmVariationItem.getTrade().getProductType();
		} else if (columnName.equals(STRUCTURE_ID)) {
			return noMtmVariationItem.getTrade().getKeywordValue(TradeInterfaceUtils.TRADE_KWD_STRUCTURE_ID);
		} else {
			throw new InvalidParameterException("The column name:" + columnName + " is not expected");
		}

	}
}
