package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralManagerUtil;

import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

public class SantCreditRatingReportLogic {

	private static final String FITCH = "Fitch";
	private static final String SYP = "S&P";
	private static final String MOODY = "Moody";
	private static final String SC = "SC";

	private static final String[] DEFAULT_AGENCIES = { FITCH, SYP, MOODY, SC };

	// Copy from SantConcentrationLimitsUtil
	/**
	 * Returns a list containing the ids of the selected Processing Orgs.
	 * 
	 * @param reportTemplate
	 *            Report Template that uses a Processing Org selector
	 * 
	 * @return A list of Processing Org Ids
	 */
	public static List<Integer> getProcessingOrgIds(
			final ReportTemplate reportTemplate) {
		List<Integer> poIdsList = new ArrayList<Integer>();

		Object rawPOIds = reportTemplate
				.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
		if (rawPOIds != null && rawPOIds instanceof String) {
			String poIdsString = (String) rawPOIds;
			String[] poIdsArray = poIdsString.split(",");
			for (int iPoId = 0; iPoId < poIdsArray.length; iPoId++) {
				try {
					poIdsList.add(Integer.parseInt(poIdsArray[iPoId]));
				} catch (NumberFormatException e) {
					Log.error(SantCreditRatingReportLogic.class
							.getCanonicalName(), String.format(
							"Could not parse \"%s\" as integer",
							poIdsArray[iPoId]), e);
				}
			}
		}

		return poIdsList;
	}

	/**
	 * Load contracts using a filter MarginCallConfigFilter.
	 * 
	 * @param contractFilter
	 * @return
	 */
	public static List<CollateralConfig> loadContracts(
			final MarginCallConfigFilter contractFilter) {
		List<CollateralConfig> contracts = new ArrayList<CollateralConfig>();
		try {
			contracts = CollateralManagerUtil
					.loadCollateralConfigs(contractFilter);
		} catch (CollateralServiceException e) {
			Log.error(SantCreditRatingReportLogic.class,
					"Cannot get contracts. Error: " + e.getMessage());
		}

		return contracts;
	}

	/**
	 * Get all CreditRatings by LE id, eligible agencies and process date.
	 * 
	 * @param leId
	 * @param agencies
	 * @param processDate
	 * @return
	 */
	public static Vector<CreditRating> getCreditRatings(final int leId,
			Vector<String> agencies, final JDate processDate) {

		Vector<CreditRating> vectorCreditRatings = new Vector<CreditRating>();

		if (Util.isEmpty(agencies)) {
			agencies = new Vector<String>();
			agencies.addAll(Arrays.asList(DEFAULT_AGENCIES));
		}

		for (String agency : agencies) {
			final CreditRating creditRating = getCreditRating(leId, agency,
					processDate);

			if (creditRating != null) {
				vectorCreditRatings.add(creditRating);
			}
		}

		return vectorCreditRatings;
	}

	/**
	 * Get a CreditRating by LE id, agency and process date.
	 * 
	 * @param leId
	 * @param agency
	 * @param processDate
	 * @return
	 */
	private static CreditRating getCreditRating(final int leId,
			final String agency, final JDate processDate) {

		JDate asOfDate = processDate;
		if (processDate == null) {
			asOfDate = JDate.getNow();
		}

		CreditRating cr = null;

		try {
			cr = DSConnection.getDefault().getRemoteMarketData()
					.getLatestRating(leId, agency, asOfDate);
		} catch (CalypsoServiceException e) {
			Log.error(
					SantCreditRatingReportLogic.class,
					"Could not retrieve CreditRating from database. "
							+ e.getMessage(), e);
		}

		return cr;
	}

}
