/*
 *
 * Copyright (c) 2011 Kaupthing Bank
 * Borgart?n 19, IS-105 Reykjavik, Iceland
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantComboBoxPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;
import calypsox.tk.report.SantConcentrationReportTemplate;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.User;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

public class SantConcentrationReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final static String LOG_CATEGORY = "SantConcentrationReport";
	public final static String MOVEMENT_BALANCE = "Balance";
	public final static String MOVEMENT_DIRTY_VALUE = "Balance_DirtyValue";
	public final static String MOVEMENT_CLEAN_VALUE = "Balance_CleanValue";

	public final static String GLOBAL_POSITION = "Global Position";
	public final static String TOTAL_ISSUED = "Total Issued";

	protected ReportTemplate reportTemplate;

	protected SantChooseButtonPanel bookPanel;
	protected Collection<String> booksCollection;

	protected SantProcessDatePanel datePanel;
	protected SantComboBoxPanel<String, String> movementType;

	protected SantComboBoxPanel<String, String> filter1;
	protected SantComboBoxPanel<String, String> filter2;
	protected SantComboBoxPanel<String, String> filter3;
	protected SantComboBoxPanel<String, String> filter4;
	protected SantComboBoxPanel<String, String> filter5;

	protected JTextField posPercentage1;
	protected JTextField posPercentage2;
	protected JTextField posPercentage3;
	protected JTextField posPercentage4;
	protected JTextField posPercentage5;

	protected JLabel comparisonlabel1;
	protected JLabel comparisonlabel2;
	protected JLabel comparisonlabel3;
	protected JLabel comparisonlabel4;
	protected JLabel comparisonlabel5;

	protected SantComboBoxPanel<String, String> globalOrTotalCombo1;
	protected SantComboBoxPanel<String, String> globalOrTotalCombo2;
	protected SantComboBoxPanel<String, String> globalOrTotalCombo3;
	protected SantComboBoxPanel<String, String> globalOrTotalCombo4;
	protected SantComboBoxPanel<String, String> globalOrTotalCombo5;

	protected JTextField criteria1;
	protected JTextField criteria2;
	protected JTextField criteria3;
	protected JTextField criteria4;
	protected JTextField criteria5;

	public SantConcentrationReportTemplatePanel() {
		init();
		this.reportTemplate = null;
	}

	@SuppressWarnings("unchecked")
	public Collection<String> getBookNames() {

		Vector<String> bookNames;
		try {
			bookNames = DSConnection.getDefault().getRemoteReferenceData().getBookNames();
			Collections.sort(bookNames);
			return bookNames;
		} catch (RemoteException e) {
			Log.error(this, e); //sonar
		}
		return new ArrayList<String>();
	}

	private void init() {

		setLayout(null);
		setSize(new Dimension(1173, 307));

		// Row 1
		this.bookPanel = new SantChooseButtonPanel("Portfolio ", getBookNames());
		this.bookPanel.setBounds(10, 10, 280, 24);
		add(this.bookPanel);

		this.movementType = new SantComboBoxPanel<String, String>("Movement Type", getMovementTypes());
		this.movementType.setBounds(330, 10, 320, 24);
		add(this.movementType);

		this.datePanel = new SantProcessDatePanel("Date");
		this.datePanel.customInitDomains(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);
		this.datePanel.removeDateLabel();
		this.datePanel.setBounds(700, 10, 300, 40);
		add(this.datePanel);

		// Row 2 Onwards
		// Filter Row 1
		this.filter1 = new SantComboBoxPanel<String, String>("1. SD Filter", getPositionCustomFilters());
		this.filter1.setBounds(10, 50, 280, 24);
		add(this.filter1);

		this.comparisonlabel1 = new JLabel("Must be smaller or equal to");
		this.comparisonlabel1.setBounds(300, 50, 200, 24);
		add(this.comparisonlabel1);
		this.posPercentage1 = new JTextField();
		this.posPercentage1.setBounds(475, 50, 40, 24);
		add(this.posPercentage1);
		this.globalOrTotalCombo1 = new SantComboBoxPanel<String, String>(" % of ", getPercentageOrTotalIssuedChoices());
		this.globalOrTotalCombo1.setBounds(515, 50, 220, 24);
		add(this.globalOrTotalCombo1);
		this.criteria1 = new JTextField("Criteria1");
		this.criteria1.setBounds(750, 50, 80, 24);
		add(this.criteria1);

		// Filter Row 2
		this.filter2 = new SantComboBoxPanel<String, String>("2. SD Filter", getPositionCustomFilters());
		this.filter2.setBounds(10, 80, 280, 24);
		add(this.filter2);
		this.comparisonlabel2 = new JLabel("Must be smaller or equal to");
		this.comparisonlabel2.setBounds(300, 80, 200, 24);
		add(this.comparisonlabel2);
		this.posPercentage2 = new JTextField();
		this.posPercentage2.setBounds(475, 80, 40, 24);
		add(this.posPercentage2);
		this.globalOrTotalCombo2 = new SantComboBoxPanel<String, String>(" % of ", getPercentageOrTotalIssuedChoices());
		this.globalOrTotalCombo2.setBounds(515, 80, 220, 24);
		add(this.globalOrTotalCombo2);
		this.criteria2 = new JTextField("Criteria2");
		this.criteria2.setBounds(750, 80, 80, 24);
		add(this.criteria2);

		// Filter Row 3
		this.filter3 = new SantComboBoxPanel<String, String>("3. SD Filter", getPositionCustomFilters());
		this.filter3.setBounds(10, 110, 280, 24);
		add(this.filter3);
		this.comparisonlabel3 = new JLabel("Must be smaller or equal to");
		this.comparisonlabel3.setBounds(300, 110, 200, 24);
		add(this.comparisonlabel3);
		this.posPercentage3 = new JTextField();
		this.posPercentage3.setBounds(475, 110, 40, 24);
		add(this.posPercentage3);
		this.globalOrTotalCombo3 = new SantComboBoxPanel<String, String>(" % of ", getPercentageOrTotalIssuedChoices());
		this.globalOrTotalCombo3.setBounds(515, 110, 220, 24);
		add(this.globalOrTotalCombo3);
		this.criteria3 = new JTextField("Criteria3");
		this.criteria3.setBounds(750, 110, 80, 24);
		add(this.criteria3);

		// Filter Row 4
		this.filter4 = new SantComboBoxPanel<String, String>("4. SD Filter", getPositionCustomFilters());
		this.filter4.setBounds(10, 140, 280, 24);
		add(this.filter4);
		this.comparisonlabel4 = new JLabel("Must be smaller or equal to");
		this.comparisonlabel4.setBounds(300, 140, 200, 24);
		add(this.comparisonlabel4);
		this.posPercentage4 = new JTextField();
		this.posPercentage4.setBounds(475, 140, 40, 24);
		add(this.posPercentage4);
		this.globalOrTotalCombo4 = new SantComboBoxPanel<String, String>(" % of ", getPercentageOrTotalIssuedChoices());
		this.globalOrTotalCombo4.setBounds(515, 140, 220, 24);
		add(this.globalOrTotalCombo4);
		this.criteria4 = new JTextField("Criteria4");
		this.criteria4.setBounds(750, 140, 80, 24);
		add(this.criteria4);

		// Filter Row 5
		this.filter5 = new SantComboBoxPanel<String, String>("5. SD Filter", getPositionCustomFilters());
		this.filter5.setBounds(10, 170, 280, 24);
		add(this.filter5);
		this.comparisonlabel5 = new JLabel("Must be smaller or equal to");
		this.comparisonlabel5.setBounds(300, 170, 200, 24);
		add(this.comparisonlabel5);
		this.posPercentage5 = new JTextField();
		this.posPercentage5.setBounds(475, 170, 40, 24);
		add(this.posPercentage5);
		this.globalOrTotalCombo5 = new SantComboBoxPanel<String, String>(" % of ", getPercentageOrTotalIssuedChoices());
		this.globalOrTotalCombo5.setBounds(515, 170, 220, 24);
		add(this.globalOrTotalCombo5);
		this.criteria5 = new JTextField("Criteria5");
		this.criteria5.setBounds(750, 170, 80, 24);
		add(this.criteria5);

	}

	private Collection<String> getMovementTypes() {
		ArrayList<String> movements = new ArrayList<String>();
		movements.add(MOVEMENT_BALANCE);
		movements.add(MOVEMENT_DIRTY_VALUE);
		movements.add(MOVEMENT_CLEAN_VALUE);
		return movements;
	}

	@SuppressWarnings("unchecked")
	private Collection<String> getPositionCustomFilters() {
		Vector<String> v = AccessUtil.getAllNames(User.STATIC_DATA_FILTER);
		v.add(0, "");
		ArrayList<String> filters = new ArrayList<String>(v);
		return filters;
	}

	private Collection<String> getPercentageOrTotalIssuedChoices() {
		ArrayList<String> list = new ArrayList<String>();
		list.add(GLOBAL_POSITION);
		list.add(TOTAL_ISSUED);
		return list;
	}

	@SuppressWarnings("unused")
	@Override
	public ReportTemplate getTemplate() {

		String value = this.bookPanel.getValue();
		this.reportTemplate.put(BOSecurityPositionReportTemplate.BOOK_LIST, this.bookPanel.getValue());
		this.reportTemplate.put(BOSecurityPositionReportTemplate.MOVE, this.movementType.getValue());
		this.datePanel.read(this.reportTemplate);

		this.reportTemplate.put(SantConcentrationReportTemplate.FILTER1, this.filter1.getValue());
		this.reportTemplate.put(SantConcentrationReportTemplate.FILTER2, this.filter2.getValue());
		this.reportTemplate.put(SantConcentrationReportTemplate.FILTER3, this.filter3.getValue());
		this.reportTemplate.put(SantConcentrationReportTemplate.FILTER4, this.filter4.getValue());
		this.reportTemplate.put(SantConcentrationReportTemplate.FILTER5, this.filter5.getValue());

		this.reportTemplate.put(SantConcentrationReportTemplate.PERCENTAGE1, this.posPercentage1.getText());
		this.reportTemplate.put(SantConcentrationReportTemplate.PERCENTAGE2, this.posPercentage2.getText());
		this.reportTemplate.put(SantConcentrationReportTemplate.PERCENTAGE3, this.posPercentage3.getText());
		this.reportTemplate.put(SantConcentrationReportTemplate.PERCENTAGE4, this.posPercentage4.getText());
		this.reportTemplate.put(SantConcentrationReportTemplate.PERCENTAGE5, this.posPercentage5.getText());

		this.reportTemplate.put(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED1,
				this.globalOrTotalCombo1.getValue());
		this.reportTemplate.put(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED2,
				this.globalOrTotalCombo2.getValue());
		this.reportTemplate.put(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED3,
				this.globalOrTotalCombo3.getValue());
		this.reportTemplate.put(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED4,
				this.globalOrTotalCombo4.getValue());
		this.reportTemplate.put(SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED5,
				this.globalOrTotalCombo5.getValue());

		this.reportTemplate.put(SantConcentrationReportTemplate.CRITERIA1, this.criteria1.getText());
		this.reportTemplate.put(SantConcentrationReportTemplate.CRITERIA2, this.criteria2.getText());
		this.reportTemplate.put(SantConcentrationReportTemplate.CRITERIA3, this.criteria3.getText());
		this.reportTemplate.put(SantConcentrationReportTemplate.CRITERIA4, this.criteria4.getText());
		this.reportTemplate.put(SantConcentrationReportTemplate.CRITERIA5, this.criteria5.getText());

		return this.reportTemplate;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		this.reportTemplate = template;

		this.bookPanel.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.BOOK_LIST);
		this.movementType.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.MOVE);
		this.datePanel.write(this.reportTemplate);

		this.filter1.setValue(this.reportTemplate, SantConcentrationReportTemplate.FILTER1);
		this.filter2.setValue(this.reportTemplate, SantConcentrationReportTemplate.FILTER2);
		this.filter3.setValue(this.reportTemplate, SantConcentrationReportTemplate.FILTER3);
		this.filter4.setValue(this.reportTemplate, SantConcentrationReportTemplate.FILTER4);
		this.filter5.setValue(this.reportTemplate, SantConcentrationReportTemplate.FILTER5);

		String s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.PERCENTAGE1);
		if (!Util.isEmpty(s)) {
			this.posPercentage1.setText(s);
		}
		s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.PERCENTAGE2);
		if (!Util.isEmpty(s)) {
			this.posPercentage2.setText(s);
		}
		s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.PERCENTAGE3);
		if (!Util.isEmpty(s)) {
			this.posPercentage3.setText(s);
		}
		s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.PERCENTAGE4);
		if (!Util.isEmpty(s)) {
			this.posPercentage4.setText(s);
		}
		s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.PERCENTAGE5);
		if (!Util.isEmpty(s)) {
			this.posPercentage5.setText(s);
		}

		this.globalOrTotalCombo1.setValue(this.reportTemplate,
				SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED1);
		this.globalOrTotalCombo2.setValue(this.reportTemplate,
				SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED2);
		this.globalOrTotalCombo3.setValue(this.reportTemplate,
				SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED3);
		this.globalOrTotalCombo4.setValue(this.reportTemplate,
				SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED4);
		this.globalOrTotalCombo5.setValue(this.reportTemplate,
				SantConcentrationReportTemplate.GLOBALPOS_OR_TOTALISSUED5);

		s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.CRITERIA1);
		if (!Util.isEmpty(s)) {
			this.criteria1.setText(s);
		}
		s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.CRITERIA2);
		if (!Util.isEmpty(s)) {
			this.criteria2.setText(s);
		}
		s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.CRITERIA3);
		if (!Util.isEmpty(s)) {
			this.criteria3.setText(s);
		}
		s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.CRITERIA4);
		if (!Util.isEmpty(s)) {
			this.criteria4.setText(s);
		}
		s = (String) this.reportTemplate.get(SantConcentrationReportTemplate.CRITERIA5);
		if (!Util.isEmpty(s)) {
			this.criteria5.setText(s);
		}

	}

	@SuppressWarnings("unused")
	private Object getKey(final String value, final Map<String, String> map) {
		for (final Entry<String, String> entry : map.entrySet()) {
			if (entry.getValue() == null) {
				return null;
			}
			if (entry.getValue().equals(value)) {
				String key = entry.getKey();
				return key;
			}
		}
		return null;
	}

	public static void main(String... args) throws ConnectException {
		ConnectionUtil.connect(args, "MainEntry");
		JFrame frame = new JFrame();
		frame.setContentPane(new SantConcentrationReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}

}
