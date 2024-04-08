package calypsox.tk.anacredit.processor;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.copys.*;
import calypsox.tk.anacredit.formatter.AnacreditFormatter;
import calypsox.tk.anacredit.formatter.AnacreditFormatterRepo;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.util.StopWatch;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Process ReportRows to produce Copy3, Copy4 and Copy4A records according to template selection
 */
public class AnacreditProcessorRepo extends AnacreditProcessorSecFinance {

    private AnacreditFormatterRepo _formatter = new AnacreditFormatterRepo();

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
    protected List<Copy11Record> createCopy11GarantiasRealesRecord(CacheModuloD cachedData, ReportRow reportRow, Vector errorMsgs) {
        return  _formatter.formatCopy11Garantias(cachedData, reportRow, errorMsgs);
    }

    @Override
    protected List<Copy13Record> createCopy13ActivosFinancerosRecord(CacheModuloD cache, ReportRow reportRow, Vector errorMsgs) {
        return  _formatter.formatCopy13ActivosFinanceros(cache, reportRow, errorMsgs);
    }

    @Override
    protected void preCalculatePricerMeasures(Report report, DefaultReportOutput output, List<ReportRow> reportRows, Vector errorMsgs) {

        if (reportRows == null)  {
            return;
        }

        Log.system(this.getClass().getSimpleName(), "Start to calculate Pricer Measures for cache.");

        TradeReportStyle trs = new TradeReportStyle();

        StopWatch watch = new StopWatch();
        watch.start();

        reportRows.stream().forEach(reportRow -> {
            Trade trade = reportRow.getProperty(ReportRow.TRADE);
            if (!(trade.getProduct() instanceof SecFinance)) {
                return;
            }
            final List<String> columns = Arrays.asList(TradeReportStyle.PRINCIPAL_AMOUNT,
                    "Pricer.MTM_NET_MUREX", "Sec. Nominal (Current)", "Pricer.ACCRUAL_FIRST", "Quantity");

            trs.precalculateColumnValues(reportRow, (String[]) columns.toArray(), errorMsgs);
            for (String column : columns) {

                Object resultObj = trs.getColumnValue(reportRow, column, errorMsgs);

                if (null != resultObj
                        && resultObj instanceof Amount)   {
                    Double nominal = ((Amount)resultObj).get();
                    if (nominal != null) {
                        reportRow.setProperty(column, nominal);
                    }
                } else {
                    reportRow.setProperty(column, resultObj);
                }
            }
        });

        watch.stop();
        //System.out.println("Total execution time to calculate Measures : " + watch.getElapsedTime());

        Log.system(this.getClass().getSimpleName(), "Calculate PM done! cached for " + reportRows.size() + " records.");
    }

    @Override
    protected AnacreditFormatter getFormatter() {
        return _formatter;
    }


}
