/**
 * Report template to be imported in Delivery Notice template
 * 
 * @author aela
 * 
 */
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

public class SantMCConfigReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 1L;

	// SantProcessDatePanel processDatePanel;

	public SantMCConfigReportTemplatePanel() {
		setPanelVisibility();
	}

	// protected void buildControlsPanel() {
	// super.buildControlsPanel();
	// processDatePanel = new SantProcessDatePanel("Process...");
	// }
	//
	// @Override
	// protected Component getNorthPanel() {
	// return processDatePanel;
	// }

	private void setPanelVisibility() {
		hideAllPanels();
		// add(new SantProcessDatePanel("Process Date"));
		this.processStartEndDatePanel.setVisible(true);
		// this.agreementNamePanel.setVisible(true);
		//this.valuationPanel.setVisible(true);
		// this.economicSectorPanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
		// this.agreementStatusPanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.cptyPanel.setVisible(true);
		this.lastAllocationCurrencyCheckBox.setVisible(true);
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Valid Contract not calculated");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected SantLegalEntityPanel getCounterPartyPanel() {
		return new SantLegalEntityPanel(LegalEntity.COUNTERPARTY, "CounterParty", false, true, true, true);
	}

	// public ReportTemplate getTemplate() {
	// ReportTemplate tempalte = super.getTemplate();
	// this.processDatePanel.read(this.reportTemplate);
	// return tempalte;
	// }

	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "SantMCConfigReportTemplatePanel");
		final JFrame frame = new JFrame();
		frame.setContentPane(new SantMCConfigReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}
}
