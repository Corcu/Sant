package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.tk.core.Product;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantTradesPotentialMtmErrorReportStyle extends ReportStyle {

	private static final long serialVersionUID = 123L;

	public static final String PROCESS_DATE = "PROCESS_DATE";
	public static final String REGISTRY = "REGISTRY"; // BO_REF
	public static final String FRONT_ID = "FRONT_ID"; // Ext Ref
	public static final String CONTRACTO = "CONTRACTO";
	public static final String OWNER = "OWNER";
	public static final String PRODUCTO = "PRODUCTO";
	public static final String MATURITY = "MATURITY";
	public static final String ESTRUCTURA = "ESTRUCTURA";
	public static final String MTM_D_MINUS1 = "MTM D-1";

	public SantTradesPotentialMtmErrorReportStyle() {
		super();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		SantTradesPotentialMtmErrorItem item = (SantTradesPotentialMtmErrorItem) row.getProperty("Default");

		if (PROCESS_DATE.equals(columnName)) {
			return row.getProperty(PROCESS_DATE);
		} else if (REGISTRY.equals(columnName)) {
			return item.getBoRef();
		} else if (FRONT_ID.equals(columnName)) {
			return item.getExtRef();
		} else if (CONTRACTO.equals(columnName)) {
			return item.getMccDesc();
		} else if (OWNER.equals(columnName)) {
			return item.getOwner();

		} else if (PRODUCTO.equals(columnName)) {
			if (item.getProductType().equals(Product.REPO)) {
				return CollateralStaticAttributes.INSTRUMENT_TYPE_REPO;
			} else if (item.getProductType().equals(Product.SEC_LENDING)) {
				return CollateralStaticAttributes.INSTRUMENT_TYPE_SEC_LENDING;
			} else {
				return item.getProductSubType();
			}
		} else if (MATURITY.equals(columnName)) {
			return item.getMaturityDate();
		} else if (ESTRUCTURA.equals(columnName)) {
			return item.getStructureId();
		} else if (MTM_D_MINUS1.equals(columnName)) {
			return CollateralUtilities.formatNumber(item.getMtmPrevious());
		}

		return null;
	}

}
