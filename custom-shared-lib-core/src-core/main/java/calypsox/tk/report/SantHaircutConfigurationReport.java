package calypsox.tk.report;

import calypsox.tk.util.concentrationlimits.SantConcentrationLimitsUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CrossCurrencyHaircut;
import com.calypso.tk.refdata.haircut.HaircutRule;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

//Project: Concentration Limits

public class SantHaircutConfigurationReport extends MarginCallReport {

    private static final long serialVersionUID = 1234L;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(final Vector errorMsgsP) {

        try {
            return getReportOutput();

        } catch (RemoteException e) {
            String error = "Error generating SantHaircutConfigurationReport.\n";
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
        Collection<CollateralConfig> contracts = loadContracts();
        if (Util.isEmpty(contracts)) {
            Log.info(this, "Cannot find any contract.\n");
            return null;
        }

        // load items
        List<SantHaircutConfigurationItem> haircutDefItems = buildItems(
                contracts);
        for (SantHaircutConfigurationItem haircutDefItem : haircutDefItems) {

            ReportRow row = new ReportRow(haircutDefItem.getContract(),
                    ReportRow.MARGIN_CALL_CONFIG);
            row.setProperty(
                    SantHaircutConfigurationReportTemplate.HAIRCUT_CONF_ITEM,
                    haircutDefItem);
            reportRows.add(row);

        }

        // set report rows on output
        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

        return output;

    }

    /**
     * Load all contracts in the system
     *
     * @return
     * @throws CollateralServiceException
     */
    private Collection<CollateralConfig> loadContracts()
            throws CollateralServiceException {

        MarginCallConfigFilter contractFilter = new MarginCallConfigFilter();

        // Select POs in Concentration Limit reports
        List<Integer> poIds = SantConcentrationLimitsUtil
                .getProcessingOrgIds(getReportTemplate());
        if (poIds != null && poIds.size() > 0) {
            contractFilter.setProcessingOrgIds(poIds);
        }
        // Select POs in Concentration Limit reports - End

        List<CollateralConfig> contracts = CollateralManagerUtil
                .loadCollateralConfigs(contractFilter);

        return contracts;
    }

    /**
     * Build data items
     *
     * @param contracts
     * @return
     * @throws CollateralServiceException
     */
    private static List<SantHaircutConfigurationItem> buildItems(
            Collection<CollateralConfig> contracts)
            throws CollateralServiceException {

        List<SantHaircutConfigurationItem> items = new ArrayList<SantHaircutConfigurationItem>();

        for (CollateralConfig contract : contracts) {

            // build haircut rule list
            List<CrossCurrencyHaircut> crossCcyList = getCrossCcyList(contract);

            // Build the item
            for (CrossCurrencyHaircut crossCcy : crossCcyList) {

                String ccy1 = crossCcy.getLiabilityCcy(); // CCY1
                String ccy2 = crossCcy.getCollateralCcy(); // CCY2
                String haircutValue = crossCcy.getValueAsRate().toString(); // Haircut
                // Add-on

                SantHaircutConfigurationItem item = new SantHaircutConfigurationItem(
                        contract, ccy1, ccy2, haircutValue);
                items.add(item);
            }
        }

        return items;

    }

    /**
     * Get the cross currency list of contract
     *
     * @param contract
     * @return
     */
    private static List<CrossCurrencyHaircut> getCrossCcyList(
            CollateralConfig contract) {

        List<CrossCurrencyHaircut> crossCcyRules = new ArrayList<>();

        try {
            // Get the haircut rules
            final HaircutRule hr = ServiceRegistry.getDefault()
                    .getCollateralDataServer()
                    .getHaircutRule(contract.getHaircutName());

            // Get the cross currency haircut
            crossCcyRules = hr.getCrossCurrencyRules();

        } catch (Exception e) {
            Log.error(SantHaircutConfigurationReport.class,
                    "Cannot get haircut rule from haircut = "
                            + contract.getHaircutName() + "\n",
                    e);
        }

        return crossCcyRules;

    }
}
