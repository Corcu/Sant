package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.entitlement.DataEntitlementCheckProxy;
import com.calypso.tk.marketdata.MarketDataEntitlementController;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.PLMarkReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.DataServer;
import com.calypso.tk.service.LocalCache;

import calypsox.tk.collateral.service.RemotePLMarkHist;
/**
 * 
 * @author ?? & aalonsop (refactor)
 *
 */

public class PLMarkHistReport extends PLMarkReport {

	private static final long serialVersionUID = 808382949620178007L;
	public static final String POSITION_TRADE_IND = "Position/Trade";
	private final boolean _countOnly = false;

	// For testing purposes only
	private boolean useTestTables = false;
	private static String TEST_DOMAIN_VALUE_NAME = "PLMarkHistTesting";

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput load(final Vector paramVector) {
		boolean isTrade = true;
		boolean isPosition = true;
		String bookId = null;
		JDate fromDate = null;
		JDate toDate = null;

		Collection<PLMark> plMarkHistTradePosCollection = null;
		Collection<PLMark> plMarkHistTradeZeroCollection = null;

		String tradePosParams = "";
		String tradeZeroParams = "";

		//FOR TESTING PURPOSES ONLY
		useTestTables=checkDomainValue();
		
		initDates();

		final DefaultReportOutput defaultReportOutput = new DefaultReportOutput(this);
		Book book;
		Boolean isAdjustmentOnly;
		if (this._reportTemplate != null) {

			isTrade = ((Boolean) this._reportTemplate.get("IncTrade")).booleanValue();

			isPosition = ((Boolean) this._reportTemplate.get("IncPosition")).booleanValue();

			bookId = (String) this._reportTemplate.get("Book");
			fromDate = (JDate) this._reportTemplate.get("FromDate");

			toDate = (JDate) this._reportTemplate.get("ToDate");

			tradeZeroParams = "trade_id = 0";

			if ((isTrade) && (!isPosition)) {
				tradePosParams = "position_or_trade like '%Trade' ";
			}
			if ((!isTrade) && (isPosition)) {
				tradePosParams = "position_or_trade not like '%Trade' ";
			}
			if ((!isTrade) && (!isPosition)) {
				tradePosParams = "position_or_trade not like '%Trade' AND position_or_trade not like '%Position' ";
			}

			if ((!Util.isEmpty(bookId)) && (!bookId.equals("ALL"))) {
				book = BOCache.getBook(DSConnection.getDefault(), bookId);

				if (!Util.isEmpty(tradePosParams)) {
					tradePosParams += " AND ";
				}
				tradePosParams += "(book_id = " + book.getId() + " OR book_id = 0)";
			}

			if (fromDate != null) {
				if (!Util.isEmpty(tradePosParams)) {
					tradePosParams += " AND ";
				}
				tradePosParams += " valuation_date >= " + Util.date2SQLString(fromDate);

				tradeZeroParams += " AND valuation_date >= " + Util.date2SQLString(fromDate);
			}

			if (toDate != null) {
				if (!Util.isEmpty(tradePosParams)) {
					tradePosParams += " AND ";
				}
				tradePosParams += " valuation_date <= " + Util.date2SQLString(toDate);

				tradeZeroParams += " AND valuation_date <= " + Util.date2SQLString(toDate);
			}

			final String externalRef = (String) this._reportTemplate.get("External Reference");

			if (!Util.isEmpty(externalRef)) {
				tradePosParams = tradePosParams
						+ " AND trade_id in (select trade_id from trade where external_reference = "
						+ ioSQL.string2SQLString(externalRef) + ")";
			}

			isAdjustmentOnly = this._reportTemplate.getBoolean("Adjustments Only");
			String queryCondition;
			if (isAdjustmentOnly.booleanValue()) {
				final String tradeType = (String) this._reportTemplate.get("Trade Type");

				queryCondition = null;
				if ("Missing Trade Adjustments".equals(tradeType)) {
					queryCondition = ioSQL.buildLike("position_or_trade", "dummy", false, true);
				} else if ("Trade/Position Adjustment".equals(tradeType)) {
					queryCondition = "lower(position_or_trade) Not Like '%dummy%'";
				}
				if (queryCondition != null) {
					tradePosParams += " AND " + queryCondition;
				}
			}
			if (this._reportTemplate.get("Trade Id") != null) {
				final String tradeId = (String) this._reportTemplate.get("Trade Id");

				if (Long.parseLong(tradeId) >= 0L) {
					tradePosParams += " AND trade_id = " + tradeId;
				}
			}
			final String pricingEnvironment = (String) this._reportTemplate.get("Pricing Environment");

			if (!Util.isEmpty(pricingEnvironment)) {
				tradePosParams += " AND pricing_env_name = " + ioSQL.string2SQLString(pricingEnvironment);

				tradeZeroParams += " AND pricing_env_name = " + ioSQL.string2SQLString(pricingEnvironment);
			}

			try {
				RemotePLMarkHist remotePLMarkHist = null;
				try {
					remotePLMarkHist = (RemotePLMarkHist) getDSConnection().getRMIService("PLMarkHistServer",
							RemotePLMarkHist.class);
					if ((!DataServer._isDataServer)
							&& (MarketDataEntitlementController.getDefault().needToPerformEntitlementCheck())) {
						remotePLMarkHist = DataEntitlementCheckProxy.newInstance(remotePLMarkHist);
					}
				} catch (final Exception e) {
					Log.error(this, e);
				}

				if (!this._countOnly) {

					plMarkHistTradePosCollection = remotePLMarkHist.getPLMarksTableSwitch(tradePosParams,
							useTestTables);
					plMarkHistTradeZeroCollection = remotePLMarkHist.getPLMarksTableSwitch(tradeZeroParams,
							useTestTables);

				} else {
					int j = remotePLMarkHist.countPLMarks(tradePosParams);
					j += remotePLMarkHist.countPLMarks(tradeZeroParams);
					addPotentialSize(PLMark.class.getName(), j);
				}
			} catch (final Exception e) {
				Log.error(this, e);
			}
		}

		if (plMarkHistTradePosCollection != null) {
			final Collection<PLMark> plMarkCollection = flatenMarks(plMarkHistTradePosCollection,
					plMarkHistTradeZeroCollection);
			if ((plMarkCollection != null) && (plMarkCollection.size() > 0)) {
				final ReportRow[] reportRowArray = new ReportRow[plMarkCollection.size()];
				final Iterator<PLMark> iterator = plMarkCollection.iterator();
				int i = 0;
				while (iterator.hasNext()) {
					final PLMark pLMark = iterator.next();
					reportRowArray[(i++)] = new ReportRow(pLMark);
				}
				defaultReportOutput.setRows(reportRowArray);
			}
		}
		return defaultReportOutput;
	}

