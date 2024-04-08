package calypsox.apps.reporting;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.report.ReportTemplate;

/**
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 *
 */
public class SantCollateralEligibleAssetsReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = -7631677738357788828L;

	protected ReportTemplate _reportTemplate;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.apps.reporting.ReportTemplatePanel#getTemplate()
	 */
	@Override
	public ReportTemplate getTemplate() {

		return this._reportTemplate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.calypso.apps.reporting.ReportTemplatePanel#setTemplate(com.calypso.tk
	 * .report.ReportTemplate)
	 */
	@Override
	public void setTemplate(ReportTemplate paramReportTemplate) {
		this._reportTemplate = paramReportTemplate;

	}

}
