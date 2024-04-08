package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

public class SantCreditRatingReport extends Report {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ReportOutput load(Vector errorMsgs) {

		final DefaultReportOutput output = new DefaultReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		// load contracts with PO
		final Collection<CollateralConfig> contracts = loadContracts();

		if (!Util.isEmpty(contracts)) {

			// Get the credit rating by contract by LE
			final Vector<CreditRating> vCreditRating = getMCCreditEntries(contracts);

			for (CreditRating creditRating : vCreditRating) {
				final SantCreditRatingItem item = new SantCreditRatingItem(
						creditRating);

				final ReportRow row = new ReportRow(item,
						SantCreditRatingItem.CREDIT_RATING_ITEM);
				reportRows.add(row);
			}

			output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
		} else {
			Log.info(SantCreditRatingReport.class, "Cannot find any contract.");
		}

		return output;
	}

	/**
	 * Get all contracts by PO
	 * 
	 * @return contracts
	 */
	private Collection<CollateralConfig> loadContracts() {

		MarginCallConfigFilter contractFilter = new MarginCallConfigFilter();

		// Select POs
		List<Integer> poIds = SantCreditRatingReportLogic
				.getProcessingOrgIds(getReportTemplate());
		if (poIds != null && poIds.size() > 0) {
			contractFilter.setProcessingOrgIds(poIds);
		}

		// Select POs - End

		Log.info(SantCreditRatingReport.class, "Load Contracts with PO: "
				+ poIds);

		final List<CollateralConfig> contracts = SantCreditRatingReportLogic
				.loadContracts(contractFilter);

		Log.info(SantCreditRatingReport.class, "Number of Contracts with PO: "
				+ contracts.size());

		return contracts;
	}

	/**
	 * Get MC CreditRating by LegalEntity entries
	 * 
	 * @param list
	 * @return
	 */
	private Vector<CreditRating> getMCCreditEntries(
			Collection<CollateralConfig> list) {

		Log.info(SantCreditRatingReport.class, "Load CreditRating by LE.");

		Vector<CreditRating> creditRatingsFinal = new Vector<CreditRating>();

		if (!Util.isEmpty(list)) {

			Iterator<CollateralConfig> iterator = list.iterator();
			while (iterator.hasNext()) {
				final CollateralConfig contract = iterator.next();
				final Vector<String> agencies = contract.getEligibleAgencies();
				final int leId = contract.getLegalEntity().getLegalEntityId();

				final Vector<CreditRating> creditRatings = SantCreditRatingReportLogic
						.getCreditRatings(leId, agencies, getValDate());

				if (creditRatings != null && !creditRatings.isEmpty()) {
					creditRatingsFinal.addAll(creditRatings);
				}
			}
		}

		return creditRatingsFinal;
	}

}
