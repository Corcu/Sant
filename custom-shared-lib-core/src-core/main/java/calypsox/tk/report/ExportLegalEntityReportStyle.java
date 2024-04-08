package calypsox.tk.report;

import com.calypso.tk.report.MarginCallReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class ExportLegalEntityReportStyle extends MarginCallReportStyle {

    public static final String GENERATION_DATE = "Generation Date";
    public static final String CP_DESC = "Counterparty Description";
    public static final String PROCESSING_ORG = "Processing Org";
    public static final String COUNTERPARTY = "Counterparty";
    public static final String OVERNIGHT = "MarginCallConfig.Legal Entity.Attribute.Overnight";
    public static final String EFFECTIVE_DATE = "MarginCallConfig.Legal Entity.Attribute.EffectiveDate";

    private static final Map<String, String> columnFormat;
    private static final long serialVersionUID = -5833424675566674107L;

    static {
        columnFormat = new HashMap<>();
        columnFormat.put(GENERATION_DATE, "DD/MM/YYYY");
    }

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        ExportLegalEntityItem item = row.getProperty(ExportLegalEntityItem.EXPORT_LE_ITEM);
        if (GENERATION_DATE.equals(columnName)) {
            return item.getGenerationDate();
        } else if (CP_DESC.equals(columnName)) {
            return item.getCounterpartyDescription();
        } else if (PROCESSING_ORG.equals(columnName)) {
            return item.getProcessingOrg();
        } else if (COUNTERPARTY.equals(columnName)) {
            return item.getCounterparty();
        } else if (OVERNIGHT.equals(columnName)) {
            return item.getOvernight();
        } else if (EFFECTIVE_DATE.equals(columnName)) {
            return item.getGetEffectiveDate();
        } else {
            return super.getColumnValue(row, columnName, errors);
        }
    }
}
