package calypsox.apps.feed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import calypsox.tk.event.PSEventSendOptimizerMarginCall;
import calypsox.tk.event.PSEventSendOptimizerMarginCallStatus;
import calypsox.tk.util.SantCollateralOptimConstants;
import calypsox.tk.util.optimizer.OptimizerStatusUtil;

import com.calypso.apps.util.AppUtil;
import com.calypso.apps.util.CalypsoCheckBox;
import com.calypso.apps.util.ExoticGUIUtils;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.ESStarter;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSSubscriber;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;

public class SantMarginCallEntryFeedOptimizerWindow extends JFrame implements
		SantCollateralOptimConstants {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8334549470177570434L;

	// Labels
	private static final String SENDING_ENTRIES_TO_OPTIMIZER = "Sending entries to Optimizer...";
	// private static final String SEND_OSLA_ENTRIES = "Send OSLA entries";
	private static final String SEND_OPTIMIZER_ENTRIES = "Send Optimizer Entries";

	private static final String WINDOW_TITLE = "MarginCallEntry Feed Optimizer";

	// panels
	protected JPanel mainPanel = new JPanel();

	protected JPanel infoPanel = new JPanel();
	protected JPanel oslaPanel = new JPanel();
	protected JPanel sendPanel = new JPanel();

	// buttons
	protected JButton sendButton = new JButton();

	// Info fields
	protected JLabel infoLabel = new JLabel();

	// OSLA fields
	protected CalypsoCheckBox oslaCheckBox = new CalypsoCheckBox();
	protected JLabel oslaLabel = new JLabel();

	private BorderLayout borderLayout1 = new BorderLayout();

	public SantMarginCallEntryFeedOptimizerWindow() {
		setTitle(WINDOW_TITLE);
		setSize(600, 200);
		setResizable(true);
		setVisible(true);
		setLocation(200, 200);
		initPanels();
	}

	// *** Panels & tables management *** //
	protected void initPanels() {
		this.getContentPane().setLayout(borderLayout1);
		getContentPane().setBackground(
				AppUtil.makeDarkerColor(getContentPane().getBackground(),
						ExoticGUIUtils.CALLOUT_EXTRA_DARKER_FACTOR));

		GridLayout gridLayoutMain = new GridLayout(2, 1, 2, 2);
		mainPanel.setLayout(gridLayoutMain);

		initInfoPanel();
		// initOslaPanel();
		initSendPanel();

		getContentPane().add(mainPanel, BorderLayout.CENTER);

		final SymAction lSymAction = new SymAction();
		this.sendButton.addActionListener(lSymAction);
	}

	private void initInfoPanel() {
		GridLayout gridLayoutInfo = new GridLayout(1, 1, 1, 1);

		infoPanel.setBounds(new Rectangle(1, 1, 150, 24));
		infoPanel.setLayout(gridLayoutInfo);

		/* begin first row */
		BoxLayout blInfoPanel = new BoxLayout(infoPanel, BoxLayout.X_AXIS);
		infoPanel.setLayout(blInfoPanel);
		infoPanel.add(Box.createHorizontalStrut(20));

		// start infoLabel properties
		infoLabel.setFont(new Font(infoLabel.getFont().getName(), Font.BOLD
				+ Font.ITALIC, infoLabel.getFont().getSize()));
		infoLabel.setText("");
		infoLabel.setPreferredSize(new java.awt.Dimension(150, 24));
		infoLabel.setMaximumSize(new Dimension(550, 24));
		infoLabel.setMinimumSize(new Dimension(120, 24));
		infoLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		// end oslaLabel properties
		infoPanel.add(infoLabel);
		infoPanel.add(Box.createHorizontalStrut(10));
		infoPanel.setBorder(BorderFactory.createEtchedBorder());

		/* end first row */
		mainPanel.add(infoPanel, BorderLayout.WEST);
	}

	// private void initOslaPanel() {
	// GridLayout gridLayoutOsla = new GridLayout(1, 1, 1, 1);
	//
	// oslaPanel.setBounds(new Rectangle(1, 1, 150, 24));
	// oslaPanel.setLayout(gridLayoutOsla);
	// oslaPanel.add(Box.createHorizontalStrut(35));
	//
	// /* begin first row */
	// BoxLayout blOslaPanel = new BoxLayout(oslaPanel, BoxLayout.X_AXIS);
	// oslaPanel.setLayout(blOslaPanel);
	//
	// // start oslaLabel properties
	// oslaLabel.setText(SEND_OSLA_ENTRIES);
	// oslaLabel.setPreferredSize(new java.awt.Dimension(80, 24));
	// oslaLabel.setMaximumSize(new Dimension(120, 24));
	// oslaLabel.setMinimumSize(new Dimension(70, 24));
	// oslaLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
	// // end oslaLabel properties
	// oslaPanel.add(oslaLabel);
	// oslaPanel.add(Box.createHorizontalStrut(10));
	//
	// // start oslaCheckBox properties
	// oslaCheckBox.setPreferredSize(new java.awt.Dimension(20, 24));
	// oslaCheckBox.setMaximumSize(new Dimension(30, 24));
	// oslaCheckBox.setMinimumSize(new Dimension(20, 24));
	// oslaCheckBox.setToolTipText(SEND_OSLA_ENTRIES);
	// // end oslaCheckBox properties
	// oslaPanel.add(oslaCheckBox);
	//
	// oslaPanel.setBorder(BorderFactory.createEtchedBorder());
	//
	// /* end first row */
	// mainPanel.add(oslaPanel, BorderLayout.WEST);
	// }

	private void initSendPanel() {
		GridLayout gridLayoutSend = new GridLayout(1, 1, 1, 1);

		sendPanel.setBounds(new Rectangle(1, 1, 150, 24));
		sendPanel.setLayout(gridLayoutSend);

		/* begin first row */
		BoxLayout blSendPanel = new BoxLayout(sendPanel, BoxLayout.X_AXIS);
		sendPanel.setLayout(blSendPanel);
		sendPanel.add(Box.createHorizontalStrut(50));

		// start sendButton properties
		this.sendButton.setText(SEND_OPTIMIZER_ENTRIES);
		sendButton.setPreferredSize(new java.awt.Dimension(120, 24));
		sendButton.setMaximumSize(new Dimension(150, 24));
		sendButton.setMinimumSize(new Dimension(80, 24));
		sendButton.setOpaque(false);
		sendButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		sendButton.setFont(new Font("Dialog", Font.BOLD, 12));
		sendButton.setBorder(BorderFactory.createEtchedBorder(1));
		sendButton.setToolTipText(SEND_OPTIMIZER_ENTRIES);
		// end sendButton properties
		sendPanel.add(sendButton, BorderLayout.CENTER);
		sendPanel.add(Box.createHorizontalStrut(1));
		/* end first row */

		sendPanel.setBorder(BorderFactory.createEtchedBorder());

		mainPanel.add(sendPanel, BorderLayout.WEST);
	}

	// *** Listeners *** //
	class SymAction implements java.awt.event.ActionListener {
		@Override
		public void actionPerformed(final java.awt.event.ActionEvent event) {
			final Object object = event.getSource();
			if (object == SantMarginCallEntryFeedOptimizerWindow.this.sendButton) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				sendButton_actionPerformed(event);
			}
		}
	}

	void sendButton_actionPerformed(final java.awt.event.ActionEvent event) {
		if (OptimizerStatusUtil.isUnderOptimization()) {
			infoLabel.setText("");
			if (!AppUtil
					.displayQuestion(
							"WARNING: Optimization under process, do you really want to launch a complete new optimization?",
							this)) {
				return;
			}
		}
		infoLabel.setForeground(Color.red);
		infoLabel.setText(SENDING_ENTRIES_TO_OPTIMIZER);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		PSEventSendOptimizerMarginCall eventMC = new PSEventSendOptimizerMarginCall();
		try {
			DSConnection.getDefault().getRemoteTrade().saveAndPublish(eventMC);
			infoLabel.setForeground(Color.black);
			infoLabel.setText(SENDING_ENTRIES_TO_OPTIMIZER);
		} catch (Exception e) {
			Log.error(
					SantMarginCallEntryFeedOptimizerWindow.class.getName()
							+ ": failed to publish PSEventSendOptimizerMarginCall event",
					e);
			infoLabel
					.setText("Failed to publish PSEventSendOptimizerMarginCall event:"
							+ e);
		}

		Runnable stRun = new Runnable() {
			@SuppressWarnings({ "rawtypes", "unused" })
			public void run() {
				SantMarginCallEntryFeedOptimizerListener eventListener = new SantMarginCallEntryFeedOptimizerListener(
						infoLabel);
				// events we are interested in
				Class[] subscriptionList = new Class[] { PSEventSendOptimizerMarginCallStatus.class, };
				try {
					PSConnection ps = ESStarter.startConnection(eventListener,
							subscriptionList);
				} catch (ConnectException e) {
					Log.error(this, e);
				}
			}

			// private void lauchSTOptimizer() {
			// try {
			// infoLabel.setForeground(Color.black);
			// infoLabel.setText(SENDING_ENTRIES_TO_OPTIMIZER);
			//
			// ScheduledTaskSANT_EXPORT_OPTIMIZER st =
			// (ScheduledTaskSANT_EXPORT_OPTIMIZER) DSConnection
			// .getDefault()
			// .getRemoteBO()
			// .getScheduledTaskByExternalReference(
			// "EXPORT_OPT_MC_STATUS_REPRO");
			// if (st != null) {
			// // run ST
			// runSTInScheduler(st);
			//
			// infoLabel.setForeground(Color.red);
			// infoLabel.setText("Entries sent to Optimizer");
			//
			// //PSEventSantOptimizerFile event = new
			// PSEventSantOptimizerFile("FileName", 100);
			//
			// //DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
			// }
			// } catch (RemoteException e) {
			// Log.error(SantMarginCallEntryFeedOptimizerWindow.class
			// .getName(), e);
			// } finally {
			// setCursor(origCursor);
			// }
			// }

			// private void runSTInScheduler(ScheduledTaskSANT_EXPORT_OPTIMIZER
			// st) {
			// JDatetime executeTime = new JDatetime();
			// st.setDatetime(executeTime);
			// // PSEventScheduling event = new PSEventScheduling(
			// // PSEventScheduling.SchedulingEventType.RUN_REQUEST);
			// // event.setScheduledTask(st);
			//
			// PSEventSendOptimizerMarginCall event = new
			// PSEventSendOptimizerMarginCall();
			// PSConnection ps = MainEntryJFrame.getPSConnection();
			// try {
			// ps.publish(event);
			// } catch (Exception e) {
			// Log.error(
			// SantMarginCallEntryFeedOptimizerWindow.class
			// .getName()
			// + ": failed to publish scheduling event", e);
			// infoLabel
			// .setText("Failed to publish scheduling event:" + e);
			// }
			//
			// }
		};
		SwingUtilities.invokeLater(stRun);
	}

	// public void start(int esPort, String esHost, String user) {
	// try {
	// Vector<String> events = new Vector<String>();
	// events.addElement(PSEventSendOptimizerMarginCall.class.getName());
	// PSConnection ps = ESStarter.startConnection(PSS.getDefault(), this);
	// ps.start();
	// ps.subscribe(events);
	//
	// PSConnection ps = ESStarter.startConnection(
	// DSConnection.getDefault(), this);
	// ps.start();
	// PSConnection.setCurrent(ps);
	//
	// Vector eventClassNames = new Vector();
	// eventClassNames.addElement(new PSEventTrade().getClass().getName());
	//
	// ps.subscribe(eventClassNames);
	//
	// } catch (Exception e) {
	// Log.error(S_CUSTOM_BLB, e);
	// return;
	// }
	// }

	// @SuppressWarnings({ "rawtypes", "unused" })
	// @Override
	// public void onStartUp() {
	// SantMarginCallEntryFeedOptimizerListener eventListener = new
	// SantMarginCallEntryFeedOptimizerListener(
	// infoLabel);
	// // events we are interested in
	// Class[] subscriptionList = new Class[] {
	// PSEventSendOptimizerMarginCallStatus.class, };
	// try {
	// PSConnection ps = ESStarter.startConnection(eventListener,
	// subscriptionList);
	// } catch (ConnectException e) {
	// Log.error(this, e);
	// }
	// }

	private static class SantMarginCallEntryFeedOptimizerListener implements
			PSSubscriber {
		private JLabel infoLabel = null;

		public SantMarginCallEntryFeedOptimizerListener(JLabel infoLabel) {
			this.infoLabel = infoLabel;
		}

		@Override
		public void newEvent(PSEvent event) {
			if (!(event instanceof PSEventSendOptimizerMarginCallStatus)) {
				return;
			}
			PSEventSendOptimizerMarginCallStatus sendOptimizerMarginCallEventStatus = ((PSEventSendOptimizerMarginCallStatus) event);
			if (!Util.isEmpty(sendOptimizerMarginCallEventStatus.getFileName())) {
				infoLabel.setForeground(Color.red);
				infoLabel.setText("Message sent to Optimizer ["
						+ sendOptimizerMarginCallEventStatus.getFileName()
						+ "] ("
						+ sendOptimizerMarginCallEventStatus.getNbRecords()
						+ " entries).");
			}
		}

		@Override
		public void onDisconnect() {
			infoLabel.setForeground(Color.red);
			infoLabel.setText("Disconnected from EventServer");
			Log.error(SantMarginCallEntryFeedOptimizerWindow.class.getName(),
					"Disconnected from EventServer");
		}
	}
}
