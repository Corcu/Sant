package calypsox.tk.anacredit.loader;

import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.report.AnacreditOperacionesReportTemplate;
import com.calypso.tk.collateral.dto.MarginCallPositionDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AnacreditLoader {


    private static final String ANACREDIT = "ANACREDIT";

    /**
     * load Anacredit Data items concerning to given contract list
     *
     * @param extractionType
     * @param configs
     * @param valDate
     * @param errors
     * @return
     */
    protected abstract List<ReportRow> loadData(String extractionType, List<CollateralConfig> configs, ReportRow[] rows, JDate valDate, PricingEnv pEnv, Vector<String> errors) ;

    public abstract List<CollateralConfig> selectContractsToReport(Map<Integer, CollateralConfig> contracts, JDate valDate);


    /**
     * Collect all rows
     */
    public final  List<ReportRow> collectAllRows(String extractionType, List<CollateralConfig> configs, ReportRow[] rows, JDate valDate, PricingEnv pEnv, Vector<String> errors) {
        Log.system(this.getClass().getName(), "loader " + extractionType + " started.");
        Long timemillis = System.currentTimeMillis();
        List<ReportRow> result  = loadData(extractionType, configs, rows, valDate, pEnv, errors);
        result.stream().forEach(reportRow -> {
            reportRow.setProperty("EXTRACTION_TYPE", extractionType);
        });
        long time = (System.currentTimeMillis() - timemillis)/100;
        Log.system(this.getClass().getName(), String.format("Loader %s, finished %s records in %s seconds.", extractionType, result.size(), time));
        return result;
    };

    public static Map<Integer, CollateralConfig> loadAnacreditContracts() throws PersistenceException {

        List<CollateralConfig> filteredContracts = new ArrayList<>();
        RemoteSantCollateralService remoteSantColService = DSConnection.getDefault()
                .getRMIService("baseSantCollateralService", RemoteSantCollateralService.class);
        HashMap<String,String> additionalFields = new HashMap<>();
        additionalFields.put(ANACREDIT,"True");

        List<CollateralConfig> marginCallConfigByAdditionalField = remoteSantColService.getMarginCallConfigByAdditionalField(additionalFields);
        Map<Integer, CollateralConfig> map = AnacreditLoaderUtil.buildContractsMap(marginCallConfigByAdditionalField);
        return map;
    }

    /*
    protected  boolean isValidContract(CollateralConfig config, MarginCallPositionDTO position) {
        return true;
    }
    */
    public static void addRowData(ArrayList<ReportRow> result, ReportRow reportRow, AnacreditOperacionesItem item) {
        if (item != null) {
            reportRow.setProperty(AnacreditOperacionesReportTemplate.ROW_DATA, item);
            result.add(reportRow);
        }
    }


}


