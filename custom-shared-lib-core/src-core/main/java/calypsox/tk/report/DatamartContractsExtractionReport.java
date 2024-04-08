package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import calypsox.tk.report.DatamartContractsExtractionLogic.ContractWrapper;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;

public class DatamartContractsExtractionReport extends MarginCallReport {

	private static final long serialVersionUID = 123L;

	private static final String DATAMART_FULL_EXTRACTION = "Full extraction to Datamart";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ReportOutput load(final Vector errorMsgsP) {

		try {
			return getReportOutput();
		} catch (RemoteException e) {
			String error = "Error generating Datamart Contracts Extraction\n";
			Log.error(this, error, e);
			errorMsgsP.add(error + e.getMessage());
		}
		return null;

	}

	@SuppressWarnings("rawtypes")
	private DefaultReportOutput getReportOutput() throws RemoteException {

		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
		Map<Integer, CollateralConfig> contractsMap = new HashMap<Integer, CollateralConfig>();

		final ReportTemplate reportTemp = getReportTemplate();

		// get full extraction attribute
		final Attributes attributes = reportTemp.getAttributes();
		String fullExtractionAtt = (String) attributes.get(DATAMART_FULL_EXTRACTION);
		Boolean isFullExtraction = Boolean.parseBoolean(fullExtractionAtt);

		// get date & holidays
		Vector holidays = reportTemp.getHolidays();
		JDate processDate = reportTemp.getValDate();
		JDate valueDate = processDate.addBusinessDays(-1, holidays);

		// get contracts
		contractsMap = loadContracts(isFullExtraction, processDate);
		if (!Util.isEmpty(contractsMap)) {
			for (CollateralConfig contract : contractsMap.values()) {

				// Exclude CSA Facade	
				if (KGR_Collateral_MarginCallReport.CSA_FACADE.equals(contract.getContractType())) {
					continue;
				}
				
				// GSM 15/07/15. SBNA Multi-PO filter
				if (CollateralUtilities.filterPoByTemplate(reportTemp, contract)) {
					continue;
				}

				if (contract != null) {
					ReportRow row = new ReportRow(contract, ReportRow.MARGIN_CALL_CONFIG);
					row.setProperty(DatamartContractsExtractionReportTemplate.PROCESS_DATE, processDate);
					//row.setProperty(DatamartContractsExtractionReportTemplate.VAL_DATE, valueDate);
					row.setProperty(DatamartContractsExtractionReportTemplate.CONTRACT_WRAPPER, new ContractWrapper(
							contract, processDate, valueDate));
					reportRows.add(row);

				}

			}
		}

		// set report rows on output
		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
		return output;

	}

	/* Load contracts depending on isFullExtraction */
	private Map<Integer, CollateralConfig> loadContracts(boolean isFullExtraction, JDate processDate) {

		ArrayList<Integer> contractsIds = new ArrayList<Integer>();
		Map<Integer, CollateralConfig> contractsMap = new HashMap<Integer, CollateralConfig>();
		String query = new String();
		
		// get query
		if (isFullExtraction) {
			query = getFullContractsQuery(processDate);
		} else {
			query = getDailyContractsQuery(processDate);
		}

		// get contracts
		try {
			contractsIds = SantReportingUtil.getSantReportingService(getDSConnection()).getMarginCallConfigIds(query);
			contractsMap = SantReportingUtil.getSantReportingService(getDSConnection()).getMarginCallConfigByIds(
					contractsIds);
		} catch (RemoteException e) {
			Log.error(this, "Error getting contracts ids from DB", e);
		} catch (PersistenceException e) {
			Log.error(this, "Error getting contracts from DB", e);
		}

		return contractsMap;

	}

	/* Get query for load new or modified contracts on day passed */
	private String getDailyContractsQuery(JDate processDate) {

		String selectClause = "select distinct entity_id ";
		String fromClause = "from bo_audit ";
		String whereClause = "where (entity_class_name = 'MarginCallConfig' OR entity_class_name = 'CollateralConfig') AND trunc(modif_date) = "
				+ Util.date2SQLString(processDate);

		return selectClause + fromClause + whereClause;

	}

	/* Get query for load all contracts on system */
	private String getFullContractsQuery(JDate processDate) {

		String selectClause = "select mrg_call_def ";
		String fromClause = "from mrgcall_config ";

		return selectClause + fromClause;

	}
}
