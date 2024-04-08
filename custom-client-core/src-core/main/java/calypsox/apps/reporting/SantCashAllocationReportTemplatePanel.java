package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.tk.report.SantCashAllocationReportStyle;

import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

public class SantCashAllocationReportTemplatePanel extends SantGenericReportTemplatePanel {

	public final static String CCY_CASH = "CCY_CASH";

	private static final long serialVersionUID = 1L;
	private SantComboBoxPanel<Integer, String> cashCcyPanel;
	private SantComboBoxPanel<Integer, String> collSecIndicator;

	public SantCashAllocationReportTemplatePanel() {
		setPanelVisibility();
	}

	// @Override
	// protected void init() {
	// buildControlsPanel();
	//
	// setSize(0, 185);
	// final JPanel masterPanel = new JPanel();
	// masterPanel.setLayout(new BorderLayout());
	// masterPanel.setBorder(getMasterPanelBorder());
	// add(masterPanel);
	//
	// final JPanel mainPanel = new JPanel();
	// mainPanel.setLayout(new GridLayout(1, 4));
	//
	// masterPanel.add(mainPanel, BorderLayout.CENTER);
	//
	// final JPanel column1Panel = getColumn1Panel();
	// final JPanel column2Panel = getColumn2Panel();
	// final JPanel column3Panel = getColumn3Panel();
	//
	// mainPanel.add(column1Panel);
	// mainPanel.add(column2Panel);
	// mainPanel.add(column3Panel);
	//
	// }

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();

		this.cashCcyPanel = new SantComboBoxPanel<Integer, String>("Cash Ccy", LocalCache.getCurrencies());

		Vector<String> collSecIndValues = new Vector<String>();
		collSecIndValues.add("Delivery vs Payment");
		collSecIndValues.add("Receipt vs Payment");
		collSecIndValues.add("Non-DVP or Free of Payment");
		collSecIndValues.add("Non-RVP or Free of Payment");
		this.collSecIndicator = new SantComboBoxPanel<Integer, String>("Col/SecLen", collSecIndValues);

	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = super.getColumn1Panel();
		column1Panel.removeAll();

		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.poDealPanel);
		column1Panel.add(this.cptyPanel);

		return column1Panel;
	}

	@Override
	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = super.getColumn2Panel();
		column2Panel.removeAll();

		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);

		column2Panel.add(this.collSecIndicator);
		column2Panel.add(this.tradeStatusPanel);
		return column2Panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = super.getColumn3Panel();
		column3Panel.removeAll();

		column3Panel.add(this.baseCcyPanel);
		column3Panel.add(this.cashCcyPanel);
		return column3Panel;
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Sant Cash Allocation");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.processStartEndDatePanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.poDealPanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
		this.cptyPanel.setVisible(true);
		this.baseCcyPanel.setVisible(true);
		this.tradeStatusPanel.setVisible(true);

	}

	@Override
	public ReportTemplate getTemplate() {
		super.getTemplate();

		this.reportTemplate.put(SantCashAllocationReportTemplatePanel.CCY_CASH, this.cashCcyPanel.getValue());
		this.reportTemplate.put(SantCashAllocationReportStyle.COLL_SEC, this.collSecIndicator.getValue());

		return this.reportTemplate;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		super.setTemplate(template);

		this.cashCcyPanel.setValue(this.reportTemplate, SantCashAllocationReportTemplatePanel.CCY_CASH);
		this.collSecIndicator.setValue(this.reportTemplate, SantCashAllocationReportStyle.COLL_SEC);
	}

	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "SantCashAllocationTemplatePanel");
		final JFrame frame = new JFrame();
		frame.setTitle("SantCashAllocationTemplatePanel");
		frame.setContentPane(new SantCashAllocationReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1273, 307));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}