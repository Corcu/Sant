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
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.calypso.apps.product.ProductUtil;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportTemplateDatePanel;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoTextField;
import com.calypso.apps.util.LegalEntityTextPanel;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.User;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.ui.component.dialog.DualListDialog;

import calypsox.apps.reporting.util.ValueComparator;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.SantTradeBrowserReportTemplate;
import calypsox.tk.report.loader.AgreementLoader;

public class SantTradeBrowserReportTemplatePanel extends ReportTemplatePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final static String LOG_CATEGORY = "SantTradeBrowserReportTemplatePanel";

	protected ReportTemplate reportTemplate;

	protected LegalEntityTextPanel cptyPanel = null;
	protected LegalEntityTextPanel poAgrPanel = null;
	protected LegalEntityTextPanel poDealPanel = null;

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

	protected SantChooseButtonPanel agreementNamePanel;
	protected Map<Integer, String> marginCallContractIdsMap;

	protected javax.swing.JLabel boRefLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField boRefText = new javax.swing.JTextField();

	protected javax.swing.JLabel frontIDLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField frontIDText = new javax.swing.JTextField();
	protected javax.swing.JLabel sourceLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField sourceText = new javax.swing.JTextField();
	protected javax.swing.JLabel valAgentLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField valAgentText = new javax.swing.JTextField();
	protected javax.swing.JLabel structureLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField structureText = new javax.swing.JTextField();
	protected javax.swing.JLabel rigCodeLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField rigCodeText = new javax.swing.JTextField();

	protected javax.swing.JLabel valDateFromLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField valDateFromText = new javax.swing.JTextField();
	protected javax.swing.JLabel valDateToLabel = new javax.swing.JLabel();
	protected javax.swing.JTextField valDateToText = new javax.swing.JTextField();

	protected javax.swing.JButton settleCurrButton = new javax.swing.JButton();
	protected javax.swing.JLabel prodTypeLabel = new javax.swing.JLabel();
	protected CalypsoTextField prodTypeText = new CalypsoTextField();
	protected javax.swing.JButton prodTypeButton = new javax.swing.JButton();

	protected javax.swing.JLabel bookLabel = new javax.swing.JLabel();
	protected com.calypso.apps.util.CalypsoComboBox bookChoice = new com.calypso.apps.util.CalypsoComboBox();

	protected javax.swing.JLabel agrTypeLabel = new javax.swing.JLabel();
	protected com.calypso.apps.util.CalypsoComboBox agrTypeChoice = new com.calypso.apps.util.CalypsoComboBox();

	protected javax.swing.JLabel tradeStatusLabel = new javax.swing.JLabel();
	protected CalypsoTextField tradeStatusText = new CalypsoTextField();
	protected javax.swing.JButton tradeStatusButton = new javax.swing.JButton();

	protected javax.swing.JLabel buySellLabel = new javax.swing.JLabel();
	protected com.calypso.apps.util.CalypsoComboBox buySellChoice = new com.calypso.apps.util.CalypsoComboBox();

	protected javax.swing.JLabel instrumentLabel = new javax.swing.JLabel();
	protected com.calypso.apps.util.CalypsoComboBox instrumentChoice = new com.calypso.apps.util.CalypsoComboBox();

	public static final String DOMAIN_AGREEMENT_TYPE = "legalAgreementType";
	public static final String DOMAIN_TRADE_STATUS = "tradeStatus";

	@SuppressWarnings("rawtypes")
	protected Vector selectedProds;
	@SuppressWarnings("rawtypes")
	protected Vector selectedTradeStatuses;

	// ARCHIVE
	protected JCheckBox includeArchiveCheckBox = new JCheckBox();

	public SantTradeBrowserReportTemplatePanel() {

		this.marginCallContractIdsMap = new AgreementLoader().load();

		init();
		initDomains();
		this.reportTemplate = null;
	}

	private int getMaxContractsSelectable() {
		int MaxContractsSelectable = 0;
		String MaxContractsSelectableStr = LocalCache.getDomainValueComment(DSConnection.getDefault(),
				CollateralStaticAttributes.DOMAIN_SANT_CUSTOMREPORT_PROPS,
				CollateralStaticAttributes.TRADEBROWSER2_MAX_SELECTABLE_AGRS);

		try {
			MaxContractsSelectable = Integer.parseInt(MaxContractsSelectableStr);
		} catch (Exception exc) {
			Log.error(this, exc); //sonar
		}

		if (MaxContractsSelectable == 0) {
			MaxContractsSelectable = 10;
		}
		return MaxContractsSelectable;
	}

	private void init() {

		setLayout(null);
		setSize(new Dimension(1173, 307));

		// Column 1
		this.poAgrPanel = new LegalEntityTextPanel();
		this.poAgrPanel.setBounds(new Rectangle(10, 10, 283, 24));
		this.poAgrPanel.setRole(LegalEntity.PROCESSINGORG, "Owner (Agr)", false, true);
		this.poAgrPanel.allowMultiple(true);
		this.poAgrPanel.setEditable(true);
		add(this.poAgrPanel);

		// poDealPanel
		this.poDealPanel = new LegalEntityTextPanel();
		this.poDealPanel.setBounds(new Rectangle(10, 40, 283, 24));
		this.poDealPanel.setRole(LegalEntity.PROCESSINGORG, "Owner(Deals)", false, true);
		this.poDealPanel.allowMultiple(true);
		this.poDealPanel.setEditable(true);
		add(this.poDealPanel);

		this.cptyPanel = new LegalEntityTextPanel();
		this.cptyPanel.setBounds(new Rectangle(10, 70, 283, 24));
		this.cptyPanel.setRole(LegalEntity.COUNTERPARTY, "CounterParty:", true, true);
		this.cptyPanel.allowMultiple(true);
		this.cptyPanel.allowMultiple(true);
		add(this.cptyPanel);

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

		this.agrTypeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.agrTypeLabel.setText("Agr Type ");
		add(this.agrTypeLabel);
		this.agrTypeLabel.setBounds(35, 160, 100, 24);
		add(this.agrTypeChoice);
		this.agrTypeChoice.setBounds(142, 160, 150, 24);

		this.tradeStatusLabel.setText("Trade Status");
		add(this.tradeStatusLabel);
		this.tradeStatusLabel.setBounds(50, 190, 80, 24);
		add(this.tradeStatusText);
		this.tradeStatusText.setBounds(142, 190, 120, 24);
		this.tradeStatusButton.setText("...");
		this.tradeStatusButton.setActionCommand("...");
		this.tradeStatusButton.setBounds(262, 190, 32, 24);
		add(this.tradeStatusButton);

		// buySellLabel buySellChoice
		this.buySellLabel.setText("Direction");
		this.buySellLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.buySellLabel.setBounds(35, 220, 100, 24);
		this.buySellChoice.setBounds(142, 220, 150, 24);
		add(this.buySellLabel);
		add(this.buySellChoice);

		// instrumentChoice
		this.instrumentLabel.setText("Instrument");
		this.instrumentLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		this.instrumentLabel.setBounds(35, 250, 100, 24);
		this.instrumentChoice.setBounds(142, 250, 150, 24);
		add(this.instrumentLabel);
		add(this.instrumentChoice);

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
		this.tradeIdLabel.setBounds(320, 130, 70, 24);
		add(this.tradeIdLabel);
		this.tradeIdText.setBounds(390, 130, 100, 24);
		add(this.tradeIdText);

		// this.contractIdLabel.setText("Contract Id");
		// this.contractIdLabel
		// .setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		// this.contractIdLabel.setBounds(600, 130, 70, 24);
		// add(this.contractIdLabel);
		// this.contractIdText.setBounds(680, 130, 100, 24);
		// add(this.contractIdText);

		final ValueComparator bvc = new ValueComparator(this.marginCallContractIdsMap);
		final Map<Integer, String> sortedMap = new TreeMap<Integer, String>(bvc);
		sortedMap.putAll(this.marginCallContractIdsMap);
		this.agreementNamePanel = new SantChooseButtonPanel("Agr Name", sortedMap.values(),
				getMaxContractsSelectable());
		this.agreementNamePanel.setBounds(560, 130, 350, 24);
		add(this.agreementNamePanel);

		this.boRefLabel.setText("BO Ref");
		this.boRefLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.boRefLabel.setBounds(320, 160, 70, 24);
		add(this.boRefLabel);
		this.boRefText.setBounds(390, 160, 100, 24);
		add(this.boRefText);

		this.frontIDLabel.setText("Front Id");
		this.frontIDLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.frontIDLabel.setBounds(600, 160, 70, 24);
		add(this.frontIDLabel);
		this.frontIDText.setBounds(680, 160, 100, 24);
		add(this.frontIDText);

		this.sourceLabel.setText("Source");
		this.sourceLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.sourceLabel.setBounds(320, 190, 70, 24);
		add(this.sourceLabel);
		this.sourceText.setBounds(390, 190, 100, 24);
		add(this.sourceText);

		this.structureLabel.setText("Structure");
		this.structureLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.structureLabel.setBounds(600, 190, 70, 24);
		add(this.structureLabel);
		this.structureText.setBounds(680, 190, 100, 24);
		add(this.structureText);

		this.valAgentLabel.setText("Val Agent");
		this.valAgentLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.valAgentLabel.setBounds(320, 220, 70, 24);
		add(this.valAgentLabel);
		this.valAgentText.setBounds(390, 220, 100, 24);
		add(this.valAgentText);

		// GSM: rigCode
		this.rigCodeLabel.setText("RIG Code");
		this.rigCodeLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.rigCodeLabel.setBounds(600, 220, 70, 24);
		add(this.rigCodeLabel);
		this.rigCodeText.setBounds(680, 220, 100, 24);
		add(this.rigCodeText);

		this.valDateFromLabel.setText("From Date");
		this.valDateFromLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.valDateFromLabel.setBounds(320, 250, 70, 24);
		add(this.valDateFromLabel);
		this.valDateFromText.setBounds(390, 250, 100, 24);
		add(this.valDateFromText);

		this.valDateToLabel.setText("To Date");
		this.valDateToLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.valDateToLabel.setBounds(600, 250, 70, 24);
		add(this.valDateToLabel);
		this.valDateToText.setBounds(680, 250, 100, 24);
		add(this.valDateToText);

		// Register Listners
		final SymAction lSymAction = new SymAction();
		this.settleCurrButton.addActionListener(lSymAction);
		this.prodTypeButton.addActionListener(lSymAction);
		this.tradeStatusButton.addActionListener(lSymAction);

		// ARCHIVE
		this.includeArchiveCheckBox.setBounds(new Rectangle(850, 181, 177, 24));
		this.includeArchiveCheckBox.setText("Include Archive ");
		this.includeArchiveCheckBox.setMargin(new Insets(2, 2, 2, 2));
		this.includeArchiveCheckBox.setOpaque(false);
		this.includeArchiveCheckBox.setActionCommand("IncludeArchive");
		this.includeArchiveCheckBox.setAlignmentY(0.5F);
		add(this.includeArchiveCheckBox);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
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

		v = AccessUtil.getAllNames(User.BOOK);
		if (v != null) {
			v = (Vector) v.clone();
			v.insertElementAt("", 0);
		} else {
			v = new Vector();
		}
		AppUtil.set(this.bookChoice, v);

		Vector<String> agrTypeDomainTemp = LocalCache.getDomainValues(DSConnection.getDefault(), DOMAIN_AGREEMENT_TYPE);
		Vector<String> agrTypeDomain = new Vector<String>();
		if (agrTypeDomainTemp != null) {
			agrTypeDomain = (Vector) agrTypeDomainTemp.clone();
		}
		agrTypeDomain.insertElementAt("", 0);
		AppUtil.set(this.agrTypeChoice, agrTypeDomain);

		final Vector buyVec = new Vector();
		buyVec.add("");
		buyVec.add("Buy");
		buyVec.add("Sell");
		AppUtil.set(this.buySellChoice, buyVec);

		// Instrument Combo
		List<String> instrumentsList = new ArrayList<String>();
		instrumentsList.add("");
		instrumentsList.addAll(CollateralExposure.getSubTypes(false));

		Collections.sort(instrumentsList);
		AppUtil.set(this.instrumentChoice, instrumentsList);

		AppUtil.addDateListener(this.valDateFromText);
		AppUtil.addDateListener(this.valDateToText);

	}

	class SymAction implements java.awt.event.ActionListener {
		@Override
		public void actionPerformed(final java.awt.event.ActionEvent event) {
			try {
				setCursor(new Cursor(java.awt.Cursor.WAIT_CURSOR));
				final Object object = event.getSource();

				// if (object == settleCurrButton)
				// settleCurrButton_ActionPerformed(event);
				if (object == SantTradeBrowserReportTemplatePanel.this.prodTypeButton) {
					productTypeButton_ActionPerformed(event);
				} else if (object == SantTradeBrowserReportTemplatePanel.this.tradeStatusButton) {
					tradeStatusButton_ActionPerformed(event);
				}
			} finally {
				setCursor(new Cursor(java.awt.Cursor.DEFAULT_CURSOR));
			}
		}
	}

	// void settleCurrButton_ActionPerformed(java.awt.event.ActionEvent event) {
	// try {
	// Vector currencies =
	// LocalCache.getDomainValues(DSConnection.getDefault(),
	// "currency");
	// Vector myVector = Util.string2Vector(settleCurrText.getText());
	// myVector = AppUtil.chooseList(this, currencies, myVector);
	// if (myVector != null) {
	// settleCurrText.setText(Util.vector2String(myVector));
	// }
	// }
	// catch (Exception e) {
	// Log.error(LOG_CATEGORY, e);
	// }
	// }

	void productTypeButton_ActionPerformed(final java.awt.event.ActionEvent event) {
		try {
			final List<String> sels = ProductUtil.chooseProductTypeList(this,
					Util.string2Vector(this.prodTypeText.getText()));
			if (sels != null) {
				this.prodTypeText.setText(Util.collectionToString(sels), true);
				this.prodTypeLabel.setText("Product Type");
				this.selectedProds = null;
				this.prodTypeLabel.setToolTipText("Dbl-Click to choose Product Ids");
			}
		} catch (final Exception e) {
			Log.error(LOG_CATEGORY, e);
		}
	}

	void tradeStatusButton_ActionPerformed(final java.awt.event.ActionEvent event) {
		try {
			final Vector<String> tradeStatuses = LocalCache.getDomainValues(DSConnection.getDefault(),
					DOMAIN_TRADE_STATUS);
			Vector<String> myVector = Util.string2Vector(this.tradeStatusText.getText());
			myVector = (Vector<String>) DualListDialog.chooseList(new Vector<String>(), this, tradeStatuses, myVector);
			if (myVector != null) {
				this.tradeStatusText.setText(Util.collectionToString(myVector));
			}
		} catch (final Exception e) {
			Log.error(LOG_CATEGORY, e);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public ReportTemplate getTemplate() {

		if (Util.isEmpty(this.valDateFromText.getText())) {
			AppUtil.displayError("Please enter MTM From date", this);
			return null;
		} else {
			final JDate valDateFrom = Util.stringToJDate(this.valDateFromText.getText());
			this.reportTemplate.put(SantTradeBrowserReportTemplate.VAL_DATE_FROM, valDateFrom);
		}

		if (Util.isEmpty(this.valDateToText.getText())) {
			AppUtil.displayError("Please enter MTM To date", this);
			return null;
		} else {
			final JDate valDateTo = Util.stringToJDate(this.valDateToText.getText());
			this.reportTemplate.put(SantTradeBrowserReportTemplate.VAL_DATE_TO, valDateTo);

			// From Date should be before ToDate
			final JDate valDateFrom = Util.stringToJDate(this.valDateFromText.getText());
			if (valDateFrom.compareTo(valDateTo) > 0) {
				AppUtil.displayError("MTM To date should be later than From Date", this);
				return null;
			}
		}

		// Set contract id in the template
		final String value = this.agreementNamePanel.getValue();
		if (!Util.isEmpty(value)) {
			this.reportTemplate.put(SantTradeBrowserReportTemplate.CONTRACT_IDS,
					getMultipleKey(value, this.marginCallContractIdsMap));
		} else {
			this.reportTemplate.remove(SantTradeBrowserReportTemplate.CONTRACT_IDS);
		}

		this.tradeStartDate.read(this.reportTemplate);
		this.tradeEndDate.read(this.reportTemplate);
		this.settleStartDate.read(this.reportTemplate);
		this.settleEndDate.read(this.reportTemplate);
		this.maturityStartDate.read(this.reportTemplate);
		this.maturityEndDate.read(this.reportTemplate);

		if (!Util.isEmpty(this.poAgrPanel.getLEIdsStr())) {
			this.reportTemplate.put(SantTradeBrowserReportTemplate.PROCESSING_ORG_AGR, this.poAgrPanel.getLEIdsStr());
		} else {
			this.reportTemplate.remove(SantTradeBrowserReportTemplate.PROCESSING_ORG_AGR);
		}

		if (!Util.isEmpty(this.poDealPanel.getLEIdsStr())) {
			this.reportTemplate.put(SantTradeBrowserReportTemplate.PROCESSING_ORG_DEAL, this.poDealPanel.getLEIdsStr());
		} else {
			this.reportTemplate.remove(SantTradeBrowserReportTemplate.PROCESSING_ORG_DEAL);
		}

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

		this.reportTemplate.put(TradeReportTemplate.BOOK, this.bookChoice.getSelectedItem());
		this.reportTemplate.put(SantTradeBrowserReportTemplate.AGR_TYPE, this.agrTypeChoice.getSelectedItem());
		this.reportTemplate.put(SantTradeBrowserReportTemplate.TRADE_STATUS, this.tradeStatusText.getText());
		this.reportTemplate.put(SantTradeBrowserReportTemplate.BUY_SELL, this.buySellChoice.getSelectedItem());
		this.reportTemplate.put(SantTradeBrowserReportTemplate.INSTRUMENT, this.instrumentChoice.getSelectedItem());

		this.reportTemplate.put(TradeReportTemplate.TRADE_ID, this.tradeIdText.getText());

		this.reportTemplate.put(SantTradeBrowserReportTemplate.BO_REFERENCE, this.boRefText.getText());
		this.reportTemplate.put(SantTradeBrowserReportTemplate.FRONT_ID, this.frontIDText.getText());
		this.reportTemplate.put(SantTradeBrowserReportTemplate.SOURCE, this.sourceText.getText());
		this.reportTemplate.put(SantTradeBrowserReportTemplate.STRUCTURE, this.structureText.getText());
		this.reportTemplate.put(SantTradeBrowserReportTemplate.VAL_AGENT, this.valAgentText.getText());
		// GSM: RIG CODE
		this.reportTemplate.put(SantTradeBrowserReportTemplate.RIG_CODE, this.rigCodeText.getText());

		// ARCHIVE
		this.reportTemplate.put("IncludeArchive", Boolean.valueOf(this.includeArchiveCheckBox.isSelected()));

		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
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

		this.agreementNamePanel.setValue(this.reportTemplate, SantTradeBrowserReportTemplate.CONTRACT_IDS,
				this.marginCallContractIdsMap);

		final JDate valDateFrom = (JDate) this.reportTemplate.get(SantTradeBrowserReportTemplate.VAL_DATE_FROM);
		if (valDateFrom != null) {
			this.valDateFromText.setText(valDateFrom.toString());
		} else {
			try {
				// Set previous day by default
				final JDate prevDate = JDate.getNow().addBusinessDays(-1,
						LocalCache.getCurrencyDefault("EUR").getDefaultHolidays());
				this.valDateFromText.setText(prevDate.toString());
			} catch (final Exception exc) {
				Log.error(this, exc); //sonar
			}
		}

		final JDate valDateTo = (JDate) this.reportTemplate.get(SantTradeBrowserReportTemplate.VAL_DATE_TO);
		if (valDateTo != null) {
			this.valDateToText.setText(valDateTo.toString());
		} else {
			try {
				// Set previous day by default
				final JDate prevDate = JDate.getNow().addBusinessDays(-1,
						LocalCache.getCurrencyDefault("EUR").getDefaultHolidays());
				this.valDateToText.setText(prevDate.toString());
			} catch (final Exception exc) {
				Log.error(this, exc); //sonar
			}
		}

		this.poAgrPanel.setLE("");
		String s = (String) this.reportTemplate.get(SantTradeBrowserReportTemplate.PROCESSING_ORG_AGR);
		if (s != null) {
			this.poAgrPanel.setLEIdsStr(s);
		}

		this.poDealPanel.setLE("");
		s = (String) this.reportTemplate.get(SantTradeBrowserReportTemplate.PROCESSING_ORG_DEAL);
		if (s != null) {
			this.poDealPanel.setLE(s);
		}
		if (s != null) {
			this.poDealPanel.setLEIdsStr(s);
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
		final String role = (String) this.reportTemplate.get(TradeReportTemplate.LE_ROLE);
		if (!Util.isEmpty(role)) {
			this.cptyPanel.setSelectedRole(role);
		}

		this.prodTypeText.setText("", true);
		this.selectedProds = null;
		this.prodTypeLabel.setText("Product Type");
		s = (String) this.reportTemplate.get(TradeReportTemplate.PRODUCT_TYPE);
		if (s != null) {
			this.prodTypeText.setText(s, true);
			this.prodTypeText.setToolTipText(s);
		}
		

		this.bookChoice.setSelectedIndex(0);
		s = (String) this.reportTemplate.get(TradeReportTemplate.BOOK);
		if (s != null) {
			this.bookChoice.setSelectedItem(s);
		}

		this.agrTypeChoice.setSelectedIndex(0);
		s = (String) this.reportTemplate.get(SantTradeBrowserReportTemplate.AGR_TYPE);
		if (s != null) {
			this.agrTypeChoice.setSelectedItem(s);
		}

		this.tradeStatusText.setText("");
		s = (String) this.reportTemplate.get(SantTradeBrowserReportTemplate.TRADE_STATUS);
		if (s != null) {
			this.tradeStatusText.setText(s);
		}

		s = (String) this.reportTemplate.get(SantTradeBrowserReportTemplate.BUY_SELL);
		if (s != null) {
			this.buySellChoice.setSelectedItem(s);
		} else {
			this.buySellChoice.setSelectedIndex(0);
		}

		this.instrumentChoice.setSelectedIndex(0);
		s = (String) this.reportTemplate.get(SantTradeBrowserReportTemplate.INSTRUMENT);
		if (s != null) {
			this.instrumentChoice.setSelectedItem(s);
		}

		// Column 2

		s = (String) this.reportTemplate.get(SantTradeBrowserReportTemplate.BO_REFERENCE);
		if (s != null) {
			this.boRefText.setText(s);
		} else {
			this.boRefText.setText("");
		}

		s = (String) this.reportTemplate.get(TradeReportTemplate.TRADE_ID);
		if (s != null) {
			this.tradeIdText.setText(s);
		} else {
			this.tradeIdText.setText("");
		}

		// GSM: RIC_CODE. COLUMN 3
		s = (String) this.reportTemplate.get(SantTradeBrowserReportTemplate.RIG_CODE);
		if (s != null) {
			this.rigCodeText.setText(s);
		} else {
			this.rigCodeText.setText("");
		}

		// ARCHIVE
		Boolean b = (Boolean) this.reportTemplate.get("IncludeArchive");
		if (b != null)
			this.includeArchiveCheckBox.setSelected(b.booleanValue());
		else {
			this.includeArchiveCheckBox.setSelected(false);
		}
		// TradeReportTemplate.TRADE_ID

	}

	protected Object getMultipleKey(final String value, final Map<Integer, String> map) {
		final Vector<String> agreementNames = Util.string2Vector(value);
		final Vector<Integer> agreementIds = new Vector<Integer>();
		for (String agreementName : agreementNames) {
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
				final int key = entry.getKey();
				return key;
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean isValidLoad(ReportPanel panel) {
		Map potentialSizesByTypeOfObject = panel.getReport().getPotentialSize();
		return displayLargeListWarningMessage(this, potentialSizesByTypeOfObject);
	}

}
