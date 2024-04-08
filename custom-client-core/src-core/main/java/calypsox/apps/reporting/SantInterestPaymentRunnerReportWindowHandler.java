/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.reporting;


import calypsox.tk.report.SantInterestPaymentRunnerEntry;
import calypsox.tk.report.SantInterestPaymentRunnerReportTemplate;
import calypsox.tk.report.SantInterestPaymentUtil;
import com.calypso.apps.refdata.AccountFrame;
import com.calypso.apps.refdata.BOMarginCallConfigWindow;
import com.calypso.apps.reporting.ReportObjectHandler;
import com.calypso.apps.reporting.ReportPanel;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.reporting.ReportWindowHandlerAdapter;
import com.calypso.apps.trading.TradeUtil;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.workflow.BOMessageWorkflow;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SantInterestPaymentRunnerReportWindowHandler extends ReportWindowHandlerAdapter implements ActionListener {

	JMenu tradeActionsBar = new JMenu("Trade Actions");
	JMenu messageActionBar = new JMenu("Message Actions");
	JMenu swiftActionBar = new JMenu("Swift Actions");

	public static final String  PAYMENTMSG = "PAYMENTMSG";
	public static final String  MC_INTEREST = "MC_INTEREST";

	@Override
	public JMenu getCustomMenu(ReportWindow window) {
		setReportWindow(window);
		JMenu showMenu = new JMenu("Show");

		JMenuItem ibMenuItem = new JMenuItem("Interest Bearing");
		ibMenuItem.setActionCommand("Interest Bearing");
		ibMenuItem.addActionListener(this);

		tradeActionsBar.addMenuListener( new CustomMenuListener());

		JMenuItem ctMenuItem = new JMenuItem("Customer Transfer");
		ctMenuItem.setActionCommand("Customer Transfer");
		ctMenuItem.addActionListener(this);

		JMenuItem simpleXferMenuItem = new JMenuItem("Notification");
		simpleXferMenuItem.setActionCommand("Notification");
		simpleXferMenuItem.addActionListener(this);

		JMenuItem accountMenuItem = new JMenuItem("Account");
		accountMenuItem.setActionCommand("Account");
		accountMenuItem.addActionListener(this);

		JMenuItem accountActivityMenuItem = new JMenuItem("Account Activity");
		accountActivityMenuItem.setActionCommand("Account Activity");
		accountActivityMenuItem.addActionListener(this);

		JMenuItem contractMenuItem = new JMenuItem("MarginCall Contract");
		contractMenuItem.setActionCommand("MarginCall Contract");
		contractMenuItem.addActionListener(this);



		showMenu.add(ibMenuItem);
		showMenu.add(ctMenuItem);
		showMenu.add(simpleXferMenuItem);
		showMenu.add(accountMenuItem);
		showMenu.add(accountActivityMenuItem);
		showMenu.add(contractMenuItem);
		showMenu.add(new JSeparator());
		showMenu.add(tradeActionsBar);
		showMenu.add(messageActionBar);
		showMenu.add(swiftActionBar);

		return showMenu;
	}

	@Override
	public void callBeforeLoad(ReportPanel panel) {
		super.callBeforeLoad(panel);
	}

	private Vector<SantInterestPaymentRunnerEntry> getSelectedEntry(){

		if (this._reportWindow == null) {
			return null;
		}

		if (this._reportWindow.getReportPanel().getRowCount() <= 0) {
			return null;
		}

		Vector<SantInterestPaymentRunnerEntry> objectVector = this._reportWindow.getReportPanel().getSelectedObjects(
				SantInterestPaymentRunnerReportTemplate.ROW_DATA);

		if (objectVector.size() == 0) {
			AppUtil.displayWarning("Select at least one row", this._reportWindow);
		}

		return objectVector;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(final ActionEvent e) {
		if (this._reportWindow == null) {
			return;
		}

		if (this._reportWindow.getReportPanel().getRowCount() <= 0) {
			return;
		}

		Vector<SantInterestPaymentRunnerEntry> objectVector = this._reportWindow.getReportPanel().getSelectedObjects(
				SantInterestPaymentRunnerReportTemplate.ROW_DATA);
		if (objectVector.size() > 1) {
			AppUtil.displayWarning("Select at most one row", this._reportWindow);
		}
		if (objectVector.size() == 0) {
			AppUtil.displayWarning("Select at least one row", this._reportWindow);
		}

		final SantInterestPaymentRunnerEntry entry = objectVector.get(0);
		if (e.getActionCommand().equals("Interest Bearing")) {
			if (entry.getIbTrade() != null) {
				ReportObjectHandler.showTrade(entry.getIbTrade());
			}
		} else if (e.getActionCommand().equals("Notification")) {
			if (entry.getSimpleXferTrade() != null) {
				ReportObjectHandler.showTrade(entry.getSimpleXferTrade());
			}
		} else if (e.getActionCommand().equals("Customer Transfer")) {
			if (entry.getCtTrade() != null) {
				ReportObjectHandler.showTrade(entry.getCtTrade());
			}
		} else if (e.getActionCommand().equals("Account")) {
			AccountFrame win = new AccountFrame();
			Account account = BOCache.getAccount(DSConnection.getDefault(), entry.getAccount().getId());
			win.show(account);
			win.setVisible(true);
		} else if (e.getActionCommand().equals("Account Activity")) {
			Account account = BOCache.getAccount(DSConnection.getDefault(), entry.getAccount().getId());
			AccountFrame.showAccountActivityReport(account, this._reportWindow);
		} else if (e.getActionCommand().equals("MarginCall Contract")) {
			BOMarginCallConfigWindow win = new BOMarginCallConfigWindow();
			Integer contractId = getContractId(entry.getAccount());
			if (contractId != null) {
				win.showMarginCallConfig(contractId);
			}
			win.setVisible(true);
		}

	}

	private Integer getContractId(Account account) {
		String mccIdStr = account.getAccountProperty("MARGIN_CALL_CONTRACT");

		if (Util.isEmpty(mccIdStr)) {
			return null;
		}

		try {
			return Integer.parseInt(mccIdStr);
		} catch (Exception e) {
			Log.error(this, e); //sonar
		}
		return null;
	}

	/**
	 * Refresh report data.
	 */
	private void refreshReport(Vector<SantInterestPaymentRunnerEntry> selectedEntry){
        if(!Util.isEmpty(selectedEntry)){
            for(SantInterestPaymentRunnerEntry entry : selectedEntry){

				SantInterestPaymentUtil.getInstance().reloadMessages(entry);

                if(entry.getCtTrade()!=null){
                    Trade trade = reloadTrade(entry.getCtTrade().getLongId());
                    if("CANCELED".equalsIgnoreCase(trade.getStatus().toString())){
                        //remove customer transfer trade from report if canceled
                        entry.setCtTrade(null);
                    }else{
                        entry.setCtTrade(reloadTrade(entry.getCtTrade().getLongId()));
                    }
                }
            }
            this._reportWindow.getReportPanel().refresh();
        }
	}


	/**
	 * Update Trade or Message Actions
	 */
	class CustomMenuListener implements MenuListener {

		@Override
		public void menuSelected(MenuEvent e) {

			List<String> aplicableActions = new ArrayList<>();

			Vector<SantInterestPaymentRunnerEntry> selectedEntry = getSelectedEntry();
			messageActionBar.removeAll();
			tradeActionsBar.removeAll();
			swiftActionBar.removeAll();
			if(selectedEntry!=null){
				for(SantInterestPaymentRunnerEntry entry : selectedEntry){
					Trade ctTrade = entry.getCtTrade();
					if(null!=ctTrade){
						Vector<String> actions = TradeUtil.getActions(ctTrade, DSConnection.getDefault());
						if(!Util.isEmpty(actions)){
							for(String action : actions){
								if(!aplicableActions.contains(action)){
									aplicableActions.add(action);
								}
							}
							aplicableActions = Util.sort(aplicableActions);

						}
					}
				}
			}
			//Create Actions options for trade
			for(String action : aplicableActions){
				JMenuItem tradeActions = new JMenuItem(action);
				tradeActions.setActionCommand("TRADE."+action);
				tradeActions.addActionListener(new ActionRowListener());
				tradeActionsBar.add(tradeActions);
			}
			//Message MC_INTEREST
			aplicableActions = new ArrayList<>();
			if(selectedEntry!=null){
				for(SantInterestPaymentRunnerEntry entry : selectedEntry){
					BOMessage mcInterestMessage = entry.getInterest();
					if(null!=mcInterestMessage){
						Vector<String> actions = BOMessageWorkflow.getBOMessageActions(mcInterestMessage, DSConnection.getDefault());
						if(!Util.isEmpty(actions)){
							for(String action : actions){
								if(!aplicableActions.contains(action)){
									aplicableActions.add(action);
								}
							}
							aplicableActions = Util.sort(aplicableActions);

						}
					}
				}
			}
			//Create Actions options for message
			for(String action : aplicableActions){
				JMenuItem messageAction = new JMenuItem(action);
				messageAction.setActionCommand("MESSAGE."+action);
				messageAction.addActionListener(new ActionRowListener());
				messageActionBar.add(messageAction);
			}

			//Message SWIFT
			aplicableActions = new ArrayList<>();
			if(selectedEntry!=null){
				for(SantInterestPaymentRunnerEntry entry : selectedEntry){
					BOMessage paymentMessage = entry.getPaymentMessage();
					if(null!=paymentMessage){
						Vector<String> actions = BOMessageWorkflow.getBOMessageActions(paymentMessage, DSConnection.getDefault());
						if(!Util.isEmpty(actions)){
							for(String action : actions){
								if(!aplicableActions.contains(action)){
									aplicableActions.add(action);
								}
							}
							aplicableActions = Util.sort(aplicableActions);
						}
					}
				}
			}
			//Create Actions options for message
			for(String action : aplicableActions){
				JMenuItem messageAction = new JMenuItem(action);
				messageAction.setActionCommand("SWMESSAGE."+action);
				messageAction.addActionListener(new ActionRowListener());
				swiftActionBar.add(messageAction);
			}
		}

		/*Clean Actions*/
		@Override
		public void menuDeselected(MenuEvent e) {

		}

		@Override
		public void menuCanceled(MenuEvent e) {

		}
	}

	/**
	 * Actions for Updates trades OR messages
	 */
	class ActionRowListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
            Vector<SantInterestPaymentRunnerEntry> selectedEntry = getSelectedEntry();

            if(e.getActionCommand().contains("TRADE")){ /*Update selected trades apply actions*/
				List<Trade> tradesToSave = new ArrayList<>();
				String action = e.getActionCommand().substring(6);

				for(SantInterestPaymentRunnerEntry entry : selectedEntry){
					Trade ctTrade = entry.getCtTrade();
					if(null!=ctTrade){
						ctTrade.setAction(Action.valueOf(action));
						tradesToSave.add(ctTrade);
						if("CANCEL".equalsIgnoreCase(action)){
						    entry.setCtTrade(null);
                        }
					}
				}
				saveTrades(tradesToSave);

			}else if(e.getActionCommand().contains("SWMESSAGE")){ /*Update selected messages apply actions*/
				List<BOMessage> messagesToSave = new ArrayList<>();
				String action = e.getActionCommand().substring(10);
				for(SantInterestPaymentRunnerEntry entry : selectedEntry){
					BOMessage paymentMessage = entry.getPaymentMessage();
					if(null!=paymentMessage){
						paymentMessage.setAction(Action.valueOf(action));
						messagesToSave.add(paymentMessage);
					}
				}
				saveMessages(messagesToSave);
			}else if(e.getActionCommand().contains("MESSAGE")){ /*Update selected messages apply actions*/
				List<BOMessage> messagesToSave = new ArrayList<>();
				String action = e.getActionCommand().substring(8);
				for(SantInterestPaymentRunnerEntry entry : selectedEntry){
					entry.getPaymentMessage();
					BOMessage mcInterstMessage = entry.getInterest();
					if(null!=mcInterstMessage){
						mcInterstMessage.setAction(Action.valueOf(action));
						messagesToSave.add(mcInterstMessage);
					}
				}
				saveMessages(messagesToSave);

			}

			refreshReport(selectedEntry);
		}

		private void saveTrades(List<Trade> trades){ //TODO optimize
			if(!Util.isEmpty(trades)){
				try {
					DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(trades));
				} catch (CalypsoServiceException e) {
					Log.error(this,"Error saving trades: " + e);
				} catch (InvalidClassException e) {
					Log.error(this,"Error creating ExternalArray for saving trades: " + e);
				}
			}
		}

		/**
		 * Saving messages
		 * @param messages
		 */
		private void saveMessages(List<BOMessage> messages){ //TODO optimize
			if(!Util.isEmpty(messages)){
				for(BOMessage message : messages){
					try {
						DSConnection.getDefault().getRemoteBackOffice().save(message,0,null);
					} catch (CalypsoServiceException e) {
						Log.error(this,"Error saving message: " + e);
					}
				}
			}
		}
	}

    private Trade reloadTrade(long id){
        try {
            return DSConnection.getDefault().getRemoteTrade().getTrade(id);
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading Customer Transfer: " + e );
        }
        return null;
    }
}
