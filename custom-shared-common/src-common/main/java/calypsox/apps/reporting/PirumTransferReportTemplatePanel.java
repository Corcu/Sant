package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.calypso.apps.internationalization.rb.CalypsoResourceBundle;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.reporting.TemplatePanelLayoutUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.apps.util.LegalEntityTextPanel;
import com.calypso.apps.util.SDFilterGroupUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TransferReportTemplate;
import com.calypso.tk.util.SqlEscaper;
import com.calypso.ui.component.dialog.DualListDialog;
import com.jidesoft.swing.JideBoxLayout;

public class PirumTransferReportTemplatePanel extends ReportTemplatePanel {
	/**
	 *
	 */
	private static final long serialVersionUID = 3338034850955955520L;

	private static final String LOG_CATEGORY = PirumTransferReportTemplatePanel.class.getSimpleName();

	public static final String TRADE_FILTER = "TRADE_FILTER";
	public static final String IS_ONLINE = "IS_ONLINE";

	protected ReportTemplate _template;

	// JLabel
	JLabel transferIdLabel;
	JLabel sdFilterLabel;
	JLabel tradeFilterLabel;
	JLabel booksLabel;
	// JLabel onlineLabel;

	// JTextField
	JTextField transferIdText;
	JTextField sdFilterText;
	JTextField booksText;

	// JButton
	JButton sdFilterButton;
	JButton booksButton;

	// CalypsoComboBox
	CalypsoComboBox tradeFilterChoice;

	// LegalEntityTextPanel
	LegalEntityTextPanel _cptyPanel;

	// JCheckBox
	JCheckBox onlineCheckBox;

	public PirumTransferReportTemplatePanel() {
		try {
			jbInit();
		} catch (final Exception var2) {
			Log.error(LOG_CATEGORY, var2);
		}
	initDomains();
	}

	protected void jbInit() throws Exception {
		this.setSize(new Dimension(500, 200));
		setLayout(new JideBoxLayout(this, 1));
	initOtherPanel();
	initButton();
	initComboBox();
	initCheckBox();
		initTextFields();
		initLabels();
	initListeners();
		final JPanel main = new JPanel();
		main.setSize(200, 200);
		main.setBorder(new EmptyBorder(10, 0, 0, 0));
		initLayout(main);
		setLayout(new BorderLayout());
		this.add(main, BorderLayout.WEST);
	}

	protected void initLayout(JPanel pane) {
		pane.setLayout(new GridBagLayout());

	// SDFilter Pane
	final JPanel sdFilterPane = TemplatePanelLayoutUtil.buildTextButtonPanel(sdFilterText, sdFilterButton);

	// Book Pane
	final JPanel bookPane = TemplatePanelLayoutUtil.buildTextButtonPanel(booksText, booksButton);

	// CounterParty
	final JPanel cptyPane = TemplatePanelLayoutUtil.buildTextButtonPanel(_cptyPanel.getTextComponent(),
			_cptyPanel.getButtonComponent());
	final JLabel cptyLabel = _cptyPanel.getLabelComponent();

		TemplatePanelLayoutUtil.buildLine(pane, 0,
			new Component[] { transferIdLabel, transferIdText, sdFilterLabel, sdFilterPane, tradeFilterLabel,
					tradeFilterChoice });
	TemplatePanelLayoutUtil.buildLine(pane, 1, new Component[] { booksLabel, bookPane, cptyLabel, cptyPane });
	}

	protected void initLabels() {
		transferIdLabel = new JLabel(CalypsoResourceBundle.getResourceKey("TransferReportTemplatePanel",
				"com.calypso.apps.reporting.TransferReportTemplatePanel.initLabels.transferIdLabel"), 4);
		transferIdLabel.setLabelFor(transferIdText);

	// SDFilter
	sdFilterLabel = new JLabel(CalypsoResourceBundle.getResourceKey("TransferReportTemplatePanel",
			"com.calypso.apps.reporting.TransferReportTemplatePanel.initLabels.sdFilterLabel"));
	// this.sdFilterLabel.setLabelFor(this.sdFilterText);

	// TradeFilter
	tradeFilterLabel = new JLabel(CalypsoResourceBundle.getResourceKey("TradeReportTemplatePanel",
			"com.calypso.apps.reporting.TradeReportTemplatePanel.initLabels.tradeFilterLabel"), 4);

	// Book
	booksLabel = new JLabel(CalypsoResourceBundle.getResourceKey("TransferReportTemplatePanel",
			"com.calypso.apps.reporting.TransferReportTemplatePanel.initLabels.booksLabel"), 4);
	booksLabel.setLabelFor(booksText);

	// CheckBox
	// this.onlineLabel = new JLabel("Online", 4);

	}

