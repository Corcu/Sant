package calypsox.tk.report;

public class PirumNotificationReportTemplate extends SantInterestNotificationReportTemplate {
    @Override
    public void setDefaults() {
        super.setDefaults();
        setColumns(PirumNotificationReportStyle.ADDITIONAL_COLUMNS);
    }
}
