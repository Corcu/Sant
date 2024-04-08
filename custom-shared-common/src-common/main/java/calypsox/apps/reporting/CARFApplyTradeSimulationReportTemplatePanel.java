package calypsox.apps.reporting;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.reporting.corporateaction.CAApplyInfoPropertyPane;
import com.calypso.tk.product.BondDanishMortgage;
import com.calypso.tk.product.corporateaction.CAPositionType;
import com.calypso.tk.report.CAApplyTradeReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.ui.factory.PropertyFactory;
import com.calypso.ui.factory.PropertyTableFactory;
import com.calypso.ui.property.BooleanProperty;
import com.calypso.ui.property.DefaultProperty;
import com.calypso.ui.property.MultipleSelectionListProperty;
import com.calypso.ui.property.StringProperty;
import com.jidesoft.grid.Property;
import com.jidesoft.grid.PropertyPane;
import com.jidesoft.grid.PropertyTable;

public class CARFApplyTradeSimulationReportTemplatePanel
		extends com.calypso.apps.reporting.CAApplyTradeSimulationReportTemplatePanel {

	public CARFApplyTradeSimulationReportTemplatePanel() {
		super();
	}

	@Override
	protected ReportTemplatePanel buildTemplatePanelDelegate() {
		return new CARFApplyTradeSimulationFilterReportTemplatePanel();
	}

	private static class CARFApplyTradeSimulationFilterReportTemplatePanel extends ReportTemplatePanel {
		private static final long serialVersionUID = 7465137067377908120L;
		private final CAApplyInfoPropertyPane _caApplyCriteriaPropertyPane;
		private CAApplyTradeReportTemplate _caApplyTradeReportTemplate;

		private BooleanProperty _isAgentAggregationOnlyProperty;
		private MultipleSelectionListProperty<CAPositionType> _caPositionTypeProperty;
		private BooleanProperty _showLogProgressProperty;
		private final BooleanProperty _generateCAFirstProperty;
		private final BooleanProperty _generateRelatedIssuancesProperty;
		private final DefaultProperty<String> _generateDateTypeProperty;
		private final BooleanProperty _useGenerateDrawnBondProperty;
		private transient StringProperty _tradePO;

		private CARFApplyTradeSimulationFilterReportTemplatePanel() {
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createTitledBorder("Corporate Action Application Criteria"));

			this._caApplyCriteriaPropertyPane = new CAApplyInfoPropertyPane(this);
			this._caApplyCriteriaPropertyPane.setShowDescription(false);

			add(this._caApplyCriteriaPropertyPane, "Center");
			PropertyPane tradeSelectionCriteriaPropertyPane = buildFilterDisplayCriteriaPropertyPane();

			BooleanProperty filterDisplayProperty = PropertyFactory
					.makeBooleanProperty(tradeSelectionCriteriaPropertyPane.getName(), null, null);
			filterDisplayProperty.setAutoExpandOnSelection(true);
			filterDisplayProperty.addChildren(0,
					tradeSelectionCriteriaPropertyPane.getPropertyTable().getPropertyTableModel().getProperties());

			this._generateCAFirstProperty = PropertyFactory.makeBooleanProperty("Generate Ca First", null, null);
			this._generateDateTypeProperty = PropertyFactory.makeListProperty("Date Type",
					new Object[] { "Ex Date", "Record Date", "Payment Date" });
			this._generateDateTypeProperty.setDefaultValue("Ex Date");
			this._generateDateTypeProperty.setType(String.class);
			this._generateRelatedIssuancesProperty = PropertyFactory.makeBooleanProperty("Generate Related Issuances",
					null, null);
			this._generateRelatedIssuancesProperty.setDefaultValue(Boolean.valueOf(true));
			this._useGenerateDrawnBondProperty = PropertyFactory.makeBooleanProperty("Generate Drawn Bond", null, null);
			this._useGenerateDrawnBondProperty.setDefaultValue(Boolean.valueOf(true));
			List<DefaultProperty<?>> childrenOfGenerateCAFirst = new ArrayList<DefaultProperty<?>>();
			childrenOfGenerateCAFirst.add(this._generateDateTypeProperty);
			childrenOfGenerateCAFirst.add(this._generateRelatedIssuancesProperty);
			if (BondDanishMortgage.isDrawnBondCreationRequired()) {
				childrenOfGenerateCAFirst.add(this._useGenerateDrawnBondProperty);
			}
			this._generateCAFirstProperty.addChildren(0, childrenOfGenerateCAFirst);
			this._tradePO = PropertyFactory.makeStringProperty("Trade Processing Org", null, null);
			this._tradePO.setDisplayName("Trade Processing Org");
			_caApplyCriteriaPropertyPane.addProperties(
					Arrays.asList(new BooleanProperty[] { filterDisplayProperty, this._generateCAFirstProperty }));
			_caApplyCriteriaPropertyPane.addProperties(Arrays.asList(new StringProperty[] { this._tradePO }));
		}

		public void setTemplate(ReportTemplate template) {
			this._caApplyTradeReportTemplate = (CAApplyTradeReportTemplate) template;
			this._caApplyCriteriaPropertyPane.setReportTemplate(this._caApplyTradeReportTemplate);
			this._caPositionTypeProperty.setValue((List) template.get("CA Position Type"));
			this._isAgentAggregationOnlyProperty.setValue((Boolean) template.get("Agent Aggregation Only"));
			this._showLogProgressProperty.setValue((Boolean) template.get("Show Log Progress"));
			this._generateCAFirstProperty.setValue((Boolean) template.get("Generate Ca First"));
			this._generateRelatedIssuancesProperty.setValue((Boolean) template.get("Generate Related Issuances"));
			this._generateDateTypeProperty.setValue(template.get("Date Type"));
			this._useGenerateDrawnBondProperty.setValue((Boolean) template.get("Generate Drawn Bond"));
			this._tradePO.setValue(template.get("Trade Processing Org"));
		}

		public CAApplyTradeReportTemplate getTemplate() {
			this._caApplyTradeReportTemplate = this._caApplyCriteriaPropertyPane
					.getOrCreateReportTemplate(this._caApplyTradeReportTemplate);

			if (_caApplyTradeReportTemplate != null) {
				_caApplyTradeReportTemplate.putOrRemove("CA Position Type", this._caPositionTypeProperty.getValue());
				_caApplyTradeReportTemplate.putOrRemove("Agent Aggregation Only",
						this._isAgentAggregationOnlyProperty.getValue());
				_caApplyTradeReportTemplate.putOrRemove("Show Log Progress", this._showLogProgressProperty.getValue());
				_caApplyTradeReportTemplate.putOrRemove("Generate Ca First", this._generateCAFirstProperty.getValue());
				_caApplyTradeReportTemplate.putOrRemove("Generate Related Issuances",
						this._generateRelatedIssuancesProperty.getValue());
				_caApplyTradeReportTemplate.putOrRemove("Date Type", this._generateDateTypeProperty.getValue());
				_caApplyTradeReportTemplate.putOrRemove("Generate Drawn Bond",
						this._useGenerateDrawnBondProperty.getValue());
				_caApplyTradeReportTemplate.put("Trade Processing Org", this._tradePO.getValue());

			}
			return _caApplyTradeReportTemplate;
		}

		private PropertyPane buildFilterDisplayCriteriaPropertyPane() {
			this._caPositionTypeProperty = PropertyFactory.makeMultipleSelectionListProperty("CA Position Type", null,
					null, CAPositionType.values(true));
			this._isAgentAggregationOnlyProperty = PropertyFactory.makeBooleanProperty("Agent Aggregation Only", null,
					null);
			this._showLogProgressProperty = PropertyFactory.makeBooleanProperty("Show Log Progress", null, null);
			PropertyTable propertyTable = PropertyTableFactory
					.makePropertyTable(new Property[] { this._caPositionTypeProperty,
							this._isAgentAggregationOnlyProperty, this._showLogProgressProperty });

			PropertyPane p = new PropertyPane(propertyTable, 0);
			p.setShowDescription(false);
			p.setName("Filter Display");
			p.setBorder(BorderFactory.createTitledBorder(p.getName()));
			return p;
		}
	}

}
