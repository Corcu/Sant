package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.tk.report.SantRiskParametersReportStyle;

import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.ConnectException;

public class SantRiskParametersReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 5010201052862592375L;

	protected SantComboBoxPanel<Integer, String> counterpartyOwnerChoicePanel;

	private static Vector<String> v = new Vector<String>();

	static {
		v.insertElementAt("", 0);
		v.insertElementAt("OWNER", 1);
		v.insertElementAt("COUNTERPARTY", 2);
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
		this.counterpartyOwnerChoicePanel = new SantComboBoxPanel<Integer, String>("Entity", v);
	}

	@Override
	protected Dimension getPanelSize() {
		return new Dimension(0, 115);
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Risk Parameters");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(2, 1));
		column1Panel.add(this.poAgrPanel);
		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(2, 1));
		column2Panel.add(this.agreementNamePanel);
		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(2, 1));
		column3Panel.add(this.counterpartyOwnerChoicePanel);
		return column3Panel;
	}

	@Override
	public ReportTemplate getTemplate() {
		this.reportTemplate = super.getTemplate();

		this.reportTemplate.put(SantRiskParametersReportStyle.COUNTERPARTY_OWNER,
				this.counterpartyOwnerChoicePanel.getValue());

		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);

		this.counterpartyOwnerChoicePanel.setValue("");
		final String s = (String) this.reportTemplate.get(SantRiskParametersReportStyle.COUNTERPARTY_OWNER);
		if (s != null) {
			this.counterpartyOwnerChoicePanel.setValue(s);
		}

	}

	public static void main(final String... args) throws ConnectException {
		final JFrame frame = new JFrame();
		frame.setContentPane(new SantRiskParametersReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}

}
