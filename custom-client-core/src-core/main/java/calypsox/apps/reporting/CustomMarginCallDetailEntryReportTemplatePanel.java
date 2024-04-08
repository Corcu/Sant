package calypsox.apps.reporting;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.calypso.apps.reporting.MarginCallDetailEntryReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.report.ReportTemplate;

import calypsox.util.collateral.CollateralUtilities;

public class CustomMarginCallDetailEntryReportTemplatePanel extends MarginCallDetailEntryReportTemplatePanel {

	private static final String ADITIONAL_VALUE = "ADITIONAL_VALUE";
	private static final String VALUE_FIELD = "VALUE_FIELD";
	private static final long serialVersionUID = -2452686739167579729L;
	private CollateralConfigFilterPanel collateralConfigFilterPanel;
	
	public CustomMarginCallDetailEntryReportTemplatePanel() {
		super();
		add(getCollateralConfigFilterPanel());
		setPreferredSize(new Dimension(960, 440));
		setSize(new Dimension(960, 440));
	}
	
	private CollateralConfigFilterPanel getCollateralConfigFilterPanel(){
		if(collateralConfigFilterPanel == null){
			collateralConfigFilterPanel = new CollateralConfigFilterPanel();
			collateralConfigFilterPanel.setBounds(5, 290, 1010, 70);
		}
		return collateralConfigFilterPanel;
	}
	
	public ReportTemplate getTemplate() {
		ReportTemplate template= super.getTemplate();
		template.put(ADITIONAL_VALUE, this.getCollateralConfigFilterPanel().getAdditionalFieldComboBox().getSelectedItem());
		template.put(VALUE_FIELD, this.getCollateralConfigFilterPanel().getAdditionalFieldValueText().getText());
		return template;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		super.setTemplate(template);
		String aditionalValue = template.get(ADITIONAL_VALUE);
		if(aditionalValue!=null)
			this.getCollateralConfigFilterPanel().getAdditionalFieldComboBox().setSelectedItem(aditionalValue);
		else
			this.getCollateralConfigFilterPanel().getAdditionalFieldComboBox().setSelectedIndex(0);
		String valueField = template.get(VALUE_FIELD);
		if(valueField!=null)
			this.getCollateralConfigFilterPanel().getAdditionalFieldValueText().setText(valueField);
		else
			this.getCollateralConfigFilterPanel().getAdditionalFieldValueText().setText("");
		
	}
	
	public class CollateralConfigFilterPanel extends JPanel {

		private static final String DOMAIN_VALUE = "mccAdditionalField";

		private static final String ADDITIONAL_FIELD_TEXT = "Additional Field :";

		private static final String VALUE_FIELD_TEXT = "Value Field :";

		private static final String COLLATERAL_CONFIG = "Collateral Config";

		private static final long serialVersionUID = 627824578386220222L;
		
		@SuppressWarnings("rawtypes")
		protected JComboBox additionalFieldComboBox = null;
		private JLabel additionalFieldLabel = null;
		private JLabel additionalFieldValueLabel = null;
		private JTextField additionalFieldValueText = null;
		
		public CollateralConfigFilterPanel() {
			setLayout(null);
			add(getAdditionalFieldLabel());
			add(getAdditionalFieldComboBox());
			add(getAdditionalFieldValueText());
			add(getAdditionalFieldValueLabel());
			
			setBorder(new TitledBorder(new EtchedBorder(1, null, null), COLLATERAL_CONFIG, 4, 2, null, null));
		}
		
		private JTextField getAdditionalFieldValueText() {
			if (additionalFieldValueText == null) {
				additionalFieldValueText = new JTextField("");
				additionalFieldValueText.setVisible(true);
				additionalFieldValueText.setEditable(true);
				additionalFieldValueText.setBounds(140+250+20+58+8, 25, 132, 24);
				
			}
			return additionalFieldValueText;
		}
		
		private JLabel getAdditionalFieldValueLabel() {
			if (additionalFieldValueLabel == null) {
				additionalFieldValueLabel = new JLabel(VALUE_FIELD_TEXT);
				additionalFieldValueLabel.setBounds(140+250+20, 25, 58, 24);
			}
			return additionalFieldValueLabel;
		}
		
		private JLabel getAdditionalFieldLabel() {
			if (additionalFieldLabel == null) {
				additionalFieldLabel = new JLabel(ADDITIONAL_FIELD_TEXT);
				additionalFieldLabel.setBounds(10, 25, 132, 24);
			}
			return additionalFieldLabel;
		}
		
		@SuppressWarnings("rawtypes")
		private JComboBox getAdditionalFieldComboBox() {
			if (additionalFieldComboBox == null) {
				additionalFieldComboBox = new JComboBox();
				additionalFieldComboBox.setEditable(false);
				additionalFieldComboBox.setBounds(140, 25, 250, 24);
				AppUtil.set(additionalFieldComboBox, getAdditionField());
			}
			return additionalFieldComboBox;
		}
		
		private List<String> getAdditionField() {
		       List<String> reportTypes = new ArrayList<String>();
		       Vector<String> values = CollateralUtilities.getDomainValues(DOMAIN_VALUE);
		       reportTypes.add("");
		       reportTypes.addAll(values);
		       return reportTypes;
		}
	}
}
