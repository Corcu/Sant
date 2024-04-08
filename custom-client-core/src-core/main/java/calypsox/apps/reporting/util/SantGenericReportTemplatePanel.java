package calypsox.apps.reporting.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.border.Border;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.LocalCache;

import calypsox.apps.reporting.util.control.SantCheckBoxPanel;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.apps.reporting.util.control.SantLegalEntityPanel;
import calypsox.apps.reporting.util.control.SantStartEndDatePanel;
import calypsox.apps.reporting.util.control.SantTextFieldPanel;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.loader.AgreementLoader;
import calypsox.tk.report.loader.InstrumentTypeLoader;
import calypsox.tk.report.loader.LegalEntityLoader;
import calypsox.tk.report.loader.PortfolioLoader;

public class SantGenericReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = 3315090510665968822L;

	protected ReportTemplate reportTemplate;

	protected Map<Integer, String> marginCallContractIdsMap;

	protected Map<Integer, String> portfolioIdsMap;

	protected Map<Integer, String> valAgentIdsMap;

	protected List<String> instrumentTypes;

	protected SantStartEndDatePanel processStartEndDatePanel;

	protected SantComboBoxPanel<Integer, String> agreementStatusPanel;

	protected SantComboBoxPanel<Integer, String> agreementTypePanel;

	protected SantComboBoxPanel<Integer, String> instrumentTypePanel;

	protected SantLegalEntityPanel poDealPanel;

	protected SantLegalEntityPanel poAgrPanel;

	protected SantLegalEntityPanel cptyPanel;

	protected SantComboBoxPanel<Integer, String> valuationPanel;

	protected SantComboBoxPanel<Integer, String> baseCcyPanel;

	protected SantTextFieldPanel tradeIdPanel;

	protected SantChooseButtonPanel agreementNamePanel;

	protected SantChooseButtonPanel tradeStatusPanel;

	protected SantChooseButtonPanel portfolioPanel;

	protected SantComboBoxPanel<Integer, String> economicSectorPanel;

	protected SantComboBoxPanel<Integer, String> isFundPanel;

	protected SantComboBoxPanel<Integer, String> headCloneIndicatorPanel;

	protected SantCheckBoxPanel matureDealsPanel;

	protected SantCheckBoxPanel mtmZeroPanel;

	protected JPanel checkBoxPanel;
	
	protected SantCheckBoxPanel lastAllocationCurrencyCheckBox;
	
	public static final String LAST_ALLOCATION_CURRENCY = "LAST_ALLOCATION_CURRENCY";

	public SantGenericReportTemplatePanel() {
		loadStaticData();
		init();
	}

	@Override
	public void setValDatetime(JDatetime valDatetime) {
		this.processStartEndDatePanel.setValDatetime(valDatetime);
	}

	protected SantLegalEntityPanel getCounterPartyPanel() {
		return new SantLegalEntityPanel(LegalEntity.COUNTERPARTY, "CounterParty", false, true, true, true);
	}

	protected void loadStaticData() {
		this.instrumentTypes = new InstrumentTypeLoader().load();
		this.marginCallContractIdsMap = new AgreementLoader().load();
		this.portfolioIdsMap = new PortfolioLoader().load();
		this.valAgentIdsMap = new LegalEntityLoader().load("Calc_Agent");
	}

	protected void init() {
		buildControlsPanel();

		setSize(getPanelSize());

		final JPanel masterPanel = new JPanel();
		masterPanel.setLayout(new BorderLayout());
		masterPanel.setBorder(getMasterPanelBorder());
		add(masterPanel);

		masterPanel.add(getNorthPanel(), BorderLayout.NORTH);

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(1, 3));

		masterPanel.add(mainPanel, BorderLayout.CENTER);

		final JPanel column1Panel = getColumn1Panel();
		final JPanel column2Panel = getColumn2Panel();
		final JPanel column3Panel = getColumn3Panel();

		mainPanel.add(column1Panel);
		mainPanel.add(column2Panel);
		mainPanel.add(column3Panel);

	}

	protected Dimension getPanelSize() {
		return new Dimension(0, 210);
	}

	protected Component getNorthPanel() {
		return this.processStartEndDatePanel;
	}

	protected void buildControlsPanel() {
		this.processStartEndDatePanel = new SantStartEndDatePanel("Process");

		this.agreementStatusPanel = new SantComboBoxPanel<Integer, String>("Agr Status", "CollateralStatus", true);

		this.agreementTypePanel = new SantComboBoxPanel<Integer, String>("Agr Type", "legalAgreementType", true);

		this.instrumentTypePanel = new SantComboBoxPanel<Integer, String>("Instr Type", this.instrumentTypes);

		this.poDealPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, "Owner (Deals)", false, true, true, true);

		this.poAgrPanel = new SantLegalEntityPanel(LegalEntity.PROCESSINGORG, "Owner (Agr)", false, true, true, true);

		this.cptyPanel = getCounterPartyPanel();

		this.agreementNamePanel = new SantChooseButtonPanel("Agr Name", getSortedMap(this.marginCallContractIdsMap).values());

		this.baseCcyPanel = new SantComboBoxPanel<Integer, String>("Base Currency", LocalCache.getCurrencies());

		this.tradeIdPanel = new SantTextFieldPanel("Trade Id", Color.RED);

		this.valuationPanel = new SantComboBoxPanel<Integer, String>("Val Agent", this.valAgentIdsMap);

		this.portfolioPanel = new SantChooseButtonPanel("Portfolio", getSortedMap(this.portfolioIdsMap).values());

		this.tradeStatusPanel = new SantChooseButtonPanel("Trade Status", "tradeStatus");

		this.economicSectorPanel = new SantComboBoxPanel<Integer, String>("Eco Sector",
				"mccAdditionalField.ECONOMIC_SECTOR", true);

		this.isFundPanel = new SantComboBoxPanel<Integer, String>("Is fund", "mccAdditionalField.HEDGE_FUNDS_REPORT",
				true);
		this.headCloneIndicatorPanel = new SantComboBoxPanel<Integer, String>("Head/Clone Ind",
				"mccAdditionalField.HEAD_CLONE", true);
		
		this.matureDealsPanel = new SantCheckBoxPanel("Mature Deals");
		
		this.mtmZeroPanel = new SantCheckBoxPanel("MTM Zero");
		
		this.lastAllocationCurrencyCheckBox = new SantCheckBoxPanel("Last Allocation Currency Filter", 180);
	}

	protected Border getMasterPanelBorder() {
		return BorderFactory.createLineBorder(Color.BLUE);
	}

	protected JPanel getColumn1Panel() {
		final JPanel column1Panel = new JPanel();
		column1Panel.setLayout(new GridLayout(6, 1));

		column1Panel.add(this.poAgrPanel);
		column1Panel.add(this.poDealPanel);
		column1Panel.add(this.cptyPanel);
		column1Panel.add(this.tradeStatusPanel);
		column1Panel.add(this.instrumentTypePanel);

		return column1Panel;
	}

	protected JPanel getColumn2Panel() {
		final JPanel column2Panel = new JPanel();
		column2Panel.setLayout(new GridLayout(7, 1));

		column2Panel.add(this.agreementNamePanel);
		column2Panel.add(this.agreementTypePanel);
		column2Panel.add(this.agreementStatusPanel);
		column2Panel.add(this.valuationPanel);
		column2Panel.add(this.tradeIdPanel);
		column2Panel.add(this.lastAllocationCurrencyCheckBox);

		return column2Panel;
	}

	protected JPanel getColumn3Panel() {
		final JPanel column3Panel = new JPanel();
		column3Panel.setLayout(new GridLayout(6, 1));

		column3Panel.add(this.portfolioPanel);
		column3Panel.add(this.economicSectorPanel);
		column3Panel.add(this.isFundPanel);
		column3Panel.add(this.headCloneIndicatorPanel);
		column3Panel.add(this.baseCcyPanel);

		this.checkBoxPanel = new JPanel();
		final Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		box.add(this.matureDealsPanel);
		box.add(Box.createRigidArea(new Dimension(5, 0)));
		box.add(this.mtmZeroPanel);
		this.checkBoxPanel.add(box);
		column3Panel.add(this.checkBoxPanel);

		this.checkBoxPanel.setPreferredSize(this.economicSectorPanel.getPreferredSize());
		return column3Panel;
	}

	protected boolean getFundsOnly() {
		return false;
	}
	
	protected Map<Integer, String> getSortedMap (Map<Integer, String> ids){
		ValueComparator bvc = new ValueComparator(ids);
		Map<Integer, String> sortedMap = new TreeMap<Integer, String>(bvc);
		sortedMap.putAll(ids);
		return sortedMap;
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

	protected Object getMultipleKey(final String value, final Map<Integer, String> map) {
		final Vector<String> agreementNames = Util.string2Vector(value);
		final Vector<Integer> agreementIds = new Vector<Integer>();
		for (final String agreementName : agreementNames) {
			agreementIds.add((Integer) getKey(agreementName, map));
		}
		return Util.collectionToString(agreementIds);
	}

	@Override
	public ReportTemplate getTemplate() {

		this.processStartEndDatePanel.read(this.reportTemplate);

		this.reportTemplate.put(SantGenericTradeReportTemplate.AGREEMENT_STATUS, this.agreementStatusPanel.getValue());

		this.reportTemplate.put(SantGenericTradeReportTemplate.AGREEMENT_TYPE, this.agreementTypePanel.getValue());
		if (!Util.isEmpty(this.cptyPanel.getLE())) {
			this.reportTemplate.put(SantGenericTradeReportTemplate.COUNTERPARTY, this.cptyPanel.getLEIdsStr());
		} else {
			this.reportTemplate.remove(SantGenericTradeReportTemplate.COUNTERPARTY);
		}

		this.reportTemplate.put(SantGenericTradeReportTemplate.FUND_ONLY, getFundsOnly());

		this.reportTemplate.put(SantGenericTradeReportTemplate.IS_FUND, this.isFundPanel.getValue());

		this.reportTemplate.put(SantGenericTradeReportTemplate.ECONOMIC_SECTOR, this.economicSectorPanel.getValue());
		this.reportTemplate.put(SantGenericTradeReportTemplate.HEAD_CLONE_INDICATOR,
				this.headCloneIndicatorPanel.getValue());
		this.reportTemplate.put(SantGenericTradeReportTemplate.INSTRUMENT_TYPE, this.instrumentTypePanel.getValue());
		this.reportTemplate.put(SantGenericTradeReportTemplate.TRADE_ID, this.tradeIdPanel.getValue());
		this.reportTemplate.put(SantGenericTradeReportTemplate.MATURE_DEALS, this.matureDealsPanel.getValue());
		this.reportTemplate.put(SantGenericTradeReportTemplate.MTM_ZERO, this.mtmZeroPanel.getValue());
		if (!Util.isEmpty(this.poAgrPanel.getLE())) {
			this.reportTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS, this.poAgrPanel.getLEIdsStr());
			this.reportTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES, this.poAgrPanel.getLE());
		} else {
			this.reportTemplate.remove(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
			this.reportTemplate.remove(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);
		}
		if (!Util.isEmpty(this.poDealPanel.getLE())) {
			this.reportTemplate.put(SantGenericTradeReportTemplate.OWNER_DEALS, this.poDealPanel.getLEIdsStr());
		} else {
			this.reportTemplate.remove(SantGenericTradeReportTemplate.OWNER_DEALS);
		}

		this.reportTemplate.put(SantGenericTradeReportTemplate.TRADE_STATUS, this.tradeStatusPanel.getValue());

		String value = this.agreementNamePanel.getValue();
		this.reportTemplate.put(SantGenericTradeReportTemplate.AGREEMENT_ID,
				getMultipleKey(value, this.marginCallContractIdsMap));

		value = this.portfolioPanel.getValue();
		this.reportTemplate.put(SantGenericTradeReportTemplate.PORTFOLIO, getMultipleKey(value, this.portfolioIdsMap));

		value = this.valuationPanel.getValue();
		this.reportTemplate.put(SantGenericTradeReportTemplate.VALUATION_AGENT, getKey(value, this.valAgentIdsMap));

		this.reportTemplate.put(SantGenericTradeReportTemplate.BASE_CURRENCY, this.baseCcyPanel.getValue());
		
		this.reportTemplate.put(SantGenericReportTemplatePanel.LAST_ALLOCATION_CURRENCY, lastAllocationCurrencyCheckBox.getValue());
		
		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		this.reportTemplate = template;
		this.processStartEndDatePanel.setTemplate(template);
		this.processStartEndDatePanel.write(template);

		this.agreementNamePanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.AGREEMENT_ID,
				this.marginCallContractIdsMap);

		this.portfolioPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.PORTFOLIO,
				this.portfolioIdsMap);

		this.agreementStatusPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.AGREEMENT_STATUS);

		this.agreementTypePanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.AGREEMENT_TYPE);

		this.cptyPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.COUNTERPARTY);

		this.economicSectorPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.ECONOMIC_SECTOR);

		this.isFundPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.IS_FUND);

		this.headCloneIndicatorPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.HEAD_CLONE_INDICATOR);

		this.instrumentTypePanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.INSTRUMENT_TYPE);

		this.tradeIdPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.TRADE_ID);

		this.matureDealsPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.MATURE_DEALS);

		this.mtmZeroPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.MTM_ZERO);

		this.poAgrPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);

		// this.poAgrPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);

		this.poDealPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.OWNER_DEALS);

		this.tradeStatusPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.TRADE_STATUS);

		this.valuationPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.VALUATION_AGENT);

		this.baseCcyPanel.setValue(this.reportTemplate, SantGenericTradeReportTemplate.BASE_CURRENCY);

		/**
		 * Default value is true (As it was before the add option.)
		 * Eloy
		 */
		if(this.reportTemplate.get(SantGenericReportTemplatePanel.LAST_ALLOCATION_CURRENCY)==null){
			this.reportTemplate.put(SantGenericReportTemplatePanel.LAST_ALLOCATION_CURRENCY, true);
		}
		this.lastAllocationCurrencyCheckBox.setValue(this.reportTemplate, SantGenericReportTemplatePanel.LAST_ALLOCATION_CURRENCY);
	}

	public void hideAllPanels() {
		final boolean visibility = false;
		this.processStartEndDatePanel.setVisible(visibility);
		this.agreementNamePanel.setVisible(visibility);
		this.agreementStatusPanel.setVisible(visibility);
		this.agreementTypePanel.setVisible(visibility);
		this.cptyPanel.setVisible(visibility);
		this.economicSectorPanel.setVisible(visibility);
		this.isFundPanel.setVisible(visibility);
		this.headCloneIndicatorPanel.setVisible(visibility);
		this.instrumentTypePanel.setVisible(visibility);
		this.tradeIdPanel.setVisible(visibility);
		this.matureDealsPanel.setVisible(visibility);
		this.mtmZeroPanel.setVisible(visibility);
		this.poAgrPanel.setVisible(visibility);
		this.poDealPanel.setVisible(visibility);
		this.tradeStatusPanel.setVisible(visibility);
		this.valuationPanel.setVisible(visibility);
		this.portfolioPanel.setVisible(visibility);
		this.baseCcyPanel.setVisible(visibility);
		this.lastAllocationCurrencyCheckBox.setVisible(visibility);
	}

}
