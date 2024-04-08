package calypsox.tk.report;

import calypsox.tk.anacredit.formatter.IPersonaFormatter;
import calypsox.tk.anacredit.items.AnacreditOperacionesItem;
import calypsox.tk.anacredit.items.AnacreditPersonaOperacionesItem;
import calypsox.tk.anacredit.loader.AnacreditLoader;
import calypsox.tk.anacredit.util.AnacreditFactory;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AnacreditPersonaOperacionesReport extends AnacreditAbstractReport {


    @Override
    protected List<ReportRow> extendReportRows(List<ReportRow> allRows, Vector<String> errors) {
        Log.system("AnacreditPersonaOperacionesReport", "start to generate Persona (copty4a) file..");
        ArrayList<ReportRow> result = new ArrayList<>();

        for (ReportRow row: allRows) {
            AnacreditOperacionesItem item = row.getProperty(AnacreditOperacionesReportTemplate.ROW_DATA);
            String extractionType =  row.getProperty(AnacreditOperacionesReportTemplate.ANACREDIT_EXTRACTION_TYPE);
            IPersonaFormatter formatter = AnacreditFactory.instance().getPersonaFormatter(extractionType);
            // type safe only
            if (formatter != null)  {
                List<AnacreditPersonaOperacionesItem> personaItems = formatter.formatPersonaItem(item, row,  getValDate(), errors);
                for (AnacreditPersonaOperacionesItem personaItem : personaItems) {
                    ReportRow clone = (ReportRow) row.clone();
                    AnacreditLoader.addRowData(result, clone, personaItem);
                }
            }
        }

        return result;
    }
}
