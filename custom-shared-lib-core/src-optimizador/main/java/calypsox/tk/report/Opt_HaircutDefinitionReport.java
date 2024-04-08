package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.Haircut;
import com.calypso.tk.refdata.HaircutPoint;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class Opt_HaircutDefinitionReport extends MarginCallReport {

	private static final long serialVersionUID = 123L;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ReportOutput load(final Vector errorMsgsP) {

		try {
			return getReportOutput();
			
		} catch (RemoteException e) {
			String error = "Error generating Optimization_HaircutDefinitionReport.\n";
			Log.error(this, error, e);
			errorMsgsP.add(error + e.getMessage());
		}

		return null;

	}

	/**
	 * Get report output
	 * 
	 * @return
	 * @throws RemoteException
	 */
	private DefaultReportOutput getReportOutput() throws RemoteException {

		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		// load contracts
		Collection<CollateralConfig> contracts = loadOpenContracts();
		if (Util.isEmpty(contracts)) {
			Log.info(this, "Cannot find any open contract.\n");
			return null;
		}

		// load items
		List<Opt_HaircutDefinitionItem> haircutDefItems = buildItems(contracts);
		for (Opt_HaircutDefinitionItem haircutDefItem : haircutDefItems) {

			ReportRow row = new ReportRow(haircutDefItem.getContract(), ReportRow.MARGIN_CALL_CONFIG);
			row.setProperty(Opt_HaircutDefinitionReportTemplate.OPT_HAIRCUT_DEF_ITEM, haircutDefItem);
			reportRows.add(row);

		}

		// set report rows on output
		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

		return output;

	}

	/**
	 * Load all contracts in the system with status OPEN
	 * 
	 * @return
	 */
	public Collection<CollateralConfig> loadOpenContracts() {

		ArrayList<Integer> contractsIds = new ArrayList<Integer>();
		Map<Integer, CollateralConfig> contractsMap = new HashMap<Integer, CollateralConfig>();
		String query = "select mrg_call_def from mrgcall_config where agreement_status = 'OPEN'";
		
		// GSM 21/07/15. SBNA Multi-PO filter
		query = CollateralUtilities.filterPoByQuery(getReportTemplate(),query);

		try {
			// get contract ids
			contractsIds = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).getMarginCallConfigIds(
					query);
			// get contracts
			contractsMap = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
					.getMarginCallConfigByIds(contractsIds);
		} catch (RemoteException e) {
			Log.error(Opt_HaircutDefinitionReport.class, "Cannot get contract ids from DB", e);
		} catch (PersistenceException e) {
			Log.error(Opt_HaircutDefinitionReport.class, "Cannot get contracts from DB", e);
		}

		return contractsMap.values();

	}

	/**
	 * Build data items
	 * 
	 * @param contracts
	 * @return
	 */
	public static List<Opt_HaircutDefinitionItem> buildItems(Collection<CollateralConfig> contracts) {

		List<Opt_HaircutDefinitionItem> items = new ArrayList<Opt_HaircutDefinitionItem>();

		for (CollateralConfig contract : contracts) {

			// if (contract.getName().contains("CSA - CSFP") || contract.getName().contains("MMOO - TH0B")) {
			// System.out.println("stop ");
			// }

			// get eligible security filter names from contract
			List<String> eligibleSecFilterNames = contract.getEligibilityFilterNames();
			if (Util.isEmpty(eligibleSecFilterNames)) {
				continue;
			}

			// build haircut rule's haircut definitions map
			HashMap<String, Haircut> haircutDefinitionsMap = Opt_HaircutDefinitionReport
					.buildHaircutDefinitionsMap(contract);
			if (Util.isEmpty(haircutDefinitionsMap)) {
				continue;
			}

			// check if eligilible sec filters are linked to haircut rule, in this case get data
			for (String filterName : eligibleSecFilterNames) {

				if (haircutDefinitionsMap.containsKey(filterName.trim())) {
					// get haircut points from haircut definition
					List<HaircutPoint> haircutPoints = haircutDefinitionsMap.get(filterName.trim()).getPoints();
					if (Util.isEmpty(haircutPoints)) {
						continue;
					}
					for (HaircutPoint haircutPoint : haircutPoints) {
						// build item
						Opt_HaircutDefinitionItem item = new Opt_HaircutDefinitionItem(contract, filterName.trim(),
								haircutPoint);
						items.add(item);
					}
				}

			}

		}

		return items;

	}

	/**
	 * Build a map with contract eligible sec filter names and haircut definitions linked to these filters
	 * 
	 * @param contract
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static HashMap<String, Haircut> buildHaircutDefinitionsMap(CollateralConfig contract) {

		HashMap<String, Haircut> haircutDefinitionsMap = new HashMap<String, Haircut>();

		try {
			List<Haircut> haircutDefs = DSConnection.getDefault().getRemoteReferenceData()
					.getHaircuts(contract.getHaircutName());
			for (Haircut haircutDef : haircutDefs) {
				if (haircutDef.getSecFilter() != null) {
					haircutDefinitionsMap.put(haircutDef.getSecFilter().getName().trim(), haircutDef);
				}
			}
		} catch (Exception e) {
			Log.error(Opt_HaircutDefinitionReport.class, "Cannot get haircut definitions from haircut rule = "
					+ contract.getHaircutName() + "\n", e);
		}

		return haircutDefinitionsMap;

	}

}
