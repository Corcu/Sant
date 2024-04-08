/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.apps.cws.presentation.format.JDateFormat;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.CashPosition;
import com.calypso.tk.collateral.MarginCallPositionFacade;
import com.calypso.tk.collateral.SecurityPositionFacade;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import calypsox.util.collateral.CollateralUtilities;

public class MarginCallPositionReportStyle extends com.calypso.tk.report.MarginCallPositionReportStyle {

	private static final long serialVersionUID = 1L;

	public static final String SANT_DIRTY_PRICE = "Sant Dirty Price";

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValueBase(ReportRow row, MarginCallPositionFacade position, String columnName, Vector errors)
			throws InvalidParameterException {
		if (SANT_DIRTY_PRICE.equals(columnName)) {
			if (position instanceof CashPosition) {
				return "";
			}
			JDate valDate =  position.getValuationDate().getJDatetime().getJDate(TimeZone.getDefault());
			String priceEnvname = ((CollateralConfig) row.getProperty("MarginCallConfig")).getPricingEnvName();
			SecurityPositionFacade security = row.getProperty("Default");

			if(!Util.isEmpty(priceEnvname) && null!=security){
				Product product = (Product)BOCache.getExchangedTradedProduct(DSConnection.getDefault(), security.getProductId());
				if(null!=product){
					JDateFormat dateformat = new JDateFormat(new SimpleDateFormat("dd/MM/yy",Locale.getDefault()));
					String date = dateformat.format(valDate);
					try {
						String quoteName = CollateralUtilities.getQuoteNameFromISIN(product.getSecCode("ISIN"),valDate);
						if(product instanceof Bond){
								PricingEnv priceEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(priceEnvname);
								String quoteSet = priceEnv.getQuoteSetName();
								if(null != priceEnv && !Util.isEmpty(quoteSet) && !Util.isEmpty(quoteName) && null!=date){
									String clausule = "quote_name = " + "'" + quoteName + "' AND trunc(quote_date) = to_date('"
											+ date + "', 'dd/mm/yy') AND quote_set_name = '"+quoteSet+"'";
									Vector<QuoteValue> quoteValue = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
									if ((quoteValue != null) && (quoteValue.size() > 0)) {
										return quoteValue.get(0).getClose() * 100;
									}
								}
							
						}else if(product instanceof Equity){
							if(!Util.isEmpty(quoteName) && null!=date){
								String clausule = "quote_name = " + "'" + quoteName + "' AND trunc(quote_date) = to_date('"
										+ date + "', 'dd/mm/yy') AND quote_set_name = 'OFFICIAL'";
								Vector<QuoteValue> quoteValue = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
								if ((quoteValue != null) && (quoteValue.size() > 0)) {
									return quoteValue.get(0).getClose();
								}
							}
						}
					} catch (RemoteException e) {
						Log.error(this, "Can?t load Quote for Security: " + position.getDescription());
						Log.error(this, e); //sonar
					}
				}
			}else{
				Log.error(this, "Add Prince Envioremt to the contract");
			}
			return "";
		}
		
		return super.getColumnValueBase(row, position, columnName, errors);

	}
}
