/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

import calypsox.apps.reporting.util.SantMultiPOSelectorReportTemplatePanel;
import calypsox.tk.report.KGR_Collateral_MarginCallReportTemplate;

public class KGR_Collateral_MarginCallReportTemplatePanel extends SantMultiPOSelectorReportTemplatePanel {

	private static final long serialVersionUID = -652481905098762452L;

	// Cestas
	private JLabel systemLabel;
	private JTextField systemName;
	//Maturity offset
	private JLabel maturityLabel;
	private JTextField maturityValue;

	public KGR_Collateral_MarginCallReportTemplatePanel() {

		add(buildKGRPanel());
		setPreferredSize(new Dimension(1000, 130));
		setSize(new Dimension(900, 130));
		setLayout(null);
	}

	private Component buildKGRPanel() {

		JPanel panel = new JPanel();
		panel.setBounds(5, 5, 580, 120);
		panel.setBorder(new TitledBorder(new EtchedBorder(1, null, null), "KGR/IRIS Collateral MarginCall Report", 4,
				2, null, null));
		panel.setLayout(null);

		super.buildControlsPanel();
		this.poAgrPanel.setBounds(5, 80, 300, 24);
		add(this.poAgrPanel);
		panel.add(this.poAgrPanel);

		// Add components Cestas
		panel.add(getSystemLabel());
		panel.add(getSystemTextField());
		
		//AAP Add components Maturity Offset Days
		panel.add(getSystemLabelMaturity());
		panel.add(getMaturityTextField());

		return panel;
	}

	@Override
	public ReportTemplate getTemplate() {

		ReportTemplate template = super.getTemplate();
		Vector<String> errors=setDefautlsAndBuildErrorMessage();
		if(!errors.isEmpty()){
			AppUtil.displayAdvice(errors, this);
		}
		template.put(KGR_Collateral_MarginCallReportTemplate.MATURITY_OFFSET, this.maturityValue.getText());
		template.put(KGR_Collateral_MarginCallReportTemplate.SOURCE_SYSTEM, this.systemName.getText());

		return template;
	}

	private Vector<String> setDefautlsAndBuildErrorMessage(){
		Vector<String> errors=new Vector<>();
		if (Util.isEmpty(this.systemName.getText())) {
			errors.add("Please enter Source System for IRIS\n");
		}
		if(Util.isEmpty(this.maturityValue.getText())){
			errors.add("No value was set in Maturity Days Offset:");
			errors.add("7 days is used by default");
		}
		return errors;

	}
	@Override
	public void setTemplate(ReportTemplate template) {

		super.setTemplate(template);
		String s = (String) super.reportTemplate.get(KGR_Collateral_MarginCallReportTemplate.SOURCE_SYSTEM);
		if (Util.isEmpty(s)) {
			s = "";
		}
		this.systemName.setText(s);
		
		String s1 = (String) super.reportTemplate.get(KGR_Collateral_MarginCallReportTemplate.MATURITY_OFFSET);
		if (Util.isEmpty(s1)) {
			s1 = "";
		}
		this.maturityValue.setText(s1);
	}

	// CESTAS
	private Component getSystemLabel() {
		if (this.systemLabel == null) {
			this.systemLabel = new JLabel("Source System:");
			this.systemLabel.setBounds(10, 30, 154, 24);
		}
		return this.systemLabel;
	}

	//AAP Maturity offset days
	private Component getSystemLabelMaturity() {
		if (this.maturityLabel == null) {
			this.maturityLabel = new JLabel("Collateral Maturity Offset:");
			this.maturityLabel.setBounds(276, 30, 154, 24);
		}
		return this.maturityLabel;
	}
	
	private Component getMaturityTextField() {
		if (this.maturityValue == null) {
			this.maturityValue = new JTextField();
			this.maturityValue.setBounds(429, 30, 95, 24);
			this.maturityValue.setToolTipText("7 days by default");
			this.maturityValue.setText("");
		}
		return this.maturityValue;
	}
	
	
	private Component getSystemTextField() {
		if (this.systemName == null) {
			this.systemName = new JTextField();
			this.systemName.setBounds(110, 30, 154, 24);
		}
		return this.systemName;
	}

	// Test
	public static void main(final String... argsss) throws ConnectException {
		final String args[] = { "-env", "dev4-local", "-user", "nav_it_sup_tec", "-password", "upgrade" };
		ConnectionUtil.connect(args, "KGR_Collateral_MarginCallReportTemplatePanel");
		final JFrame frame = new JFrame();
		frame.setTitle("KGR_Collateral_MarginCallReportTemplatePanel");
		frame.setContentPane(new KGR_Collateral_MarginCallReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1273, 307));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
