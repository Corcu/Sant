package calypsox.tk.report;

import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;


public class EquityMicEodReport extends EquityMicCarteraReport {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


    @SuppressWarnings("rawtypes")
    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput defaultReportOutput = null;
        ReportOutput reportOutput = (ReportOutput)super.load(errorMsgs);
        ConcurrentLinkedQueue<ReportRow> finalRows = new ConcurrentLinkedQueue<>();

        if (reportOutput instanceof DefaultReportOutput) {
            defaultReportOutput = (DefaultReportOutput) reportOutput;
            ReportRow[] rows = defaultReportOutput.getRows();
            EquityMicEodReportStyle reportStyle = new EquityMicEodReportStyle();
            List list = new ArrayList<String>();
            Arrays.stream(rows).parallel().forEach(row -> {
                String empresa = (String) reportStyle.getColumnValue(row,"EMPRECON",errorMsgs);
                String centro = (String) reportStyle.getColumnValue(row,"CENTRO",errorMsgs);
                String producto = (String) reportStyle.getColumnValue(row,"CDPRODUC",errorMsgs);
                if(!Util.isEmpty(empresa) && !Util.isEmpty(centro) && !Util.isEmpty(producto)) {
                    String linea = empresa + ";" + centro + ";" + producto;
                    if (!list.contains(linea)) {
                        list.add(linea);
                        finalRows.add(row);
                    }
                }
            });
            final ReportRow[] finalReportRows = finalRows.toArray(new ReportRow[finalRows.size()]);
            defaultReportOutput.setRows(finalReportRows);
        }

        return defaultReportOutput;
    }


}