package calypsox.tk.report;


public class MessageSwiftReportTemplate extends com.calypso.tk.report.MessageReportTemplate {

    /**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public void setDefaults() {
		super.setDefaults();
		put("Title", "Message Report"); 

    }
}
