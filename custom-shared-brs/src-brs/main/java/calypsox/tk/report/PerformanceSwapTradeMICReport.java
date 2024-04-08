package calypsox.tk.report;

import calypsox.tk.report.extracontable.MICExtracontableBRSBuilder;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;

/**
 * @author dmenendd
 * A file containing info trade is generated to be sent to MIC system.
 */
public class PerformanceSwapTradeMICReport extends TradeReport {

    static final String ROW_PROP_NAME = "ACyGFileAtrBRS";

    private static final long serialVersionUID = -1655127533046540816L;

    @Override
    public ReportOutput load(Vector errorMsgs) {
        ReportOutput output = super.load(errorMsgs);
        StandardReportOutput standardReportOutput=new StandardReportOutput(this);
        standardReportOutput.setRows(Optional.ofNullable(output)
                .map(outputOpt-> ((DefaultReportOutput)outputOpt).getRows()).orElse(new ReportRow[0]));
        initMICBeans(standardReportOutput);
        return standardReportOutput;
    }


    private void initMICBeans(DefaultReportOutput output) {
        //Como un for de una lista
        Arrays.stream(output.getRows()).parallel().forEach(this::enrichReportRow);
    }

    private void enrichReportRow(ReportRow originalReportRow) {
        MICExtracontableBRSBuilder msgBuilder =
                new MICExtracontableBRSBuilder(originalReportRow.getProperty(Trade.class.getSimpleName()));
        originalReportRow.setProperty(ROW_PROP_NAME, msgBuilder.build());
    }

}
