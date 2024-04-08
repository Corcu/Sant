package calypsox.tk.report;

import com.calypso.tk.report.MarginCallPositionEntryReportTemplate;

import java.util.Vector;

public class SantBostonCollateralLevelReportTemplate extends MarginCallPositionEntryReportTemplate {


    private static final String PROCCES_DATE = "ProccesDate";
    private static final String DIRECTION = "Direction";


    @Override
    public void setDefaults() {
        super.setDefaults();
        			
        final Vector<String> columns = new Vector<String>();
        
        columns.addElement(PROCCES_DATE);
        columns.addElement(DIRECTION);
        
        setColumns(columns.toArray(new String[columns.size()]));
    }



    @Override
    protected Vector<String> getDefaultColumns() {
        return super.getDefaultColumns();
    }


}
