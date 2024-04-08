/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.CONTRACT_VALUE_YESTERDAY_QUOTES;
import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.CORE_TEMPLATE_MC_IDS;
import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.DEFAULT;
import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.DIRTY_PRICE_QUOTE;
import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.ISIN;
import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.SECURITIES;
import static calypsox.tk.report.SantMarginCallPositionValuationReportTemplate.VAL_DATE;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;


import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.MarginCallPosition;
import com.calypso.tk.collateral.SecurityPosition;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallPositionValuationReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.CurrencyUtil;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.util.ScheduledTaskCSVREPORT;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

/**
 * Custom Margin Call Valuation report, originally requested for MMOO conciliation.
 * 
 * @author aela & Guillermo Solano
 * @date 27/03/2015
 * @version 2.0, added collateral config columns and custom columns (new filters: isin, agreement owner, agreements &
 *          process date)
 * 
 */
public class SantMarginCallPositionValuationReport extends MarginCallPositionValuationReport {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -6477906564837339580L;

	/**
	 * Generates report output.
	 * 
	 * @param errorMsgs
	 * 
	 * @return DefaultReportOutput
	 * @throws RemoteException
	 */
	private DefaultReportOutput getReportOutput(final Vector<String> errorMsgs) throws Exception {

		final List<ReportRow> rowsList = new ArrayList<ReportRow>();

		// process date & previous for quote dirty price
		final JDate process = getProcessDate();
		super.setValuationDatetime(new JDatetime(process, TimeZone.getDefault()));
		final JDate quoteDate = process.addBusinessDays(-1, getReportTemplate().getHolidays());

		// AAP SETS Custom PO Filters
		setCustomPOValuesIntoCoreAttr();
		// filter agreements, custom filters are transformed into core template
		// filter
		filterAgreementsByCore();

		// get set of isins to filter
		final Set<String> isins2filter = getIsinsFromFilter();

		// call core report load
		StandardReportOutput output = new StandardReportOutput(this);
		ReportRow[] rows = ((DefaultReportOutput) super.load(errorMsgs)).getRows();

		for (ReportRow row : rows) {

			final MarginCallPosition position = (MarginCallPosition) row.getProperty(DEFAULT);

			row.setProperty(VAL_DATE, getValDate());

			if (position instanceof SecurityPosition) {

				final Product security = ((SecurityPosition) position).getProduct();
				final String isin = security.getSecCode(ISIN);

				if (filterSecurityIsin(isins2filter, isin)) {
					continue;
				}

				if (!Util.isEmpty(isin)) {

					QuoteValue quote = getDirtyPriceQv(security, quoteDate);

					if ((quote != null) && (quote.getClose() != 0.0d)) {

						row.setProperty(DIRTY_PRICE_QUOTE, quote.getClose());
						final Double valueYesterdayQuote = ((SecurityPosition) position).getNominal()
								* (quote.getClose());
						final Amount value = new Amount(valueYesterdayQuote,
								CurrencyUtil.getRoundingUnit(position.getCurrency()));
						row.setProperty(CONTRACT_VALUE_YESTERDAY_QUOTES, value);

					} else {

						errorMsgs.add("Not possible to load Dirty quoteValue in " + quoteDate.toString()
								+ " for the Bond: " + isin);
						final Amount zero = new Amount(0.0d, CurrencyUtil.getRoundingUnit(position.getCurrency()));
						row.setProperty(DIRTY_PRICE_QUOTE, zero);
						row.setProperty(CONTRACT_VALUE_YESTERDAY_QUOTES, zero);
					}
				}

				// end Bond position - else must be cash position, in case isin
				// filter is selected, row is discriminated
			} else if (!isins2filter.isEmpty()) {
				continue;
			}
			rowsList.add(row);
		} // end for

		output.setRows(rowsList.toArray(new ReportRow[0]));
		return output;
	}

	/**
	 * Main methods. calls the report output & manages possible errors to show
	 * to the user.
	 * 
	 * @param errorMsgs
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ReportOutput load(final Vector errorMsgsP) {

		StringBuffer error = new StringBuffer();

		try {

			return getReportOutput(errorMsgsP);

		} catch (RemoteException e) {
			error.append("Error generating Report.\n");
			error.append(e.getLocalizedMessage());
			Log.error(this, e);//Sonar
		} catch (OutOfMemoryError e2) {
			error.append("Not enough local memory to run this report, use more filters.\n");
			Log.error(this, e2);//Sonar
		} catch (Exception e3) {
			error.append("Error generating Report.\n");
			error.append(e3.getLocalizedMessage());
			Log.error(this, e3);//Sonar
		}

		Log.error(this, error.toString());
		errorMsgsP.add(error.toString());

		return null;
	}

	/**
	 * @return set of isins if the user fills the isin filter
	 */
	private Set<String> getIsinsFromFilter() {

		HashSet<String> isinSet = new HashSet<String>();
		final String isinsString = (String) getReportTemplate().get(SECURITIES);
		if (!Util.isEmpty(isinsString)) {
			isinSet = new HashSet<String>(Util.string2Vector(isinsString));
		}
		return isinSet;
	}

	/**
	 * Transforms the custom agreements filters into a String of Ids and passes
	 * it to the core template filter
	 */
	private void filterAgreementsByCore() {

		// 05/08/15. SBNA Multi-PO filter adapted to ST filter too
		// final String po = (String) getReportTemplate().get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
		final String po = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());

