package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;

import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

public class SantLegChangeControlReportTemplatePanel extends SantGenericReportTemplatePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SantLegChangeControlReportTemplatePanel() {
		setPanelVisibility();
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = super.getColumn1Panel();
		column1Panel.removeAll();

		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.poDealPanel);

		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = super.getColumn2Panel();
		column2Panel.removeAll();

		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);

		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = super.getColumn3Panel();
		column3Panel.removeAll();

		column3Panel.add(this.cptyPanel);
		column3Panel.add(this.baseCcyPanel);

		return column3Panel;
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Sant Leg Change Control");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.poAgrPanel.setVisible(true);
		this.poDealPanel.setVisible(true);
		this.processStartEndDatePanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
		this.cptyPanel.setVisible(true);
		this.baseCcyPanel.setVisible(true);

	}

	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "SantLegChangeControlReportTemplatePanel");
		final JFrame frame = new JFrame();
		frame.setTitle("SantLegChangeControlReportTemplatePanel");
		frame.setContentPane(new SantLegChangeControlReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1273, 307));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}