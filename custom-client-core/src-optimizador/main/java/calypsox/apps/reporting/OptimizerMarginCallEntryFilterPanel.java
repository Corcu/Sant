package calypsox.apps.reporting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.PreviousPositionDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.ESStarter;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSSubscriber;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ConnectException;

import calypsox.apps.feed.SantMarginCallEntryFeedOptimizerWindow;
import calypsox.tk.event.PSEventSendOptimizerMarginCall;
import calypsox.tk.event.PSEventSendOptimizerMarginCallStatus;
import calypsox.tk.report.OptimizerMarginCallEntryReportStyle;
import calypsox.tk.util.OptimizerMarginCallEntryConstants;
import calypsox.tk.util.SantCollateralOptimConstants;

public class OptimizerMarginCallEntryFilterPanel extends JPanel implements
		OptimizerMarginCallEntryConstants {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7024823440214405396L;

	private static final String OPTIMIZATION_TITLE = "Optimization";
	private static final String OPTIMIZE_CONTRACTS = "Optimize Contracts";

	private static final String ACTION_SELECT_ALL = "ACTION_SELECT_ALL";
	private static final String ACTION_UNSELECT_ALL = "ACTION_UNSELECT_ALL";
	private static final String ACTION_OPTIMIZE_CONTRACTS = "ACTION_OPTIMIZE_CONTRACTS";

	private static final String SENDING_ENTRIES_TO_OPTIMIZER = "Sending entries to Optimizer...";

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

	public OptimizerMarginCallEntryFilterPanel(
			ReportTemplatePanel reportTemplatePanel) {
		super();
	}

	private DefaultActionListener defaultActionListener = null;
	protected ReportTemplatePanel reportTemplatePanel = null;

	public ReportTemplatePanel getReportTemplatePanel() {
		return reportTemplatePanel;
	}

	public void setReportTemplatePanel(ReportTemplatePanel reportTemplatePanel) {
		this.reportTemplatePanel = reportTemplatePanel;
	}

	private class SantMarginCallEntryFeedOptimizerListener implements
			PSSubscriber {

		public SantMarginCallEntryFeedOptimizerListener() {
		}

		@Override
		public void newEvent(PSEvent event) {
			if (!(event instanceof PSEventSendOptimizerMarginCallStatus)) {
				return;
			}
			PSEventSendOptimizerMarginCallStatus sendOptimizerMarginCallEventStatus = ((PSEventSendOptimizerMarginCallStatus) event);
			if (!Util.isEmpty(sendOptimizerMarginCallEventStatus.getFileName())) {
				infoTextArea.setForeground(Color.red);
				addMessageInfo("Message sent to Optimizer ["
						+ sendOptimizerMarginCallEventStatus.getFileName()
						+ "] ("
						+ sendOptimizerMarginCallEventStatus.getNbRecords()
						+ " entries).");
			}
		}

		@Override
		public void onDisconnect() {
			infoTextArea.setForeground(Color.red);
			addMessageInfo("Disconnected from EventServer");
			Log.error(SantMarginCallEntryFeedOptimizerWindow.class.getName(),
					"Disconnected from EventServer");
		}
	}

	private class DefaultActionListener implements ActionListener {

		private DefaultActionListener() {
			super();
		}

		@SuppressWarnings("deprecation")
		public void actionPerformed(ActionEvent e) {
			String action = e.getActionCommand();
			if (ACTION_SELECT_ALL.equals(action)) {
				clearOptimizeAttribute();
				if (selectAllCheckBox.getSelectedObjects() != null) {
					reportTemplatePanel.getTemplate().put(OPTIMIZE_SELECT_ALL,
							SELECT_TRUE);
					reportTemplatePanel.getTemplate().put(
							OPTIMIZE_UNSELECT_ALL, "");
					unSelectAllCheckBox.setSelected(false);
				} else {
					reportTemplatePanel.getTemplate().put(OPTIMIZE_SELECT_ALL,
							"");
				}
				// call ReportStyle getColumnValue
				reportTemplatePanel.getReportWindow().getReportPanel()
						.refresh();
			} else if (ACTION_UNSELECT_ALL.equals(action)) {
				if (unSelectAllCheckBox.getSelectedObjects() != null) {
					reportTemplatePanel.getTemplate().put(
							OPTIMIZE_UNSELECT_ALL, SELECT_TRUE);
					reportTemplatePanel.getTemplate().put(OPTIMIZE_SELECT_ALL,
							"");
					selectAllCheckBox.setSelected(false);
				} else {
					reportTemplatePanel.getTemplate().put(
							OPTIMIZE_UNSELECT_ALL, "");
				}
				// call ReportStyle getColumnValue
				reportTemplatePanel.getReportWindow().getReportPanel()
						.refresh();
			} else if (ACTION_OPTIMIZE_CONTRACTS.equals(action)) {
				JDatetime currentDate = new JDatetime();
				List<MarginCallEntryDTO> marginCallEntriesToSend = new ArrayList<MarginCallEntryDTO>();
				List<MarginCallEntryDTO> marginCallEntriesDoNotSend = new ArrayList<MarginCallEntryDTO>();

				DefaultReportOutput reportOutput = reportTemplatePanel
						.getReportWindow().getReportPanel().getOutput();							
				if (reportOutput != null && reportOutput.getRows() != null) {
					ReportRow[] rows = reportOutput.getRows();
					JTable table = reportTemplatePanel.getReportWindow()
							.getReportPanel().getTableModel().getTable();	
					for (int i = 0; i < reportTemplatePanel.getReportWindow()
							.getReportPanel().getTableModel().getTable().getRowCount(); i++) {
				
						int marginCallEntryId =(Integer) table
								.getValueAt(
										i,
										OptimizerMarginCallEntryReportTemplatePanel.findColumnIndex(
												reportTemplatePanel
														.getReportWindow(),
												ID_MARGIN_CALL));
						MarginCallEntryDTO marginCallEntry = getMarginCallEntryFromId(marginCallEntryId, rows);
						if (marginCallEntry != null) {
							if ((Boolean) table
									.getValueAt(
											i,
											OptimizerMarginCallEntryReportTemplatePanel.findColumnIndex(
													reportTemplatePanel
															.getReportWindow(),
													OptimizerMarginCallEntryReportStyle.OPTIMIZE_FIELD))) {
								marginCallEntriesToSend.add(marginCallEntry);
							} else if (marginCallEntry.getProcessDatetime() != null
									&& sdf.format(currentDate).equals(
											sdf.format(marginCallEntry
													.getProcessDatetime()))
									&& SantCollateralOptimConstants.OPTIMIZER_TO_BE_SENT_STATUS_VALUE
											.equals(marginCallEntry
													.getAttribute(SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS))) {
								// do not send these entries to optimizer
								marginCallEntriesDoNotSend.add(marginCallEntry);
							}
						}
					}
				}

				if (!Util.isEmpty(marginCallEntriesToSend)) {
					optimizeContracts(marginCallEntriesToSend,
							marginCallEntriesDoNotSend);
				} else {
					addMessageInfo("No contract selected - doing nothing.");
				}
			}
		}

		private MarginCallEntryDTO getMarginCallEntryFromId(
				int marginCallEntryId, ReportRow[] rows) {
			for (int i = 0; i < rows.length; i++) {
				MarginCallEntryDTO marginCallEntry = (MarginCallEntryDTO) rows[i].getProperty(DEFAULT_PROPERTY);
				if (marginCallEntry != null && marginCallEntry.getId() == marginCallEntryId) {
					return marginCallEntry;
				}
			}
			return null;
		}

		public void optimizeContracts(
				List<MarginCallEntryDTO> marginCallEntriesToSend,
				List<MarginCallEntryDTO> marginCallEntriesDoNotSend) {
			JDatetime currentDate = new JDatetime();
			infoTextArea.setText("");
			infoTextArea.setForeground(Color.black);
			boolean hasEntriesToSend = false;
			for (MarginCallEntryDTO mcEntryDTO : marginCallEntriesToSend) {
				if (mcEntryDTO.getProcessDatetime() != null
						&& !sdf.format(currentDate).equals(
								sdf.format(mcEntryDTO.getProcessDatetime()))) {
					addMessageInfo("Skipping entries with process date different from current date: "
							+ getContractName(mcEntryDTO)
							+ " ["
							+ sdf.format(mcEntryDTO.getProcessDatetime()) + "]");
					continue;
				}
				hasEntriesToSend = true;
				if (mcEntryDTO
						.getAttribute(SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS) == null
						|| "".equals(mcEntryDTO
								.getAttribute(SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS))) {
					// FIRST LOAD CURRENT MCE AS SOME FIELDS ARE NOT RETRIEVED
					// DURING CALYPSO REPORT LOAD
					mcEntryDTO = loadMarginCallEntry(mcEntryDTO);
					if (mcEntryDTO != null) {
						// add optimizer send status
						mcEntryDTO
								.addAttribute(
										SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS,
										SantCollateralOptimConstants.OPTIMIZER_TO_BE_SENT_STATUS_VALUE);
	
						updateMarginCallEntry(mcEntryDTO);
					}
				}
			}

			if (hasEntriesToSend) {
				for (MarginCallEntryDTO marginCallEntryDTO : marginCallEntriesDoNotSend) {
					// FIRST LOAD CURRENT MCE AS SOME FIELDS ARE NOT RETRIEVED
					// DURING CALYPSO REPORT LOAD
					marginCallEntryDTO = loadMarginCallEntry(marginCallEntryDTO);
					
					if (marginCallEntryDTO != null) {
						// unflag margincall entries previously flag to be sent to
						// Optimizer
						marginCallEntryDTO.addAttribute(
								SantCollateralOptimConstants.OPTIMIZER_SEND_STATUS,
								null);
						updateMarginCallEntry(marginCallEntryDTO);
					}
				}
			}

			if (eventListener == null) {
				Runnable stRun = new Runnable() {
					@SuppressWarnings({ "rawtypes", "unused" })
					public void run() {
						eventListener = new SantMarginCallEntryFeedOptimizerListener();
						// events we are interested in
						Class[] subscriptionList = new Class[] { PSEventSendOptimizerMarginCallStatus.class, };
						try {
							PSConnection ps = ESStarter.startConnection(
									eventListener, subscriptionList);
						} catch (ConnectException e) {
							Log.error(this, e);
							eventListener = null;
						}
					}
				};
				SwingUtilities.invokeLater(stRun);
			}

			if (hasEntriesToSend) {
				PSEventSendOptimizerMarginCall eventMC = new PSEventSendOptimizerMarginCall();
				try {
					DSConnection.getDefault().getRemoteTrade()
							.saveAndPublish(eventMC);
					infoTextArea.setForeground(Color.black);
					addMessageInfo(SENDING_ENTRIES_TO_OPTIMIZER);
				} catch (Exception e) {
					Log.error(
							SantMarginCallEntryFeedOptimizerWindow.class
									.getName()
									+ ": failed to publish PSEventSendOptimizerMarginCall event",
							e);
					addMessageInfo("Failed to publish PSEventSendOptimizerMarginCall event:"
							+ e);
				}
			} else {
				addMessageInfo("No entry updated - doing nothing");
			}
		}

		private MarginCallEntryDTO loadMarginCallEntry(
				MarginCallEntryDTO mcEntryDTO) {
			try {
				return ServiceRegistry
						.getDefault(DSConnection.getDefault())
						.getCollateralServer()
						.loadEntry(mcEntryDTO.getId());
			} catch (RemoteException e) {
				return null;
			}
		}

		private void updateMarginCallEntry(MarginCallEntryDTO mcEntryDTO) {
			String actionToApply = "UPDATE";
			// TODO: delete with upgrade 1.6.3
			if (Util.isEmpty(mcEntryDTO.getCashPositions())) {
				mcEntryDTO
						.setCashPosition(new PreviousPositionDTO<CashPositionDTO>());
			}

			int entryId;
			try {
				entryId = ServiceRegistry.getDefault(DSConnection.getDefault())
						.getCollateralServer()
						.save(mcEntryDTO, actionToApply, TimeZone.getDefault());

				Log.info(OptimizerMarginCallEntryFilterPanel.class
						.getName(),
						"Entry with id " + entryId
								+ " successfully saved for the contract "
								+ mcEntryDTO.getCollateralConfigId());
			} catch (RemoteException e) {
				Log.error(OptimizerMarginCallEntryFilterPanel.class.getName(),
						e);
				addMessageInfo(e.getMessage());
			}
		}

		private String getContractName(MarginCallEntryDTO entry) {
			CollateralConfig collateralConfig = CacheCollateralClient
					.getCollateralConfig(DSConnection.getDefault(),
							entry.getCollateralConfigId());
			if (collateralConfig != null) {
				return collateralConfig.getName();
			}
			return "";
		}
	}

	private void addMessageInfo(String message) {
		infoTextArea.append(message + "\n");
	}

	private void clearOptimizeAttribute() {
		DefaultReportOutput reportOutput = reportTemplatePanel
				.getReportWindow().getReportPanel().getOutput();
		if (reportOutput == null) {
			return;
		}
		ReportRow[] reportRow = reportOutput.getRows();
		if (reportRow == null) {
			return;
		}
		for (int i = 0; i < reportRow.length; i++) {
			if (reportRow[i] == null) {
				continue;
			}
			reportRow[i].setProperty(OPTIMIZE_PROPERTY, "");
		}
	}

	// panels
	protected JPanel infoPanel = new JPanel();
	protected JPanel selectPanel = new JPanel();
	protected JPanel optimizeContractsPanel = new JPanel();

	// Info fields
	protected JTextArea infoTextArea = new JTextArea();

	protected SantMarginCallEntryFeedOptimizerListener eventListener = null;

	// Select fields
	protected CalypsoCheckBox selectAllCheckBox = new CalypsoCheckBox();
	protected JLabel selectAllLabel = new JLabel();
	protected CalypsoCheckBox unSelectAllCheckBox = new CalypsoCheckBox();
	protected JLabel unSelectAllLabel = new JLabel();

	// OptimizeContracts fields
	protected JButton optimizeContractsButton = new JButton(OPTIMIZE_CONTRACTS);

	public OptimizerMarginCallEntryFilterPanel() {
		setBorder(new TitledBorder(new EtchedBorder(1, null, null),
				OPTIMIZATION_TITLE, 4, 2, null, null));
		setLayout(new GridBagLayout());

		initInfoPanel();
		initSelectPanel();
		initOptimizeContractPanel();
	}

	private void initInfoPanel() {
		GridLayout gridLayoutInfo = new GridLayout(1, 1, 5, 5);
		infoPanel.setLayout(gridLayoutInfo);

		/* begin first row */
		BoxLayout blInfoPanel = new BoxLayout(infoPanel, BoxLayout.X_AXIS);
		infoPanel.setLayout(blInfoPanel);

		// start infoLabel properties
		infoTextArea.setText("");
		infoTextArea.setEditable(false);
		infoTextArea.setEnabled(true);
		JScrollPane sp = new JScrollPane(infoTextArea);
		// end infoLabel properties
		infoPanel.add(sp);
		infoPanel.setBorder(BorderFactory.createEtchedBorder());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0.7;
		c.gridx = 0;
		c.gridy = 0;

		/* end first row */
		this.add(infoPanel, c);
	}

	private void initSelectPanel() {
		GridLayout gridLayoutSelect = new GridLayout(1, 1, 1, 1);

		selectPanel.setBounds(new Rectangle(1, 4, 150, 24));
		selectPanel.setLayout(gridLayoutSelect);

		/* begin first row */
		BoxLayout blSelectPanel = new BoxLayout(selectPanel, BoxLayout.X_AXIS);
		selectPanel.setLayout(blSelectPanel);
		selectPanel.add(Box.createHorizontalStrut(20));

		// start selectAllCheckBox properties
		selectAllCheckBox.setText("");
		selectAllCheckBox.setPreferredSize(new java.awt.Dimension(20, 24));
		selectAllCheckBox.setMaximumSize(new Dimension(20, 24));
		selectAllCheckBox.setMinimumSize(new Dimension(10, 24));
		selectAllCheckBox
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		selectAllCheckBox.setActionCommand(ACTION_SELECT_ALL);
		selectAllCheckBox.addActionListener(getDefaultActionListener());
		// end selectAllCheckBox properties
		selectPanel.add(selectAllCheckBox);

		// start selectAllLabel properties
		selectAllLabel.setText("Select All");
		selectAllLabel.setPreferredSize(new java.awt.Dimension(150, 24));
		selectAllLabel.setMaximumSize(new Dimension(150, 24));
		selectAllLabel.setMinimumSize(new Dimension(120, 24));
		selectAllLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		// end selectAllLabel properties
		selectPanel.add(selectAllLabel);

		// start unSelectAllCheckBox properties
		unSelectAllCheckBox.setText("");
		unSelectAllCheckBox.setPreferredSize(new java.awt.Dimension(20, 24));
		unSelectAllCheckBox.setMaximumSize(new Dimension(20, 24));
		unSelectAllCheckBox.setMinimumSize(new Dimension(10, 24));
		unSelectAllCheckBox
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		unSelectAllCheckBox.setActionCommand(ACTION_UNSELECT_ALL);
		unSelectAllCheckBox.addActionListener(getDefaultActionListener());
		// end unSelectAllCheckBox properties
		selectPanel.add(unSelectAllCheckBox);

		// start unSelectAllLabel properties
		unSelectAllLabel.setText("Unselect All");
		unSelectAllLabel.setPreferredSize(new java.awt.Dimension(150, 24));
		unSelectAllLabel.setMaximumSize(new Dimension(150, 24));
		unSelectAllLabel.setMinimumSize(new Dimension(120, 24));
		unSelectAllLabel
				.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		// end unSelectAllLabel properties
		selectPanel.add(unSelectAllLabel);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0.15;
		c.gridx = 0;
		c.gridy = 1;

		/* end first row */
		this.add(selectPanel, c);
	}

	private void initOptimizeContractPanel() {
		GridLayout gridLayoutOptimizeContracts = new GridLayout(1, 1, 1, 1);

		optimizeContractsPanel.setBounds(new Rectangle(1, 1, 150, 24));
		optimizeContractsPanel.setLayout(gridLayoutOptimizeContracts);

		/* begin first row */
		BoxLayout blOptimizeContractsPanel = new BoxLayout(
				optimizeContractsPanel, BoxLayout.X_AXIS);
		optimizeContractsPanel.setLayout(blOptimizeContractsPanel);
		optimizeContractsPanel.add(Box.createHorizontalStrut(20));

		// start optimizeContractsButton properties
		optimizeContractsButton.setText(OPTIMIZE_CONTRACTS);
		optimizeContractsButton
				.setPreferredSize(new java.awt.Dimension(150, 24));
		optimizeContractsButton.setMaximumSize(new Dimension(150, 24));
		optimizeContractsButton.setMinimumSize(new Dimension(120, 24));
		optimizeContractsButton
				.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		// end optimizeContractsButton properties
		optimizeContractsButton.setActionCommand(ACTION_OPTIMIZE_CONTRACTS);
		optimizeContractsButton.addActionListener(getDefaultActionListener());
		optimizeContractsPanel.add(optimizeContractsButton);
		optimizeContractsPanel.add(Box.createHorizontalStrut(10));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0.15;
		c.gridx = 0;
		c.gridy = 2;

		/* end first row */
		this.add(optimizeContractsPanel, c);

	}

	private DefaultActionListener getDefaultActionListener() {
		if (defaultActionListener == null)
			defaultActionListener = new DefaultActionListener();
		return defaultActionListener;
	}
}
