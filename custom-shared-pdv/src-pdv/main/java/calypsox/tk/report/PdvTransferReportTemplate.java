package calypsox.tk.report;

import com.calypso.tk.report.TransferReportTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PdvTransferReportTemplate extends TransferReportTemplate {

    private static final long serialVersionUID = 1L;
    // columns
    public static final String TOMADO_PRESTADO = "Tomado/Prestado";
    public static final String SIGNO_COMISION = "Signo Comision";
    public static final String INDEMNITY_ACCRUAL = "PM Indemnity Accrual";
    public static final String REPORT_DATE = "Report Date";


    @Override
    public void setDefaults() {
        super.setDefaults();
        String[] columns = super.getColumns();
        if(columns!=null && columns.length!=0){
            List<String> columnsToAdd = Arrays.asList(columns);
            List<String> newColumns = new ArrayList<String>();
            newColumns.addAll(columnsToAdd);
            newColumns.add(TOMADO_PRESTADO);
            newColumns.add(SIGNO_COMISION);
            newColumns.add(INDEMNITY_ACCRUAL);
            newColumns.add(REPORT_DATE);
            super.setColumns(newColumns.toArray(new String[newColumns.size()]));
        }
    }
}

