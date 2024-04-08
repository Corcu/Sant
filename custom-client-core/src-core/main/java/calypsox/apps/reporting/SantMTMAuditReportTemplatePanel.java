/*
 *
 * Copyright (c) 2011 Kaupthing Bank
 * Borgart?n 19, IS-105 Reykjavik, Iceland
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.calypso.apps.product.ProductUtil;
import com.calypso.apps.reporting.ReportTemplateDatePanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoTextField;
import com.calypso.apps.util.LegalEntityTextPanel;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.User;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.ui.component.dialog.DualListDialog;

import calypsox.apps.reporting.util.ValueComparator;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.SantMTMAuditReportTemplate;
import calypsox.tk.report.loader.AgreementLoader;

public class SantMTMAuditReportTemplatePanel extends ReportTemplatePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final static String LOG_CATEGORY = "SantMTMAuditReport";

	protected ReportTemplate reportTemplate;

	protected LegalEntityTextPanel cptyPanel = null;
	protected LegalEntityTextPanel poPanel = null;

	protected JLabel tradeIdLabel = new JLabel();
	protected JTextField tradeIdText = new JTextField();
	protected ReportTemplateDatePanel tradeStartDate = null;
	protected ReportTemplateDatePanel tradeEndDate = null;
	protected ReportTemplateDatePanel tradeMaturityDate = null;

	protected ReportTemplateDatePanel settleStartDate = null;
	protected ReportTemplateDatePanel settleEndDate = null;
	protected ReportTemplateDatePanel maturityStartDate = null;
	protected ReportTemplateDatePanel maturityEndDate = null;
	protected javax.swing.JLabel tradeLabel = new javax.swing.JLabel();
	protected javax.swing.JLabel settleLabel = new javax.swing.JLabel();
	protected javax.swing.JLabel maturityLabel = new javax.swing.JLabel();

	// javax.swing.JLabel contractIdLabel = new javax.swing.JLabel();
	// javax.swing.JTextField contractIdText = new javax.swing.JTextField();
	protected SantChooseButtonPanel agreementNamePanel;
	protected Map<Integer, String> marginCallContractIdsMap;

	protected javax.swing.JLabel mtmValDateLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField mtmValDateText = new javax.swing.JTextField();

	protected javax.swing.JLabel currencyLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField settleCurrText = new javax.swing.JTextField();
	protected javax.swing.JButton settleCurrButton = new javax.swing.JButton();
	protected javax.swing.JLabel prodTypeLabel = new javax.swing.JLabel();
	protected CalypsoTextField prodTypeText = new CalypsoTextField();
	protected javax.swing.JButton prodTypeButton = new javax.swing.JButton();

	protected javax.swing.JLabel bookLabel = new javax.swing.JLabel();
	protected com.calypso.apps.util.CalypsoComboBox bookChoice = new com.calypso.apps.util.CalypsoComboBox();

	protected javax.swing.JLabel reasonLabel = new javax.swing.JLabel();
	protected com.calypso.apps.util.CalypsoComboBox reasonChoice = new com.calypso.apps.util.CalypsoComboBox();

	@SuppressWarnings("rawtypes")
	protected Vector selectedProds;

	public SantMTMAuditReportTemplatePanel() {

		init();
		initDomains();
		this.reportTemplate = null;
	}

	private void init() {

		setLayout(null);
		setSize(new Dimension(1173, 307));

		// Column 1
		this.poPanel = new LegalEntityTextPanel();
		this.poPanel.setBounds(new Rectangle(10, 10, 283, 24));
		this.poPanel.setRole(LegalEntity.PROCESSINGORG, "Processing Org", false, true);
		this.poPanel.allowMultiple(true);
		this.poPanel.setEditable(true);
		add(this.poPanel);

		this.cptyPanel = new LegalEntityTextPanel();
		this.cptyPanel.setBounds(new Rectangle(10, 40, 283, 24));
		this.cptyPanel.setRole(LegalEntity.COUNTERPARTY, "CounterParty:", true, true);
		this.cptyPanel.allowMultiple(false);
		add(this.cptyPanel);

		this.currencyLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.currencyLabel.setText("Currency");
		add(this.currencyLabel);
		this.currencyLabel.setBounds(50, 70, 80, 24);
		add(this.settleCurrText);
		this.settleCurrText.setBounds(142, 70, 120, 24);
		this.settleCurrButton.setText("...Ccy");
		this.settleCurrButton.setActionCommand("...");
		this.settleCurrButton.setBounds(262, 70, 32, 24);
		add(this.settleCurrButton);

		this.prodTypeLabel.setText("Product Type");
		add(this.prodTypeLabel);
		this.prodTypeLabel.setBounds(50, 100, 80, 24);
		add(this.prodTypeText);
		this.prodTypeText.setBounds(142, 100, 120, 24);
		this.prodTypeButton.setText("...");
		this.prodTypeButton.setActionCommand("...");
		this.prodTypeButton.setBounds(262, 100, 32, 24);
		add(this.prodTypeButton);

		this.bookLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.bookLabel.setText("Portfolio ");
		add(this.bookLabel);
		this.bookLabel.setBounds(50, 130, 80, 24);
		add(this.bookChoice);
		this.bookChoice.setBounds(142, 130, 150, 24);

		this.reasonLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.reasonLabel.setText("Change Reason ");
		add(this.reasonLabel);
		this.reasonLabel.setBounds(35, 160, 100, 24);
		add(this.reasonChoice);
		this.reasonChoice.setBounds(142, 160, 150, 24);

		// Column 2
		this.tradeLabel.setText("Trade");
		this.tradeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.tradeLabel.setBounds(310, 10, 50, 24);
		add(this.tradeLabel);
		this.tradeStartDate = ReportTemplateDatePanel.getStart();
		this.tradeStartDate.setBounds(360, 10, 215, 24);
		add(this.tradeStartDate);
		this.tradeEndDate = ReportTemplateDatePanel.getEnd();
		this.tradeEndDate.setBounds(575, 10, 215, 24);
		add(this.tradeEndDate);
		this.tradeEndDate.setDependency(this.tradeStartDate);

		this.settleLabel.setText("Settle");
		this.settleLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.settleLabel.setBounds(310, 40, 50, 24);
		add(this.settleLabel);
		this.settleStartDate = ReportTemplateDatePanel.getStart();
		add(this.settleStartDate);
		this.settleStartDate.setBounds(360, 40, 215, 24);
		this.settleEndDate = ReportTemplateDatePanel.getEnd();
		add(this.settleEndDate);
		this.settleEndDate.setBounds(575, 40, 215, 24);
		this.settleEndDate.setDependency(this.settleStartDate);

		this.maturityLabel.setText("Maturity");
		this.maturityLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.maturityLabel.setBounds(310, 70, 50, 24);
		add(this.maturityLabel);
		this.maturityStartDate = ReportTemplateDatePanel.getStart();
		add(this.maturityStartDate);
		this.maturityStartDate.setBounds(360, 70, 215, 24);
		this.maturityEndDate = ReportTemplateDatePanel.getEnd();
		add(this.maturityEndDate);
		this.maturityEndDate.setBounds(575, 70, 215, 24);
		this.maturityEndDate.setDependency(this.maturityStartDate);

		this.tradeIdLabel.setText("TradeId");
		this.tradeIdLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.tradeIdLabel.setBounds(320, 130, 100, 24);
		add(this.tradeIdLabel);
		this.tradeIdText.setBounds(420, 130, 100, 24);
		add(this.tradeIdText);

		// this.contractIdLabel.setText("Contract Id");
		// this.contractIdLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		// this.contractIdLabel.setBounds(600, 130, 70, 24);
		// add(this.contractIdLabel);
		// this.contractIdText.setBounds(680, 130, 100, 24);
		// add(this.contractIdText);

		this.marginCallContractIdsMap = new AgreementLoader().load();
		ValueComparator bvc = new ValueComparator(this.marginCallContractIdsMap);
		Map<Integer, String> sortedMap = new TreeMap<Integer, String>(bvc);
		sortedMap.putAll(this.marginCallContractIdsMap);
		this.agreementNamePanel = new SantChooseButtonPanel("Agr Name", sortedMap.values());
		this.agreementNamePanel.setBounds(600, 130, 270, 24);
		add(this.agreementNamePanel);

		this.mtmValDateLabel.setText("MTM ValDate");
		this.mtmValDateLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.mtmValDateLabel.setBounds(320, 160, 100, 24);
		add(this.mtmValDateLabel);
		this.mtmValDateText.setBounds(420, 160, 100, 24);
		add(this.mtmValDateText);

		// //_tradeMaturityDate
		// _tradeMaturityDate = ReportTemplateDatePanel.getEnd();
		// _tradeMaturityDate.setBounds(300, 70, 215, 24);
		// add(_tradeMaturityDate);

		// Register Listners
		SymAction lSymAction = new SymAction();
		this.settleCurrButton.addActionListener(lSymAction);
		this.prodTypeButton.addActionListener(lSymAction);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void initDomains() {
		Vector v = null;

		this.tradeStartDate.init(TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
				TradeReportTemplate.START_TENOR);
		this.tradeEndDate.init(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);

		AppUtil.addStartEndDatesActionListener(this.tradeStartDate, this.tradeEndDate);

		this.settleStartDate.init(TradeReportTemplate.SETTLE_START_DATE, TradeReportTemplate.SETTLE_START_PLUS,
				TradeReportTemplate.SETTLE_START_TENOR);
		this.settleEndDate.init(TradeReportTemplate.SETTLE_END_DATE, TradeReportTemplate.SETTLE_END_PLUS,
				TradeReportTemplate.SETTLE_END_TENOR);

		AppUtil.addStartEndDatesActionListener(this.settleStartDate, this.settleEndDate);
		this.maturityStartDate.init(TradeReportTemplate.MATURITY_START_DATE, TradeReportTemplate.MATURITY_START_PLUS,
				TradeReportTemplate.MATURITY_START_TENOR);
		this.maturityEndDate.init(TradeReportTemplate.MATURITY_END_DATE, TradeReportTemplate.MATURITY_END_PLUS,
				TradeReportTemplate.MATURITY_END_TENOR);

		AppUtil.addStartEndDatesActionListener(this.maturityStartDate, this.maturityEndDate);

		AppUtil.addDateListener(this.mtmValDateText);
		// AppUtil.addNumberListener(contractIdText);

		v = AccessUtil.getAllNames(User.BOOK);
		if (v != null) {
			v = (Vector) v.clone();
			v.insertElementAt("", 0);
		} else {
			v = new Vector();
		}
		AppUtil.set(this.bookChoice, v);

		Vector<String> reasonValuesTemp = LocalCache.getDomainValues(DSConnection.getDefault(),
				CollateralStaticAttributes.DOMAIN_SANT_MTM_CHANGE_REASON);
		Vector<String> reasonValues = new Vector<String>();
		if (reasonValuesTemp != null) {
			reasonValues = (Vector) reasonValuesTemp.clone();
		}
		reasonValues.insertElementAt("", 0);
		AppUtil.set(this.reasonChoice, reasonValues);
	}

	class SymAction implements java.awt.event.ActionListener {
		@Override
		public void actionPerformed(java.awt.event.ActionEvent event) {
			try {
				setCursor(new Cursor(java.awt.Cursor.WAIT_CURSOR));
				Object object = event.getSource();

				if (object == SantMTMAuditReportTemplatePanel.this.settleCurrButton) {
					settleCurrButton_ActionPerformed(event);
				} else if (object == SantMTMAuditReportTemplatePanel.this.prodTypeButton) {
					productTypeButton_ActionPerformed(event);
					// else if (object == productTypeText)
					// productTypeText_ActionPerformed(event);
				}
			} finally {
				setCursor(new Cursor(java.awt.Cursor.DEFAULT_CURSOR));
			}
		}
	}

	void settleCurrButton_ActionPerformed(java.awt.event.ActionEvent event) {
		try {
			Vector<String> currencies = LocalCache.getDomainValues(DSConnection.getDefault(), "currency");
			Vector<String> myVector = Util.string2Vector(this.settleCurrText.getText());
			myVector = (Vector<String>)DualListDialog.chooseList(new Vector<String>(), this, currencies, myVector);
			if (myVector != null) {
				this.settleCurrText.setText(Util.collectionToString(myVector));
			}
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
		}
	}

	void productTypeButton_ActionPerformed(java.awt.event.ActionEvent event) {
		try {
			List<String> sels = ProductUtil
					.chooseProductTypeList(this, Util.string2Vector(this.prodTypeText.getText()));
			if (sels != null) {
				this.prodTypeText.setText(Util.collectionToString(sels), true);
				this.prodTypeLabel.setText("Product Type");
				this.selectedProds = null;
				this.prodTypeLabel.setToolTipText("Dbl-Click to choose Product Ids");
			}
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public ReportTemplate getTemplate() {

		if (Util.isEmpty(this.mtmValDateText.getText())) {
			AppUtil.displayError("Please enter MTM Value date", this);
			return null;
		}

		this.tradeStartDate.read(this.reportTemplate);
		this.tradeEndDate.read(this.reportTemplate);
		this.settleStartDate.read(this.reportTemplate);
		this.settleEndDate.read(this.reportTemplate);
		this.maturityStartDate.read(this.reportTemplate);
		this.maturityEndDate.read(this.reportTemplate);

		if (!Util.isEmpty(this.cptyPanel.getLE())) {
			this.reportTemplate.put(TradeReportTemplate.CPTYNAME, this.cptyPanel.getLEIdsStr());
		} else {
			this.reportTemplate.remove(TradeReportTemplate.CPTYNAME);
		}

		this.reportTemplate.put(TradeReportTemplate.PRODUCT_TYPE, this.prodTypeText.getText());
		this.reportTemplate.remove(TradeReportTemplate.PRODUCT_IDS);
		if (this.selectedProds != null) {
			this.reportTemplate.put(TradeReportTemplate.PRODUCT_IDS, Util.vector2String(this.selectedProds));
		}

		this.reportTemplate.put(SantMTMAuditReportTemplate.MTM_CURRENCY, this.settleCurrText.getText());
		this.reportTemplate.put(TradeReportTemplate.BOOK, this.bookChoice.getSelectedItem());

		this.reportTemplate.put(TradeReportTemplate.PROCESSING_ORG, this.poPanel.getLEIdsStr());
		this.reportTemplate.put(SantMTMAuditReportTemplate.MTM_VAL_DATE, this.mtmValDateText.getText());

		// this.reportTemplate.put(SantMTMAuditReportTemplate.AGR_IDS, this.agreementNamePanel.getValue());
		String value = this.agreementNamePanel.getValue();
		this.reportTemplate.put(SantMTMAuditReportTemplate.AGR_IDS,
				getMultipleKey(value, this.marginCallContractIdsMap));

		this.reportTemplate.put(TradeReportTemplate.TRADE_ID, this.tradeIdText.getText());

		this.reportTemplate.put(SantMTMAuditReportTemplate.MTM_CHANGE_REASON, this.reasonChoice.getSelectedItem());

		return this.reportTemplate;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		this.reportTemplate = template;
		this.tradeStartDate.setTemplate(template);
		this.tradeEndDate.setTemplate(template);
		this.settleStartDate.setTemplate(template);
		this.settleEndDate.setTemplate(template);
		this.maturityStartDate.setTemplate(template);
		this.maturityEndDate.setTemplate(template);
		this.tradeStartDate.write(template);
		this.tradeEndDate.write(template);
		this.settleStartDate.write(template);
		this.settleEndDate.write(template);
		this.maturityStartDate.write(template);
		this.maturityEndDate.write(template);

		String s = (String) this.reportTemplate.get(SantMTMAuditReportTemplate.MTM_VAL_DATE);
		if (s != null) {
			this.mtmValDateText.setText(s);
		} else {
			try {
				// Set previous day by default
				JDate prevDate = JDate.getNow().addBusinessDays(-1,
						LocalCache.getCurrencyDefault("EUR").getDefaultHolidays());
				this.mtmValDateText.setText(prevDate.toString());
			} catch (Exception exc) {
				Log.error(this, exc); //sonar
			}
		}

		this.cptyPanel.setLE("");
		s = (String) this.reportTemplate.get(TradeReportTemplate.CPTYNAME);
		if (s != null) {
			this.cptyPanel.setLE(s);
		}
		if (s != null) {
			this.cptyPanel.setLEIdsStr(s);
		}
		this.cptyPanel.setRole(null, "CP role: ALL", true, true);
		s = (String) this.reportTemplate.get(TradeReportTemplate.LE_ROLE_ONLY);
		if (s != null) {
			if (!Util.isEmpty(s)) {
				this.cptyPanel.setSelectedRole(s);
			}
		}
		String role = (String) this.reportTemplate.get(TradeReportTemplate.LE_ROLE);
		if (!Util.isEmpty(role)) {
			this.cptyPanel.setSelectedRole(role);
		}

		this.bookChoice.setSelectedIndex(0);
		s = (String) this.reportTemplate.get(TradeReportTemplate.BOOK);
		if (s != null) {
			this.bookChoice.setSelectedItem(s);
		}
		s = (String) this.reportTemplate.get(SantMTMAuditReportTemplate.MTM_CURRENCY);
		if (s != null) {
			this.settleCurrText.setText(s);
		} else {
			this.settleCurrText.setText("");
		}
		this.prodTypeText.setText("", true);
		this.selectedProds = null;
		this.prodTypeLabel.setText("Product Type");
		s = (String) this.reportTemplate.get(TradeReportTemplate.PRODUCT_TYPE);
		if (s != null) {
			this.prodTypeText.setText(s, true);
			this.prodTypeText.setToolTipText(s);
		}
		s = (String) this.reportTemplate.get(TradeReportTemplate.PRODUCT_IDS);
		if (s != null) {
			this.selectedProds = Util.string2Vector(s);
			this.prodTypeLabel.setText("Securities");
			this.prodTypeLabel.setToolTipText("Hit button [...] to choose Product Types");
		}

		this.poPanel.setLE("");
		s = (String) this.reportTemplate.get(TradeReportTemplate.PROCESSING_ORG);
		if (s != null) {
			this.poPanel.setLE(s);
		}
		if (s != null) {
			this.poPanel.setLEIdsStr(s);
		}

	}

	protected Object getMultipleKey(final String value, final Map<Integer, String> map) {
		final Vector<String> agreementNames = Util.string2Vector(value);
		final Vector<Integer> agreementIds = new Vector<Integer>();
		for (final String agreementName : agreementNames) {
			agreementIds.add((Integer) getKey(agreementName, map));
		}
		return Util.collectionToString(agreementIds);
	}

	private Object getKey(final String value, final Map<Integer, String> map) {
		for (final Entry<Integer, String> entry : map.entrySet()) {
			if (entry.getValue() == null) {
				return null;
			}
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static void main(String... args) throws ConnectException {
		ConnectionUtil.connect(args, "SantMTMAuditReportTemplate");
		JFrame frame = new JFrame();
		frame.setContentPane(new SantMTMAuditReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}

}
