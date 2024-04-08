package calypsox.tk.report;

import calypsox.tk.bo.fiflow.factory.CalypsoToTCyCCMsgBuilder;
import calypsox.tk.bo.fiflow.model.CalypsoToTCyCCBean;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;

import java.util.*;

/**
 * @author aalonsop
 * A file containing FI open trades is generated to be sent to TCyCC system.
 */
public class MarginCallTradeFIFlowReport extends TradeReport {

    static final String ROW_PROP_NAME = "TCyCCBean";

    private static final long serialVersionUID = -1655127533046540816L;

    @Override
    public ReportOutput load(Vector errorMsgs) {
        ReportOutput output = super.load(errorMsgs);
        if(null != output) {
            ReportRow[] rows = ((DefaultReportOutput) output).getRows();
            List<ReportRow> filteredList = Arrays.asList(rows);
            return initCarterasReportOutput(filteredList);
        }
        return output;
    }

    private StandardReportOutput initCarterasReportOutput(List<ReportRow> rows) {
        StandardReportOutput standardReportOutput = new StandardReportOutput(this);
        ReportRow[] rowArray = Optional.ofNullable(rows).map(r -> rows.toArray(new ReportRow[0])).orElse(new ReportRow[0]);
        standardReportOutput.setRows(rowArray);
        initTCyCCBeans(standardReportOutput);
        return standardReportOutput;
    }

    private void initTCyCCBeans(DefaultReportOutput output) {
        if(output!=null) {
            Arrays.stream(output.getRows()).parallel().forEach(this::enrichReportRow);
        }
    }

    private void enrichReportRow(ReportRow originalReportRow) {
        Trade trade = originalReportRow.getProperty(Trade.class.getSimpleName());
        CalypsoToTCyCCMsgBuilder msgBuilder =
                new CalypsoToTCyCCMsgBuilder(trade,getValDate());
        CalypsoToTCyCCBean carterasBean = msgBuilder.build();
        originalReportRow.setProperty(ROW_PROP_NAME, carterasBean);
    }

}
