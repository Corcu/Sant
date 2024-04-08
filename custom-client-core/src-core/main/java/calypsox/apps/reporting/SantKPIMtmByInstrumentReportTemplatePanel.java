package calypsox.apps.reporting;

import java.awt.Dimension;

import javax.swing.JFrame;

import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

public class SantKPIMtmByInstrumentReportTemplatePanel extends SantKPIMtmGenericReportTemplatePanel {

	private static final long serialVersionUID = 7168581995331883232L;

	public SantKPIMtmByInstrumentReportTemplatePanel() {
		setPanelVisibility();

	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.poAgrPanel.setVisible(true);
		this.instrumentTypePanel.setVisible(true);
	}

	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "KPIMtmByAgreementOwnerReportTemplatePanel");
		final JFrame frame = new JFrame();
		frame.setContentPane(new SantKPIMtmByInstrumentReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}
}
