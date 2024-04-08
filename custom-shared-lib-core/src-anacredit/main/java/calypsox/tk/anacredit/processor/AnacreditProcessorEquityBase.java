package calypsox.tk.anacredit.processor;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.copys.*;
import calypsox.tk.anacredit.formatter.AnacreditFormatter;
import calypsox.tk.anacredit.formatter.AnacreditFormatterEquityBase;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Process ReportRows to produce Copy3, Copy4 and Copy4A records according to template selection
 */
public class AnacreditProcessorEquityBase extends AnacreditProcessor {

    private AnacreditFormatterEquityBase _formatter = new AnacreditFormatterEquityBase();

    protected ReportRow createCopy3OperRecord(ReportRow reportRow, PricingEnv pEnv, Vector errorMsgs) {
        Copy3Record r = _formatter.formatRecOperacionesCopy3(reportRow, pEnv, _holidays, errorMsgs);
        if (r != null) {
            //Set copy3 type on ReportRow
            reportRow.setProperty(AnacreditConstants.COPY_3, r);
            return reportRow;
        }
        return null;

    }

    @Override
    protected List<Copy4ARecord> createCopy4APersonaRecord(ReportRow reportRow, Vector errorMsgs) {
        return _formatter.formatCopy4APersona(reportRow, errorMsgs);
    }

    @Override
    protected List<Copy4Record> createCopy4ImportesRecord(ReportRow reportRow, Vector errorMsgs) {
        return _formatter.formatImportesCopy4(reportRow, errorMsgs);
    }

    @Override
    protected List<Copy11Record> createCopy11GarantiasRealesRecord(CacheModuloD cache, ReportRow reportRow, Vector errorMsgs) {
        return new ArrayList<>();
    }

    @Override
    protected List<Copy13Record> createCopy13ActivosFinancerosRecord(CacheModuloD cache, ReportRow reportRow, Vector errorMsgs) {
        return new ArrayList<>();
    }

    @Override
    protected AnacreditFormatter getFormatter() {
        return _formatter;
    }


    @Override
    protected List<ReportRow> processCopy4APersona(List<ReportRow> reportRows, Vector errorMsgs) {
        ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();
        reportRows.stream().forEach(reportRow -> {
            List<Copy4ARecord> personas = createCopy4APersonaRecord(reportRow, errorMsgs);
            if (personas != null) {
                personas.stream().forEach(persona -> {
                    ReportRow clone = reportRow.clone();
                    clone.setProperty(AnacreditConstants.COPY_4A, persona);
                    clone.setUniqueKey(new WrapperKey());
                    syncList.add(clone);
                });
            }
        });
        List<ReportRow> result = new ArrayList<>();
        result.addAll(syncList);
        return result;
    }



    @Override
    protected List<ReportRow> processImportesCopy4(List<ReportRow> reportRows, Vector errorMsgs) {
        ConcurrentLinkedQueue<ReportRow> syncList = new ConcurrentLinkedQueue<>();
        reportRows.stream().forEach(reportRow -> {
            List<Copy4Record> importes = createCopy4ImportesRecord(reportRow, errorMsgs);
            if (importes != null) {
                importes.stream().forEach(importe -> {
                    ReportRow clone = reportRow.clone();
                    clone.setProperty(AnacreditConstants.COPY_4, importe);
                    // Allow dulplicates in Inventory Report
                    clone.setUniqueKey(new WrapperKey());
                    //System.out.println("millis :"+s);
                    syncList.add(clone);
                });
            }
        });
        List<ReportRow> result = new ArrayList<>();
        result.addAll(syncList);
        return result;
    }

    public class WrapperKey {
        public WrapperKey() {
        }
        public Object  getUniqueKey() {
            return new Long(System.nanoTime()+ System.currentTimeMillis());
        }
        public String  toString() {
            return getUniqueKey().toString();
        }
    }


    @Override
    protected void preCalculatePricerMeasures(Report report, DefaultReportOutput output, List<ReportRow> reportRows, Vector errorMsgs) {
        if (reportRows == null)  {
            return;
        }
       Log.system(this.getClass().getSimpleName(), "Start to calculate Pricer Measures for cache.");
        StopWatch watch = new StopWatch();
        watch.start();
        reportRows.stream().forEach(reportRow -> {
        });
        watch.stop();
        System.out.println("### Total execution time to calculate Measures : " + watch.getElapsedTimeMs());

        Log.system(this.getClass().getSimpleName(), "Calculate PM done! cached for " + reportRows.size() + " records.");
    }
}
