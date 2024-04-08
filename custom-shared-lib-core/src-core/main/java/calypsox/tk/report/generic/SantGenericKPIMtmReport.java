package calypsox.tk.report.generic;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import calypsox.tk.report.KPIMtmIndividualItem;
import calypsox.tk.report.KPIMtmReportItem;
import calypsox.tk.report.SantReport;
import calypsox.util.SantReportingUtil;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

public abstract class SantGenericKPIMtmReport extends SantReport {

	private static final long serialVersionUID = 1L;

	public static final String KPIMtmByAgreementOwner = "SantKPIMtmByAgreementOwner";
	public static final String KPIMtmByEconomicSector = "SantKPIMtmByEconomicSector";
	public static final String KPIMtmByInstrument = "SantKPIMtmByInstrument";
	public static final String KPIMtmByPortfolios = "SantKPIMtmByPortfolios";
	public static final String KPIMtmByAgreement = "SantKPIMtmByAgreement";

	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput loadReport(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

		if (this._reportTemplate == null) {
			return null;
		}

		List<KPIMtmReportItem> kpiMtmReportItems = null;

		try {
			final String query = buildQuery();
			final List<KPIMtmIndividualItem> kpiMtmIndividualItems = SantReportingUtil.getSantReportingService(
					getDSConnection()).getKPIMtmReportItems(query, getType());

			kpiMtmReportItems = buildKPIMtmReportItems(kpiMtmIndividualItems);

		} catch (final MarketDataException e) {
			Log.error(SantGenericKPIMtmReport.class, "Error building buildKPIMtmReportItems", e);
			errorMsgsP.add(e.getMessage());
		} catch (final RemoteException e) {
			Log.error(SantGenericKPIMtmReport.class, "Error loading KPIMtmReportItem", e);
			errorMsgsP.add(e.getMessage());
		}

		// initDates();
		final DefaultReportOutput output = new DefaultReportOutput(this);

		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		if ((kpiMtmReportItems != null) && (kpiMtmReportItems.size() > 0)) {
			for (final KPIMtmReportItem item : kpiMtmReportItems) {
				final ReportRow row = new ReportRow(item);
				row.setProperty(SantGenericKPIMtmReportTemplate.VALUATION_DATE, getProcessStartDate());
				reportRows.add(row);
			}
		}

		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

		return output;
	}

	@Override
	protected boolean checkProcessEndDate() {
		return false;
	}

	protected JDate getValueDate() {
		return getProcessStartDate().addBusinessDays(-1, getReportTemplate().getHolidays());
	}

	/**
	 * This method converts the NPV values to EUR and USD seperately, does the sums and then build KPIMtmReportItem to
	 * display in the report
	 * 
	 * @param individualItems
	 * @return
	 * @throws MarketDataException
	 */
	public abstract List<KPIMtmReportItem> buildKPIMtmReportItems(final List<KPIMtmIndividualItem> individualItems)
			throws MarketDataException;

	public abstract String buildQuery();
}