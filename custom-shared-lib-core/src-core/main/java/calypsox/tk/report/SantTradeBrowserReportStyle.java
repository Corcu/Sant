package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericTradeReportStyle;

import com.calypso.tk.core.Product;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.report.ProductReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.util.CurrencyUtil;

public class SantTradeBrowserReportStyle extends SantGenericTradeReportStyle {

	private static final long serialVersionUID = 1007723141002942588L;

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	
		final Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
		if (columnName.equals(TradeReportStyle.PRINCIPAL_AMOUNT)) {
			if (trade.getProduct() instanceof SecLending) {
				double amount = ((SecLending) trade.getProduct()).getSecuritiesNominalValue();
				int digits = 2;
				String cur = trade.getProduct().getCurrency();
				if (cur == null) {
					cur = trade.getTradeCurrency();
				}
				if (cur != null) {
					digits = CurrencyUtil.getRoundingUnit(cur);
				}
				return new SignedAmount(amount, digits);
			}
		}else if(columnName.equals(TradeReportStyle.SETTLE_DATE)){
			return  String.valueOf(trade.getSettleDate());
		}else if(columnName.equals(TradeReportStyle.TRADE_DATE)){
			return  sdf.format(trade.getTradeDate());
		}else if(columnName.equals(ProductReportStyle.MATURITY_DATE)){
			Product pro = trade.getProduct();
			
			if(pro!=null){
				if(pro instanceof CollateralExposure){
					CollateralExposure p =(CollateralExposure) trade.getProduct();
					return p.getEndDate();
				}else if(pro instanceof Repo){
					Repo p =(Repo) trade.getProduct();
					return p.getEndDate();
				}else if(pro instanceof SecLending){
					SecLending p =(SecLending) trade.getProduct();
					p.getEndDate();
				}
			}else{
				return "";
			}
		}

		return super.getColumnValue(row, columnName, errors);

	}

}
