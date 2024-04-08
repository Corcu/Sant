package calypsox.tk.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jfree.util.Log;

import com.calypso.tk.collateral.MarginCallDetailEntry;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.MarginCallEntryFactory;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.MarginCallDetailReportTemplate;
import com.calypso.tk.report.ReportOutput;

import calypsox.tk.report.restservices.WebServiceReport;
import calypsox.util.CheckRowsNumberReport;

public class MarginCallDetailReport extends com.calypso.tk.report.MarginCallDetailReport
		implements CheckRowsNumberReport, WebServiceReport {

	private static final long serialVersionUID = 3570475534699968519L;
	private static String injectionQuery = null;

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput load(Vector errorMsgs) {
		List<MarginCallEntry> entries = ((MarginCallDetailReportTemplate) this._reportTemplate).getMarginCallEntries();
		if (Util.isEmpty(entries)) {
			String mce_id = this.getReportTemplate().get("MCE_ID");
			if (!Util.isEmpty(mce_id)) {
				List<Integer> lstEntries = Util.string2IntVector(mce_id);
				if (!Util.isEmpty(lstEntries)) {
					try {
						entries = new ArrayList<MarginCallEntry>();
						List<MarginCallEntryDTO> dtos = ServiceRegistry.getDefault().getCollateralServer()
								.loadEntries(lstEntries);
						for (MarginCallEntryDTO marginCallEntryDTO : dtos) {
							entries.add(MarginCallEntryFactory.getInstance(marginCallEntryDTO)
									.createMarginCallEntry(marginCallEntryDTO));
						}
						((MarginCallDetailReportTemplate) this._reportTemplate).setMarginCallEntries(entries);
					} catch (CollateralServiceException e) {
						Log.error(e);
					}
				}
			}
		}
		int size = 0;
		if (!Util.isEmpty(entries)) {
			for (MarginCallEntry marginCallEntry : entries) {
				List<MarginCallDetailEntry> detEntry = marginCallEntry.getDetailEntries();
				if (!Util.isEmpty(detEntry)) {
					size += detEntry.size();
				}
			}
		}
		addPotentialSize("MarginCallDetail", size);

		return super.load(errorMsgs);
	}

	@Override
	public Map getPotentialSize() {
		Vector<String> errors = new Vector<>();
		this._potentialSize = new HashMap<String, Integer>();
		load(errors);
		return this._potentialSize;
	}

	@Override
	public ReportOutput loadFromWS(String query, Vector<String> errorMsgs) {
		injectionQuery = query;
		ReportOutput output = super.load(errorMsgs);
		injectionQuery = null;
		return output;
	}

}
