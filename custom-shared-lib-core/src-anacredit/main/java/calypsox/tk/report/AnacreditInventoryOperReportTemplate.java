package calypsox.tk.report;


public class AnacreditInventoryOperReportTemplate extends BOSecurityPositionReportTemplate {

    public static final String ROW_DATA = "AnacreditOperacionesItem";

    public AnacreditInventoryOperReportTemplate() {
        super();
        setDefaults();
    }

    @Override
    public void setDefaults() {
        setColumns(AnacreditInventoryOperReportStyle.DEFAULT_COLUMNS);
    }

    @Override
    public void setDefaultDateColumns() {

    }

}
