package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;


import calypsox.apps.reporting.util.SantGenericReportTemplatePanel;
import calypsox.apps.reporting.util.ValueComparator;
import calypsox.apps.reporting.util.control.SantChooseButtonPanel;
import calypsox.tk.report.SantCashFlowsReportTemplate;

import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

public class SantCashFlowsReportTemplatePanel extends SantGenericReportTemplatePanel {

	private static final long serialVersionUID = -2509019658734835894L;

	private SantChooseButtonPanel callAccountPanel;
	private Map<Integer, String> callAccountMap;

	public SantCashFlowsReportTemplatePanel() {
		setPanelVisibility();
	}

	@Override
	protected void buildControlsPanel() {
		super.buildControlsPanel();
	}

	@Override
	protected Border getMasterPanelBorder() {
		final TitledBorder titledBorder = BorderFactory.createTitledBorder("Sant Cash Flows");
		titledBorder.setTitleColor(Color.BLUE);
		return titledBorder;
	}

	private void setPanelVisibility() {
		hideAllPanels();
		this.processStartEndDatePanel.setVisible(true);
		this.poDealPanel.setVisible(true);
		this.poAgrPanel.setVisible(true);
		this.portfolioPanel.setVisible(true);
		this.cptyPanel.setVisible(true);
		this.agreementNamePanel.setVisible(true);
		this.agreementTypePanel.setVisible(true);
	}

	@Override
	protected JPanel getColumn1Panel() {
		final JPanel column1panel = super.getColumn1Panel();
		column1panel.removeAll();
		column1panel.add(this.poDealPanel);
		column1panel.add(this.poAgrPanel);
		column1panel.add(this.cptyPanel);

		return column1panel;
	}

	@Override
	protected JPanel getColumn3Panel() {
		final JPanel column3panel = super.getColumn3Panel();
		column3panel.removeAll();
		column3panel.add(this.portfolioPanel);

		this.callAccountMap = getAllAccounts();
		final ValueComparator bvc = new ValueComparator(this.callAccountMap);
		final Map<Integer, String> sortedMap = new TreeMap<Integer, String>(bvc);
		sortedMap.putAll(this.callAccountMap);
		this.callAccountPanel = new SantChooseButtonPanel("Call Account", sortedMap.values());
		column3panel.add(this.callAccountPanel);

		return column3panel;
	}

	@SuppressWarnings("rawtypes")
	private Map<Integer, String> getAllAccounts() {
		this.callAccountMap = new HashMap<Integer, String>();

		try {
			final Vector accounts = DSConnection.getDefault().getRemoteAccounting().getAccounts(true);

			for (final Object account : accounts) {
				if (account instanceof Account) {
					if (((Account) account).getCallAccountB()) {
						// GSM: Call account name fix
						this.callAccountMap.put(((Account) account).getId(), ((Account) account).getExternalName());
						// ((Account) account).getName()); old
					}
				}
			}
		} catch (final RemoteException e) {
			final StringBuffer message = new StringBuffer();
			message.append("Couldn't load the accounts ");
			message.append(e.getCause());
			Log.error(this, message.toString());
			Log.error(this, e); //sonar
		}
		return this.callAccountMap;
	}

	@Override
	public ReportTemplate getTemplate() {
		super.getTemplate();

		this.reportTemplate.put(SantCashFlowsReportTemplate.CALL_ACCOUNT, this.callAccountPanel.getValue());

		return this.reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate template) {
		super.setTemplate(template);
		this.callAccountPanel.setValue(this.reportTemplate, SantCashFlowsReportTemplate.CALL_ACCOUNT);
	}

	public static void main(final String... args) throws ConnectException {
		ConnectionUtil.connect(args, "SantCashFlowsReportTemplate");
		final JFrame frame = new JFrame();
		frame.setTitle("SantCashFlowsReportTemplate");
		frame.setContentPane(new SantCashFlowsReportTemplatePanel());
		frame.setVisible(true);
		frame.setSize(new Dimension(1273, 307));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
