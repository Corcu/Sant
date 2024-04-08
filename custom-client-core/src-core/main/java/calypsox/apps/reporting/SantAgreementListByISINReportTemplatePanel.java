package calypsox.apps.reporting;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTextField;

import calypsox.tk.report.SantAgreementListByISINReportTemplate;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;

public class SantAgreementListByISINReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = 123L;

	protected ReportTemplate reportTemplate;

	protected JLabel isinLabel = new JLabel();
	protected JTextField isinText = new JTextField();

	public SantAgreementListByISINReportTemplatePanel() {
		setLayout(null);
		setSize(new Dimension(1173, 307));

		this.isinLabel.setText("Isin:");
		this.isinLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.isinLabel.setBounds(110, 20, 70, 24);
		add(this.isinLabel);
		this.isinText.setBounds(200, 20, 100, 24);
		add(this.isinText);

	}

	@Override
	public ReportTemplate getTemplate() {
		if (!Util.isEmpty(this.isinText.getText())) {
			this.reportTemplate.put(SantAgreementListByISINReportTemplate.ISIN, this.isinText.getText());
		}
		return this.reportTemplate;
	}

	@Override
	public void setTemplate(ReportTemplate reporttemplate) {
		this.reportTemplate = reporttemplate;
		String isin = (String) this.reportTemplate.get(SantAgreementListByISINReportTemplate.ISIN);
		if (!Util.isEmpty(isin)) {
			this.isinText.setText(isin);
		}
	}

}
