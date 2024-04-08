/**
 * 
 */
package calypsox.apps.reporting;

import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

/**
 * @author aalonsop
 *
 */
public class KGR_MarginCallLegalEntitiesReportTemplatePanel extends SantMCConfigReportTemplatePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -674375352140784707L;

	@Override
	public ReportTemplate getTemplate() {

		this.reportTemplate.put("Contract Type", "ALL");
		if (!(Util.isEmpty(this.cptyPanel.getLE())))
			this.processStartEndDatePanel.read(this.reportTemplate);
		if (!(Util.isEmpty(this.cptyPanel.getLE())))
			this.reportTemplate.put("Legal Entity", this.cptyPanel.getLE());
		else {
			this.reportTemplate.remove("Legal Entity");
		}
		this.reportTemplate.put("Role", "CounterParty");
		if (!(Util.isEmpty(this.poAgrPanel.getLE()))) {
			this.reportTemplate.put("OWNER_AGR_IDS", this.poAgrPanel.getLEIdsStr());
			this.reportTemplate.put("Processing Org", this.poAgrPanel.getLE());
		} else {
			this.reportTemplate.remove("OWNER_AGR_IDS");
			this.reportTemplate.remove("Processing Org");
		}
		this.reportTemplate.put("Discount Currency", "ALL");
		this.reportTemplate.put("Status", "ALL");
		this.processStartEndDatePanel.read(reportTemplate);
		return this.reportTemplate;
	}

	public void setTemplate(ReportTemplate template) {
		this.reportTemplate = template;
		this.processStartEndDatePanel.setTemplate(template);
		this.processStartEndDatePanel.write(template);
		this.agreementTypePanel.setValue(this.reportTemplate, "Contract Type");
		this.cptyPanel.setValue(this.reportTemplate, "Legal Entity");
		this.processStartEndDatePanel.write(template);
	}

}