	protected void initTextFields() {
		transferIdText = TemplatePanelLayoutUtil.standardTextField();
	sdFilterText = TemplatePanelLayoutUtil.standardTextField();
	booksText = TemplatePanelLayoutUtil.standardTextField();
	}

protected void initButton() {
	sdFilterButton = TemplatePanelLayoutUtil.standardButton();
	booksButton = TemplatePanelLayoutUtil.standardButton();
}

protected void initCheckBox() {
	onlineCheckBox = new JCheckBox();
	onlineCheckBox.setHorizontalAlignment(11);
}

protected void initComboBox() {
	tradeFilterChoice = TemplatePanelLayoutUtil.standardCalypsoComboBox();
}

protected void initListeners() {
	final PirumTransferReportTemplatePanel.SymAction lSymAction = new PirumTransferReportTemplatePanel.SymAction();
	sdFilterButton.addActionListener(lSymAction);
	booksButton.addActionListener(lSymAction);
}

protected void initOtherPanel() {
	_cptyPanel = new LegalEntityTextPanel();
	_cptyPanel.setRole((String) null, "CP role: ALL", true, true);
	_cptyPanel.allowMultiple(true);
}

protected void initDomains() {
	AppUtil.setTradeFilterChoice(tradeFilterChoice);
	final Vector<String> filter = new Vector<>();
	filter.add("");
	filter.addAll(AppUtil.get(tradeFilterChoice));
	AppUtil.set(tradeFilterChoice, filter, true);
}

void sdFilterButton_actionPerformed(ActionEvent event) {
	try {
		final List<String> names = SDFilterGroupUtil.getSDFilterNamesForWindow(this.getClass().getName(), true,
				new String[] { "Reporting", "XferReport" });
		if (names == null) {
			return;
		}

		names.add(0, "");
		final JTextField var5 = sdFilterText;
		String var6 = null;
		final String var7 = var5.getText();
		var6 = SqlEscaper.escape(var5, var7);
		final String source = AppUtil.chooseValue("Static Data Filter ", "Select Static Data Filter", names, var6,
				false, false, this, -1, false, true, true);
		if (source == null) {
			return;
		}

		sdFilterText.setText(source);
	} catch (final Exception var8) {
		Log.error(this, var8);
	}

}

@SuppressWarnings({ "rawtypes", "unchecked" })
void booksButton_actionPerformed(ActionEvent event) {
	Vector books = new Vector();

	try {
		books = AccessUtil.getAllNames(2);
	} catch (final Exception var7) {
		Log.error(this, var7);
	}

	final JTextField var4 = booksText;
	String var5 = null;
	final String var6 = var4.getText();
	var5 = SqlEscaper.escape(var4, var6);
	Vector v = Util.string2Vector(var5);
	v = (Vector) DualListDialog.chooseList(new Vector(), this, books, v, "Select Books", true, true);
	if (v != null) {
		booksText.setText(Util.collectionToString(v));
	}

}

