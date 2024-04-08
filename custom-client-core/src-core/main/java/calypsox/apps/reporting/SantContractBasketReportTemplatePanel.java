/**
 * Report template to be imported in Delivery Notice template
 * 
 * @author aela
 * 
 */
package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.report.ReportTemplate;

public class SantContractBasketReportTemplatePanel extends ReportTemplatePanel {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 6271658179046890608L;

	@Override
	public void setTemplate(ReportTemplate template) {
		this.template = template;
	}

	@Override
	public ReportTemplate getTemplate() {
		return template;
	}

	private ReportTemplate template;


}
