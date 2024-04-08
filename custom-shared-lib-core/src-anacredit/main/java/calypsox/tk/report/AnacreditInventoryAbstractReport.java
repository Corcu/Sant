package calypsox.tk.report;

import calypsox.tk.anacredit.loader.AnacreditLoader;
import calypsox.tk.anacredit.util.AnacreditFactory;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;


import java.util.*;
import java.util.stream.Collectors;

public abstract class AnacreditInventoryAbstractReport extends BOSecurityPositionReport
    implements  IAnacreditReport {

    private static final String ALL = "ALL";
    protected static final String LINE = "LINE";
    public static final String DIRTY_PRICE = "DIRTY_PRICE";


    public AnacreditInventoryAbstractReport()  {
        super();
    }

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
        processRows(errorMsgs, output, cachedItems);
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

        final List<CollateralConfig> configs = new ArrayList<>();
        // only 1000 in arguments for SQL
        //ReportRow[]  rows = loadBulk(configs, errorMsgs);

        ReportRow[] rows = ((DefaultReportOutput) super.load(errorMsgs)).getRows();

        List<ReportRow> reportRows = collectAllRows(configs, rows, valDate, errorMsgs);
        processRows(errorMsgs, output, reportRows);
        return output;
    }

    /**
     * Perform the extension od the Record Type based on the Type of report executed
     * @param errorMsgs
     * @param output
     * @param reportRows
     */
    private void processRows(Vector errorMsgs, DefaultReportOutput output, List<ReportRow> reportRows) {
        List<ReportRow> allRows = extendReportRows(reportRows, errorMsgs);
        setRows(allRows, output);
        printLog(errorMsgs);
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
            List<String>  typesDomain = getExtractionTypesDomain();
            if (!Util.isEmpty(typesDomain)) {
               return  typesDomain.stream()
                       .filter(type  -> !type.equals(ALL)).collect(Collectors.toList());
            }
            return typesDomain;
        }
        return Util.string2Vector(s);
    }

    public static List<String> getExtractionTypesDomain() {
        List<String> exTypes = LocalCache.getDomainValues(DSConnection.getDefault(), AnacreditOperacionesReportTemplate.DV_ANACREDIT_EXTRACTION_TYPES);
        if (Util.isEmpty(exTypes)) {
            exTypes = Arrays.asList("ALL", "SecuritiesRF", "SecuritiesRV");
        }
        return exTypes;
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