package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Vector;

import calypsox.tk.product.BondCustomData;
import calypsox.tk.product.EquityCustomData;

import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.ProductReportStyle;
import com.calypso.tk.report.ReportRow;

public class Opt_StockLendingRateReportStyle extends ProductReportStyle {
	
private static final long serialVersionUID = 1L;

	public static String ISIN = "ISIN";
	
	public static String FEE = "Fee";
	
	public static String ACTIVE_AVAILABLE_QUANTITY = "Active available quantity";
	
	public static String QUANTITY_ON_LOAN = "QuantityOnLoan";
	
	protected DecimalFormat df;
	
	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { ISIN,
		ACTIVE_AVAILABLE_QUANTITY, FEE, QUANTITY_ON_LOAN};
	
	public Opt_StockLendingRateReportStyle() {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator(',');
		dfs.setGroupingSeparator('.');
		df = new DecimalFormat();
		df.setMaximumFractionDigits(10);
		df.setDecimalFormatSymbols(dfs);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		if (row == null) {
			return null;
		}
		
		 Product product = getProduct(row);

		if (columnName.equals(ISIN)) {
			return super.getColumnValue(row, PRODUCT_CODE_PREFIX + ISIN, errors);
		}

		if (columnName.equals(ACTIVE_AVAILABLE_QUANTITY)) {
			if (product instanceof Bond) {
				// get custom data
				BondCustomData bondCustomData = (BondCustomData) product
						.getCustomData();
				if (bondCustomData != null
						&& bondCustomData.getActive_available_qty() != null) {
					return df.format(bondCustomData.getActive_available_qty()
							.doubleValue());
				}
			}

			if (product instanceof Equity) {
				// get custom data
				EquityCustomData equityCustomData = (EquityCustomData) product
						.getCustomData();
				if (equityCustomData != null
						&& equityCustomData.getActive_available_qty() != null) {
					return df.format(equityCustomData.getActive_available_qty()
							.doubleValue());
				}
			}
		}

		if (columnName.equals(FEE)) {
			if (product instanceof Bond) {
				// get custom data
				BondCustomData bondCustomData = (BondCustomData) product
						.getCustomData();
				if (bondCustomData != null && bondCustomData.getFee() != null) {
					return df.format(bondCustomData.getFee().doubleValue());
				}
			}

			if (product instanceof Equity) {
				// get custom data
				EquityCustomData equityCustomData = (EquityCustomData) product
						.getCustomData();
				if (equityCustomData != null
						&& equityCustomData.getFee() != null) {
					return df.format(equityCustomData.getFee().doubleValue());
				}
			}
		}

		if (columnName.equals(QUANTITY_ON_LOAN)) {
			if (product instanceof Bond) {
				// get custom data
				BondCustomData bondCustomData = (BondCustomData) product
						.getCustomData();
				if (bondCustomData != null
						&& bondCustomData.getQty_on_loan() != null) {
					return df.format(bondCustomData.getQty_on_loan().doubleValue());
				}
			}

			if (product instanceof Equity) {
				// get custom data
				EquityCustomData equityCustomData = (EquityCustomData) product
						.getCustomData();
				if (equityCustomData != null
						&& equityCustomData.getQty_on_loan() != null) {
					return df.format(equityCustomData.getQty_on_loan().doubleValue());
				}
			}
		}

		return super.getColumnValue(row, columnName, errors);
	}

}
