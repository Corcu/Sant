package calypsox.apps.reporting;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.control.SantProcessDatePanel;

public class SantKPIMtmByPortfoliosReportTemplatePanel extends SantKPIMtmGenericReportTemplatePanel {

	private static final long serialVersionUID = 7168581995331883232L;

	public SantKPIMtmByPortfoliosReportTemplatePanel() {
		setPanelVisibility();

	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("KPI Mtm By Portfolio");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.poAgrPanel.setVisible(true);
		this.portfolioPanel.setMaxSelectableItems(999);
		this.portfolioPanel.setVisible(true);
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3 = super.getColumn3Panel();
		column3.removeAll();

		column3.add(this.portfolioPanel);
		this.processDatePanel = new SantProcessDatePanel("Process");
		column3.add(this.processDatePanel);

		return column3;
	}

}
