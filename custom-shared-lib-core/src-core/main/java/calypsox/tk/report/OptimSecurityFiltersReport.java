package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.OptimizationConfiguration;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.optimization.impl.Category;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

public class OptimSecurityFiltersReport extends Report {

	private static final long serialVersionUID = 4988134544656532409L;
	public static final String OPTIM_SECURITY_FILTERS_REPORT = "OptimSecurityFiltersReport";

	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

		if (this._reportTemplate == null) {
			return null;
		}
		final Vector<String> errorMsgs = errorMsgsP;

		String configIdsStr = (String) getReportTemplate().get(OptimSecurityFiltersReportTemplate.OPTIM_CONFIG_IDS);

		if (Util.isEmpty(configIdsStr)) {
			errorMsgs.add("Please select at least one Optimization config");
			return null;
		}

		List<OptimizationConfiguration> configs = null;
		try {
			Vector<Integer> optimConfigIds = Util.string2IntVector(configIdsStr);
			configs = getOptimConfigs(optimConfigIds);

		} catch (RemoteException e) {
			Log.error(this, e); //sonar
		}

		final DefaultReportOutput output = new DefaultReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		for (OptimizationConfiguration config : configs) {
			List<Category> categories = config.getTarget().getCategories();
			for (Category category : categories) {
				if (category.getType().equals(Category.TYPE_SECURITY)) {
					StaticDataFilter securityFilter = category.getSecurityFilter();
					Vector<StaticDataFilterElement> elements = securityFilter.getElements();
					for (StaticDataFilterElement filterElement : elements) {
						ReportRow row = new ReportRow(config);
						row.setProperty(OptimSecurityFiltersReportTemplate.OPTIM_CONFIG, config);
						row.setProperty(OptimSecurityFiltersReportTemplate.TARGET_CATEGORY, category);
						row.setProperty(OptimSecurityFiltersReportTemplate.FILTER_ELEMENT, filterElement);
						reportRows.add(row);
					}
				}
			}

		}
		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

		return output;
	}

	private List<OptimizationConfiguration> getOptimConfigs(Vector<Integer> optimConfigIds) throws RemoteException {
		List<OptimizationConfiguration> configsList = new ArrayList<OptimizationConfiguration>();

		List<OptimizationConfiguration> allOptimizationConfigs = ServiceRegistry.getDefault().getCollateralDataServer()
				.loadAllOptimizationConfiguration();
		for (OptimizationConfiguration config : allOptimizationConfigs) {
			if (optimConfigIds.contains(config.getId())) {
				configsList.add(config);
			}
		}

		return configsList;
	}

	@SuppressWarnings("unused")
	public static void main(final String... args) throws ConnectException {

		final DSConnection ds = ConnectionUtil.connect(args, "Test");

		final OptimSecurityFiltersReport report = new OptimSecurityFiltersReport();

	}
}