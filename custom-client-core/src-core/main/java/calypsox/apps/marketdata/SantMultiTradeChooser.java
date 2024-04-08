/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.marketdata;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportPanelListener;
import com.calypso.apps.reporting.ReportUtil;
import com.calypso.apps.trading.ShowTrade;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

public class SantMultiTradeChooser extends JPanel implements ReportPanelListener {

	private static final long serialVersionUID = 1L;

	public SantMultiTradeChooser() {
	}

	/**
	 * Pick a trade from a list of trades passed.
	 */
	@SuppressWarnings({ "unused", "deprecation" })
	public Trade pickATrade(TradeArray trades) {
		if ((trades == null) || (trades.size() == 0)) {
			return null;
		}
		if (trades.size() == 1) {
			return trades.get(0);
		}
		if (this._tradeReportDialog == null) {
			this._tradeReportDialog = setupDialog();
		}
		DefaultReportOutput defaultReportOutput = this._reportPanel.getOutput();
		TradeReport report = new TradeReport();
		DefaultReportOutput output = new DefaultReportOutput(report);
		ReportRow[] rows = new ReportRow[trades.size()];
		for (int i = 0, j = rows.length; i < j; i++) {
			rows[i] = new ReportRow(trades.get(i));
		}
		this._reportPanel.addReportPanelListener(this);
		defaultReportOutput.clear();
		defaultReportOutput.setRows(rows);
		this._reportPanel.refresh();

		this._selectedTrade = null;
		this._tradeReportDialog.show();
		return this._selectedTrade;
	}

	/**
	 * Setup a selector dialog with a table to show all the trades. The Table is consisted of Trade Report.
	 * 
	 * @return - JDialog the setup dialog.
	 */
	@SuppressWarnings("deprecation")
	private JDialog setupDialog() {
		JDialog tradeReportDialog = null;
		this._reportPanel = new ReportPanel("Trade", null, true);
		this._reportPanel.setVisible(true);
		this._reportPanel.setShowObjectOnDoubleClick(false);
		//AAP MIG 14.4 JIRA-145
		this._reportPanel.initTable();
		this._reportPanel.getTableModelWithFocus().getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tradeReportDialog = new JDialog((Frame) this._tradeWindow, "Select Trade", true);
		tradeReportDialog.setLocationRelativeTo(tradeReportDialog.getParent());
		//AppUtil.centerDialogToParent(tradeReportDialog);
		tradeReportDialog.setSize(new Dimension(700, 400));
		tradeReportDialog.getContentPane().setLayout(new BorderLayout());
		tradeReportDialog.getContentPane().add(this._reportPanel, BorderLayout.CENTER);
		// setup and add the button panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SantMultiTradeChooser.this._tradeReportDialog.hide();
				SantMultiTradeChooser.this._selectedTrade = null;
				saveReportConfiguration();
			}
		});
		JButton applyButton = new JButton("Apply");
		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SantMultiTradeChooser.this._tradeReportDialog.dispose();
				ReportRow[] rows = SantMultiTradeChooser.this._reportPanel.getSelectedReportRows();
				if ((rows != null) && (rows.length > 0)) {
					SantMultiTradeChooser.this._selectedTrade = (Trade) rows[0].getProperty(ReportRow.DEFAULT);
				}
				saveReportConfiguration();
			}
		});
		buttonPanel.add(cancelButton);
		buttonPanel.add(applyButton);
		tradeReportDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		tradeReportDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		tradeReportDialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				saveReportConfiguration();
			}
		});
		initReportPanel();
		//AppUtil.centerDialog(tradeReportDialog);
		tradeReportDialog.setLocationRelativeTo(tradeReportDialog.getParent());
		return tradeReportDialog;
	}

	/**
	 * Initialized the TradeReportPanel which is the table for the the chooser dialog.
	 */
	private void initReportPanel() {
		String templateName = getTemplateName();
		ReportTemplate reportTemplate = this._reportPanel.loadTemplate(templateName);
		if (reportTemplate != null) {
			try {
				this._originalTemplate = (ReportTemplate) reportTemplate.clone();
			} catch (Exception e) {
				Log.error(Log.GUI, "Unable to clone report tempalte: " + e);
			}
		} else {
			this._originalTemplate = null;
		}
		if (reportTemplate == null) {
			reportTemplate = this._reportPanel.getTemplate();
			reportTemplate.setTemplateName(templateName);
			reportTemplate.setHidden(true);
		}
		this._reportPanel.setTemplate(reportTemplate);
	}

	/**
	 * Saves the Trade Report column config. The internal table from which a user chooses a trade is a Trade Report.
	 */
	private void saveReportConfiguration() {
		ReportTemplate currentTemplate = this._reportPanel.getTemplate();
		if (!ReportUtil.columnsNamesAreTheSame(currentTemplate, this._originalTemplate)) {
			String reportType = this._reportPanel.getReport().getType();
			if (((AccessUtil.isAuthorized(AccessUtil.CREATEPRIVATEREPORTTEMPLATES) && (currentTemplate.getId() == 0)) || (AccessUtil
					.isAuthorized(AccessUtil.MODIFYPRIVATEREPORTTEMPLATES) && (currentTemplate.getId() != 0)))
					&& (!DSConnection.getDefault().isReadOnly())) {
				ReportUtil.saveTemplate(currentTemplate, reportType, this, false, false, true);
			} else {
				Log.warn(Log.GUI, "Due to access permissions or read only database"
						+ ": Unable to save Trade Retriever Chooser report column configuration");
			}
		}
	}

	// ReportPanelListener - begin
	@Override
	public boolean isValidLoad(ReportPanel panel) {
		return true;
	}

	@Override
	public void callBeforeLoad(ReportPanel panel) {
	}

	@Override
	public void callAfterDisplay(ReportPanel panel) {
	}

	@Override
	public void handleRowSelection(Object obj) {
		this._selectedTrade = (Trade) obj;
		this._tradeReportDialog.dispose();
	}

	@SuppressWarnings("rawtypes")
	private String getTemplateName() {
		String templateName = TEMPLATENAME;
		Vector domainValues;
		try {
			domainValues = DSConnection.getDefault().getRemoteReferenceData().getDomainValues(TEMPLATE_DOMAIN_NAME);
			if (!Util.isEmpty(domainValues)) {
				templateName = (String) domainValues.get(0);
			}
		} catch (RemoteException e) {
			Log.error(SantMultiTradeChooser.class, "Error getting values for domain - " + TEMPLATE_DOMAIN_NAME);
			Log.error(this, e); //sonar
		}

		return templateName;
	}

	protected ShowTrade _tradeWindow = null;

	protected boolean _listenersSet = false;
	protected Trade _selectedTrade = null;
	protected JDialog _tradeReportDialog = null;
	protected ReportPanel _reportPanel = null;
	protected ReportTemplate _originalTemplate = null;

	private static String TEMPLATENAME = "Overun_MultiTradeChooserTemplate";
	private static String TEMPLATE_DOMAIN_NAME = "Overun.MultiTradeChooserTemplate";

}