	@Override
	public ReportTemplate getTemplate() {

		ReportTemplate reportTemplate = _template;
		JTextField var2 = transferIdText;
		String var3 = null;
		String var4 = var2.getText();
		var3 = SqlEscaper.escape(var2, var4);
		reportTemplate.put(TransferReportTemplate.XFER_ID, var3);

		// SDFilter
		reportTemplate = _template;
		var2 = sdFilterText;
		var3 = null;
		var4 = var2.getText();
		var3 = SqlEscaper.escape(var2, var4);
		reportTemplate.put(TransferReportTemplate.SD_FILTER, var3);

		// TradeFilter
		_template.put(TRADE_FILTER, tradeFilterChoice.getSelectedItem());

		// Book
		reportTemplate = _template;
		var2 = booksText;
		var3 = null;
		var4 = var2.getText();
		var3 = SqlEscaper.escape(var2, var4);
		reportTemplate.put(TransferReportTemplate.BOOK, var3);

		// Counterparty
		if (Util.isEmpty(_cptyPanel.getRole())) {
			_template.remove(TransferReportTemplate.CPTYROLE);
		} else {
			_template.put(TransferReportTemplate.CPTYROLE, _cptyPanel.getRole());
		}
		_template.put(TransferReportTemplate.CPTYNAME, _cptyPanel.getLEIdsStr());

		// CheckBox
		_template.put(IS_ONLINE, onlineCheckBox.isSelected());

		return _template;

	}

	@Override
	public void setTemplate(ReportTemplate template) {
		_template = template;

		transferIdText.setText("");
	String s = (String) _template.get(TransferReportTemplate.XFER_ID);
		if (s != null) {
			transferIdText.setText(s);
		}

	// SDFilter
	s = (String) _template.get(TransferReportTemplate.SD_FILTER);
	if (s != null) {
		sdFilterText.setText(s);
	} else {
		sdFilterText.setText("");
	}

	// TradeFilter
	s = (String) _template.get(TRADE_FILTER);
	if (s != null) {
		tradeFilterChoice.calypsoSetSelectedItem(s);
	} else {
		tradeFilterChoice.calypsoSetSelectedItem("ALL");
	}

	// Book
	booksText.setText("");
	s = (String) _template.get(TransferReportTemplate.BOOK);
	if (s != null) {
		booksText.setText(s);
	}

	// CounterParty
	s = (String) _template.get(TransferReportTemplate.CPTYNAME);
	setLegalEntitiesFromTemplate(_cptyPanel, s);
	final String role = (String) _template.get(TransferReportTemplate.CPTYROLE);
	_cptyPanel.setRole((String) null, "CP role: ALL", true, true);
	if (!Util.isEmpty(role)) {
		_cptyPanel.setSelectedRole(role);
	}

}

private void setLegalEntitiesFromTemplate(final LegalEntityTextPanel panel, final String s) {
	if (!Util.isEmpty(s)) {
		final Vector<String> leNames = Util.string2Vector(s);
		final String first = leNames.get(0);
		boolean ids = true;
		try {
			Integer.valueOf(first);
		} catch (final NumberFormatException e) {
			ids = false;
		}
		if (ids) {
			panel.setLEIds(Util.string2IntVector(s));
		} else {
			panel.setLEs(leNames);
		}
	} else {
		panel.setLE("");
		panel.setLEIdsStr("");
	}
}

// Setters and getters

public JLabel getTransferIdLabel() {
	return transferIdLabel;
}

public void setTransferIdLabel(JLabel transferIdLabel) {
	this.transferIdLabel = transferIdLabel;
}

public JLabel getSdFilterLabel() {
	return sdFilterLabel;
}

public void setSdFilterLabel(JLabel sdFilterLabel) {
	this.sdFilterLabel = sdFilterLabel;
}

public JTextField getTransferIdText() {
	return transferIdText;
}

public void setTransferIdText(JTextField transferIdText) {
	this.transferIdText = transferIdText;
}

public JTextField getSdFilterText() {
	return sdFilterText;
}

public void setSdFilterText(JTextField sdFilterText) {
	this.sdFilterText = sdFilterText;
}

public JButton getSdFilterButton() {
	return sdFilterButton;
}

public void setSdFilterButton(JButton sdFilterButton) {
	this.sdFilterButton = sdFilterButton;
}

class SymAction implements ActionListener {
	@Override
	public void actionPerformed(ActionEvent event) {
		try {
			setCursor(new Cursor(3));
			final Object object = event.getSource();
			if (object == sdFilterButton) {
				sdFilterButton_actionPerformed(event);
			} else if (object == booksButton) {
				booksButton_actionPerformed(event);
			}
		} finally {
			setCursor(new Cursor(0));
		}

	}
	}

}