		final String agreementIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
		if (Util.isEmpty(po) && Util.isEmpty(agreementIds)) {
			return; // do nothing
		}
		// build query
		final StringBuilder sqlClause = new StringBuilder();
		sqlClause.append("SELECT mrg_call_def FROM mrgcall_config WHERE agreement_status = 'OPEN' ");

		// attach agreements ids filtered
		if (!Util.isEmpty(agreementIds)) {
			final Vector<String> agrIds = Util.string2Vector(agreementIds);
			sqlClause.append(" AND mrgcall_config.description IN ").append(Util.collectionToSQLString(agrIds));
		}

		// attachs the list of ids for the different POs filtered by the user
		if (!Util.isEmpty(po)) {

			final Vector<String> agrPosIds = Util.string2Vector(po);
			final Vector<String> agrPosIdsFinal = new Vector<String>(agrPosIds.size());

			for (String processingOrg : agrPosIds) {
				int poId = BOCache.getLegalEntityId(getDSConnection(), processingOrg.trim());
				if (poId > 0) {
					agrPosIdsFinal.add(poId + "");
				}
			}
			if (!agrPosIdsFinal.isEmpty()) {
				sqlClause.append(" AND mrgcall_config.process_org_id  IN ").append(
						Util.collectionToSQLString(agrPosIdsFinal));
			}
		}
		try {
			// retrieve contract ids
			ArrayList<Integer> marginCallConfigIds = SantReportingUtil.getSantReportingService(getDSConnection())
					.getMarginCallConfigIds(sqlClause.toString());

			HashSet<Integer> customIds = new HashSet<Integer>(marginCallConfigIds); // set1
			final String coreTemplateIds = (String) getReportTemplate().get("MARGIN_CALL_CONFIG_IDS");

			// attachs the agreement core template
			if (!Util.isEmpty(coreTemplateIds)) {

				HashSet<Integer> coreIds = new HashSet<Integer>(Util.string2IntVector(coreTemplateIds)); // set2
				customIds.retainAll(coreIds);// intersection
				if (customIds.isEmpty()) {
					customIds.add(0); // empty selection
				}
			}
			// retrieve final list of Ids and put it into the core template
			final String collateralConfigsStringIds = Util.collectionToString(customIds);
			getReportTemplate().put(CORE_TEMPLATE_MC_IDS, collateralConfigsStringIds);

		} catch (RemoteException e) {
			Log.error(this, "Cannot recover collaterals configs ids by service");
			Log.error(this, e);//Sonar
		}
	}

	/**
	 * AAP ST PO Filtering prevales over the others
	 */
	private void setCustomPOValuesIntoCoreAttr() {
		String panelPOValues = getReportTemplate().get(SantMarginCallPositionValuationReportTemplate.OWNER_AGR);
		String stPOValues = getReportTemplate().get(ScheduledTaskCSVREPORT.PO_NAME);
		if (!Util.isEmpty(panelPOValues))
			getReportTemplate().put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS, panelPOValues);
		if (!Util.isEmpty(stPOValues))
			getReportTemplate().put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS, stPOValues);
	}

	/**
	 * @param security
	 *            , either bond or equity
	 * @param quoteDate
	 *            to recover
	 * @return retrieves the quote value for the DirtyPrice quote set
	 */
	@SuppressWarnings("unchecked")
	private QuoteValue getDirtyPriceQv(final Product security, final JDate quoteDate) {

		Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
		if (security != null) {

			StringBuffer clausule = new StringBuffer("quote_name = '");
			clausule.append(security.getQuoteName()).append("' AND trunc(quote_date) = to_date('");
			
			if(security instanceof Equity){
				clausule.append(quoteDate).append(
						"', 'dd/mm/yy') AND quote_set_name = 'OFFICIAL'");
			}else{
				clausule.append(quoteDate).append(
						"', 'dd/mm/yy') AND quote_set_name = 'DirtyPrice' AND quote_type = 'DirtyPrice'");	
			}
			try {
				vQuotes = getDSConnection().getRemoteMarketData().getQuoteValues(clausule.toString());
			} catch (RemoteException e) {
				Log.error(this, "Unable to recover quoteName " + security.getQuoteName());
				Log.error(this, e);//Sonar
			}

			if ((vQuotes != null) && (vQuotes.size() > 0)) {
				return vQuotes.get(0);
			}
		}
		return null;
	}

	/**
	 * @param isins2filter
	 *            set of filters
	 * @param isin
	 *            to check if must be filtered
	 * @return true if ISIN must be filtered (not contained in the set)
	 */
	private boolean filterSecurityIsin(Set<String> isins2filter, final String isin) {

		if (isins2filter.isEmpty()) {
			return false;
		}
		if (Util.isEmpty(isin)) {
			return true;
		}

		return !isins2filter.contains(isin.trim());
	}

	/**
	 * @return execution date of the report
	 */
	protected JDate getProcessDate() {

		JDate date = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()), "PROCESS_DATE", "PROCESS_PLUS",
				"PROCESS_TENOR");

		if (date != null) {
			return date;
		}
		final String startDate = (String) getReportTemplate().getAttributes().get(ReportTemplate.START_DATE); // for
																											  // tests
		if (!Util.isEmpty(startDate)) {
			date = JDate.valueOf(startDate);
		}
		if (date == null) {
			date = getReportTemplate().getValDate(); // as param from the STRunner
		}
		if (date == null) {
			date = JDate.getNow().addBusinessDays(-1, Util.string2Vector("SYSTEM"));
		}
		return date;
	}
}
