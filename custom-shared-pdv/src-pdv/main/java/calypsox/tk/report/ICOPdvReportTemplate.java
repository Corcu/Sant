package calypsox.tk.report;

import com.calypso.tk.report.TransferReportTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ICOPdvReportTemplate extends TransferReportTemplate {

    private static final long serialVersionUID = 1L;
    public static final String REPORT_DATE = "Report Date";


    @Override
    public void setDefaults() {
        super.setDefaults();
        String[] columns = super.getColumns();
        if(columns!=null && columns.length!=0){
            List<String> columnsToAdd = Arrays.asList(columns);
            List<String> newColumns = new ArrayList<String>();
            newColumns.addAll(columnsToAdd);
            newColumns.add(REPORT_DATE);
            super.setColumns(newColumns.toArray(new String[newColumns.size()]));
        }
    }
}

