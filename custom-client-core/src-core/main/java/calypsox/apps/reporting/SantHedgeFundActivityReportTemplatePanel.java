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
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;

public class SantHedgeFundActivityReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 7168581995331883232L;

	public SantHedgeFundActivityReportTemplatePanel() {
		setPanelVisibility();
	}

	private void setPanelVisibility() {
		hideAllPanels();

		this.processStartEndDatePanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.agreementStatusPanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
		this.economicSectorPanel.setVisible(true);
		this.isFundPanel.setVisible(true);
		this.matureDealsPanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.poDealPanel.setVisible(true);
		this.valuationPanel.setVisible(true);
	}

	@Override
	protected SantLegalEntityPanel getCounterPartyPanel() {
		return new SantLegalEntityPanel(LegalEntity.FUND, "Fund", false, true, true, true);
	}

	@Override
	protected boolean getFundsOnly() {
		return false;
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Hedge Fund Activity");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	public static void main(final String... args) throws Exception {

		final DSConnection ds = ConnectionUtil.connect(args, "MainEntry");
		DSConnection.setDefault(ds);
		final JFrame frame = new JFrame();
		frame.setContentPane(new SantHedgeFundActivityReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}
}
