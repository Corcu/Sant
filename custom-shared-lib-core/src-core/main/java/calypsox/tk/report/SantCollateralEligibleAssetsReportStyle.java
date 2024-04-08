package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 *
 */
public class SantCollateralEligibleAssetsReportStyle extends ReportStyle {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4497570618871819707L;
	public static final String ISIN = "ISIN";
	public static final String CURRENCY = "Divisa";
	public static final String FACE_VALUE = "Nominal";
	public static final String DIRTY_PRICE = "Precio Sucio";
	public static final String CLEAN_PRICE = "Precio Limpio";
	public static final String POOL_FACTOR = "Factor de Amortizaci?n";
	public static final String MATURITY_DATE = "Fecha de Vencimiento";
	public static final String PROCESS_DATE = "Fecha Valoraci?n";
	public static final String PRODUCT_TYPE = "Tipo de Producto";

	public static final String[] DEFAULTS_COLUMNS = { ISIN, CURRENCY, FACE_VALUE, DIRTY_PRICE, CLEAN_PRICE,
			POOL_FACTOR, MATURITY_DATE, PROCESS_DATE, PRODUCT_TYPE };

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.calypso.tk.report.ReportStyle#getColumnValue(com.calypso.tk.report.
	 * ReportRow, java.lang.String, java.util.Vector)
	 */
	@Override
	public Object getColumnValue(ReportRow row, String columnName, final Vector errors)
			throws InvalidParameterException {
		SantCollateralEligibleAssetsItem item = (SantCollateralEligibleAssetsItem) row
				.getProperty(SantCollateralEligibleAssetsReportTemplate.COL_ELIGIBLE_ASSETS);

		if (ISIN.equals(columnName)) {
			return item.getIsin();
		} else if (CURRENCY.equals(columnName)) {
			return item.getCurrency();
		} else if (FACE_VALUE.equals(columnName)) {
			return item.getFaceValue();
		} else if (DIRTY_PRICE.equals(columnName)) {
			return item.getDirtyPrice();
		} else if (CLEAN_PRICE.equals(columnName)) {
			return item.getCleanPrice();
		} else if (POOL_FACTOR.equals(columnName)) {
			return item.getPoolFactor();
		} else if (MATURITY_DATE.equals(columnName)) {
			return item.getMaturityDate();
		} else if (PROCESS_DATE.equals(columnName)) {
			return item.getProcessDate();
		} else if (PRODUCT_TYPE.equals(columnName)) {
			return item.getProductType();
		}
		return null;
	}

}
