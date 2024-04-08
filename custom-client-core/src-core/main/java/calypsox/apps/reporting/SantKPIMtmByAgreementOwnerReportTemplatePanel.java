package calypsox.apps.reporting;


public class SantKPIMtmByAgreementOwnerReportTemplatePanel extends SantKPIMtmGenericReportTemplatePanel {

	private static final long serialVersionUID = 7168581995331883232L;

	public SantKPIMtmByAgreementOwnerReportTemplatePanel() {
		setPanelVisibility();

	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.poAgrPanel.setVisible(true);
	}

	// public static void main(final String... arg) throws ConnectException {
	// String args[] = { "-env", "dev5-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
	// ConnectionUtil.connect(args, "KPIMtmByAgreementOwnerReportTemplatePanel");
	// final JFrame frame = new JFrame();
	// frame.setContentPane(new SantKPIMtmByAgreementOwnerReportTemplatePanel());
	// frame.setVisible(true);
	// frame.setSize(new Dimension(1173, 307));
	// }

}
