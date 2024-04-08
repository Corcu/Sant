package calypsox.tk.anacredit.processor;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.copys.Copy11Record;
import calypsox.tk.anacredit.api.copys.Copy13Record;
import calypsox.tk.anacredit.api.copys.Copy4ARecord;
import calypsox.tk.anacredit.api.copys.Copy4Record;
import calypsox.tk.anacredit.formatter.AnacreditFormatter;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AnacreditProcessor {

    protected Vector _holidays = new Vector();

    protected abstract ReportRow createCopy3OperRecord(ReportRow reportRow, PricingEnv pEnv, Vector errorMsgs);

    protected abstract List<Copy4ARecord> createCopy4APersonaRecord(ReportRow reportRow,  Vector errorMsgs);

    protected abstract List<Copy4Record>  createCopy4ImportesRecord(ReportRow reportRow, Vector errorMsgs);

    protected abstract List<Copy11Record> createCopy11GarantiasRealesRecord(CacheModuloD cache, ReportRow reportRow, Vector errorMsgs);

    protected abstract List<Copy13Record> createCopy13ActivosFinancerosRecord(CacheModuloD cache, ReportRow reportRow, Vector errorMsgs);

    protected abstract void preCalculatePricerMeasures(Report report, DefaultReportOutput output, List<ReportRow> reportRows, Vector errorMsgs);


    public void processReportRows(Report report, DefaultReportOutput output, List<ReportRow> reportRows, Vector errorMsgs) {

        _holidays = getHolidays(report.getReportTemplate());

        boolean needsCopy3  = true;
         // Only when executed by ScheduledTasks
        if (report.getReportTemplate().get(AnacreditConstants.USE_CACHED_ROWS) != null) {
            if (!Util.isEmpty(reportRows)) {
                for (ReportRow row : reportRows) {
                    if (row.getProperty(AnacreditConstants.COPY_3) != null) {
                        needsCopy3 = false;
                        Log.system(getClass().getSimpleName(), "### Excecuting from ScheduledTask. Using cached Copy3 data to generate other files");
                        break;
                    }
                }
            }
         }
        if (needsCopy3)  {
            preCalculatePricerMeasures(report, output, reportRows, errorMsgs );
            reportRows = processCopy3Operaciones(reportRows, report.getPricingEnv(), errorMsgs);
        }


        //preCalculatePricerMeasures(report, output, reportRows, errorMsgs );
        String rowDataType = report.getReportTemplate().get(AnacreditConstants.ROW_DATA_TYPE);
        if (Util.isEmpty(rowDataType)) {
            rowDataType = AnacreditConstants.COPY_3;
        }

        //reportRows = processCopy3Operaciones(reportRows, report.getPricingEnv(), errorMsgs);

        if (!Util.isEmpty(rowDataType)) {
            if (AnacreditConstants.COPY_3.equalsIgnoreCase(rowDataType)) {
                setRowsToDefaultOutput(rowDataType, reportRows, output);
            }
            else if (AnacreditConstants.COPY_4.equalsIgnoreCase(rowDataType)) {
                reportRows = processImportesCopy4(reportRows, errorMsgs);
                setRowsToDefaultOutput(rowDataType, reportRows, output);
            }
            else if (AnacreditConstants.COPY_4A.equalsIgnoreCase(rowDataType)) {
                reportRows = processCopy4APersona(reportRows, errorMsgs);
                setRowsToDefaultOutput(rowDataType, reportRows, output);
            }
            else if (AnacreditConstants.COPY_11.equalsIgnoreCase(rowDataType)) {
                reportRows = processCopy11GarantiasReales( reportRows, errorMsgs);
                setRowsToDefaultOutput(rowDataType, reportRows, output);
            }
            else if (AnacreditConstants.COPY_13.equalsIgnoreCase(rowDataType)) {
                reportRows = processCopy13ActivosFinanceros(reportRows, errorMsgs);
                setRowsToDefaultOutput(rowDataType, reportRows, output);
            }
        }
    }


    /**
     * Generate Entries for Copy3 - Operaciones
     * @param reportRows
     * @param errorMsgs
     * @return
     */
    private  List<ReportRow> processCopy3Operaciones(List<ReportRow> reportRows, PricingEnv pEnv, Vector errorMsgs) {
        ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();
        reportRows.stream().forEach(reportRow -> {

            ReportRow r = createCopy3OperRecord(reportRow, pEnv, errorMsgs);
            if (r != null) {
                syncList.add(reportRow);
            }
        });

        Log.system(LogBase.CALYPSOX, "### Records for copy3 generated :" + syncList.size());
        List<ReportRow> result = new ArrayList<>();
        result.addAll(syncList);
        return result;
    }

    protected List<ReportRow> processCopy4APersona(List<ReportRow> reportRows, Vector errorMsgs) {
        ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();
        reportRows.stream().parallel().forEach(reportRow -> {
            List<Copy4ARecord> personas = createCopy4APersonaRecord(reportRow, errorMsgs);
            if (personas != null) {
                personas.stream().forEach(persona -> {
                    ReportRow clone = reportRow.clone();
                    clone.setProperty(AnacreditConstants.COPY_4A, persona);
                    syncList.add(clone);
                });
            }
        });
        List<ReportRow> result = new ArrayList<>();
        result.addAll(syncList);
        return result;
    }

    protected List<ReportRow> processImportesCopy4(List<ReportRow> reportRows, Vector errorMsgs) {
        ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();
        reportRows.stream().parallel().forEach(reportRow -> {
            List<Copy4Record> importes = createCopy4ImportesRecord(reportRow, errorMsgs);
            if (importes != null) {
                importes.stream().forEach(importe -> {
                    ReportRow clone = reportRow.clone();
                    clone.setProperty(AnacreditConstants.COPY_4, importe);
                    syncList.add(clone);
                });
            }
        });
        List<ReportRow> result = new ArrayList<>();
        result.addAll(syncList);
        return result;
    }


    protected CacheModuloD buildCacheData(List<ReportRow> reportRows) {
        CacheModuloD cache =  new CacheModuloD();
        cache.buildCacheModuloD(reportRows);
        return cache;
    }


    private List<ReportRow> processCopy11GarantiasReales(List<ReportRow> reportRows, Vector errorMsgs) {

        CacheModuloD cache = buildCacheData(reportRows);

        ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();
        reportRows.stream().forEach(reportRow -> {
            List<Copy11Record> recordList = createCopy11GarantiasRealesRecord(cache, reportRow, errorMsgs);
            if (recordList != null) {
                recordList.stream().forEach(record -> {
                    ReportRow clone = reportRow.clone();
                    clone.setProperty(AnacreditConstants.COPY_11, record);
                    syncList.add(clone);
                });
            }
        });
        List<ReportRow> result = new ArrayList<>();
        result.addAll(syncList);
        return result;

    }

    private List<ReportRow> processCopy13ActivosFinanceros(List<ReportRow> reportRows, Vector errorMsgs) {

        CacheModuloD cache = buildCacheData(reportRows);
        ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();
        reportRows.stream().forEach(reportRow -> {
            List<Copy13Record> recordList = createCopy13ActivosFinancerosRecord(cache, reportRow, errorMsgs);
            if (recordList != null) {
                recordList.stream().forEach(record -> {
                    ReportRow clone = reportRow.clone();
                    clone.setProperty(AnacreditConstants.COPY_13, record);
                    syncList.add(clone);
                });
            }
        });
        List<ReportRow> result = new ArrayList<>();
        result.addAll(syncList);
        return result;

    }

    protected abstract AnacreditFormatter getFormatter();

    protected synchronized void setRowsToDefaultOutput(String rowDataType, List<ReportRow> items, DefaultReportOutput output) {
        if (output != null) {
            ReportRow[] rows = new ReportRow[items.size()];
            for (int i = 0; i < items.size(); i++) {

                if (items.get(i) != null) {
                    ReportRow row = items.get(i);
                    row.setProperty(AnacreditConstants.ROW_DATA_TYPE, rowDataType);

                    rows[i] = row;
                }

            }
            output.setRows(rows);
        }
    }

    protected void log(AnacreditFormatter.LogLevel level, long identifier, String message, Vector<String> errors ) {
        createMessage(level, String.valueOf(identifier), message, errors);
    }

    protected void log(AnacreditFormatter.LogLevel level, String identifier, String message, Vector<String> errors ) {
       createMessage(level, identifier, message, errors);
    }


    public static void createMessage(AnacreditFormatter.LogLevel level, String identifier, String message, Vector<String> errors ) {
        StringBuilder sb = new StringBuilder();
        sb.append(level.getLevel()).append(" - ");
        if (identifier != null) {
            sb.append("[").append(identifier).append("] - ");
        }
        sb.append(message);
        if (null != errors) {
            if (!errors.contains(sb.toString())) {
                synchronized (errors) {
                    errors.add(sb.toString());
                }
            }
        }
    }

    protected Vector getHolidays(ReportTemplate template) {
        Vector holidays = new Vector<>();
        if (template.getHolidays() != null) {
            holidays = template.getHolidays();
        } else {
            holidays.add("SYSTEM");
        }
        return holidays;
    }

}
