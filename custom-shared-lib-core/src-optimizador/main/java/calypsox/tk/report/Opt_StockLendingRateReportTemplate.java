package calypsox.tk.report;

import com.calypso.tk.report.ProductReportTemplate;

public class Opt_StockLendingRateReportTemplate extends ProductReportTemplate {

	private static final long serialVersionUID = 1L;

	@Override
	public void setDefaults() {
		super.setDefaults();
		// put(ProductReportStyle.PRODUCT_TYPE, Bond.BOND+","+ "BondAssetBacked"+","+Equity.EQUITY);
		setColumns(Opt_StockLendingRateReportStyle.DEFAULTS_COLUMNS);
	}

}
