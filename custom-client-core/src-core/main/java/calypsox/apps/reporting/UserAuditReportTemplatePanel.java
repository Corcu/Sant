/*
 *
 * Copyright (c) 2011 Kaupthing Bank
 * Borgart?n 19, IS-105 Reykjavik, Iceland
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;



import calypsox.apps.reporting.util.control.SantStartEndDatePanel;
import calypsox.tk.report.UserAuditReportTemplate;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoComboBox;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

public class UserAuditReportTemplatePanel extends ReportTemplatePanel {

	private static final long serialVersionUID = -4536379902501882512L;

	// CAL_COLLAT_REPORT_0114
	private final SantStartEndDatePanel processStartEndDatePanel;

	protected JPanel userNamePanel;
	protected JLabel userNamelabel;
	private final JTextField userNameText;

	protected JPanel userGroupPanel;
	protected JLabel usrGroupLabel;
	protected CalypsoComboBox userGroupCombo;
	protected ReportTemplate _reportTemplate;

	public UserAuditReportTemplatePanel() {
		setLayout(new GridLayout(3, 1));
		setSize(new Dimension(500, 130));

		// CAL_COLLAT_REPORT_0114
		this.processStartEndDatePanel = new SantStartEndDatePanel("Process");

		this.userNamelabel = new JLabel();
		this.userNamelabel.setText("User Name");
		this.userNamelabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

		this.userNameText = new JTextField();
		this.userNameText.setPreferredSize(new Dimension(300, 30));

		this.userNamePanel = new JPanel();
		this.userNamePanel.add(this.userNamelabel);
		this.userNamePanel.add(this.userNameText);

		this.usrGroupLabel = new JLabel();
		this.usrGroupLabel.setText("User Group");
		this.usrGroupLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

		this.userGroupCombo = new CalypsoComboBox();

		this.userGroupPanel = new JPanel();
		this.userGroupPanel.add(this.usrGroupLabel);
		this.userGroupPanel.add(this.userGroupCombo);

		this.processStartEndDatePanel.setVisible(true);
		add(this.processStartEndDatePanel);

		add(this.userNamePanel);

		add(this.userGroupPanel);

		init();
		this._reportTemplate = null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void init() {
		// Initialise User groups
		Vector grpNames = new Vector();
		try {
			grpNames = DSConnection.getDefault().getRemoteAccess().getGroupNames();
		} catch (final RemoteException e) {
			Log.error(this, e); //sonar
		}

		grpNames.insertElementAt("", 0);
		AppUtil.set(this.userGroupCombo, grpNames);

	}

	@Override
	public ReportTemplate getTemplate() {
		this.processStartEndDatePanel.read(this._reportTemplate);

		this._reportTemplate.put(UserAuditReportTemplate.USER_NAME, this.userNameText.getText());
		this._reportTemplate.put(UserAuditReportTemplate.USER_GROUP, this.userGroupCombo.getSelectedItem());
		return this._reportTemplate;
	}

	@Override
	public void setTemplate(final ReportTemplate reporttemplate) {
		this.processStartEndDatePanel.setTemplate(reporttemplate);
		this.processStartEndDatePanel.write(reporttemplate);

		this.userNameText.setText((String) reporttemplate.get(UserAuditReportTemplate.USER_NAME));
		this.userGroupCombo.setSelectedItem(reporttemplate.get(UserAuditReportTemplate.USER_GROUP));

		this._reportTemplate = reporttemplate;
	}

	// public static void main(String... args) throws ConnectException {
	// DSConnection ds = ConnectionUtil.connect(args, "Temp");
	// // DSConnection.setDefault(ds);
	// JFrame frame = new JFrame();
	// frame.setContentPane(new UserAuditReportTemplatePanel());
	// frame.setVisible(true);
	// frame.setPreferredSize(new Dimension(825, 225));
	// }

}
