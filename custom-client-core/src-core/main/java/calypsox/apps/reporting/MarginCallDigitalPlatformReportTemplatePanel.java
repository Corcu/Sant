package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.calypso.apps.reporting.MarginCallEntryFilterPanel;
import com.calypso.apps.reporting.MarginCallEntryReportTemplatePanel;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.CurrencyUtil;
import com.jidesoft.swing.JideSwingUtilities;

/**
 * 
 * @author x957355
 *
 */
public class MarginCallDigitalPlatformReportTemplatePanel extends MarginCallEntryReportTemplatePanel {

	private static final long serialVersionUID = 4620660903615688096L;

	private JComboBox<String> displayCollateralsComboBox;
	private JLabel currencyLabel = null;
	private JTextField currencyTextField = null;
	private JButton currencyButton = null;
	private MarginCallEntryFilterPanel entryFilterPanel = null;
	private JPanel detailFilterPanel = null;
	private transient MarginCallDigitalPlatformReportTemplatePanel.DefaultActionListener defaultActionListener = null;

	public MarginCallDigitalPlatformReportTemplatePanel() {
		super();
		this.add(this.getDigitalPlatformFilterPanel());
		this.setPreferredSize(new Dimension(960, 280));
		this.setSize(new Dimension(960, 280));
	}



	public void setTemplate(ReportTemplate template) {
		this._template = template;
		super.setTemplate(template);

		String b = (String) this._template.get("ACADIA");
		if (b != null) {
			this.getDisplayAcadiaComboBox().setSelectedItem(b);
		} else {
			this.getDisplayAcadiaComboBox().setSelectedItem("ALL");
		}

		String s = (String) template.get("CURRENCY");

		if (Util.isEmpty(s)) {
			s = "";
		}
		this.getCurrencyTextField().setText(s);

	}

	public ReportTemplate getTemplate() {
		super.getTemplate();
		this._template.put("ACADIA", this.getDisplayAcadiaComboBox().getSelectedItem());
		this._template.put("CURRENCY", this.getCurrencyTextField().getText());

		return this._template;
	}

	protected JComboBox<String> getDisplayAcadiaComboBox() {
		if (this.displayCollateralsComboBox == null) {
			this.displayCollateralsComboBox = new JComboBox<String>();
			this.displayCollateralsComboBox.setBounds(20, 113, 200, 25);
			this.displayCollateralsComboBox.addItem("ALL");
			this.displayCollateralsComboBox.addItem("ACADIA");
			this.displayCollateralsComboBox.addItem("NON ACADIA");
		}
		return this.displayCollateralsComboBox;
	}

	private JPanel getDigitalPlatformFilterPanel() {
		if (this.detailFilterPanel == null) {
			this.detailFilterPanel = new JPanel((LayoutManager) null);
			this.detailFilterPanel.setBorder(new TitledBorder(new EtchedBorder(1, (Color) null, (Color) null),
					"Digital Platform", 4, 2, (Font) null, (Color) null));

			this.detailFilterPanel.setBounds(1024, 6, 328, 204);

			this.detailFilterPanel.add(this.getCurrencyLabel());
			this.detailFilterPanel.add(this.getCurrencyTextField());
			this.detailFilterPanel.add(this.getCurrencyButton());

			this.detailFilterPanel.add(this.getDisplayAcadiaComboBox());
		}

		return this.detailFilterPanel;
	}

	private JLabel getCurrencyLabel() {
		if (this.currencyLabel == null) {
			this.currencyLabel = new JLabel("Currency :");
			this.currencyLabel.setBounds(20, 77, 123, 24);
		}
		return this.currencyLabel;
	}

	private JTextField getCurrencyTextField() {
		if (this.currencyTextField == null) {
			this.currencyTextField = new JTextField();
			this.currencyTextField.setEditable(false);
			this.currencyTextField.setBounds(155, 77, 118, 24);
		}
		return this.currencyTextField;
	}

	private JButton getCurrencyButton() {
		if (this.currencyButton == null) {
			this.currencyButton = new JButton();
			this.currencyButton.setText("...");
			this.currencyButton.setBounds(273, 78, 32, 24);
			this.currencyButton.setActionCommand("ACTION_SELECT_CURRENCY");
			this.currencyButton.addActionListener(this.getDefaultActionListener());
		}
		return this.currencyButton;
	}

	private MarginCallDigitalPlatformReportTemplatePanel.DefaultActionListener getDefaultActionListener() {
		if (this.defaultActionListener == null) {
			this.defaultActionListener = new MarginCallDigitalPlatformReportTemplatePanel.DefaultActionListener();
		}
		return this.defaultActionListener;
	}

	private class DefaultActionListener implements ActionListener {
		private DefaultActionListener() {
		}

		public void actionPerformed(ActionEvent e) {
			String action = e.getActionCommand();
			if ("ACTION_SELECT_CURRENCY".equals(action)) {
				MarginCallDigitalPlatformReportTemplatePanel.this.selectCurrency();
			}
		}
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


	@SuppressWarnings("rawtypes")
	public boolean isValidLoad(ReportPanel panel) {
		Map potentialSizesByTypeOfObject = panel.getReport().getPotentialSize();
		return this.displayLargeListWarningMessage(this, potentialSizesByTypeOfObject);
	}
}
