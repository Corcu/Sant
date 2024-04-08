/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.reporting;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.calypso.apps.reporting.AllocationDialog;
import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.presentation.risk.RiskPresenterWorker;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.report.ReportRow;


public class TradeReportWindowHandler extends com.calypso.apps.reporting.TradeReportWindowHandler {



	private static final String ACTION_MENU_NAME = "Action";
	private static final String BO_UTI_AMEND_ACTION = "BO_UTI_AMEND";
	private static final String PROCESS_ACTIONS = "BO_UTI_AMEND ";
	public static final String KEYWORD_UTI_TRADE_ID = "UTI_REFERENCE";
	public static final String KEYWORD_USI_TRADE_ID = "USI_REFERENCE";
	public static final String KEYWORD_PRIOR_UTI_PREFIX_TRADE_ID = "PriorUTIPrefix";
	public static final String KEYWORD_PRIOR_UTI_VALUE_TRADE_ID = "PriorUTIValue";
	public static final String ALLOCATE="Allocate";


	/**
	 * remove action from Action menu. Users will use Process menu
	 */
	@Override
	public void customizePopupMenu(final JPopupMenu jpopupmenu, final RiskPresenterWorker riskpresenterworker) {
		super.customizePopupMenu(jpopupmenu, riskpresenterworker);
		// look for Action Menu
		final Component[] components = jpopupmenu.getComponents();
		for (int i = 0; i < components.length; i++) {
			final Component component = components[i];
			if (component instanceof JMenu) {
				final JMenu menu = (JMenu) component;
				final String menuName = menu.getText();
				if (ACTION_MENU_NAME.equalsIgnoreCase(menuName)) {
					// check values inside this menu
					final JPopupMenu popUpActions = menu.getPopupMenu();
					final Component[] actions = popUpActions.getComponents();

					for (int j = 0; j < actions.length; j++) {
						final JMenuItem action = (JMenuItem) actions[j];
						final String actionName = action.getText();
						if (PROCESS_ACTIONS.contains(actionName)) {
							action.setVisible(false);
						}
					}
					break;
				}
			}
		}
	}



	@Override
	public JMenu getCustomMenu(final ReportWindow window) {
		final ActionListener actionListener = new CustomActionListener();
		final JMenu menu = new JMenu("Process", true);
		final JMenuItem boUtiAmendAction = new JMenuItem(BO_UTI_AMEND_ACTION);
		final JMenuItem allocate = new JMenuItem(ALLOCATE);
		menu.add(boUtiAmendAction);
		menu.add(allocate);
		boUtiAmendAction.setActionCommand(BO_UTI_AMEND_ACTION);
		boUtiAmendAction.addActionListener(actionListener);
		allocate.addActionListener(actionListener);
		return menu;
	}



	public class CustomActionListener implements ActionListener {
		@Override
		public void actionPerformed(final java.awt.event.ActionEvent event) {
			final Vector<Trade> selectedTrades = getSelectedTrades();
			final String action = event.getActionCommand();
			if (BO_UTI_AMEND_ACTION.equalsIgnoreCase(action)) {
				applyBoUtiAmend(selectedTrades);
			}else if(ALLOCATE.equalsIgnoreCase(action)){
				applyAllocate(selectedTrades);
			}

		}
	}



	public Frame getReportFrame() {
		Frame frame = null;
		if ((getReportWindow() != null) || (getReportPanel() != null)) {
			frame = (getReportWindow() == null) ? AppUtil.getFrame() : getReportWindow();
		}
		return frame;
	}



	@SuppressWarnings("unchecked")
	Vector<Trade> getSelectedTrades() {
		final Frame frame = getReportFrame();
		Vector<Trade> trades = getReportPanel().getSelectedObjects(ReportRow.TRADE);
		if ((getReportPanel().getRowCount() <= 0) || Util.isEmpty(trades)) {
			AppUtil.displayWarning("Select a trade", frame);
			trades = null;
		}
		return trades;
	}



	public void applyBoUtiAmend(final Vector<Trade> selectedTrades) {
		final Frame frame = getReportFrame();
		final Trade trade = selectedTrades.get(0);
		if (selectedTrades.size() > 1) {
			AppUtil.displayWarning("Bulk BO_UTI_AMEND not allowed for more than one row.", frame);
		} else if (trade!=null && !Product.PERFORMANCESWAP.equalsIgnoreCase(trade.getProductFamily())) {
			AppUtil.displayWarning("BO_UTI_AMEND not allowed for this Product.", frame);
		} else {
			// if action authorized
			if (AccessUtil.isAuthorized(trade, BO_UTI_AMEND_ACTION)) {
				final List<String> kwToShow = new ArrayList<String>();
				kwToShow.add(KEYWORD_UTI_TRADE_ID);
				kwToShow.add(KEYWORD_USI_TRADE_ID);
				kwToShow.add(KEYWORD_PRIOR_UTI_PREFIX_TRADE_ID);
				kwToShow.add(KEYWORD_PRIOR_UTI_VALUE_TRADE_ID);
				SantKeywordsFrame keywordFrame = new SantKeywordsFrame(frame, BO_UTI_AMEND_ACTION, trade, kwToShow, new Vector<String>());
				keywordFrame.setVisible(true);
			} else {
				AppUtil.displayWarning("Permission denied for action BO_UTI_AMEND", frame);
			}
		}
	}

	public void applyAllocate(final Vector<Trade> selectedTrades) {
		final Frame frame = getReportFrame();
		final Trade trade = selectedTrades.get(0);
		if (selectedTrades.size() > 1) {
			AppUtil.displayWarning("Action [" + ALLOCATE + "] not allowed for more than one row.", frame);
		}else if (trade.isAllocationChild()){
			AppUtil.displayWarning("Allocate action can only be applied to the parent trade, " + trade.getLongId() + " is a child of an allocated trade", frame);
		}else{
			AllocationDialog ad;
			if(trade.getProduct() instanceof Bond){
				SantAllocationPanel bap = new SantAllocationPanel();
				ad = new AllocationDialog(frame, bap);
			}else{
				ad = new AllocationDialog(frame);
			}
			ad.setRoleAllocations(trade.getRoleAllocations(), trade);
			ad.setVisible(true);
		}
	}

}
