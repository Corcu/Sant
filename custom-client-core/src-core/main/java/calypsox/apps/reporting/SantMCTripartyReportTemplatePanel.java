/**
 * Report template to be imported in Delivery Notice template
 * 
 * @author aela
 * 
 */
package calypsox.apps.reporting;

import java.awt.Dimension;

import javax.swing.JCheckBox;

import calypsox.apps.reporting.util.control.SantLegalEntityPanel;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

public class SantMCTripartyReportTemplatePanel extends ReportTemplatePanel {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -2172048332462371098L;

	private ReportTemplate template;

	private final JCheckBox accumulated;

	public static final String ACCUMULATED_VALUE = "ACCUMULATED_VALUE";

	// GSM 21/07/15. SBNA Multi-PO filter
	public static final String OWNER_AGR = "Owner agreement";
	protected SantLegalEntityPanel poAgrPanel;

	public SantMCTripartyReportTemplatePanel() {
		setSize(new Dimension(1140, 50));

		// GSM 21/07/15. SBNA Multi-PO filter
		this.poAgrPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, "Owner (Agr)", false, true, true, true);
		this.poAgrPanel.setBounds(5, 5, 300, 24);
		add(this.poAgrPanel);

		this.accumulated = new JCheckBox("Accumulate positions");
		this.accumulated.setBounds(350, 2, 150, 40);

		add(this.accumulated);

	}

	/**
	 * 
	 * Get the ReportTemplate. Null in this case.
	 * 
	 * 
	 */

	@Override
	public final ReportTemplate getTemplate() {

		final boolean isAccumulatedSelected = this.accumulated.isSelected();

		this.template.put(ACCUMULATED_VALUE, isAccumulatedSelected);
		// GSM 21/07/15. SBNA Multi-PO filter
		if (!Util.isEmpty(this.poAgrPanel.getLE())) {
			this.template.put(OWNER_AGR, this.poAgrPanel.getLEIdsStr());

		} else {
			this.template.remove(OWNER_AGR);

		}

		return this.template;

	}

	/**
	 * 
	 * Set the ReportTemplate. Ignored.
	 * 
	 * 
	 */

	@Override
	public void setTemplate(final ReportTemplate arg0) {

		this.template = arg0;

		final Boolean isAccumulatedSelected = (Boolean) this.template.get(ACCUMULATED_VALUE);

		if (isAccumulatedSelected != null) {

			this.accumulated.setSelected(isAccumulatedSelected);
		}

		// GSM 21/07/15. SBNA Multi-PO filter
		this.poAgrPanel.setValue(this.template, OWNER_AGR);
	}

	// Test
	// public static void main(final String... argsss) throws ConnectException {
	// final String args[] = { "-env", "dev4-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
	// ConnectionUtil.connect(args, "SantMCTripartyReportTemplatePanel");
	// final JFrame frame = new JFrame();
	// frame.setTitle("SantMCTripartyReportTemplatePanel");
	// frame.setContentPane(new SantMCTripartyReportTemplatePanel());
	// frame.setVisible(true);
	// frame.setSize(new Dimension(1273, 307));
	// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// }

}
