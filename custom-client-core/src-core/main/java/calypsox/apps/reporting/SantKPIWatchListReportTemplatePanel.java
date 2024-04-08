package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

public class SantKPIWatchListReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = 3315090510665968822L;

	protected SantProcessDatePanel processDatePanel;

	@Override
	public void setValDatetime(JDatetime valDatetime) {
		this.processDatePanel.setValDatetime(valDatetime);
	}

	@Override
	protected Dimension getPanelSize() {
		return new Dimension(0, 135);
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
		this.processDatePanel = new SantProcessDatePanel("Process");
		this.processDatePanel.customInitDomains(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);
		this.processDatePanel.removeDateLabel();
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("KPI Watchlist");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	@Override
	protected JPanel getNorthPanel() {
		return this.processDatePanel;
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(3, 1));
		column1Panel.add(this.poAgrPanel);
		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(3, 1));
		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);
		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(3, 1));
		column3Panel.add(this.baseCcyPanel);
		return column3Panel;
	}

	@Override
	public ReportTemplate getTemplate() {
		super.getTemplate();
		this.processDatePanel.read(this.reportTemplate);
		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);
		this.processDatePanel.setTemplate(template);
		this.processDatePanel.write(template);
	}

	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "SantKPIWatchListReportTemplatePanel");
		final JFrame frame = new JFrame();
		frame.setContentPane(new SantKPIWatchListReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}

}