	private boolean checkDomainValue() {
		Vector<String> dvalue = LocalCache.getDomainValues(DSConnection.getDefault(), TEST_DOMAIN_VALUE_NAME);
		if (dvalue.size() > 1)
			return false;
		else if (dvalue.size() == 1) {
			return Boolean.parseBoolean(dvalue.get(0));
		} else
			return false;
	}

	private Collection<PLMark> flatenMarks(final Collection<PLMark> paramCollection1,
			final Collection<PLMark> paramCollection2) {
		ArrayList<PLMark> pLMarkArrayList = null;
		final Boolean isAdjustmentOnly = this._reportTemplate.getBoolean("Adjustments Only");

		final Boolean hasCreateSystemPLMarkAuthorization = Boolean
				.valueOf(AccessUtil.isAuthorized("CreateSystemPLMark"));

		if ((paramCollection1 != null) && (paramCollection1.size() > 0)) {
			try {
				pLMarkArrayList = new ArrayList<>();
				for (PLMark pLMark : paramCollection1) {
					final PLMark independentPLMark = getIndependentPLMark(pLMark, paramCollection2);
					final List<PLMarkValue> pLMarkValues = pLMark.getMarkValuesAsList();
					for (PLMarkValue pLMarkValue : pLMarkValues) {
						if (((!isAdjustmentOnly.booleanValue()) || (pLMarkValue.isAdjusted()))
								&& ((hasCreateSystemPLMarkAuthorization.booleanValue()) || (!"System".equals("")))) {
							final PLMark clonedPLMark = (PLMark) pLMark.clone();
							final List<PLMarkValue> convertedPLMarkValues = new ArrayList<>();
							convertedPLMarkValues.add(pLMarkValue);
							final PLMarkValue convertedPLMarkValue = getConversionFactorMarkValue(independentPLMark,
									pLMarkValue.getCurrency());

							if (convertedPLMarkValue != null) {
								convertedPLMarkValues.add(convertedPLMarkValue);
							}
							clonedPLMark.setMarkValuesAsList(convertedPLMarkValues);
							pLMarkArrayList.add(clonedPLMark);
						}
					}
				}
			} catch (final Exception e) {
				Log.error(this, e);
			}
		}
		return pLMarkArrayList;
	}

	private PLMark getIndependentPLMark(final PLMark paramPLMark, final Collection<PLMark> paramCollection) {
		if ((paramPLMark != null) && (paramCollection != null)) {
			for (PLMark pLMark : paramCollection) {
				if (paramPLMark.getValDate().equals(pLMark.getValDate())) {
					return pLMark;
				}
			}
		}
		return null;
	}

	private PLMarkValue getConversionFactorMarkValue(final PLMark paramPLMark, final String paramString) {
		if ((paramPLMark != null) && (paramString != null)) {
			if (paramPLMark.getMarkValuesAsList() == null) {
				return null;
			}
			final String str = PLMarkValue.getKey("CONVERSION_FACTOR", paramString);

			return (PLMarkValue) paramPLMark.getPLMarkValue(str);
		}
		return null;
	}
}