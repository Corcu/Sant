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

import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.apps.reporting.util.control.SantProcessDatePanel;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

public class SantInventoryMissingSecurityQuotesReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = 1L;

	public final static String MOVEMENT_BALANCE = "Balance";

	protected ReportTemplate reportTemplate;

	protected SantChooseButtonPanel bookPanel;
	protected Collection<String> booksCollection;

	protected SantProcessDatePanel datePanel;

	public SantInventoryMissingSecurityQuotesReportTemplatePanel() {
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
		this.bookPanel.setBounds(10, 30, 280, 24);
		add(this.bookPanel);

		this.datePanel = new SantProcessDatePanel("Date");
		this.datePanel.customInitDomains(TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
				TradeReportTemplate.END_TENOR);
		this.datePanel.removeDateLabel();
		this.datePanel.setBounds(700, 30, 300, 40);
		add(this.datePanel);
	}

	@Override
	public ReportTemplate getTemplate() {
		this.reportTemplate.put(BOSecurityPositionReportTemplate.BOOK_LIST, this.bookPanel.getValue());
		this.datePanel.read(this.reportTemplate);
		return this.reportTemplate;
	}

	@Override
	public void setTemplate(ReportTemplate template) {
		this.reportTemplate = template;
		this.bookPanel.setValue(this.reportTemplate, BOSecurityPositionReportTemplate.BOOK_LIST);
		this.datePanel.write(this.reportTemplate);
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
		frame.setContentPane(new SantInventoryMissingSecurityQuotesReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1173, 307));
	}

}
