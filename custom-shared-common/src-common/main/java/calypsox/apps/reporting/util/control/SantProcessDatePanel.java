package calypsox.apps.reporting.util.control;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.calypso.apps.reporting.ReportTemplateDatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;

public class SantProcessDatePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private ReportTemplateDatePanel startDatePanel = null;

	private JLabel label = null;

	public SantProcessDatePanel(final String name) {
		setLayout(new FlowLayout());

		this.label = new JLabel(name);
		this.startDatePanel = ReportTemplateDatePanel.getStart();

		this.label.setPreferredSize(new Dimension(70, 24));
		this.startDatePanel.setPreferredSize(new Dimension(215, 24));

		add(this.label);
		add(this.startDatePanel);

		initDomains();
	}

	public void setDateLabelName(String name) {
		this.label.setText(name);
	}

	public void setPreferredSize(Dimension labelSize, Dimension startPanelSize) {
		this.label.setPreferredSize(labelSize);
		this.startDatePanel.setPreferredSize(startPanelSize);
	}

	private void initDomains() {
		this.startDatePanel.init(TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
				TradeReportTemplate.START_TENOR);
		AppUtil.addDateListener(this.startDatePanel.getDateText());

	}

	public void customInitDomains(final String date, final String plus, final String tenor) {
		this.startDatePanel.init(date, plus, tenor);
	}

	public void read(final ReportTemplate reportTemplate) {
		this.startDatePanel.read(reportTemplate);
	}

	public void setTemplate(final ReportTemplate template) {
		this.startDatePanel.setTemplate(template);
	}

	public void write(final ReportTemplate template) {
		this.startDatePanel.write(template);
	}

	public void setValDatetime(JDatetime valDatetime) {
		this.startDatePanel.setValDatetime(valDatetime);

	}

	public void removeDateLabel() {
		((JLabel) this.startDatePanel.getComponent(0)).setText("");
	}
	
	public void setPanelLabelName(String name) {
		((JLabel) this.startDatePanel.getComponent(0)).setText(name);
	}
}
