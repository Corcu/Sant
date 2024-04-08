package calypsox.tk.report;

public class AccountingNotificationTemplate extends SantInterestNotificationReportTemplate {
    public static final String ROW_DATA = "SantInterestNotificationEntry";

    @Override
    public void setDefaults() {
        super.setDefaults();
        setColumns(AccountingNotificationReportStyle.ADDITIONAL_COLUMNS);
    }
}
