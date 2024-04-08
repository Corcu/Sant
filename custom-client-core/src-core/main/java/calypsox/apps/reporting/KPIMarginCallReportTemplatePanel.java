package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.calypso.apps.reporting.ReportTemplateDatePanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.CurrencyUtil;
import com.jidesoft.swing.JideSwingUtilities;

public class KPIMarginCallReportTemplatePanel extends ReportTemplatePanel {

	private static final String KPI_TO = "KPIto";
	private static final String KPI_FROM = "KPIfrom";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ReportTemplate template;
	public static final String TEMPLATEQUERY = "KPIquery";
	public static final String TEMPLATEQUERYCURRENCY = "KPICurrency";
	private static final String TEMPLATEQUERYTYPE = "KPItype";
	private JComboBox<String> comboxQuery;
	private JPanel detailFilterPanel = null;
	private JLabel currencyLabel = null;
	private JLabel typeLabel = null;
	private JLabel subTypeLabel = null;
	private JTextField currencyTextField = null;
	private JButton currencyButton = null;
	private JButton contractTypeButton = null;
	private JTextField displayCollateralsTextField;
	private transient KPIMarginCallReportTemplatePanel.DefaultActionListener defaultActionListener = null;
	private ReportTemplateDatePanel processDateStart = null;
	private ReportTemplateDatePanel processDateEnd = null;

	@SuppressWarnings({ "unchecked"})
	public KPIMarginCallReportTemplatePanel() {
		this.setLayout((LayoutManager) null);
		this.add(this.getDigitalPlatformFilterPanel());
		this.setPreferredSize(new Dimension(960, 280));
		this.setSize(new Dimension(960, 280));
		setSize(new Dimension(1140, 50));
		getProcessDateStart().init(KPI_FROM, "KPIfromPlus", "KPIfromTenor");
		getProcessDateEnd().init(KPI_TO, "KPItoPlus", "KPItoTenor");
		AppUtil.addStartEndDatesActionListener(getProcessDateStart(), getProcessDateEnd());
	}

	@Override
	public ReportTemplate getTemplate() {
		this.template.put(TEMPLATEQUERY, this.comboxQuery.getSelectedItem());
		this.template.put(TEMPLATEQUERYCURRENCY, this.getCurrencyTextField().getText());
		this.template.put(TEMPLATEQUERYTYPE, this.getSubTypeTextField().getText());
		getProcessDateStart().read(template);
		getProcessDateEnd().read(template);
		return this.template;
	}

	@Override
	public void setTemplate(final ReportTemplate arg0) {
		this.template = arg0;
		if (!Util.isEmpty((String) template.get(TEMPLATEQUERY))) {
			this.comboxQuery.setSelectedItem(template.get(TEMPLATEQUERY));
		}
		this.getCurrencyTextField().setText(template.get(TEMPLATEQUERYCURRENCY));
		this.getSubTypeTextField().setText(template.get(TEMPLATEQUERYTYPE));
		getProcessDateStart().setTemplate(template);
		getProcessDateEnd().setTemplate(template);
		getProcessDateStart().write(template);
		getProcessDateEnd().write(template);

	}

	private JPanel getDigitalPlatformFilterPanel() {
		if (this.detailFilterPanel == null) {
			this.detailFilterPanel = new JPanel((LayoutManager) null);
			this.detailFilterPanel.setBorder(new TitledBorder(new EtchedBorder(1, (Color) null, (Color) null),
					"Digital Platform", 4, 2, (Font) null, (Color) null));

			this.detailFilterPanel.setBounds(20, 6, 528, 204);

			this.detailFilterPanel.add(this.getCurrencyLabel());
			this.detailFilterPanel.add(this.getCurrencyTextField());
			this.detailFilterPanel.add(this.getCurrencyButton());
			this.detailFilterPanel.add(this.getTypeLabel());
			this.detailFilterPanel.add(this.getDisplayTypeComboBox());
			this.detailFilterPanel.add(this.getSubTypeLabel());
			this.detailFilterPanel.add(this.getSubTypeTextField());
			this.detailFilterPanel.add(this.getSubTypeButton());
			this.detailFilterPanel.add(getProcessDateStart());
			this.detailFilterPanel.add(getProcessDateEnd());
		}

		return this.detailFilterPanel;
	}

	private JLabel getTypeLabel() {
		if (this.typeLabel == null) {
			this.typeLabel = new JLabel("KPI Type:");
			this.typeLabel.setBounds(20, 20, 123, 24);
		}
		return this.typeLabel;
	}

	private JLabel getCurrencyLabel() {
		if (this.currencyLabel == null) {
			this.currencyLabel = new JLabel("Currency:");
			this.currencyLabel.setBounds(20, 67, 123, 24);
		}
		return this.currencyLabel;
	}

	private JLabel getSubTypeLabel() {
		if (this.subTypeLabel == null) {
			this.subTypeLabel = new JLabel("KPI SubType :");
			this.subTypeLabel.setBounds(20, 113, 123, 24);
		}
		return this.subTypeLabel;
	}

