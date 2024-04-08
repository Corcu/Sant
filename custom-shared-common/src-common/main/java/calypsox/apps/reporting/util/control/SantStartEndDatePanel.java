package calypsox.apps.reporting.util.control;

import com.calypso.apps.reporting.ReportTemplateDatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;

import javax.swing.*;
import java.awt.*;

public class SantStartEndDatePanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private ReportTemplateDatePanel startDatePanel = null;
	private ReportTemplateDatePanel endDatePanel = null;

	private JLabel label = null;

	public SantStartEndDatePanel(final String name) {
		setLayout(new FlowLayout());

		this.label = new JLabel(name);
		this.startDatePanel = ReportTemplateDatePanel.getStart();
		this.endDatePanel = ReportTemplateDatePanel.getEnd();

		this.endDatePanel.setDependency(this.startDatePanel);

		this.label.setPreferredSize(new Dimension(50, 24));
		this.startDatePanel.setPreferredSize(new Dimension(215, 24));
		this.endDatePanel.setPreferredSize(new Dimension(215, 24));

		add(this.label);
		add(this.startDatePanel);
		add(this.endDatePanel);

		initDomains();
	}

	public void setDateLabelName(String name) {
		this.label.setText(name);
	}

	public void setPreferredSize(Dimension labelSize, Dimension startPanelSize, Dimension endDatePanelSize) {

		this.label.setPreferredSize(labelSize);
		this.startDatePanel.setPreferredSize(startPanelSize);
		this.endDatePanel.setPreferredSize(endDatePanelSize);
	}

	private void initDomains() {
		this.startDatePanel.init(TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
				TradeReportTemplate.START_TENOR);
		this.endDatePanel.init(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);

		AppUtil.addStartEndDatesActionListener(this.startDatePanel, this.endDatePanel);

	}

	public void customInitDomains(final String startDate, final String startPlus, final String startTenor,
			final String endDate, final String endPlus, final String endTenor) {
		this.startDatePanel.init(startDate, startPlus, startTenor);
		this.endDatePanel.init(endDate, endPlus, endTenor);
	}

	public void read(final ReportTemplate reportTemplate) {
		this.startDatePanel.read(reportTemplate);
		this.endDatePanel.read(reportTemplate);
	}

	public void setTemplate(final ReportTemplate template) {
		this.startDatePanel.setTemplate(template);
		this.endDatePanel.setTemplate(template);
	}

	public void write(final ReportTemplate template) {
		this.startDatePanel.write(template);
		this.endDatePanel.write(template);

	}

	public void setValDatetime(JDatetime valDatetime) {
		this.startDatePanel.setValDatetime(valDatetime);
		this.endDatePanel.setValDatetime(valDatetime);

	}
}
