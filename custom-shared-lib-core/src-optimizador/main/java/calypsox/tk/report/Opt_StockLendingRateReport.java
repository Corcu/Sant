package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Product;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ProductReport;
import com.calypso.tk.report.ProductReportTemplate;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class Opt_StockLendingRateReport extends ProductReport {

	private static final long serialVersionUID = 123L;

	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") Vector errorMsgs) {
		
		Vector<Product> products = new Vector<Product>();
		DSConnection dsCon = DSConnection.getDefault();
		products.addAll(BOCache.getExchangeTradedProducts(dsCon, Bond.BOND, null, null, true));
		products.addAll(BOCache.getExchangeTradedProducts(dsCon, "BondAssetBacked", null, null, true));
		products.addAll(BOCache.getExchangeTradedProducts(dsCon, Equity.EQUITY, null, null, true));
		
		((ProductReportTemplate)getReportTemplate()).setProducts(products);

		StandardReportOutput output = new StandardReportOutput(this);
		
		ReportRow[] rows = ((DefaultReportOutput)super.load(errorMsgs)).getRows();

		List<ReportRow> rowsList = new ArrayList<ReportRow>();

		//GSY - 26/12/14 - Incidencia optimizador HD 6880554
		for (ReportRow row : rows) {

			Product product = (Product) row.getProperty("Product");
			
			if (product.getMaturityDate()==null || product.getMaturityDate().gte(getValDate())){
				rowsList.add(row);
			}
		}

		// finally attach the new rows list to the output: collateral config y rating matrices
		output.setRows(rowsList.toArray(new ReportRow[0]));
		return output;
	}
	
	
}
