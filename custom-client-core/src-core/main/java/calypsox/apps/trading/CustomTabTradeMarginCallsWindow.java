/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.trading;

import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JTextField;

import com.calypso.apps.trading.CustomTabTradeWindow;
import com.calypso.apps.trading.ShowTrade;
import com.calypso.tk.core.Trade;

public class CustomTabTradeMarginCallsWindow implements CustomTabTradeWindow {

	protected ShowTrade _showTrade = null;

	@Override
	public void buildTrade(final Trade trade) {
		// nothing to do
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getMainTabPanels() {
		final JPanel panel = new JPanel();
		panel.setName("Custom Keywords");

		// Add components to the Panel here
		panel.add(new JTextField("Soma"));

		final Vector panels = new Vector();
		panels.add(panel);
		return panels;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getMarketDataTabPanels() {
		final Vector panels = new Vector();
		return panels;
	}

	@Override
	public void newTrade() {
		// Nothing to do
	}

	@Override
	public void setShowTrade(final ShowTrade showTrade) {
		this._showTrade = showTrade;
	}

	@SuppressWarnings("unused")
	@Override
	public void showTrade(final Trade arg0) {
		// Need to add code to show keywords here
		final String str = "temp";
	}

}
