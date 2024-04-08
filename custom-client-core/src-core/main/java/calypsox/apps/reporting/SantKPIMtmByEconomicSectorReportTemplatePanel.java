package calypsox.apps.reporting;

import javax.swing.JPanel;

import calypsox.apps.reporting.util.control.SantProcessDatePanel;

public class SantKPIMtmByEconomicSectorReportTemplatePanel extends SantKPIMtmGenericReportTemplatePanel {

	private static final long serialVersionUID = 7168581995331883232L;

	public SantKPIMtmByEconomicSectorReportTemplatePanel() {
		setPanelVisibility();

	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.poAgrPanel.setVisible(true);
		this.economicSectorPanel.setVisible(true);
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3 = super.getColumn3Panel();
		column3.removeAll();

		column3.add(this.economicSectorPanel);
		this.processDatePanel = new SantProcessDatePanel("Process");
		column3.add(this.processDatePanel);

		return column3;
	}

}
