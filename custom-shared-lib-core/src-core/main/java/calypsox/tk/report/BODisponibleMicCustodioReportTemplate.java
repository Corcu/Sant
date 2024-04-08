package calypsox.tk.report;

import com.calypso.tk.core.Util;

import java.util.Vector;

public class BODisponibleMicCustodioReportTemplate extends BODisponibleSecurityPositionReportTemplate{
    public String[] getColumns(boolean forConfig) {
        Vector<String> columns = toVector(this.getColumns());
        return Util.collection2StringArray(columns);
    }
}
