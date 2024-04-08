package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import calypsox.util.SantReportingUtil;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantAgreementListByISINReport extends Report {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

		if (getReportTemplate() == null) {
			return null;
		}

		String isin = (String) getReportTemplate().get(SantAgreementListByISINReportTemplate.ISIN);
		if (Util.isEmpty(isin)) {
			errorMsgsP.add("Please enter an ISIN.");
			return null;
		}

		ArrayList<CollateralConfig> eligibleAgreements = null;

		try {
			Product product = DSConnection.getDefault().getRemoteProduct().getProductByCode("ISIN", isin);
			if (product == null) {
				errorMsgsP.add("Please enter a valid ISIN.");
				return null;
			}

			eligibleAgreements = getEligibleAgreements(product, errorMsgsP);
		} catch (Exception e) {
			Log.error(this, e); //sonar
		}

		if (eligibleAgreements == null) {
			return null;
		}

		final DefaultReportOutput output = new DefaultReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
		for (int i = 0; i < eligibleAgreements.size(); i++) {
			ReportRow row = new ReportRow(eligibleAgreements.get(i));
			row.setProperty("MarginCallConfig", eligibleAgreements.get(i));
			reportRows.add(row);
		}

		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
		return output;
	}

	@SuppressWarnings("rawtypes")
	private ArrayList<CollateralConfig> getEligibleAgreements(Product security, Vector errorMsgsP) throws Exception {

		String sqlMcList = "select mrg_call_def from mrgcall_config where le_coll_type in ('SECURITY','BOTH')";
		ArrayList<Integer> marginCallConfigIds = SantReportingUtil.getSantReportingService(getDSConnection())
				.getMarginCallConfigIds(sqlMcList);
		Map<Integer, CollateralConfig> agreements = SantReportingUtil.getSantReportingService(getDSConnection())
				.getMarginCallConfigByIds(marginCallConfigIds);

		if (Util.isEmpty(agreements)) {
			return null;
		}

		Iterator<Integer> iterator = agreements.keySet().iterator();

		ArrayList<CollateralConfig> finalList = new ArrayList<CollateralConfig>();

		while (iterator.hasNext()) {
			Integer agrId = iterator.next();
			CollateralConfig agreement = agreements.get(agrId);
			List<StaticDataFilter> agrFilters = agreement.getEligibilityFilters();
			if (!Util.isEmpty(agrFilters)) {
				for (int i = 0; i < agrFilters.size(); i++) {
					StaticDataFilter secFilter = agrFilters.get(i);
					if (secFilter.accept(null, security)) {
						finalList.add(agreement);
						break;
					}
				}
			}

		}

		return finalList;
	}
}
