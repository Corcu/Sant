package calypsox.tk.report;

import calypsox.apps.reporting.AnacreditOperacionesReportTemplatePanel;
import calypsox.tk.anacredit.loader.AnacreditLoader;
import calypsox.tk.anacredit.loader.AnacreditLoaderUtil;
import calypsox.tk.anacredit.util.AnacreditFactory;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallPositionDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallPositionEntryReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class AnacreditAbstractReport extends MarginCallPositionEntryReport implements  IAnacreditReport {

    private static final String ALL = "ALL";
    protected static final String LINE = "LINE";
    private static final String MARGIN_CALL_CONFIG_IDS = "MARGIN_CALL_CONFIG_IDS";
    public static final String INTEREST_BEARING = "INTEREST_BEARING";
    public static final String F_ACCOUNT = "F_ACCOUNT";
    ConcurrentHashMap<Integer, CollateralConfig> contractMap = null;


    /**
     * Do the transformation to each record type per file extraction
     * OPERACIONES, PERSONA and IMPORTES
     * @param allRows
     * @return
     */
    protected abstract  List<ReportRow> extendReportRows (List<ReportRow> allRows, Vector<String> errors);

    /**
     * Load Report Output Rows
     *
     * @param errorMsgs
     *
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ReportOutput load(final Vector errorMsgs) {
        StringBuilder error = new StringBuilder();
        try {
            errorMsgs.clear();
            return getReportOutput(errorMsgs);

     } catch (OutOfMemoryError e2) {
            error.append("Not enough local memory to run this report, use more filters.\n");
            Log.error(this, e2);//Sonar
        } catch (Exception e3) {
            e3.printStackTrace();
            error.append("Error generating Report.\n");
            error.append(e3.getLocalizedMessage());
            Log.error(this, e3);//Sonar
        }
        Log.error(this, error.toString());
        errorMsgs.add(error.toString());
        printLog(errorMsgs);
        return null;
    }

    /**
     * Build and return one ReportOutput object from a ReportRow provided list.
     * @param items
     * @param errorMsgs
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ReportOutput buildReportOutputFrom(ReportRow[] items, Vector errorMsgs) {
        final DefaultReportOutput output = new DefaultReportOutput(this);
        List<ReportRow> cachedItems = Arrays.asList(items);
        prepareFinalResultSet(errorMsgs, output, cachedItems);
        return output;
    }

    /**
     * Generates report output by running
     * @param errorMsgs error messages
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ReportOutput getReportOutput(Vector errorMsgs) throws PersistenceException {
        DefaultReportOutput output = new DefaultReportOutput(this);
        JDate valDate =  getReportTemplate().getValDate();

        List<CollateralConfig> configs = selectContractsToReport();
        if (Util.isEmpty(configs)) {
            return output;
        }

        // only 1000 in arguments for SQL
        ReportRow[] rows = loadBulk(configs, errorMsgs);
        configs = getCollateralContractList(rows);
        List<ReportRow> reportRows = collectAllRows(configs, rows, valDate, errorMsgs);
        prepareFinalResultSet(errorMsgs, output, reportRows);
        return output;
    }

    private List<CollateralConfig> getCollateralContractList(ReportRow[] rows){
        HashMap<Integer,CollateralConfig> finalContractList = new HashMap<>();
        List<ReportRow> keeper = new ArrayList<>(Arrays.asList(rows));

        for (final ReportRow row : keeper) {
            MarginCallPositionDTO mcef = row.getProperty("Default");
            int marginCallConfigId = mcef.getMarginCallConfigId();
            if(!finalContractList.containsKey(marginCallConfigId)){
                finalContractList.put(marginCallConfigId,contractMap.get(marginCallConfigId));
            }
        }
        return new ArrayList<>(finalContractList.values());
    }

    /**
     * Perform the extension od the Record Type based on the Type of report executed
     * @param errorMsgs
     * @param output
     * @param reportRows
     */
    private void prepareFinalResultSet(Vector errorMsgs, DefaultReportOutput output, List<ReportRow> reportRows) {
        List<ReportRow> allRows = extendReportRows(reportRows, errorMsgs);
        setRows(allRows, output);
        printLog(errorMsgs);
    }


    private List<CollateralConfig> selectContractsToReport() throws PersistenceException {
        Map<Integer, CollateralConfig> contractsMap = AnacreditLoader.loadAnacreditContracts();
        contractMap = new ConcurrentHashMap<Integer, CollateralConfig>();

        List<CollateralConfig> result = new LinkedList<CollateralConfig>();
        List<String> loaders = getTypeOfExtractionList();
        loaders.stream().forEach(loaderName -> {
            AnacreditLoader loader = AnacreditFactory.instance().getLoader(loaderName);
            List<CollateralConfig> tmp = loader.selectContractsToReport(contractsMap, getValDate());
            tmp.forEach(contract -> {
                //if(contract.getId()==17238659)
                    contractMap.putIfAbsent(contract.getId(), contract);
            });
        });

        // Finally the final list of contracts
        List<CollateralConfig> list = contractMap.values().stream().collect(Collectors.toList());
        return list;
    }

    private ReportRow[] loadBulk(List<CollateralConfig> configs, Vector errorMsgs) {
        Log.system(this.getClass().getName(), String.format("### Loading positions report for %s contracts started.", configs.size()));
        List<ReportRow> keeper = new ArrayList<>();
        List<String> idsInLines = AnacreditLoaderUtil.contractListToIdString(configs);

        for (String inLineIds : idsInLines) {
            //String param = AnacreditLoaderUtil.idsToCommaSeparatedString(configs);
            if (!Util.isEmpty(inLineIds)) {
                getReportTemplate().put(MARGIN_CALL_CONFIG_IDS, inLineIds);
                // call core report load
                getReportTemplate().callBeforeLoad();
                // Load Positions for each round
                ReportRow[] rows = ((DefaultReportOutput) super.load(errorMsgs)).getRows();
                if (rows != null) {
                    keeper.addAll(Arrays.asList(rows));
                }
            }
        }

        //Mata los duplicados por book (Bug de Calypso Core)
        Map<String, ReportRow> roadMap = new HashMap<>();

        for (final ReportRow row : keeper) {
            MarginCallPositionDTO mcef = row.getProperty("Default");
            Account account = AnacreditLoaderUtil.loadAccount(String.valueOf( mcef.getMarginCallConfigId()), mcef.getCurrency());
            Book book = AnacreditLoaderUtil.getBook(mcef);
            String bookAccountingLink = AnacreditLoaderUtil.getBookAccountingLink(book);

            if(account!=null && book!=null){
                int marginCallConfigId = mcef.getMarginCallConfigId();
                String key = account.getLongId()+"_"+marginCallConfigId+"_"+mcef.getCurrency()+"_"+bookAccountingLink;
                //In case of duplicate, take just Negative Position.
                if(roadMap.containsKey(key)){
                    ReportRow reportRow = roadMap.get(key);
                    MarginCallPositionDTO aDefault = (MarginCallPositionDTO) reportRow.getProperty("Default");
                    if(aDefault.getAllInValue()>0 && mcef.getAllInValue()<0){
                        roadMap.put(key, row);
                    }
                }else if (!roadMap.containsKey(key)){
                    roadMap.put(key, row);
                }
            }
        }
        keeper.clear();
        keeper.addAll(roadMap.values());
        ReportRow[] rows = new ReportRow[keeper.size()];
        keeper.toArray(rows); // fill the array

        Log.system(this.getClass().getName(), String.format("### Loading positions report for %s contracts finished sucessfully. rows= %d", configs.size(), keeper.size()));
        return rows;

    }

    /**
     * Collect given Positions from original report and enrich with more data when necessary
     * according to the type pf extraction specified
     * @param configs
     * @param rows
     * @param valDate
     * @return
     */
    protected List<ReportRow>  collectAllRows(List<CollateralConfig> configs, ReportRow[] rows, JDate valDate, Vector<String> errorMsgs) {
        ArrayList<ReportRow> result = new ArrayList<>();
        List<String> extracionList = getTypeOfExtractionList();
        for (String extractionType : extracionList) {
            AnacreditLoader loader = AnacreditFactory.instance().getLoader(extractionType);
            result.addAll(loader.collectAllRows(extractionType, configs, rows, valDate, getPricingEnv(), errorMsgs));
        }
        return result;
    }

    private List<String> getTypeOfExtractionList() {
        String s = getReportTemplate().get(AnacreditOperacionesReportTemplate.ANACREDIT_EXTRACTION_TYPE);
        if (Util.isEmpty(s))   {
            s = ALL;
        }
        if (s.contains(ALL)) {
            List<String>  typesDomain = AnacreditOperacionesReportTemplatePanel.getExtractionTypesDomain();
            if (!Util.isEmpty(typesDomain)) {
               return  typesDomain.stream()
                       .filter(type  -> !type.equals(ALL)).collect(Collectors.toList());
            }
            return typesDomain;
        }
        return Util.string2Vector(s);
    }


    private void setRows(List<ReportRow> items, DefaultReportOutput output) {
        final ReportRow[] rows = new ReportRow[items.size()];
        for (int i = 0; i < items.size(); i++) {
            final ReportRow row = items.get(i);
            String blankLine = "";
            row.setProperty(LINE,blankLine);
            rows[i] = row;
        }
        output.setRows(rows);
    }

    private void printLog(Vector<String> errorMsgs){
        if(!Util.isEmpty(errorMsgs)){
            for(String line : errorMsgs){
                Log.info("Anacredit: ",line.trim());
            }
        }
    }
}