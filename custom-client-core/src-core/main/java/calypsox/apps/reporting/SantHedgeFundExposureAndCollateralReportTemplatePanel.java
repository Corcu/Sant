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

public class SantHedgeFundExposureAndCollateralReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 7168581995331883232L;

	public SantHedgeFundExposureAndCollateralReportTemplatePanel() {
		setPanelVisibility();
	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.processStartEndDatePanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.valuationPanel.setVisible(true);
		this.economicSectorPanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		// this.cptyPanel.setVisible(true);
		this.isFundPanel.setVisible(true);

	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Hedge Fund Exposure And Collateral");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected SantLegalEntityPanel getCounterPartyPanel() {
		return new SantLegalEntityPanel(LegalEntity.FUND, "Fund", false, true, true, true);
	}

	@Override
	protected boolean getFundsOnly() {
		return false;
	}

	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "SantHedgeFundActivity");
		final JFrame frame = new JFrame();
		frame.setContentPane(new SantHedgeFundExposureAndCollateralReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}
}