	private JTextField getCurrencyTextField() {
		if (this.currencyTextField == null) {
			this.currencyTextField = new JTextField();
			this.currencyTextField.setEditable(false);
			this.currencyTextField.setBounds(155, 67, 118, 24);
		}
		return this.currencyTextField;
	}

	private JButton getCurrencyButton() {
		if (this.currencyButton == null) {
			this.currencyButton = new JButton();
			this.currencyButton.setText("...");
			this.currencyButton.setBounds(273, 67, 32, 24);
			this.currencyButton.setActionCommand("ACTION_SELECT_CURRENCY");
			this.currencyButton.addActionListener(this.getDefaultActionListener());
		}
		return this.currencyButton;
	}

	private KPIMarginCallReportTemplatePanel.DefaultActionListener getDefaultActionListener() {
		if (this.defaultActionListener == null) {
			this.defaultActionListener = new KPIMarginCallReportTemplatePanel.DefaultActionListener();
		}
		return this.defaultActionListener;
	}

	protected JTextField getSubTypeTextField() {
		if (this.displayCollateralsTextField == null) {
			this.displayCollateralsTextField = new JTextField();
			this.displayCollateralsTextField.setEditable(false);
			this.displayCollateralsTextField.setBounds(155, 113, 118, 24);

		}
		return this.displayCollateralsTextField;
	}

	protected JComboBox<String> getDisplayTypeComboBox() {
		if (this.comboxQuery == null) {
			this.comboxQuery = new JComboBox<String>();
			this.comboxQuery.setBounds(155, 20, 200, 25);
			this.comboxQuery.addItem("MC_STATUS");
			this.comboxQuery.addItem("MC_CONTRACT_TYPE");
			this.comboxQuery.addItem("MC_METRICS");
			this.comboxQuery.addItem("MC_SETTLE_CASH");
			this.comboxQuery.addItem("MC_SETTLE_NON_CASH");
			this.comboxQuery.addItem("PDV_SETTLE_NON_CASH");
			this.comboxQuery.addItem("SETTLE_BALANCE");
			this.comboxQuery.setSelectedIndex(0);
		}
		return this.comboxQuery;
	}

	private class DefaultActionListener implements ActionListener {
		private DefaultActionListener() {
		}

		public void actionPerformed(ActionEvent e) {
			String action = e.getActionCommand();
			if ("ACTION_SELECT_CURRENCY".equals(action)) {
				KPIMarginCallReportTemplatePanel.this.selectCurrency();
			} else if ("ACTION_SELECT_CONTRACT_TYPE".equals(action)) {
				KPIMarginCallReportTemplatePanel.this.selectSubType();
			}
		}
	}

	private JButton getSubTypeButton() {
		if (this.contractTypeButton == null) {
			this.contractTypeButton = new JButton();
			this.contractTypeButton.setText("...");
			this.contractTypeButton.setBounds(273, 113, 32, 24);
			this.contractTypeButton.setActionCommand("ACTION_SELECT_CONTRACT_TYPE");
			this.contractTypeButton.addActionListener(getDefaultActionListener());
		}
		return this.contractTypeButton;
	}

	@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
	private void selectCurrency() {
		List<String> all = CurrencyUtil.getCurrencies();
		Vector<String> sels = Util.string2Vector(this.getCurrencyTextField().getText());

		sels = AppUtil.chooseList(JideSwingUtilities.getFrame(this), new Vector(all), sels, "Select Currency");

		if (sels != null) {
			this.getCurrencyTextField().setText(Util.collectionToString(sels));
		}
	}

	private void selectSubType() {
		Vector<String> all = new Vector<>();
		String sel = (String) comboxQuery.getSelectedItem();
		if ("MC_STATUS,MC_CONTRACT_TYPE,MC_METRICS".contains(sel)) {
			all.add("ALL");
			all.add("ACADIA");
			all.add("NON ACADIA");
		} else {
			all.addAll(CollateralConfig.getContractTypes());
		}
		Vector<String> sels = Util.string2Vector(getSubTypeTextField().getText());
		sels = AppUtil.chooseList(JideSwingUtilities.getFrame(this), all, sels, "Select Contract Types");
		if (sels == null)
			return;
		getSubTypeTextField().setText(Util.collectionToString(sels));
	}

	private ReportTemplateDatePanel getProcessDateStart() {
		if (this.processDateStart == null) {
			this.processDateStart = ReportTemplateDatePanel.getStart();
			this.processDateStart.setBounds(20, 159, 215, 24);
		}
		return this.processDateStart;
	}

	private ReportTemplateDatePanel getProcessDateEnd() {
		if (this.processDateEnd == null) {
			this.processDateEnd = ReportTemplateDatePanel.getEnd();
			this.processDateEnd.setBounds(255, 159, 215, 24);
			this.processDateEnd.setDependency(getProcessDateStart());
		}
		return this.processDateEnd;
	}
}