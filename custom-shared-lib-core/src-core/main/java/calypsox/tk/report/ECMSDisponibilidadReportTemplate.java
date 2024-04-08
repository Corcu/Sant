package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Locale;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;

public class ECMSDisponibilidadReportTemplate extends BOSecurityPositionReportTemplate {
    @Override
    public void setDefaultDateColumns() {

    }
    @Override
    public String[] getColumns(boolean forConfig) {
        String[] ret = super.getColumns(forConfig);
        ArrayList<String> arr=new ArrayList<String>();
        for (String stringVal : ret) {
            JDate dateObj = null;
            dateObj = Util.MStringToDate(stringVal,Locale.getDefault(),true);
            if(dateObj==null) {
                arr.add(stringVal);
            }
        }
        return arr.toArray(new String[arr.size()]);
    }
}
