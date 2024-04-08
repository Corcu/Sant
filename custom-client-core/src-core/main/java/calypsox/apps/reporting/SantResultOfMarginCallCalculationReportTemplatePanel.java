package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantLegalEntityPanel;

import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

public class SantResultOfMarginCallCalculationReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 1L;

	public SantResultOfMarginCallCalculationReportTemplatePanel() {
		setPanelVisibility();
	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.processStartEndDatePanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.valuationPanel.setVisible(true);
		this.economicSectorPanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
		this.agreementStatusPanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.cptyPanel.setVisible(true);
		this.headCloneIndicatorPanel.setVisible(true);
		this.tradeStatusPanel.setVisible(true);
		this.valuationPanel.setVisible(true);

	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Result Of Margin Call Calculation");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected SantLegalEntityPanel getCounterPartyPanel() {
		return new SantLegalEntityPanel(LegalEntity.COUNTERPARTY, "CounterParty", false, true, true, true);
	}

	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "SantResultOfMarginCallCalculation");
		final JFrame frame = new JFrame();
		frame.setContentPane(new SantResultOfMarginCallCalculationReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}
}
