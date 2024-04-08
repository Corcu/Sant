package calypsox.apps.reporting;

import calypsox.tk.report.PositionKeeperReportTemplate;
import com.calypso.apps.internationalization.rb.CalypsoResourceBundle;
import com.calypso.apps.product.ProductUtil;
import com.calypso.apps.reporting.PositionKeeperJFrame;
import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.reporting.bo.PosAggregationFilter.AggregationKind;
import com.calypso.apps.reporting.bo.PosAggregationFilterPanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Product;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

@SuppressWarnings("rawtypes")
public class PositionKeeperReportTemplatePanel extends ReportTemplatePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected ReportTemplate _template;

	Product _product;
	JLabel JLabel2 = new JLabel();
	JLabel JLabel3 = new JLabel();
	JLabel JLabel4 = new JLabel();
	CalypsoComboBox portfolioChoice = new CalypsoComboBox();
	JLabel aggregateLabel = new JLabel();

	JComboBox aggregateChoice = new JComboBox();
	JTextField productDescText = new JTextField();
	JButton productDescButton = new JButton();
	JLabel zeroPositionLabel = new JLabel();
	JComboBox zeroPositionChoice = new JComboBox();
	JCheckBox mergeWithFeesCheck = new JCheckBox();
	JCheckBox bySettleDateCheck = new JCheckBox();
	JLabel liquidationKeysLabel = new JLabel();
	PosAggregationFilterPanel liquidationKeysPanel;

	public PositionKeeperReportTemplatePanel() {
		initPanel();
		updateDomains();
	}

	public void initPanel() {

		this.setLayout((LayoutManager) null);
		add(this.portfolioChoice);
		this.portfolioChoice.setBounds(105, 5, 184, 22);
		this.JLabel2.setHorizontalAlignment(4);
		this.JLabel2.setText(CalypsoResourceBundle.getResourceKey("PositionKeeperJFrame",
				"com.calypso.apps.reporting.PositionKeeperJFrame.jbInit.JLabel2"));
		add(this.JLabel2);
		this.JLabel2.setBounds(5, 5, 93, 22);
		this.aggregateLabel.setHorizontalAlignment(4);
		this.aggregateLabel.setText(CalypsoResourceBundle.getResourceKey("PositionKeeperJFrame",
				"com.calypso.apps.reporting.PositionKeeperJFrame.jbInit.aggregateLabel"));
		add(this.aggregateLabel);
		this.aggregateLabel.setBounds(5, 30, 93, 22);
		add(this.aggregateChoice);
		this.aggregateChoice.setBounds(105, 30, 184, 22);
		this.mergeWithFeesCheck.setText(CalypsoResourceBundle.getResourceKey("PositionKeeperJFrame",
				"com.calypso.apps.reporting.PositionKeeperJFrame.jbInit.mergeWithFeesCheck"));
		this.mergeWithFeesCheck.setActionCommand("IncludeFees");
		add(this.mergeWithFeesCheck);
		this.mergeWithFeesCheck.setBounds(310, 55, 180, 22);
		this.bySettleDateCheck.setText(CalypsoResourceBundle.getResourceKey("PositionKeeperJFrame",
				"com.calypso.apps.reporting.PositionKeeperJFrame.jbInit.bySettleDateCheck"));
		this.bySettleDateCheck.setActionCommand("PositionBySettleDate");
		add(this.bySettleDateCheck);
		this.bySettleDateCheck.setBounds(310, 30, 180, 22);
		this.liquidationKeysLabel.setText(CalypsoResourceBundle.getResourceKey("PositionKeeperJFrame",
				"com.calypso.apps.reporting.PositionKeeperJFrame.jbInit.liquidationKeysLabel"));
		this.liquidationKeysLabel.setHorizontalAlignment(4);
		this.liquidationKeysLabel.setBounds(5, 80, 93, 24);
		add(this.liquidationKeysLabel);
		this.liquidationKeysPanel = PosAggregationFilterPanel.createAggregationFilterPanel(AggregationKind.LIQUIDATION,
				"");
		this.liquidationKeysPanel.setSize(new Dimension(250, 24));
		this.liquidationKeysPanel.setPreferredSize(new Dimension(250, 24));
		this.liquidationKeysPanel.setMinimumSize(new Dimension(250, 24));
		this.liquidationKeysPanel.setBounds(105, 80, 250, 24);
		add(this.liquidationKeysPanel);
		this.JLabel3.setHorizontalAlignment(4);
		this.JLabel3.setText(CalypsoResourceBundle.getResourceKey("PositionKeeperJFrame",
				"com.calypso.apps.reporting.PositionKeeperJFrame.jbInit.JLabel3"));
		this.JLabel3.setToolTipText(CalypsoResourceBundle.getResourceKey("PositionKeeperJFrame",
				"com.calypso.apps.reporting.PositionKeeperJFrame.jbInit.JLabel30"));
		add(this.JLabel3);
		this.JLabel3.setForeground(Color.red);
		this.JLabel3.setBounds(290, 5, 70, 22);
		add(this.productDescText);
		this.productDescText.setBounds(365, 5, 180, 22);
		this.productDescText.setEditable(false);
		this.productDescButton.setText(CalypsoResourceBundle.getResourceKey("PositionKeeperJFrame",
				"com.calypso.apps.reporting.PositionKeeperJFrame.jbInit.productDescButton"));
		this.productDescButton.setActionCommand("...");
		add(this.productDescButton);
		this.productDescButton.setBounds(546, 5, 25, 22);
		this.zeroPositionLabel.setHorizontalAlignment(4);
		this.zeroPositionLabel.setText(CalypsoResourceBundle.getResourceKey("PositionKeeperJFrame",
				"com.calypso.apps.reporting.PositionKeeperJFrame.jbInit.zeroPositionLabel"));
		add(this.zeroPositionLabel);
		this.zeroPositionLabel.setBounds(5, 55, 93, 22);
		add(this.zeroPositionChoice);
		this.zeroPositionChoice.setBounds(105, 55, 184, 22);


		SymAction lSymAction = new SymAction();
		productDescButton.addActionListener(lSymAction);

	}

	@Override
	public ReportTemplate getTemplate() {
		_template.put(PositionKeeperReportTemplate.TRADE_FILTER, portfolioChoice.getSelectedItem());
		_template.put(PositionKeeperReportTemplate.AGGREGATION, aggregateChoice.getSelectedItem());
		_template.put(PositionKeeperReportTemplate.ZERO_POSITION, zeroPositionChoice.getSelectedIndex());
		_template.put(PositionKeeperReportTemplate.PRODUCT, _product);
		_template.put(PositionKeeperReportTemplate.POSITION_BY_SETTLEDATE, bySettleDateCheck.isSelected());
		_template.put(PositionKeeperReportTemplate.INCLUDE_FEE, mergeWithFeesCheck.isSelected());
		_template.put(PositionKeeperReportTemplate.LIQUIDATION_KEYS, liquidationKeysPanel.getFilterDescriptor());

		return _template;
	}
	
	protected void setJComboBoxValue(JComboBox comboBox, String value) {
		if(value==null) 
			comboBox.setSelectedIndex(0);
		else
			comboBox.setSelectedItem(value);
	}
	
	protected void setJCheckBoxValue(JCheckBox checkBox, Boolean value) {
		if(value==null) 
			checkBox.setSelected(false);
		else
			checkBox.setSelected(value);
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		_template = template;
		if(template!=null) {
		
			setJComboBoxValue(portfolioChoice, template.get(PositionKeeperReportTemplate.TRADE_FILTER));
			setJComboBoxValue(aggregateChoice, template.get(PositionKeeperReportTemplate.AGGREGATION));
			Integer selectedIndex = template.get(PositionKeeperReportTemplate.ZERO_POSITION);
			if(selectedIndex==null)
				selectedIndex=0;
			zeroPositionChoice.setSelectedIndex(selectedIndex);
			
			_product=template.get(PositionKeeperReportTemplate.PRODUCT);
			if(_product!=null) {
				productDescText.setText(_product.getDescription());
			}
			else {
				productDescText.setText("");
			}
		
			setJCheckBoxValue(bySettleDateCheck, template.get(PositionKeeperReportTemplate.POSITION_BY_SETTLEDATE));
			setJCheckBoxValue(mergeWithFeesCheck, template.get(PositionKeeperReportTemplate.INCLUDE_FEE));
	
			liquidationKeysPanel.setFilterDescriptor(template.get(PositionKeeperReportTemplate.LIQUIDATION_KEYS));
		}
		
	}

	class SymAction implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Object object = event.getSource();
			if (object == productDescButton) {
				productDescButton_actionPerformed(event);
			} 
		}
	}



	void productDescButton_actionPerformed(ActionEvent event) {
		Vector v = null;
		Product p = null;

		new Vector();
		v = PositionKeeperJFrame.getOnlySecondaryMarketProduct(false);
		p = ProductUtil.getProduct(null, v);
		this.productDescText(p);

	}

	@SuppressWarnings("deprecation")
	void productDescText(Product product) {
		this._product = null;
		this.productDescText.setText("");

		int id = product.getId();
		try {
			product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), id);
		} catch (Exception e) {
			AppUtil.displayError("Error accessing Product " + id, this);
		}
		if (product == null) {
			AppUtil.displayError("No Product " + id + " found.", this);
			return;
		}
		this._product = product;
		this.productDescText.setText(this._product.getDescription());

	}

	@SuppressWarnings("unchecked")
	void updateDomains() {

		String sel = (String) this.portfolioChoice.getSelectedItem();
		Vector v = AccessUtil.getAllNames(5);
		AppUtil.set(this.portfolioChoice, v, true);
		if (sel != null) {
			this.portfolioChoice.calypsoSetSelectedItem(sel);
		}

		try {
			v = DSConnection.getDefault().getRemoteReferenceData().getBookAttributeNames();
			v.addElement("LegalEntity");
			v.addElement("BookName");
			v.addElement("Activity");
			v.addElement("Location");
			v.addElement("AccountingBook");
			AppUtil.set(this.aggregateChoice, v, true);
			this.aggregateChoice.setSelectedItem("BookName");
		} catch (CalypsoServiceException e) {
			Log.error(this,e);
		}

		v = new Vector();
		v.insertElementAt("Include", 0);
		v.insertElementAt("Exclude 0 nominal / 0 P&L", 1);
		v.insertElementAt("Exclude 0 nominal", 2);
		AppUtil.set(this.zeroPositionChoice, v, false);

	}


}
