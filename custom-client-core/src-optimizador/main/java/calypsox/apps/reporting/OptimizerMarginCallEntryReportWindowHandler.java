package calypsox.apps.reporting;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.calypso.apps.reporting.ReportWindow;
import com.calypso.apps.reporting.ReportWindowHandlerAdapter;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;

import calypsox.tk.util.OptimizerMarginCallEntryConstants;

public class OptimizerMarginCallEntryReportWindowHandler extends
		ReportWindowHandlerAdapter implements OptimizerMarginCallEntryConstants {

	private ReportWindow _reportWindow = null;
	private MenuActionListener _actionListener = null;

	private static final String ACTION_CHECK_OPTIMIZE_MC_ENTRIES = "Select for Optimization";
	private static final String ACTION_UNCHECK_OPTIMIZE_MC_ENTRIES = "Unselect for Optimization";
	private static final String ACTION_OPTIMIZE_CONTRACTS = "Optimize Contracts";

	public JMenu getCustomMenu(ReportWindow window) {
		JMenu menu = null;
		_reportWindow = window;
		menu = new JMenu("Process", true);
		menu.add(getMenuItem(ACTION_CHECK_OPTIMIZE_MC_ENTRIES,
				ACTION_CHECK_OPTIMIZE_MC_ENTRIES));
		menu.add(getMenuItem(ACTION_UNCHECK_OPTIMIZE_MC_ENTRIES,
				ACTION_UNCHECK_OPTIMIZE_MC_ENTRIES));
		menu.add(getMenuItem(ACTION_OPTIMIZE_CONTRACTS,
				ACTION_OPTIMIZE_CONTRACTS));

		return menu;
	}

	protected JMenuItem getMenuItem(String label, String action) {
		JMenuItem item = new JMenuItem(label);
		item.setActionCommand(action);
		item.addActionListener(getMenuActionListener());
		return item;
	}

	protected MenuActionListener getMenuActionListener() {
		if (_actionListener == null) {
			_actionListener = new MenuActionListener();
		}
		return _actionListener;
	}

	protected class MenuActionListener implements java.awt.event.ActionListener {
		public void actionPerformed(java.awt.event.ActionEvent event) {
			menuAction(event.getSource(), event.getActionCommand());
		}
	}

	protected void menuAction(Object source, String action) {
		if (ACTION_CHECK_OPTIMIZE_MC_ENTRIES.equals(action)) {
			setRowsOptimizePropertyValue(Boolean.TRUE);
			_reportPanel.refresh();

		} else if (ACTION_UNCHECK_OPTIMIZE_MC_ENTRIES.equals(action)) {
			setRowsOptimizePropertyValue(Boolean.FALSE);
			_reportPanel.refresh();

		} else if (ACTION_OPTIMIZE_CONTRACTS.equals(action)) {
			DefaultReportOutput output = (DefaultReportOutput) _reportWindow
					.getReportPanel().getOutput();
			if (output != null && output.getRows() != null) {
				ReportRow[] rows = output.getRows();
				for (int i = 0; i < rows.length; i++) {
					@SuppressWarnings("unused")
					MarginCallEntryDTO marginCall = (MarginCallEntryDTO) rows[i]
							.getProperty(DEFAULT_PROPERTY);
				}
			}
		}
	}

	private void setRowsOptimizePropertyValue(Boolean propertyValue) {
		if (_reportWindow != null
				&& _reportWindow.getReportPanel() != null
				&& _reportWindow.getReportPanel().getSelectedReportRows() != null) {
			ReportRow[] reportRow = _reportWindow.getReportPanel()
					.getSelectedReportRows();
			if (reportRow != null) {
				for (int i = 0; i < reportRow.length; i++) {
					if (reportRow[i] == null) {
						continue;
					}
					reportRow[i].setProperty(OPTIMIZE_PROPERTY, propertyValue);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.calypso.apps.reporting.ReportWindowHandlerAdapter#clean()
	 */
	public void clean() {
		super.clean();
		this._reportWindow = null;
	}
}
