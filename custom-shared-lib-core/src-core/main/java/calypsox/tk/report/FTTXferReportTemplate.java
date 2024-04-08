package calypsox.tk.report;

import com.calypso.tk.report.TransferReportTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FTTXferReportTemplate extends TransferReportTemplate {
    private static final long serialVersionUID = 2350719641778484191L;
    // columns
    public static final String REPORT_DATE = "Report Date";
    public static final String REPORT_ROW_SEQ = "Report Row Sequence";
    public static final String FTT_REFERENCE_ID = "FTT.Reference ID";
    public static final String FTT_STANDARD_S = "FTT.Standard_S";
    public static final String FTT_STANDARD_N = "FTT.Standard_N";
    public static final String FTT_TRADE_DATE = "FTT.Trade Date";
    public static final String FTT_SETTLE_DATE = "FTT.Settle Date";
    public static final String FTT_UNIT = "FTT.Unit";
    public static final String FTT_TAXABLE_FLAG = "FTT.Taxable Flag";
    public static final String FTT_EXONERATION_CODE = "FTT.Exoneration Code";
    public static final String FTT_TAX_AMOUNT = "FTT.Tax Amount";
    public static final String FTT_PLACE_OF_TRADE = "FTT.Place of Trade";
    public static final String FTT_NARRATIVE = "FTT.Narrative";
    public static final String FTT_QUANTITY = "FTT.Quantity";
    public static final String FTT_CASH_AMOUNT = "FTT.Cash Amount";

    @Override
    public void setDefaults() {
        super.setDefaults();
        String[] columns = super.getColumns();
        if(columns!=null && columns.length!=0){
            List<String> columnsToAdd = Arrays.asList(columns);
            List<String> newColumns = new ArrayList<String>();
            newColumns.addAll(columnsToAdd);
            newColumns.add(REPORT_DATE);
            newColumns.add(REPORT_ROW_SEQ);
            newColumns.add(FTT_REFERENCE_ID);
            newColumns.add(FTT_STANDARD_S);
            newColumns.add(FTT_STANDARD_N);
            newColumns.add(FTT_TRADE_DATE);
            newColumns.add(FTT_SETTLE_DATE);
            newColumns.add(FTT_QUANTITY);
            newColumns.add(FTT_UNIT);
            newColumns.add(FTT_CASH_AMOUNT);
            newColumns.add(FTT_TAXABLE_FLAG);
            newColumns.add(FTT_EXONERATION_CODE);
            newColumns.add(FTT_TAX_AMOUNT);
            newColumns.add(FTT_PLACE_OF_TRADE);
            newColumns.add(FTT_NARRATIVE);
            super.setColumns(newColumns.toArray(new String[newColumns.size()]));
        }
    }
}
