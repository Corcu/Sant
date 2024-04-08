/**
 * Report template to be imported in Delivery Notice template
 *
 * 
 */
package calypsox.apps.reporting;

import calypsox.apps.reporting.util.control.SantLegalEntityPanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class SantMCGCPoolingReportTemplatePanel extends ReportTemplatePanel {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 6271658179046890608L;

	private ReportTemplate template;

	// Accumulated
	private JCheckBox accumulated;
	public static final String ACCUMULATED_VALUE = "ACCUMULATED_VALUE";

	// GSM 21/07/15. SBNA Multi-PO filter
	public static final String OWNER_AGR = "Owner agreement";
	protected SantLegalEntityPanel poAgrPanel;

	public SantMCGCPoolingReportTemplatePanel() {

		add(getGCPoolingPanel());
		setPreferredSize(new Dimension(1140, 130));
		setSize(new Dimension(1140, 130));
		setLayout(null);
	}

	private Component getGCPoolingPanel() {

		// Init JPanel
		JPanel panel = new JPanel();
		panel.setBounds(5, 5, 800, 120);
		panel.setBorder(new TitledBorder(new EtchedBorder(1, null, null), "GCPooling baskets of bonds", 4, 2, null,
				null));
		panel.setLayout(null);

		// GSM 21/07/15. SBNA Multi-PO filter
		this.poAgrPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, "Owner (Agr)", false, true, true, true);
		this.poAgrPanel.setBounds(5, 80, 300, 24);
		add(this.poAgrPanel);

		this.accumulated = new JCheckBox("Accumulate positions");
		this.accumulated.setBounds(350, 72, 150, 40);
		add(this.accumulated);

		return panel;
	}

	@Override
	public ReportTemplate getTemplate() {

		if (this.template == null) {
			this.template = new ReportTemplate();
		}

		this.template.put(ACCUMULATED_VALUE, this.accumulated.isSelected());

		// GSM 21/07/15. SBNA Multi-PO filter
		if (!Util.isEmpty(this.poAgrPanel.getLE())) {
			this.template.put(OWNER_AGR, this.poAgrPanel.getLEIdsStr());

		} else {
			this.template.remove(OWNER_AGR);

		}
		return this.template;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		this.template = template;

		final Boolean isAccumulatedSelected = (Boolean) this.template.get(ACCUMULATED_VALUE);
		if (isAccumulatedSelected != null) {
			this.accumulated.setSelected(isAccumulatedSelected);
		}
		// GSM 21/07/15. SBNA Multi-PO filter
		this.poAgrPanel.setValue(this.template, OWNER_AGR);
	}

}
