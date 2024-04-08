package calypsox.tk.report;

import calypsox.tk.report.extracontable.MICExtracontableBean;
import calypsox.tk.report.extracontable.MICExtracontableMarginCallBuilder;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.report.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;

public class MarginCallCashMICExtracontableReport extends BOCashPositionReport {

    static final String ROW_PROP_NAME = "MIC_DATA";
    //16 zeros
    static final String ACCOUNT_ID_ZERO = "0000000000000000";

    @Override
    public ReportOutput load(Vector errorMsgs){
        ReportOutput output=super.load(errorMsgs);

        StandardReportOutput standardReportOutput = new StandardReportOutput(this);
        standardReportOutput.setRows(Optional.ofNullable(output)
                .map(outputOpt -> ((DefaultReportOutput) outputOpt).getRows()).orElse(new ReportRow[0]));
        initMICBeans(standardReportOutput);

        //filter out rows that have zero as account id, these are not wanted
        Vector<ReportRow> nonEmptyRows = new Vector<>();
        for (ReportRow row : standardReportOutput.getRows()) {
            MICExtracontableBean bean = row.getProperty(ROW_PROP_NAME);
            if (!ACCOUNT_ID_ZERO.equalsIgnoreCase(bean.getAccountId().getContent())) {
                nonEmptyRows.add(row);
            }
        }

        ReportRow[] nonEmptyRowsArray = new ReportRow[nonEmptyRows.size()];
        standardReportOutput.setRows(nonEmptyRows.toArray(nonEmptyRowsArray));

        return standardReportOutput;
    }


    private void initMICBeans(DefaultReportOutput output) {
        Arrays.stream(output.getRows()).parallel().forEach(this::enrichReportRow);
    }

    private void enrichReportRow(ReportRow originalReportRow) {
        Inventory position = originalReportRow.getProperty(Inventory.class.getSimpleName());
        if (position != null) {
            MICExtracontableMarginCallBuilder builder = new MICExtracontableMarginCallBuilder(position);
            originalReportRow.setProperty(ROW_PROP_NAME, builder.build());
        }
    }

}
